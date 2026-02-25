package com.ammons.taskactivity.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * LoginController - Simple controller to serve the login page. Spring Security handles the POST
 * processing. When the React app redirects here after detecting an unauthenticated user, it passes
 * the intended destination as a {@code redirect} query parameter. This controller stores that value
 * in the HTTP session so {@code CustomAuthenticationSuccessHandler} can redirect the user back to
 * their original destination after a successful login.
 *
 * Modified by: Dean Ammons - February 2026 Change: Added redirect param capture to support
 * post-login return-to-destination Reason: Users navigating directly to /dashboard were sent to the
 * Angular UI after login instead of the React dashboard because the permission-based redirect
 * ignored the intended URL.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since October 2025
 */
@Controller
public class LoginController {

    /** Session attribute key used to carry the post-login redirect path. */
    public static final String POST_LOGIN_REDIRECT_ATTR = "POST_LOGIN_REDIRECT";

    /**
     * Renders the login page. If the caller supplied a {@code redirect} query parameter (e.g. from
     * the React ProtectedRoute), the value is stored in the HTTP session so that
     * {@code CustomAuthenticationSuccessHandler} can honour it after successful authentication.
     *
     * @param redirect optional path to redirect to after login (must start with {@code /dashboard})
     * @param session the current HTTP session
     * @return the Thymeleaf login view name
     */
    @GetMapping("/login")
    public String login(@RequestParam(value = "redirect", required = false) String redirect,
            HttpSession session) {

        // Only store safe internal SPA paths to prevent open redirect attacks.
        // Accepted: /dashboard (React admin dashboard) and /app (Angular application).
        if (redirect != null && !redirect.isBlank()
                && (redirect.startsWith("/dashboard") || redirect.startsWith("/app"))) {
            session.setAttribute(POST_LOGIN_REDIRECT_ATTR, redirect);
        }

        return "login";
    }
}
