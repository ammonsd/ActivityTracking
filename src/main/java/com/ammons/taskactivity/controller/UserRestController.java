package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.ApiResponse;
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
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        logger.debug("REST API: Getting current user: {}", username);
        return userService.getUserByUsername(username)
                .map(user -> ResponseEntity.ok(ApiResponse.success("Current user", user)))
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
}
