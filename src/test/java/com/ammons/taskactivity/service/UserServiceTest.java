package com.ammons.taskactivity.service;

import com.ammons.taskactivity.entity.Role;
import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.repository.UserRepository;
import com.ammons.taskactivity.validation.PasswordValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService Tests user management operations and validation
 * 
 * @author Dean Ammons
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

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, passwordValidationService,
                loginAuditService, passwordHistoryService);
    }

    @Test
    void testCreateUserSuccess() {
        // Arrange
        String username = "testuser";
        String firstname = "Test";
        String lastname = "User";
        String password = "ValidPass123!"; // Updated to meet new requirements
        Role role = Role.USER;
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
        Role role = Role.USER;

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
        // Test null username
        assertThrows(IllegalArgumentException.class,
                        () -> userService.createUser(null, "ValidPass123!", Role.USER));

        // Test empty username
        assertThrows(IllegalArgumentException.class,
                        () -> userService.createUser("", "ValidPass123!", Role.USER));

        // Test short username
        assertThrows(IllegalArgumentException.class,
                        () -> userService.createUser("ab", "ValidPass123!", Role.USER));

        // Test null password
        assertThrows(IllegalArgumentException.class,
                () -> userService.createUser("testuser", null, Role.USER));

        // Test password too short (less than 10 characters)
        assertThrows(IllegalArgumentException.class,
                        () -> userService.createUser("testuser", "Short1!", Role.USER));

        // Test password without uppercase
        assertThrows(IllegalArgumentException.class,
                        () -> userService.createUser("testuser", "nouppercas1!", Role.USER));

        // Test password without digit
        assertThrows(IllegalArgumentException.class,
                        () -> userService.createUser("testuser", "NoDigitPass!", Role.USER));

        // Test password without special character
        assertThrows(IllegalArgumentException.class,
                        () -> userService.createUser("testuser", "NoSpecial123", Role.USER));

        // Test null role
        assertThrows(IllegalArgumentException.class,
                        () -> userService.createUser("testuser", "ValidPass123!", null));
    }

    @Test
    void testGetUserByUsername() {
        // Arrange
        String username = "testuser";
        User user = new User(username, "encodedPassword", Role.USER);
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
        User user = new User(username, encodedOldPassword, Role.USER);

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
}
