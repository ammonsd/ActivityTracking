package com.ammons.taskactivity.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import java.io.IOException;

/**
 * ServerConfig - Configuration for the server.
 *
 * @author Dean Ammons
 * @version 1.0
 */
@Configuration
public class ServerConfig implements WebMvcConfigurer {

    /**
     * CORS allowed origins - configurable via environment variable for production deployment.
     * Defaults to localhost ports for development.
     * 
     * For local network access, set environment variable:
     * CORS_ALLOWED_ORIGINS=http://localhost:4200,http://localhost:3000,http://YOUR_IP:8080
     * 
     * For AWS deployment, set environment variable:
     * CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
     */
    @Value("${cors.allowed-origins:http://localhost:4200,http://localhost:3000,http://localhost:8080}")
    private String allowedOrigins;

    /**
     * Configures Cross-Origin Resource Sharing (CORS) for the application. CORS allows the backend
     * API to accept requests from frontend applications running on different domains/ports.
     * 
     * Environment-Based Configuration: - Development: Uses localhost origins (4200 for Angular,
     * 3000 for React, 8080 for local) - Local Network: Set CORS_ALLOWED_ORIGINS with your current
     * IP (e.g., http://192.168.12.179:8080) - Production (AWS): Set CORS_ALLOWED_ORIGINS
     * environment variable with production domains
     * 
     * Example local network:
     * CORS_ALLOWED_ORIGINS=http://localhost:4200,http://localhost:3000,http://192.168.12.179:8080
     * 
     * Example AWS deployment:
     * CORS_ALLOWED_ORIGINS=https://app.yourdomain.com,https://api.yourdomain.com
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Split comma-separated origins from environment variable
        String[] origins = allowedOrigins.split(",");

        registry.addMapping("/api/**") // Apply CORS to all /api/** endpoints
                // Allowed Origins: Configurable via environment variable
                // Development defaults:
                // - http://localhost:4200: Angular development server (default port)
                // - http://localhost:3000: React development server (default port)
                // - http://localhost:8080: Local network access
                // Local Network: Add your current IP via CORS_ALLOWED_ORIGINS environment variable
                // Production: Set via CORS_ALLOWED_ORIGINS environment variable
                .allowedOrigins(origins)

                // Allowed HTTP methods that can be used in CORS requests
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")

                // Allowed headers: "*" means any header sent by the client is accepted
                .allowedHeaders("*")

                // Allow credentials (cookies, authorization headers) to be sent
                // Required for authentication/session management
                .allowCredentials(true)

                // Cache preflight response for 3600 seconds (1 hour)
                // Reduces preflight OPTIONS requests for better performance
                .maxAge(3600);
    }

    /**
     * Configure resource handlers for static content including Angular SPA. Routes /app/** to serve
     * Angular app with fallback to index.html for client-side routing.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/app/**")
                .addResourceLocations("classpath:/static/app/").resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location)
                            throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        // If resource exists, return it (JS, CSS, etc.)
                        if (requestedResource.exists() && requestedResource.isReadable()) {
                            return requestedResource;
                        }
                        // Otherwise, return index.html for Angular routing
                        return new ClassPathResource("/static/app/index.html");
                    }
                });
    }

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> containerCustomizer() {
        return factory -> {
            // Port is configured via server.port in application.properties
            // Don't override it here to allow environment-specific configuration
            factory.setContextPath("");
            factory.addConnectorCustomizers(connector -> {
                connector.setProperty("maxThreads", "200");
                connector.setProperty("minSpareThreads", "10");
                connector.setProperty("connectionTimeout", "20000");
            });
        };
    }
}