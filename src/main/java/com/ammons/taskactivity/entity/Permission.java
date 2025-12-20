package com.ammons.taskactivity.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a permission in the database-driven authorization system. Permissions are
 * defined as resource-action pairs (e.g., "TASK_ACTIVITY:CREATE").
 * 
 * <p>
 * Standard resources include:
 * <ul>
 * <li>TASK_ACTIVITY - Task activity management</li>
 * <li>EXPENSE - Expense tracking and approval</li>
 * <li>USER_MANAGEMENT - User and role administration</li>
 * <li>REPORTS - Report generation and viewing</li>
 * </ul>
 * 
 * <p>
 * Standard actions include: CREATE, READ, READ_ALL, UPDATE, DELETE, SUBMIT, APPROVE, REJECT,
 * MARK_REIMBURSED, MANAGE_RECEIPTS, MANAGE_ROLES
 * 
 * @see Roles
 * @see com.ammons.taskactivity.security.RequirePermission
 */
@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String resource; // "TASK_ACTIVITY", "USER_MANAGEMENT", etc.

    @Column(nullable = false, length = 50)
    private String action; // "READ", "CREATE", "UPDATE", "DELETE"

    @Column(length = 255)
    private String description;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    public Permission() {
        this.createdDate = LocalDateTime.now();
    }

    public Permission(String resource, String action) {
        this();
        this.resource = resource;
        this.action = action;
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

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Returns the permission key in the format "RESOURCE:ACTION". This is the format used
     * throughout the application for permission checks.
     * 
     * @return the permission key (e.g., "TASK_ACTIVITY:CREATE")
     */
    public String getPermissionKey() {
        return resource + ":" + action;
    }
}
