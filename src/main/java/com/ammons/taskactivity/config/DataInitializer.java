package com.ammons.taskactivity.config;

import com.ammons.taskactivity.entity.Role;
import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * DataInitializer - Creates admin user if not exists
 * 
 * Note: Initial data loading is handled by Spring Boot's native SQL initialization. Place SQL files
 * in src/main/resources/: - schema.sql: Table creation (optional, Hibernate handles this) -
 * data.sql: Initial/seed data
 *
 * Active for: local, docker, and aws profiles - local: Creates admin user even though security is
 * disabled (for database consistency) - docker: Creates admin user for containerized deployments -
 * aws: Creates admin user for cloud deployments
 *
 * @author Dean Ammons
 * @version 1.0
 */
@Component
@Profile({"local", "docker", "aws"})
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);
    private static final String ADMIN_USERNAME = "admin";

    @Value("${app.admin.initial-password:admin123}")
    private String adminPassword;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void initializeData() {
        Optional<User> existingAdmin = userRepository.findByUsername(ADMIN_USERNAME);

        if (!existingAdmin.isPresent()) {
            logger.info("Creating admin user...");

            User admin = new User();
            admin.setUsername(ADMIN_USERNAME);
            admin.setFirstname("");
            admin.setLastname("Administrator");
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            admin.setForcePasswordUpdate(true);
            admin.setCreatedDate(LocalDateTime.now(ZoneOffset.UTC));

            userRepository.save(admin);
            logger.info("Admin user created successfully");
        }
    }
}
