package com.ammons.taskactivity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for CORS settings.
 * 
 * @author Dean Ammons
 * @version 1.0
 */
@Component
@ConfigurationProperties(prefix = "cors")
public class CorsConfig {

    /**
     * Allowed origins for CORS requests.
     */
    private String allowedOrigins = "https://yourdomain.com";

    /**
     * Allowed HTTP methods for CORS requests.
     */
    private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS";

    /**
     * Allowed headers for CORS requests.
     */
    private String allowedHeaders = "*";

    /**
     * Whether credentials are allowed in CORS requests.
     */
    private boolean allowCredentials = true;

    public String getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(String allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public String getAllowedMethods() {
        return allowedMethods;
    }

    public void setAllowedMethods(String allowedMethods) {
        this.allowedMethods = allowedMethods;
    }

    public String getAllowedHeaders() {
        return allowedHeaders;
    }

    public void setAllowedHeaders(String allowedHeaders) {
        this.allowedHeaders = allowedHeaders;
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }
}
