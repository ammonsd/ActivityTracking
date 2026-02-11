package com.ammons.taskactivity.validation;

import com.ammons.taskactivity.entity.PasswordHistory;
import com.ammons.taskactivity.repository.PasswordHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Service for validating password strength and requirements.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since December 2025
 */
@Component
public class PasswordValidationService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordValidationService.class);
    private static final Pattern CONSECUTIVE_CHARS_PATTERN =
            Pattern.compile(ValidationConstants.CONSECUTIVE_CHARS_PATTERN);

    private final PasswordHistoryRepository passwordHistoryRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${security.password.history.enabled:true}")
    private boolean historyEnabled;

    @Value("${security.password.history.size:5}")
    private int historySize;

    /**
     * Constructor for PasswordValidationService.
     *
     * @param passwordHistoryRepository Repository for password history operations
     * @param passwordEncoder Password encoder for comparing hashed passwords
     */
    public PasswordValidationService(PasswordHistoryRepository passwordHistoryRepository,
            PasswordEncoder passwordEncoder) {
        this.passwordHistoryRepository = passwordHistoryRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Validates that a password meets the required strength criteria.
     *
     * @param password the password to validate
     * @throws IllegalArgumentException if password doesn't meet requirements
     */
    public void validatePasswordStrength(String password) {
        validatePasswordStrength(password, null);
    }

    /**
     * Validates that a password meets the required strength criteria.
     *
     * @param password the password to validate
     * @param username the username to check against (optional)
     * @throws IllegalArgumentException if password doesn't meet requirements
     */
    public void validatePasswordStrength(String password, String username) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException(ValidationConstants.PASSWORD_NULL_OR_EMPTY_MSG);
        }

        if (password.length() < ValidationConstants.PASSWORD_MIN_LENGTH) {
            logger.warn("Password validation failed: too short");
            throw new IllegalArgumentException(ValidationConstants.PASSWORD_MIN_LENGTH_MSG);
        }

        if (!password.matches(ValidationConstants.UPPERCASE_PATTERN)) {
            logger.warn("Password validation failed: no uppercase letter");
            throw new IllegalArgumentException(ValidationConstants.PASSWORD_UPPERCASE_MSG);
        }

        if (!password.matches(ValidationConstants.DIGIT_PATTERN)) {
            logger.warn("Password validation failed: no digit");
            throw new IllegalArgumentException(ValidationConstants.PASSWORD_DIGIT_MSG);
        }

        if (!password.matches(ValidationConstants.SPECIAL_CHAR_PATTERN)) {
            logger.warn("Password validation failed: no special character");
            throw new IllegalArgumentException(ValidationConstants.PASSWORD_SPECIAL_CHAR_MSG);
        }

        Matcher consecutiveMatcher = CONSECUTIVE_CHARS_PATTERN.matcher(password);
        if (consecutiveMatcher.find()) {
            logger.warn(
                    "Password validation failed: contains more than 2 consecutive identical characters");
            throw new IllegalArgumentException(ValidationConstants.PASSWORD_CONSECUTIVE_CHARS_MSG);
        }

        if (username != null && !username.isEmpty()) {
            if (password.toLowerCase().contains(username.toLowerCase())) {
                logger.warn("Password validation failed: contains username");
                throw new IllegalArgumentException(
                        ValidationConstants.PASSWORD_CONTAINS_USERNAME_MSG);
            }
        }

        logger.debug("Password validation successful");
    }

    /**
     * Checks if a password meets all strength requirements without throwing exceptions.
     *
     * @param password the password to check
     * @return true if password is valid, false otherwise
     */
    public boolean isPasswordValid(String password) {
        return isPasswordValid(password, null);
    }

    /**
     * Checks if a password meets all strength requirements without throwing exceptions.
     *
     * @param password the password to check
     * @param username the username to check against (optional)
     * @return true if password is valid, false otherwise
     */
    public boolean isPasswordValid(String password, String username) {
        try {
            validatePasswordStrength(password, username);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Validates that a new password does not match any of the user's recent password history. This
     * method retrieves the configured number of most recent passwords and compares them against the
     * proposed new password using BCrypt matching.
     *
     * @param userId the user's ID for history lookup
     * @param newPassword the new plaintext password to validate
     * @throws IllegalArgumentException if the password matches any entry in the user's password
     *         history
     */
    public void validatePasswordNotInHistory(Long userId, String newPassword) {
        // Skip validation if history checking is disabled
        if (!historyEnabled) {
            logger.debug("Password history validation disabled via configuration");
            return;
        }

        if (userId == null) {
            throw new IllegalArgumentException(
                    "User ID cannot be null for password history validation");
        }

        if (newPassword == null || newPassword.isEmpty()) {
            throw new IllegalArgumentException(ValidationConstants.PASSWORD_NULL_OR_EMPTY_MSG);
        }

        // Retrieve recent password history (configured size, default 5)
        Pageable pageable = PageRequest.of(0, historySize);
        List<PasswordHistory> recentPasswords =
                passwordHistoryRepository.findRecentByUserId(userId, pageable);

        logger.debug("Checking new password against {} recent password(s) for user ID: {}",
                recentPasswords.size(), userId);

        // Check if new password matches any of the recent passwords
        for (PasswordHistory history : recentPasswords) {
            if (passwordEncoder.matches(newPassword, history.getPasswordHash())) {
                logger.warn(
                        "Password validation failed: matches password from history for user ID: {}",
                        userId);
                throw new IllegalArgumentException(ValidationConstants.PASSWORD_REUSE_HISTORY_MSG);
            }
        }

        logger.debug("Password history validation successful for user ID: {}", userId);
    }
}
