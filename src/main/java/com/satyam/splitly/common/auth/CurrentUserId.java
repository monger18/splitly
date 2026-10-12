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
 * {@link CurrentUserIdArgumentResolver} supplies the value from the verified subject of
 * the request's JWT. It previously read an {@code X-User-Id} header instead; swapping to
 * real authentication changed that one resolver and nothing else.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {
}
