package org.project.cote.common.security.oauth2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.cote.common.dto.ErrorCode;
import org.project.cote.common.exception.ApiException;
import org.project.cote.user.domain.AuthProvider;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GithubOAuth2UserInfoTest {

    @Test
    @DisplayName("GitHub attributes 에서 provider/id/email/nickname/image 를 추출한다")
    void extractsAllAttributes() {
        Map<String, Object> attrs = Map.of(
                "id", 12345L,
                "login", "octocat",
                "email", "octo@example.com",
                "avatar_url", "https://avatars.example.com/octocat.png"
        );

        OAuth2UserInfo info = OAuth2UserInfo.of("github", attrs);

        assertEquals(AuthProvider.GITHUB, info.provider());
        assertEquals("12345", info.providerId());
        assertEquals("octo@example.com", info.email());
        assertEquals("octocat", info.nickname());
        assertEquals("https://avatars.example.com/octocat.png", info.profileImageUrl());
    }

    @Test
    @DisplayName("email 이 private 으로 누락되어도 정상 동작한다 (email = null)")
    void allowsNullEmail() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("id", 12345L);
        attrs.put("login", "octocat");
        attrs.put("email", null);
        attrs.put("avatar_url", "https://avatars.example.com/octocat.png");

        OAuth2UserInfo info = OAuth2UserInfo.of("github", attrs);

        assertNull(info.email());
        assertEquals("octocat", info.nickname());
    }

    @Test
    @DisplayName("id 가 정수형(Integer)인 경우에도 String 으로 변환한다")
    void coercesIntegerIdToString() {
        Map<String, Object> attrs = Map.of(
                "id", 67890,
                "login", "hubot"
        );

        OAuth2UserInfo info = OAuth2UserInfo.of("github", attrs);

        assertEquals("67890", info.providerId());
    }

    @Test
    @DisplayName("지원하지 않는 provider 는 OAUTH2_PROVIDER_ERROR 예외를 던진다")
    void unsupportedProvider_throws() {
        ApiException ex = assertThrows(ApiException.class, () ->
                OAuth2UserInfo.of("twitter", Map.of("id", 1)));
        assertEquals(ErrorCode.OAUTH2_PROVIDER_ERROR, ex.getErrorCode());
    }
}
