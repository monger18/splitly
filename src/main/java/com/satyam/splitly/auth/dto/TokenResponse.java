package com.satyam.splitly.auth.dto;

/**
 * @param tokenType        always "Bearer" - the scheme to put in the Authorization header
 * @param expiresInSeconds lifetime of the access token, so a client can refresh before it
 *                         expires instead of waiting for a 401
 */
public record TokenResponse(String accessToken, String refreshToken, String tokenType, long expiresInSeconds) {

    public static TokenResponse of(String accessToken, String refreshToken, long expiresInSeconds) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", expiresInSeconds);
    }
}
