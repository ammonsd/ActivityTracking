package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.service.EmailService;
import com.ammons.taskactivity.service.UserDropdownAccessService;
import com.ammons.taskactivity.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

/**
 * Description: Controller for the admin "Notify Users" feature. Allows administrators to select one
 * or more active users and send each a profile notification email containing their username, name,
 * company, temporary password (when applicable), and explicitly assigned task/expense clients and
 * projects.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since February 2026
 */
@Controller
@RequestMapping("/task-activity/manage-users/notify")
public class UserNotificationController {

    private static final Logger logger = LoggerFactory.getLogger(UserNotificationController.class);
    private static final String REDIRECT_NOTIFY = "redirect:/task-activity/manage-users/notify";
    private static final String SUCCESS_MESSAGE = "successMessage";
    private static final String ERROR_MESSAGE = "errorMessage";

    private final UserService userService;
    private final UserDropdownAccessService userDropdownAccessService;
    private final EmailService emailService;

    @Value("${spring.mail.enabled:false}")
    private boolean mailEnabled;

    public UserNotificationController(UserService userService,
            UserDropdownAccessService userDropdownAccessService, EmailService emailService) {
        this.userService = userService;
        this.userDropdownAccessService = userDropdownAccessService;
        this.emailService = emailService;
    }

    /**
     * Displays the user notification selection page. Loads all active users who have an email
     * address, with an optional last-name prefix filter.
     *
     * @param lastNameFilter optional last name prefix; blank means show all eligible users
     * @param model the view model
     * @param authentication the current admin session
     * @return the admin/user-notify template name
     */
    @GetMapping
    public String showNotifyPage(
            @RequestParam(required = false, defaultValue = "") String lastNameFilter, Model model,
            Authentication authentication) {

        logger.info("Admin {} accessing user notification page", authentication.getName());

        List<User> users = userService.getActiveUsersWithEmail(lastNameFilter);

        model.addAttribute("users", users);
        model.addAttribute("lastNameFilter", lastNameFilter);
        model.addAttribute("mailEnabled", mailEnabled);
        addDisplayInfo(model, authentication);

        return "admin/user-notify";
    }

    /**
     * Processes the notification form submission. Iterates over each selected username, loads the
     * user's profile and access assignments, and sends a profile notification email. Skips users
     * who cannot be found or who have no email address.
     *
     * @param selectedUsernames list of usernames chosen by the admin
     * @param lastNameFilter filter value to preserve on redirect
     * @param redirectAttributes flash attributes for the redirect response
     * @param authentication the current admin session
     * @return redirect back to the notification page
     */
    @PostMapping
    public String sendNotifications(@RequestParam(required = false) List<String> selectedUsernames,
            @RequestParam(required = false, defaultValue = "") String lastNameFilter,
            RedirectAttributes redirectAttributes, Authentication authentication) {

        logger.info("Admin {} sending profile notifications", authentication.getName());

        if (selectedUsernames == null || selectedUsernames.isEmpty()) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "No users were selected.");
            return REDIRECT_NOTIFY
                    + (lastNameFilter.isBlank() ? "" : "?lastNameFilter=" + lastNameFilter);
        }

        int sent = 0;
        int skipped = 0;

        for (String username : selectedUsernames) {
            Optional<User> userOpt = userService.getUserByUsername(username);
            if (userOpt.isEmpty()) {
                logger.warn("Skipping notification for unknown username: {}", username);
                skipped++;
                continue;
            }

            User user = userOpt.get();
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                logger.warn("Skipping notification for user {} — no email address", username);
                skipped++;
                continue;
            }

            List<String> taskClients =
                    userDropdownAccessService.getExplicitTaskClientNames(username);
            List<String> taskProjects =
                    userDropdownAccessService.getExplicitTaskProjectNames(username);
            List<String> expenseClients =
                    userDropdownAccessService.getExplicitExpenseClientNames(username);
            List<String> expenseProjects =
                    userDropdownAccessService.getExplicitExpenseProjectNames(username);

            emailService.sendUserProfileNotification(user, taskClients, taskProjects,
                    expenseClients, expenseProjects);
            sent++;
        }

        String message = String.format("Profile notification sent to %d user(s).", sent);
        if (skipped > 0) {
            message +=
                    String.format(" %d user(s) skipped (no email address or not found).", skipped);
        }
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, message);

        return REDIRECT_NOTIFY
                + (lastNameFilter.isBlank() ? "" : "?lastNameFilter=" + lastNameFilter);
    }

    /**
     * Adds the authenticated admin's display name to the model for the navbar.
     */
    private void addDisplayInfo(Model model, Authentication authentication) {
        if (authentication != null) {
            String username = authentication.getName();
            model.addAttribute("username", username);

            userService.getUserByUsername(username).ifPresent(user -> {
                String firstname = user.getFirstname() != null ? user.getFirstname() : "";
                String lastname = user.getLastname() != null ? user.getLastname() : "";
                String displayName = (firstname + " " + lastname + " (" + username + ")").trim();
                displayName = displayName.replaceAll("\\s+", " ");
                model.addAttribute("userDisplayName", displayName);
            });
        }
    }
}
