package com.ammons.taskactivity.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI Configuration for Swagger UI
 * 
 * Provides API documentation for the Task Activity Management API. Access Swagger UI at:
 * /swagger-ui.html or /swagger-ui/index.html Access OpenAPI JSON at: /v3/api-docs
 * 
 * @author Dean Ammons
 * @version 1.0
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Task Activity Management API").version("1.0.0").description(
                        "REST API for managing task activities, projects, clients, and users."));
    }
}
