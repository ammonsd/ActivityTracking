package com.ammons.taskactivity.dto;

import com.ammons.taskactivity.entity.Permission;

/**
 * Description: Data Transfer Object for Permission information. Simplifies permission data for
 * frontend consumption by providing only essential fields.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since February 2026
 */
public class PermissionDto {
    private Long id;
    private String resource;
    private String action;
    private String description;
    private String permissionKey; // "RESOURCE:ACTION" format

    // Default constructor
    public PermissionDto() {}

    // Constructor from Permission entity
    public PermissionDto(Permission permission) {
        this.id = permission.getId();
        this.resource = permission.getResource();
        this.action = permission.getAction();
        this.description = permission.getDescription();
        this.permissionKey = permission.getPermissionKey();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPermissionKey() {
        return permissionKey;
    }

    public void setPermissionKey(String permissionKey) {
        this.permissionKey = permissionKey;
    }
}
