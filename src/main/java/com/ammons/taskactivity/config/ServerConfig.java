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
 * @since January 2026
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
     * CORS Configuration Note: CORS is configured in SecurityConfig.java for better security
     * integration. The SecurityConfig CORS includes: - Production wildcard validation (fails fast
     * if misconfigured) - Explicit origin lists for production - Credentials support - All HTTP
     * methods and headers
     * 
     * This avoids duplicate CORS configuration and potential conflicts.
     */

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
