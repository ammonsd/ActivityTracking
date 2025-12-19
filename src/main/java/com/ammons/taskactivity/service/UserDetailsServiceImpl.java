package com.ammons.taskactivity.service;

import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * UserDetailsServiceImpl
 *
 * @author Dean Ammons
 * @version 1.0
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        log.debug("Loading user: {}, password hash starts with: {}", username,
                user.getPassword() != null
                        ? user.getPassword().substring(0, Math.min(20, user.getPassword().length()))
                        : "NULL");

        // Handle case where role might not be loaded yet (e.g., during test initialization)
        if (user.getRole() == null) {
            log.error("User {} has null role - this should not happen in production", username);
            throw new UsernameNotFoundException("User " + username + " has no role assigned");
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername()).password(user.getPassword())
                .authorities("ROLE_" + user.getRole().getName()).accountExpired(false)
                .accountLocked(user.isAccountLocked()).credentialsExpired(false)
                .disabled(!user.isEnabled()).build();
    }

    /**
     * Updates the last login time for a user. This should be called after successful
     * authentication.
     */
    public void updateLastLoginTime(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            LocalDateTime utcNow = LocalDateTime.now(ZoneOffset.UTC);
            log.info("Setting last login for user '{}' to UTC time: {}", username, utcNow);
            user.setLastLogin(utcNow);
            userRepository.save(user);
            log.info("Saved user '{}' with last login: {}", username, user.getLastLogin());
        });
    }
}
