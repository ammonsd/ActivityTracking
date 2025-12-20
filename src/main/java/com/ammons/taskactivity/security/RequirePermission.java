package com.ammons.taskactivity.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for method-level authorization using database-driven permissions. This annotation is
 * intercepted by {@link PermissionAspect} to verify that the authenticated user has the required
 * permission before allowing method execution.
 * 
 * <p>
 * Usage example:
 * 
 * <pre>
 * {@code @RequirePermission(resource = "TASK_ACTIVITY", action = "CREATE")}
 * public TaskActivity createTaskActivity(TaskActivity taskActivity) {
 *     // method implementation
 * }
 * </pre>
 * 
 * @see PermissionAspect
 * @see com.ammons.taskactivity.entity.Permission
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    /**
     * The resource being accessed (e.g., "TASK_ACTIVITY", "EXPENSE", "USER_MANAGEMENT").
     * 
     * @return the resource name
     */
    String resource();

    /**
     * The action being performed (e.g., "CREATE", "READ", "UPDATE", "DELETE").
     * 
     * @return the action name
     */
    String action();
}
