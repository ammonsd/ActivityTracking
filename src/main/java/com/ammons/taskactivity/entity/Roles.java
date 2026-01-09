package com.ammons.taskactivity.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a user role in the database-driven authorization system. Roles are associated
 * with a set of permissions that define what actions users with this role can perform.
 * 
 * <p>
 * Standard roles include:
 * <ul>
 * <li>ADMIN - Full system access</li>
 * <li>USER - Standard user access</li>
 * <li>GUEST - Limited read-only access</li>
 * <li>EXPENSE_ADMIN - Expense management and approval capabilities</li>
 * </ul>
 * 
 * @see Permission
 * @see User
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since December 2025
 */
@Entity
@Table(name = "roles")
public class Roles {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String name; // "ADMIN", "USER", "GUEST", "EXPENSE_ADMIN"

    @Column(length = 255)
    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    public Roles() {
        this.createdDate = LocalDateTime.now();
    }

    public Roles(String name) {
        this();
        this.name = name;
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

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Adds a permission to this role.
     * 
     * @param permission the permission to add
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since December 2025
     */
    public void addPermission(Permission permission) {
        this.permissions.add(permission);
    }

    /**
     * Removes a permission from this role.
     * 
     * @param permission the permission to remove
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since December 2025
     */
    public void removePermission(Permission permission) {
        this.permissions.remove(permission);
    }
}
