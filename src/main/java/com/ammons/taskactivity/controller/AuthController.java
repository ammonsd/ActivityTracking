package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.service.EmailService;
import com.ammons.taskactivity.service.PasswordResetService;
import com.ammons.taskactivity.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * AuthController
 *
 * @author Dean Ammons
 * @version 1.0
 * @since October 2025
 */
@Controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final PasswordResetService passwordResetService;
    private final UserService userService;
    private final EmailService emailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public AuthController(PasswordResetService passwordResetService, UserService userService,
            EmailService emailService) {
        this.passwordResetService = passwordResetService;
        this.userService = userService;
        this.emailService = emailService;
    }

    // REMOVED the /login GET mapping - let Spring Security handle it
    // The custom login page is configured in SecurityConfig

    @GetMapping("/")
    public String home() {
        return "redirect:/task-activity";
    }

    /**
     * Show password reset request form
     */
    @GetMapping("/reset-password")
    public String showResetPasswordForm() {
        return "reset-password";
    }

    /**
     * Process password reset request - send email with reset link
     */
    @PostMapping("/reset-password")
    public String processResetPasswordRequest(@RequestParam String email,
            RedirectAttributes redirectAttributes) {
        logger.info("Password reset requested for email: {}", email);

        try {
            Optional<User> userOpt = userService.getUserByEmail(email);

            if (userOpt.isPresent()) {
                User user = userOpt.get();

                // Block password reset for GUEST role (silent block for security)
                if (user.getRole() != null && "GUEST".equals(user.getRole().getName())) {
                    logger.warn("Password reset blocked for GUEST user: {} (email: {})",
                            user.getUsername(), email);
                    // Don't send email, but show generic success message for security
                } else {
                    String token = passwordResetService.generateResetToken(email);
                    String resetLink = baseUrl + "/change-password?token=" + token;

                    emailService.sendPasswordResetEmail(email, user.getUsername(),
                            user.getFirstname() + " " + user.getLastname(), resetLink,
                            passwordResetService.getTokenExpiryMinutes());

                    logger.info("Password reset email sent to: {}", email);
                }
            } else {
                logger.warn("Password reset requested for non-existent email: {}", email);
                // Don't reveal that email doesn't exist - same message for security
            }

            // Always show success message (don't reveal if email exists or role restrictions)
            redirectAttributes.addFlashAttribute("successMessage",
                    "If that email address is registered, you will receive a password reset link shortly.");
            return "redirect:/reset-password?success";

        } catch (Exception e) {
            logger.error("Error processing password reset request for email {}: {}", email,
                    e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Unable to process your request. Please try again.");
            return "redirect:/reset-password?error";
        }
    }
}
