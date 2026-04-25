package org.project.cote.common.security.oauth2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.project.cote.user.domain.AuthProvider;
import org.project.cote.user.domain.User;
import org.project.cote.user.domain.UserRole;
import org.project.cote.user.service.UserRegistrationService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomOAuth2UserServiceTest {

    @Test
    @DisplayName("loadUser 는 GitHub attributes 를 OAuth2UserInfo 로 변환해 등록 서비스에 위임한다")
    void loadUser_delegatesToRegistrationService() {
        UserRegistrationService registrationService = mock(UserRegistrationService.class);
        Map<String, Object> attributes = Map.of(
                "id", 12345L,
                "login", "octocat",
                "email", "u@e.com",
                "avatar_url", "https://avatar"
        );
        OAuth2User upstream = new DefaultOAuth2User(
                List.of(), attributes, "id"
        );

        TestableCustomOAuth2UserService service = new TestableCustomOAuth2UserService(registrationService, upstream);

        User savedUser = withId(User.create(AuthProvider.GITHUB, "12345", "u@e.com", "octocat", "https://avatar"), 7L);
        when(registrationService.upsertFromOAuth2(org.mockito.ArgumentMatchers.any())).thenReturn(savedUser);

        OAuth2User result = service.loadUser(buildRequest("github"));

        ArgumentCaptor<OAuth2UserInfo> captor = ArgumentCaptor.forClass(OAuth2UserInfo.class);
        verify(registrationService).upsertFromOAuth2(captor.capture());
        OAuth2UserInfo passed = captor.getValue();
        assertEquals(AuthProvider.GITHUB, passed.provider());
        assertEquals("12345", passed.providerId());
        assertEquals("octocat", passed.nickname());

        assertInstanceOf(OAuth2UserPrincipal.class, result);
        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) result;
        assertEquals(savedUser.getId(), principal.userId());
        assertEquals(savedUser.getRole(), principal.role());
        assertEquals(savedUser.getNickname(), principal.nickname());
        assertEquals(attributes, principal.getAttributes());
        assertEquals("7", principal.getName());
        assertEquals(1, principal.getAuthorities().size());
        assertEquals("ROLE_USER", principal.getAuthorities().iterator().next().getAuthority());
    }

    private OAuth2UserRequest buildRequest(String registrationId) {
        ClientRegistration registration = ClientRegistration.withRegistrationId(registrationId)
                .clientId("test")
                .clientSecret("test")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost/{action}/oauth2/code/{registrationId}")
                .authorizationUri("http://example.com/auth")
                .tokenUri("http://example.com/token")
                .userInfoUri("http://example.com/user")
                .userNameAttributeName("id")
                .build();
        OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "tok", Instant.now(), Instant.now().plusSeconds(60),
                Set.of("read:user")
        );
        return new OAuth2UserRequest(registration, token);
    }

    /** 상위 DefaultOAuth2UserService 의 외부 호출을 우회하기 위한 테스트용 서브타입. */
    private static class TestableCustomOAuth2UserService extends CustomOAuth2UserService {
        private final OAuth2User stub;

        TestableCustomOAuth2UserService(UserRegistrationService userRegistrationService, OAuth2User stub) {
            super(userRegistrationService);
            this.stub = stub;
        }

        @Override
        protected OAuth2User loadFromProvider(OAuth2UserRequest userRequest) {
            return stub;
        }
    }

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
