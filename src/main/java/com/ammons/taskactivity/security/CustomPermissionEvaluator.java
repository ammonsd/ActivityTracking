package com.ammons.taskactivity.security;

import com.ammons.taskactivity.service.PermissionService;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Custom implementation of Spring Security's {@link PermissionEvaluator} interface that integrates
 * with the database-driven permission system.
 * 
 * <p>
 * This evaluator can be used in SpEL expressions within security annotations:
 * 
 * <pre>
 * {@code @PreAuthorize("hasPermission(null, 'TASK_ACTIVITY:CREATE')")}
 * </pre>
 * 
 * <p>
 * However, the preferred approach is to use the {@link RequirePermission} annotation which provides
 * better type safety and readability.
 * 
 * @see PermissionService
 * @see RequirePermission
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since December 2025
 */
@Component("permissionEvaluator")
public class CustomPermissionEvaluator implements PermissionEvaluator {

    private final PermissionService permissionService;

    public CustomPermissionEvaluator(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * Evaluates whether the authenticated user has the specified permission.
     * 
     * @param authentication the authentication object containing user details
     * @param targetDomainObject the target domain object (not used in this implementation)
     * @param permission the permission key in "RESOURCE:ACTION" format
     * @return true if the user has the permission, false otherwise
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since December 2025
     */
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject,
            Object permission) {
        if (authentication == null || permission == null) {
            return false;
        }

        String username = authentication.getName();
        String permissionKey = permission.toString();

        return permissionService.userHasPermission(username, permissionKey);
    }

    /**
     * Evaluates whether the authenticated user has the specified permission for a target object.
     * This implementation delegates to the simpler hasPermission method since targetId and
     * targetType are not used in the current authorization model.
     * 
     * @param authentication the authentication object containing user details
     * @param targetId the identifier of the target domain object
     * @param targetType the type of the target domain object
     * @param permission the permission key in "RESOURCE:ACTION" format
     * @return true if the user has the permission, false otherwise
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since December 2025
     */
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId,
            String targetType, Object permission) {
        // For this implementation, we don't use targetId/targetType
        return hasPermission(authentication, null, permission);
    }
}
