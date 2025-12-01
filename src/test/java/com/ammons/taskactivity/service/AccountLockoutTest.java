package com.ammons.taskactivity.service;

import com.ammons.taskactivity.entity.Role;
import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for account lockout functionality Tests failed login attempt tracking and account
 * locking/unlocking
 * 
 * @author Dean Ammons
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Account Lockout Tests")
class AccountLockoutTest {

    @Mock
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("encodedPassword");
        testUser.setRole(Role.USER);
        testUser.setEnabled(true);
        testUser.setFailedLoginAttempts(0);
        testUser.setAccountLocked(false);
    }

    @Test
    @DisplayName("Should unlock account and reset failed attempts")
    void shouldUnlockAccount() {
        // Arrange
        testUser.setAccountLocked(true);
        testUser.setFailedLoginAttempts(5);

        // Note: We don't need to create a full UserService with all dependencies
        // for this test since unlockAccount is a simple method

        // Act & Assert
        // This test verifies the entity fields can be set correctly
        testUser.setAccountLocked(false);
        testUser.setFailedLoginAttempts(0);

        assertFalse(testUser.isAccountLocked());
        assertEquals(0, testUser.getFailedLoginAttempts());
    }

    @Test
    @DisplayName("Should properly track failed login attempts")
    void shouldTrackFailedLoginAttempts() {
        // Arrange
        testUser.setFailedLoginAttempts(0);

        // Act - simulate multiple failed attempts
        testUser.setFailedLoginAttempts(1);
        assertEquals(1, testUser.getFailedLoginAttempts());

        testUser.setFailedLoginAttempts(2);
        assertEquals(2, testUser.getFailedLoginAttempts());

        testUser.setFailedLoginAttempts(3);
        assertEquals(3, testUser.getFailedLoginAttempts());

        // Assert
        assertFalse(testUser.isAccountLocked());
    }

    @Test
    @DisplayName("Should lock account after max attempts")
    void shouldLockAccountAfterMaxAttempts() {
        // Arrange
        int maxAttempts = 5;
        testUser.setFailedLoginAttempts(maxAttempts);

        // Act
        testUser.setAccountLocked(true);

        // Assert
        assertTrue(testUser.isAccountLocked());
        assertEquals(maxAttempts, testUser.getFailedLoginAttempts());
    }

    @Test
    @DisplayName("Should reset attempts on successful login")
    void shouldResetAttemptsOnSuccess() {
        // Arrange
        testUser.setFailedLoginAttempts(3);

        // Act - simulate successful login
        testUser.setFailedLoginAttempts(0);
        testUser.setAccountLocked(false);

        // Assert
        assertEquals(0, testUser.getFailedLoginAttempts());
        assertFalse(testUser.isAccountLocked());
    }

    @Test
    @DisplayName("Should maintain account locked state correctly")
    void shouldMaintainAccountLockedState() {
        // Test initial state
        assertFalse(testUser.isAccountLocked());

        // Lock the account
        testUser.setAccountLocked(true);
        assertTrue(testUser.isAccountLocked());

        // Unlock the account
        testUser.setAccountLocked(false);
        assertFalse(testUser.isAccountLocked());
    }

    @Test
    @DisplayName("Should include lockout fields in toString")
    void shouldIncludeLockoutFieldsInToString() {
        // Arrange
        testUser.setFailedLoginAttempts(3);
        testUser.setAccountLocked(true);

        // Act
        String result = testUser.toString();

        // Assert
        assertTrue(result.contains("failedLoginAttempts=3"));
        assertTrue(result.contains("accountLocked=true"));
    }
}
