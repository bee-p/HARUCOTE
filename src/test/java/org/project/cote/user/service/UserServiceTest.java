package org.project.cote.user.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.cote.common.dto.ErrorCode;
import org.project.cote.common.exception.ApiException;
import org.project.cote.user.domain.AuthProvider;
import org.project.cote.user.domain.User;
import org.project.cote.user.dto.UserResponse;
import org.project.cote.user.repository.UserRepository;

import java.lang.reflect.Field;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository userRepository;
    private UserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new UserService(userRepository);
    }

    @Test
    @DisplayName("getMe 는 사용자 정보를 UserResponse 로 반환한다")
    void getMe_returnsUserResponse() {
        User user = withId(User.create(AuthProvider.GITHUB, "12345", "u@e.com", "octocat", "img"), 7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        UserResponse response = service.getMe(7L);

        assertEquals(7L, response.id());
        assertEquals("octocat", response.nickname());
        assertEquals("u@e.com", response.email());
        assertEquals(AuthProvider.GITHUB, response.provider());
    }

    @Test
    @DisplayName("getMe 는 사용자가 없으면 USER_NOT_FOUND 예외를 던진다")
    void getMe_throwsWhenNotFound() {
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.getMe(7L));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("changeNickname 은 닉네임을 변경하고 변경된 사용자 정보를 반환한다")
    void changeNickname_updatesAndReturns() {
        User user = withId(User.create(AuthProvider.GITHUB, "12345", null, "octocat", null), 7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("hubot")).thenReturn(false);

        UserResponse response = service.changeNickname(7L, "hubot");

        assertEquals("hubot", response.nickname());
        assertEquals("hubot", user.getNickname());
    }

    @Test
    @DisplayName("changeNickname 은 동일 닉네임을 입력하면 변경 없이 통과한다")
    void changeNickname_noopWhenSame() {
        User user = withId(User.create(AuthProvider.GITHUB, "12345", null, "octocat", null), 7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        UserResponse response = service.changeNickname(7L, "octocat");

        assertEquals("octocat", response.nickname());
        verify(userRepository, never()).existsByNickname(any());
    }

    @Test
    @DisplayName("changeNickname 은 이미 사용 중인 닉네임이면 NICKNAME_ALREADY_TAKEN 예외를 던진다")
    void changeNickname_throwsWhenTaken() {
        User user = withId(User.create(AuthProvider.GITHUB, "12345", null, "octocat", null), 7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("hubot")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> service.changeNickname(7L, "hubot"));
        assertEquals(ErrorCode.NICKNAME_ALREADY_TAKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("changeNickname 은 사용자가 없으면 USER_NOT_FOUND 예외를 던진다")
    void changeNickname_throwsWhenUserNotFound() {
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.changeNickname(7L, "hubot"));
        assertEquals(ErrorCode.USER_NOT_FOUND, ex.getErrorCode());
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
