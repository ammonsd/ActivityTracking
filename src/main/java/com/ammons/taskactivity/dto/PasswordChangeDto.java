package com.ammons.taskactivity.dto;

import com.ammons.taskactivity.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;

/**
 * PasswordChangeDto
 *
 * @author Dean Ammons
 * @version 1.0
 */
public class PasswordChangeDto {

    @NotBlank(message = "Username is required")
    private String username;

    private String currentPassword; // Optional for forced updates

    @NotBlank(message = "New password is required")
    @ValidPassword
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    private String confirmNewPassword;

    private boolean forcePasswordUpdate = false;

    // Constructors
    public PasswordChangeDto() {}

    public PasswordChangeDto(String username, String newPassword, String confirmNewPassword) {
        this.username = username;
        this.newPassword = newPassword;
        this.confirmNewPassword = confirmNewPassword;
    }

    public PasswordChangeDto(String username, String currentPassword, String newPassword,
            String confirmNewPassword) {
        this.username = username;
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
        this.confirmNewPassword = confirmNewPassword;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmNewPassword() {
        return confirmNewPassword;
    }

    public void setConfirmNewPassword(String confirmNewPassword) {
        this.confirmNewPassword = confirmNewPassword;
    }

    public boolean isForcePasswordUpdate() {
        return forcePasswordUpdate;
    }

    public void setForcePasswordUpdate(boolean forcePasswordUpdate) {
        this.forcePasswordUpdate = forcePasswordUpdate;
    }

    // Validation helper
    public boolean isPasswordMatching() {
        return newPassword != null && newPassword.equals(confirmNewPassword);
    }
}
