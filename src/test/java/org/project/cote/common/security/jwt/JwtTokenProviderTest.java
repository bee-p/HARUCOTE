package org.project.cote.common.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.project.cote.common.dto.ErrorCode;
import org.project.cote.common.exception.ApiException;
import org.project.cote.user.domain.UserRole;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-please-change-this-to-a-long-enough-random-string-min-256-bits";
    private static final String ISSUER = "harucote-test";
    private static final long TTL_SECONDS = 3600;

    private MutableClock clock;
    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        JwtProperties props = new JwtProperties(SECRET, TTL_SECONDS, ISSUER);
        provider = new JwtTokenProvider(props, clock);
    }

    @Test
    @DisplayName("createAccessToken 으로 발급한 토큰을 다시 파싱하면 userId/role 이 일치한다")
    void createAndParse_returnsOriginalClaims() {
        String token = provider.createAccessToken(42L, UserRole.USER);

        JwtTokenProvider.AuthClaims claims = provider.parse(token);

        assertEquals(42L, claims.userId());
        assertEquals(UserRole.USER, claims.role());
    }

    @Test
    @DisplayName("만료된 토큰은 AUTH_EXPIRED_TOKEN 예외를 던진다")
    void parse_expiredToken_throws() {
        String token = provider.createAccessToken(42L, UserRole.USER);

        clock.advance(Duration.ofSeconds(TTL_SECONDS + 10));

        ApiException ex = assertThrows(ApiException.class, () -> provider.parse(token));
        assertEquals(ErrorCode.AUTH_EXPIRED_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("서명이 위조된 토큰은 AUTH_INVALID_TOKEN 예외를 던진다")
    void parse_forgedSignature_throws() {
        String token = provider.createAccessToken(42L, UserRole.USER);

        String forged = token.substring(0, token.length() - 4) + "AAAA";

        ApiException ex = assertThrows(ApiException.class, () -> provider.parse(forged));
        assertEquals(ErrorCode.AUTH_INVALID_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("형식이 잘못된 토큰은 AUTH_INVALID_TOKEN 예외를 던진다")
    void parse_malformedToken_throws() {
        ApiException ex = assertThrows(ApiException.class, () -> provider.parse("not-a-jwt"));
        assertEquals(ErrorCode.AUTH_INVALID_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("다른 secret 으로 발급된 토큰은 AUTH_INVALID_TOKEN 예외를 던진다")
    void parse_tokenSignedWithDifferentSecret_throws() {
        JwtProperties otherProps = new JwtProperties(
                "another-secret-please-change-this-to-a-long-enough-random-string-min-256-bits",
                TTL_SECONDS,
                ISSUER
        );
        JwtTokenProvider other = new JwtTokenProvider(otherProps, clock);
        String foreignToken = other.createAccessToken(42L, UserRole.USER);

        ApiException ex = assertThrows(ApiException.class, () -> provider.parse(foreignToken));
        assertEquals(ErrorCode.AUTH_INVALID_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("issuer 가 다른 토큰은 AUTH_INVALID_TOKEN 예외를 던진다")
    void parse_wrongIssuer_throws() {
        JwtProperties otherIssuerProps = new JwtProperties(SECRET, TTL_SECONDS, "different-issuer");
        JwtTokenProvider otherIssuer = new JwtTokenProvider(otherIssuerProps, clock);
        String tokenWithWrongIssuer = otherIssuer.createAccessToken(42L, UserRole.USER);

        ApiException ex = assertThrows(ApiException.class, () -> provider.parse(tokenWithWrongIssuer));
        assertEquals(ErrorCode.AUTH_INVALID_TOKEN, ex.getErrorCode());
    }

    @Test
    @DisplayName("ADMIN 역할이 토큰에 그대로 보존된다")
    void createAccessToken_preservesAdminRole() {
        String token = provider.createAccessToken(99L, UserRole.ADMIN);
        JwtTokenProvider.AuthClaims claims = provider.parse(token);
        assertEquals(UserRole.ADMIN, claims.role());
    }

    @Test
    @DisplayName("토큰의 만료 시각은 발급 시각 + TTL 이다")
    void createAccessToken_expiryEqualsIssuedAtPlusTtl() {
        Instant issuedAt = clock.instant();
        String token = provider.createAccessToken(1L, UserRole.USER);

        JwtTokenProvider.AuthClaims claims = provider.parse(token);

        long expectedExpiryEpoch = issuedAt.plusSeconds(TTL_SECONDS).getEpochSecond();
        assertEquals(expectedExpiryEpoch, claims.expiresAt().getEpochSecond());
        assertTrue(claims.expiresAt().isAfter(issuedAt));
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant initial) {
            this.now = initial;
        }

        void advance(Duration d) {
            now = now.plus(d);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
