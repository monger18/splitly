package com.satyam.splitly.common.auth;

import com.satyam.splitly.common.exception.UnauthorizedException;
import com.satyam.splitly.user.UserRepository;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@link CurrentUserId} parameters from the {@code X-User-Id} header.
 *
 * <p><strong>Temporary, and deliberately not security.</strong> The header is trusted
 * blindly, so anyone can act as anyone. It exists so the domain work in phases 1 to 3
 * can proceed without authentication, and it is replaced in phase 4.
 *
 * <p>The id is checked against the database rather than merely parsed, so a caller
 * cannot pass an id that does not exist and have it surface much later as a confusing
 * foreign key error.
 */
@Component
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    static final String HEADER = "X-User-Id";

    private final UserRepository userRepository;

    public CurrentUserIdArgumentResolver(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        String raw = webRequest.getHeader(HEADER);
        if (raw == null || raw.isBlank()) {
            throw new UnauthorizedException("Missing " + HEADER + " header");
        }

        long userId;
        try {
            userId = Long.parseLong(raw.trim());
        } catch (NumberFormatException ex) {
            throw new UnauthorizedException(HEADER + " must be a number");
        }

        if (!userRepository.existsById(userId)) {
            throw new UnauthorizedException("No user with id " + userId);
        }
        return userId;
    }
}
