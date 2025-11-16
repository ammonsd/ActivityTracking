package com.ammons.taskactivity.config;

import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.repository.UserRepository;
import com.ammons.taskactivity.service.UserDetailsServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
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
    private static final String PASSWORD_EXPIRED_URL = "/change-password?expired=true";
    private static final String DEFAULT_SUCCESS_URL = "/app";

    private final UserRepository userRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final com.ammons.taskactivity.service.UserService userService;
    private final com.ammons.taskactivity.service.LoginAuditService loginAuditService;
    private final com.ammons.taskactivity.service.GeoIpService geoIpService;

    public CustomAuthenticationSuccessHandler(UserRepository userRepository,
            UserDetailsServiceImpl userDetailsService,
            @Lazy com.ammons.taskactivity.service.UserService userService,
            com.ammons.taskactivity.service.LoginAuditService loginAuditService,
            com.ammons.taskactivity.service.GeoIpService geoIpService) {
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
        this.loginAuditService = loginAuditService;
        this.geoIpService = geoIpService;
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
        String ipAddress = getClientIpAddress(request);
        log.info("User '{}' successfully authenticated from IP: {}", username, ipAddress);

        // Lookup geographic location
        String location = geoIpService.lookupLocation(ipAddress);

        // Record successful login attempt with location
        loginAuditService.recordLoginAttempt(username, ipAddress, location, true);

        try {
            // Update the last login time for this user
            userDetailsService.updateLastLoginTime(username);

            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isPresent()) {
                User user = userOptional.get();

                // Reset failed login attempts on successful login
                if (user.getFailedLoginAttempts() > 0) {
                    log.info("Resetting failed login attempts for user '{}' (was: {})", username,
                            user.getFailedLoginAttempts());
                    user.setFailedLoginAttempts(0);
                    user.setAccountLocked(false); // Also unlock if locked
                    userRepository.save(user);
                }

                // Check if password has expired
                if (userService.isPasswordExpired(username)) {
                    log.info("User '{}' has an expired password", username);

                    // Note: GUEST users with expired passwords are already blocked at
                    // authentication time by CustomAuthenticationProvider

                    request.getSession().setAttribute("passwordExpired", true);
                    getRedirectStrategy().sendRedirect(request, response, PASSWORD_EXPIRED_URL);
                    return;
                }

                // Check if user needs to update password (admin forced)
                if (user.isForcePasswordUpdate()) {
                    log.info("User '{}' requires forced password update", username);
                    request.getSession().setAttribute("requiresPasswordUpdate", true);
                    getRedirectStrategy().sendRedirect(request, response,
                            FORCE_PASSWORD_UPDATE_URL);
                    return;
                }
            }

            // Normal login flow - check for saved request
            log.debug("User '{}' proceeding with normal login flow", username);

            // Get saved request (original URL before login redirect)
            HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
            SavedRequest savedRequest = requestCache.getRequest(request, response);

            String targetUrl;
            if (savedRequest != null) {
                String savedUrl = savedRequest.getRedirectUrl();
                log.info("Found saved request URL: {}", savedUrl);

                // Map root path to Thymeleaf task list
                if (savedUrl.endsWith("/") && !savedUrl.contains("/app")) {
                    targetUrl = "/task-activity/list";
                    log.info("Root path requested, redirecting to Thymeleaf UI: {}", targetUrl);
                } else {
                    targetUrl = savedUrl;
                    log.info("Redirecting to saved URL: {}", targetUrl);
                }

                // Clear the saved request
                requestCache.removeRequest(request, response);
            } else {
                // No saved request, default to Angular dashboard
                targetUrl = DEFAULT_SUCCESS_URL;
                log.debug("No saved request found, using default URL: {}", targetUrl);
            }

            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (Exception e) {
            log.error("Error during authentication success handling for user '{}'", username, e);
            // Fall back to default behavior on error
            super.onAuthenticationSuccess(request, response, authentication);
        }
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
