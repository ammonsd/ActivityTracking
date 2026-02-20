package com.ammons.taskactivity.entity;

import jakarta.persistence.*;

/**
 * Description: Entity representing a user's access to a specific dropdown value (Client or
 * Project). Used to restrict which clients and projects appear in dropdowns based on the
 * authenticated user. Values with allUsers=true on DropdownValue do not require a row here — they
 * are visible to everyone. ADMIN role bypasses all access checks.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since February 2026
 */
@Entity
@Table(name = "user_dropdown_access", schema = "public",
        indexes = {@Index(name = "idx_uda_username", columnList = "username"),
                @Index(name = "idx_uda_dropdown_value_id", columnList = "dropdown_value_id")},
        uniqueConstraints = {@UniqueConstraint(name = "uk_uda",
                columnNames = {"username", "dropdown_value_id"})})
public class UserDropdownAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dropdown_value_id", nullable = false)
    private DropdownValue dropdownValue;

    public UserDropdownAccess() {}

    public UserDropdownAccess(String username, DropdownValue dropdownValue) {
        this.username = username;
        this.dropdownValue = dropdownValue;
    }

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

    public DropdownValue getDropdownValue() {
        return dropdownValue;
    }

    public void setDropdownValue(DropdownValue dropdownValue) {
        this.dropdownValue = dropdownValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof UserDropdownAccess))
            return false;
        UserDropdownAccess that = (UserDropdownAccess) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "UserDropdownAccess{id=" + id + ", username='" + username + "', dropdownValueId="
                + (dropdownValue != null ? dropdownValue.getId() : null) + '}';
    }
}
