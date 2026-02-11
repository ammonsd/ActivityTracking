package com.ammons.taskactivity.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Description: Entity representing a user's password history entry.
 * 
 * This entity stores historical password hashes to prevent users from reusing recent passwords.
 * Only BCrypt hashed passwords are stored, never plain text. Old entries are automatically cleaned
 * up when they exceed the configured history size (default: 5 passwords).
 * 
 * Security Notes: - Password hashes are BCrypt (same format as User.userpassword) - Entries are
 * automatically deleted when user is deleted (CASCADE) - Cleanup is performed after each password
 * change - Used by PasswordValidationService to prevent password reuse
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since February 2026
 */
@Entity
@Table(name = "password_history",
        indexes = {@Index(name = "idx_password_history_user_id", columnList = "user_id"),
                @Index(name = "idx_password_history_changed_at", columnList = "changed_at")})
public class PasswordHistory {

    /**
     * Primary key - auto-generated sequence
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Foreign key to users table. CASCADE DELETE ensures password history is removed when user is
     * deleted. JPA relationship with ON DELETE CASCADE for proper cascade in both JPA and SQL.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    /**
     * Convenience field for direct access to user ID without loading the User entity. Read-only to
     * avoid conflicts with the user relationship.
     */
    @Column(name = "user_id", insertable = false, updatable = false)
    private Long userId;

    /**
     * BCrypt hash of the password (same format as User.userpassword). Never stores plain text
     * passwords. Used for comparison with BCryptPasswordEncoder.matches()
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * Timestamp when this password was set. Used for ordering (most recent first) and cleanup of
     * old entries.
     */
    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    // Constructors

    /**
     * Default constructor required by JPA
     */
    public PasswordHistory() {
        this.changedAt = LocalDateTime.now();
    }

    /**
     * Full constructor for creating new password history entries with User entity.
     * 
     * @param user the User entity
     * @param passwordHash the BCrypt hash of the password
     * @param changedAt the timestamp when password was changed
     */
    public PasswordHistory(User user, String passwordHash, LocalDateTime changedAt) {
        this.user = user;
        this.passwordHash = passwordHash;
        this.changedAt = changedAt != null ? changedAt : LocalDateTime.now();
    }

    /**
     * Full constructor for creating new password history entries.
     * 
     * @param userId the user's ID
     * @param passwordHash the BCrypt hash of the password
     * @param changedAt the timestamp when password was changed
     */
    public PasswordHistory(Long userId, String passwordHash, LocalDateTime changedAt) {
        this.userId = userId;
        // Create a minimal User object just to satisfy the relationship
        this.user = new User();
        this.user.setId(userId);
        this.passwordHash = passwordHash;
        this.changedAt = changedAt != null ? changedAt : LocalDateTime.now();
    }

    /**
     * Convenience constructor using current timestamp.
     * 
     * @param userId the user's ID
     * @param passwordHash the BCrypt hash of the password
     */
    public PasswordHistory(Long userId, String passwordHash) {
        this(userId, passwordHash, LocalDateTime.now());
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    // Object methods

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PasswordHistory that = (PasswordHistory) o;
        return Objects.equals(id, that.id) && Objects.equals(userId, that.userId)
                && Objects.equals(passwordHash, that.passwordHash)
                && Objects.equals(changedAt, that.changedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, passwordHash, changedAt);
    }

    @Override
    public String toString() {
        return "PasswordHistory{" + "id=" + id + ", userId=" + userId + ", passwordHash='***'" + // Never
                                                                                                 // log
                                                                                                 // actual
                                                                                                 // hash
                ", changedAt=" + changedAt + '}';
    }
}
