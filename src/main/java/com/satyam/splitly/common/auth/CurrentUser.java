package com.satyam.splitly.common.auth;

import com.satyam.splitly.auth.AuthenticatedUser;
import com.satyam.splitly.common.exception.UnauthorizedException;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

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
