package org.project.cote.common.security.jwt;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.cote.common.dto.ErrorCode;
import org.project.cote.common.security.AuthenticatedUser;
import org.project.cote.user.domain.UserRole;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-please-change-this-to-a-long-enough-random-string-min-256-bits";
    private static final String ISSUER = "harucote-test";

    private JwtTokenProvider tokenProvider;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2025-01-01T00:00:00Z"), ZoneOffset.UTC);
        JwtProperties props = new JwtProperties(SECRET, 3600, ISSUER);
        tokenProvider = new JwtTokenProvider(props, clock);
        filter = new JwtAuthenticationFilter(tokenProvider);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 SecurityContext 가 비어 있는 채로 다음 필터를 호출한다")
    void noHeader_passesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Bearer 형식이 아니면 무시하고 통과시킨다")
    void nonBearerHeader_passesThrough() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic something");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("유효한 Bearer 토큰이면 SecurityContext 에 AuthenticatedUser 를 설정한다")
    void validToken_setsAuthentication() throws Exception {
        String token = tokenProvider.createAccessToken(42L, UserRole.USER);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertInstanceOf(AbstractAuthenticationToken.class, auth);
        assertInstanceOf(AuthenticatedUser.class, auth.getPrincipal());

        AuthenticatedUser principal = (AuthenticatedUser) auth.getPrincipal();
        assertEquals(42L, principal.userId());
        assertEquals(UserRole.USER, principal.role());
        assertTrue(auth.isAuthenticated());
        assertTrue(auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("잘못된 토큰이면 SecurityContext 는 비어 있고 요청 attribute 에 에러 코드를 기록한 뒤 통과시킨다")
    void invalidToken_recordsErrorAndPasses() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer not-a-valid-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        Object cause = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE);
        assertEquals(ErrorCode.AUTH_INVALID_TOKEN, cause);
        verify(chain, times(1)).doFilter(request, response);
    }
}
