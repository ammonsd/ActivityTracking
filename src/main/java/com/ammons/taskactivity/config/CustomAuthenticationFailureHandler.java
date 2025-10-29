package com.ammons.taskactivity.config;

import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

/**
 * Custom authentication failure handler that provides specialized handling for different types of
 * authentication failures, particularly for disabled user accounts.
 * 
 * <p>
 * This handler:
 * <ul>
 * <li>Detects when authentication fails due to disabled user accounts</li>
 * <li>Provides specific error parameters for different failure types</li>
 * <li>Enhances user experience with meaningful error messages</li>
 * <li>Logs authentication failures for security monitoring</li>
 * </ul>
 * 
 * @author Dean Ammons
 * @version 1.0
 */
@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger log =
            LoggerFactory.getLogger(CustomAuthenticationFailureHandler.class);
    private static final String LOGIN_URL = "/login";

    private final UserRepository userRepository;

    public CustomAuthenticationFailureHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Handles authentication failures with specialized logic for disabled accounts.
     * 
     * <p>
     * Processing logic:
     * <ol>
     * <li>Check if failure is due to DisabledException</li>
     * <li>If not, check if user exists but is disabled in database</li>
     * <li>Redirect to login with appropriate error parameter:
     * <ul>
     * <li>?disabled=true for disabled accounts</li>
     * <li>?error=true for other authentication failures</li>
     * </ul>
     * </li>
     * </ol>
     * 
     * @param request the HTTP request containing login attempt
     * @param response the HTTP response for redirect
     * @param exception the authentication exception that occurred
     * @throws IOException if redirect fails
     * @throws ServletException if servlet processing fails
     */
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {

        String username = request.getParameter("username");
        log.debug("Authentication failed for user: {}", username);

        // Check if the failure is due to a disabled account
        if (exception instanceof DisabledException) {
            log.info("Authentication failed for disabled user: {}", username);
            // Redirect with disabled error parameter
            getRedirectStrategy().sendRedirect(request, response, LOGIN_URL + "?disabled=true");
            return;
        }

        // For other authentication failures, check if user exists and is disabled
        if (username != null && !username.isEmpty()) {
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isPresent() && !userOptional.get().isEnabled()) {
                log.info("Authentication failed for existing disabled user: {}", username);
                getRedirectStrategy().sendRedirect(request, response, LOGIN_URL + "?disabled=true");
                return;
            }
        }

        // Default behavior for other authentication failures
        log.debug("Authentication failed with standard error for user: {}", username);
        getRedirectStrategy().sendRedirect(request, response, LOGIN_URL + "?error=true");
    }
}
