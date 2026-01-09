package com.ammons.taskactivity.exception;

import com.ammons.taskactivity.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * REST API Exception Handler for centralized error handling. Handles exceptions thrown by REST
 * controllers and returns consistent JSON responses. This handler has higher priority than
 * GlobalExceptionHandler.
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since October 2025
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.ammons.taskactivity.controller")
public class RestExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(RestExceptionHandler.class);

    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        logger.debug("Validation error occurred: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ApiResponse<Map<String, String>> response = ApiResponse.error("Validation failed", errors);

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle TaskActivityNotFoundException - entity not found
     */
    @ExceptionHandler(TaskActivityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleTaskActivityNotFound(
            TaskActivityNotFoundException ex) {

        logger.debug("Task activity not found: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * Handle IllegalArgumentException - business validation errors
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {

        logger.warn("Business validation error: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(ex.getMessage());
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * Handle AccessDeniedException - authorization failures
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {

        logger.warn("Access denied: {}", ex.getMessage());

        ApiResponse<Void> response =
                ApiResponse.error("You don't have permission to access this resource");

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    /**
     * Handle NullPointerException - indicates a bug
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Void>> handleNullPointer(NullPointerException ex) {

        logger.error("Null pointer exception - this indicates a bug", ex);

        ApiResponse<Void> response =
                ApiResponse.error("An unexpected error occurred. Please contact support");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handle all other uncaught exceptions Logs the full stack trace but returns a generic message
     * to avoid exposing internals
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {

        logger.error("Unexpected error occurred", ex);

        ApiResponse<Void> response =
                ApiResponse.error("An unexpected error occurred. Please try again later");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * Handle RuntimeException - general runtime errors
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException ex) {

        logger.error("Runtime exception occurred: {}", ex.getMessage(), ex);

        // Check if it's a "not found" type message
        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("not found")) {
            ApiResponse<Void> response = ApiResponse.error(ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        ApiResponse<Void> response =
                ApiResponse.error("An error occurred while processing your request");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
