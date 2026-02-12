package com.ammons.taskactivity.service;

import com.ammons.taskactivity.entity.RevokedToken;
import com.ammons.taskactivity.repository.RevokedTokenRepository;
import com.ammons.taskactivity.security.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

/**
 * Service for managing JWT token revocation (blacklist).
 * 
 * Provides: - Token revocation on logout - Token revocation on password change - Token revocation
 * for security incidents - Automatic cleanup of expired tokens
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@Service
public class TokenRevocationService {

    private static final Logger logger = LoggerFactory.getLogger(TokenRevocationService.class);
    private static final String REASON_PASSWORD_CHANGE_ALL = "password_change_all";
    private static final String TOKEN_TYPE_CUTOFF_MARKER = "cutoff_marker";

    private final RevokedTokenRepository revokedTokenRepository;
    private final JwtUtil jwtUtil;

    public TokenRevocationService(RevokedTokenRepository revokedTokenRepository, JwtUtil jwtUtil) {
        this.revokedTokenRepository = revokedTokenRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Revoke a single token by its JWT string. Extracts JTI, username, and expiration from the
     * token.
     * 
     * @param token JWT token string
     * @param reason Reason for revocation
     * @return true if revoked successfully, false if token invalid or already revoked
     */
    @Transactional
    public boolean revokeToken(String token, String reason) {
        try {
            // Extract claims from token
            Claims claims = jwtUtil.extractAllClaims(token);
            String jti = claims.getId();
            String username = claims.getSubject();
            String tokenType = claims.get("token_type", String.class);
            Date expiration = claims.getExpiration();

            if (jti == null) {
                logger.warn("Token does not have JTI claim, cannot revoke");
                return false;
            }

            // Check if already revoked
            if (isTokenRevoked(jti)) {
                logger.debug("Token already revoked: {}", jti);
                return false;
            }

            // Create revoked token entry
            LocalDateTime expirationTime =
                    expiration.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

            RevokedToken revokedToken = new RevokedToken(jti, username,
                    tokenType != null ? tokenType : "unknown", expirationTime, reason);

            revokedTokenRepository.save(revokedToken);
            logger.info("[Token Revocation] Token revoked for user {}: JTI={}, Reason={}", username,
                    jti, reason);

            return true;

        } catch (Exception e) {
            logger.error("[Token Revocation] Failed to revoke token: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Check if a token (by JTI) is revoked. This should be called during JWT validation.
     * 
     * @param jti JWT ID
     * @return true if revoked, false otherwise
     */
    public boolean isTokenRevoked(String jti) {
        if (jti == null) {
            return false;
        }
        return revokedTokenRepository.existsByJti(jti);
    }

    /**
     * Revoke all tokens for a user (e.g., on password change). Note: This only works for tokens
     * with JTI claims.
     * 
     * @param username Username
     * @param reason Reason for revocation
     */
    @Transactional
    public void revokeAllUserTokens(String username, String reason) {
        logger.info("[Token Revocation] Revoking all tokens for user {}: Reason={}", username,
                reason);

        // Modified by: Dean Ammons - February 2026
        // Change: Persist a user-level cutoff marker to invalidate all tokens issued before
        // password change
        // Reason: Ensure password changes revoke previously issued JWTs, even if they were not
        // individually blacklisted
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime markerExpiration = now.plusDays(30);
        String markerJti = "REVOKE_ALL_" + username + "_" + UUID.randomUUID();

        RevokedToken cutoffMarker = new RevokedToken(markerJti, username, TOKEN_TYPE_CUTOFF_MARKER,
                markerExpiration, REASON_PASSWORD_CHANGE_ALL);
        revokedTokenRepository.save(cutoffMarker);
        logger.info("[Token Revocation] Created revoke-all cutoff marker for user {} at {}",
                username, now);
    }

    /**
     * Validate whether a token was issued before the latest password-change revocation marker for a
     * user.
     *
     * @param username username from JWT subject
     * @param tokenIssuedAt token iat claim
     * @return true if token should be rejected due to password-change cutoff
     */
    public boolean isTokenIssuedBeforePasswordChangeRevocation(String username,
            Date tokenIssuedAt) {
        if (username == null || tokenIssuedAt == null) {
            return false;
        }

        LocalDateTime latestCutoff = revokedTokenRepository
                .findLatestRevocationTimeByUsernameAndReason(username, REASON_PASSWORD_CHANGE_ALL);

        if (latestCutoff == null) {
            return false;
        }

        LocalDateTime issuedAt =
                tokenIssuedAt.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        return issuedAt.isBefore(latestCutoff);
    }

    /**
     * Cleanup expired revoked tokens. Runs daily at 2 AM to keep the revoked_tokens table from
     * growing indefinitely.
     */
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        long expiredCount = revokedTokenRepository.countExpiredTokens(now);

        if (expiredCount > 0) {
            int deleted = revokedTokenRepository.deleteExpiredTokens(now);
            logger.info("[Token Revocation Cleanup] Deleted {} expired revoked tokens", deleted);
        } else {
            logger.debug("[Token Revocation Cleanup] No expired tokens to clean up");
        }
    }
}
