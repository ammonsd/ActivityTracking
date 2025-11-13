package com.ammons.taskactivity.config;

import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${security.login.max-attempts:5}")
    private int maxLoginAttempts;

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
        String ipAddress = getClientIpAddress(request);
        log.warn("Authentication failed for user: '{}' from IP: {}", username, ipAddress);

        // Check for GUEST user with expired password
        if (exception instanceof GuestPasswordExpiredException) {
            log.info("Authentication failed: GUEST user '{}' has expired password", username);
            String redirectUrl = LOGIN_URL + "?guest_expired";
            log.info("Redirecting to: {}", redirectUrl);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            return;
        }

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
            if (userOptional.isPresent()) {
                User user = userOptional.get();

                // Increment failed login attempts
                int attempts = user.getFailedLoginAttempts() + 1;
                user.setFailedLoginAttempts(attempts);

                // Check if account should be locked
                if (attempts >= maxLoginAttempts && !user.isAccountLocked()) {
                    user.setAccountLocked(true);
                    log.warn("User '{}' account locked after {} failed login attempts", username,
                            attempts);
                } else {
                    log.info("Failed login attempt {} of {} for user '{}'", attempts,
                            maxLoginAttempts, username);
                }

                userRepository.save(user);

                // Check if user is locked or disabled
                if (user.isAccountLocked()) {
                    log.info("Authentication failed for locked user: {}", username);
                    getRedirectStrategy().sendRedirect(request, response, LOGIN_URL + "?locked=true");
                    return;
                }

                if (!user.isEnabled()) {
                    log.info("Authentication failed for existing disabled user: {}", username);
                    getRedirectStrategy().sendRedirect(request, response, LOGIN_URL + "?disabled=true");
                    return;
                }
            }
        }

        // Default behavior for other authentication failures
        log.debug("Authentication failed with standard error for user: {}", username);
        getRedirectStrategy().sendRedirect(request, response, LOGIN_URL + "?error=true");
    }

    /**
     * Extracts the client's IP address from the HTTP request, handling proxies and load balancers.
     * 
     * <p>
     * Checks the following headers in order:
     * <ul>
     * <li>X-Forwarded-For - Standard proxy header (Cloudflare, AWS ALB, etc.)</li>
     * <li>X-Real-IP - Alternative proxy header</li>
     * <li>Proxy-Client-IP - WebLogic proxy</li>
     * <li>WL-Proxy-Client-IP - WebLogic proxy alternative</li>
     * <li>HTTP_X_FORWARDED_FOR - Alternative format</li>
     * <li>HTTP_CLIENT_IP - Some proxies</li>
     * <li>Remote address - Direct connection</li>
     * </ul>
     * 
     * @param request the HTTP request
     * @return the client's IP address, or "unknown" if not determinable
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {"X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP",
                "WL-Proxy-Client-IP", "HTTP_X_FORWARDED_FOR", "HTTP_CLIENT_IP"};

        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs (client, proxy1, proxy2)
                // The first one is the original client
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        // Fallback to remote address
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isEmpty()) ? remoteAddr : "unknown";
    }
}
