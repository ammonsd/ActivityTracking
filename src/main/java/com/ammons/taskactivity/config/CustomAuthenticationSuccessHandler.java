package com.ammons.taskactivity.config;

import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.repository.UserRepository;
import com.ammons.taskactivity.service.UserDetailsServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

/**
 * Custom authentication success handler that manages post-login behavior including forced password
 * updates for new or admin-created users.
 * 
 * <p>
 * This handler:
 * <ul>
 * <li>Checks if the authenticated user requires a forced password update</li>
 * <li>Redirects to password change page if update is required</li>
 * <li>Redirects to default application page for normal login flow</li>
 * <li>Provides comprehensive logging for authentication events</li>
 * </ul>
 * 
 * @author Dean Ammons
 * @version 1.0
 */
@Component
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log =
            LoggerFactory.getLogger(CustomAuthenticationSuccessHandler.class);
    private static final String FORCE_PASSWORD_UPDATE_URL = "/change-password?forced=true";
    private static final String DEFAULT_SUCCESS_URL = "/task-activity/list";

    private final UserRepository userRepository;
    private final UserDetailsServiceImpl userDetailsService;

    public CustomAuthenticationSuccessHandler(UserRepository userRepository,
            UserDetailsServiceImpl userDetailsService) {
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
        // Set default target URL for the parent SimpleUrlAuthenticationSuccessHandler
        setDefaultTargetUrl(DEFAULT_SUCCESS_URL);
        setAlwaysUseDefaultTargetUrl(false);
    }

    /**
     * Handles successful authentication by checking if the user requires a forced password update
     * and redirecting accordingly.
     * 
     * <p>
     * Flow:
     * <ol>
     * <li>Retrieve the authenticated user from the database</li>
     * <li>Check if user has forcePasswordUpdate flag set</li>
     * <li>If yes: redirect to password change page with forced=true parameter</li>
     * <li>If no: redirect to default application landing page</li>
     * </ol>
     * 
     * @param request the HTTP request
     * @param response the HTTP response
     * @param authentication the authentication object containing user details
     * @throws IOException if redirect fails
     * @throws ServletException if servlet processing fails
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        String username = authentication.getName();
        log.info("User '{}' successfully authenticated", username);

        try {
            // Update the last login time for this user
            userDetailsService.updateLastLoginTime(username);

            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isPresent()) {
                User user = userOptional.get();

                // Check if user needs to update password
                if (user.isForcePasswordUpdate()) {
                    log.info("User '{}' requires forced password update", username);
                    request.getSession().setAttribute("requiresPasswordUpdate", true);
                    getRedirectStrategy().sendRedirect(request, response,
                            FORCE_PASSWORD_UPDATE_URL);
                    return;
                }
            }

            // Normal login flow
            log.debug("User '{}' proceeding with normal login flow", username);
            getRedirectStrategy().sendRedirect(request, response, DEFAULT_SUCCESS_URL);

        } catch (Exception e) {
            log.error("Error during authentication success handling for user '{}'", username, e);
            // Fall back to default behavior on error
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }
}
