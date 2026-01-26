package com.ammons.taskactivity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing password reset tokens in-memory.
 * 
 * Tokens are stored temporarily in memory with a short expiration time for security. Tokens are
 * automatically cleaned up on expiration or application restart.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@Service
public class PasswordResetService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int TOKEN_EXPIRY_MINUTES = 15;

    // In-memory store: token -> PasswordResetToken
    private final ConcurrentHashMap<String, PasswordResetToken> resetTokens =
            new ConcurrentHashMap<>();

    /**
     * Inner class to hold token data
     */
    private static class PasswordResetToken {
        final String email;
        final LocalDateTime expiryTime;

        PasswordResetToken(String email, LocalDateTime expiryTime) {
            this.email = email;
            this.expiryTime = expiryTime;
        }
    }

    /**
     * Generate a unique reset token for the given email address.
     * 
     * @param email the user's email address
     * @return the generated token
     */
    public String generateResetToken(String email) {
        String token = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES);
        resetTokens.put(token, new PasswordResetToken(email, expiry));
        logger.info("Generated password reset token for email: {} (expires in {} minutes)", email,
                TOKEN_EXPIRY_MINUTES);
        return token;
    }

    /**
     * Validate a reset token and return the associated email if valid.
     * 
     * @param token the reset token to validate
     * @return Optional containing the email if token is valid, empty otherwise
     */
    public Optional<String> validateToken(String token) {
        PasswordResetToken resetToken = resetTokens.get(token);

        if (resetToken == null) {
            logger.warn("Password reset attempted with non-existent token");
            return Optional.empty();
        }

        if (LocalDateTime.now().isAfter(resetToken.expiryTime)) {
            logger.warn("Password reset attempted with expired token for email: {}",
                    resetToken.email);
            resetTokens.remove(token); // Cleanup expired token
            return Optional.empty();
        }

        logger.debug("Valid password reset token found for email: {}", resetToken.email);
        return Optional.of(resetToken.email);
    }

    /**
     * Consume a token after successful password reset. Tokens are single-use only.
     * 
     * @param token the token to consume
     */
    public void consumeToken(String token) {
        PasswordResetToken removed = resetTokens.remove(token);
        if (removed != null) {
            logger.info("Password reset token consumed for email: {}", removed.email);
        }
    }

    /**
     * Get the token expiry time in minutes.
     * 
     * @return token expiry duration in minutes
     */
    public int getTokenExpiryMinutes() {
        return TOKEN_EXPIRY_MINUTES;
    }

    /**
     * Scheduled task to cleanup expired tokens. Runs every 5 minutes to remove expired tokens from
     * memory.
     */
    @Scheduled(fixedRate = 300000) // Every 5 minutes
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        int initialSize = resetTokens.size();

        resetTokens.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiryTime));

        int removed = initialSize - resetTokens.size();
        if (removed > 0) {
            logger.info("Cleaned up {} expired password reset token(s)", removed);
        }
    }

    /**
     * Get the current number of active reset tokens (for monitoring/debugging).
     * 
     * @return number of active tokens
     */
    public int getActiveTokenCount() {
        return resetTokens.size();
    }
}
