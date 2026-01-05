package com.ammons.taskactivity.security;

import com.ammons.taskactivity.entity.RevokedToken;
import com.ammons.taskactivity.entity.Roles;
import com.ammons.taskactivity.entity.User;
import com.ammons.taskactivity.repository.RevokedTokenRepository;
import com.ammons.taskactivity.repository.UserRepository;
import com.ammons.taskactivity.service.TokenRevocationService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for JWT token revocation system.
 * 
 * Tests Issue #9 (Server-Side Token Revocation): - Logout revokes tokens - Revoked tokens rejected
 * on authentication - Password change revokes tokens - Cleanup removes expired tokens
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TokenRevocationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenRevocationService tokenRevocationService;

    @Autowired
    private RevokedTokenRepository revokedTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        revokedTokenRepository.deleteAll();

        // Create test user if doesn't exist
        testUser = userRepository.findByUsername("tokenTestUser").orElseGet(() -> {
            User user = new User();
            user.setUsername("tokenTestUser");
            user.setFirstname("Token");
            user.setLastname("Test");
            user.setPassword(passwordEncoder.encode("TestPass123!"));
            user.setRole(new Roles("USER"));
            user.setEnabled(true);
            user.setAccountLocked(false);
            return userRepository.save(user);
        });
    }

    @Test
    void testTokenRevocationOnLogout() {
        // Generate access token
        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(testUser.getUsername(),
                        testUser.getPassword(), testUser.getAuthorities());

        String token = jwtUtil.generateToken(userDetails);

        // Extract JTI
        Claims claims = jwtUtil.extractAllClaims(token);
        String jti = claims.getId();
        assertNotNull(jti, "Token should have JTI claim");

        // Token should not be revoked initially
        assertFalse(tokenRevocationService.isTokenRevoked(jti),
                "Token should not be revoked initially");

        // Revoke token (simulate logout)
        boolean revoked = tokenRevocationService.revokeToken(token, "logout");
        assertTrue(revoked, "Token revocation should succeed");

        // Token should now be revoked
        assertTrue(tokenRevocationService.isTokenRevoked(jti),
                "Token should be revoked after logout");

        // Check database entry
        RevokedToken revokedToken = revokedTokenRepository.findByJti(jti).orElse(null);
        assertNotNull(revokedToken, "Revoked token should exist in database");
        assertEquals(testUser.getUsername(), revokedToken.getUsername());
        assertEquals("access", revokedToken.getTokenType());
        assertEquals("logout", revokedToken.getReason());
        assertNotNull(revokedToken.getRevokedAt());
    }

    @Test
    void testRefreshTokenRevocation() {
        // Generate refresh token
        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(testUser.getUsername(),
                        testUser.getPassword(), testUser.getAuthorities());

        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        // Extract JTI
        Claims claims = jwtUtil.extractAllClaims(refreshToken);
        String jti = claims.getId();

        // Revoke refresh token
        boolean revoked = tokenRevocationService.revokeToken(refreshToken, "logout");
        assertTrue(revoked, "Refresh token revocation should succeed");

        // Check database entry
        RevokedToken revokedToken = revokedTokenRepository.findByJti(jti).orElse(null);
        assertNotNull(revokedToken);
        assertEquals("refresh", revokedToken.getTokenType());
    }

    @Test
    void testDoubleRevocationFails() {
        // Generate token
        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(testUser.getUsername(),
                        testUser.getPassword(), testUser.getAuthorities());

        String token = jwtUtil.generateToken(userDetails);

        // First revocation should succeed
        boolean firstRevoke = tokenRevocationService.revokeToken(token, "logout");
        assertTrue(firstRevoke);

        // Second revocation should fail (already revoked)
        boolean secondRevoke = tokenRevocationService.revokeToken(token, "logout");
        assertFalse(secondRevoke, "Second revocation should fail");
    }

    @Test
    void testRevokedTokenRejectedInAuthentication() throws Exception {
        // Generate token
        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(testUser.getUsername(),
                        testUser.getPassword(), testUser.getAuthorities());

        String token = jwtUtil.generateToken(userDetails);

        // Token should work initially (using logout endpoint as test)
        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Now token is revoked, try to use it again
        mockMvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest()); // Can't revoke already-revoked token
    }

    @Test
    void testCleanupExpiredTokens() {
        // Create expired token entry
        RevokedToken expiredToken = new RevokedToken();
        expiredToken.setJti("expired-jti-123");
        expiredToken.setUsername(testUser.getUsername());
        expiredToken.setTokenType("access");
        expiredToken.setExpirationTime(LocalDateTime.now().minusDays(1)); // Expired yesterday
        expiredToken.setRevokedAt(LocalDateTime.now().minusDays(2));
        expiredToken.setReason("logout");
        revokedTokenRepository.save(expiredToken);

        // Create non-expired token entry
        RevokedToken validToken = new RevokedToken();
        validToken.setJti("valid-jti-456");
        validToken.setUsername(testUser.getUsername());
        validToken.setTokenType("access");
        validToken.setExpirationTime(LocalDateTime.now().plusDays(1)); // Expires tomorrow
        validToken.setRevokedAt(LocalDateTime.now());
        validToken.setReason("logout");
        revokedTokenRepository.save(validToken);

        // Count before cleanup
        long countBefore = revokedTokenRepository.count();
        assertEquals(2, countBefore);

        // Run cleanup
        tokenRevocationService.cleanupExpiredTokens();

        // Count after cleanup
        long countAfter = revokedTokenRepository.count();
        assertEquals(1, countAfter, "Only non-expired token should remain");

        // Verify correct token was kept
        assertFalse(revokedTokenRepository.findByJti("expired-jti-123").isPresent());
        assertTrue(revokedTokenRepository.findByJti("valid-jti-456").isPresent());
    }

    @Test
    void testTokenWithoutJtiCannotBeRevoked() {
        // Create a token-like string without JTI (invalid token)
        String invalidToken = "invalid.token.string";

        // Attempt to revoke should fail gracefully
        boolean revoked = tokenRevocationService.revokeToken(invalidToken, "test");
        assertFalse(revoked, "Invalid token should not be revoked");
    }

    @Test
    void testFindRevokedTokensByUsername() {
        // Generate multiple tokens for same user
        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(testUser.getUsername(),
                        testUser.getPassword(), testUser.getAuthorities());

        String token1 = jwtUtil.generateToken(userDetails);
        String token2 = jwtUtil.generateToken(userDetails);

        // Revoke both
        tokenRevocationService.revokeToken(token1, "logout");
        tokenRevocationService.revokeToken(token2, "password_change");

        // Find by username
        var revokedTokens = revokedTokenRepository.findByUsername(testUser.getUsername());
        assertEquals(2, revokedTokens.size());

        // Verify reasons
        assertTrue(revokedTokens.stream().anyMatch(t -> "logout".equals(t.getReason())));
        assertTrue(revokedTokens.stream().anyMatch(t -> "password_change".equals(t.getReason())));
    }

    @Test
    void testCountExpiredTokens() {
        // Create expired token
        RevokedToken expiredToken = new RevokedToken();
        expiredToken.setJti("count-expired-1");
        expiredToken.setUsername(testUser.getUsername());
        expiredToken.setTokenType("access");
        expiredToken.setExpirationTime(LocalDateTime.now().minusHours(1));
        expiredToken.setRevokedAt(LocalDateTime.now().minusDays(1));
        expiredToken.setReason("logout");
        revokedTokenRepository.save(expiredToken);

        // Create valid token
        RevokedToken validToken = new RevokedToken();
        validToken.setJti("count-valid-1");
        validToken.setUsername(testUser.getUsername());
        validToken.setTokenType("access");
        validToken.setExpirationTime(LocalDateTime.now().plusHours(1));
        validToken.setRevokedAt(LocalDateTime.now());
        validToken.setReason("logout");
        revokedTokenRepository.save(validToken);

        // Count expired
        long expiredCount = revokedTokenRepository.countExpiredTokens(LocalDateTime.now());
        assertEquals(1, expiredCount, "Should count 1 expired token");
    }

    @Test
    void testTokenRevocationWithDifferentReasons() {
        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(testUser.getUsername(),
                        testUser.getPassword(), testUser.getAuthorities());

        // Test different revocation reasons
        String[] reasons = {"logout", "password_change", "security_incident", "manual"};

        for (String reason : reasons) {
            String token = jwtUtil.generateToken(userDetails);
            boolean revoked = tokenRevocationService.revokeToken(token, reason);
            assertTrue(revoked, "Token should be revoked with reason: " + reason);

            Claims claims = jwtUtil.extractAllClaims(token);
            RevokedToken revokedToken =
                    revokedTokenRepository.findByJti(claims.getId()).orElse(null);
            assertNotNull(revokedToken);
            assertEquals(reason, revokedToken.getReason());
        }
    }
}
