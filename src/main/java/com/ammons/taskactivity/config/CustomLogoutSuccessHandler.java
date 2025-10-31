package com.ammons.taskactivity.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
 */
@Component
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        // Always redirect to login page with logout parameter
        response.sendRedirect("/login?logout=true");
    }
}
