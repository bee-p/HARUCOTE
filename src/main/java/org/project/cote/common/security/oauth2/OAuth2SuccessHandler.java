package org.project.cote.common.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.project.cote.common.dto.ErrorCode;
import org.project.cote.common.exception.ApiException;
import org.project.cote.common.security.jwt.JwtTokenProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * OAuth2 인증 성공 시 자체 JWT 를 발급해 redirect URI 의 fragment 에 담아 클라이언트로 보낸다.
 * Fragment(#) 는 서버 액세스 로그/Referer 에 남지 않으므로 query string 보다 안전.
 * 클라이언트는 location.hash 에서 토큰을 추출 후 history.replaceState 로 즉시 정리해야 한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider tokenProvider;
    private final OAuth2Properties oauth2Properties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2UserPrincipal principal = extractPrincipal(authentication);

        String token = tokenProvider.createAccessToken(principal.userId(), principal.role());
        String target = buildRedirectTarget(token);

        // target 변수에는 토큰 fragment 가 포함되어 있다. 절대 로그에 출력하지 말 것.
        log.info("OAuth2 로그인 성공: userId={}", principal.userId());
        getRedirectStrategy().sendRedirect(request, response, target);
    }

    private OAuth2UserPrincipal extractPrincipal(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2UserPrincipal principal) {
            return principal;
        }
        throw new ApiException(ErrorCode.OAUTH2_PROVIDER_ERROR,
                "OAuth2 인증 결과의 principal 형태가 예상과 다릅니다.");
    }

    private String buildRedirectTarget(String token) {
        String base = UriComponentsBuilder
                .fromUriString(oauth2Properties.redirectUri())
                .build()
                .toUriString();
        return base + "#token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
    }
}
