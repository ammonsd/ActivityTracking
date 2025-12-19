package com.ammons.taskactivity.service;

import com.ammons.taskactivity.entity.Permission;
import com.ammons.taskactivity.entity.Roles;
import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.repository.RoleRepository;
import com.ammons.taskactivity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Service for managing and evaluating user permissions in the database-driven authorization system.
 * 
 * <p>
 * This service provides the core permission checking logic used by:
 * <ul>
 * <li>{@link com.ammons.taskactivity.security.PermissionAspect} - For {@code @RequirePermission}
 * annotations</li>
 * <li>{@link com.ammons.taskactivity.security.CustomPermissionEvaluator} - For SpEL-based
 * permission checks</li>
 * </ul>
 * 
 * <p>
 * Permission keys are in the format "RESOURCE:ACTION" (e.g., "TASK_ACTIVITY:CREATE").
 * 
 * @see com.ammons.taskactivity.entity.Permission
 * @see com.ammons.taskactivity.entity.Roles
 */
@Service
@Transactional(readOnly = true)
public class PermissionService {

    private static final Logger logger = LoggerFactory.getLogger(PermissionService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public PermissionService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * Check if a user has a specific permission
     * 
     * @param username The username
     * @param permissionKey Permission in format "RESOURCE:ACTION" (e.g., "TASK_ACTIVITY:CREATE")
     * @return true if user has the permission
     */
    public boolean userHasPermission(String username, String permissionKey) {
        logger.debug("Checking permission {} for user {}", permissionKey, username);

        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getRole() == null) {
            logger.warn("User {} not found or has no role", username);
            return false;
        }

        // Parse permission key
        String[] parts = permissionKey.split(":");
        if (parts.length != 2) {
            logger.error("Invalid permission key format: {}", permissionKey);
            return false;
        }

        String resource = parts[0];
        String action = parts[1];

        // Check if user's role has this permission
        Roles role = user.getRole();
        boolean hasPermission = role.getPermissions().stream()
                .anyMatch(p -> p.getResource().equals(resource) && p.getAction().equals(action));

        logger.debug("User {} {} permission {} (role: {})", username,
                hasPermission ? "HAS" : "LACKS", permissionKey, role.getName());

        return hasPermission;
    }

    /**
     * Retrieves all permissions assigned to a user through their role.
     * 
     * @param username the username to retrieve permissions for
     * @return a set of permissions, or an empty set if the user is not found or has no role
     */
    public Set<Permission> getUserPermissions(String username) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null || user.getRole() == null) {
            return Set.of();
        }
        return user.getRole().getPermissions();
    }

    /**
     * Checks if a specific role has a given permission.
     * 
     * @param roleId the ID of the role to check
     * @param resource the resource name (e.g., "TASK_ACTIVITY")
     * @param action the action name (e.g., "CREATE")
     * @return true if the role has the permission, false otherwise
     */
    public boolean roleHasPermission(Long roleId, String resource, String action) {
        return roleRepository.roleHasPermission(roleId, resource, action);
    }
}
