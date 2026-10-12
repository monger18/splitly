package com.satyam.splitly.auth;

/**
 * The principal held in the security context once a request is authenticated.
 *
 * <p>Carries the user id so that nothing downstream has to look the user up by email on
 * every request. {@link com.satyam.splitly.common.auth.CurrentUserIdArgumentResolver}
 * reads the id straight off this object.
 */
public record AuthenticatedUser(Long id, String email) {
}
