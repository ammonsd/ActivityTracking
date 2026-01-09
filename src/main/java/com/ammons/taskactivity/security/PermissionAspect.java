package com.ammons.taskactivity.security;

import com.ammons.taskactivity.service.PermissionService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Aspect that intercepts methods annotated with {@link RequirePermission} and performs
 * authorization checks before allowing method execution.
 * 
 * <p>
 * This aspect implements the database-driven authorization system by:
 * <ol>
 * <li>Extracting the resource and action from the {@code @RequirePermission} annotation</li>
 * <li>Retrieving the authenticated user from the security context</li>
 * <li>Verifying the user has the required permission via {@link PermissionService}</li>
 * <li>Throwing {@link AccessDeniedException} if permission is denied</li>
 * </ol>
 * 
 * @see RequirePermission
 * @see PermissionService
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since December 2025
 */
@Aspect
@Component
public class PermissionAspect {

    private final PermissionService permissionService;

    public PermissionAspect(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    /**
     * Intercepts method calls annotated with {@link RequirePermission} and performs authorization
     * checks before proceeding with method execution.
     * 
     * @param joinPoint the join point representing the intercepted method
     * @param requirePermission the annotation containing permission requirements
     * @return the result of the method execution if permission check passes
     * @throws Throwable if the underlying method throws an exception
     * @throws AccessDeniedException if the user is not authenticated or lacks the required
     *         permission
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since December 2025
     */
    @Around("@annotation(requirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint,
            RequirePermission requirePermission) throws Throwable {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AccessDeniedException("Not authenticated");
        }

        String username = authentication.getName();
        String permissionKey = requirePermission.resource() + ":" + requirePermission.action();

        if (!permissionService.userHasPermission(username, permissionKey)) {
            throw new AccessDeniedException(
                    "User " + username + " lacks permission: " + permissionKey);
        }

        return joinPoint.proceed();
    }
}
