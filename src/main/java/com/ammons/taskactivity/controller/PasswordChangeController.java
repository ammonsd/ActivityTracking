package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.PasswordChangeDto;
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

/**
 * PasswordChangeController
 *
 * @author Dean Ammons
 * @version 1.0
 */
@Controller
public class PasswordChangeController {

    private static final Logger logger = LoggerFactory.getLogger(PasswordChangeController.class);
    private static final String PASSWORD_CHANGE_DTO = "passwordChangeDto";
    private static final String IS_FORCED = "isForced";
    private static final String CHANGE_PASSWORD_VIEW = "change-password";

    private final UserService userService;

    public PasswordChangeController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Show the password change form
     */
    @GetMapping("/change-password")
    public String showPasswordChangeForm(
            @RequestParam(value = "forced", required = false) String forced, Model model,
            Authentication authentication, HttpSession session) {

        if (authentication == null) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        logger.info("User '{}' accessing password change form", username);

        // Check if this is a forced password update
        boolean isForced = "true".equals(forced)
                || Boolean.TRUE.equals(session.getAttribute("requiresPasswordUpdate"));

        // Create DTO and set the username
        PasswordChangeDto passwordChangeDto = new PasswordChangeDto();
        passwordChangeDto.setUsername(username);

        model.addAttribute(PASSWORD_CHANGE_DTO, passwordChangeDto);
        addUserDisplayInfo(model, authentication);
        model.addAttribute(IS_FORCED, isForced);

        return CHANGE_PASSWORD_VIEW;
    }

    /**
     * Process the password change form submission
     */
    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute PasswordChangeDto passwordChangeDto,
            BindingResult bindingResult, Model model, Authentication authentication,
            HttpSession session, RedirectAttributes redirectAttributes) {

        if (authentication == null) {
            return "redirect:/login";
        }

        String username = authentication.getName();
        logger.info("User '{}' attempting to change password", username);

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

            // Remove the forced password update flag
            session.removeAttribute("requiresPasswordUpdate");

            // Add success message
            redirectAttributes.addFlashAttribute("successMessage",
                    "Password changed successfully!");

            // Redirect to task list after successful password change
            return "redirect:/task-activity/list";

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
