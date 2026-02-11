package com.ammons.taskactivity.repository;

import com.ammons.taskactivity.entity.PasswordHistory;
import com.ammons.taskactivity.entity.Roles;
import com.ammons.taskactivity.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Description: Unit tests for PasswordHistoryRepository
 * 
 * Tests all query methods including: - findRecentByUserId with pagination -
 * deleteOldPasswordHistory cleanup - countByUserId - findAllByUserId - CASCADE DELETE when user is
 * deleted
 *
 * @author Dean Ammons
 * @version 1.0
 * @since February 2026
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {"spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"})
@DisplayName("PasswordHistoryRepository Tests")
class PasswordHistoryRepositoryTest {

    @Autowired
    private PasswordHistoryRepository passwordHistoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        // Create test user
        Roles userRole = new Roles("USER");
        entityManager.persist(userRole);

        testUser = new User("testuser", "encodedPassword", userRole);
        testUser.setLastname("User");
        entityManager.persist(testUser);
        entityManager.flush();

        testUserId = testUser.getId();
    }

    @Test
    @DisplayName("Should save password history entry")
    void shouldSavePasswordHistory() {
        // Given
        PasswordHistory history = new PasswordHistory(testUserId, "hashedPassword1");

        // When
        PasswordHistory saved = passwordHistoryRepository.save(history);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(testUserId);
        assertThat(saved.getPasswordHash()).isEqualTo("hashedPassword1");
        assertThat(saved.getChangedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should find recent passwords by user ID with correct ordering")
    void shouldFindRecentPasswordsByUserId() {
        // Given - Create 5 password history entries
        for (int i = 1; i <= 5; i++) {
            PasswordHistory history = new PasswordHistory(testUserId, "hashedPassword" + i);
            entityManager.persist(history);
            // Small delay to ensure different timestamps
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        entityManager.flush();

        // When - Request last 3 passwords
        Pageable pageable = PageRequest.of(0, 3);
        List<PasswordHistory> recentPasswords =
                passwordHistoryRepository.findRecentByUserId(testUserId, pageable);

        // Then
        assertThat(recentPasswords).hasSize(3);
        // Verify ordering - most recent first
        assertThat(recentPasswords.get(0).getPasswordHash()).isEqualTo("hashedPassword5");
        assertThat(recentPasswords.get(1).getPasswordHash()).isEqualTo("hashedPassword4");
        assertThat(recentPasswords.get(2).getPasswordHash()).isEqualTo("hashedPassword3");
    }

    @Test
    @DisplayName("Should return empty list when no password history exists")
    void shouldReturnEmptyListWhenNoHistory() {
        // When
        Pageable pageable = PageRequest.of(0, 5);
        List<PasswordHistory> recentPasswords =
                passwordHistoryRepository.findRecentByUserId(testUserId, pageable);

        // Then
        assertThat(recentPasswords).isEmpty();
    }

    @Test
    @DisplayName("Should delete old password history entries beyond keep count")
    void shouldDeleteOldPasswordHistory() {
        // Given - Create 8 password history entries
        for (int i = 1; i <= 8; i++) {
            PasswordHistory history = new PasswordHistory(testUserId, "hashedPassword" + i);
            entityManager.persist(history);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        entityManager.flush();
        entityManager.clear();

        // Verify initial count
        long initialCount = passwordHistoryRepository.countByUserId(testUserId);
        assertThat(initialCount).isEqualTo(8);

        // When - Keep only 5 most recent
        passwordHistoryRepository.deleteOldPasswordHistory(testUserId, 5);
        entityManager.flush();
        entityManager.clear();

        // Then - Should have only 5 entries remaining
        long remainingCount = passwordHistoryRepository.countByUserId(testUserId);
        assertThat(remainingCount).isEqualTo(5);

        // Verify that the 5 most recent passwords remain
        List<PasswordHistory> remaining = passwordHistoryRepository.findAllByUserId(testUserId);
        assertThat(remaining).hasSize(5);
        assertThat(remaining).extracting(PasswordHistory::getPasswordHash)
                .containsExactlyInAnyOrder("hashedPassword4", "hashedPassword5", "hashedPassword6",
                        "hashedPassword7", "hashedPassword8");
    }

    @Test
    @DisplayName("Should count password history entries by user ID")
    void shouldCountByUserId() {
        // Given - Create 3 password history entries
        for (int i = 1; i <= 3; i++) {
            PasswordHistory history = new PasswordHistory(testUserId, "hashedPassword" + i);
            entityManager.persist(history);
        }
        entityManager.flush();

        // When
        long count = passwordHistoryRepository.countByUserId(testUserId);

        // Then
        assertThat(count).isEqualTo(3);
    }

    @Test
    @DisplayName("Should find all password history entries by user ID")
    void shouldFindAllByUserId() {
        // Given - Create 4 password history entries
        for (int i = 1; i <= 4; i++) {
            PasswordHistory history = new PasswordHistory(testUserId, "hashedPassword" + i);
            entityManager.persist(history);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        entityManager.flush();

        // When
        List<PasswordHistory> allHistory = passwordHistoryRepository.findAllByUserId(testUserId);

        // Then
        assertThat(allHistory).hasSize(4);
        // Verify ordering - most recent first
        assertThat(allHistory.get(0).getPasswordHash()).isEqualTo("hashedPassword4");
        assertThat(allHistory.get(3).getPasswordHash()).isEqualTo("hashedPassword1");
    }

    @Test
    @DisplayName("Should cascade delete password history when user is deleted")
    void shouldCascadeDeleteWhenUserDeleted() {
        // Given - Create password history entries for the test user
        for (int i = 1; i <= 3; i++) {
            PasswordHistory history = new PasswordHistory(testUserId, "hashedPassword" + i);
            entityManager.persist(history);
        }
        entityManager.flush();

        // Verify history exists
        long initialCount = passwordHistoryRepository.countByUserId(testUserId);
        assertThat(initialCount).isEqualTo(3);

        // When - Delete the user (reload from DB to ensure managed state)
        User userToDelete = entityManager.find(User.class, testUserId);
        entityManager.remove(userToDelete);
        entityManager.flush();
        entityManager.clear();

        // Then - Password history should be automatically deleted (CASCADE)
        long finalCount = passwordHistoryRepository.countByUserId(testUserId);
        assertThat(finalCount).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle multiple users with separate password histories")
    void shouldHandleMultipleUsersSeparately() {
        // Given - Create second user with its own role
        Roles secondUserRole = new Roles("USER2");
        entityManager.persist(secondUserRole);

        User secondUser = new User("seconduser", "encodedPassword", secondUserRole);
        secondUser.setLastname("Second");
        entityManager.persist(secondUser);
        entityManager.flush();

        Long secondUserId = secondUser.getId();

        // Create password history for both users
        for (int i = 1; i <= 3; i++) {
            PasswordHistory history1 = new PasswordHistory(testUserId, "user1_password" + i);
            PasswordHistory history2 = new PasswordHistory(secondUserId, "user2_password" + i);
            entityManager.persist(history1);
            entityManager.persist(history2);
        }
        entityManager.flush();

        // When
        long user1Count = passwordHistoryRepository.countByUserId(testUserId);
        long user2Count = passwordHistoryRepository.countByUserId(secondUserId);

        // Then
        assertThat(user1Count).isEqualTo(3);
        assertThat(user2Count).isEqualTo(3);

        // Verify histories are separate
        List<PasswordHistory> user1History = passwordHistoryRepository.findAllByUserId(testUserId);
        assertThat(user1History).extracting(PasswordHistory::getPasswordHash)
                .allMatch(hash -> hash.startsWith("user1_"));

        List<PasswordHistory> user2History =
                passwordHistoryRepository.findAllByUserId(secondUserId);
        assertThat(user2History).extracting(PasswordHistory::getPasswordHash)
                .allMatch(hash -> hash.startsWith("user2_"));
    }

    @Test
    @DisplayName("Should not delete any entries when keep count exceeds existing count")
    void shouldNotDeleteWhenKeepCountExceedsExistingCount() {
        // Given - Create only 3 password history entries
        for (int i = 1; i <= 3; i++) {
            PasswordHistory history = new PasswordHistory(testUserId, "hashedPassword" + i);
            entityManager.persist(history);
        }
        entityManager.flush();

        // When - Try to keep 10 entries (more than exist)
        passwordHistoryRepository.deleteOldPasswordHistory(testUserId, 10);
        entityManager.flush();

        // Then - All 3 entries should still exist
        long count = passwordHistoryRepository.countByUserId(testUserId);
        assertThat(count).isEqualTo(3);
    }
}
