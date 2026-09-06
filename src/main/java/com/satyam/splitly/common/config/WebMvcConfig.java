package com.satyam.splitly.common.config;

import com.satyam.splitly.common.auth.CurrentUserIdArgumentResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers custom controller-parameter resolvers.
 *
 * <p>Implementing {@link WebMvcConfigurer} adds to Spring MVC's defaults. Note that it
 * is not annotated {@code @EnableWebMvc} - that would switch Boot's auto-configuration
 * off entirely and take the JSON converters and error handling with it.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentUserIdArgumentResolver currentUserIdArgumentResolver;

    public WebMvcConfig(CurrentUserIdArgumentResolver currentUserIdArgumentResolver) {
        this.currentUserIdArgumentResolver = currentUserIdArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserIdArgumentResolver);
    }
}
