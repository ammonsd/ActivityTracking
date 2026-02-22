package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.service.EmailService;
import com.ammons.taskactivity.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST endpoint for the "Contact System Administrator" feature. Accepts a user-submitted subject
 * and message and forwards them to the configured administrator email address(es) via EmailService.
 * Accessible to all authenticated roles, including GUEST.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since February 2026
 */
@RestController
@RequestMapping("/contact")
public class ContactAdminController {

    private static final Logger logger = LoggerFactory.getLogger(ContactAdminController.class);

    private final EmailService emailService;
    private final UserService userService;

    public ContactAdminController(EmailService emailService, UserService userService) {
        this.emailService = emailService;
        this.userService = userService;
    }

    /**
     * Receives a contact-admin request and dispatches it as an email to the administrator. The
     * sender's username and email are resolved server-side from the authenticated session; they are
     * not part of the request payload.
     *
     * @param payload JSON map containing "subject" and "message" keys
     * @param authentication the current authenticated user session
     * @return 200 OK with {@code {"success": true}} on success, or 500 with
     *         {@code {"success": false, "error": "..."}} on failure
     */
    @PostMapping("/admin")
    public ResponseEntity<Map<String, Object>> contactAdmin(
            @RequestBody Map<String, String> payload, Authentication authentication) {

        String subject = payload.getOrDefault("subject", "").trim();
        String message = payload.getOrDefault("message", "").trim();
        String username = authentication.getName();

        if (subject.isEmpty() || message.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Subject and message are required."));
        }

        // Resolve the sender's email address from their profile (may be blank)
        String senderEmail = userService.getUserByUsername(username)
                .map(u -> u.getEmail() != null ? u.getEmail() : "").orElse("");

        try {
            emailService.sendAdminContactRequest(username, senderEmail, subject, message);
            logger.info("Admin contact request submitted by user: {}", username);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            logger.error("Failed to send admin contact request for user {}: {}", username,
                    e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("success", false, "error",
                    "Failed to send message. Please try again later."));
        }
    }
}
