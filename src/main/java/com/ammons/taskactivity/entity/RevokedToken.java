package com.ammons.taskactivity.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for tracking revoked JWT tokens (blacklist).
 * 
 * Used for: - User logout (revoke access and refresh tokens) - Security incidents (revoke
 * compromised tokens) - Password changes (revoke all existing tokens)
 * 
 * Tokens are stored by their JTI (JWT ID) claim with expiration time. Expired entries can be
 * periodically cleaned up.
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@Entity
@Table(name = "revoked_tokens", indexes = {@Index(name = "idx_jti", columnList = "jti"),
        @Index(name = "idx_expiration", columnList = "expiration_time")})
public class RevokedToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * JWT ID (jti claim) - unique identifier for the token
     */
    @Column(nullable = false, unique = true, length = 100)
    private String jti;

    /**
     * Username associated with the token
     */
    @Column(nullable = false, length = 100)
    private String username;

    /**
     * Token type: "access" or "refresh"
     */
    @Column(name = "token_type", nullable = false, length = 20)
    private String tokenType;

    /**
     * When the token expires (from JWT exp claim) Used for automatic cleanup of old revoked tokens
     */
    @Column(name = "expiration_time", nullable = false)
    private LocalDateTime expirationTime;

    /**
     * When the token was revoked
     */
    @Column(name = "revoked_at", nullable = false)
    private LocalDateTime revokedAt;

    /**
     * Reason for revocation: "logout", "password_change", "security_incident", "manual"
     */
    @Column(length = 50)
    private String reason;

    // Constructors
    public RevokedToken() {}

    public RevokedToken(String jti, String username, String tokenType, LocalDateTime expirationTime,
            String reason) {
        this.jti = jti;
        this.username = username;
        this.tokenType = tokenType;
        this.expirationTime = expirationTime;
        this.revokedAt = LocalDateTime.now();
        this.reason = reason;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public LocalDateTime getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(LocalDateTime expirationTime) {
        this.expirationTime = expirationTime;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
