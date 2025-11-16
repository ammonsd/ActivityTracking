package com.ammons.taskactivity.dto;

import java.time.LocalDateTime;

/**
 * DTO for login audit information. Used to display login activity in admin dashboards.
 */
public class LoginAuditDto {
    private Long id;
    private String username;
    private LocalDateTime loginTime;
    private String ipAddress;
    private String location;
    private boolean successful;

    public LoginAuditDto() {}

    public LoginAuditDto(Long id, String username, LocalDateTime loginTime, String ipAddress,
            String location, boolean successful) {
        this.id = id;
        this.username = username;
        this.loginTime = loginTime;
        this.ipAddress = ipAddress;
        this.location = location;
        this.successful = successful;
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

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    @Override
    public String toString() {
        return "LoginAuditDto{" + "id=" + id + ", username='" + username + '\'' + ", loginTime="
                + loginTime + ", ipAddress='" + ipAddress + '\'' + ", location='" + location + '\''
                + ", successful=" + successful + '}';
    }
}
