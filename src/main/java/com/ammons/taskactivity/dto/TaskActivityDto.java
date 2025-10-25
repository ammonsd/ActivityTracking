package com.ammons.taskactivity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * TaskActivityDto
 *
 * @author Dean Ammons
 * @version 1.0
 */
public class TaskActivityDto {

    @NotNull(message = "Task date is required")
    @DateTimeFormat(pattern = "yyyy-MM-dd") // For form binding
    @JsonFormat(pattern = "yyyy-MM-dd") // Controls JSON date format (ISO 8601)
    private LocalDate taskDate;

    @NotBlank(message = "Client is required")
    @Size(max = 255, message = "Client name cannot exceed 255 characters")
    private String client;

    @NotBlank(message = "Project is required")
    @Size(max = 255, message = "Project name cannot exceed 255 characters")
    private String project;

    @NotBlank(message = "Phase is required")
    @Size(max = 255, message = "Phase name cannot exceed 255 characters")
    private String phase;

    @NotNull(message = "Hours is required")
    @DecimalMin(value = "0.01", message = "Hours must be greater than 0")
    @DecimalMax(value = "24.00", message = "Hours cannot exceed 24")
    private BigDecimal hours;

    @Size(max = 255, message = "Details cannot exceed 255 characters")
    private String details;

    // Username - read-only field, populated automatically from logged-in user
    // No validation annotations as this is set programmatically, not from user input
    private String username;

    public TaskActivityDto() {}

    public TaskActivityDto(LocalDate taskDate, String client, String project, String phase,
            BigDecimal hours, String details) {
        this.taskDate = taskDate;
        this.client = client;
        this.project = project;
        this.phase = phase;
        this.hours = hours;
        this.details = details;
    }

    // Getters and Setters
    public LocalDate getTaskDate() {
        return taskDate;
    }

    public void setTaskDate(LocalDate taskDate) {
        this.taskDate = taskDate;
    }

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public BigDecimal getHours() {
        return hours;
    }

    public void setHours(BigDecimal hours) {
        this.hours = hours;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "TaskActivityDto{" + "taskDate=" + taskDate + ", client='" + client + '\''
                + ", project='" + project + '\'' + ", phase='" + phase + '\'' + ", hours=" + hours
                + ", details='" + details + '\'' + ", username='" + username + '\'' + '}';
    }
}
