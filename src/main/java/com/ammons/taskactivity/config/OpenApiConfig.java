package com.ammons.taskactivity.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

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

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Value("${swagger.server.url:}")
    private String serverUrl;

    @Bean
    public OpenAPI customOpenAPI() {
            OpenAPI openAPI =
                            new OpenAPI()
                .info(new Info().title("Task Activity Management API").version("1.0.0").description(
                        """
                                REST API for managing task activities, projects, clients, and users.

                                ## Authentication
                                This API uses JWT (JSON Web Token) for authentication. To use protected endpoints:

                                1. Call POST /api/auth/login with your credentials to get an access token
                                2. Click the 'Authorize' button (🔒) at the top of this page
                                3. Enter your token in the format: `Bearer <your-token>`
                                4. Click 'Authorize' and close the dialog
                                5. Now you can use 'Try it out' on any endpoint

                                The token will be automatically included in all subsequent requests."""))
                .components(new Components().addSecuritySchemes(SECURITY_SCHEME_NAME,
                        new SecurityScheme().name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                                .description("Enter your JWT token obtained from /api/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));

        // Add server URL if configured (useful for AWS deployments)
        if (serverUrl != null && !serverUrl.isEmpty()) {
                List<Server> servers = new ArrayList<>();
                servers.add(new Server().url(serverUrl).description("Application Server"));
                openAPI.servers(servers);
        }

        return openAPI;
    }
}
