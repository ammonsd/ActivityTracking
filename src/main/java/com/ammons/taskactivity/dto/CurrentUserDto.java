package com.ammons.taskactivity.dto;

import com.ammons.taskactivity.entity.Roles;

/**
 * DTO for current user information including password expiration warning
 *
 * @author Dean Ammons
 * @version 1.0
 */
public class CurrentUserDto {
    private Long id;
    private String username;
    private String firstname;
    private String lastname;
    private String company;
    private String email;
    private String role; // Role name as String for Angular compatibility
    private boolean enabled;
    private String passwordExpiringWarning;
    private Long daysUntilExpiration;

    public CurrentUserDto() {}

    public CurrentUserDto(Long id, String username, String firstname, String lastname,
            String company, String email, Roles role, boolean enabled) {
        this.id = id;
        this.username = username;
        this.firstname = firstname;
        this.lastname = lastname;
        this.company = company;
        this.email = email;
        this.role = role != null ? role.getName() : null; // Extract name from Roles entity
        this.enabled = enabled;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPasswordExpiringWarning() {
        return passwordExpiringWarning;
    }

    public void setPasswordExpiringWarning(String passwordExpiringWarning) {
        this.passwordExpiringWarning = passwordExpiringWarning;
    }

    public Long getDaysUntilExpiration() {
        return daysUntilExpiration;
    }

    public void setDaysUntilExpiration(Long daysUntilExpiration) {
        this.daysUntilExpiration = daysUntilExpiration;
    }
}
