package com.ammons.taskactivity.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter to prevent brute force attacks on authentication endpoints. Implements
 * per-IP rate limiting using Bucket4j token bucket algorithm.
 * 
 * Rate limits are configurable via application.properties: - security.rate-limit.enabled:
 * Enable/disable rate limiting (default: true) - security.rate-limit.capacity: Number of requests
 * allowed per time window (default: 5) - security.rate-limit.refill-minutes: Time window in minutes
 * (default: 1)
 * 
 * @author Dean Ammons
 * @version 1.0
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    @Value("${security.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${security.rate-limit.capacity:5}")
    private int capacity;

    @Value("${security.rate-limit.refill-minutes:1}")
    private int refillMinutes;

    // Store buckets per IP address
    private final Map<String, Bucket> cache = new ConcurrentHashMap();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();

        // Only apply rate limiting to authentication endpoints if enabled
        if (rateLimitEnabled && shouldApplyRateLimit(requestUri)) {
            String clientIp = getClientIp(request);
            Bucket bucket = resolveBucket(clientIp);

            if (bucket.tryConsume(1)) {
                // Request allowed
                filterChain.doFilter(request, response);
            } else {
                // Rate limit exceeded
                logger.warn("Rate limit exceeded for IP: {} on endpoint: {}", clientIp, requestUri);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}");
            }
        } else {
            // No rate limiting for other endpoints
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Determine if rate limiting should be applied to this endpoint
     */
    private boolean shouldApplyRateLimit(String requestUri) {
        return requestUri.equals("/api/auth/login") || requestUri.equals("/login")
                || requestUri.equals("/api/auth/refresh");
    }

    /**
     * Get or create a bucket for the given IP address
     */
    private Bucket resolveBucket(String clientIp) {
        return cache.computeIfAbsent(clientIp, k -> createNewBucket());
    }

    /**
     * Create a new rate limit bucket with configured capacity and refill duration
     */
    private Bucket createNewBucket() {
        Duration refillDuration = Duration.ofMinutes(refillMinutes);
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, refillDuration));
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Extract client IP address from request, considering proxy headers
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
