package com.ammons.taskactivity.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom Access Denied Handler that provides intelligent response handling based on request type
 * (web browser vs AJAX/API) when users lack permission to access resources.
 * 
 * <p>
 * This handler:
 * <ul>
 * <li>Detects AJAX, API, and JSON requests vs regular web requests</li>
 * <li>Returns JSON responses for programmatic clients</li>
 * <li>Redirects to user-friendly error pages for web browsers</li>
 * <li>Stores context information for error page display</li>
 * </ul>
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since October 2025
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    /**
     * Handles access denied scenarios with intelligent response routing based on request type.
     * 
     * <p>
     * Decision logic:
     * <ol>
     * <li>Check request headers to identify request type:
     * <ul>
     * <li>X-Requested-With: XMLHttpRequest (AJAX requests)</li>
     * <li>Accept: application/json (API requests)</li>
     * <li>URI starting with /api/ (API endpoints)</li>
     * </ul>
     * </li>
     * <li>For programmatic requests: return HTTP 403 with JSON error</li>
     * <li>For web requests: redirect to friendly access-denied page with context</li>
     * </ol>
     * 
     * @param request the HTTP request that was denied
     * @param response the HTTP response to be sent
     * @param accessDeniedException the exception representing the access denial
     * @throws IOException if I/O errors occur during response handling
     * @throws ServletException if servlet processing fails
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {

        // Check if this is an AJAX request or API call
        String requestedWith = request.getHeader("X-Requested-With");
        String accept = request.getHeader("Accept");

        // Only return JSON for explicit AJAX requests or API endpoints
        boolean isAjaxRequest = "XMLHttpRequest".equals(requestedWith);
        boolean isApiRequest = request.getRequestURI().startsWith("/api/");
        boolean requestsJson = accept != null && accept.contains("application/json")
                && !accept.contains("text/html");

        if (isAjaxRequest || isApiRequest || requestsJson) {
            // For AJAX/API requests, return JSON response
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Access Denied\",\"message\":\"You don't have permission to access this resource\"}");
        } else {
            // For regular web requests, redirect to friendly error page
            String targetUrl = request.getRequestURI();
            request.getSession().setAttribute("accessDeniedMessage",
                    "Sorry, you don't have permission to access this page. Please contact your administrator if you believe this is an error.");
            request.getSession().setAttribute("requestedUrl", targetUrl);
            response.sendRedirect("/access-denied");
        }
    }
}
