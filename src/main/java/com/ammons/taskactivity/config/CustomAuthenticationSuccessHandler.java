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
 * @since January 2026
 */
@Component
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log =
            LoggerFactory.getLogger(CustomAuthenticationSuccessHandler.class);
    private static final String FORCE_PASSWORD_UPDATE_URL = "/change-password?forced=true";
    private static final String PASSWORD_EXPIRED_URL = "/change-password?expired=true";

    private final String reactDashboardUrl;
    private final UserRepository userRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final com.ammons.taskactivity.service.UserService userService;
    private final com.ammons.taskactivity.service.LoginAuditService loginAuditService;
    private final com.ammons.taskactivity.service.GeoIpService geoIpService;
    private final com.ammons.taskactivity.service.EmailService emailService;
    private final com.ammons.taskactivity.service.PasswordHistoryService passwordHistoryService;

    public CustomAuthenticationSuccessHandler(
            @org.springframework.beans.factory.annotation.Value("${app.dashboard.react-url:/dashboard}") String reactDashboardUrl,
            UserRepository userRepository,
            UserDetailsServiceImpl userDetailsService,
            @Lazy com.ammons.taskactivity.service.UserService userService,
            com.ammons.taskactivity.service.LoginAuditService loginAuditService,
            com.ammons.taskactivity.service.GeoIpService geoIpService,
            com.ammons.taskactivity.service.EmailService emailService,
            com.ammons.taskactivity.service.PasswordHistoryService passwordHistoryService) {
        this.reactDashboardUrl = reactDashboardUrl;
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
        this.loginAuditService = loginAuditService;
        this.geoIpService = geoIpService;
        this.emailService = emailService;
        this.passwordHistoryService = passwordHistoryService;
        // Set default target URL for the parent SimpleUrlAuthenticationSuccessHandler
        setDefaultTargetUrl(reactDashboardUrl);
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

            // Store original password hash for this session
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                passwordHistoryService.storeOriginalPassword(username, user.getPassword());
                log.debug("Stored original password hash for user: {}", username);
            }

            // Send email notification if GUEST role user logs in
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                if ("GUEST".equals(user.getRole().getName())) {
                    try {
                        String fullName = user.getFirstname() != null && user.getLastname() != null
                                ? user.getFirstname() + " " + user.getLastname()
                                : null;
                        emailService.sendGuestLoginNotification(username, fullName, ipAddress,
                                location);
                        log.info(
                                "Guest login notification email sent for user: {}, IP: {}, Location: {}",
                                username, ipAddress, location);
                    } catch (Exception e) {
                        log.error("Failed to send guest login notification email", e);
                        // Continue processing even if email fails
                    }
                }

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

                // Map root path to Thymeleaf task list (unless it's /app or /dashboard)
                if (savedUrl.endsWith("/") && !savedUrl.contains("/app")
                        && !savedUrl.contains("/dashboard")) {
                    targetUrl = "/task-activity/list";
                    log.info("Root path requested, redirecting to Thymeleaf UI: {}", targetUrl);
                } else {
                    targetUrl = savedUrl;
                    log.info("Redirecting to saved URL: {}", targetUrl);
                }

                // Clear the saved request
                requestCache.removeRequest(request, response);
            } else {
                // No saved request, default to React dashboard
                targetUrl = reactDashboardUrl;
                log.debug("No saved request found, using React dashboard URL: {}", targetUrl);
            }

            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (Exception e) {
            log.error("Error during authentication success handling for user '{}'", username, e);
            // Fall back to default behavior on error
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }

    /**
     * Extracts the client's IP address from the HTTP request.
     * 
     * <p>
     * SECURITY: Uses CloudFlare's CF-Connecting-IP header which cannot be spoofed by clients.
     * CloudFlare sets this header with the actual client IP address. Falls back to getRemoteAddr()
     * if not behind CloudFlare.
     * 
     * <p>
     * X-Forwarded-For and other proxy headers are NOT trusted as they can be easily spoofed by
     * attackers to bypass rate limiting and security controls.
     * 
     * @param request the HTTP request
     * @return the client's IP address, or "unknown" if not determinable
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // CloudFlare provides CF-Connecting-IP with the real client IP (cannot be spoofed)
        String cfConnectingIp = request.getHeader("CF-Connecting-IP");
        if (cfConnectingIp != null && !cfConnectingIp.isEmpty()
                && !"unknown".equalsIgnoreCase(cfConnectingIp)) {
            return cfConnectingIp;
        }

        // Fallback to direct connection IP (not behind CloudFlare)
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !remoteAddr.isEmpty()) ? remoteAddr : "unknown";
    }
}
