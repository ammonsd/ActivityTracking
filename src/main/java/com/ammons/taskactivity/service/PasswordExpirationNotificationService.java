package com.ammons.taskactivity.service;

import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Scheduled service to send password expiration warning notifications.
 * 
 * <p>
 * This service runs daily to check for users whose passwords are expiring soon and sends them email
 * notifications. Warnings are sent when passwords will expire within 7 days.
 * 
 * <p>
 * Key Features:
 * <ul>
 * <li>Runs daily at 8 AM server time</li>
 * <li>Checks all users with expiration dates set</li>
 * <li>Sends warnings for passwords expiring within 7 days</li>
 * <li>Skips GUEST users (they cannot change their own passwords)</li>
 * <li>Skips users without email addresses</li>
 * <li>Respects mail.enabled configuration</li>
 * </ul>
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@Service
public class PasswordExpirationNotificationService {

    private static final Logger logger =
            LoggerFactory.getLogger(PasswordExpirationNotificationService.class);

    private final UserRepository userRepository;
    private final EmailService emailService;

    public PasswordExpirationNotificationService(UserRepository userRepository,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    /**
     * Check for expiring passwords and send email notifications.
     * 
     * Runs daily at 8:00 AM server time. Sends warnings to users whose passwords will expire within
     * the next 7 days, and sends expired notification on the first day after expiration
     * (yesterday).
     */
    @Scheduled(cron = "0 0 8 * * *") // Daily at 8 AM
    public void checkExpiringPasswordsAndNotify() {
        logger.info("Starting daily password expiration check...");

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate yesterday = today.minusDays(1);
        LocalDate sevenDaysFromNow = today.plusDays(7);

        // Find all users with expiration dates in the warning window
        List<User> allUsers = userRepository.findAll();
        int expiringNotificationsSent = 0;
        int expiredNotificationsSent = 0;
        int skippedUsers = 0;

        for (User user : allUsers) {
            try {
                // Skip if no expiration date set
                if (user.getExpirationDate() == null) {
                    continue;
                }

                // Skip GUEST users - they cannot change their own passwords
                if ("GUEST".equalsIgnoreCase(user.getRole().getName())) {
                    logger.debug("Skipping GUEST user: {}", user.getUsername());
                    skippedUsers++;
                    continue;
                }

                // Skip users without email addresses
                if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
                    logger.debug("Skipping user {} - no email address", user.getUsername());
                    skippedUsers++;
                    continue;
                }

                // Skip disabled or locked accounts
                if (!user.isEnabled() || user.isAccountLocked()) {
                    logger.debug("Skipping disabled/locked user: {}", user.getUsername());
                    skippedUsers++;
                    continue;
                }

                LocalDate expirationDate = user.getExpirationDate();

                // Check if password expired YESTERDAY (first day after expiration)
                // This sends the expired notification only once - on the day after expiration
                if (expirationDate.isEqual(yesterday)) {
                    // Send expired notification (only once, because tomorrow it won't equal
                    // yesterday)
                    String fullName = buildFullName(user);
                    emailService.sendPasswordExpiredNotification(user.getEmail(),
                            user.getUsername(), fullName);

                    expiredNotificationsSent++;
                    logger.info("Sent password EXPIRED notification to user: {} (expired on: {})",
                            user.getUsername(), expirationDate);
                }
                // Check if password expires within warning window (0-7 days from now)
                // Include today (day 0) to ensure notification on expiration day
                else if (!expirationDate.isBefore(today)
                        && !expirationDate.isAfter(sevenDaysFromNow)) {
                    long daysUntilExpiration =
                            java.time.temporal.ChronoUnit.DAYS.between(today, expirationDate);

                    // Send warning notification
                    String fullName = buildFullName(user);
                    emailService.sendPasswordExpirationWarning(user.getEmail(), user.getUsername(),
                            fullName, daysUntilExpiration);

                    expiringNotificationsSent++;
                    logger.info("Sent password expiration warning to user: {} ({} days remaining)",
                            user.getUsername(), daysUntilExpiration);
                }

            } catch (Exception e) {
                logger.error("Error processing password expiration check for user {}: {}",
                        user.getUsername(), e.getMessage(), e);
            }
        }

        logger.info(
                "Password expiration check complete. Expiring warnings sent: {}, Expired notifications sent: {}, Skipped users: {}, Total checked: {}",
                expiringNotificationsSent, expiredNotificationsSent, skippedUsers, allUsers.size());
    }

    /**
     * Manual trigger for testing purposes. Can be called from a controller or management endpoint
     * to test the notification system without waiting for the scheduled run.
     * 
     * @return number of notifications sent
     */
    public int triggerManualCheck() {
        logger.info("Manual password expiration check triggered");
        checkExpiringPasswordsAndNotify();
        return 0; // Return value for testing/monitoring
    }

    /**
     * Build full name from user's firstname and lastname.
     * 
     * @param user the user
     * @return formatted full name or null if both are empty
     */
    private String buildFullName(User user) {
        String firstname = user.getFirstname();
        String lastname = user.getLastname();

        if (firstname != null && !firstname.trim().isEmpty()) {
            return firstname.trim() + " " + (lastname != null ? lastname.trim() : "").trim();
        }

        return lastname != null ? lastname.trim() : null;
    }
}
