package com.ammons.taskactivity.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Service for validating password strength and requirements.
 *
 * @author Dean Ammons
 * @version 1.0
 */
@Component
public class PasswordValidationService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordValidationService.class);
    private static final Pattern CONSECUTIVE_CHARS_PATTERN =
            Pattern.compile(ValidationConstants.CONSECUTIVE_CHARS_PATTERN);

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
}
