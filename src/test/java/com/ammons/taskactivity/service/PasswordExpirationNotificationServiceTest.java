package com.ammons.taskactivity.service;

import com.ammons.taskactivity.entity.Roles;
import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Unit tests for PasswordExpirationNotificationService.
 *
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@ExtendWith(MockitoExtension.class)
class PasswordExpirationNotificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private PasswordExpirationNotificationService service;

    private Roles userRole;
    private Roles guestRole;

    @BeforeEach
    void setUp() {
        userRole = new Roles();
        userRole.setId(2L);
        userRole.setName("USER");

        guestRole = new Roles();
        guestRole.setId(3L);
        guestRole.setName("GUEST");
    }

    @Test
    void shouldSendNotificationForExpiringPassword() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate expiresIn5Days = today.plusDays(5);

        User user = createUser("testuser", "test@example.com", expiresIn5Days, userRole);
        user.setFirstname("Test");
        user.setLastname("User");

        when(userRepository.findAll()).thenReturn(List.of(user));

        // Act
        service.checkExpiringPasswordsAndNotify();

        // Assert
        verify(emailService, times(1)).sendPasswordExpirationWarning(eq("test@example.com"),
                eq("testuser"), eq("Test User"), eq(5L));
    }

    @Test
    void shouldSendNotificationForPasswordExpiringToday() {
        // Arrange
        LocalDate today = LocalDate.now();

        User user = createUser("testuser", "test@example.com", today, userRole);
        user.setFirstname("Test");
        user.setLastname("User");

        when(userRepository.findAll()).thenReturn(List.of(user));

        // Act
        service.checkExpiringPasswordsAndNotify();

        // Assert
        verify(emailService, times(1)).sendPasswordExpirationWarning(eq("test@example.com"),
                eq("testuser"), eq("Test User"), eq(0L));
    }

    @Test
    void shouldNotSendNotificationForPasswordExpiringBeyond7Days() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate expiresIn10Days = today.plusDays(10);

        User user = createUser("testuser", "test@example.com", expiresIn10Days, userRole);

        when(userRepository.findAll()).thenReturn(List.of(user));

        // Act
        service.checkExpiringPasswordsAndNotify();

        // Assert
        verify(emailService, never()).sendPasswordExpirationWarning(anyString(), anyString(),
                anyString(), anyLong());
    }

    @Test
    void shouldSkipGuestUsers() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate expiresIn5Days = today.plusDays(5);

        User guestUser = createUser("guest", "guest@example.com", expiresIn5Days, guestRole);

        when(userRepository.findAll()).thenReturn(List.of(guestUser));

        // Act
        service.checkExpiringPasswordsAndNotify();

        // Assert
        verify(emailService, never()).sendPasswordExpirationWarning(anyString(), anyString(),
                anyString(), anyLong());
    }

    @Test
    void shouldSkipUsersWithoutEmail() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate expiresIn5Days = today.plusDays(5);

        User userNoEmail = createUser("testuser", null, expiresIn5Days, userRole);

        when(userRepository.findAll()).thenReturn(List.of(userNoEmail));

        // Act
        service.checkExpiringPasswordsAndNotify();

        // Assert
        verify(emailService, never()).sendPasswordExpirationWarning(anyString(), anyString(),
                anyString(), anyLong());
    }

    @Test
    void shouldSkipDisabledUsers() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate expiresIn5Days = today.plusDays(5);

        User disabledUser = createUser("testuser", "test@example.com", expiresIn5Days, userRole);
        disabledUser.setEnabled(false);

        when(userRepository.findAll()).thenReturn(List.of(disabledUser));

        // Act
        service.checkExpiringPasswordsAndNotify();

        // Assert
        verify(emailService, never()).sendPasswordExpirationWarning(anyString(), anyString(),
                anyString(), anyLong());
    }

    @Test
    void shouldSkipLockedUsers() {
        // Arrange
        LocalDate today = LocalDate.now();
        LocalDate expiresIn5Days = today.plusDays(5);

        User lockedUser = createUser("testuser", "test@example.com", expiresIn5Days, userRole);
        lockedUser.setAccountLocked(true);

        when(userRepository.findAll()).thenReturn(List.of(lockedUser));

        // Act
        service.checkExpiringPasswordsAndNotify();

        // Assert
        verify(emailService, never()).sendPasswordExpirationWarning(anyString(), anyString(),
                anyString(), anyLong());
    }

    @Test
    void shouldSkipUsersWithNoExpirationDate() {
        // Arrange
        User userNoExpiration = createUser("testuser", "test@example.com", null, userRole);

        when(userRepository.findAll()).thenReturn(List.of(userNoExpiration));

        // Act
        service.checkExpiringPasswordsAndNotify();

        // Assert
        verify(emailService, never()).sendPasswordExpirationWarning(anyString(), anyString(),
                anyString(), anyLong());
    }

    @Test
    void shouldHandleMultipleUsers() {
        // Arrange
        LocalDate today = LocalDate.now();

        User user1 = createUser("user1", "user1@example.com", today.plusDays(3), userRole);
        User user2 = createUser("user2", "user2@example.com", today.plusDays(7), userRole);
        User user3 = createUser("user3", null, today.plusDays(5), userRole); // No email
        User user4 = createUser("user4", "user4@example.com", today.plusDays(10), userRole); // Beyond
                                                                                             // window

        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2, user3, user4));

        // Act
        service.checkExpiringPasswordsAndNotify();

        // Assert
        verify(emailService, times(1)).sendPasswordExpirationWarning(eq("user1@example.com"),
                eq("user1"), anyString(), eq(3L));
        verify(emailService, times(1)).sendPasswordExpirationWarning(eq("user2@example.com"),
                eq("user2"), anyString(), eq(7L));
        verify(emailService, times(2)).sendPasswordExpirationWarning(anyString(), anyString(),
                anyString(), anyLong());
    }

    private User createUser(String username, String email, LocalDate expirationDate, Roles role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setExpirationDate(expirationDate);
        user.setRole(role);
        user.setEnabled(true);
        user.setAccountLocked(false);
        user.setFirstname("First");
        user.setLastname("Name");
        return user;
    }
}
