package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.ApiResponse;
import com.ammons.taskactivity.dto.PermissionDto;
import com.ammons.taskactivity.dto.RoleDto;
import com.ammons.taskactivity.entity.Permission;
import com.ammons.taskactivity.entity.Roles;
import com.ammons.taskactivity.repository.PermissionRepository;
import com.ammons.taskactivity.repository.RoleRepository;
import com.ammons.taskactivity.security.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Description: REST API Controller for Role Management. Provides endpoints for the React dashboard
 * to manage roles and their associated permissions.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since February 2026
 */
@RestController
@RequestMapping("/api/roles")
public class RoleManagementRestController {

    private static final Logger logger =
            LoggerFactory.getLogger(RoleManagementRestController.class);

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public RoleManagementRestController(RoleRepository roleRepository,
            PermissionRepository permissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
    }

    /**
     * Get all roles with their permissions. Returns a complete list of all roles in the system.
     *
     * @return ResponseEntity containing list of all roles
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "READ")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoleDto>>> getAllRoles() {
        logger.debug("REST API: Getting all roles");

        List<Roles> roles = roleRepository.findAll();
        List<RoleDto> roleDtos = roles.stream().map(RoleDto::new).toList();

        ApiResponse<List<RoleDto>> response = ApiResponse
                .success("Roles retrieved successfully", roleDtos).withCount(roleDtos.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Get role by ID with all permissions. Returns the complete role entity with permission
     * details.
     *
     * @param id the role ID to retrieve
     * @return ResponseEntity containing the role if found
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "READ")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleDto>> getRoleById(@PathVariable Long id) {
        logger.debug("REST API: Getting role with ID: {}", id);

        Optional<Roles> roleOptional = roleRepository.findById(id);
        if (roleOptional.isEmpty()) {
            return ResponseEntity.status(404).body(ApiResponse.error("Role not found"));
        }

        RoleDto roleDto = new RoleDto(roleOptional.get());
        return ResponseEntity.ok(ApiResponse.success("Role found", roleDto));
    }

    /**
     * Get all available permissions in the system.
     *
     * @return ResponseEntity containing list of all permissions
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "READ")
    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<List<PermissionDto>>> getAllPermissions() {
        logger.debug("REST API: Getting all permissions");

        List<Permission> permissions = permissionRepository.findAll();
        List<PermissionDto> permissionDtos = permissions.stream().map(PermissionDto::new).toList();

        ApiResponse<List<PermissionDto>> response =
                ApiResponse.success("Permissions retrieved successfully", permissionDtos)
                        .withCount(permissionDtos.size());
        return ResponseEntity.ok(response);
    }

    /**
     * Create a new role with specified permissions.
     *
     * @param roleRequest the DTO containing role name, description, and permission IDs
     * @return ResponseEntity containing the created role
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "CREATE")
    @PostMapping
    public ResponseEntity<ApiResponse<RoleDto>> createRole(
            @Valid @RequestBody RoleCreateRequest roleRequest) {
        logger.debug("REST API: Creating new role: {}", roleRequest.getName());

        try {
            // Check if role already exists
            if (roleRepository.findByName(roleRequest.getName()).isPresent()) {
                return ResponseEntity.status(400)
                        .body(ApiResponse.error("A role with this name already exists"));
            }

            // Create new role
            Roles newRole = new Roles(roleRequest.getName());
            newRole.setDescription(roleRequest.getDescription());

            // Add permissions
            if (roleRequest.getPermissionIds() != null
                    && !roleRequest.getPermissionIds().isEmpty()) {
                List<Permission> permissions =
                        permissionRepository.findAllById(roleRequest.getPermissionIds());
                permissions.forEach(newRole::addPermission);
            }

            Roles savedRole = roleRepository.save(newRole);
            logger.info("Successfully created role: {}", savedRole.getName());

            RoleDto roleDto = new RoleDto(savedRole);
            return ResponseEntity.ok(ApiResponse.success("Role created successfully", roleDto));
        } catch (Exception e) {
            logger.error("Error creating role: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Error creating role: " + e.getMessage()));
        }
    }

    /**
     * Update existing role with new description and permissions.
     *
     * @param id the role ID to update
     * @param roleRequest the DTO containing updated description and permission IDs
     * @return ResponseEntity containing the updated role
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "UPDATE")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoleDto>> updateRole(@PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest roleRequest) {
        logger.debug("REST API: Updating role with ID: {}", id);

        Optional<Roles> roleOptional = roleRepository.findById(id);
        if (roleOptional.isEmpty()) {
            return ResponseEntity.status(404).body(ApiResponse.error("Role not found"));
        }

        try {
            Roles role = roleOptional.get();
            role.setDescription(roleRequest.getDescription());

            // Update permissions
            role.getPermissions().clear();
            if (roleRequest.getPermissionIds() != null
                    && !roleRequest.getPermissionIds().isEmpty()) {
                List<Permission> permissions =
                        permissionRepository.findAllById(roleRequest.getPermissionIds());
                permissions.forEach(role::addPermission);
            }

            Roles updatedRole = roleRepository.save(role);
            logger.info("Successfully updated role: {}", updatedRole.getName());

            RoleDto roleDto = new RoleDto(updatedRole);
            return ResponseEntity.ok(ApiResponse.success("Role updated successfully", roleDto));
        } catch (Exception e) {
            logger.error("Error updating role {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Error updating role: " + e.getMessage()));
        }
    }

    /**
     * Delete role by ID. Note: Cannot delete roles that are currently assigned to users.
     *
     * @param id the role ID to delete
     * @return ResponseEntity with success or error message
     */
    @RequirePermission(resource = "USER_MANAGEMENT", action = "DELETE")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        logger.debug("REST API: Deleting role with ID: {}", id);

        Optional<Roles> roleOptional = roleRepository.findById(id);
        if (roleOptional.isEmpty()) {
            return ResponseEntity.status(404).body(ApiResponse.error("Role not found"));
        }

        try {
            roleRepository.deleteById(id);
            logger.info("Successfully deleted role with ID: {}", id);
            return ResponseEntity.ok(ApiResponse.success("Role deleted successfully", null));
        } catch (Exception e) {
            logger.error("Error deleting role {}: {}", id, e.getMessage(), e);

            // Check if it's a constraint violation (role assigned to users)
            if (e.getMessage() != null && e.getMessage().contains("constraint")) {
                return ResponseEntity.status(400)
                        .body(ApiResponse.error("Cannot delete role that is assigned to users"));
            }

            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Error deleting role: " + e.getMessage()));
        }
    }

    /**
     * DTO for role creation request
     */
    public static class RoleCreateRequest {
        @NotBlank(message = "Role name is required")
        private String name;
        private String description;
        private List<Long> permissionIds;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<Long> getPermissionIds() {
            return permissionIds;
        }

        public void setPermissionIds(List<Long> permissionIds) {
            this.permissionIds = permissionIds;
        }
    }

    /**
     * DTO for role update request
     */
    public static class RoleUpdateRequest {
        private String description;
        private List<Long> permissionIds;

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<Long> getPermissionIds() {
            return permissionIds;
        }

        public void setPermissionIds(List<Long> permissionIds) {
            this.permissionIds = permissionIds;
        }
    }
}
