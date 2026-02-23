package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.entity.Permission;
import com.ammons.taskactivity.entity.Roles;
import com.ammons.taskactivity.repository.PermissionRepository;
import com.ammons.taskactivity.repository.RoleRepository;
import com.ammons.taskactivity.security.RequirePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Web UI Controller for managing roles and permissions. Provides admin interface for viewing and
 * modifying roles and their permissions.
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since December 2025
 */
@Controller
@RequestMapping("/task-activity/manage-roles")
public class RoleManagementController {

    private static final Logger logger = LoggerFactory.getLogger(RoleManagementController.class);
    private static final String REDIRECT_MANAGE_ROLES = "redirect:/task-activity/manage-roles";
    private static final String SUCCESS_MESSAGE = "successMessage";
    private static final String ERROR_MESSAGE = "errorMessage";
    private static final String ROLE_NOT_FOUND = "Role not found";

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final com.ammons.taskactivity.service.UserService userService;

    public RoleManagementController(RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            com.ammons.taskactivity.service.UserService userService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userService = userService;
    }

    /**
     * Display the role management page with list of all roles
     */
    @GetMapping
    @RequirePermission(resource = "USER_MANAGEMENT", action = "READ")
    public String manageRoles(Model model, Authentication authentication) {
        logger.info("Admin {} accessing role management", authentication.getName());

        List<Roles> roles = roleRepository.findAll();
        model.addAttribute("roles", roles);
        addUserDisplayInfo(model, authentication);

        return "admin/role-management";
    }

    /**
     * Show the edit role form
     */
    @GetMapping("/edit/{id}")
    @RequirePermission(resource = "USER_MANAGEMENT", action = "READ")
    public String showEditRoleForm(@PathVariable Long id, Model model,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        logger.info("Admin {} accessing edit form for role ID: {}", authentication.getName(), id);

        Optional<Roles> roleOptional = roleRepository.findById(id);
        if (roleOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, ROLE_NOT_FOUND);
            return REDIRECT_MANAGE_ROLES;
        }

        Roles role = roleOptional.get();
        List<Permission> allPermissions = permissionRepository.findAll();

        // Get permission IDs that are currently assigned to this role
        Set<Long> assignedPermissionIds =
                role.getPermissions().stream().map(Permission::getId).collect(Collectors.toSet());

        model.addAttribute("role", role);
        model.addAttribute("allPermissions", allPermissions);
        model.addAttribute("assignedPermissionIds", assignedPermissionIds);
        addUserDisplayInfo(model, authentication);

        return "admin/role-edit";
    }

    /**
     * Process the edit role form submission
     */
    @PostMapping("/edit/{id}")
    @RequirePermission(resource = "USER_MANAGEMENT", action = "UPDATE")
    public String updateRole(@PathVariable Long id, @RequestParam String description,
            @RequestParam(required = false) List<Long> permissionIds, Authentication authentication,
            RedirectAttributes redirectAttributes) {

        logger.info("Admin {} attempting to update role ID: {}", authentication.getName(), id);

        Optional<Roles> roleOptional = roleRepository.findById(id);
        if (roleOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, ROLE_NOT_FOUND);
            return REDIRECT_MANAGE_ROLES;
        }

        Roles role = roleOptional.get();
        role.setDescription(description);

        // Update permissions
        role.getPermissions().clear();
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<Permission> permissions = permissionRepository.findAllById(permissionIds);
            permissions.forEach(role::addPermission);
        }

        roleRepository.save(role);
        logger.info("Admin {} successfully updated role: {}", authentication.getName(),
                role.getName());

        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE,
                "Role '" + role.getName() + "' updated successfully");
        return REDIRECT_MANAGE_ROLES;
    }

    /**
     * Clone an existing role by pre-populating the Add New Role form with the source role's
     * description and permissions. The Role Name is intentionally left blank so the admin must
     * supply a unique name for the new role.
     */
    @GetMapping("/clone/{id}")
    @RequirePermission(resource = "USER_MANAGEMENT", action = "CREATE")
    public String cloneRole(@PathVariable Long id, Model model, Authentication authentication,
            RedirectAttributes redirectAttributes) {
        logger.info("Admin {} cloning role ID: {}", authentication.getName(), id);

        Optional<Roles> roleOptional = roleRepository.findById(id);
        if (roleOptional.isEmpty()) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, ROLE_NOT_FOUND);
            return REDIRECT_MANAGE_ROLES;
        }

        Roles sourceRole = roleOptional.get();

        // Pre-populate with source role's description; name is left blank
        RoleDto roleDto = new RoleDto();
        roleDto.setDescription(sourceRole.getDescription());

        // Collect the permission IDs from the source role to pre-check them on the form
        Set<Long> assignedPermissionIds = sourceRole.getPermissions().stream()
                .map(Permission::getId).collect(Collectors.toSet());

        List<Permission> allPermissions = permissionRepository.findAll();
        model.addAttribute("allPermissions", allPermissions);
        model.addAttribute("roleDto", roleDto);
        model.addAttribute("assignedPermissionIds", assignedPermissionIds);
        addUserDisplayInfo(model, authentication);

        return "admin/role-add";
    }

    /**
     * Show the add role form
     */
    @GetMapping("/add")
    @RequirePermission(resource = "USER_MANAGEMENT", action = "CREATE")
    public String showAddRoleForm(Model model, Authentication authentication) {
        logger.info("Admin {} accessing add role form", authentication.getName());

        List<Permission> allPermissions = permissionRepository.findAll();
        model.addAttribute("allPermissions", allPermissions);
        model.addAttribute("roleDto", new RoleDto());
        addUserDisplayInfo(model, authentication);

        return "admin/role-add";
    }

    /**
     * Process the add role form submission
     */
    @PostMapping("/add")
    @RequirePermission(resource = "USER_MANAGEMENT", action = "CREATE")
    public String addRole(@Valid @ModelAttribute RoleDto roleDto, BindingResult bindingResult,
            @RequestParam(required = false) List<Long> permissionIds, Model model,
            Authentication authentication, RedirectAttributes redirectAttributes) {

        logger.info("Admin {} attempting to create new role: {}", authentication.getName(),
                roleDto.getName());

        if (bindingResult.hasErrors()) {
            List<Permission> allPermissions = permissionRepository.findAll();
            model.addAttribute("allPermissions", allPermissions);
            addUserDisplayInfo(model, authentication);
            return "admin/role-add";
        }

        // Check if role already exists
        if (roleRepository.findByName(roleDto.getName()).isPresent()) {
            bindingResult.rejectValue("name", "error.name", "A role with this name already exists");
            List<Permission> allPermissions = permissionRepository.findAll();
            model.addAttribute("allPermissions", allPermissions);
            addUserDisplayInfo(model, authentication);
            return "admin/role-add";
        }

        Roles newRole = new Roles(roleDto.getName());
        newRole.setDescription(roleDto.getDescription());

        // Add permissions
        if (permissionIds != null && !permissionIds.isEmpty()) {
            List<Permission> permissions = permissionRepository.findAllById(permissionIds);
            permissions.forEach(newRole::addPermission);
        }

        roleRepository.save(newRole);
        logger.info("Admin {} successfully created role: {}", authentication.getName(),
                newRole.getName());

        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE,
                "Role '" + newRole.getName() + "' created successfully");
        return REDIRECT_MANAGE_ROLES;
    }

    /**
     * Add user display information to the model
     */
    private void addUserDisplayInfo(Model model, Authentication authentication) {
        if (authentication != null) {
            String username = authentication.getName();
            model.addAttribute("currentUser", username);

            // Fetch user details to display full name
            userService.getUserByUsername(username).ifPresent(user -> {
                String firstname = user.getFirstname() != null ? user.getFirstname() : "";
                String lastname = user.getLastname() != null ? user.getLastname() : "";
                String displayName = (firstname + " " + lastname + " (" + username + ")").trim();
                // Remove extra spaces if firstname is empty
                displayName = displayName.replaceAll("\\s+", " ");
                model.addAttribute("userDisplayName", displayName);
            });
        }
    }

    /**
     * DTO for role creation
     */
    public static class RoleDto {
        @NotBlank(message = "Role name is required")
        private String name;

        private String description;

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
    }
}
