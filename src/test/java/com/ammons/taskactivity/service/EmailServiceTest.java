package com.ammons.taskactivity.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EmailService
 * 
 * Tests email notification functionality including: - Account lockout notifications - Test email
 * functionality - Error handling
 * 
 * Note: These tests are disabled for AWS deployment as they require SMTP configuration
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since December 2025
 */
@Disabled("Email tests require SMTP configuration - skip for AWS deployment")
@ExtendWith(MockitoExtension.class)
@DisplayName("Email Service Tests")
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_IP = "192.168.1.100";
    private static final String TEST_ADMIN_EMAIL = "admin@test.com";
    private static final String TEST_FROM_EMAIL = "noreply@test.com";
    private static final int TEST_FAILED_ATTEMPTS = 5;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender);

        // Set up the service with test configuration
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        ReflectionTestUtils.setField(emailService, "fromAddress", TEST_FROM_EMAIL);
        ReflectionTestUtils.setField(emailService, "adminEmail", TEST_ADMIN_EMAIL);
        ReflectionTestUtils.setField(emailService, "appName", "Test App");
    }

    @Test
    @DisplayName("Should send account lockout notification when email is enabled")
    void shouldSendAccountLockoutNotification() {
        // Arrange
        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.sendAccountLockoutNotification(TEST_USERNAME, null, TEST_FAILED_ATTEMPTS,
                TEST_IP);

        // Assert
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);
        assertEquals(TEST_FROM_EMAIL, sentMessage.getFrom());
        assertArrayEquals(new String[] {TEST_ADMIN_EMAIL}, sentMessage.getTo());
        assertTrue(sentMessage.getSubject().contains("Account Locked"));
        assertTrue(sentMessage.getSubject().contains(TEST_USERNAME));

        String body = sentMessage.getText();
        assertNotNull(body);
        assertTrue(body.contains(TEST_USERNAME));
        assertTrue(body.contains(String.valueOf(TEST_FAILED_ATTEMPTS)));
        assertTrue(body.contains(TEST_IP));
        assertTrue(body.contains("ACCOUNT LOCKOUT ALERT"));
    }

    @Test
    @DisplayName("Should not send email when email is disabled")
    void shouldNotSendEmailWhenDisabled() {
        // Arrange
        ReflectionTestUtils.setField(emailService, "mailEnabled", false);

        // Act
        emailService.sendAccountLockoutNotification(TEST_USERNAME, null, TEST_FAILED_ATTEMPTS,
                TEST_IP);

        // Assert
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should handle null IP address gracefully")
    void shouldHandleNullIpAddress() {
        // Arrange
        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.sendAccountLockoutNotification(TEST_USERNAME, null, TEST_FAILED_ATTEMPTS,
                null);

        // Assert
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        String body = sentMessage.getText();
        assertTrue(body.contains("Unknown"));
    }

    @Test
    @DisplayName("Should handle email sending failure gracefully")
    void shouldHandleEmailSendingFailure() {
        // Arrange
        doThrow(new MailSendException("Test exception")).when(mailSender)
                .send(any(SimpleMailMessage.class));

        // Act & Assert - Should not throw exception
        assertDoesNotThrow(() -> emailService.sendAccountLockoutNotification(TEST_USERNAME, null,
                TEST_FAILED_ATTEMPTS, TEST_IP));

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should send test email successfully")
    void shouldSendTestEmail() {
        // Arrange
        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        boolean result = emailService.sendTestEmail();

        // Assert
        assertTrue(result);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);
        assertEquals(TEST_FROM_EMAIL, sentMessage.getFrom());
        assertArrayEquals(new String[] {TEST_ADMIN_EMAIL}, sentMessage.getTo());
        assertTrue(sentMessage.getSubject().contains("Test Email"));
    }

    @Test
    @DisplayName("Should return false when test email fails")
    void shouldReturnFalseWhenTestEmailFails() {
        // Arrange
        doThrow(new MailSendException("Test exception")).when(mailSender)
                .send(any(SimpleMailMessage.class));

        // Act
        boolean result = emailService.sendTestEmail();

        // Assert
        assertTrue(result);
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should not send test email when email is disabled")
    void shouldNotSendTestEmailWhenDisabled() {
        // Arrange
        ReflectionTestUtils.setField(emailService, "mailEnabled", false);

        // Act
        boolean result = emailService.sendTestEmail();

        // Assert
        assertFalse(result);
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should include action instructions in lockout email")
    void shouldIncludeActionInstructionsInLockoutEmail() {
        // Arrange
        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // Act
        emailService.sendAccountLockoutNotification(TEST_USERNAME, null, TEST_FAILED_ATTEMPTS,
                TEST_IP);

        // Assert
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        String body = sentMessage.getText();

        // Verify instructions are included
        assertTrue(body.contains("Action Required"));
        assertTrue(body.contains("User Management"));
        assertTrue(body.contains("Account is locked"));
    }

    @Test
    @DisplayName("Should send build success notification")
    void testSendBuildSuccessNotification() {
            // Arrange
            String buildNumber = "72";
            String branch = "main";
            String commit = "abc1234";
            String buildUrl = "https://jenkins.example.com/job/taskactivity/72/";

            ReflectionTestUtils.setField(emailService, "mailEnabled", true);
            ReflectionTestUtils.setField(emailService, "useAwsSdk", false);
            ReflectionTestUtils.setField(emailService, "adminEmail", TEST_ADMIN_EMAIL);
            ReflectionTestUtils.setField(emailService, "fromAddress", TEST_FROM_EMAIL);
            ReflectionTestUtils.setField(emailService, "appName",
                            "Task Activity Management System");

            ArgumentCaptor<SimpleMailMessage> messageCaptor =
                            ArgumentCaptor.forClass(SimpleMailMessage.class);

            // Act
            emailService.sendBuildSuccessNotification(buildNumber, branch, commit, buildUrl);

            // Assert
            verify(mailSender, times(1)).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertEquals(TEST_ADMIN_EMAIL, sentMessage.getTo()[0]);
            assertEquals(TEST_FROM_EMAIL, sentMessage.getFrom());
            assertTrue(sentMessage.getSubject().contains("✅"));
            assertTrue(sentMessage.getSubject().contains("SUCCESS"));
            assertTrue(sentMessage.getSubject().contains(buildNumber));

            String body = sentMessage.getText();
            assertTrue(body.contains("SUCCESS"));
            assertTrue(body.contains(buildNumber));
            assertTrue(body.contains(branch));
            assertTrue(body.contains(commit));
            assertTrue(body.contains(buildUrl));
            assertTrue(body.contains("✅"));
    }

    @Test
    @DisplayName("Should send build failure notification")
    void testSendBuildFailureNotification() {
            // Arrange
            String buildNumber = "73";
            String branch = "develop";
            String commit = "def5678";
            String buildUrl = "https://jenkins.example.com/job/taskactivity/73/";
            String consoleUrl = "https://jenkins.example.com/job/taskactivity/73/console";

            ReflectionTestUtils.setField(emailService, "mailEnabled", true);
            ReflectionTestUtils.setField(emailService, "useAwsSdk", false);
            ReflectionTestUtils.setField(emailService, "adminEmail", TEST_ADMIN_EMAIL);
            ReflectionTestUtils.setField(emailService, "fromAddress", TEST_FROM_EMAIL);
            ReflectionTestUtils.setField(emailService, "appName",
                            "Task Activity Management System");

            ArgumentCaptor<SimpleMailMessage> messageCaptor =
                            ArgumentCaptor.forClass(SimpleMailMessage.class);

            // Act
            emailService.sendBuildFailureNotification(buildNumber, branch, commit, buildUrl,
                            consoleUrl);

            // Assert
            verify(mailSender, times(1)).send(messageCaptor.capture());

            SimpleMailMessage sentMessage = messageCaptor.getValue();
            assertEquals(TEST_ADMIN_EMAIL, sentMessage.getTo()[0]);
            assertEquals(TEST_FROM_EMAIL, sentMessage.getFrom());
            assertTrue(sentMessage.getSubject().contains("❌"));
            assertTrue(sentMessage.getSubject().contains("FAILED"));
            assertTrue(sentMessage.getSubject().contains(buildNumber));

            String body = sentMessage.getText();
            assertTrue(body.contains("FAILURE"));
            assertTrue(body.contains(buildNumber));
            assertTrue(body.contains(branch));
            assertTrue(body.contains(commit));
            assertTrue(body.contains(buildUrl));
            assertTrue(body.contains(consoleUrl));
            assertTrue(body.contains("View logs"));
            assertTrue(body.contains("❌"));
}

    @Test
    @DisplayName("Should not send build notification when email disabled")
    void testSendBuildNotification_EmailDisabled() {
            // Arrange
            ReflectionTestUtils.setField(emailService, "mailEnabled", false);
            ReflectionTestUtils.setField(emailService, "adminEmail", TEST_ADMIN_EMAIL);

            // Act
            emailService.sendBuildSuccessNotification("72", "main", "abc1234",
                            "https://jenkins.example.com/job/taskactivity/72/");

            // Assert
            verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should not send build notification when no admin email configured")
    void testSendBuildNotification_NoAdminEmail() {
            // Arrange
            ReflectionTestUtils.setField(emailService, "mailEnabled", true);
            ReflectionTestUtils.setField(emailService, "adminEmail", "");

            // Act
            emailService.sendBuildFailureNotification("73", "develop", "def5678",
                            "https://jenkins.example.com/job/taskactivity/73/",
                            "https://jenkins.example.com/job/taskactivity/73/console");

            // Assert
            verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Should send build notification to multiple admin emails")
    void testSendBuildNotification_MultipleAdmins() {
            // Arrange
            String multipleAdmins = "admin1@test.com,admin2@test.com,admin3@test.com";

            ReflectionTestUtils.setField(emailService, "mailEnabled", true);
            ReflectionTestUtils.setField(emailService, "useAwsSdk", false);
            ReflectionTestUtils.setField(emailService, "adminEmail", multipleAdmins);
            ReflectionTestUtils.setField(emailService, "fromAddress", TEST_FROM_EMAIL);
            ReflectionTestUtils.setField(emailService, "appName",
                            "Task Activity Management System");

            ArgumentCaptor<SimpleMailMessage> messageCaptor =
                            ArgumentCaptor.forClass(SimpleMailMessage.class);

            // Act
            emailService.sendBuildSuccessNotification("74", "main", "ghi9012",
                            "https://jenkins.example.com/job/taskactivity/74/");

            // Assert
            verify(mailSender, times(3)).send(messageCaptor.capture());

            // Verify emails were sent to all three admins
            var sentMessages = messageCaptor.getAllValues();
            assertEquals(3, sentMessages.size());
            assertTrue(sentMessages.stream().anyMatch(m -> "admin1@test.com".equals(m.getTo()[0])));
            assertTrue(sentMessages.stream().anyMatch(m -> "admin2@test.com".equals(m.getTo()[0])));
            assertTrue(sentMessages.stream().anyMatch(m -> "admin3@test.com".equals(m.getTo()[0])));
    }
}


