package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.PasswordChangeDto;
import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.service.EmailService;
import com.ammons.taskactivity.service.PasswordResetService;
import com.ammons.taskactivity.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

/**
 * PasswordChangeController
 *
 * @author Dean Ammons
 * @version 1.0
 * @since December 2025
 */
@Controller
public class PasswordChangeController {

    private static final Logger logger = LoggerFactory.getLogger(PasswordChangeController.class);
    private static final String PASSWORD_CHANGE_DTO = "passwordChangeDto";
    private static final String IS_FORCED = "isForced";
    private static final String IS_PASSWORD_RESET = "isPasswordReset";
    private static final String CHANGE_PASSWORD_VIEW = "change-password";

    private final UserService userService;
    private final PasswordResetService passwordResetService;
    private final EmailService emailService;

    public PasswordChangeController(UserService userService,
            PasswordResetService passwordResetService, EmailService emailService) {
        this.userService = userService;
        this.passwordResetService = passwordResetService;
        this.emailService = emailService;
    }

    /**
     * Show the password change form
     */
    @GetMapping("/change-password")
    public String showPasswordChangeForm(
            @RequestParam(value = "forced", required = false) String forced,
            @RequestParam(value = "expired", required = false) String expired,
            @RequestParam(value = "token", required = false) String token, Model model,
            Authentication authentication, HttpSession session) {

        // Handle password reset via email token (no authentication required)
        if (token != null && !token.isEmpty()) {
            Optional<String> emailOpt = passwordResetService.validateToken(token);

            if (emailOpt.isEmpty()) {
                logger.warn("Invalid or expired password reset token");
                model.addAttribute("errorMessage",
                        "Invalid or expired reset link. Please request a new one.");
                return "redirect:/reset-password?error";
            }

            String email = emailOpt.get();
            Optional<User> userOpt = userService.getUserByEmail(email);

            if (userOpt.isEmpty()) {
                logger.error("User not found for email during password reset: {}", email);
                return "redirect:/reset-password?error";
            }

            User user = userOpt.get();
            PasswordChangeDto dto = new PasswordChangeDto();
            dto.setUsername(user.getUsername());

            model.addAttribute(PASSWORD_CHANGE_DTO, dto);
            model.addAttribute(IS_PASSWORD_RESET, true);
            model.addAttribute(IS_FORCED, false);
            model.addAttribute("resetToken", token);
            model.addAttribute("userDisplayName", user.getFirstname() + " " + user.getLastname());

            logger.info("Displaying password reset form for user: {}", user.getUsername());
            return CHANGE_PASSWORD_VIEW;
        }

        // Regular authenticated password change flow
        if (authentication == null) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        logger.info("User '{}' accessing password change form", username);

        // Block GUEST users from accessing password change
        if (authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_GUEST"))) {
            logger.warn("GUEST user '{}' attempted to access password change page", username);
            return "redirect:/task-activity/list?error=guest_restricted";
        }

        // Check if this is a forced password update or expired password
        boolean isForced = "true".equals(forced)
                || Boolean.TRUE.equals(session.getAttribute("requiresPasswordUpdate"));
        boolean isExpired = "true".equals(expired)
                || Boolean.TRUE.equals(session.getAttribute("passwordExpired"));

        // Create DTO and set the username
        PasswordChangeDto passwordChangeDto = new PasswordChangeDto();
        passwordChangeDto.setUsername(username);

        model.addAttribute(PASSWORD_CHANGE_DTO, passwordChangeDto);
        addUserDisplayInfo(model, authentication);
        model.addAttribute(IS_FORCED, isForced || isExpired);
        model.addAttribute(IS_PASSWORD_RESET, false);
        model.addAttribute("isExpired", isExpired);

        return CHANGE_PASSWORD_VIEW;
    }

    /**
     * Process the password change form submission
     */
    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute PasswordChangeDto passwordChangeDto,
            @RequestParam(value = "resetToken", required = false) String resetToken,
            BindingResult bindingResult, Model model, Authentication authentication,
            HttpSession session, RedirectAttributes redirectAttributes) {

        // Handle password reset via token (no authentication required)
        if (resetToken != null && !resetToken.isEmpty()) {
            return handlePasswordReset(resetToken, passwordChangeDto, bindingResult, model,
                    redirectAttributes);
        }

        // Regular authenticated password change flow
        if (authentication == null) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        logger.info("User '{}' attempting to change password", username);

        // Block GUEST users from changing password
        if (authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_GUEST"))) {
            logger.warn("GUEST user '{}' attempted to change password", username);
            return "redirect:/task-activity/list?error=guest_restricted";
        }

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            model.addAttribute(PASSWORD_CHANGE_DTO, passwordChangeDto);
            addUserDisplayInfo(model, authentication);
            model.addAttribute(IS_FORCED, true);
            return CHANGE_PASSWORD_VIEW;
        }

        // Check if passwords match
        if (!passwordChangeDto.getNewPassword().equals(passwordChangeDto.getConfirmNewPassword())) {
            model.addAttribute("errorMessage", "New passwords do not match");
            model.addAttribute(PASSWORD_CHANGE_DTO, passwordChangeDto);
            addUserDisplayInfo(model, authentication);
            model.addAttribute(IS_FORCED, true);
            return CHANGE_PASSWORD_VIEW;
        }

        try {
            // Change the password
            userService.changePassword(username, passwordChangeDto.getNewPassword(), true);
            logger.info("Password successfully changed for user '{}'", username);

            // Remove the forced password update and expired password flags
            session.removeAttribute("requiresPasswordUpdate");
            session.removeAttribute("passwordExpired");

            // Add success message
            redirectAttributes.addFlashAttribute("successMessage",
                    "Password changed successfully!");

            // Redirect to profile if accessed from My Profile, otherwise to task list
            return "redirect:/profile/edit";

        } catch (Exception e) {
            logger.error("Error changing password for user '{}': {}", username, e.getMessage(), e);
            model.addAttribute("errorMessage", "Failed to change password: " + e.getMessage());
            model.addAttribute(PASSWORD_CHANGE_DTO, passwordChangeDto);
            addUserDisplayInfo(model, authentication);
            model.addAttribute(IS_FORCED, true);
            return CHANGE_PASSWORD_VIEW;
        }
    }

    /**
     * Handle password reset via email token
     */
    private String handlePasswordReset(String token, PasswordChangeDto passwordChangeDto,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {

        // Validate token
        Optional<String> emailOpt = passwordResetService.validateToken(token);
        if (emailOpt.isEmpty()) {
            logger.warn("Password reset attempted with invalid or expired token");
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Invalid or expired reset link. Please request a new one.");
            return "redirect:/reset-password?error";
        }

        String email = emailOpt.get();
        Optional<User> userOpt = userService.getUserByEmail(email);

        if (userOpt.isEmpty()) {
            logger.error("User not found for email during password reset: {}", email);
            return "redirect:/reset-password?error";
        }

        User user = userOpt.get();
        String username = user.getUsername();

        // Validate form
        if (bindingResult.hasErrors()) {
            model.addAttribute(PASSWORD_CHANGE_DTO, passwordChangeDto);
            model.addAttribute(IS_PASSWORD_RESET, true);
            model.addAttribute("resetToken", token);
            model.addAttribute("userDisplayName", user.getFirstname() + " " + user.getLastname());
            return CHANGE_PASSWORD_VIEW;
        }

        // Check if passwords match
        if (!passwordChangeDto.isPasswordMatching()) {
            bindingResult.rejectValue("confirmNewPassword", "error.confirmNewPassword",
                    "Passwords do not match");
            model.addAttribute(PASSWORD_CHANGE_DTO, passwordChangeDto);
            model.addAttribute(IS_PASSWORD_RESET, true);
            model.addAttribute("resetToken", token);
            model.addAttribute("userDisplayName", user.getFirstname() + " " + user.getLastname());
            return CHANGE_PASSWORD_VIEW;
        }

        try {
            // Change the password
            userService.changePassword(username, passwordChangeDto.getNewPassword(), false);
            logger.info("Password successfully reset for user '{}' via email token", username);

            // Consume the token (single use)
            passwordResetService.consumeToken(token);

            // Send confirmation email
            if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
                emailService.sendPasswordChangedConfirmation(user.getEmail(), username,
                        user.getFirstname() + " " + user.getLastname());
            }

            // Redirect to login with success message
            redirectAttributes.addFlashAttribute("successMessage",
                    "Password changed successfully! Please log in with your new password.");
            return "redirect:/login?resetSuccess";

        } catch (Exception e) {
            logger.error("Error resetting password for user '{}': {}", username, e.getMessage(), e);
            model.addAttribute("errorMessage", "Failed to reset password: " + e.getMessage());
            model.addAttribute(PASSWORD_CHANGE_DTO, passwordChangeDto);
            model.addAttribute(IS_PASSWORD_RESET, true);
            model.addAttribute("resetToken", token);
            model.addAttribute("userDisplayName", user.getFirstname() + " " + user.getLastname());
            return CHANGE_PASSWORD_VIEW;
        }
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
