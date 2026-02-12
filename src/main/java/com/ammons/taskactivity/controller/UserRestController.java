package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.ApiResponse;
import com.ammons.taskactivity.dto.CurrentUserDto;
import com.ammons.taskactivity.dto.UserDto;
import com.ammons.taskactivity.dto.UserEditDto;
import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.entity.Roles;
import com.ammons.taskactivity.service.UserService;
import com.ammons.taskactivity.service.TaskActivityService;
import com.ammons.taskactivity.repository.RoleRepository;
import com.ammons.taskactivity.security.RequirePermission;
import com.ammons.taskactivity.validation.ValidPassword;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for User Management Used by Angular frontend
 *
 * @author Dean Ammons
 * @version 1.0
 * @since December 2025
 */
@RestController
@RequestMapping("/api/users")
public class UserRestController {

    private static final Logger logger = LoggerFactory.getLogger(UserRestController.class);

    private final UserService userService;
    private final TaskActivityService taskActivityService;
    private final RoleRepository roleRepository;

    public UserRestController(UserService userService, TaskActivityService taskActivityService,
            RoleRepository roleRepository) {
        this.userService = userService;
        this.taskActivityService = taskActivityService;
        this.roleRepository = roleRepository;
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
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_GUEST"))) {
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
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_GUEST"))) {
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
            }).orElse(ResponseEntity.status(404).body(ApiResponse.error("User not found")));
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
        }).orElse(ResponseEntity.status(404).body(ApiResponse.error("User not found")));
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
        }).orElse(ResponseEntity.status(404).body(ApiResponse.error("User not found")));
    }

    /**
     * Get login audit data for guest activity dashboard. Returns recent login activity for
     * specified user (default: guest). Accessible to all authenticated users for dashboard demo
     * purposes.
     */
    @GetMapping("/login-audit")
    public ResponseEntity<ApiResponse<List<com.ammons.taskactivity.dto.LoginAuditDto>>> getLoginAudit(
            @RequestParam(defaultValue = "guest") String username,
            @RequestParam(defaultValue = "50") int limit) {
        logger.debug("REST API: Getting login audit for user: {} (limit: {})", username, limit);

        try {
            List<com.ammons.taskactivity.dto.LoginAuditDto> auditData =
                    userService.getLoginAudit(username, limit);
            return ResponseEntity.ok(ApiResponse
                    .success("Retrieved " + auditData.size() + " login records", auditData));
        } catch (Exception e) {
            logger.error("Error retrieving login audit data", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error retrieving login audit: " + e.getMessage()));
        }
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
        }).orElse(ResponseEntity.status(404).body(ApiResponse.error("User not found")));
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
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_GUEST"))) {
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
        }).orElse(ResponseEntity.status(404).body(ApiResponse.error("User not found")));
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
}


