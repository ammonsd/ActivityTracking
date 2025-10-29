package com.ammons.taskactivity.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom annotation for password validation. Validates that password meets security requirements: -
 * At least 10 characters - At least 1 uppercase letter - At least 1 numeric digit - At least 1
 * special character
 *
 * @author Dean Ammons
 * @version 1.0
 */
@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default "Password must be at least 10 characters and include at least 1 uppercase letter, 1 number, and 1 special character";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
