package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.ApiResponse;
import com.ammons.taskactivity.dto.CurrentUserDto;
import com.ammons.taskactivity.dto.UserDto;
import com.ammons.taskactivity.dto.UserEditDto;
import com.ammons.taskactivity.entity.DropdownValue;
import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.entity.Roles;
import com.ammons.taskactivity.service.DropdownValueService;
import com.ammons.taskactivity.service.EmailService;
import com.ammons.taskactivity.service.UserDropdownAccessService;
import com.ammons.taskactivity.service.UserService;
import com.ammons.taskactivity.service.TaskActivityService;
import com.ammons.taskactivity.repository.RoleRepository;
import com.ammons.taskactivity.security.RequirePermission;
import com.ammons.taskactivity.validation.ValidPassword;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

/**
 * REST API Controller for User Management Used by Angular and React frontends
 *
 * Modified by: Dean Ammons - February 2026 Change: Added dropdown access management endpoints for
 * React admin Reason: Allow React admin to GET and PUT user dropdown access assignments
 *
 * Modified by: Dean Ammons - February 2026 Change: Added notify-eligible and notify endpoints
 * Reason: Expose Notify Users admin function to React Admin Dashboard
 *
 * @author Dean Ammons
 * @version 1.0
 * @since December 2025
 */
@RestController
@RequestMapping("/api/users")
public class UserRestController {

    private static final Logger logger = LoggerFactory.getLogger(UserRestController.class);
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_GUEST = "ROLE_GUEST";
    private static final String LOGIN_AUDIT_PERMISSION = "PERMISSION_LOGIN_AUDIT:READ";
    private static final int MAX_LOGIN_AUDIT_LIMIT = 200;
    private static final String USER_NOT_FOUND_MESSAGE = "User not found";

    private final UserService userService;
    private final TaskActivityService taskActivityService;
    private final RoleRepository roleRepository;
    private final UserDropdownAccessService userDropdownAccessService;
    private final DropdownValueService dropdownValueService;
    private final EmailService emailService;

    @Value("${spring.mail.enabled:false}")
    private boolean mailEnabled;

    public UserRestController(UserService userService, TaskActivityService taskActivityService,
            RoleRepository roleRepository, UserDropdownAccessService userDropdownAccessService,
            DropdownValueService dropdownValueService, EmailService emailService) {
        this.userService = userService;
        this.taskActivityService = taskActivityService;
        this.roleRepository = roleRepository;
        this.userDropdownAccessService = userDropdownAccessService;
        this.dropdownValueService = dropdownValueService;
        this.emailService = emailService;
    }

    /**
     * Get current authenticated user with profile details. Includes password expiration warnings if
     * applicable.
     * 
     * No permission check required - all authenticated users can view their own basic info.
     * 
     * @param authentication the authenticated user making the request
     * @return ResponseEntity containing the current user's profile data
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserDto>> getCurrentUser(
            Authentication authentication) {
        String username = authentication.getName();
        logger.debug("REST API: Getting current user: {}", username);
        return userService.getUserByUsername(username)
                .map(user -> {
                    CurrentUserDto dto = new CurrentUserDto(user.getId(), user.getUsername(),
                            user.getFirstname(), user.getLastname(), user.getCompany(),
                            user.getEmail(), user.getRole(), user.isEnabled());

                    // Add password expiration warning if applicable
                    if (userService.isPasswordExpiringSoon(username)) {
                        Long daysUntilExpiration = userService.getDaysUntilExpiration(username);
                        dto.setDaysUntilExpiration(daysUntilExpiration);
                        if (daysUntilExpiration != null) {
                            if (daysUntilExpiration == 0) {
                                dto.setPasswordExpiringWarning(
                                        "🔴 Your password expires TODAY. Please change it immediately!");
                            } else {
                                dto.setPasswordExpiringWarning(
                                        "⚠️ Your password will expire in " + daysUntilExpiration
                                                + " day" + (daysUntilExpiration == 1 ? "" : "s")
                                                + ". Please change it soon.");
                            }
                        }
                    }

                    return ResponseEntity.ok(ApiResponse.success("Current user", dto));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get current user's full profile (for editing). Returns the complete user entity for profile
     * editing.
     * 
     * Accessible by USER and ADMIN roles only. GUEST users are blocked.
     * 
     * @param authentication the authenticated user making the request
     * @return ResponseEntity containing the user's complete profile
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<User>> getCurrentUserProfile(Authentication authentication) {
        String username = authentication.getName();
        logger.debug("REST API: Getting profile for user: {}", username);

        // Block GUEST users from accessing profile
        if (authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(ROLE_GUEST))) {
            logger.warn("GUEST user '{}' attempted to access profile", username);
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Guest users cannot access profile settings"));
        }

        return userService.getUserByUsername(username)
                .map(user -> ResponseEntity.ok(ApiResponse.success("Profile retrieved", user)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update current user's profile. Non-admin users can only update their own profile fields:
     * firstname, lastname, company, email. Role and other security-related fields cannot be
     * modified.
     * 
     * Accessible by USER and ADMIN roles only. GUEST users are blocked.
     * 
     * @param profileUpdate the DTO containing updated profile data
     * @param authentication the authenticated user making the request
     * @return ResponseEntity containing the updated user profile
     */
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<User>> updateCurrentUserProfile(
            @RequestBody UserEditDto profileUpdate,
            Authentication authentication) {
        String username = authentication.getName();
        logger.debug("REST API: Updating profile for user: {}", username);

        // Block GUEST users from updating profile
        if (authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(ROLE_GUEST))) {
            logger.warn("GUEST user '{}' attempted to update profile", username);
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Guest users cannot modify profile settings"));
        }

        try {
            return userService.getUserByUsername(username).map(existingUser -> {
                // Non-admin users can only update specific profile fields
                existingUser.setFirstname(profileUpdate.getFirstname());
                existingUser.setLastname(profileUpdate.getLastname());
                existingUser.setCompany(profileUpdate.getCompany());
                existingUser.setEmail(profileUpdate.getEmail());

                User savedUser = userService.updateUser(existingUser);
                return ResponseEntity
                        .ok(ApiResponse.success("Profile updated successfully", savedUser));
            }).orElse(ResponseEntity.status(404).body(ApiResponse.error(USER_NOT_FOUND_MESSAGE)));
        } catch (Exception e) {
            logger.error("Error updating profile for user {}: {}", username, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Error updating profile: " + e.getMessage()));
        }
    }

    /**
     * Get all users in the system with optional filtering. Returns a complete list of users or
     * filtered list based on query parameters.
     * 
     * @param username optional username filter (partial match, case-insensitive)
     * @param role optional role filter (exact match)
     * @param company optional company filter (partial match, case-insensitive)
     * @return ResponseEntity containing list of filtered users
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "READ")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String company) {

        logger.debug("REST API: Getting users with filters - username: {}, role: {}, company: {}",
                username, role, company);

        List<User> users;

        // If any filter is provided, use filterUsers; otherwise get all users
        if ((username != null && !username.trim().isEmpty())
                || (role != null && !role.trim().isEmpty())
                || (company != null && !company.trim().isEmpty())) {
            users = userService.filterUsers(username, role, company);
        } else {
            users = userService.getAllUsers();
        }

        // Convert to DTOs and populate hasTasks field
        List<UserDto> userDtos = users.stream().map(user -> {
            UserDto dto = new UserDto(user);
            dto.setHasTasks(taskActivityService.userHasTaskActivities(user.getUsername()));
            return dto;
        }).toList();

        ApiResponse<List<UserDto>> response = ApiResponse
                .success("Users retrieved successfully", userDtos).withCount(userDtos.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Get user by ID. Returns the complete user entity for the specified user ID.
     * 
     * @param id the user ID to retrieve
     * @return ResponseEntity containing the user if found
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "READ")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long id) {
        logger.debug("REST API: Getting user with ID: {}", id);
        return userService.getUserById(id)
                .map(user -> ResponseEntity
                        .ok(ApiResponse.success("User found", new UserDto(user))))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create new user with validation
     * 
     * @param user the user entity with username, password, role, etc.
     * @return ResponseEntity containing the created user
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "CREATE")
    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(@RequestBody User user) {
        logger.debug("REST API: Creating new user: {}", user.getUsername());
        try {
            // Call the full createUser method with all required fields
            User createdUser = userService.createUser(user.getUsername(), user.getFirstname(),
                    user.getLastname(), user.getCompany(), user.getEmail(), user.getPassword(),
                    user.getRole(), user.isForcePasswordUpdate());

            // Set enabled status
            createdUser.setEnabled(user.isEnabled());
            User savedUser = userService.updateUser(createdUser);

            return ResponseEntity
                    .ok(ApiResponse.success("User created successfully", new UserDto(savedUser)));
        } catch (Exception e) {
            logger.error("Error creating user: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Error creating user: " + e.getMessage()));
        }
    }

    /**
     * Update existing user with validation
     * 
     * @param id the user ID to update
     * @param user the user entity with updated fields
     * @return ResponseEntity containing the updated user
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "UPDATE")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(@PathVariable Long id,
            @RequestBody User user) {
        logger.debug("REST API: Updating user with ID: {}", id);
        return userService.getUserById(id).map(existingUser -> {
            try {
                // Update editable fields
                existingUser.setUsername(user.getUsername());
                existingUser.setFirstname(user.getFirstname());
                existingUser.setLastname(user.getLastname());
                existingUser.setCompany(user.getCompany());
                existingUser.setEmail(user.getEmail());
                existingUser.setRole(user.getRole());
                existingUser.setEnabled(user.isEnabled());
                existingUser.setAccountLocked(user.isAccountLocked());
                existingUser.setForcePasswordUpdate(user.isForcePasswordUpdate());

                User updatedUser = userService.updateUser(existingUser);
                return ResponseEntity.ok(
                        ApiResponse.success("User updated successfully", new UserDto(updatedUser)));
            } catch (Exception e) {
                logger.error("Error updating user {}: {}", id, e.getMessage(), e);
                return ResponseEntity.status(500).<ApiResponse<UserDto>>body(
                        ApiResponse.error("Error updating user: " + e.getMessage()));
            }
        }).orElse(ResponseEntity.status(404).body(ApiResponse.error(USER_NOT_FOUND_MESSAGE)));
    }

    /**
     * Delete user by ID
     * 
     * @param id the user ID to delete
     * @return ResponseEntity with success or error message
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "DELETE")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        logger.debug("REST API: Deleting user with ID: {}", id);
        return userService.getUserById(id).map(user -> {
            try {
                userService.deleteUser(id);
                return ResponseEntity
                        .ok(ApiResponse.<Void>success("User deleted successfully", null));
            } catch (Exception e) {
                logger.error("Error deleting user {}: {}", id, e.getMessage(), e);
                return ResponseEntity.status(500).<ApiResponse<Void>>body(
                        ApiResponse.error("Error deleting user: " + e.getMessage()));
            }
        }).orElse(ResponseEntity.status(404).body(ApiResponse.error(USER_NOT_FOUND_MESSAGE)));
    }

    /**
     * Modified by: Dean Ammons - March 2026 Change: Restricted login audit visibility to admins or
     * the requesting user. Reason: Prevent unauthorized access to other users' login history.
     */
    @GetMapping("/login-audit")
    public ResponseEntity<ApiResponse<List<com.ammons.taskactivity.dto.LoginAuditDto>>> getLoginAudit(
            @RequestParam(defaultValue = "guest") String username,
            @RequestParam(defaultValue = "50") int limit, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Authentication is required"));
        }

        String requester = authentication.getName();
        boolean canViewAll = hasAuthority(authentication, ROLE_ADMIN)
                || hasAuthority(authentication, LOGIN_AUDIT_PERMISSION);

        if (!canViewAll) {
            if (!requester.equalsIgnoreCase(username)) {
                logger.warn("User {} attempted to view login audit for {} without permission",
                        requester, username);
            }
            username = requester;
        }

        int sanitizedLimit = Math.clamp(limit, 1, MAX_LOGIN_AUDIT_LIMIT);
        logger.debug("REST API: Getting login audit for user: {} (limit: {}) requested by {}",
                username, sanitizedLimit, requester);

        try {
            List<com.ammons.taskactivity.dto.LoginAuditDto> auditData =
                    userService.getLoginAudit(username, sanitizedLimit);
            return ResponseEntity.ok(ApiResponse
                    .success("Retrieved " + auditData.size() + " login records", auditData));
        } catch (Exception e) {
            logger.error("Error retrieving login audit data", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error retrieving login audit: " + e.getMessage()));
        }
    }

    /**
     * Get dropdown access assignments for a user. Returns all available clients, projects, and
     * their expense equivalents, plus the set of IDs explicitly assigned to this user.
     *
     * @param username the username whose access assignments to retrieve
     * @return ResponseEntity containing UserAccessDto with all items and assigned IDs
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "READ")
    @GetMapping("/{username}/access")
    public ResponseEntity<ApiResponse<UserAccessDto>> getUserAccess(@PathVariable String username) {
        logger.debug("REST API: Getting dropdown access for user {}", username);
        return userService.getUserByUsername(username).map(user -> {
            UserAccessDto dto = new UserAccessDto(dropdownValueService.getActiveClients(),
                    dropdownValueService.getActiveProjects(),
                    dropdownValueService.getActiveExpenseClients(),
                    dropdownValueService.getActiveExpenseProjects(),
                    userDropdownAccessService.getAssignedDropdownValueIds(username));
            return ResponseEntity
                    .ok(ApiResponse.success("User access retrieved successfully", dto));
        }).orElse(ResponseEntity.status(404).body(ApiResponse.error(USER_NOT_FOUND_MESSAGE)));
    }

    /**
     * Save dropdown access assignments for a user. When view=TASK, replaces TASK client/project
     * rows. When view=EXPENSE, replaces EXPENSE client/project rows. The other tab's assignments
     * are never touched.
     *
     * @param username the username whose access to update
     * @param request body containing view, clientIds, projectIds, expenseClientIds,
     *        expenseProjectIds
     * @return ResponseEntity with success or error message
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "UPDATE")
    @PutMapping("/{username}/access")
    public ResponseEntity<ApiResponse<Void>> saveUserAccess(@PathVariable String username,
            @RequestBody UserAccessUpdateRequest request) {
        logger.debug("REST API: Saving dropdown access for user {} (view={})", username,
                request.getView());
        return userService.getUserByUsername(username).map(user -> {
            if ("EXPENSE".equals(request.getView())) {
                userDropdownAccessService.saveExpenseClientAssignments(username,
                        request.getExpenseClientIds() != null ? request.getExpenseClientIds()
                                : List.of());
                userDropdownAccessService.saveExpenseProjectAssignments(username,
                        request.getExpenseProjectIds() != null ? request.getExpenseProjectIds()
                                : List.of());
            } else {
                userDropdownAccessService.saveClientAssignments(username,
                        request.getClientIds() != null ? request.getClientIds() : List.of());
                userDropdownAccessService.saveProjectAssignments(username,
                        request.getProjectIds() != null ? request.getProjectIds() : List.of());
            }
            return ResponseEntity
                    .ok(ApiResponse.<Void>success("Access updated successfully", null));
        }).orElse(ResponseEntity.status(404).body(ApiResponse.error(USER_NOT_FOUND_MESSAGE)));
    }

    /**
     * Get all roles for user management filtering. Returns a list of all available roles in the
     * system.
     * 
     * @return ResponseEntity containing list of all roles
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "READ")
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<Roles>>> getAllRoles() {
        logger.debug("REST API: Getting all roles");
        List<Roles> roles = roleRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success("Roles retrieved successfully", roles));
    }

    /**
     * Change password for a user (admin function).
     * 
     * @param id the user ID whose password to change
     * @param request the request body containing newPassword and forcePasswordUpdate flag
     * @return ResponseEntity with success or error message
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "UPDATE")
    @PutMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@PathVariable Long id,
            @Valid @RequestBody PasswordChangeRequest request) {
        logger.debug("REST API: Changing password for user ID: {}", id);
        return userService.getUserById(id).map(user -> {
            try {
                userService.changePassword(user.getUsername(), request.getNewPassword(),
                        !request.isForcePasswordUpdate());
                return ResponseEntity
                        .ok(ApiResponse.<Void>success("Password changed successfully", null));
            } catch (Exception e) {
                logger.error("Error changing password for user {}: {}", id, e.getMessage(), e);
                return ResponseEntity.status(500).<ApiResponse<Void>>body(
                        ApiResponse.error("Error changing password: " + e.getMessage()));
            }
        }).orElse(ResponseEntity.status(404).body(ApiResponse.error(USER_NOT_FOUND_MESSAGE)));
    }

    /**
     * Change password for current logged-in user. Allows any authenticated user to change their own
     * password without admin permissions.
     * 
     * @param request the request body containing currentPassword and newPassword
     * @param authentication the authentication object containing the current user
     * @return ResponseEntity with success or error message
     */
    @PutMapping("/profile/password")
    public ResponseEntity<ApiResponse<Void>> changeOwnPassword(
            @Valid @RequestBody ProfilePasswordChangeRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        logger.debug("REST API: User '{}' changing their own password", username);

        // Block GUEST users from changing password
        if (authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(ROLE_GUEST))) {
            logger.warn("GUEST user '{}' attempted to change password", username);
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Guest users cannot change passwords"));
        }

        return userService.getUserByUsername(username).map(user -> {
            try {
                // Verify current password first
                if (!userService.verifyCurrentPassword(username, request.getCurrentPassword())) {
                    return ResponseEntity.status(401).<ApiResponse<Void>>body(
                            ApiResponse.error("Current password is incorrect"));
                }

                // Change the password
                userService.changePassword(username, request.getNewPassword(), true);
                return ResponseEntity
                        .ok(ApiResponse.<Void>success("Password changed successfully", null));
            } catch (Exception e) {
                logger.error("Error changing password for user '{}': {}", username, e.getMessage(),
                        e);
                return ResponseEntity.status(500).<ApiResponse<Void>>body(
                        ApiResponse.error("Error changing password: " + e.getMessage()));
            }
        }).orElse(ResponseEntity.status(404).body(ApiResponse.error(USER_NOT_FOUND_MESSAGE)));
    }

    /**
     * Returns all active users who have an email address, suitable for admin notification. Supports
     * optional last-name prefix filtering.
     *
     * @param lastNameFilter optional last-name prefix; blank means return all eligible users
     * @return ResponseEntity containing list of eligible users
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "READ")
    @GetMapping("/notify-eligible")
    public ResponseEntity<ApiResponse<List<NotifyEligibleUserDto>>> getNotifyEligibleUsers(
            @RequestParam(required = false, defaultValue = "") String lastNameFilter) {
        logger.debug("REST API: Getting notify-eligible users, filter='{}'", lastNameFilter);
        List<User> users = userService.getActiveUsersWithEmail(lastNameFilter);
        List<NotifyEligibleUserDto> dtos =
                users.stream().map(u -> new NotifyEligibleUserDto(u.getUsername(), u.getFirstname(),
                        u.getLastname(), u.getEmail(), u.getCompany())).toList();
        return ResponseEntity
                .ok(ApiResponse.success("Notify-eligible users retrieved successfully", dtos));
    }

    /**
     * Sends a profile notification email to each selected user. Skips users with no email address
     * or who cannot be found. Returns a summary of sent and skipped counts.
     *
     * @param request body containing the list of selected usernames
     * @param authentication the current admin session
     * @return ResponseEntity containing the notification result summary
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "UPDATE")
    @PostMapping("/notify")
    public ResponseEntity<ApiResponse<NotifyResultDto>> sendNotifications(
            @RequestBody NotifyRequest request, Authentication authentication) {
        logger.info("REST API: Admin {} sending profile notifications", authentication.getName());

        if (request.getUsernames() == null || request.getUsernames().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("No users were selected."));
        }

        int sent = 0;
        int skipped = 0;

        for (String username : request.getUsernames()) {
            java.util.Optional<User> userOpt = userService.getUserByUsername(username);
            boolean hasEmail = userOpt.isPresent() && userOpt.get().getEmail() != null
                    && !userOpt.get().getEmail().isBlank();

            if (!hasEmail) {
                if (userOpt.isEmpty()) {
                    logger.warn("Skipping notification for unknown username: {}", username);
                } else {
                    logger.warn("Skipping notification for user {} — no email address", username);
                }
                skipped++;
            } else {
                User user = userOpt.get();
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
        }

        NotifyResultDto result = new NotifyResultDto(sent, skipped, mailEnabled);
        String message = String.format("Profile notification sent to %d user(s).", sent);
        if (skipped > 0) {
            message += String.format(" %d user(s) skipped (no email or not found).", skipped);
        }
        return ResponseEntity.ok(ApiResponse.success(message, result));
    }

    /**
     * DTO for password change request
     */
    public static class PasswordChangeRequest {
        @ValidPassword
        @NotBlank(message = "Password cannot be blank")
        private String newPassword;
        private boolean forcePasswordUpdate;

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }

        public boolean isForcePasswordUpdate() {
            return forcePasswordUpdate;
        }

        public void setForcePasswordUpdate(boolean forcePasswordUpdate) {
            this.forcePasswordUpdate = forcePasswordUpdate;
        }
    }

    /**
     * DTO for profile password change request (requires current password verification)
     */
    public static class ProfilePasswordChangeRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @ValidPassword
        @NotBlank(message = "New password cannot be blank")
        private String newPassword;

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    /**
     * DTO returned by GET /api/users/{username}/access. Contains all available dropdown items for
     * each category plus the set of dropdown value IDs explicitly assigned to this user.
     */
    public static class UserAccessDto {
        private final List<DropdownValue> allClients;
        private final List<DropdownValue> allProjects;
        private final List<DropdownValue> allExpenseClients;
        private final List<DropdownValue> allExpenseProjects;
        private final Set<Long> assignedIds;

        public UserAccessDto(List<DropdownValue> allClients, List<DropdownValue> allProjects,
                List<DropdownValue> allExpenseClients, List<DropdownValue> allExpenseProjects,
                Set<Long> assignedIds) {
            this.allClients = allClients;
            this.allProjects = allProjects;
            this.allExpenseClients = allExpenseClients;
            this.allExpenseProjects = allExpenseProjects;
            this.assignedIds = assignedIds;
        }

        public List<DropdownValue> getAllClients() {
            return allClients;
        }

        public List<DropdownValue> getAllProjects() {
            return allProjects;
        }

        public List<DropdownValue> getAllExpenseClients() {
            return allExpenseClients;
        }

        public List<DropdownValue> getAllExpenseProjects() {
            return allExpenseProjects;
        }

        public Set<Long> getAssignedIds() {
            return assignedIds;
        }
    }

    /**
     * Request body for PUT /api/users/{username}/access. Specifies which tab (TASK or EXPENSE) is
     * being saved and the selected IDs for each category.
     */
    public static class UserAccessUpdateRequest {
        private String view;
        private List<Long> clientIds;
        private List<Long> projectIds;
        private List<Long> expenseClientIds;
        private List<Long> expenseProjectIds;

        public String getView() {
            return view;
        }

        public void setView(String view) {
            this.view = view;
        }

        public List<Long> getClientIds() {
            return clientIds;
        }

        public void setClientIds(List<Long> clientIds) {
            this.clientIds = clientIds;
        }

        public List<Long> getProjectIds() {
            return projectIds;
        }

        public void setProjectIds(List<Long> projectIds) {
            this.projectIds = projectIds;
        }

        public List<Long> getExpenseClientIds() {
            return expenseClientIds;
        }

        public void setExpenseClientIds(List<Long> expenseClientIds) {
            this.expenseClientIds = expenseClientIds;
        }

        public List<Long> getExpenseProjectIds() {
            return expenseProjectIds;
        }

        public void setExpenseProjectIds(List<Long> expenseProjectIds) {
            this.expenseProjectIds = expenseProjectIds;
        }
    }

    /**
     * DTO representing a user eligible to receive a profile notification. Used by GET
     * /api/users/notify-eligible.
     */
    public static class NotifyEligibleUserDto {
        private final String username;
        private final String firstname;
        private final String lastname;
        private final String email;
        private final String company;

        public NotifyEligibleUserDto(String username, String firstname, String lastname,
                String email, String company) {
            this.username = username;
            this.firstname = firstname;
            this.lastname = lastname;
            this.email = email;
            this.company = company;
        }

        public String getUsername() {
            return username;
        }

        public String getFirstname() {
            return firstname;
        }

        public String getLastname() {
            return lastname;
        }

        public String getEmail() {
            return email;
        }

        public String getCompany() {
            return company;
        }
    }

    /**
     * Request body for POST /api/users/notify. Contains the list of usernames to notify.
     */
    public static class NotifyRequest {
        private List<String> usernames;

        public List<String> getUsernames() {
            return usernames;
        }

        public void setUsernames(List<String> usernames) {
            this.usernames = usernames;
        }
    }

    /**
     * Result DTO returned by POST /api/users/notify. Contains sent/skipped counts and mail status.
     */
    public static class NotifyResultDto {
        private final int sent;
        private final int skipped;
        private final boolean mailEnabled;

        public NotifyResultDto(int sent, int skipped, boolean mailEnabled) {
            this.sent = sent;
            this.skipped = skipped;
            this.mailEnabled = mailEnabled;
        }

        public int getSent() {
            return sent;
        }

        public int getSkipped() {
            return skipped;
        }

        public boolean isMailEnabled() {
            return mailEnabled;
        }
    }

    private boolean hasAuthority(Authentication authentication, String authority) {
        if (authentication == null || authority == null || authority.isBlank()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> authority.equals(auth.getAuthority()));
    }
}


