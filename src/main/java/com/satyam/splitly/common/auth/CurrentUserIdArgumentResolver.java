package com.satyam.splitly.common.auth;

import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Supplies {@link CurrentUserId} parameters from the authenticated principal.
 *
 * <p>This class is the seam the whole design was built around. Until phase 4 it read an
 * {@code X-User-Id} header that any caller could set to any value; now it reads the
 * verified subject of a signed JWT. <strong>Not one controller or service changed.</strong>
 * That is the payoff for routing identity through a single annotation from the start,
 * rather than reading the header wherever it was needed.
 */
@Component
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        // The security filter chain has already rejected unauthenticated requests, so this
        // normally cannot fail. It throws rather than returning null so that a
        // misconfigured permitAll on a rule expecting a user surfaces as a 401 instead of
        // a NullPointerException deep in a service.
        return CurrentUser.requireId();
    }
}
