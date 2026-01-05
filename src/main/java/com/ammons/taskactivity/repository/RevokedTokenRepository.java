package com.ammons.taskactivity.repository;

import com.ammons.taskactivity.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for managing revoked JWT tokens (blacklist).
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@Repository
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    /**
     * Check if a token (by JTI) is revoked
     */
    boolean existsByJti(String jti);

    /**
     * Find a revoked token by JTI
     */
    Optional<RevokedToken> findByJti(String jti);

    /**
     * Delete all revoked tokens that have expired (cleanup) Should be called periodically to
     * prevent table growth
     */
    @Modifying
    @Query("DELETE FROM RevokedToken r WHERE r.expirationTime < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Count expired tokens (for monitoring)
     */
    @Query("SELECT COUNT(r) FROM RevokedToken r WHERE r.expirationTime < :now")
    long countExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Get all revoked tokens for a specific user
     */
    @Query("SELECT r FROM RevokedToken r WHERE r.username = :username ORDER BY r.revokedAt DESC")
    java.util.List<RevokedToken> findByUsername(@Param("username") String username);
}
