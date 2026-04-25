package org.project.cote.common.security.oauth2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.cote.common.security.jwt.JwtProperties;
import org.project.cote.common.security.jwt.JwtTokenProvider;
import org.project.cote.user.domain.AuthProvider;
import org.project.cote.user.domain.User;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.lang.reflect.Field;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuth2SuccessHandlerTest {

    private static final String SECRET = "test-secret-please-change-this-to-a-long-enough-random-string-min-256-bits";
    private static final String REDIRECT_URI = "http://localhost:3000/oauth/callback";

    private JwtTokenProvider tokenProvider;
    private OAuth2SuccessHandler handler;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
        tokenProvider = new JwtTokenProvider(new JwtProperties(SECRET, 900, "harucote-test"), clock);
        OAuth2Properties props = new OAuth2Properties(REDIRECT_URI);
        handler = new OAuth2SuccessHandler(tokenProvider, props);
    }

    @Test
    @DisplayName("성공 시 access token 을 발급해 fragment 에 담아 redirect_uri 로 redirect 한다")
    void onSuccess_redirectsWithFragmentToken() throws Exception {
        User user = withId(User.create(AuthProvider.GITHUB, "12345", null, "octocat", null), 7L);
        OAuth2UserPrincipal principal = OAuth2UserPrincipal.from(user, Map.of("id", "12345"));
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, auth);

        String location = response.getRedirectedUrl();
        assertTrue(location.startsWith(REDIRECT_URI + "#token="),
                "예상 prefix '" + REDIRECT_URI + "#token=', 실제: " + location);

        String encodedToken = location.substring((REDIRECT_URI + "#token=").length());
        String token = URLDecoder.decode(encodedToken, StandardCharsets.UTF_8);

        JwtTokenProvider.AuthClaims claims = tokenProvider.parse(token);
        assertEquals(7L, claims.userId());
        assertEquals(user.getRole(), claims.role());
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
