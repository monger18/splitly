package com.satyam.splitly.auth;

import com.satyam.splitly.common.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Creates and verifies the tokens.
 *
 * <p>Signed with HS256, a symmetric algorithm: the same secret signs and verifies. That
 * suits a single application which issues and consumes its own tokens. If a separate
 * service had to verify them, RS256 would be the choice, so the private key never leaves
 * the issuer.
 *
 * <p>The algorithm is named explicitly. Left to itself, jjwt picks the strongest HMAC
 * variant the key length allows - a 48-byte secret quietly produces HS384 instead - so the
 * signing algorithm would depend on how long somebody made the secret. Pinning it means
 * the code and the tokens agree.
 *
 * <p>A JWT is signed, <strong>not encrypted</strong>. Anyone holding one can read its
 * claims. The signature only proves the claims were not altered, so nothing secret goes
 * inside - the subject is a user id, and nothing else identifying.
 */
@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;

        byte[] keyBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            // Fail at startup rather than on the first login. HS256 with a short key is
            // brute-forceable, and a weak signing key means anyone can mint valid tokens.
            throw new IllegalStateException(
                    "splitly.jwt.secret must be at least 32 bytes for HS256 (got " + keyBytes.length + ")");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(Long userId) {
        return create(userId, TokenType.ACCESS, properties.accessTokenTtl());
    }

    public String createRefreshToken(Long userId) {
        return create(userId, TokenType.REFRESH, properties.refreshTokenTtl());
    }

    public Duration accessTokenTtl() {
        return properties.accessTokenTtl();
    }

    private String create(Long userId, TokenType type, Duration ttl) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(properties.issuer())
                .claim("typ", type.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Verifies a token and returns the user id it identifies.
     *
     * <p>Every failure is reported the same way, with no detail about which check failed.
     * Telling a caller whether a token was expired, forged, or the wrong type helps an
     * attacker and helps nobody else.
     *
     * @param expected the kind of token this call site accepts
     * @throws UnauthorizedException if the signature, issuer, expiry or type is wrong
     */
    public Long parseUserId(String token, TokenType expected) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)   // verifies signature and expiry
                    .getPayload();

            if (!expected.name().equals(claims.get("typ", String.class))) {
                throw new UnauthorizedException("Invalid token");
            }
            return Long.valueOf(claims.getSubject());

        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("Invalid token");
        }
    }
}
