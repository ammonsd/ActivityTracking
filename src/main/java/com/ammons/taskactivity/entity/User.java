package com.ammons.taskactivity.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * User Entity
 *
 * @author Dean Ammons
 * @version 1.0
 */
@Entity
@Table(name = "users", schema = "public")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(name = "firstname", length = 50)
    private String firstname;

    @Column(name = "lastname", nullable = false, length = 50)
    private String lastname;

    @Column(name = "company", length = 100)
    private String company;

    @Column(name = "userpassword", nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "userrole", nullable = false, length = 20)
    private Role role = Role.USER;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "forcepasswordupdate", nullable = false)
    private boolean forcePasswordUpdate = true;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // Constructors
    public User() {
        this.createdDate = LocalDateTime.now(ZoneOffset.UTC);
    }

    public User(String username, String password, Role role) {
        this();
        this.username = username;
        this.password = password;
        this.role = role;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof User))
            return false;
        User user = (User) o;
        return username != null && username.equals(user.username);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "User{" + "id=" + id + ", username='" + username + '\'' + ", firstname='" + firstname
                + '\'' + ", lastname='" + lastname + '\'' + ", company='" + company + '\''
                + ", role=" + role + ", enabled=" + enabled + ", forcePasswordUpdate="
                + forcePasswordUpdate + ", createdDate=" + createdDate + ", lastLogin=" + lastLogin
                + '}';
        // Note: Password is intentionally excluded from toString for security
    }
}
