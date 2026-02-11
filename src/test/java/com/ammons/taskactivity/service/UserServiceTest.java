package com.ammons.taskactivity.service;

import com.ammons.taskactivity.entity.PasswordHistory;
import com.ammons.taskactivity.entity.Roles;
import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.repository.PasswordHistoryRepository;
import com.ammons.taskactivity.repository.UserRepository;
import com.ammons.taskactivity.validation.PasswordValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService Tests user management operations and validation
 * 
 * Modified: February 2026 - Added password history validation tests
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private PasswordValidationService passwordValidationService;

    @Mock
    private LoginAuditService loginAuditService;

    @Mock
    private PasswordHistoryService passwordHistoryService;

    @Mock
    private TokenRevocationService tokenRevocationService;

    @Mock
    private PasswordHistoryRepository passwordHistoryRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, passwordValidationService,
                        loginAuditService, passwordHistoryService, tokenRevocationService,
                        passwordHistoryRepository);

        // Set password history configuration values via reflection
        ReflectionTestUtils.setField(userService, "historyEnabled", true);
        ReflectionTestUtils.setField(userService, "historySize", 5);
    }

    @Test
    void testCreateUserSuccess() {
        // Arrange
        String username = "testuser";
        String firstname = "Test";
        String lastname = "User";
        String password = "ValidPass123!"; // Updated to meet new requirements
        Roles role = new Roles("USER");
        String encodedPassword = "encodedPassword";

        when(userRepository.existsByUsername(username)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class)))
                .thenReturn(new User(username, encodedPassword, role));

        // Act
        User result = userService.createUser(username, firstname, lastname, password, role, true);

        // Assert
        assertNotNull(result);
        assertEquals(username, result.getUsername());
        assertEquals(encodedPassword, result.getPassword());
        assertEquals(role, result.getRole());

        verify(userRepository).existsByUsername(username);
        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testCreateUserWithExistingUsername() {
        // Arrange
        String username = "existinguser";
        String firstname = "Existing";
        String lastname = "User";
        String password = "ValidPass123!"; // Updated to meet new requirements
        Roles role = new Roles("USER");

        when(userRepository.existsByUsername(username)).thenReturn(true);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(username, firstname, lastname, password, role, true));

        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository).existsByUsername(username);
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testCreateUserWithInvalidInput() {
        Roles role = new Roles("USER");

        // Test null username
        assertThrows(IllegalArgumentException.class,
                () -> userService.createUser(null, "ValidPass123!", role));

        // Test empty username
        assertThrows(IllegalArgumentException.class,
                () -> userService.createUser("", "ValidPass123!", role));

        // Test short username
        assertThrows(IllegalArgumentException.class,
                () -> userService.createUser("ab", "ValidPass123!", role));

        // Test null password
        assertThrows(IllegalArgumentException.class,
                () -> userService.createUser("testuser", null, role));

        // Test password too short (less than 10 characters)
        assertThrows(IllegalArgumentException.class,
                () -> userService.createUser("testuser", "Short1!", role));

        // Test password without uppercase
        assertThrows(IllegalArgumentException.class,
                () -> userService.createUser("testuser", "nouppercas1!", role));

        // Test password without digit
        assertThrows(IllegalArgumentException.class,
                () -> userService.createUser("testuser", "NoDigitPass!", role));

        // Test password without special character
        assertThrows(IllegalArgumentException.class,
                () -> userService.createUser("testuser", "NoSpecial123", role));

        // Test null role
        assertThrows(IllegalArgumentException.class,
                        () -> userService.createUser("testuser", "ValidPass123!", null));
    }

    @Test
    void testGetUserByUsername() {
        // Arrange
        String username = "testuser";
        User user = new User(username, "encodedPassword", new Roles("USER"));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userService.getUserByUsername(username);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(username, result.get().getUsername());
        verify(userRepository).findByUsername(username);
    }

    @Test
    void testGetUserByUsernameWithInvalidInput() {
        // Test null username
        assertThrows(IllegalArgumentException.class, () -> userService.getUserByUsername(null));

        // Test empty username
        assertThrows(IllegalArgumentException.class, () -> userService.getUserByUsername(""));
    }

    @Test
    void testChangePasswordSuccess() {
        // Arrange
        String username = "testuser";
        String newPassword = "NewValid123!"; // Updated to meet new requirements
        String encodedOldPassword = "encodedOldPassword";
        String encodedNewPassword = "encodedNewPassword";
        User user = new User(username, encodedOldPassword, new Roles("USER"));

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);
        when(userRepository.save(any(User.class))).thenReturn(user);
        // Mock that new password does NOT match current password (password reuse check)
        when(passwordEncoder.matches(newPassword, encodedOldPassword)).thenReturn(false);
        // Mock that original password hash is null (no session history)
        when(passwordHistoryService.getOriginalPasswordHash(username)).thenReturn(null);

        // Act - Test the 3-parameter method directly (where the actual logic is)
        // Pass false for clearForceUpdate, so forcePasswordUpdate should remain at its default
        // value (true)
        userService.changePassword(username, newPassword, false);

        // Assert
        verify(userRepository, times(2)).findByUsername(username); // Called twice - once for
                                                                   // change, once for verification
        verify(passwordEncoder).encode(newPassword);
        verify(userRepository).save(user);
        verify(passwordEncoder).matches(newPassword, encodedOldPassword);
        verify(passwordHistoryService).getOriginalPasswordHash(username);
        assertEquals(encodedNewPassword, user.getPassword());
        assertTrue(user.isForcePasswordUpdate()); // Should still be true since we passed false for
                                                  // clearForceUpdate
    }

    @Test
    void testChangePasswordUserNotFound() {
        // Arrange
        String username = "nonexistentuser";
        String newPassword = "NewValid123!"; // Updated to meet new requirements

        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert - Test the 3-parameter method directly
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                        () -> userService.changePassword(username, newPassword, false));

        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findByUsername(username);
        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testChangePasswordWithInvalidInput() {
            // Note: We don't need to mock userRepository.findByUsername here because
            // all these tests should fail validation before reaching the repository lookup

            // Test null username - should fail before checking repository
        assertThrows(IllegalArgumentException.class,
                        () -> userService.changePassword(null, "NewValid123!", false));

        // Test empty username - should fail before checking repository
        assertThrows(IllegalArgumentException.class,
                        () -> userService.changePassword("", "NewValid123!", false));

        // Test null password - should fail before checking repository
        assertThrows(IllegalArgumentException.class,
                        () -> userService.changePassword("testuser", null, false));

        // Test password too short - should fail at validation, before checking repository
        assertThrows(IllegalArgumentException.class,
                        () -> userService.changePassword("testuser", "Short1!", false));

        // Test password without uppercase - should fail at validation, before checking repository
        assertThrows(IllegalArgumentException.class,
                        () -> userService.changePassword("testuser", "nouppercase1!", false));

        // Test password without digit - should fail at validation, before checking repository
        assertThrows(IllegalArgumentException.class,
                        () -> userService.changePassword("testuser", "NoDigitPass!", false));

        // Test password without special character - should fail at validation, before checking
        // repository
        assertThrows(IllegalArgumentException.class,
                        () -> userService.changePassword("testuser", "NoSpecial123", false));
    }

    @Test
    void testIsPasswordExpiringSoon_ExpiringToday() {
            // Arrange
            String username = "testuser";
            User user = new User(username, "encodedPassword", new Roles("USER"));
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            user.setExpirationDate(today);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

            // Act
            boolean result = userService.isPasswordExpiringSoon(username);

            // Assert
            assertTrue(result, "Password expiring today should trigger warning");
            verify(userRepository).findByUsername(username);
    }

    @Test
    void testIsPasswordExpiringSoon_ExpiringInSevenDays() {
            // Arrange
            String username = "testuser";
            User user = new User(username, "encodedPassword", new Roles("USER"));
            LocalDate sevenDaysFromNow = LocalDate.now(ZoneOffset.UTC).plusDays(7);
            user.setExpirationDate(sevenDaysFromNow);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

            // Act
            boolean result = userService.isPasswordExpiringSoon(username);

            // Assert
            assertTrue(result, "Password expiring in 7 days should trigger warning");
            verify(userRepository).findByUsername(username);
    }

    @Test
    void testIsPasswordExpiringSoon_ExpiringInThreeDays() {
            // Arrange
            String username = "testuser";
            User user = new User(username, "encodedPassword", new Roles("USER"));
            LocalDate threeDaysFromNow = LocalDate.now(ZoneOffset.UTC).plusDays(3);
            user.setExpirationDate(threeDaysFromNow);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

            // Act
            boolean result = userService.isPasswordExpiringSoon(username);

            // Assert
            assertTrue(result, "Password expiring in 3 days should trigger warning");
            verify(userRepository).findByUsername(username);
    }

    @Test
    void testIsPasswordExpiringSoon_ExpiringInEightDays() {
            // Arrange
            String username = "testuser";
            User user = new User(username, "encodedPassword", new Roles("USER"));
            LocalDate eightDaysFromNow = LocalDate.now(ZoneOffset.UTC).plusDays(8);
            user.setExpirationDate(eightDaysFromNow);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

            // Act
            boolean result = userService.isPasswordExpiringSoon(username);

            // Assert
            assertFalse(result, "Password expiring in 8 days should not trigger warning");
            verify(userRepository).findByUsername(username);
    }

    @Test
    void testIsPasswordExpiringSoon_AlreadyExpired() {
            // Arrange
            String username = "testuser";
            User user = new User(username, "encodedPassword", new Roles("USER"));
            LocalDate yesterday = LocalDate.now(ZoneOffset.UTC).minusDays(1);
            user.setExpirationDate(yesterday);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

            // Act
            boolean result = userService.isPasswordExpiringSoon(username);

            // Assert
            assertFalse(result, "Already expired password should not trigger warning");
            verify(userRepository).findByUsername(username);
    }

    @Test
    void testIsPasswordExpiringSoon_NoExpirationDate() {
            // Arrange
            String username = "testuser";
            User user = new User(username, "encodedPassword", new Roles("USER"));
            user.setExpirationDate(null);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

            // Act
            boolean result = userService.isPasswordExpiringSoon(username);

            // Assert
            assertFalse(result, "No expiration date should not trigger warning");
            verify(userRepository).findByUsername(username);
    }

    @Test
    void testIsPasswordExpiringSoon_UserNotFound() {
            // Arrange
            String username = "nonexistentuser";
            when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

            // Act
            boolean result = userService.isPasswordExpiringSoon(username);

            // Assert
            assertFalse(result, "Non-existent user should not trigger warning");
            verify(userRepository).findByUsername(username);
    }

    @Test
    void testGetDaysUntilExpiration_ExpiringToday() {
            // Arrange
            String username = "testuser";
            User user = new User(username, "encodedPassword", new Roles("USER"));
            LocalDate today = LocalDate.now(ZoneOffset.UTC);
            user.setExpirationDate(today);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

            // Act
            Long result = userService.getDaysUntilExpiration(username);

            // Assert
            assertEquals(0L, result, "Password expiring today should return 0 days");
            verify(userRepository).findByUsername(username);
    }

    @Test
    void testGetDaysUntilExpiration_ExpiringInSevenDays() {
            // Arrange
            String username = "testuser";
            User user = new User(username, "encodedPassword", new Roles("USER"));
            LocalDate sevenDaysFromNow = LocalDate.now(ZoneOffset.UTC).plusDays(7);
            user.setExpirationDate(sevenDaysFromNow);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

            // Act
            Long result = userService.getDaysUntilExpiration(username);

            // Assert
            assertEquals(7L, result, "Password expiring in 7 days should return 7");
            verify(userRepository).findByUsername(username);
    }

    // ========================================
    // Password History Tests - Added February 2026
    // ========================================

    @Test
    void testChangePasswordWithHistoryEnabled_SavesToHistory() {
            // Arrange
            String username = "testuser";
            String newPassword = "NewValid123!";
            String encodedOldPassword = "encodedOldPassword";
            String encodedNewPassword = "encodedNewPassword";
            User user = new User(username, encodedOldPassword, new Roles("USER"));
            user.setId(1L);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(passwordEncoder.matches(newPassword, encodedOldPassword)).thenReturn(false);
            when(passwordHistoryService.getOriginalPasswordHash(username)).thenReturn(null);
            when(passwordHistoryRepository.save(any(PasswordHistory.class)))
                            .thenReturn(new PasswordHistory(1L, encodedNewPassword));

            // Act
            userService.changePassword(username, newPassword, false);

            // Assert - Verify password history was saved
            verify(passwordHistoryRepository).save(any(PasswordHistory.class));
            verify(passwordHistoryRepository).deleteOldPasswordHistory(1L, 5);
    }

    @Test
    void testChangePasswordWithHistoryDisabled_DoesNotSaveToHistory() {
            // Arrange - Disable history for this test
            ReflectionTestUtils.setField(userService, "historyEnabled", false);

            String username = "testuser";
            String newPassword = "NewValid123!";
            String encodedOldPassword = "encodedOldPassword";
            String encodedNewPassword = "encodedNewPassword";
            User user = new User(username, encodedOldPassword, new Roles("USER"));
            user.setId(1L);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(passwordEncoder.matches(newPassword, encodedOldPassword)).thenReturn(false);
            when(passwordHistoryService.getOriginalPasswordHash(username)).thenReturn(null);

            // Act
            userService.changePassword(username, newPassword, false);

            // Assert - Verify password history was NOT saved
            verify(passwordHistoryRepository, never()).save(any(PasswordHistory.class));
            verify(passwordHistoryRepository, never()).deleteOldPasswordHistory(anyLong(),
                            anyInt());

            // Re-enable for other tests
            ReflectionTestUtils.setField(userService, "historyEnabled", true);
    }

    @Test
    void testCreateUserWithHistoryEnabled_SavesInitialPasswordToHistory() {
            // Arrange
            String username = "newuser";
            String firstname = "New";
            String lastname = "User";
            String password = "ValidPass123!";
            Roles role = new Roles("USER");
            String encodedPassword = "encodedPassword";

            User savedUser = new User(username, encodedPassword, role);
            savedUser.setId(1L);

            when(userRepository.existsByUsername(username)).thenReturn(false);
            when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(passwordHistoryRepository.save(any(PasswordHistory.class)))
                            .thenReturn(new PasswordHistory(1L, encodedPassword));

            // Act
            User result = userService.createUser(username, firstname, lastname, password, role,
                            true);

            // Assert
            assertNotNull(result);
            verify(passwordHistoryRepository).save(any(PasswordHistory.class));
    }

    @Test
    void testCreateUserWithHistoryDisabled_DoesNotSaveToHistory() {
            // Arrange - Disable history for this test
            ReflectionTestUtils.setField(userService, "historyEnabled", false);

            String username = "newuser";
            String firstname = "New";
            String lastname = "User";
            String password = "ValidPass123!";
            Roles role = new Roles("USER");
            String encodedPassword = "encodedPassword";

            when(userRepository.existsByUsername(username)).thenReturn(false);
            when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
            when(userRepository.save(any(User.class)))
                            .thenReturn(new User(username, encodedPassword, role));

            // Act
            User result = userService.createUser(username, firstname, lastname, password, role,
                            true);

            // Assert
            assertNotNull(result);
            verify(passwordHistoryRepository, never()).save(any(PasswordHistory.class));

            // Re-enable for other tests
            ReflectionTestUtils.setField(userService, "historyEnabled", true);
    }

    @Test
    void testChangePassword_CallsPasswordHistoryValidation() {
            // Arrange
            String username = "testuser";
            String newPassword = "NewValid123!";
            String encodedOldPassword = "encodedOldPassword";
            String encodedNewPassword = "encodedNewPassword";
            User user = new User(username, encodedOldPassword, new Roles("USER"));
            user.setId(1L);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(passwordEncoder.matches(newPassword, encodedOldPassword)).thenReturn(false);
            when(passwordHistoryService.getOriginalPasswordHash(username)).thenReturn(null);

            // Act
            userService.changePassword(username, newPassword, false);

            // Assert - Verify password history validation was called
            verify(passwordValidationService).validatePasswordNotInHistory(1L, newPassword);
    }

    @Test
    void testChangePassword_CleansUpOldHistoryEntries() {
            // Arrange
            String username = "testuser";
            String newPassword = "NewValid123!";
            String encodedOldPassword = "encodedOldPassword";
            String encodedNewPassword = "encodedNewPassword";
            User user = new User(username, encodedOldPassword, new Roles("USER"));
            user.setId(42L); // Use specific ID to verify cleanup call

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(newPassword)).thenReturn(encodedNewPassword);
            when(userRepository.save(any(User.class))).thenReturn(user);
            when(passwordEncoder.matches(newPassword, encodedOldPassword)).thenReturn(false);
            when(passwordHistoryService.getOriginalPasswordHash(username)).thenReturn(null);
            when(passwordHistoryRepository.save(any(PasswordHistory.class)))
                            .thenReturn(new PasswordHistory(42L, encodedNewPassword));

            // Act
            userService.changePassword(username, newPassword, false);

            // Assert - Verify cleanup was called with correct user ID and history size
            verify(passwordHistoryRepository).deleteOldPasswordHistory(42L, 5);
    }
}
