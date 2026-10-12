package com.satyam.splitly.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.satyam.splitly.common.exception.UnauthorizedException;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Plain unit tests - no Spring context and no database. {@link JwtService} depends only on
 * a properties record, so it can be built with {@code new} and exercised in milliseconds.
 */
class JwtServiceTest {

    private static final String SECRET = "test-only-signing-key-that-is-long-enough-32";

    private static JwtService service(Duration accessTtl, Duration refreshTtl) {
        return new JwtService(new JwtProperties(SECRET, "splitly", accessTtl, refreshTtl));
    }

    private static JwtService service() {
        return service(Duration.ofMinutes(15), Duration.ofDays(30));
    }

    @Test
    @DisplayName("an access token round-trips to the same user id")
    void access_token_round_trips() {
        JwtService jwt = service();
        String token = jwt.createAccessToken(42L);

        assertThat(jwt.parseUserId(token, TokenType.ACCESS)).isEqualTo(42L);
    }

    @Test
    @DisplayName("a refresh token is rejected where an access token is required")
    void refresh_token_is_not_an_access_token() {
        JwtService jwt = service();
        String refresh = jwt.createRefreshToken(42L);

        // Both are signed with the same key and name the same subject. Only the typ claim
        // separates them, so if this passes the short access-token lifetime is meaningless.
        assertThat(jwt.parseUserId(refresh, TokenType.REFRESH)).isEqualTo(42L);
        assertThatThrownBy(() -> jwt.parseUserId(refresh, TokenType.ACCESS))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("an expired token is rejected")
    void expired_token_is_rejected() {
        // A negative TTL puts the expiry in the past, so no waiting and no flaky test.
        JwtService jwt = service(Duration.ofMinutes(-1), Duration.ofDays(30));
        String token = jwt.createAccessToken(7L);

        assertThatThrownBy(() -> jwt.parseUserId(token, TokenType.ACCESS))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("a token signed with a different key is rejected")
    void forged_signature_is_rejected() {
        String token = service().createAccessToken(1L);

        JwtService other = new JwtService(new JwtProperties(
                "a-completely-different-signing-key-also-32", "splitly",
                Duration.ofMinutes(15), Duration.ofDays(30)));

        assertThatThrownBy(() -> other.parseUserId(token, TokenType.ACCESS))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("a token from a different issuer is rejected")
    void wrong_issuer_is_rejected() {
        String token = new JwtService(new JwtProperties(SECRET, "somebody-else",
                Duration.ofMinutes(15), Duration.ofDays(30))).createAccessToken(1L);

        assertThatThrownBy(() -> service().parseUserId(token, TokenType.ACCESS))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("garbage is rejected rather than crashing")
    void malformed_token_is_rejected() {
        JwtService jwt = service();
        for (String bad : new String[] {"", "not-a-token", "a.b.c", "Bearer x"}) {
            assertThatThrownBy(() -> jwt.parseUserId(bad, TokenType.ACCESS))
                    .as("input %s", bad)
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Test
    @DisplayName("a short signing key fails at construction, not on first login")
    void short_secret_fails_fast() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties(
                "too-short", "splitly", Duration.ofMinutes(15), Duration.ofDays(30))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }

    @Test
    @DisplayName("the payload is readable by anyone - a JWT is signed, not encrypted")
    void payload_is_not_secret() {
        String token = service().createAccessToken(99L);
        String payload = new String(java.util.Base64.getUrlDecoder()
                .decode(token.split("\\.")[1]));

        // Documents the property rather than asserting a nicety: nothing confidential may
        // ever go into a claim.
        assertThat(payload).contains("\"sub\":\"99\"").contains("splitly");
    }
}
