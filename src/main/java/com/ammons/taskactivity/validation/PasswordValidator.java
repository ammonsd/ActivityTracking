package com.ammons.taskactivity.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator implementation for the ValidPassword annotation. Checks password strength requirements.
 *
 * @author Dean Ammons
 * @version 1.0
 */
public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        // Null or empty passwords are handled by @NotBlank, so we allow them here
        if (password == null || password.isEmpty()) {
            return true;
        }

        // Check minimum length
        if (password.length() < ValidationConstants.PASSWORD_MIN_LENGTH) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    ValidationConstants.PASSWORD_MIN_LENGTH_MSG)
                    .addConstraintViolation();
            return false;
        }

        // Check for uppercase letter
        if (!password.matches(ValidationConstants.UPPERCASE_PATTERN)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(ValidationConstants.PASSWORD_UPPERCASE_MSG)
                    .addConstraintViolation();
            return false;
        }

        // Check for digit
        if (!password.matches(ValidationConstants.DIGIT_PATTERN)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(ValidationConstants.PASSWORD_DIGIT_MSG)
                    .addConstraintViolation();
            return false;
        }

        // Check for special character
        if (!password.matches(ValidationConstants.SPECIAL_CHAR_PATTERN)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    ValidationConstants.PASSWORD_SPECIAL_CHAR_MSG)
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
