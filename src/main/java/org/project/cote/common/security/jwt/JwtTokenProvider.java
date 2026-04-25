package org.project.cote.common.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.project.cote.common.dto.ErrorCode;
import org.project.cote.common.exception.ApiException;
import org.project.cote.user.domain.UserRole;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";

    private final JwtProperties properties;
    private final Clock clock;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, UserRole role) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plusSeconds(properties.accessTtlSeconds());

        return Jwts.builder()
                .issuer(properties.issuer())
                .subject(String.valueOf(userId))
                .claim(CLAIM_ROLE, role.name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public AuthClaims parse(String token) {
        Claims claims = parseClaims(token);
        validateIssuer(claims);

        Long userId = parseUserId(claims);
        UserRole role = parseRole(claims);
        Instant expiresAt = claims.getExpiration().toInstant();

        return new AuthClaims(userId, role, expiresAt);
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            throw new ApiException(ErrorCode.AUTH_EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(ErrorCode.AUTH_INVALID_TOKEN);
        }
    }

    private void validateIssuer(Claims claims) {
        if (!properties.issuer().equals(claims.getIssuer())) {
            throw new ApiException(ErrorCode.AUTH_INVALID_TOKEN);
        }
    }

    private Long parseUserId(Claims claims) {
        try {
            return Long.parseLong(claims.getSubject());
        } catch (NumberFormatException | NullPointerException e) {
            throw new ApiException(ErrorCode.AUTH_INVALID_TOKEN);
        }
    }

    private UserRole parseRole(Claims claims) {
        String roleName = claims.get(CLAIM_ROLE, String.class);
        if (roleName == null) {
            throw new ApiException(ErrorCode.AUTH_INVALID_TOKEN);
        }
        try {
            return UserRole.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.AUTH_INVALID_TOKEN);
        }
    }

    public record AuthClaims(Long userId, UserRole role, Instant expiresAt) {
    }
}
