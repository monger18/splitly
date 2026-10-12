package com.satyam.splitly.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Describes the API for springdoc, which reads the controllers at startup and serves
 * generated docs at /swagger-ui.html - a browsable client you can send real requests from,
 * without writing a front end.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    /**
     * Declaring the bearer scheme puts an Authorize button in Swagger UI: paste an access
     * token once and every request carries it. Without it, trying a protected endpoint from
     * the browser means hand-crafting the Authorization header on each call.
     */
    @Bean
    public OpenAPI splitlyOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Splitly API")
                        .version("v1")
                        .description("Shared expenses: groups, expenses, splits, balances and settlements. "
                                + "Call POST /api/v1/auth/register or /api/v1/auth/login, then paste the "
                                + "accessToken into Authorize. Every endpoint outside /api/v1/auth needs it."))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Access token from /api/v1/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
