package com.ammons.taskactivity.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Unit tests for short-term security fixes (Issues #5-7).
 * 
 * Tests rate limiting CloudFlare header usage without requiring full Spring context.
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
public class RateLimitCloudFlareTest {

    @Mock
    private HttpServletRequest request;

    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        rateLimitFilter = new RateLimitFilter();
    }

    @Test
    public void testUsesCloudFlareConnectingIpWhenAvailable() throws Exception {
        // Given: Request with CF-Connecting-IP header
        when(request.getHeader("CF-Connecting-IP")).thenReturn("203.0.113.42");
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4"); // Should be ignored
        when(request.getRemoteAddr()).thenReturn("127.0.0.1"); // Should not be used

        // When: Extract client IP
        String clientIp = invokeGetClientIp(request);

        // Then: Should use CF-Connecting-IP
        assertEquals("203.0.113.42", clientIp, "Should use CF-Connecting-IP when available");
    }

    @Test
    public void testIgnoresXForwardedForHeader() throws Exception {
        // Given: Request with only X-Forwarded-For (easily spoofed)
        when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // When: Extract client IP
        String clientIp = invokeGetClientIp(request);

        // Then: Should use getRemoteAddr(), NOT X-Forwarded-For
        assertEquals("127.0.0.1", clientIp,
                "Should ignore X-Forwarded-For and use getRemoteAddr()");
    }

    @Test
    public void testFallsBackToRemoteAddrWhenNoCloudFlare() throws Exception {
        // Given: Request without CloudFlare headers (direct connection)
        when(request.getHeader("CF-Connecting-IP")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        // When: Extract client IP
        String clientIp = invokeGetClientIp(request);

        // Then: Should use getRemoteAddr()
        assertEquals("192.168.1.100", clientIp,
                "Should use getRemoteAddr() when not behind CloudFlare");
    }

    @Test
    public void testHandlesEmptyCloudFlareHeader() throws Exception {
        // Given: Request with empty CF-Connecting-IP
        when(request.getHeader("CF-Connecting-IP")).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        // When: Extract client IP
        String clientIp = invokeGetClientIp(request);

        // Then: Should fall back to getRemoteAddr()
        assertEquals("192.168.1.100", clientIp,
                "Should fall back to getRemoteAddr() when CF-Connecting-IP is empty");
    }

    /**
     * Use reflection to invoke private getClientIp method for testing. This allows unit testing
     * without full Spring context.
     */
    private String invokeGetClientIp(HttpServletRequest request) throws Exception {
        Method method =
                RateLimitFilter.class.getDeclaredMethod("getClientIp", HttpServletRequest.class);
        method.setAccessible(true);
        return (String) method.invoke(rateLimitFilter, request);
    }
}
