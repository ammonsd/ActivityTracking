package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller for handling access denied scenarios and error pages
 *
 * @author Dean Ammons
 * @version 1.0
 * @since October 2025
 */
@Controller
public class ErrorController {

    private final UserService userService;

    public ErrorController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Display the access denied page with user-friendly message
     */
    @GetMapping("/access-denied")
    public String accessDenied(Model model, Authentication authentication) {
        if (authentication != null) {
            addUserDisplayInfo(model, authentication);
        }
        return "access-denied";
    }

    /**
     * Display the rate limit exceeded page with countdown timer
     */
    @GetMapping("/rate-limit")
    public String rateLimit(Model model, HttpServletRequest request) {
        // Get retry-after value from request attribute (set by RateLimitFilter)
        Object retryAfter = request.getAttribute("retryAfter");
        if (retryAfter != null) {
            model.addAttribute("retryAfter", retryAfter);
        } else {
            // Default to 60 seconds if not set
            model.addAttribute("retryAfter", 60);
        }
        return "rate-limit";
    }

    /**
     * Clean up session attributes used for access denied messaging This endpoint is called via
     * JavaScript to clear session data after displaying the message
     */
    @PostMapping("/clear-access-denied-session")
    @ResponseBody
    public ResponseEntity<String> clearAccessDeniedSession(HttpSession session) {
        session.removeAttribute("accessDeniedMessage");
        session.removeAttribute("requestedUrl");
        return ResponseEntity.ok("Session cleaned");
    }

    /**
     * Add user display information to the model
     */
    private void addUserDisplayInfo(Model model, Authentication authentication) {
        String username = authentication.getName();
        userService.getUserByUsername(username).ifPresent(user -> {
            String firstname = user.getFirstname();
            String lastname = user.getLastname();
            String displayName = (firstname + " " + lastname + " (" + username + ")").trim()
                    .replaceAll("\\s+", " ");
            model.addAttribute("userDisplayName", displayName);
        });
    }
}
