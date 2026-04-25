package org.project.cote.common.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long accessTtlSeconds,
        String issuer
) {
    /** HS256 권장 최소 키 길이 (bit). */
    private static final int MIN_SECRET_BYTES = 32;

    public JwtProperties {
        Objects.requireNonNull(secret, "app.jwt.secret 이 설정되지 않았습니다.");
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "app.jwt.secret 은 최소 " + MIN_SECRET_BYTES + " bytes (256 bit) 이상이어야 합니다.");
        }
        Objects.requireNonNull(issuer, "app.jwt.issuer 가 설정되지 않았습니다.");
        if (accessTtlSeconds <= 0) {
            throw new IllegalArgumentException(
                    "app.jwt.access-ttl-seconds 는 양수여야 합니다 (현재: " + accessTtlSeconds + ").");
        }
    }
}
