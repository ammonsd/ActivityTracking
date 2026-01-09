package com.ammons.taskactivity.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Custom Logout Success Handler that provides clean redirect after logout, regardless of where the
 * logout was initiated from (Angular app or Spring Boot pages).
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since November 2025
 */
@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomLogoutSuccessHandler.class);

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        // Check if this is a GUEST user with expired password
        String guestExpired = request.getParameter("guest_expired");
        log.info("Logout success handler called. guest_expired parameter: {}", guestExpired);

        if ("true".equals(guestExpired)) {
            log.info("Redirecting to login with guest_expired error");
            // Redirect to login with guest_expired error message
            response.sendRedirect("/login?error=guest_expired");
            return;
        }

        log.info("Redirecting to login with logout message");
        // Always redirect to login page with logout parameter
        response.sendRedirect("/login?logout=true");
    }
}
