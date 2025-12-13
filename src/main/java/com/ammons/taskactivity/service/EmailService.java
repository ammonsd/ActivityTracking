package com.ammons.taskactivity.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

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
 * <p>
 * Supports two methods for sending emails:
 * <ul>
 * <li><b>SMTP</b> - Traditional SMTP using JavaMailSender (requires username/password)</li>
 * <li><b>AWS SDK</b> - AWS SES SDK using IAM role credentials (no username/password needed)</li>
 * </ul>
 * 
 * <p>
 * The AWS SDK method is recommended for AWS deployments (ECS/EC2) as it uses IAM role credentials
 * automatically, eliminating the need to manage SMTP credentials.
 * 
 * @author Dean Ammons
 * @version 2.0
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JavaMailSender mailSender;
    private SesClient sesClient;

    @Value("${spring.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${spring.mail.use-aws-sdk:false}")
    private boolean useAwsSdk;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @Value("${app.mail.from:noreply@taskactivity.com}")
    private String fromAddress;

    @Value("${app.mail.admin-email:admin@taskactivity.com}")
    private String adminEmail;

    @Value("${app.mail.expense-approvers:}")
    private String expenseApprovers;

    @Value("${app.name:Task Activity Management System}")
    private String appName;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Initialize AWS SES client if AWS SDK method is enabled. Uses DefaultCredentialsProvider which
     * automatically uses ECS task role credentials. Called after dependency injection is complete
     * to ensure @Value fields are populated.
     */
    @PostConstruct
    private void initializeSesClient() {
        if (useAwsSdk) {
            try {
                sesClient = SesClient.builder().region(Region.of(awsRegion))
                        .credentialsProvider(DefaultCredentialsProvider.create()).build();
                logger.info("AWS SES client initialized for region: {}", awsRegion);
            } catch (Exception e) {
                logger.error("Failed to initialize AWS SES client", e);
                sesClient = null;
            }
        }
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
     * @param fullName the full name of the user (optional, can be null)
     * @param failedAttempts the number of failed login attempts
     * @param ipAddress the IP address of the last failed login attempt
     */
    public void sendAccountLockoutNotification(String username, String fullName, int failedAttempts,
            String ipAddress) {
        if (!mailEnabled) {
            logger.info(
                    "Email notifications are disabled. Would have sent lockout notification for user: {}",
                    username);
            return;
        }

        if (adminEmail == null || adminEmail.trim().isEmpty()) {
            logger.warn(
                    "No admin email configured - skipping account lockout notification for user: {}",
                    username);
            return;
        }

        String subject = String.format("[%s] Account Locked: %s", appName, username);
        String body = buildLockoutEmailBody(username, fullName, failedAttempts, ipAddress);

        // Parse comma-separated admin emails
        String[] adminEmails = adminEmail.split(",");
        for (String email : adminEmails) {
            String trimmedEmail = email.trim();
            if (trimmedEmail.isEmpty()) {
                continue;
            }

            try {
                if (useAwsSdk && sesClient != null) {
                    sendEmailViaAwsSdk(trimmedEmail, subject, body);
                } else {
                    sendEmailViaSmtp(trimmedEmail, subject, body);
                }
                logger.info("Account lockout notification sent to {} for user: {}", trimmedEmail,
                        username);
            } catch (Exception e) {
                logger.error("Failed to send lockout notification to {}: {}", trimmedEmail,
                        e.getMessage(), e);
            }
        }
    }

    /**
     * Send email using AWS SES SDK. Uses IAM role credentials (no username/password needed).
     * 
     * @param to recipient email address
     * @param subject email subject
     * @param body email body text
     */
    private void sendEmailViaAwsSdk(String to, String subject, String body) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .destination(Destination.builder().toAddresses(to).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).charset("UTF-8").build())
                            .body(Body.builder()
                                    .text(Content.builder().data(body).charset("UTF-8").build())
                                    .build())
                            .build())
                    .source(fromAddress).build();

            SendEmailResponse response = sesClient.sendEmail(request);
            logger.info("Email sent successfully via AWS SES. MessageId: {}", response.messageId());

        } catch (Exception e) {
            logger.error("Failed to send email via AWS SES to: {}", to, e);
            // Don't throw exception - email failure should not prevent login processing
        }
    }

    /**
     * Send email using traditional SMTP. Requires SMTP username/password configuration.
     * 
     * @param to recipient email address
     * @param subject email subject
     * @param body email body text
     */
    private void sendEmailViaSmtp(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            logger.info("Email sent successfully via SMTP to: {}", to);

        } catch (MailException e) {
            logger.error("Failed to send email via SMTP to: {}", to, e);
            // Don't throw exception - email failure should not prevent login processing
        }
    }

    /**
     * Send notification when GUEST role user logs in.
     * 
     * @param username the username of the GUEST user
     * @param fullName the full name of the user (optional, can be null)
     * @param ipAddress the IP address of the login
     * @param location the geographic location of the login
     */
    public void sendGuestLoginNotification(String username, String fullName, String ipAddress,
            String location) {
        if (!mailEnabled) {
            logger.info(
                    "Email notifications are disabled. Would have sent GUEST login notification");
            return;
        }

        if (adminEmail == null || adminEmail.trim().isEmpty()) {
            logger.warn(
                    "No admin email configured - skipping GUEST login notification for user: {}",
                    username);
            return;
        }

        String subject = String.format("[%s] GUEST User Login", appName);
        String body = buildGuestLoginEmailBody(username, fullName, ipAddress, location);

        // Parse comma-separated admin emails
        String[] adminEmails = adminEmail.split(",");
        for (String email : adminEmails) {
            String trimmedEmail = email.trim();
            if (trimmedEmail.isEmpty()) {
                continue;
            }

            try {
                if (useAwsSdk && sesClient != null) {
                    sendEmailViaAwsSdk(trimmedEmail, subject, body);
                } else {
                    sendEmailViaSmtp(trimmedEmail, subject, body);
                }
                logger.info("GUEST login notification sent to {} for user: {}", trimmedEmail,
                        username);
            } catch (Exception e) {
                logger.error("Failed to send GUEST login notification to {}: {}", trimmedEmail,
                        e.getMessage(), e);
            }
        }
    }

    /**
     * Builds the email body for guest login notifications.
     * 
     * @param username the username of the GUEST user
     * @param fullName the full name of the user (optional, can be null)
     * @param ipAddress the IP address of the login
     * @param location the geographic location of the login
     * @return formatted email body text
     */
    private String buildGuestLoginEmailBody(String username, String fullName, String ipAddress,
            String location) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);

        StringBuilder details = new StringBuilder();
        if (fullName != null) {
            details.append(String.format("Name:               %s%n", fullName));
        }
        details.append(String.format("Username:           %s", username));

        return String
                .format("""
                A user with GUEST role has logged into the system.

                Details:
                ----------------------------------------
                %s
                Login Timestamp:    %s
                IP Address:         %s
                Location:           %s
                ----------------------------------------

                This is an automated notification from %s.
                Do not reply to this email. This email is sent from an unattended mailbox.
                """, details.toString(), timestamp, ipAddress != null ? ipAddress : "Unknown",
                location != null ? location : "Unknown", appName);
    }

    /**
     * Builds the email body for account lockout notifications.
     * 
     * @param username the username of the locked account
     * @param fullName the full name of the user (optional, can be null)
     * @param failedAttempts the number of failed login attempts
     * @param ipAddress the IP address of the last failed login attempt
     * @return formatted email body text
     */
    private String buildLockoutEmailBody(String username, String fullName, int failedAttempts,
            String ipAddress) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);

        StringBuilder details = new StringBuilder();
        if (fullName != null) {
            details.append(String.format("Name:               %s%n", fullName));
        }
        details.append(String.format("Username:           %s", username));

        return String.format("""
                ACCOUNT LOCKOUT ALERT

                A user account has been locked due to excessive failed login attempts.

                Details:
                ----------------------------------------
                %s
                Failed Attempts:    %d
                Lockout Timestamp:  %s
                IP Address:         %s
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
                Do not reply to this email. This email is sent from an unattended mailbox.
                """, details.toString(), failedAttempts, timestamp,
                ipAddress != null ? ipAddress : "Unknown", appName, username, appName);
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

        if (adminEmail == null || adminEmail.trim().isEmpty()) {
            logger.warn("No admin email configured - cannot send test email");
            return false;
        }

        String method = useAwsSdk ? "AWS SES SDK" : "SMTP";
        String subject = String.format("[%s] Test Email", appName);

        // Parse comma-separated admin emails
        String[] adminEmails = adminEmail.split(",");
        boolean allSuccessful = true;

        for (String email : adminEmails) {
            String trimmedEmail = email.trim();
            if (trimmedEmail.isEmpty()) {
                continue;
            }

            String body = String.format(
                    "This is a test email to verify email configuration is working correctly.\n\n"
                            + "Email method: %s\n" + "From: %s\n" + "To: %s\n" + "AWS Region: %s\n"
                            + "Timestamp: %s",
                    method, fromAddress, trimmedEmail, awsRegion,
                    LocalDateTime.now().format(DATE_FORMATTER));

            try {
                if (useAwsSdk && sesClient != null) {
                    sendEmailViaAwsSdk(trimmedEmail, subject, body);
                } else {
                    sendEmailViaSmtp(trimmedEmail, subject, body);
                }
                logger.info("Test email sent successfully to {}", trimmedEmail);
            } catch (Exception e) {
                logger.error("Failed to send test email to {}: {}", trimmedEmail, e.getMessage(),
                        e);
                allSuccessful = false;
            }
        }

        return allSuccessful;
    }

    /**
     * Send expense submitted notification to approvers.
     * 
     * @param username the username of the expense submitter
     * @param fullName the full name of the user (optional)
     * @param expenseId the ID of the expense
     * @param expenseDescription brief description of the expense
     * @param amount the expense amount
     * @param currency the currency code (e.g., USD)
     * @param expenseDate the date of the expense
     */
    public void sendExpenseSubmittedNotification(String username, String fullName, Long expenseId,
            String expenseDescription, String amount, String currency, String expenseDate) {
        if (!mailEnabled) {
            logger.debug("Email notifications disabled - skipping expense submission notification");
            return;
        }

        if (expenseApprovers == null || expenseApprovers.trim().isEmpty()) {
            logger.warn(
                    "No expense approvers configured - skipping expense submission notification");
            return;
        }

        // Parse comma-separated approver emails
        String[] approverEmails = expenseApprovers.split(",");
        if (approverEmails.length == 0) {
            logger.warn("No valid expense approver emails found");
            return;
        }

        String subject = String.format("[%s] New Expense Submitted - %s", appName, expenseId);
        String body = buildExpenseSubmittedEmailBody(username, fullName, expenseId,
                expenseDescription, amount, currency, expenseDate);

        // Send to all configured approvers
        for (String approverEmail : approverEmails) {
            String email = approverEmail.trim();
            if (email.isEmpty()) {
                continue;
            }

            try {
                if (useAwsSdk && sesClient != null) {
                    sendEmailViaAwsSdk(email, subject, body);
                } else {
                    sendEmailViaSmtp(email, subject, body);
                }
                logger.info("Expense submission notification sent to {} for expense ID: {}", email,
                        expenseId);
            } catch (Exception e) {
                logger.error("Failed to send expense submission notification to {}: {}", email,
                        e.getMessage(), e);
            }
        }
    }

    /**
     * Builds the email body for expense submission notifications.
     */
    private String buildExpenseSubmittedEmailBody(String username, String fullName, Long expenseId,
            String expenseDescription, String amount, String currency, String expenseDate) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER); // Uses local time

        StringBuilder details = new StringBuilder();
        if (fullName != null && !fullName.trim().isEmpty()) {
            details.append(String.format("Submitted By:       %s (%s)%n", fullName, username));
        } else {
            details.append(String.format("Submitted By:       %s%n", username));
        }
        details.append(String.format("Description:        %s%n", expenseDescription));
        details.append(String.format("Amount:             %s %s%n", amount, currency));
        details.append(String.format("Expense Date:       %s%n", expenseDate));
        details.append(String.format("Submitted:          %s", timestamp));

        return String.format("""
                NEW EXPENSE SUBMITTED FOR APPROVAL

                A new expense has been submitted and is awaiting your review.

                Expense Details:
                ----------------------------------------
                %s
                ----------------------------------------

                This is an automated notification from %s.
                Do not reply to this email. This email is sent from an unattended mailbox.
                """, details.toString(), appName, expenseId, appName);
    }

    /**
     * Send expense status change notification to user.
     * 
     * @param userEmail the email address of the expense owner
     * @param username the username of the expense owner
     * @param fullName the full name of the user (optional)
     * @param expenseId the ID of the expense
     * @param expenseDescription brief description of the expense
     * @param amount the expense amount
     * @param currency the currency code (e.g., USD)
     * @param newStatus the new status (Approved, Rejected, Reimbursed)
     * @param notes any notes from the approver/processor
     * @param processedBy username of who processed the expense
     */
    public void sendExpenseStatusNotification(String userEmail, String username, String fullName,
            Long expenseId, String expenseDescription, String amount, String currency,
            String newStatus, String notes, String processedBy) {
        if (!mailEnabled) {
            logger.debug("Email notifications disabled - skipping expense status notification");
            return;
        }

        if (userEmail == null || userEmail.trim().isEmpty()) {
            logger.warn("Cannot send expense notification - user {} has no email address",
                    username);
            return;
        }

        String subject = String.format("[%s] Expense %s - %s", appName, newStatus, expenseId);
        String body = buildExpenseStatusEmailBody(username, fullName, expenseId, expenseDescription,
                amount, currency, newStatus, notes, processedBy);

        try {
            if (useAwsSdk && sesClient != null) {
                sendEmailViaAwsSdk(userEmail, subject, body);
            } else {
                sendEmailViaSmtp(userEmail, subject, body);
            }
            logger.info("Expense status notification sent to {} for expense ID: {}", userEmail,
                    expenseId);
        } catch (Exception e) {
            logger.error("Failed to send expense status notification to {}: {}", userEmail,
                    e.getMessage(), e);
        }
    }

    /**
     * Builds the email body for expense status notifications.
     */
    private String buildExpenseStatusEmailBody(String username, String fullName, Long expenseId,
            String expenseDescription, String amount, String currency, String newStatus,
            String notes, String processedBy) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);

        StringBuilder details = new StringBuilder();
        if (fullName != null) {
            details.append(String.format("User:               %s%n", fullName));
        }
        details.append(String.format("Description:        %s%n", expenseDescription));
        details.append(String.format("Amount:             %s %s%n", amount, currency));
        details.append(String.format("New Status:         %s%n", newStatus));
        if (processedBy != null) {
            details.append(String.format("Processed By:       %s%n", processedBy));
        }
        details.append(String.format("Date:               %s", timestamp));

        StringBuilder notesSection = new StringBuilder();
        if (notes != null && !notes.trim().isEmpty()) {
            notesSection.append("\n\nNotes:\n");
            notesSection.append("----------------------------------------\n");
            notesSection.append(notes);
            notesSection.append("\n----------------------------------------");
        }

        String statusMessage;
        switch (newStatus.toUpperCase()) {
            case "APPROVED":
                statusMessage =
                        "Your expense has been approved and is ready for reimbursement processing.";
                break;
            case "REJECTED":
                statusMessage =
                        "Your expense has been rejected. Please review the notes below for details. Once the issue has been resolved, resubmit the expense for approval.";
                break;
            case "REIMBURSED":
                statusMessage =
                        "Your expense has been reimbursed. The payment should be reflected in your account soon.";
                break;
            default:
                statusMessage = "Your expense status has been updated.";
        }

        return String.format("""
                EXPENSE STATUS UPDATE

                %s

                Expense Details:
                ----------------------------------------
                %s
                ----------------------------------------%s

                This is an automated notification from %s.
                Do not reply to this email. This email is sent from an unattended mailbox.
                """, statusMessage, details.toString(), notesSection.toString(), appName);
    }
}

