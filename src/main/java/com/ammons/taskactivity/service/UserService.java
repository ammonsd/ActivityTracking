package com.ammons.taskactivity.service;

import com.ammons.taskactivity.entity.Role;
import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.repository.UserRepository;
import com.ammons.taskactivity.validation.PasswordValidationService;
import com.ammons.taskactivity.validation.ValidationConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * UserService
 *
 * @author Dean Ammons
 * @version 1.0
 */
@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidationService passwordValidationService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            PasswordValidationService passwordValidationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidationService = passwordValidationService;
    }

    public List<User> getAllUsers() {
        logger.debug("Retrieving all users");
        return userRepository.findAll();
    }

    public List<User> filterUsers(String username, Role role, String company) {
        logger.debug("Filtering users - username: {}, role: {}, company: {}", username, role,
                company);

        List<User> users = userRepository.findAll();

        // Apply username filter (case-insensitive partial match)
        if (username != null && !username.trim().isEmpty()) {
            String lowerUsername = username.toLowerCase().trim();
            users = users.stream()
                    .filter(user -> user.getUsername().toLowerCase().contains(lowerUsername))
                    .toList();
        }

        // Apply role filter (exact match)
        if (role != null) {
            users = users.stream().filter(user -> user.getRole().equals(role)).toList();
        }

        // Apply company filter (case-insensitive partial match, including null values)
        if (company != null && !company.trim().isEmpty()) {
            String lowerCompany = company.toLowerCase().trim();
            users = users.stream().filter(user -> user.getCompany() != null
                    && user.getCompany().toLowerCase().contains(lowerCompany)).toList();
        }

        logger.debug("Filter returned {} users", users.size());
        return users;
    }

    public Optional<User> getUserById(Long id) {
        logger.debug("Retrieving user by ID: {}", id);
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return userRepository.findById(id);
    }

    public Optional<User> getUserByUsername(String username) {
        logger.debug("Retrieving user by username: {}", username);
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        return userRepository.findByUsername(username);
    }

    public User createUser(String username, String password, Role role) {
        return createUser(username, password, role, true);
    }

    public User createUser(String username, String password, Role role,
            boolean forcePasswordUpdate) {
        return createUser(username, null, null, null, password, role, forcePasswordUpdate);
    }

    public User createUser(String username, String firstname, String lastname, String password,
            Role role, boolean forcePasswordUpdate) {
        return createUser(username, firstname, lastname, null, password, role, forcePasswordUpdate);
    }

    public User createUser(String username, String firstname, String lastname, String company,
            String password, Role role, boolean forcePasswordUpdate) {
        logger.info("Attempting to create new user: {}", username);

        // Input validation
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (!StringUtils.hasText(password)) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        if (role == null) {
            throw new IllegalArgumentException("Role cannot be null");
        }
        if (!StringUtils.hasText(lastname)) {
            throw new IllegalArgumentException("Last name cannot be null or empty");
        }

        // Username length and format validation
        if (username.length() < ValidationConstants.USERNAME_MIN_LENGTH
                || username.length() > ValidationConstants.USERNAME_MAX_LENGTH) {
            throw new IllegalArgumentException(ValidationConstants.USERNAME_LENGTH_MSG);
        }

        // Password strength validation
        passwordValidationService.validatePasswordStrength(password);

        // Check if username already exists
        if (userRepository.existsByUsername(username)) {
            logger.warn("Attempt to create user with existing username: {}", username);
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User(username, passwordEncoder.encode(password), role);
        user.setFirstname(firstname);
        user.setLastname(lastname);
        user.setCompany(company);
        user.setForcePasswordUpdate(forcePasswordUpdate);
        User savedUser = userRepository.save(user);

        logger.info("Successfully created new user: {} with role: {}", username, role);
        return savedUser;
    }

    public User updateUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        if (user.getId() == null) {
            throw new IllegalArgumentException("User ID cannot be null for update operation");
        }

        logger.info("Updating user: {}", user.getUsername());

        // Verify user exists before updating
        if (!userRepository.existsById(user.getId())) {
            logger.warn("Attempt to update non-existent user with ID: {}", user.getId());
            throw new IllegalArgumentException("User not found for update");
        }

        User updatedUser = userRepository.save(user);
        logger.info("Successfully updated user: {}", user.getUsername());
        return updatedUser;
    }

    public void deleteUser(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        logger.info("Deleting user with ID: {}", id);

        // Verify user exists before deleting
        User user = userRepository.findById(id).orElseThrow(() -> {
            logger.warn("Attempt to delete non-existent user with ID: {}", id);
            return new IllegalArgumentException("User not found for deletion");
        });

        logger.info("Deleting user: {}", user.getUsername());
        userRepository.deleteById(id);
        logger.info("Successfully deleted user with ID: {}", id);
    }

    @Transactional
    public void changePassword(String username, String newPassword) {
        logger.info("Password change requested for user: {}", username);

        // Input validation
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (!StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("New password cannot be null or empty");
        }

        // Password strength validation
        passwordValidationService.validatePasswordStrength(newPassword);

        User user = userRepository.findByUsername(username).orElseThrow(() -> {
            logger.warn("Password change attempted for non-existent user: {}", username);
            return new IllegalArgumentException("User not found");
        });

        logger.debug("Found user for password change: id={}, username={}, currentForceUpdate={}",
                user.getId(), user.getUsername(), user.isForcePasswordUpdate());

        String encodedPassword = passwordEncoder.encode(newPassword);
        logger.debug("Encoded new password for user: {}", username);

        user.setPassword(encodedPassword);

        // Note: This version does not clear force password update flag
        userRepository.save(user);
        logger.info("Password successfully updated for user: {}", username);
    }

    @Transactional
    public void changePassword(String username, String newPassword, boolean clearForceUpdate) {
        logger.info("Password change requested for user: {}", username);

        // Input validation
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }
        if (!StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("New password cannot be null or empty");
        }

        // Password strength validation
        passwordValidationService.validatePasswordStrength(newPassword);

        User user = userRepository.findByUsername(username).orElseThrow(() -> {
            logger.warn("Password change attempted for non-existent user: {}", username);
            return new IllegalArgumentException("User not found");
        });

        logger.debug("Found user for password change: id={}, username={}, currentForceUpdate={}",
                user.getId(), user.getUsername(), user.isForcePasswordUpdate());

        String encodedPassword = passwordEncoder.encode(newPassword);
        logger.debug("Encoded new password for user: {}", username);

        user.setPassword(encodedPassword);

        // Clear force password update flag if requested
        if (clearForceUpdate) {
            user.setForcePasswordUpdate(false);
            logger.info("Cleared forcePasswordUpdate flag for user: {}", username);
        }

        logger.debug("About to save user: id={}, username={}, forceUpdate={}", user.getId(),
                user.getUsername(), user.isForcePasswordUpdate());

        User savedUser = userRepository.save(user);

        logger.info("Password successfully changed for user: {} (id={}), forceUpdate now: {}",
                username, savedUser.getId(), savedUser.isForcePasswordUpdate());

        // Verify the password was actually saved by reading it back
        User verifyUser = userRepository.findByUsername(username).orElse(null);
        if (verifyUser != null) {
            boolean passwordMatches =
                    passwordEncoder.matches(newPassword, verifyUser.getPassword());
            logger.info(
                    "Password verification for user '{}': saved correctly = {}, forceUpdate = {}",
                    username, passwordMatches, verifyUser.isForcePasswordUpdate());
        } else {
            logger.error("Could not verify saved password - user not found after save!");
        }
    }

    public boolean verifyCurrentPassword(String username, String currentPassword) {
        logger.debug("Verifying current password for user: {}", username);

        if (!StringUtils.hasText(username) || !StringUtils.hasText(currentPassword)) {
            return false;
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            logger.warn("Password verification attempted for non-existent user: {}", username);
            return false;
        }

        boolean matches = passwordEncoder.matches(currentPassword, user.getPassword());
        logger.debug("Password verification for user '{}': {}", username,
                matches ? "successful" : "failed");
        return matches;
    }
}
