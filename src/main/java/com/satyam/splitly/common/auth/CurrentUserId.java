package com.satyam.splitly.common.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the id of the calling user into a controller method parameter.
 *
 * <pre>
 * public GroupResponse create(&#64;CurrentUserId Long userId, ...)
 * </pre>
 *
 * <p>This annotation is the seam that keeps authentication out of every controller.
 * Right now {@link CurrentUserIdArgumentResolver} reads the id from an
 * {@code X-User-Id} header, which is a stand-in with no security whatsoever - any
 * caller can claim to be any user. In phase 4 that resolver starts reading the
 * authenticated principal from the JWT instead, and not one controller changes.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {
}
