package com.ammons.taskactivity.repository;

import com.ammons.taskactivity.entity.PasswordHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Description: Repository for managing password history entries.
 * 
 * Provides methods for: - Retrieving recent password history for a user (for validation) - Cleaning
 * up old password history entries (beyond configured size) - Counting history entries per user
 * 
 * All queries use parameterized @Query annotations with @Param for SQL injection prevention.
 * 
 * Usage Example:
 * 
 * <pre>
 * // Get last 5 passwords for user
 * List&lt;PasswordHistory&gt; recent = repository.findRecentByUserId(userId, PageRequest.of(0, 5));
 * 
 * // Cleanup old entries (keep only last 5)
 * repository.deleteOldPasswordHistory(userId, 5);
 * </pre>
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since February 2026
 */
@Repository
public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {

    /**
     * Retrieves the most recent password history entries for a user, ordered by changed_at DESC.
     * 
     * This method is used by PasswordValidationService to check if a new password matches any
     * recent passwords. The number of entries returned is controlled by the Pageable parameter.
     * 
     * @param userId the user's ID
     * @param pageable pagination parameters (typically PageRequest.of(0, historySize))
     * @return list of password history entries, most recent first
     * 
     * @see com.ammons.taskactivity.service.PasswordValidationService#validatePasswordNotInHistory
     */
    @Query("SELECT ph FROM PasswordHistory ph WHERE ph.userId = :userId "
            + "ORDER BY ph.changedAt DESC")
    List<PasswordHistory> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Deletes old password history entries for a user, keeping only the N most recent entries.
     * 
     * This cleanup method is called after each password change to prevent unlimited growth of the
     * password_history table. It keeps the configured number of recent passwords (specified by
     * keepCount) and removes all older entries.
     * 
     * Implementation Note: This uses a subquery to identify the IDs of the entries to keep, then
     * deletes everything else for the user. PostgreSQL-specific LIMIT clause is used in the
     * subquery.
     * 
     * @param userId the user's ID
     * @param keepCount number of recent password entries to keep (e.g., 5)
     * 
     * @see com.ammons.taskactivity.service.UserService#changePassword
     */
    @Modifying
    @Query(value = "DELETE FROM password_history " + "WHERE user_id = :userId " + "AND id NOT IN ("
            + "  SELECT id FROM password_history " + "  WHERE user_id = :userId "
            + "  ORDER BY changed_at DESC " + "  LIMIT :keepCount" + ")", nativeQuery = true)
    void deleteOldPasswordHistory(@Param("userId") Long userId, @Param("keepCount") int keepCount);

    /**
     * Counts the total number of password history entries for a user.
     * 
     * Useful for verification, testing, and monitoring purposes. In normal operation, this count
     * should not exceed the configured history size (default: 5) by more than 1 entry (the new
     * password before cleanup).
     * 
     * @param userId the user's ID
     * @return count of password history entries for the user
     */
    @Query("SELECT COUNT(ph) FROM PasswordHistory ph WHERE ph.userId = :userId")
    long countByUserId(@Param("userId") Long userId);

    /**
     * Finds all password history entries for a user, ordered by most recent first.
     * 
     * This method is primarily used for testing and administrative purposes. For normal validation,
     * use findRecentByUserId() with pagination instead.
     * 
     * @param userId the user's ID
     * @return list of all password history entries for the user, ordered by changed_at DESC
     */
    @Query("SELECT ph FROM PasswordHistory ph WHERE ph.userId = :userId "
            + "ORDER BY ph.changedAt DESC")
    List<PasswordHistory> findAllByUserId(@Param("userId") Long userId);

    /**
     * Deletes all password history entries for a user.
     * 
     * Note: This is typically not needed due to CASCADE DELETE on the foreign key. When a user is
     * deleted, their password history is automatically removed. This method is provided for manual
     * cleanup or testing purposes.
     * 
     * @param userId the user's ID
     */
    @Modifying
    @Query("DELETE FROM PasswordHistory ph WHERE ph.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
