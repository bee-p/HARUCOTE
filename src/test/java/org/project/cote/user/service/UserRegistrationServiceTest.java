package org.project.cote.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.project.cote.common.dto.ErrorCode;
import org.project.cote.common.exception.ApiException;
import org.project.cote.common.security.oauth2.GithubOAuth2UserInfo;
import org.project.cote.common.security.oauth2.OAuth2UserInfo;
import org.project.cote.user.domain.AuthProvider;
import org.project.cote.user.domain.User;
import org.project.cote.user.event.UserRegisteredEvent;
import org.project.cote.user.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserRegistrationServiceTest {

    private UserRepository userRepository;
    private ApplicationEventPublisher eventPublisher;
    private UserRegistrationService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new UserRegistrationService(userRepository, eventPublisher);
    }

    @Test
    @DisplayName("기존 사용자(provider+providerId 일치)는 그대로 반환하고 이벤트는 발행하지 않는다")
    void returnsExistingUser_withoutEvent() {
        User existing = withId(User.create(AuthProvider.GITHUB, "12345", "u@e.com", "octocat", null), 7L);
        when(userRepository.findByProviderAndProviderId(AuthProvider.GITHUB, "12345"))
                .thenReturn(Optional.of(existing));

        User result = service.upsertFromOAuth2(githubInfo(12345L, "octocat", "u@e.com"));

        assertSame(existing, result);
        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("신규 사용자는 저장하고 UserRegisteredEvent 를 발행한다")
    void createsNewUser_andPublishesEvent() {
        when(userRepository.findByProviderAndProviderId(AuthProvider.GITHUB, "12345"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByNickname("octocat")).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> withId(inv.getArgument(0), 42L));

        User result = service.upsertFromOAuth2(githubInfo(12345L, "octocat", "u@e.com"));

        assertEquals(42L, result.getId());
        assertEquals("octocat", result.getNickname());

        ArgumentCaptor<UserRegisteredEvent> captor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(eventPublisher, times(1)).publishEvent(captor.capture());
        assertEquals(42L, captor.getValue().userId());
        assertEquals(AuthProvider.GITHUB, captor.getValue().provider());
        assertEquals("12345", captor.getValue().providerId());
    }

    @Test
    @DisplayName("OAuth2 응답에 providerId 가 누락되면 OAUTH2_PROVIDER_ERROR(502) 로 매핑한다")
    void missingProviderId_throwsOauth2ProviderError() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("login", "octocat");
        attrs.put("email", "u@e.com");
        // id 누락
        OAuth2UserInfo info = new GithubOAuth2UserInfo(attrs);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.upsertFromOAuth2(info));

        assertEquals(ErrorCode.OAUTH2_PROVIDER_ERROR, ex.getErrorCode());
        verify(userRepository, never()).findByProviderAndProviderId(any(), any());
        verify(userRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("동시 가입 race 로 (provider, providerId) 충돌 시 기존 사용자를 반환하고 이벤트는 발행하지 않는다")
    void concurrentRegistration_returnsExistingUser_idempotent() {
        User raceWinner = withId(User.create(AuthProvider.GITHUB, "12345", null, "octocat", null), 11L);
        // 1차 조회: 없음 (둘 다 가입 시도). save 시점에 unique 위반.
        when(userRepository.findByProviderAndProviderId(AuthProvider.GITHUB, "12345"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(raceWinner));
        when(userRepository.existsByNickname("octocat")).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("uk_users_provider_provider_id"));

        User result = service.upsertFromOAuth2(githubInfo(12345L, "octocat", null));

        assertSame(raceWinner, result);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("닉네임 race 로 인한 충돌(재조회로도 못 찾음)은 NICKNAME_ALREADY_TAKEN 으로 매핑한다")
    void concurrentRegistration_nicknameRaceFallsBackToNicknameTaken() {
        when(userRepository.findByProviderAndProviderId(AuthProvider.GITHUB, "12345"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty());
        when(userRepository.existsByNickname("octocat")).thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("uk_users_nickname"));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.upsertFromOAuth2(githubInfo(12345L, "octocat", null)));

        assertEquals(ErrorCode.NICKNAME_ALREADY_TAKEN, ex.getErrorCode());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("닉네임 충돌 시 '#1234' suffix 를 붙여 가입한다")
    void resolvesNicknameCollision_withSuffix() {
        when(userRepository.findByProviderAndProviderId(any(), any())).thenReturn(Optional.empty());
        when(userRepository.existsByNickname("octocat")).thenReturn(true);
        when(userRepository.existsByNickname(org.mockito.ArgumentMatchers.startsWith("octocat#")))
                .thenReturn(false);
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> withId(inv.getArgument(0), 99L));

        User result = service.upsertFromOAuth2(githubInfo(12345L, "octocat", null));

        assertTrue(result.getNickname().startsWith("octocat#"),
                "예상: 'octocat#XXXX', 실제: " + result.getNickname());
        assertEquals("octocat#".length() + 4, result.getNickname().length());
    }

    private OAuth2UserInfo githubInfo(long id, String login, String email) {
        return new GithubOAuth2UserInfo(Map.of(
                "id", id,
                "login", login,
                "email", email == null ? "" : email
        ));
    }

    /** 테스트 픽스처용. 실제 코드에서는 JPA 가 채움. */
    private static User withId(User user, long id) {
        try {
            Field f = User.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return user;
    }
}
