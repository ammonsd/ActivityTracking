package com.ammons.taskactivity.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * TaskActivity Entity
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since October 2025
 */
@Entity
@Table(name = "taskactivity", schema = "public")
public class TaskActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull(message = "Task date is required")
    @Column(name = "taskdate", nullable = false)
    private LocalDate taskDate;

    @NotBlank(message = "Client is required")
    @Size(max = 255, message = "Client name cannot exceed 255 characters")
    @Column(name = "client", nullable = false, length = 255)
    private String client;

    @NotBlank(message = "Project is required")
    @Size(max = 255, message = "Project name cannot exceed 255 characters")
    @Column(name = "project", nullable = false, length = 255)
    private String project;

    @NotBlank(message = "Phase is required")
    @Size(max = 255, message = "Phase name cannot exceed 255 characters")
    @Column(name = "phase", nullable = false, length = 255)
    private String phase;

    @NotNull(message = "Hours is required")
    @DecimalMin(value = "0.01", message = "Hours must be greater than 0")
    @DecimalMax(value = "24.00", message = "Hours cannot exceed 24")
    @Column(name = "taskhours", nullable = false, precision = 4, scale = 2)
    private BigDecimal hours;

    @Size(max = 255, message = "Details cannot exceed 255 characters")
    @Column(name = "details", nullable = true, length = 255)
    private String details;

    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username cannot exceed 50 characters")
    @Column(name = "username", nullable = false, length = 50)
    private String username;

    public TaskActivity() {}

    public TaskActivity(LocalDate taskDate, String client, String project, String phase,
            BigDecimal hours, String details, String username) {
        this.taskDate = taskDate;
        this.client = client;
        this.project = project;
        this.phase = phase;
        this.hours = hours;
        this.details = details;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // Task date getter/setter
    public LocalDate getTaskDate() {
        return taskDate;
    }

    public void setTaskDate(LocalDate taskDate) {
        this.taskDate = taskDate;
    }

    // Client getter/setter
    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    // Project getter/setter
    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    // Phase getter/setter
    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    // Hours getter/setter
    public BigDecimal getHours() {
        return hours;
    }

    public void setHours(BigDecimal hours) {
        this.hours = hours;
    }

    // Details getter/setter
    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    // Username getter/setter
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "TaskActivity{" + "id=" + id + ", taskDate=" + taskDate + ", client='" + client
                + '\'' + ", project='" + project + '\'' + ", phase='" + phase + '\'' + ", hours="
                + hours + ", details='" + details + '\'' + ", username='" + username + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TaskActivity))
            return false;
        TaskActivity that = (TaskActivity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
