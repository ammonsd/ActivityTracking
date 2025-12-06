package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.PasswordChangeDto;
import com.ammons.taskactivity.dto.UserCreateDto;
import com.ammons.taskactivity.dto.UserEditDto;
import com.ammons.taskactivity.entity.Role;
import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.service.UserService;
import com.ammons.taskactivity.service.TaskActivityService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

/**
 * UserManagementController
 *
 * @author Dean Ammons
 * @version 1.0
 */
@Controller
@RequestMapping("/task-activity/manage-users")
@PreAuthorize("hasRole('ADMIN')")
public class UserManagementController {

    private static final Logger logger = LoggerFactory.getLogger(UserManagementController.class);
    private static final String USERNAME = "username";
    private static final String ROLES = "roles";
    private static final String ADMIN_USER_ADD = "admin/user-add";
    private static final String SUCCESS_MESSAGE = "successMessage";
    private static final String USER_PREFIX = "User '";
    private static final String REDIRECT_MANAGE_USERS = "redirect:/task-activity/manage-users";
    private static final String USER_NOT_FOUND = "User not found";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String ADMIN_USER_EDIT = "admin/user-edit";
    private static final String TARGET_USER = "targetUser";
    private static final String ADMIN_USER_CHANGE_PASSWORD = "admin/user-change-password";

    private final UserService userService;
    private final TaskActivityService taskActivityService;

    public UserManagementController(UserService userService,
            TaskActivityService taskActivityService) {
        this.userService = userService;
        this.taskActivityService = taskActivityService;
    }

    /**
     * Display the user management page with list of all users
     */
    @GetMapping
    public String manageUsers(@RequestParam(required = false) String username,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String company, Model model,
            Authentication authentication) {
        logger.info("Admin {} accessing user management", authentication.getName());

        List<User> users;

        // Check if any filter is applied
        if ((username != null && !username.trim().isEmpty()) || role != null
                || (company != null && !company.trim().isEmpty())) {
            users = userService.filterUsers(username, role, company);
            logger.info("Filtered users: {} results", users.size());
        } else {
            users = userService.getAllUsers();
        }

        model.addAttribute("users", users);

        // Add filter values back to the model to preserve them in the form
        model.addAttribute("filterUsername", username != null ? username : "");
        model.addAttribute("filterRole", role);
        model.addAttribute("filterCompany", company != null ? company : "");

        // Add roles for the dropdown filter
        model.addAttribute(ROLES, Role.values());

        // Create a map to track which users have task activities
        Map<String, Boolean> userHasTasks = new HashMap<>();
        for (User user : users) {
            userHasTasks.put(user.getUsername(),
                    taskActivityService.userHasTaskActivities(user.getUsername()));
        }
        model.addAttribute("userHasTasks", userHasTasks);

        addUserDisplayInfo(model, authentication);

        return "admin/user-management";
    }

    /**
     * Show the add user form
     */
    @GetMapping("/add")
    public String showAddUserForm(Model model, Authentication authentication) {
        logger.info("Admin {} accessing add user form", authentication.getName());

        model.addAttribute("userCreateDto", new UserCreateDto());
        model.addAttribute(ROLES, Role.values());
        addUserDisplayInfo(model, authentication);

        return ADMIN_USER_ADD;
    }

    /**
     * Process the add user form submission
     */
    @PostMapping("/add")
    public String addUser(@Valid @ModelAttribute UserCreateDto userCreateDto,
            BindingResult bindingResult, Model model, Authentication authentication,
            RedirectAttributes redirectAttributes) {

        logger.info("Admin {} attempting to create new user: {}", authentication.getName(),
                userCreateDto.getUsername());

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            model.addAttribute(ROLES, Role.values());
            addUserDisplayInfo(model, authentication);
            return ADMIN_USER_ADD;
        }

        // Check if passwords match
        if (!userCreateDto.isPasswordMatching()) {
            bindingResult.rejectValue("confirmPassword", "error.confirmPassword",
                    "Passwords do not match");
            model.addAttribute(ROLES, Role.values());
            addUserDisplayInfo(model, authentication);
            return ADMIN_USER_ADD;
        }

        try {
            User newUser = userService.createUser(userCreateDto.getUsername(),
                    userCreateDto.getFirstname(), userCreateDto.getLastname(),
                    userCreateDto.getCompany(), userCreateDto.getEmail(),
                    userCreateDto.getPassword(), userCreateDto.getRole(),
                    userCreateDto.isForcePasswordUpdate());
            logger.info("Admin {} successfully created user: {}", authentication.getName(),
                    newUser.getUsername());
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE,
                    USER_PREFIX + newUser.getUsername() + "' created successfully");
            return REDIRECT_MANAGE_USERS;
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to create user: {}", e.getMessage());
            bindingResult.rejectValue(USERNAME, "error.username", e.getMessage());
            model.addAttribute(ROLES, Role.values());
            addUserDisplayInfo(model, authentication);
            return ADMIN_USER_ADD;
        }
    }

    /**
     * Show the edit user form
     */
    @GetMapping("/edit/{id}")
    public String showEditUserForm(@PathVariable Long id, Model model,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        logger.info("Admin {} accessing edit form for user ID: {}", authentication.getName(), id);

        Optional<User> userOptional = userService.getUserById(id);
        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, USER_NOT_FOUND);
            return REDIRECT_MANAGE_USERS;
        }

        User user = userOptional.get();
        UserEditDto userEditDto =
                new UserEditDto(user.getId(), user.getUsername(), user.getRole(), user.isEnabled(),
                        user.isForcePasswordUpdate());
        userEditDto.setFirstname(user.getFirstname());
        userEditDto.setLastname(user.getLastname());
        userEditDto.setCompany(user.getCompany());
        userEditDto.setEmail(user.getEmail());
        userEditDto.setAccountLocked(user.isAccountLocked());
        userEditDto.setFailedLoginAttempts(user.getFailedLoginAttempts());

        model.addAttribute("userEditDto", userEditDto);
        model.addAttribute(ROLES, Role.values());
        model.addAttribute("isOwnProfile", false);
        addUserDisplayInfo(model, authentication);

        return ADMIN_USER_EDIT;
    }

    /**
     * Process the edit user form submission
     */
    @PostMapping("/edit/{id}")
    public String editUser(@PathVariable Long id, @Valid @ModelAttribute UserEditDto userEditDto,
            BindingResult bindingResult, Model model, Authentication authentication,
            RedirectAttributes redirectAttributes) {

        logger.info("Admin {} attempting to edit user ID: {}", authentication.getName(), id);

        if (bindingResult.hasErrors()) {
            model.addAttribute(ROLES, Role.values());
            addUserDisplayInfo(model, authentication);
            return ADMIN_USER_EDIT;
        }

        try {
            Optional<User> userOptional = userService.getUserById(id);
            if (userOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, USER_NOT_FOUND);
                return REDIRECT_MANAGE_USERS;
            }

            User user = userOptional.get();
            // Username is intentionally not updated - usernames are immutable
            user.setFirstname(userEditDto.getFirstname());
            user.setLastname(userEditDto.getLastname());
            user.setCompany(userEditDto.getCompany());
            user.setEmail(userEditDto.getEmail());
            user.setRole(userEditDto.getRole());
            user.setEnabled(userEditDto.isEnabled());
            user.setForcePasswordUpdate(userEditDto.isForcePasswordUpdate());
            user.setAccountLocked(userEditDto.isAccountLocked());
            
            // If admin is unlocking the account, reset failed login attempts
            if (!userEditDto.isAccountLocked() && user.getFailedLoginAttempts() > 0) {
                logger.info("Admin {} unlocking account for user: {}", authentication.getName(),
                        user.getUsername());
                user.setFailedLoginAttempts(0);
            }

            User updatedUser = userService.updateUser(user);
            logger.info("Admin {} successfully updated user: {}", authentication.getName(),
                    updatedUser.getUsername());
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE,
                    USER_PREFIX + updatedUser.getUsername() + "' updated successfully");
            return REDIRECT_MANAGE_USERS;
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to update user: {}", e.getMessage());
            bindingResult.rejectValue(USERNAME, "error.username", e.getMessage());
            model.addAttribute(ROLES, Role.values());
            addUserDisplayInfo(model, authentication);
            return ADMIN_USER_EDIT;
        }
    }

    /**
     * Delete a user
     */
    @PostMapping("/delete/{id}")
    public String deleteUser(@PathVariable Long id, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        logger.info("Admin {} attempting to delete user ID: {}", authentication.getName(), id);

        try {
            Optional<User> userOptional = userService.getUserById(id);
            if (userOptional.isEmpty()) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE, USER_NOT_FOUND);
                return REDIRECT_MANAGE_USERS;
            }

            User user = userOptional.get();
            String username = user.getUsername();

            // Prevent admin from deleting themselves
            if (user.getUsername().equals(authentication.getName())) {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE,
                        "You cannot delete your own account");
                return REDIRECT_MANAGE_USERS;
            }

            userService.deleteUser(id);
            logger.info("Admin {} successfully deleted user: {}", authentication.getName(),
                    username);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE,
                    USER_PREFIX + username + "' deleted successfully");
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to delete user: {}", e.getMessage());
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
        }

        return REDIRECT_MANAGE_USERS;
    }

    /**
     * Show the change password form
     */
    @GetMapping("/change-password/{id}")
    public String showChangePasswordForm(@PathVariable Long id, Model model,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        logger.info("Admin {} accessing change password form for user ID: {}",
                authentication.getName(), id);

        Optional<User> userOptional = userService.getUserById(id);
        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, USER_NOT_FOUND);
            return REDIRECT_MANAGE_USERS;
        }

        User user = userOptional.get();
        PasswordChangeDto passwordChangeDto = new PasswordChangeDto();
        passwordChangeDto.setUsername(user.getUsername());

        model.addAttribute("passwordChangeDto", passwordChangeDto);
        model.addAttribute(TARGET_USER, user);
        addUserDisplayInfo(model, authentication);

        return ADMIN_USER_CHANGE_PASSWORD;
    }

    /**
     * Process the change password form submission
     */
    @PostMapping("/change-password/{id}")
    public String changePassword(@PathVariable Long id,
            @Valid @ModelAttribute PasswordChangeDto passwordChangeDto, BindingResult bindingResult,
            Model model, Authentication authentication, RedirectAttributes redirectAttributes) {

        logger.info("Admin {} attempting to change password for user: {}", authentication.getName(),
                passwordChangeDto.getUsername());

        Optional<User> userOptional = userService.getUserById(id);
        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, USER_NOT_FOUND);
            return REDIRECT_MANAGE_USERS;
        }

        User user = userOptional.get();

        if (bindingResult.hasErrors()) {
            model.addAttribute(TARGET_USER, user);
            addUserDisplayInfo(model, authentication);
            return ADMIN_USER_CHANGE_PASSWORD;
        }

        // Check if passwords match
        if (!passwordChangeDto.isPasswordMatching()) {
            bindingResult.rejectValue("confirmPassword", "error.confirmPassword",
                    "Passwords do not match");
            model.addAttribute(TARGET_USER, user);
            addUserDisplayInfo(model, authentication);
            return ADMIN_USER_CHANGE_PASSWORD;
        }

        try {
            userService.changePassword(passwordChangeDto.getUsername(),
                    passwordChangeDto.getNewPassword(), true);
            logger.info("Admin {} successfully changed password for user: {}",
                    authentication.getName(), passwordChangeDto.getUsername());
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE,
                    "Password changed successfully for " + USER_PREFIX
                            + passwordChangeDto.getUsername()
                            + "'");
            return REDIRECT_MANAGE_USERS;
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to change password: {}", e.getMessage());
            bindingResult.rejectValue("newPassword", "error.newPassword", e.getMessage());
            model.addAttribute(TARGET_USER, user);
            addUserDisplayInfo(model, authentication);
            return ADMIN_USER_CHANGE_PASSWORD;
        }
    }

    /**
     * Add user display information to the model
     */
    private void addUserDisplayInfo(Model model, Authentication authentication) {
        if (authentication != null) {
            String username = authentication.getName();
            model.addAttribute(USERNAME, username);

            // Fetch user details to display full name
            userService.getUserByUsername(username).ifPresent(user -> {
                String firstname = user.getFirstname() != null ? user.getFirstname() : "";
                String lastname = user.getLastname() != null ? user.getLastname() : "";
                String displayName = (firstname + " " + lastname + " (" + username + ")").trim();
                // Remove extra spaces if firstname is empty
                displayName = displayName.replaceAll("\\s+", " ");
                model.addAttribute("userDisplayName", displayName);
            });
        }
    }

    /**
     * Export all users as CSV
     */
    @GetMapping("/export-csv")
    @ResponseBody
    public String exportUsersToCsv(@RequestParam(required = false) String username,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) String company) {

        List<User> users;

        // Apply filters if provided
        if ((username != null && !username.trim().isEmpty()) || role != null
                || (company != null && !company.trim().isEmpty())) {
            users = userService.filterUsers(username, role, company);
        } else {
            users = userService.getAllUsers();
        }

        // Sort by username
        users.sort((u1, u2) -> u1.getUsername().compareToIgnoreCase(u2.getUsername()));

        return generateUserCsv(users);
    }

    private String generateUserCsv(List<User> users) {
        StringBuilder csv = new StringBuilder();

        // Header
        csv.append(
                "Username,First Name,Last Name,Company,Email,Role,Enabled,Force Password Update\n");

        // Data rows
        for (User user : users) {
            csv.append(escapeCsvField(user.getUsername())).append(",");
            csv.append(escapeCsvField(user.getFirstname() != null ? user.getFirstname() : ""))
                    .append(",");
            csv.append(escapeCsvField(user.getLastname() != null ? user.getLastname() : ""))
                    .append(",");
            csv.append(escapeCsvField(user.getCompany() != null ? user.getCompany() : ""))
                    .append(",");
            csv.append(escapeCsvField(user.getEmail() != null ? user.getEmail() : "")).append(",");
            csv.append(user.getRole()).append(",");
            csv.append(user.isEnabled()).append(",");
            csv.append(user.isForcePasswordUpdate());
            csv.append("\n");
        }

        return csv.toString();
    }

    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    /**
     * Display guest activity dashboard (ADMIN only) Shows login audit trail for guest account
     */
    @GetMapping("/guest-activity")
    public String guestActivity(Model model, Authentication authentication) {
        logger.info("Displaying guest activity dashboard");
        addUserDisplayInfo(model, authentication);
        return "admin/guest-activity";
    }
}

