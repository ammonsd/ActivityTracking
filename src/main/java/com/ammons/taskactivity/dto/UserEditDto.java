package com.ammons.taskactivity.dto;

import com.ammons.taskactivity.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * UserEditDto
 *
 * @author Dean Ammons
 * @version 1.0
 */
public class UserEditDto {

    @NotNull(message = "User ID is required")
    private Long id;

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Size(max = 50, message = "First name cannot exceed 50 characters")
    private String firstname;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    private String lastname;

    @Size(max = 100, message = "Company cannot exceed 100 characters")
    private String company;

    @NotNull(message = "Role is required")
    private Role role;

    private boolean enabled;

    private boolean forcePasswordUpdate;

    // Constructors
    public UserEditDto() {}

    public UserEditDto(Long id, String username, Role role, boolean enabled) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.enabled = enabled;
    }

    public UserEditDto(Long id, String username, Role role, boolean enabled,
            boolean forcePasswordUpdate) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.enabled = enabled;
        this.forcePasswordUpdate = forcePasswordUpdate;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFirstname() {
        return firstname;
    }

    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isForcePasswordUpdate() {
        return forcePasswordUpdate;
    }

    public void setForcePasswordUpdate(boolean forcePasswordUpdate) {
        this.forcePasswordUpdate = forcePasswordUpdate;
    }
}
