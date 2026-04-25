package org.project.cote.common.security.oauth2;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.Objects;
import java.util.Set;

/**
 * OAuth2 후속 redirect 정책. 부팅 시 redirect-uri 의 스킴/호스트를 검증해
 * 환경변수 오설정으로 토큰이 외부 도메인으로 새는 사고를 막는다.
 */
@ConfigurationProperties(prefix = "app.oauth2")
public record OAuth2Properties(String redirectUri) {

    private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]", "::1");

    public OAuth2Properties {
        Objects.requireNonNull(redirectUri, "app.oauth2.redirect-uri 가 설정되지 않았습니다.");
        URI uri = parse(redirectUri);
        validateScheme(uri, redirectUri);
    }

    private static URI parse(String value) {
        try {
            return URI.create(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("app.oauth2.redirect-uri 형식이 잘못되었습니다: " + value, e);
        }
    }

    private static void validateScheme(URI uri, String original) {
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if ("https".equalsIgnoreCase(scheme)) {
            return;
        }
        if ("http".equalsIgnoreCase(scheme) && host != null && LOOPBACK_HOSTS.contains(host.toLowerCase())) {
            return;
        }
        throw new IllegalArgumentException(
                "app.oauth2.redirect-uri 는 https 이거나 loopback(localhost/127.0.0.1) 의 http 만 허용됩니다: "
                        + original
        );
    }
}
