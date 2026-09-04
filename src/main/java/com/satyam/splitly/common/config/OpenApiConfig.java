package com.satyam.splitly.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Describes the API for springdoc, which reads the controllers at startup and
 * serves generated docs at /swagger-ui.html - a browsable client you can send
 * real requests from, without writing a front end.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI splitlyOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Splitly API")
                .version("v1")
                .description("Shared expenses: groups, expenses, splits, balances and settlements."));
    }
}
