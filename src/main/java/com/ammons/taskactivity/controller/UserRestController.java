package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.ApiResponse;
import com.ammons.taskactivity.dto.CurrentUserDto;
import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for User Management Used by Angular frontend
 *
 * @author Dean Ammons
 * @version 1.0
 */
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserRestController {

    private static final Logger logger = LoggerFactory.getLogger(UserRestController.class);

    private final UserService userService;

    public UserRestController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get current authenticated user
     */
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST', 'EXPENSE_ADMIN')")
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
                            dto.setPasswordExpiringWarning(
                                    "⚠️ Your password will expire in " + daysUntilExpiration
                                            + " day" + (daysUntilExpiration == 1 ? "" : "s")
                                            + ". Please change it soon.");
                        }
                    }

                    return ResponseEntity.ok(ApiResponse.success("Current user", dto));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all users
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'GUEST')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        logger.debug("REST API: Getting all users");
        List<User> users = userService.getAllUsers();
        ApiResponse<List<User>> response =
                ApiResponse.success("Users retrieved successfully", users).withCount(users.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Get user by ID
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'GUEST')")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long id) {
        logger.debug("REST API: Getting user with ID: {}", id);
        return userService.getUserById(id)
                .map(user -> ResponseEntity.ok(ApiResponse.success("User found", user)))
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


