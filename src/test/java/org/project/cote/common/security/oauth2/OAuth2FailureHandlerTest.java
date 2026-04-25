package org.project.cote.common.security.oauth2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.AuthenticationException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OAuth2FailureHandlerTest {

    private static final String REDIRECT_URI = "http://localhost:3000/oauth/callback";

    @Test
    @DisplayName("실패 시 fragment 에 error=oauth2_failed 를 담아 redirect_uri 로 redirect 한다")
    void onFailure_redirectsWithErrorFragment() throws Exception {
        OAuth2FailureHandler handler = new OAuth2FailureHandler(new OAuth2Properties(REDIRECT_URI));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthenticationException ex = new AuthenticationException("OAuth2 거부됨") {
        };

        handler.onAuthenticationFailure(request, response, ex);

        String location = response.getRedirectedUrl();
        assertNotNull(location);
        assertTrue(location.startsWith(REDIRECT_URI + "#error="),
                "예상 prefix '" + REDIRECT_URI + "#error=', 실제: " + location);
        assertTrue(location.contains("oauth2_failed"));
    }
}
