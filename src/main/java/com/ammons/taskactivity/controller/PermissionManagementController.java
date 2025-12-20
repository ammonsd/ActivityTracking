package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.ApiResponse;
import com.ammons.taskactivity.entity.Permission;
import com.ammons.taskactivity.entity.Roles;
import com.ammons.taskactivity.repository.PermissionRepository;
import com.ammons.taskactivity.repository.RoleRepository;
import com.ammons.taskactivity.security.RequirePermission;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API Controller for managing roles and permissions. Provides admin endpoints for viewing and
 * modifying role-permission associations in the database-driven authorization system.
 * 
 * @author Dean Ammons
 * @version 1.0
 */
@RestController
@RequestMapping("/api/admin/permissions")
public class PermissionManagementController {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    public PermissionManagementController(PermissionRepository permissionRepository,
            RoleRepository roleRepository) {
        this.permissionRepository = permissionRepository;
        this.roleRepository = roleRepository;
    }

    /**
     * Get all roles with their associated permissions.
     * 
     * @return ResponseEntity containing list of all roles
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "READ")
    @GetMapping("/roles")
    public ResponseEntity<ApiResponse<List<Roles>>> getAllRoles() {
        List<Roles> roles = roleRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success("Roles retrieved", roles));
    }

    /**
     * Get all available permissions.
     * 
     * @return ResponseEntity containing list of all permissions
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "READ")
    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<List<Permission>>> getAllPermissions() {
        List<Permission> permissions = permissionRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success("Permissions retrieved", permissions));
    }

    /**
     * Grant a permission to a role. Adds the specified permission to the role's permission set.
     * 
     * @param roleId the role ID to grant the permission to
     * @param permissionId the permission ID to grant
     * @return ResponseEntity containing the updated role
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "MANAGE_ROLES")
    @PostMapping("/roles/{roleId}/permissions/{permissionId}")
    public ResponseEntity<ApiResponse<Roles>> grantPermission(@PathVariable Long roleId,
            @PathVariable Long permissionId) {

        Roles role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Permission not found"));

        role.addPermission(permission);
        roleRepository.save(role);

        return ResponseEntity.ok(ApiResponse.success("Permission granted", role));
    }

    /**
     * Revoke a permission from a role. Removes the specified permission from the role's permission
     * set.
     * 
     * @param roleId the role ID to revoke the permission from
     * @param permissionId the permission ID to revoke
     * @return ResponseEntity containing the updated role
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "MANAGE_ROLES")
    @DeleteMapping("/roles/{roleId}/permissions/{permissionId}")
    public ResponseEntity<ApiResponse<Roles>> revokePermission(@PathVariable Long roleId,
            @PathVariable Long permissionId) {

        Roles roles = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Role not found"));
        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Permission not found"));

        roles.removePermission(permission);
        roleRepository.save(roles);

        return ResponseEntity.ok(ApiResponse.success("Permission revoked", roles));
    }
}
