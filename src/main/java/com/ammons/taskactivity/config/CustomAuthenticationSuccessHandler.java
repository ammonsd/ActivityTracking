package com.ammons.taskactivity.config;

import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.repository.UserRepository;
import com.ammons.taskactivity.service.PermissionService;
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
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Custom authentication success handler that manages post-login behavior including forced password
 * updates for new or admin-created users.
 *
 * <p>
 * Post-login redirect priority (first match wins):
 * <ol>
 * <li>Force-password-update or expired-password → change-password page</li>
 * <li>Session attribute {@code POST_LOGIN_REDIRECT} (set by LoginController when React redirects to
 * login with a {@code ?redirect=} param, e.g. on 401 session expiry) → that path</li>
 * <li>Spring Security {@code SavedRequest} for a {@code /dashboard} path (set when Spring Security
 * itself intercepted a direct browser navigation to the React app) → that path</li>
 * <li>Permission-based default: {@code TASK_ACTIVITY:READ} → {@code /task-activity/list},
 * {@code EXPENSE:READ} → {@code /expenses}, otherwise → React dashboard</li>
 * </ol>
 * The full saved-request mechanism (all paths) is intentionally NOT re-enabled to avoid redirecting
 * users to admin pages they previously visited, which would cause Access Denied.
 *
 * Modified by: Dean Ammons - February 2026 Change: Added RequestCache check for /dashboard saved
 * requests to fix direct-URL navigation Reason: When an unauthenticated user navigates directly to
 * /dashboard, Spring Security intercepts the request and saves it in HttpSessionRequestCache before
 * the React app loads — so the React ProtectedRoute never runs and POST_LOGIN_REDIRECT is never
 * set.
 *
 * Modified by: Dean Ammons - February 2026 Change: Replaced saved-request / role-based redirect
 * with permission-based redirect Reason: Saved-request caused Access Denied when a user previously
 * visited an admin page; hardcoded role checks broke for custom roles.
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

    // RequestCache gives us access to the URL Spring Security saved before redirecting the
    // unauthenticated user to /login (populated by ExceptionTranslationFilter on direct
    // navigation).
    private final RequestCache requestCache = new HttpSessionRequestCache();

    private final String reactDashboardUrl;
    private final UserRepository userRepository;
    private final UserDetailsServiceImpl userDetailsService;
    private final com.ammons.taskactivity.service.UserService userService;
    private final com.ammons.taskactivity.service.LoginAuditService loginAuditService;
    private final com.ammons.taskactivity.service.GeoIpService geoIpService;
    private final com.ammons.taskactivity.service.EmailService emailService;
    private final com.ammons.taskactivity.service.PasswordHistoryService passwordHistoryService;
    private final PermissionService permissionService;

    public CustomAuthenticationSuccessHandler(
            @org.springframework.beans.factory.annotation.Value("${app.dashboard.react-url:/dashboard}") String reactDashboardUrl,
            UserRepository userRepository,
            UserDetailsServiceImpl userDetailsService,
            @Lazy com.ammons.taskactivity.service.UserService userService,
            com.ammons.taskactivity.service.LoginAuditService loginAuditService,
            com.ammons.taskactivity.service.GeoIpService geoIpService,
            com.ammons.taskactivity.service.EmailService emailService,
            com.ammons.taskactivity.service.PasswordHistoryService passwordHistoryService,
            PermissionService permissionService) {
        this.reactDashboardUrl = reactDashboardUrl;
        this.userRepository = userRepository;
        this.userDetailsService = userDetailsService;
        this.userService = userService;
        this.loginAuditService = loginAuditService;
        this.geoIpService = geoIpService;
        this.emailService = emailService;
        this.passwordHistoryService = passwordHistoryService;
        this.permissionService = permissionService;
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

            // --- Priority 2: React-initiated redirect (session expiry detected via 401) ---
            // When the React SPA detects a 401 on an API call, it redirects to
            // /login?redirect=<path>. LoginController validates and stores that path in the
            // session.
            // We read only the server-side session value (never the raw query param) to prevent
            // open-redirect attacks.
            jakarta.servlet.http.HttpSession session = request.getSession(false);
            if (session != null) {
                String savedRedirect = (String) session.getAttribute(
                        com.ammons.taskactivity.controller.LoginController.POST_LOGIN_REDIRECT_ATTR);
                if (savedRedirect != null && !savedRedirect.isBlank()) {
                    session.removeAttribute(
                            com.ammons.taskactivity.controller.LoginController.POST_LOGIN_REDIRECT_ATTR);
                    log.info("User '{}' has a POST_LOGIN_REDIRECT to '{}', honouring it", username,
                            savedRedirect);
                    getRedirectStrategy().sendRedirect(request, response, savedRedirect);
                    return;
                }
            }

            // --- Priority 3: Spring Security SavedRequest for SPA paths ---
            // When an unauthenticated user navigates directly to /dashboard (React) or /app
            // (Angular), Spring Security's ExceptionTranslationFilter stores the request in
            // HttpSessionRequestCache and redirects to /login — before any frontend JS loads.
            // We only honour saved requests starting with /dashboard or /app to avoid restoring
            // navigation to task or admin pages that could cause Access Denied.
            SavedRequest savedRequest = requestCache.getRequest(request, response);
            if (savedRequest != null) {
                String savedPath = extractSavedSpaPath(savedRequest.getRedirectUrl());
                if (savedPath != null) {
                    requestCache.removeRequest(request, response);
                    log.info("User '{}' has a Spring Security saved request to '{}', honouring it",
                            username, savedPath);
                    getRedirectStrategy().sendRedirect(request, response, savedPath);
                    return;
                }
            }

            // --- Priority 4: Permission-based default redirect ---
            // Redirect based on the user's permissions, not their role name.
            // This works for any custom role without requiring code changes.
            String targetUrl;
            if (permissionService.userHasPermission(username, "TASK_ACTIVITY:READ")) {
                targetUrl = "/task-activity/list";
                log.info("User '{}' has TASK_ACTIVITY:READ, redirecting to: {}", username,
                        targetUrl);
            } else if (permissionService.userHasPermission(username, "EXPENSE:READ")) {
                targetUrl = "/expenses";
                log.info("User '{}' has EXPENSE:READ, redirecting to: {}", username, targetUrl);
            } else {
                targetUrl = reactDashboardUrl;
                log.info(
                        "User '{}' has no task/expense READ permission, redirecting to dashboard: {}",
                        username, targetUrl);
            }

            getRedirectStrategy().sendRedirect(request, response, targetUrl);

        } catch (Exception e) {
            log.error("Error during authentication success handling for user '{}'", username, e);
            // Fall back to default behavior on error
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }

    /**
     * Extracts the path from a saved redirect URL if it is a frontend SPA path that should be
     * honoured after login. Accepted prefixes:
     * <ul>
     * <li>{@code /dashboard} — React admin dashboard</li>
     * <li>{@code /app} — Angular application</li>
     * </ul>
     * Returns {@code null} for any other path (e.g. task-activity, admin, API) so that the caller
     * falls through to permission-based routing, avoiding Access Denied errors.
     *
     * @param redirectUrl the full URL stored by Spring Security's RequestCache
     * @return the path portion if it is a recognised SPA prefix, otherwise {@code null}
     */
    private String extractSavedSpaPath(String redirectUrl) {
        try {
            String path = new URI(redirectUrl).getPath();
            if (path != null && (path.startsWith("/dashboard") || path.startsWith("/app"))) {
                return path;
            }
            return null;
        } catch (URISyntaxException e) {
            log.debug("Could not parse saved request URL: {}", redirectUrl);
            return null;
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
