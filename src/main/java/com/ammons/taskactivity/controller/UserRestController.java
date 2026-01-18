package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.ApiResponse;
import com.ammons.taskactivity.dto.CurrentUserDto;
import com.ammons.taskactivity.dto.UserDto;
import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.service.UserService;
import com.ammons.taskactivity.security.RequirePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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

    public UserRestController(UserService userService) {
        this.userService = userService;
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
     * @param authentication the authenticated user making the request
     * @return ResponseEntity containing the user's complete profile
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "READ")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<User>> getCurrentUserProfile(Authentication authentication) {
        String username = authentication.getName();
        logger.debug("REST API: Getting profile for user: {}", username);
        return userService.getUserByUsername(username)
                .map(user -> ResponseEntity.ok(ApiResponse.success("Profile retrieved", user)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update current user's profile. Non-admin users can only update their own profile fields:
     * firstname, lastname, company, email. Role and other security-related fields cannot be
     * modified.
     * 
     * @param updatedUser the user entity containing updated profile data
     * @param authentication the authenticated user making the request
     * @return ResponseEntity containing the updated user profile
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "UPDATE")
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<User>> updateCurrentUserProfile(@RequestBody User updatedUser,
            Authentication authentication) {
        String username = authentication.getName();
        logger.debug("REST API: Updating profile for user: {}", username);

        return userService.getUserByUsername(username).map(existingUser -> {
            // Non-admin users can only update specific fields
            existingUser.setFirstname(updatedUser.getFirstname());
            existingUser.setLastname(updatedUser.getLastname());
            existingUser.setCompany(updatedUser.getCompany());
            existingUser.setEmail(updatedUser.getEmail());

            User savedUser = userService.updateUser(existingUser);
            return ResponseEntity
                    .ok(ApiResponse.success("Profile updated successfully", savedUser));
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all users in the system. Returns a complete list of all registered users.
     * 
     * @return ResponseEntity containing list of all users
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "READ")
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        logger.debug("REST API: Getting all users");
        List<User> users = userService.getAllUsers();
        List<UserDto> userDtos = users.stream().map(UserDto::new).toList();
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
     * Create new user
     */
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        logger.debug("REST API: Creating new user: {}", user.getUsername());
        // Map entity fields to service method signature
        User createdUser = userService.createUser(user.getUsername(), user.getPassword(),
                user.getRole(), user.isForcePasswordUpdate());
        return ResponseEntity.ok(createdUser);
    }

    /**
     * Update existing user
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        logger.debug("REST API: Updating user with ID: {}", id);
        return userService.getUserById(id).map(existingUser -> {
            user.setId(id);
            User updatedUser = userService.updateUser(user);
            return ResponseEntity.ok(updatedUser);
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete user
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        logger.debug("REST API: Deleting user with ID: {}", id);
        return userService.getUserById(id).map(user -> {
            userService.deleteUser(id);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get login audit data (ADMIN only) Returns recent login activity for guest users
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
}


