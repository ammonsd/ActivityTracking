package com.ammons.taskactivity.repository;

import com.ammons.taskactivity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * UserRepository
 *
 * @author Dean Ammons
 * @version 1.0
 * @since November 2025
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    public Optional<User> findByUsername(String username);

    public Optional<User> findByEmail(String email);

    public boolean existsByUsername(String username);

    /**
     * Returns active users who have a non-blank email address, optionally filtered by last name
     * prefix (case-insensitive). Used by the admin notification feature to build the recipient
     * list.
     *
     * @param lastNamePrefix last name prefix for filtering; null or empty string means no filter
     * @return list of matching active users with email addresses
     */
    @org.springframework.data.jpa.repository.Query("""
            SELECT u FROM User u
            WHERE u.enabled = true
              AND u.email IS NOT NULL
              AND u.email <> ''
              AND (:lastNamePrefix IS NULL OR :lastNamePrefix = ''
                   OR LOWER(u.lastname) LIKE LOWER(CONCAT(:lastNamePrefix, '%')))
            ORDER BY u.lastname, u.firstname
            """)
    List<User> findActiveUsersWithEmail(
            @org.springframework.data.repository.query.Param("lastNamePrefix") String lastNamePrefix);
}

