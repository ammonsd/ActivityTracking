package com.ammons.taskactivity.repository;

import com.ammons.taskactivity.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
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
}

