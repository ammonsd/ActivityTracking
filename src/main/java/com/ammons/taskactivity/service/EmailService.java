package com.ammons.taskactivity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for sending email notifications.
 * 
 * <p>
 * This service handles various email notifications in the application, including:
 * <ul>
 * <li>Account lockout notifications</li>
 * <li>Security alerts</li>
 * <li>Administrative notifications</li>
 * </ul>
 * 
 * @author Dean Ammons
 * @version 1.0
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JavaMailSender mailSender;

    @Value("${spring.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:noreply@taskactivity.com}")
    private String fromAddress;

    @Value("${app.mail.admin-email:admin@taskactivity.com}")
    private String adminEmail;

    @Value("${app.name:Task Activity Management System}")
    private String appName;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Sends an account lockout notification email to the administrator.
     * 
     * <p>
     * This method is called when a user account is locked due to excessive failed login attempts.
     * The email contains details about the locked account, including:
     * <ul>
     * <li>Username of the locked account</li>
     * <li>Number of failed login attempts</li>
     * <li>Timestamp of the lockout</li>
     * <li>IP address of the last failed login attempt</li>
     * </ul>
     * 
     * @param username the username of the locked account
     * @param failedAttempts the number of failed login attempts
     * @param ipAddress the IP address of the last failed login attempt
     */
    public void sendAccountLockoutNotification(String username, int failedAttempts,
            String ipAddress) {
        if (!mailEnabled) {
            logger.info(
                    "Email notifications are disabled. Would have sent lockout notification for user: {}",
                    username);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(adminEmail);
            message.setSubject(String.format("[%s] Account Locked: %s", appName, username));

            String body = buildLockoutEmailBody(username, failedAttempts, ipAddress);
            message.setText(body);

            mailSender.send(message);
            logger.info("Account lockout notification email sent successfully for user: {}",
                    username);

        } catch (MailException e) {
            logger.error("Failed to send account lockout notification email for user: {}", username,
                    e);
            // Don't throw exception - email failure should not prevent login processing
        }
    }

    /**
     * Builds the email body for account lockout notifications.
     * 
     * @param username the username of the locked account
     * @param failedAttempts the number of failed login attempts
     * @param ipAddress the IP address of the last failed login attempt
     * @return formatted email body text
     */
    private String buildLockoutEmailBody(String username, int failedAttempts, String ipAddress) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);

        return String.format("""
                ACCOUNT LOCKOUT ALERT

                A user account has been locked due to excessive failed login attempts.

                Details:
                ----------------------------------------
                Username: %s
                Failed Login Attempts: %d
                Lockout Timestamp: %s
                IP Address: %s
                ----------------------------------------

                Action Required:
                Please investigate this potential security incident. The account can be unlocked \
                via the User Management interface if the activity is determined to be legitimate.

                To unlock the account:
                1. Log in to the %s
                2. Navigate to User Management
                3. Find the user '%s'
                4. Edit the user and uncheck 'Account is locked'
                5. Save changes

                This is an automated message from %s.
                """, username, failedAttempts, timestamp, ipAddress != null ? ipAddress : "Unknown",
                appName, username, appName);
    }

    /**
     * Tests the email configuration by sending a test email to the admin address. This can be used
     * to verify that email settings are correct.
     * 
     * @return true if the test email was sent successfully, false otherwise
     */
    public boolean sendTestEmail() {
        if (!mailEnabled) {
            logger.warn("Email notifications are disabled. Cannot send test email.");
            return false;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(adminEmail);
            message.setSubject(String.format("[%s] Test Email", appName));
            message.setText(
                    "This is a test email to verify email configuration is working correctly.");

            mailSender.send(message);
            logger.info("Test email sent successfully to: {}", adminEmail);
            return true;

        } catch (MailException e) {
            logger.error("Failed to send test email", e);
            return false;
        }
    }
}
