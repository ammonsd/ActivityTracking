package com.ammons.taskactivity.service;

import com.ammons.taskactivity.dto.LoginAuditDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service to track login attempts in memory and provide audit trail. This stores recent login
 * attempts (last 1000) for monitoring purposes.
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since November 2025
 */
@Service
public class LoginAuditService {

    private static final Logger logger = LoggerFactory.getLogger(LoginAuditService.class);
    private static final int MAX_AUDIT_ENTRIES = 1000;

    private final ConcurrentLinkedDeque<LoginAuditDto> auditLog = new ConcurrentLinkedDeque<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    /**
     * Record a login attempt
     * 
     * @param username the username attempting to login
     * @param ipAddress the IP address of the login attempt
     * @param location the location/description of the login
     * @param successful whether the login was successful
     */
    public void recordLoginAttempt(String username, String ipAddress, String location,
            boolean successful) {
        // Use Eastern Time for consistency across local and AWS deployments
        LocalDateTime loginTime =
                ZonedDateTime.now(ZoneId.of("America/New_York")).toLocalDateTime();

        LoginAuditDto audit = new LoginAuditDto(idGenerator.getAndIncrement(), username,
                loginTime, ipAddress, location, successful);

        auditLog.addFirst(audit); // Add to front (most recent first)

        // Remove oldest entries if we exceed max size
        while (auditLog.size() > MAX_AUDIT_ENTRIES) {
            auditLog.removeLast();
        }

        // Log for CloudWatch and local logs
        if (successful) {
            logger.info("LOGIN_SUCCESS: user='{}', ip='{}', location='{}', timestamp='{}'",
                    username, ipAddress, location, audit.getLoginTime());
        } else {
            logger.warn("LOGIN_FAILED: user='{}', ip='{}', location='{}', timestamp='{}'", username,
                    ipAddress, location, audit.getLoginTime());
        }
    }

    /**
     * Get recent login attempts for a specific username
     * 
     * @param username the username to filter by
     * @param limit maximum number of entries to return
     * @return list of login audit entries
     */
    public List<LoginAuditDto> getLoginAuditByUsername(String username, int limit) {
        return auditLog.stream().filter(audit -> audit.getUsername().equals(username)).limit(limit)
                .toList();
    }

    /**
     * Get all recent login attempts
     * 
     * @param limit maximum number of entries to return
     * @return list of login audit entries
     */
    public List<LoginAuditDto> getRecentLoginAudits(int limit) {
        return auditLog.stream().limit(limit).toList();
    }

    /**
     * Clear all audit entries (for testing purposes)
     */
    public void clearAuditLog() {
        auditLog.clear();
        logger.info("Login audit log cleared");
    }
}
