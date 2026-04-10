package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.PasswordChangeDto;
import com.ammons.taskactivity.dto.UserCreateDto;
import com.ammons.taskactivity.dto.UserEditDto;
import com.ammons.taskactivity.entity.Roles;
import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.repository.RoleRepository;
import com.ammons.taskactivity.service.DropdownValueService;
import com.ammons.taskactivity.service.EmailService;
import com.ammons.taskactivity.service.UserDropdownAccessService;
import com.ammons.taskactivity.service.UserService;
import com.ammons.taskactivity.service.TaskActivityService;
import com.ammons.taskactivity.service.PasswordExpirationNotificationService;
import jakarta.validation.Valid;
import com.ammons.taskactivity.security.RequirePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * UserManagementController
 *
 * Modified by: Dean Ammons - April 2026 Change: Replaced static TEMP_PASSWORD constant with
 * generateTempPassword() using SecureRandom Reason: Predictable static temp password was a security
 * risk; each admin action now produces a unique cryptographically random credential
 *
 * @author Dean Ammons
 * @version 1.0
 * @since December 2025
 */
@Controller
@RequestMapping("/task-activity/manage-users")
public class UserManagementController {

    private static final Logger logger = LoggerFactory.getLogger(UserManagementController.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
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
    private static final String ADMIN_USER_ACCESS = "admin/user-access-management";

    private final UserService userService;
    private final TaskActivityService taskActivityService;
    private final RoleRepository roleRepository;
    private final PasswordExpirationNotificationService passwordExpirationNotificationService;
    private final UserDropdownAccessService userDropdownAccessService;
    private final DropdownValueService dropdownValueService;
    private final EmailService emailService;

    public UserManagementController(UserService userService,
            TaskActivityService taskActivityService, RoleRepository roleRepository,
            PasswordExpirationNotificationService passwordExpirationNotificationService,
            UserDropdownAccessService userDropdownAccessService,
            DropdownValueService dropdownValueService, EmailService emailService) {
        this.userService = userService;
        this.taskActivityService = taskActivityService;
        this.roleRepository = roleRepository;
        this.passwordExpirationNotificationService = passwordExpirationNotificationService;
        this.userDropdownAccessService = userDropdownAccessService;
        this.dropdownValueService = dropdownValueService;
        this.emailService = emailService;
    }

    /**
     * Display the user management page with list of all users
     */
    @GetMapping
    @RequirePermission(resource = "USER_MANAGEMENT", action = "READ")
    public String manageUsers(@RequestParam(required = false) String username,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String company, Model model,
            Authentication authentication) {
        logger.info("Admin {} accessing user management", authentication.getName());

        List<User> users;

        // Check if any filter is applied
        if ((username != null && !username.trim().isEmpty())
                || (role != null && !role.trim().isEmpty())
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
        model.addAttribute(ROLES, roleRepository.findAll());

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

        UserCreateDto userCreateDto = new UserCreateDto();
        String tempPassword = generateTempPassword();
        userCreateDto.setPassword(tempPassword);
        userCreateDto.setConfirmPassword(tempPassword);
        model.addAttribute("userCreateDto", userCreateDto);
        model.addAttribute(ROLES, roleRepository.findAll());
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
            model.addAttribute(ROLES, roleRepository.findAll());
            addUserDisplayInfo(model, authentication);
            return ADMIN_USER_ADD;
        }

        // Check if passwords match
        if (!userCreateDto.isPasswordMatching()) {
            bindingResult.rejectValue("confirmPassword", "error.confirmPassword",
                    "Passwords do not match");
            model.addAttribute(ROLES, roleRepository.findAll());
            addUserDisplayInfo(model, authentication);
            return ADMIN_USER_ADD;
        }

        try {
            Roles selectedRole = roleRepository.findById(userCreateDto.getRoleId())
                    .orElseThrow(() -> new IllegalArgumentException("Selected role not found"));
            User newUser = userService.createUser(userCreateDto.getUsername(),
                    userCreateDto.getFirstname(), userCreateDto.getLastname(),
                    userCreateDto.getCompany(), userCreateDto.getEmail(),
                    userCreateDto.getPassword(), selectedRole,
                    userCreateDto.isForcePasswordUpdate());
            logger.info("Admin {} successfully created user: {}", authentication.getName(),
                    newUser.getUsername());
            // Welcome email is deferred until after User Access Management is configured.
            // Redirect admin directly to the access management page for this new user.
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE,
                    USER_PREFIX + newUser.getUsername()
                            + "' created. Please configure access below.");
            return "redirect:/task-activity/manage-users/access/" + newUser.getUsername()
                    + "?newUser=true";
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to create user: {}", e.getMessage());
            bindingResult.rejectValue(USERNAME, "error.username", e.getMessage());
            model.addAttribute(ROLES, roleRepository.findAll());
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
                new UserEditDto(user.getId(), user.getUsername(),
                        user.getRole() != null ? user.getRole().getName() : null, user.isEnabled(),
                        user.isForcePasswordUpdate());
        userEditDto.setFirstname(user.getFirstname());
        userEditDto.setLastname(user.getLastname());
        userEditDto.setCompany(user.getCompany());
        userEditDto.setEmail(user.getEmail());
        userEditDto.setAccountLocked(user.isAccountLocked());
        userEditDto.setFailedLoginAttempts(user.getFailedLoginAttempts());
        userEditDto.setWeekStartDay(
                user.getWeekStartDay() != null ? user.getWeekStartDay() : "MONDAY");

        model.addAttribute("userEditDto", userEditDto);
        model.addAttribute(ROLES, roleRepository.findAll());
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
            model.addAttribute(ROLES, roleRepository.findAll());
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
            user.setWeekStartDay(userEditDto.getWeekStartDay());

            // Convert role String to Roles entity
            Roles role = roleRepository.findByName(userEditDto.getRole()).orElseThrow(
                    () -> new IllegalArgumentException("Invalid role: " + userEditDto.getRole()));
            user.setRole(role);

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
            model.addAttribute(ROLES, roleRepository.findAll());
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
     * Show the access assignment page for a given user. TASK tab loads TASK/CLIENT and TASK/PROJECT
     * values; EXPENSE tab loads EXPENSE/CLIENT and EXPENSE/PROJECT values. Both sets share the same
     * assignedIds set since the underlying user_dropdown_access table distinguishes rows by the ID
     * of the dropdown value, which carries its own category.
     *
     * @param username the username whose access is being managed
     * @param view which tab to show: "TASK" (default) or "EXPENSE"
     */
    @GetMapping("/access/{username}")
    @RequirePermission(resource = "USER_MANAGEMENT", action = "UPDATE")
    public String showAccessForm(@PathVariable String username,
            @RequestParam(value = "view", defaultValue = "TASK") String view,
            @RequestParam(value = "newUser", defaultValue = "false") boolean newUser, Model model,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        logger.info("Admin {} accessing dropdown access form for user: {} (view={}, newUser={})",
                authentication.getName(), username, view, newUser);

        if (userService.getUserByUsername(username).isEmpty()) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, USER_NOT_FOUND);
            return REDIRECT_MANAGE_USERS;
        }

        Set<Long> assignedIds = userDropdownAccessService.getAssignedDropdownValueIds(username);

        model.addAttribute("targetUsername", username);
        model.addAttribute("allClients", dropdownValueService.getActiveClients());
        model.addAttribute("allProjects", dropdownValueService.getActiveProjects());
        model.addAttribute("allExpenseClients", dropdownValueService.getActiveExpenseClients());
        model.addAttribute("allExpenseProjects", dropdownValueService.getActiveExpenseProjects());
        model.addAttribute("assignedIds", assignedIds);
        model.addAttribute("currentView", view);
        model.addAttribute("newUser", newUser);
        addUserDisplayInfo(model, authentication);

        return ADMIN_USER_ACCESS;
    }

    /**
     * Save the access assignments for a given user. When view=TASK, replaces TASK/CLIENT and
     * TASK/PROJECT rows. When view=EXPENSE, replaces EXPENSE/CLIENT and EXPENSE/PROJECT rows.
     * Saving one tab never touches the other tab's assignments.
     *
     * @param username the username whose access is being updated
     * @param view which tab was active: "TASK" or "EXPENSE"
     * @param clientIds selected TASK client IDs (TASK view)
     * @param projectIds selected TASK project IDs (TASK view)
     * @param expenseClientIds selected EXPENSE client IDs (EXPENSE view)
     * @param expenseProjectIds selected EXPENSE project IDs (EXPENSE view)
     */
    @PostMapping("/access/{username}")
    @RequirePermission(resource = "USER_MANAGEMENT", action = "UPDATE")
    public String saveAccessAssignments(@PathVariable String username,
            @RequestParam(value = "view", defaultValue = "TASK") String view,
            @RequestParam(value = "newUser", defaultValue = "false") boolean newUser,
            @RequestParam(value = "clientIds", required = false) List<Long> clientIds,
            @RequestParam(value = "projectIds", required = false) List<Long> projectIds,
            @RequestParam(value = "expenseClientIds", required = false) List<Long> expenseClientIds,
            @RequestParam(value = "expenseProjectIds",
                    required = false) List<Long> expenseProjectIds,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        logger.info("Admin {} saving dropdown access for user: {} (view={}, newUser={})",
                authentication.getName(), username, view, newUser);

        Optional<User> userOptional = userService.getUserByUsername(username);
        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, USER_NOT_FOUND);
            return REDIRECT_MANAGE_USERS;
        }

        if ("EXPENSE".equals(view)) {
            userDropdownAccessService.saveExpenseClientAssignments(username,
                    expenseClientIds != null ? expenseClientIds : List.of());
            userDropdownAccessService.saveExpenseProjectAssignments(username,
                    expenseProjectIds != null ? expenseProjectIds : List.of());
            logger.info("Admin {} updated EXPENSE access for user: {} (clients={}, projects={})",
                    authentication.getName(), username,
                    expenseClientIds != null ? expenseClientIds.size() : 0,
                    expenseProjectIds != null ? expenseProjectIds.size() : 0);
        } else {
            userDropdownAccessService.saveClientAssignments(username,
                    clientIds != null ? clientIds : List.of());
            userDropdownAccessService.saveProjectAssignments(username,
                    projectIds != null ? projectIds : List.of());
            logger.info("Admin {} updated TASK access for user: {} (clients={}, projects={})",
                    authentication.getName(), username, clientIds != null ? clientIds.size() : 0,
                    projectIds != null ? projectIds.size() : 0);
        }

        // If this is a new user, redirect back to the access page so the admin can configure
        // the other tab and then send the welcome email manually.
        if (newUser) {
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, USER_PREFIX + username
                    + "' access saved. Configure the other tab, then send the welcome email.");
            return "redirect:/task-activity/manage-users/access/" + username + "?view="
                    + ("EXPENSE".equals(view) ? "TASK" : "EXPENSE") + "&newUser=true";
        }

        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE,
                USER_PREFIX + username + "' access assignments updated successfully");
        return REDIRECT_MANAGE_USERS;
    }

    /**
     * Sends the welcome email for a newly created user after both Task and Expense access have been
     * configured. Called from the new-user setup banner in the Thymeleaf access page.
     *
     * @param username the username of the newly created user
     * @param authentication the authenticated admin performing the operation
     */
    @PostMapping("/welcome-email/{username}")
    @RequirePermission(resource = "USER_MANAGEMENT", action = "UPDATE")
    public String sendWelcomeEmailForNewUser(@PathVariable String username,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        logger.info("Admin {} sending welcome email for new user: {}", authentication.getName(),
                username);

        Optional<User> userOptional = userService.getUserByUsername(username);
        if (userOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, USER_NOT_FOUND);
            return REDIRECT_MANAGE_USERS;
        }

        User user = userOptional.get();
        String roleName = user.getRole() != null ? user.getRole().getName() : "";
        boolean includeTask = !roleName.contains("EXPENSES");
        boolean includeExpense = !roleName.contains("TASKS");
        List<String> taskClients = List.of();
        List<String> taskProjects = List.of();
        List<String> expClients = List.of();
        List<String> expProjects = List.of();
        if (includeTask) {
            List<String> explicitClients =
                    userDropdownAccessService.getExplicitTaskClientNames(username);
            List<String> allUsersClients = userDropdownAccessService.getAllUsersTaskClientNames();
            taskClients = Stream.concat(explicitClients.stream(), allUsersClients.stream())
                    .distinct().toList();
            taskProjects = userDropdownAccessService.getExplicitTaskProjectNames(username);
        }
        if (includeExpense) {
            List<String> explicitClients =
                    userDropdownAccessService.getExplicitExpenseClientNames(username);
            List<String> allUsersClients =
                    userDropdownAccessService.getAllUsersExpenseClientNames();
            expClients = Stream.concat(explicitClients.stream(), allUsersClients.stream())
                    .distinct().toList();
            expProjects = userDropdownAccessService.getExplicitExpenseProjectNames(username);
        }
        try {
            emailService.sendNewUserWelcomeEmail(user, authentication.getName(), taskClients,
                    taskProjects, expClients, expProjects);
            logger.info("Welcome email sent for new user {} after access configuration", username);
            redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE,
                    USER_PREFIX + username + "' welcome email sent successfully");
        } catch (Exception e) {
            logger.warn("Failed to send welcome email for new user {}: {}", username,
                    e.getMessage());
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE,
                    "Access saved but welcome email failed: " + e.getMessage());
        }
        return REDIRECT_MANAGE_USERS;
    }

    /**
     * Toggle the allUsers flag on a dropdown value. Values with allUsers=true are visible to every
     * user without requiring an explicit access assignment. Redirects back to the same tab.
     *
     * @param username the username context (used for redirect back to the access page)
     * @param valueId the dropdown value ID to toggle
     * @param view the active tab (TASK or EXPENSE) to restore after redirect
     */
    @PostMapping("/access/{username}/toggle-all-users/{valueId}")
    @RequirePermission(resource = "USER_MANAGEMENT", action = "UPDATE")
    public String toggleAllUsersFlag(@PathVariable String username, @PathVariable Long valueId,
            @RequestParam(value = "view", defaultValue = "TASK") String view,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        logger.info("Admin {} toggling allUsers flag for dropdown value ID: {}",
                authentication.getName(), valueId);

        try {
            dropdownValueService.toggleAllUsers(valueId);
        } catch (RuntimeException e) {
            logger.warn("Failed to toggle allUsers for value {}: {}", valueId, e.getMessage());
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE,
                    "Could not update value: " + e.getMessage());
        }

        return "redirect:/task-activity/manage-users/access/" + username + "?view=" + view;
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

        // Pre-fill the temporary password and check Force Password when changing another user's
        // password
        if (!user.getUsername().equals(authentication.getName())) {
            String tempPassword = generateTempPassword();
            passwordChangeDto.setNewPassword(tempPassword);
            passwordChangeDto.setConfirmNewPassword(tempPassword);
            passwordChangeDto.setForcePasswordUpdate(true);
        }

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
            // Change password but don't clear force update flag (clearForceUpdate=false)
            userService.changePassword(passwordChangeDto.getUsername(),
                    passwordChangeDto.getNewPassword(), false);

            // Then set the force password update flag based on admin's choice
            if (passwordChangeDto.isForcePasswordUpdate()) {
                user.setForcePasswordUpdate(true);
                userService.updateUser(user);
                logger.info("Admin {} set force password update for user: {}",
                        authentication.getName(), passwordChangeDto.getUsername());
            }

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
     * Generates a cryptographically random temporary password that satisfies the application's
     * complexity requirements: uppercase, lowercase, digit, and special character.
     */
    private static String generateTempPassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghjkmnpqrstuvwxyz";
        String digits = "23456789";
        String special = "+&%$#@!~*";
        String all = upper + lower + digits + special;

        char[] password = new char[16];
        // Guarantee at least one character from each required group
        password[0] = upper.charAt(SECURE_RANDOM.nextInt(upper.length()));
        password[1] = lower.charAt(SECURE_RANDOM.nextInt(lower.length()));
        password[2] = digits.charAt(SECURE_RANDOM.nextInt(digits.length()));
        password[3] = special.charAt(SECURE_RANDOM.nextInt(special.length()));
        for (int i = 4; i < password.length; i++) {
            password[i] = all.charAt(SECURE_RANDOM.nextInt(all.length()));
        }
        // Shuffle so the guaranteed characters are not always at fixed positions
        for (int i = password.length - 1; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            char tmp = password[i];
            password[i] = password[j];
            password[j] = tmp;
        }
        return new String(password);
    }

    /**
     * Export all users as CSV
     */
    @GetMapping("/export-csv")
    @ResponseBody
    public String exportUsersToCsv(@RequestParam(required = false) String username,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String company) {

        List<User> users;

        // Apply filters if provided
        if ((username != null && !username.trim().isEmpty())
                || (role != null && !role.trim().isEmpty())
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

    /**
     * Manually trigger password expiration check and send notifications (ADMIN only). This endpoint
     * is useful for testing the notification system without waiting for the scheduled run.
     */
    @PostMapping("/trigger-password-expiration-check")
    @RequirePermission(resource = "USER_MANAGEMENT", action = "ADMIN")
    @ResponseBody
    public Map<String, Object> triggerPasswordExpirationCheck(Authentication authentication) {
        logger.info("Admin {} manually triggered password expiration check",
                authentication.getName());

        try {
            passwordExpirationNotificationService.checkExpiringPasswordsAndNotify();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password expiration check completed successfully");
            return response;
        } catch (Exception e) {
            logger.error("Error during manual password expiration check", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return response;
        }
    }
}

