package com.ammons.taskactivity.service;

import com.ammons.taskactivity.entity.User;
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
import java.util.List;

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
 * @since December 2025
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

    @Value("${app.mail.jenkins-build-notification-email:}")
    private String jenkinsBuildNotificationEmail;

    @Value("${app.mail.jenkins-deploy-notification-email:}")
    private String jenkinsDeployNotificationEmail;

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

        String subject = "Account Locked: " + username;
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
     * Send email via AWS SES SDK to multiple recipients in the "To" field.
     * 
     * @param subject email subject
     * @param body email body text
     * @param to recipient email addresses (varargs)
     */
    private void sendEmailViaAwsSdk(String subject, String body, String... to) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .destination(
                            Destination.builder().toAddresses(java.util.Arrays.asList(to)).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).charset("UTF-8").build())
                            .body(Body.builder()
                                    .text(Content.builder().data(body).charset("UTF-8").build())
                                    .build())
                            .build())
                    .source(fromAddress).build();

            SendEmailResponse response = sesClient.sendEmail(request);
            logger.info("Email sent successfully via AWS SES to {} recipient(s). MessageId: {}",
                    to.length, response.messageId());

        } catch (Exception e) {
            logger.error("Failed to send email via AWS SES to: {}", String.join(", ", to), e);
            // Don't throw exception - email failure should not prevent login processing
        }
    }

    /**
     * Send email using traditional SMTP. Requires SMTP username/password configuration. Supports
     * multiple recipients in the "To" field.
     * 
     * @param to recipient email address(es)
     * @param subject email subject
     * @param body email body text
     */
    private void sendEmailViaSmtp(String subject, String body, String... to) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);
            logger.info("Email sent successfully via SMTP to {} recipient(s): {}", to.length,
                    String.join(", ", to));

        } catch (MailException e) {
            logger.error("Failed to send email via SMTP to: {}", String.join(", ", to), e);
            // Don't throw exception - email failure should not prevent login processing
        }
    }

    /**
     * Send emails to recipients with support for grouping.
     * 
     * Format: - Comma (,) separates distinct email groups - each group receives a separate email -
     * Semicolon (;) separates recipients within a group - they all appear in the "To" field of one
     * email
     * 
     * Examples: - "email1@example.com" -> One email to email1 -
     * "email1@example.com,email2@example.com" -> Two separate emails -
     * "email1@example.com;email2@example.com" -> One email with both in "To" field -
     * "email1@example.com,email2@example.com;email3@example.com" -> Two emails: Email 1 to:
     * email1@example.com Email 2 to: email2@example.com, email3@example.com
     * 
     * @param recipientsConfig recipient configuration string
     * @param subject email subject
     * @param body email body text
     * @param emailType description of email type for logging (e.g., "build success", "deploy
     *        failure")
     */
    private void sendEmailsWithGrouping(String recipientsConfig, String subject, String body,
            String emailType) {
        if (recipientsConfig == null || recipientsConfig.trim().isEmpty()) {
            logger.warn("No recipients configured for {} notification", emailType);
            return;
        }

        // Split by comma to get separate email groups
        String[] emailGroups = recipientsConfig.split(",");

        for (String group : emailGroups) {
            String trimmedGroup = group.trim();
            if (trimmedGroup.isEmpty()) {
                continue;
            }

            // Split by semicolon to get individual recipients within this group
            String[] recipients = trimmedGroup.split(";");

            // Trim each recipient email and filter out empty strings
            String[] trimmedRecipients = java.util.Arrays.stream(recipients).map(String::trim)
                    .filter(email -> !email.isEmpty()).toArray(String[]::new);

            if (trimmedRecipients.length == 0) {
                continue;
            }

            try {
                if (useAwsSdk && sesClient != null) {
                    sendEmailViaAwsSdk(subject, body, trimmedRecipients);
                } else {
                    sendEmailViaSmtp(subject, body, trimmedRecipients);
                }
                logger.info("{} notification sent to {} recipient(s): {}", emailType,
                        trimmedRecipients.length, String.join(", ", trimmedRecipients));
            } catch (Exception e) {
                logger.error("Failed to send {} notification to: {}", emailType,
                        String.join(", ", trimmedRecipients), e);
            }
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

        String subject = "GUEST User Login";
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
     * Sends a profile notification email directly to the specified user. The email includes the
     * user's login username, full name, company, assigned task clients/projects, assigned expense
     * clients/projects, and — when the user's forcePasswordUpdate flag is set — the temporary
     * password they must change on first login.
     *
     * @param user the target user entity
     * @param taskClients list of explicitly assigned TASK/CLIENT values for the user
     * @param taskProjects list of explicitly assigned TASK/PROJECT values for the user
     * @param expenseClients list of explicitly assigned EXPENSE/CLIENT values for the user
     * @param expenseProjects list of explicitly assigned EXPENSE/PROJECT values for the user
     */
    public void sendUserProfileNotification(User user, List<String> taskClients,
            List<String> taskProjects, List<String> expenseClients, List<String> expenseProjects) {

        String userEmail = user.getEmail();
        String username = user.getUsername();

        if (!mailEnabled) {
            logger.info(
                    "Email notifications are disabled. Would have sent profile notification for user: {}",
                    username);
            return;
        }

        if (userEmail == null || userEmail.isBlank()) {
            logger.warn("User {} has no email address - skipping profile notification", username);
            return;
        }

        String subject = "Your " + appName + " Profile Details";
        String body = buildProfileNotificationBody(user, taskClients, taskProjects, expenseClients,
                expenseProjects);

        try {
            if (useAwsSdk && sesClient != null) {
                sendEmailViaAwsSdk(userEmail, subject, body);
            } else {
                sendEmailViaSmtp(subject, body, userEmail);
            }
            logger.info("Profile notification sent to {} for user: {}", userEmail, username);
        } catch (Exception e) {
            logger.error("Failed to send profile notification to {} for user {}: {}", userEmail,
                    username, e.getMessage(), e);
        }
    }

    private static final String SEPARATOR_LINE = "\n----------------------------------------\n";

    /**
     * Builds the plain-text email body for a user profile notification.
     *
     * When forcePasswordUpdate is true, the temporary password is shown inline in the Account
     * Information section. All assignment lists are grouped under a single "Client and Project
     * Assignments" section; subsections are omitted when empty.
     *
     * Modified by: Dean Ammons - February 2026 Change: Redesigned layout to match updated email
     * format — sentence-case headers, inline password, consolidated assignments section, updated
     * footer text.
     */
    private String buildProfileNotificationBody(User user, List<String> taskClients,
            List<String> taskProjects, List<String> expenseClients, List<String> expenseProjects) {

        StringBuilder body = new StringBuilder();
        body.append("Hello ")
                .append(user.getFirstname() != null ? user.getFirstname() : user.getUsername())
                .append(",\n\n");
        body.append("Below is a summary of your ").append(appName).append(" profile.\n\n");

        body.append(SEPARATOR_LINE);
        body.append("Account Information\n");
        body.append("----------------------------------------\n");
        body.append(String.format("Username:     %s%n", user.getUsername()));
        if (user.getFirstname() != null || user.getLastname() != null) {
            String firstName = user.getFirstname() != null ? user.getFirstname() : "";
            String lastName = user.getLastname() != null ? user.getLastname() : "";
            body.append(String.format("Name:         %s %s%n", firstName, lastName));
        }
        if (user.getRole() != null) {
            String roleDesc = user.getRole().getDescription();
            String roleDisplay;
            if (roleDesc != null && !roleDesc.isBlank()) {
                int dashIndex = roleDesc.indexOf(" - ");
                roleDisplay = (dashIndex >= 0) ? roleDesc.substring(0, dashIndex) : roleDesc;
            } else {
                roleDisplay = user.getRole().getName();
            }
            body.append(String.format("Role:         %s%n", roleDisplay));
        }
        if (user.isForcePasswordUpdate()) {
            body.append(String.format("Password:     %s (Change required on next login)%n",
                    "P@ssword!123"));
        }

        boolean hasAssignments = !taskClients.isEmpty() || !taskProjects.isEmpty()
                || !expenseClients.isEmpty() || !expenseProjects.isEmpty();
        if (hasAssignments) {
            body.append(SEPARATOR_LINE);
            body.append("Client and Project Assignments\n");
            body.append("----------------------------------------\n");
            if (!taskClients.isEmpty()) {
                body.append("\nClients\n");
                taskClients.forEach(c -> body.append("  - ").append(c).append("\n"));
            }
            if (!taskProjects.isEmpty()) {
                body.append("\nProjects\n");
                taskProjects.forEach(p -> body.append("  - ").append(p).append("\n"));
            }
            if (!expenseClients.isEmpty()) {
                body.append("\nExpense Clients\n");
                expenseClients.forEach(c -> body.append("  - ").append(c).append("\n"));
            }
            if (!expenseProjects.isEmpty()) {
                body.append("\nExpense Projects\n");
                expenseProjects.forEach(p -> body.append("  - ").append(p).append("\n"));
            }
        }

        body.append(SEPARATOR_LINE).append("\n");
        body.append("Contact the system administrator if any of these details are incorrect.\n");
        body.append("Do not reply to this email. This email is sent from an unattended mailbox.\n");

        return body.toString();
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
                                Please investigate this potential security incident.

                                The user can unlock their account by using the "Reset Password" feature on the login page, \
                which will allow them to set a new password and automatically unlock their account.

                                Alternatively, you can manually unlock the account:
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
        String subject = "Test Email";

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

        String subject = "New Expense Submitted - " + expenseId;
        String body = buildExpenseSubmittedEmailBody(username, fullName, expenseId,
                expenseDescription, amount, currency, expenseDate);

        // Use sendEmailsWithGrouping to support semicolon grouping
        sendEmailsWithGrouping(expenseApprovers, subject, body, "expense submission");
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

        String subject = String.format("Expense %s - %s", newStatus, expenseId);
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

    /**
     * Send password expiration warning notification to user.
     * 
     * @param userEmail the user's email address
     * @param username the username
     * @param fullName the full name of the user (optional)
     * @param daysUntilExpiration number of days until password expires
     */
    public void sendPasswordExpirationWarning(String userEmail, String username, String fullName,
            long daysUntilExpiration) {
        if (!mailEnabled) {
            logger.warn(
                    "Email notifications disabled - skipping password expiration warning for user: {}",
                    username);
            return;
        }

        if (userEmail == null || userEmail.trim().isEmpty()) {
            logger.warn("Cannot send password expiration warning - user {} has no email address",
                    username);
            return;
        }

        String subject = "Password Expiration Warning";
        String body =
                buildPasswordExpirationWarningEmailBody(username, fullName, daysUntilExpiration);

        try {
            if (useAwsSdk && sesClient != null) {
                sendEmailViaAwsSdk(userEmail, subject, body);
            } else {
                sendEmailViaSmtp(userEmail, subject, body);
            }
            logger.info("Password expiration warning sent to {} for user: {}", userEmail, username);
        } catch (Exception e) {
            logger.error("Failed to send password expiration warning to {}: {}", userEmail,
                    e.getMessage(), e);
        }
    }

    /**
     * Builds the email body for password expiration warning notifications.
     * 
     * @param username the username
     * @param fullName the full name of the user (optional)
     * @param daysUntilExpiration number of days until password expires
     * @return formatted email body text
     */
    private String buildPasswordExpirationWarningEmailBody(String username, String fullName,
            long daysUntilExpiration) {
        String greeting = fullName != null && !fullName.trim().isEmpty() ? fullName : username;

        String urgencyMessage;
        if (daysUntilExpiration == 0) {
            urgencyMessage = "🔴 CRITICAL: Your password expires TODAY!";
        } else if (daysUntilExpiration == 1) {
            urgencyMessage = "⚠️ URGENT: Your password expires in 1 day!";
        } else if (daysUntilExpiration <= 3) {
            urgencyMessage = String.format("⚠️ IMPORTANT: Your password expires in %d days!",
                    daysUntilExpiration);
        } else {
            urgencyMessage =
                    String.format("Your password will expire in %d days.", daysUntilExpiration);
        }

        return String.format(
                """
                        Hello %s,

                        %s

                        For security purposes, all passwords must be changed every 90 days.
                        Please change your password before it expires to avoid being locked out of your account.

                        HOW TO CHANGE YOUR PASSWORD:
                        ----------------------------------------
                        1. Log in to %s
                        2. Click "👤 Update Profile" in the sidebar menu
                        3. Click the "🔒 Update Password" button at the bottom of the page
                        4. Enter your current password and new password
                        5. Your new password must:
                           - Be at least 8 characters long
                           - Not be the same as your current password

                        WHAT HAPPENS IF MY PASSWORD EXPIRES:
                        ----------------------------------------
                        If your password expires, you will be prompted to change it immediately upon login.
                        You will not be able to access the system until you change your password.

                        If you have any questions, please contact your system administrator.

                        ---
                        This is an automated notification from %s.
                        Do not reply to this email. This email is sent from an unattended mailbox.
                        """,
                greeting, urgencyMessage, appName, appName);
    }

    /**
     * Send password expired notification to user.
     * 
     * This notification is sent ONCE when a password actually expires, informing the user that
     * their password has expired and they must change it upon next login.
     * 
     * @param userEmail the user's email address
     * @param username the username
     * @param fullName the full name of the user (optional)
     */
    public void sendPasswordExpiredNotification(String userEmail, String username,
            String fullName) {
        if (!mailEnabled) {
            logger.warn(
                    "Email notifications disabled - skipping password expired notification for user: {}",
                    username);
            return;
        }

        if (userEmail == null || userEmail.trim().isEmpty()) {
            logger.warn("Cannot send password expired notification - user {} has no email address",
                    username);
            return;
        }

        String subject = "Password Has Expired - Action Required";
        String body = buildPasswordExpiredEmailBody(username, fullName);

        try {
            if (useAwsSdk && sesClient != null) {
                sendEmailViaAwsSdk(userEmail, subject, body);
            } else {
                sendEmailViaSmtp(userEmail, subject, body);
            }
            logger.info("Password expired notification sent to {} for user: {}", userEmail,
                    username);
        } catch (Exception e) {
            logger.error("Failed to send password expired notification to {}: {}", userEmail,
                    e.getMessage(), e);
        }
    }

    /**
     * Builds the email body for password expired notifications.
     * 
     * @param username the username
     * @param fullName the full name of the user (optional)
     * @return formatted email body text
     */
    private String buildPasswordExpiredEmailBody(String username, String fullName) {
        String greeting = fullName != null && !fullName.trim().isEmpty() ? fullName : username;

        return String.format(
                """
                        Hello %s,

                        🔒 YOUR PASSWORD HAS EXPIRED

                        Your password has expired and must be changed before you can continue using the system.
                        For security purposes, all passwords must be changed every 90 days.

                        WHAT TO DO NEXT:
                        ----------------------------------------
                        1. Go to %s login page
                        2. Enter your username and current (expired) password
                        3. You will be automatically redirected to the password change page
                        4. Enter your current password and a new password
                        5. Your new password must:
                           - Be at least 8 characters long
                           - Not be the same as your current password

                        IMPORTANT:
                        ----------------------------------------
                        - You will NOT be able to access the system until you change your password
                        - Your expired password will still work ONE TIME to log in and change it
                        - After changing your password, your new password will be valid for 90 days

                        NEED HELP?
                        ----------------------------------------
                        If you have any questions or need assistance, please contact your system administrator.

                        ---
                        This is an automated notification from %s.
                        Do not reply to this email. This email is sent from an unattended mailbox.
                        """,
                greeting, appName, appName);
    }

    /**
     * Send password reset email to user.
     * 
     * @param email the user's email address
     * @param username the username
     * @param fullName the full name of the user (optional)
     * @param resetLink the password reset link
     * @param expiryMinutes number of minutes until link expires
     */
    public void sendPasswordResetEmail(String email, String username, String fullName,
            String resetLink, int expiryMinutes) {
        if (!mailEnabled) {
            logger.warn("Email notifications disabled - skipping password reset email for user: {}",
                    username);
            return;
        }

        if (email == null || email.trim().isEmpty()) {
            logger.warn("Cannot send password reset email - user {} has no email address",
                    username);
            return;
        }

        String subject = "Password Reset Request";
        String body = buildPasswordResetEmailBody(username, fullName, resetLink, expiryMinutes);

        try {
            if (useAwsSdk && sesClient != null) {
                sendEmailViaAwsSdk(email, subject, body);
            } else {
                sendEmailViaSmtp(subject, body, email);
            }
            logger.info("Password reset email sent to {} for user: {}", email, username);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {}: {}", email, e.getMessage(), e);
        }
    }

    /**
     * Builds the email body for password reset requests.
     * 
     * @param username the username
     * @param fullName the full name of the user (optional)
     * @param resetLink the password reset link
     * @param expiryMinutes number of minutes until link expires
     * @return formatted email body text
     */
    private String buildPasswordResetEmailBody(String username, String fullName, String resetLink,
            int expiryMinutes) {
        String greeting = (fullName != null && !fullName.trim().isEmpty()) ? fullName : username;

        return String.format("""
                Hello %s,

                You requested a password reset for your %s account.

                Click the link below to reset your password:
                %s

                This link will expire in %d minutes for security reasons.

                If you did not request this password reset, please ignore this email.
                Your password will remain unchanged.

                For security reasons, do not share this link with anyone.

                ---
                This is an automated notification from %s.
                Do not reply to this email. This email is sent from an unattended mailbox.
                """, greeting, appName, resetLink, expiryMinutes, appName);
    }

    /**
     * Send password changed confirmation email to user.
     * 
     * @param email the user's email address
     * @param username the username
     * @param fullName the full name of the user (optional)
     */
    public void sendPasswordChangedConfirmation(String email, String username, String fullName) {
        if (!mailEnabled) {
            logger.warn(
                    "Email notifications disabled - skipping password changed confirmation for user: {}",
                    username);
            return;
        }

        if (email == null || email.trim().isEmpty()) {
            logger.warn("Cannot send password changed confirmation - user {} has no email address",
                    username);
            return;
        }

        String subject = "Password Changed Successfully";
        String body = buildPasswordChangedConfirmationBody(username, fullName);

        try {
            if (useAwsSdk && sesClient != null) {
                sendEmailViaAwsSdk(email, subject, body);
            } else {
                sendEmailViaSmtp(subject, body, email);
            }
            logger.info("Password changed confirmation sent to {} for user: {}", email, username);
        } catch (Exception e) {
            logger.error("Failed to send password changed confirmation to {}: {}", email,
                    e.getMessage(), e);
        }
    }

    /**
     * Builds the email body for password changed confirmations.
     * 
     * @param username the username
     * @param fullName the full name of the user (optional)
     * @return formatted email body text
     */
    private String buildPasswordChangedConfirmationBody(String username, String fullName) {
        String greeting = (fullName != null && !fullName.trim().isEmpty()) ? fullName : username;

        return String.format(
                """
                        Hello %s,

                        Your password for %s has been changed successfully.

                        If you did not make this change, please contact your system administrator immediately.

                        ---
                        This is an automated notification from %s.
                        Do not reply to this email. This email is sent from an unattended mailbox.
                        """,
                greeting, appName, appName);
    }

    /**
     * Send Jenkins build success notification.
     * 
     * @param buildNumber the Jenkins build number
     * @param branch the Git branch that was built
     * @param commit the Git commit hash
     * @param buildUrl the URL to view the build
     * @param environment the build environment (e.g., dev, staging, production)
     */
    public void sendBuildSuccessNotification(String buildNumber, String branch, String commit,
            String buildUrl, String environment) {
        if (!mailEnabled) {
            logger.debug("Email notifications disabled - skipping build success notification");
            return;
        }

        String subject = String.format("✅ Jenkins Build %s - SUCCESS", buildNumber);
        String body = buildJenkinsBuildEmailBody(buildNumber, branch, commit, buildUrl, true, null,
                environment);

        sendEmailsWithGrouping(jenkinsBuildNotificationEmail, subject, body, "build success");
    }

    /**
     * Send Jenkins build failure notification.
     * 
     * @param buildNumber the Jenkins build number
     * @param branch the Git branch that was built
     * @param commit the Git commit hash
     * @param buildUrl the URL to view the build
     * @param consoleUrl the URL to view console logs
     * @param environment the build environment (e.g., dev, staging, production)
     */
    public void sendBuildFailureNotification(String buildNumber, String branch, String commit,
            String buildUrl, String consoleUrl, String environment) {
        if (!mailEnabled) {
            logger.debug("Email notifications disabled - skipping build failure notification");
            return;
        }

        String subject = String.format("❌ Jenkins Build %s - FAILED", buildNumber);
        String body = buildJenkinsBuildEmailBody(buildNumber, branch, commit, buildUrl, false,
                consoleUrl, environment);

        sendEmailsWithGrouping(jenkinsBuildNotificationEmail, subject, body, "build failure");
    }

    /**
     * Send Jenkins deploy success notification.
     * 
     * @param buildNumber the Jenkins build number
     * @param branch the Git branch that was deployed
     * @param commit the Git commit hash
     * @param deployUrl the URL to view the deployment
     * @param environment the deployment environment (e.g., staging, production)
     */
    public void sendDeploySuccessNotification(String buildNumber, String branch, String commit,
            String deployUrl, String environment) {
        if (!mailEnabled) {
            logger.debug("Email notifications disabled - skipping deploy success notification");
            return;
        }

        String subject =
                String.format("✅ Jenkins Deploy %s - SUCCESS (%s)", buildNumber, environment);
        String body = buildJenkinsDeployEmailBody(buildNumber, branch, commit, deployUrl, true,
                null, environment);

        sendEmailsWithGrouping(jenkinsDeployNotificationEmail, subject, body, "deploy success");
    }

    /**
     * Send Jenkins deploy failure notification.
     * 
     * @param buildNumber the Jenkins build number
     * @param branch the Git branch that was deployed
     * @param commit the Git commit hash
     * @param deployUrl the URL to view the deployment
     * @param consoleUrl the URL to view console logs
     * @param environment the deployment environment (e.g., staging, production)
     */
    public void sendDeployFailureNotification(String buildNumber, String branch, String commit,
            String deployUrl, String consoleUrl, String environment) {
        if (!mailEnabled) {
            logger.debug("Email notifications disabled - skipping deploy failure notification");
            return;
        }

        String subject =
                String.format("❌ Jenkins Deploy %s - FAILED (%s)", buildNumber, environment);
        String body = buildJenkinsDeployEmailBody(buildNumber, branch, commit, deployUrl, false,
                consoleUrl, environment);

        sendEmailsWithGrouping(jenkinsBuildNotificationEmail, subject, body, "deploy failure");
    }

    /**
     * Send Jenkins deploy skipped notification.
     * 
     * @param buildNumber the Jenkins build number
     * @param branch the Git branch
     * @param commit the Git commit hash
     * @param buildUrl the URL to view the build
     * @param environment the deployment environment (e.g., staging, production)
     * @param reason the reason deployment was skipped
     */
    public void sendDeploySkippedNotification(String buildNumber, String branch, String commit,
            String buildUrl, String environment, String reason) {
        if (!mailEnabled) {
            logger.debug("Email notifications disabled - skipping deploy skipped notification");
            return;
        }

        String subject =
                String.format("⏭️ Jenkins Deploy %s - SKIPPED (%s)", buildNumber, environment);
        String body = buildJenkinsDeploySkippedEmailBody(buildNumber, branch, commit, buildUrl,
                environment, reason);

        sendEmailsWithGrouping(jenkinsBuildNotificationEmail, subject, body, "deploy skipped");
    }

    /**
     * Builds the email body for Jenkins build notifications.
     * 
     * @param buildNumber the build number
     * @param branch the Git branch
     * @param commit the Git commit hash
     * @param buildUrl the URL to view the build
     * @param success whether the build succeeded
     * @param consoleUrl optional console log URL (for failures)
     * @param environment the build environment
     * @return formatted email body text
     */
    private String buildJenkinsBuildEmailBody(String buildNumber, String branch, String commit,
            String buildUrl, boolean success, String consoleUrl, String environment) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String status = success ? "SUCCESS" : "FAILURE";
        String emoji = success ? "✅" : "❌";

        StringBuilder body = new StringBuilder();
        body.append(String.format("%s JENKINS BUILD %s%n%n", emoji, status));

        body.append("Build Details:\n");
        body.append("----------------------------------------\n");
        body.append(String.format("Action Type:        BUILD%n"));
        body.append(String.format("Build Number:       %s%n", buildNumber));
        body.append(String.format("Environment:        %s%n",
                environment != null ? environment.toUpperCase() : "UNKNOWN"));
        body.append(String.format("Branch:             %s%n", branch));
        body.append(String.format("Commit:             %s%n", commit));
        body.append(String.format("Status:             %s%n", status));
        body.append(String.format("Timestamp:          %s%n", timestamp));
        body.append("----------------------------------------\n\n");

        body.append("Links:\n");
        body.append(String.format("View build: %s%n", buildUrl));
        if (!success && consoleUrl != null && !consoleUrl.trim().isEmpty()) {
            body.append(String.format("View logs:  %s%n", consoleUrl));
        }

        body.append("\n---\n");
        body.append(String.format("This is an automated notification from %s CI/CD Pipeline.%n",
                appName));
        body.append("Do not reply to this email. This email is sent from an unattended mailbox.");

        return body.toString();
    }

    /**
     * Builds the email body for Jenkins deploy notifications.
     * 
     * @param buildNumber the build number
     * @param branch the Git branch
     * @param commit the Git commit hash
     * @param deployUrl the URL to view the deployment
     * @param success whether the deployment succeeded
     * @param consoleUrl optional console log URL (for failures)
     * @param environment the deployment environment
     * @return formatted email body text
     */
    private String buildJenkinsDeployEmailBody(String buildNumber, String branch, String commit,
            String deployUrl, boolean success, String consoleUrl, String environment) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
        String status = success ? "SUCCESS" : "FAILURE";
        String emoji = success ? "✅" : "❌";

        StringBuilder body = new StringBuilder();
        body.append(String.format("%s JENKINS DEPLOYMENT %s%n%n", emoji, status));

        body.append("Deployment Details:\n");
        body.append("----------------------------------------\n");
        body.append(String.format("Action Type:        DEPLOYMENT%n"));
        body.append(String.format("Build Number:       %s%n", buildNumber));
        body.append(String.format("Environment:        %s%n",
                environment != null ? environment.toUpperCase() : "UNKNOWN"));
        body.append(String.format("Status:             %s%n", status));
        body.append(String.format("Timestamp:          %s%n", timestamp));
        body.append("----------------------------------------\n\n");

        body.append("Links:\n");
        body.append(String.format("View deployment: %s%n", deployUrl));
        if (!success && consoleUrl != null && !consoleUrl.trim().isEmpty()) {
            body.append(String.format("View logs:       %s%n", consoleUrl));
        }

        body.append("\n---\n");
        body.append(String.format("This is an automated notification from %s CI/CD Pipeline.%n",
                appName));
        body.append("Do not reply to this email. This email is sent from an unattended mailbox.");

        return body.toString();
    }

    /**
     * Builds the email body for Jenkins deploy skipped notifications.
     * 
     * @param buildNumber the build number
     * @param branch the Git branch
     * @param commit the Git commit hash
     * @param buildUrl the URL to view the build
     * @param environment the deployment environment
     * @param reason the reason deployment was skipped
     * @return formatted email body text
     */
    private String buildJenkinsDeploySkippedEmailBody(String buildNumber, String branch,
            String commit, String buildUrl, String environment, String reason) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);

        StringBuilder body = new StringBuilder();
        body.append(String.format("⏭️ JENKINS DEPLOYMENT SKIPPED%n%n"));

        body.append("Deployment Details:\n");
        body.append("----------------------------------------\n");
        body.append(String.format("Action Type:        DEPLOYMENT (SKIPPED)%n"));
        body.append(String.format("Build Number:       %s%n", buildNumber));
        body.append(String.format("Environment:        %s%n",
                environment != null ? environment.toUpperCase() : "UNKNOWN"));
        body.append(String.format("Branch:             %s%n", branch));
        body.append(String.format("Commit:             %s%n", commit));
        body.append(String.format("Status:             SKIPPED%n"));
        body.append(String.format("Reason:             %s%n", reason));
        body.append(String.format("Timestamp:          %s%n", timestamp));
        body.append("----------------------------------------\n\n");

        body.append("Links:\n");
        body.append(String.format("View build: %s%n", buildUrl));

        body.append("\n---\n");
        body.append(String.format("This is an automated notification from %s CI/CD Pipeline.%n",
                appName));
        body.append("Do not reply to this email. This email is sent from an unattended mailbox.\n");
        body.append(
                "\nNote: This notification is controlled by the JENKINS_DEPLOY_SKIPPED_CHECK environment variable.");

        return body.toString();
    }

    /**
     * Sends a user-initiated contact request email to the administrator(s).
     *
     * <p>
     * The email is sent to all configured admin email addresses and includes the user's subject,
     * message body, username, and a timestamp so the administrator has full context without needing
     * to reply for basic information.
     *
     * @param senderUsername the authenticated username submitting the request
     * @param senderEmail the email address of the sender (may be null/empty)
     * @param subject free-form subject entered by the user
     * @param messageBody free-form message body entered by the user
     */
    public void sendAdminContactRequest(String senderUsername, String senderEmail, String subject,
            String messageBody) {
        if (!mailEnabled) {
            logger.info(
                    "Email notifications are disabled. Would have forwarded contact request from user: {}",
                    senderUsername);
            return;
        }

        if (adminEmail == null || adminEmail.trim().isEmpty()) {
            logger.warn("No admin email configured — skipping contact request from user: {}",
                    senderUsername);
            return;
        }

        String emailSubject = "User Request: " + subject;
        String emailBody =
                buildAdminContactEmailBody(senderUsername, senderEmail, subject, messageBody);

        String[] adminEmails = adminEmail.split(",");
        for (String email : adminEmails) {
            String trimmedEmail = email.trim();
            if (trimmedEmail.isEmpty()) {
                continue;
            }
            try {
                if (useAwsSdk && sesClient != null) {
                    sendEmailViaAwsSdk(trimmedEmail, emailSubject, emailBody);
                } else {
                    sendEmailViaSmtp(emailSubject, emailBody, trimmedEmail);
                }
                logger.info("Admin contact request email sent to {} from user: {}", trimmedEmail,
                        senderUsername);
            } catch (Exception e) {
                logger.error("Failed to send admin contact request to {}: {}", trimmedEmail,
                        e.getMessage(), e);
            }
        }
    }

    /**
     * Builds the plain-text email body for an admin contact request.
     *
     * @param senderUsername the authenticated username
     * @param senderEmail the sender's email address (may be null/empty)
     * @param subject the user-supplied subject line
     * @param messageBody the user-supplied message body
     * @return formatted email body text
     */
    private String buildAdminContactEmailBody(String senderUsername, String senderEmail,
            String subject, String messageBody) {
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);

        StringBuilder body = new StringBuilder();
        body.append(String.format("📬 USER REQUEST — %s%n%n", appName));

        body.append("Request Details:\n");
        body.append("----------------------------------------\n");
        body.append(String.format("From:               %s%n", senderUsername));
        body.append(String.format("Reply-To Email:     %s%n",
                (senderEmail != null && !senderEmail.isBlank()) ? senderEmail : "(none on file)"));
        body.append(String.format("Subject:            %s%n", subject));
        body.append(String.format("Submitted:          %s%n", timestamp));
        body.append("----------------------------------------\n\n");

        body.append(messageBody);
        body.append("\n\n---\n");
        body.append(String.format(
                "This message was submitted via the Contact System Administrator form in %s.%n",
                appName));

        return body.toString();
    }
}

