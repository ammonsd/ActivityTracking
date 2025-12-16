package com.ammons.taskactivity.validation;

import java.math.BigDecimal;

/**
 * Constants for validation rules across the application. Centralizes all validation-related magic
 * numbers and strings.
 * 
 * @author Dean Ammons
 * @version 1.0
 */
public final class ValidationConstants {

    private ValidationConstants() {
        throw new UnsupportedOperationException(
                "This is a utility class and cannot be instantiated");
    }

    // Username validation
    public static final int USERNAME_MIN_LENGTH = 3;
    public static final int USERNAME_MAX_LENGTH = 50;

    // Password validation
    public static final int PASSWORD_MIN_LENGTH = 10;
    public static final String UPPERCASE_PATTERN = ".*[A-Z].*";
    public static final String DIGIT_PATTERN = ".*\\d.*";
    public static final String SPECIAL_CHAR_PATTERN = ".*[+&%$#@!~].*";
    public static final String ALLOWED_SPECIAL_CHARS = "+&%$#@!~";
    // Pattern to detect more than 2 consecutive identical characters
    public static final String CONSECUTIVE_CHARS_PATTERN = "(.)\\1{2,}";

    // Name validation
    public static final int FIRSTNAME_MAX_LENGTH = 50;
    public static final int LASTNAME_MAX_LENGTH = 50;

    // Task activity validation
    public static final BigDecimal MIN_HOURS = new BigDecimal("0.01");
    public static final BigDecimal MAX_HOURS = new BigDecimal("24.00");
    public static final int CLIENT_MAX_LENGTH = 255;
    public static final int PROJECT_MAX_LENGTH = 255;
    public static final int PHASE_MAX_LENGTH = 255;
    public static final int DETAILS_MAX_LENGTH = 255;

    // Dropdown validation
    public static final int CATEGORY_MAX_LENGTH = 50;
    public static final int DROPDOWN_VALUE_MAX_LENGTH = 255;

    // Validation messages
    public static final String USERNAME_NULL_OR_EMPTY_MSG = "Username cannot be null or empty";
    public static final String USERNAME_LENGTH_MSG = "Username must be between "
            + USERNAME_MIN_LENGTH + " and " + USERNAME_MAX_LENGTH + " characters";
    public static final String PASSWORD_NULL_OR_EMPTY_MSG = "Password cannot be null or empty";
    public static final String PASSWORD_MIN_LENGTH_MSG =
            "Password must be at least " + PASSWORD_MIN_LENGTH + " characters long";
    public static final String PASSWORD_UPPERCASE_MSG =
            "Password must contain at least 1 uppercase letter";
    public static final String PASSWORD_DIGIT_MSG =
            "Password must contain at least 1 numeric digit";
    public static final String PASSWORD_SPECIAL_CHAR_MSG =
            "Password must contain at least 1 special character (" + ALLOWED_SPECIAL_CHARS + ")";
    public static final String PASSWORD_CONSECUTIVE_CHARS_MSG =
                    "Password cannot contain more than 2 consecutive identical characters";
    public static final String PASSWORD_CONTAINS_USERNAME_MSG =
                    "Password cannot contain the username";
    public static final String PASSWORD_REUSE_CURRENT_MSG =
                    "New password cannot be the same as your current password";
    public static final String PASSWORD_REUSE_SESSION_MSG =
                    "New password cannot be the same as your original password from this session";
    public static final String LASTNAME_NULL_OR_EMPTY_MSG = "Last name cannot be null or empty";
    public static final String ROLE_NULL_MSG = "Role cannot be null";
}
