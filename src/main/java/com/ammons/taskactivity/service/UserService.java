package com.ammons.taskactivity.service;

import com.ammons.taskactivity.entity.Roles;
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

import java.time.LocalDate;
import java.time.ZoneOffset;
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
    private final LoginAuditService loginAuditService;
    private final PasswordHistoryService passwordHistoryService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
            PasswordValidationService passwordValidationService,
            LoginAuditService loginAuditService, PasswordHistoryService passwordHistoryService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordValidationService = passwordValidationService;
        this.loginAuditService = loginAuditService;
        this.passwordHistoryService = passwordHistoryService;
    }

    public List<User> getAllUsers() {
        logger.debug("Retrieving all users");
        return userRepository.findAll();
    }

    public List<User> filterUsers(String username, String roleName, String company) {
        logger.debug("Filtering users - username: {}, role: {}, company: {}", username, roleName,
                company);

        List<User> users = userRepository.findAll();

        // Apply username filter (case-insensitive partial match)
        if (username != null && !username.trim().isEmpty()) {
            String lowerUsername = username.toLowerCase().trim();
            users = users.stream()
                    .filter(user -> user.getUsername().toLowerCase().contains(lowerUsername))
                    .toList();
        }

        // Apply role filter (exact match by role name)
        if (roleName != null && !roleName.trim().isEmpty()) {
            users = users.stream().filter(
                    user -> user.getRole() != null && roleName.equals(user.getRole().getName()))
                    .toList();
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

    public User createUser(String username, String password, Roles role) {
        return createUser(username, password, role, true);
    }

    public User createUser(String username, String password, Roles role,
            boolean forcePasswordUpdate) {
        return createUser(username, null, null, null, null, password, role, forcePasswordUpdate);
    }

    public User createUser(String username, String firstname, String lastname, String password,
            Roles role, boolean forcePasswordUpdate) {
        return createUser(username, firstname, lastname, null, null, password, role,
                forcePasswordUpdate);
    }

    public User createUser(String username, String firstname, String lastname, String company,
            String password, Roles role, boolean forcePasswordUpdate) {
        return createUser(username, firstname, lastname, company, null, password, role,
                forcePasswordUpdate);
    }

    public User createUser(String username, String firstname, String lastname, String company,
            String email, String password, Roles role, boolean forcePasswordUpdate) {
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

        // Password strength validation (including username check)
        passwordValidationService.validatePasswordStrength(password, username);

        // Check if username already exists
        if (userRepository.existsByUsername(username)) {
            logger.warn("Attempt to create user with existing username: {}", username);
            throw new IllegalArgumentException("Username already exists");
        }

        User user = new User(username, passwordEncoder.encode(password), role);
        user.setFirstname(firstname);
        user.setLastname(lastname);
        user.setCompany(company);
        user.setEmail(email);
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

        // Verify user exists before updating and fetch existing user
        User existingUser = userRepository.findById(user.getId()).orElseThrow(() -> {
            logger.warn("Attempt to update non-existent user with ID: {}", user.getId());
            return new IllegalArgumentException("User not found for update");
        });

        // Preserve the existing password if the incoming user has null password
        // This allows updates without requiring password to be sent (security best practice)
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            logger.debug("Preserving existing password for user: {}", user.getUsername());
            user.setPassword(existingUser.getPassword());
        }

        // Preserve created date and expiration date
        user.setCreatedDate(existingUser.getCreatedDate());
        if (user.getExpirationDate() == null) {
            user.setExpirationDate(existingUser.getExpirationDate());
        }

        // If admin is unlocking the account, reset failed login attempts
        if (!user.isAccountLocked() && existingUser.isAccountLocked()) {
            logger.info("Account unlocked for user: {} - resetting failed login attempts",
                    user.getUsername());
            user.setFailedLoginAttempts(0);
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

        User user = userRepository.findByUsername(username).orElseThrow(() -> {
            logger.warn("Password change attempted for non-existent user: {}", username);
            return new IllegalArgumentException("User not found");
        });

        // Password strength validation (including username check)
        passwordValidationService.validatePasswordStrength(newPassword, username);

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

        User user = userRepository.findByUsername(username).orElseThrow(() -> {
            logger.warn("Password change attempted for non-existent user: {}", username);
            return new IllegalArgumentException("User not found");
        });

        // Password strength validation (including username check)
        passwordValidationService.validatePasswordStrength(newPassword, username);

        // Check if new password matches current password
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            logger.warn("Password change failed: new password same as current for user: {}",
                    username);
            throw new IllegalArgumentException(ValidationConstants.PASSWORD_REUSE_CURRENT_MSG);
        }

        // Check if new password matches original password from this session
        String originalPasswordHash = passwordHistoryService.getOriginalPasswordHash(username);
        if (originalPasswordHash != null
                && passwordEncoder.matches(newPassword, originalPasswordHash)) {
            logger.warn(
                    "Password change failed: new password same as original session password for user: {}",
                    username);
            throw new IllegalArgumentException(ValidationConstants.PASSWORD_REUSE_SESSION_MSG);
        }

        logger.debug("Found user for password change: id={}, username={}, currentForceUpdate={}",
                user.getId(), user.getUsername(), user.isForcePasswordUpdate());

        String encodedPassword = passwordEncoder.encode(newPassword);
        logger.debug("Encoded new password for user: {}", username);

        user.setPassword(encodedPassword);

        // Set expiration date to 90 days from now
        user.setExpirationDate(LocalDate.now(ZoneOffset.UTC).plusDays(90));
        logger.debug("Set password expiration date to 90 days from now for user: {}", username);

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

    /**
     * Check if a user's password has expired.
     * 
     * @param username the username to check
     * @return true if password is expired, false otherwise
     */
    public boolean isPasswordExpired(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }

        if (user.getExpirationDate() == null) {
            // No expiration date set - consider as not expired
            return false;
        }

        boolean expired = LocalDate.now(ZoneOffset.UTC).isAfter(user.getExpirationDate());
        logger.debug("Password expiration check for user '{}': {}", username,
                expired ? "expired" : "valid");
        return expired;
    }

    /**
     * Check if a user's password is expiring soon (within 7 days).
     * 
     * @param username the username to check
     * @return true if password expires within 7 days, false otherwise
     */
    public boolean isPasswordExpiringSoon(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getExpirationDate() == null) {
            return false;
        }

        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        LocalDate sevenDaysFromNow = now.plusDays(7);

        // Check if expiration date is between now and 7 days from now
        boolean expiringSoon = user.getExpirationDate().isAfter(now)
                && user.getExpirationDate().isBefore(sevenDaysFromNow);

        logger.debug("Password expiring soon check for user '{}': {}", username,
                expiringSoon ? "yes" : "no");
        return expiringSoon;
    }

    /**
     * Get days until password expiration for a user.
     * 
     * @param username the username to check
     * @return number of days until expiration, or null if no expiration date set
     */
    public Long getDaysUntilExpiration(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getExpirationDate() == null) {
            return null;
        }

        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        if (now.isAfter(user.getExpirationDate())) {
            return 0L; // Already expired
        }

        return java.time.temporal.ChronoUnit.DAYS.between(now, user.getExpirationDate());
    }

    /**
     * Unlock a user account and reset failed login attempts. This is typically called by
     * administrators to manually unlock an account that was locked due to too many failed login
     * attempts.
     * 
     * @param username the username of the account to unlock
     * @throws IllegalArgumentException if username is null/empty or user not found
     */
    @Transactional
    public void unlockAccount(String username) {
        logger.info("Account unlock requested for user: {}", username);

        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("Username cannot be null or empty");
        }

        User user = userRepository.findByUsername(username).orElseThrow(() -> {
            logger.warn("Account unlock attempted for non-existent user: {}", username);
            return new IllegalArgumentException("User not found");
        });

        if (!user.isAccountLocked() && user.getFailedLoginAttempts() == 0) {
            logger.info("User '{}' account is already unlocked with no failed attempts", username);
            return;
        }

        user.setAccountLocked(false);
        user.setFailedLoginAttempts(0);
        userRepository.save(user);

        logger.info("Successfully unlocked account for user: {}", username);
    }

    /**
     * Check if a user has a valid email address on file. This is used to determine if a user can
     * access expense management features, which require email for notifications.
     * 
     * @param username the username to check
     * @return true if the user has a non-empty email address, false otherwise
     */
    public boolean userHasEmail(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }

        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();
        return StringUtils.hasText(user.getEmail());
    }

    /**
     * Get login audit data for a specific user
     * 
     * @param username Username to get audit data for
     * @param limit Maximum number of records to return
     * @return List of login audit records
     */
    public List<com.ammons.taskactivity.dto.LoginAuditDto> getLoginAudit(String username,
            int limit) {
        logger.debug("Retrieving login audit for user: {} (limit: {})", username, limit);
        return loginAuditService.getLoginAuditByUsername(username, limit);
    }
}

