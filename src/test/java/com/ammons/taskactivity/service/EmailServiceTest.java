package com.ammons.taskactivity.service;

import org.junit.jupiter.api.BeforeEach;
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
 * @author Dean Ammons
 */
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
        emailService.sendAccountLockoutNotification(TEST_USERNAME, TEST_FAILED_ATTEMPTS, TEST_IP);

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
        emailService.sendAccountLockoutNotification(TEST_USERNAME, TEST_FAILED_ATTEMPTS, TEST_IP);

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
        emailService.sendAccountLockoutNotification(TEST_USERNAME, TEST_FAILED_ATTEMPTS, null);

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
        assertDoesNotThrow(() -> emailService.sendAccountLockoutNotification(TEST_USERNAME,
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
        emailService.sendAccountLockoutNotification(TEST_USERNAME, TEST_FAILED_ATTEMPTS, TEST_IP);

        // Assert
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        String body = sentMessage.getText();

        // Verify instructions are included
        assertTrue(body.contains("Action Required"));
        assertTrue(body.contains("User Management"));
        assertTrue(body.contains("Account is locked"));
    }
}
