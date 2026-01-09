package com.ammons.taskactivity.config;

import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * ForcePasswordUpdateFilter
 *
 * @author Dean Ammons
 * @version 1.0
 * @since October 2025
 */
@Component
public class ForcePasswordUpdateFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ForcePasswordUpdateFilter.class);

    private final UserRepository userRepository;

    // URLs that should be accessible even when password update is required
    private static final String[] ALLOWED_URLS = {"/change-password", "/logout", "/login", "/error",
            "/access-denied", "/clear-access-denied-session", "/css/", "/js/", "/images/",
            "/static/", "/actuator/health"};

    public ForcePasswordUpdateFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Skip filter if user is not authenticated
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            filterChain.doFilter(request, response);
            return;
        }

        String requestURI = request.getRequestURI();

        // Allow access to certain URLs
        if (isAllowedUrl(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = authentication.getName();
        log.debug("Checking force password update for user: {} accessing: {}", username,
                requestURI);

        try {
            Optional<User> userOptional = userRepository.findByUsername(username);
            if (userOptional.isPresent()) {
                User user = userOptional.get();

                if (user.isForcePasswordUpdate()) {
                    log.info(
                            "User '{}' requires password update, redirecting to change-password page",
                            username);

                    // Set session attribute to indicate forced update
                    HttpSession session = request.getSession();
                    session.setAttribute("requiresPasswordUpdate", true);

                    // Redirect to change password page
                    response.sendRedirect(
                            request.getContextPath() + "/change-password?forced=true");
                    return;
                }
            }
        } catch (Exception e) {
            log.error("Error checking force password update for user: {}", username, e);
            // Allow request to continue on error to avoid blocking the system
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedUrl(String requestURI) {
        for (String allowedUrl : ALLOWED_URLS) {
            if (requestURI.startsWith(allowedUrl) || requestURI.contains(allowedUrl)) {
                return true;
            }
        }
        return false;
    }
}
