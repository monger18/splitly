package com.satyam.splitly.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT settings, bound from {@code splitly.jwt.*}.
 *
 * <p>A record bound with {@code @ConfigurationProperties} rather than a scatter of
 * {@code @Value} fields: the settings arrive as one immutable object, they are validated
 * once at startup, and a typo in a property name is visible here instead of surfacing as
 * a null deep inside the signing code.
 *
 * @param secret          HMAC signing key. Must be at least 32 bytes for HS256.
 * @param issuer          written as the {@code iss} claim and checked on every parse
 * @param accessTokenTtl  short, because an access token cannot be revoked
 * @param refreshTokenTtl long, but only ever sent to the refresh endpoint
 */
@ConfigurationProperties(prefix = "splitly.jwt")
public record JwtProperties(String secret, String issuer, Duration accessTokenTtl, Duration refreshTokenTtl) {
}
