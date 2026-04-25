package org.project.cote.user.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserTest {

    @Test
    @DisplayName("create()는 필수 정보로 새 사용자를 만들고 기본 역할은 USER 이다")
    void create_setsDefaultRoleToUser() {
        User user = User.create(
                AuthProvider.GITHUB,
                "12345",
                "user@example.com",
                "octocat",
                "https://avatars.example.com/octocat.png"
        );

        assertEquals(AuthProvider.GITHUB, user.getProvider());
        assertEquals("12345", user.getProviderId());
        assertEquals("user@example.com", user.getEmail());
        assertEquals("octocat", user.getNickname());
        assertEquals("https://avatars.example.com/octocat.png", user.getProfileImageUrl());
        assertEquals(UserRole.USER, user.getRole());
        assertNull(user.getId(), "영속화 전이므로 id는 null");
    }

    @Test
    @DisplayName("create()는 email 이 null 이어도 허용한다 (GitHub private email 대응)")
    void create_allowsNullEmail() {
        User user = User.create(
                AuthProvider.GITHUB,
                "12345",
                null,
                "octocat",
                null
        );

        assertNotNull(user);
        assertNull(user.getEmail());
        assertNull(user.getProfileImageUrl());
    }

    @Test
    @DisplayName("create()는 provider 가 null 이면 예외를 던진다")
    void create_rejectsNullProvider() {
        assertThrows(IllegalArgumentException.class, () -> User.create(
                null, "12345", "user@example.com", "octocat", null
        ));
    }

    @Test
    @DisplayName("create()는 providerId 가 비어 있으면 예외를 던진다")
    void create_rejectsBlankProviderId() {
        assertThrows(IllegalArgumentException.class, () -> User.create(
                AuthProvider.GITHUB, " ", "user@example.com", "octocat", null
        ));
    }

    @Test
    @DisplayName("create()는 nickname 이 비어 있으면 예외를 던진다")
    void create_rejectsBlankNickname() {
        assertThrows(IllegalArgumentException.class, () -> User.create(
                AuthProvider.GITHUB, "12345", "user@example.com", "  ", null
        ));
    }

    @Test
    @DisplayName("changeNickname()은 닉네임을 변경한다")
    void changeNickname_updatesNickname() {
        User user = User.create(AuthProvider.GITHUB, "12345", null, "octocat", null);

        user.changeNickname("hubot");

        assertEquals("hubot", user.getNickname());
    }

    @Test
    @DisplayName("changeNickname()은 빈 값이면 예외를 던진다")
    void changeNickname_rejectsBlank() {
        User user = User.create(AuthProvider.GITHUB, "12345", null, "octocat", null);

        assertThrows(IllegalArgumentException.class, () -> user.changeNickname(""));
    }
}
