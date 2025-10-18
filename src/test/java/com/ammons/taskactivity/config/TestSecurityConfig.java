package com.ammons.taskactivity.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test Security Configuration for unit and integration tests. Provides simplified security setup
 * with in-memory users for testing.
 * 
 * @author Dean Ammons
 */
@TestConfiguration
@EnableWebSecurity
public class TestSecurityConfig {

    /**
     * Security filter chain for testing - disables CSRF and allows all requests
     */
    @Bean
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    /**
     * In-memory user details service for testing with predefined test users
     */
    @Bean
    public UserDetailsService testUserDetailsService() {
        UserDetails user = User.builder().username("testuser")
                .password(passwordEncoder().encode("testpass")).roles("USER").build();

        UserDetails admin = User.builder().username("testadmin")
                .password(passwordEncoder().encode("testpass")).roles("ADMIN").build();

        UserDetails guest = User.builder().username("testguest")
                .password(passwordEncoder().encode("testpass")).roles("GUEST").build();

        return new InMemoryUserDetailsManager(user, admin, guest);
    }

    /**
     * Password encoder for test users
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
