package com.satyam.splitly.common.auth;

import com.satyam.splitly.auth.AuthenticatedUser;
import com.satyam.splitly.common.exception.UnauthorizedException;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Reads the calling user out of Spring Security's context.
 *
 * <p>The single place that touches {@code SecurityContextHolder}. Keeping it here means
 * services and controllers never reach into thread-local security state, and the day
 * authentication changes shape there is one method to rewrite.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<Long> id() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return authentication.getPrincipal() instanceof AuthenticatedUser user
                ? Optional.of(user.id())
                : Optional.empty();
    }

    public static Long requireId() {
        return id().orElseThrow(() -> new UnauthorizedException("Authentication required"));
    }
}
