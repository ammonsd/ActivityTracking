package com.ammons.taskactivity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.util.HashMap;
import java.util.Map;

/**
 * Session-scoped service to track original passwords when a user logs in. This prevents users from
 * changing their password back to the original during the same session.
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since December 2025
 */
@Service
@SessionScope
public class PasswordHistoryService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordHistoryService.class);

    // Map of username -> original password hash at login
    private final Map<String, String> originalPasswordHashes = new HashMap<>();

    /**
     * Store the original password hash when user logs in.
     * 
     * @param username the username
     * @param passwordHash the hashed password
     */
    public void storeOriginalPassword(String username, String passwordHash) {
        if (username != null && passwordHash != null) {
            originalPasswordHashes.put(username, passwordHash);
            logger.debug("Stored original password hash for user: {}", username);
        }
    }

    /**
     * Get the original password hash for a user.
     * 
     * @param username the username
     * @return the original password hash, or null if not found
     */
    public String getOriginalPasswordHash(String username) {
        return originalPasswordHashes.get(username);
    }

    /**
     * Clear the stored password hash for a user (e.g., on logout).
     * 
     * @param username the username
     */
    public void clearOriginalPassword(String username) {
        originalPasswordHashes.remove(username);
        logger.debug("Cleared original password hash for user: {}", username);
    }

    /**
     * Clear all stored password hashes.
     */
    public void clearAll() {
        originalPasswordHashes.clear();
    }
}
