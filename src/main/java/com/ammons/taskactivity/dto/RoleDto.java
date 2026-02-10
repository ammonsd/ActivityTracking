package com.ammons.taskactivity.dto;

import com.ammons.taskactivity.entity.Roles;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Description: Data Transfer Object for Role information. Converts Role entity with permission
 * relationships into a simplified format for frontend consumption.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since February 2026
 */
public class RoleDto {
    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdDate;
    private List<PermissionDto> permissions;

    // Default constructor
    public RoleDto() {}

    // Constructor from Roles entity
    public RoleDto(Roles role) {
        this.id = role.getId();
        this.name = role.getName();
        this.description = role.getDescription();
        this.createdDate = role.getCreatedDate();
        this.permissions = role.getPermissions().stream().map(PermissionDto::new).toList();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public List<PermissionDto> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<PermissionDto> permissions) {
        this.permissions = permissions;
    }
}
