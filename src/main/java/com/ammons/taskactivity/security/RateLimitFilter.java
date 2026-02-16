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
 * @since January 2026
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

                // Check if client accepts HTML (browser) or JSON (API)
                String acceptHeader = request.getHeader("Accept");
                boolean isBrowserRequest =
                        acceptHeader != null && acceptHeader.contains("text/html");

                if (isBrowserRequest) {
                    // Redirect browser to pretty error page
                    request.setAttribute("retryAfter", refillMinutes * 60);
                    request.getRequestDispatcher("/rate-limit").forward(request, response);
                } else {
                    // Return JSON for API requests
                    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    response.setContentType("application/json");
                    response.setHeader("Retry-After", String.valueOf(refillMinutes * 60));
                    response.getWriter().write(
                            "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}");
                }
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
     * Extract client IP address from request.
     * 
     * SECURITY: Uses CloudFlare's CF-Connecting-IP header which cannot be spoofed by clients.
     * CloudFlare sets this header with the actual client IP address. Falls back to getRemoteAddr()
     * if not behind CloudFlare.
     * 
     * X-Forwarded-For is NOT trusted as it can be easily spoofed by attackers.
     */
    private String getClientIp(HttpServletRequest request) {
        // CloudFlare provides CF-Connecting-IP with the real client IP
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isEmpty()) {
            logger.debug("[Rate Limit] Using CF-Connecting-IP: {}", cfConnectingIp);
            return cfConnectingIp;
        }

        // Fall back to direct connection IP (not behind CloudFlare)
        String remoteAddr = request.getRemoteAddr();
        logger.debug("[Rate Limit] Using getRemoteAddr(): {}", remoteAddr);
        return remoteAddr;
    }
}
