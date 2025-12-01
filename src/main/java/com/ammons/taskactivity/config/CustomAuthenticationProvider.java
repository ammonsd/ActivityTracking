package com.ammons.taskactivity.config;

import com.ammons.taskactivity.entity.Role;
import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Custom authentication provider that prevents GUEST users with expired passwords from
 * authenticating.
 *
 * @author Dean Ammons
 * @version 1.0
 */
public class CustomAuthenticationProvider extends DaoAuthenticationProvider {

    private static final Logger log = LoggerFactory.getLogger(CustomAuthenticationProvider.class);

    private final UserRepository userRepository;

    public CustomAuthenticationProvider(UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder, UserRepository userRepository) {
        this.userRepository = userRepository;
        super.setPasswordEncoder(passwordEncoder);
        super.setUserDetailsService(userDetailsService);
    }

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        String username = authentication.getName();
        log.info("CustomAuthenticationProvider.authenticate() called for user: {}", username);

        // First, let the parent provider do the normal authentication
        Authentication result = super.authenticate(authentication);
        log.info("Parent authentication succeeded for user: {}", username);

        // After successful authentication, check if this is a GUEST user with expired password
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            log.info("User '{}' found with role: {}, expirationDate: {}", username, user.getRole(),
                    user.getExpirationDate());

            if (user.getRole() == Role.GUEST) {
                boolean expired = isPasswordExpired(user);
                log.info("GUEST user '{}' password expired: {}", username, expired);

                if (expired) {
                    log.warn(
                            "Authentication rejected: GUEST user '{}' has expired password and cannot change it",
                            username);
                    throw new GuestPasswordExpiredException(
                            "Password has expired. Contact system administrator.");
                }
            }
        }

        log.info("Authentication successful for user: {}", username);
        return result;
    }

    /**
     * Check if a user's password has expired.
     * 
     * @param user the user to check
     * @return true if password is expired, false otherwise
     */
    private boolean isPasswordExpired(User user) {
        if (user.getExpirationDate() == null) {
            // No expiration date set - consider as not expired
            return false;
        }

        return LocalDate.now(ZoneOffset.UTC).isAfter(user.getExpirationDate());
    }
}
