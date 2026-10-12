package com.satyam.splitly.auth;

/**
 * Distinguishes the two kinds of token, written into a custom {@code typ} claim.
 *
 * <p>Without this, a refresh token would be accepted as an access token. It is signed
 * with the same key and carries the same subject, so nothing else tells them apart - and
 * since refresh tokens live for a month, that would silently undo the short access-token
 * lifetime the design depends on.
 */
public enum TokenType {
    ACCESS,
    REFRESH
}
