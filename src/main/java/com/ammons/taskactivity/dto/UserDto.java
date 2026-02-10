package com.ammons.taskactivity.dto;

import com.ammons.taskactivity.entity.User;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for User information Converts the Roles entity to a simple role name string
 * for Angular frontend
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
public class UserDto {
    private Long id;
    private String username;
    private String firstname;
    private String lastname;
    private String company;
    private String email;
    private String role; // Role name as String instead of Roles entity
    private boolean enabled;
    private boolean forcePasswordUpdate;
    private LocalDate expirationDate;
    private LocalDateTime createdDate;
    private LocalDateTime lastLogin;
    private int failedLoginAttempts;
    private boolean accountLocked;
    private boolean hasTasks; // Indicates if user has task activities (affects delete permission)

    // Default constructor
    public UserDto() {}

    // Constructor from User entity
    public UserDto(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.firstname = user.getFirstname();
        this.lastname = user.getLastname();
        this.company = user.getCompany();
        this.email = user.getEmail();
        this.role = user.getRole() != null ? user.getRole().getName() : null;
        this.enabled = user.isEnabled();
        this.forcePasswordUpdate = user.isForcePasswordUpdate();
        this.expirationDate = user.getExpirationDate();
        this.createdDate = user.getCreatedDate();
        this.lastLogin = user.getLastLogin();
        this.failedLoginAttempts = user.getFailedLoginAttempts();
        this.accountLocked = user.isAccountLocked();
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

    public boolean isForcePasswordUpdate() {
        return forcePasswordUpdate;
    }

    public void setForcePasswordUpdate(boolean forcePasswordUpdate) {
        this.forcePasswordUpdate = forcePasswordUpdate;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public boolean isAccountLocked() {
        return accountLocked;
    }

    public void setAccountLocked(boolean accountLocked) {
        this.accountLocked = accountLocked;
    }

    public boolean isHasTasks() {
        return hasTasks;
    }

    public void setHasTasks(boolean hasTasks) {
        this.hasTasks = hasTasks;
    }
}
