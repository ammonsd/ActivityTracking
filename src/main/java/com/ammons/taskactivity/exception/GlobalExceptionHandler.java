package com.ammons.taskactivity.exception;

import com.ammons.taskactivity.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Global exception handler for centralized error handling across view-based controllers. This
 * handler is specifically for MVC controllers that return HTML views. REST API exceptions are
 * handled by RestExceptionHandler.
 * 
 * @author Dean Ammons
 * @version 1.0
 */
@ControllerAdvice(annotations = org.springframework.stereotype.Controller.class)
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ERROR_VIEW = "error";
    private static final String ERROR_MESSAGE_ATTR = "errorMessage";
    private static final String ERROR_CODE_ATTR = "errorCode";
    private static final String ERROR_TITLE_ATTR = "errorTitle";
    private static final String REDIRECT_TASK_LIST = "redirect:/task-activity/list";

    private final UserService userService;

    public GlobalExceptionHandler(UserService userService) {
        this.userService = userService;
    }

    /**
     * Add password expiration warning to all views
     */
    @ModelAttribute("passwordExpiringWarning")
    public String addPasswordExpirationWarning() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {

            // Don't show warning for GUEST users (they can't change passwords)
            boolean isGuest = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_GUEST"));
            if (isGuest) {
                return null;
            }

            String username = authentication.getName();
            if (userService.isPasswordExpiringSoon(username)) {
                Long daysUntilExpiration = userService.getDaysUntilExpiration(username);
                if (daysUntilExpiration != null) {
                    return "⚠️ Your password will expire in " + daysUntilExpiration + " day"
                            + (daysUntilExpiration == 1 ? "" : "s") + ". Please change it soon.";
                }
            }
        }
        return null;
    }

    /**
     * Handle TaskActivityNotFoundException - returns 404 error page
     */
    @ExceptionHandler(TaskActivityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleTaskActivityNotFound(TaskActivityNotFoundException ex, Model model) {
        logger.error("Task activity not found: {}", ex.getMessage());
        model.addAttribute(ERROR_MESSAGE_ATTR, ex.getMessage());
        model.addAttribute(ERROR_CODE_ATTR, "404");
        model.addAttribute(ERROR_TITLE_ATTR, "Task Not Found");
        return ERROR_VIEW;
    }

    /**
     * Handle IllegalArgumentException - business validation errors
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model,
            RedirectAttributes redirectAttributes) {
        logger.warn("Validation error: {}", ex.getMessage());

        // For AJAX/API requests, return error view
        // For form submissions, redirect with flash message
        redirectAttributes.addFlashAttribute(ERROR_MESSAGE_ATTR, ex.getMessage());
        return REDIRECT_TASK_LIST;
    }

    /**
     * Handle AccessDeniedException - already handled by Spring Security, but this provides
     * additional logging
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException ex, Model model) {
        logger.warn("Access denied: {}", ex.getMessage());
        model.addAttribute(ERROR_MESSAGE_ATTR,
                "You don't have permission to access this resource.");
        model.addAttribute(ERROR_CODE_ATTR, "403");
        model.addAttribute(ERROR_TITLE_ATTR, "Access Denied");
        return "access-denied";
    }

    /**
     * Handle NoResourceFoundException - static resources not found (e.g., favicon.ico) Returns 404
     * without logging as an error to avoid log noise
     */
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ExceptionHandler(NoResourceFoundException.class)
    public String handleNoResourceFound(NoResourceFoundException ex, Model model,
            HttpServletResponse response) {
        // Only log at DEBUG level to avoid cluttering logs with favicon requests
        logger.debug("Static resource not found: {}", ex.getResourcePath());

        // Set HTTP status to 404 directly
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);

        model.addAttribute(ERROR_MESSAGE_ATTR, "The requested resource was not found.");
        model.addAttribute(ERROR_CODE_ATTR, "404");
        model.addAttribute(ERROR_TITLE_ATTR, "Not Found");
        return ERROR_VIEW;
    }

    /**
     * Handle NullPointerException - indicates a bug that should be fixed
     */
    @ExceptionHandler(NullPointerException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleNullPointer(NullPointerException ex, Model model) {
        logger.error("Null pointer exception - this indicates a bug", ex);
        model.addAttribute(ERROR_MESSAGE_ATTR,
                "An unexpected error occurred. Please contact support if this persists.");
        model.addAttribute(ERROR_CODE_ATTR, "500");
        model.addAttribute(ERROR_TITLE_ATTR, "Internal Server Error");
        return ERROR_VIEW;
    }

    /**
     * Handle all other uncaught exceptions
     */
    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, Model model, HttpServletResponse response) {
        // Check if this is actually a NoResourceFoundException that wasn't caught by specific
        // handler
        if (ex instanceof NoResourceFoundException noResourceFoundException) {
            logger.debug(
                    "NoResourceFoundException caught in generic handler (this shouldn't happen): {}",
                    noResourceFoundException.getResourcePath());
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            model.addAttribute(ERROR_MESSAGE_ATTR, "The requested resource was not found.");
            model.addAttribute(ERROR_CODE_ATTR, "404");
            model.addAttribute(ERROR_TITLE_ATTR, "Not Found");
        } else {
            logger.error("Unexpected error occurred", ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            model.addAttribute(ERROR_MESSAGE_ATTR,
                    "An unexpected error occurred. Please try again later.");
            model.addAttribute(ERROR_CODE_ATTR, "500");
            model.addAttribute(ERROR_TITLE_ATTR, "Internal Server Error");
        }
        return ERROR_VIEW;
    }
}
