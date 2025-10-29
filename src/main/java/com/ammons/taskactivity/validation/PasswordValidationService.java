package com.ammons.taskactivity.validation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Service for validating password strength and requirements.
 *
 * @author Dean Ammons
 * @version 1.0
 */
@Component
public class PasswordValidationService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordValidationService.class);

    /**
     * Validates that a password meets the required strength criteria.
     *
     * @param password the password to validate
     * @throws IllegalArgumentException if password doesn't meet requirements
     */
    public void validatePasswordStrength(String password) {
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

        logger.debug("Password validation successful");
    }

    /**
     * Checks if a password meets all strength requirements without throwing exceptions.
     *
     * @param password the password to check
     * @return true if password is valid, false otherwise
     */
    public boolean isPasswordValid(String password) {
        try {
            validatePasswordStrength(password);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
