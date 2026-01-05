package com.ammons.taskactivity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * JWT Utility class for token generation, validation, and claims extraction
 * 
 * @author Dean Ammons
 * @version 1.0
 */
@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    private static final String INSECURE_DEFAULT =
            "taskactivity-secret-key-change-this-in-production-must-be-at-least-256-bits-long";
    private static final int MINIMUM_KEY_LENGTH = 32; // 256 bits
    private static final String KEY_GENERATION_HELP =
            "Generate a secure key: openssl rand -base64 32";

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 24 hours in milliseconds
    private Long expiration;

    @Value("${jwt.refresh.expiration:604800000}") // 7 days in milliseconds
    private Long refreshExpiration;

    /**
     * Validate JWT secret configuration on application startup Prevents application from starting
     * with insecure or missing JWT secret
     */
    @PostConstruct
    public void validateJwtSecret() {
        if (secret == null || secret.trim().isEmpty()) {
            logger.error("CRITICAL SECURITY ERROR: JWT secret is not configured!");
            logger.error("Set the 'jwt.secret' property or JWT_SECRET environment variable.");
            logger.error(KEY_GENERATION_HELP);
            throw new IllegalStateException("JWT secret is required but not configured. "
                    + "Set jwt.secret property or JWT_SECRET environment variable.");
        }

        if (secret.equals(INSECURE_DEFAULT)) {
            logger.error(
                    "CRITICAL SECURITY ERROR: JWT secret is using the default insecure value!");
            logger.error("This is a serious security vulnerability that allows token forgery.");
            logger.error(KEY_GENERATION_HELP);
            throw new IllegalStateException("JWT secret must not use the default value. "
                    + "Generate a secure random key and set it via jwt.secret property or JWT_SECRET environment variable.");
        }

        if (secret.getBytes(StandardCharsets.UTF_8).length < MINIMUM_KEY_LENGTH) {
            logger.error(
                    "CRITICAL SECURITY ERROR: JWT secret is too short (minimum 256 bits required)!");
            logger.error("Current length: {} bytes, Required: {} bytes",
                    secret.getBytes(StandardCharsets.UTF_8).length, MINIMUM_KEY_LENGTH);
            logger.error(KEY_GENERATION_HELP);
            throw new IllegalStateException(String.format(
                    "JWT secret must be at least %d bytes (256 bits). "
                            + "Current length: %d bytes. Generate a secure random key.",
                    MINIMUM_KEY_LENGTH, secret.getBytes(StandardCharsets.UTF_8).length));
        }

        logger.info("JWT secret validation passed - using secure {} byte key",
                secret.getBytes(StandardCharsets.UTF_8).length);
    }

    /**
     * Extract username from JWT token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract expiration date from JWT token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract a specific claim from JWT token
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract token type from JWT token SECURITY FIX: Added to validate token type (access vs
     * refresh)
     */
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get("token_type", String.class));
    }

    /**
     * Validate if token is a refresh token SECURITY FIX: Added to prevent access tokens from being
     * used as refresh tokens
     */
    public Boolean isRefreshToken(String token) {
        try {
            String tokenType = extractTokenType(token);
            return "refresh".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extract all claims from JWT token SECURITY FIX: Made public to allow TokenRevocationService
     * to extract claims
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Check if token is expired
     */
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Generate JWT token for user
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles",
                userDetails.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
        // SECURITY FIX: Add token_type claim to distinguish access tokens
        claims.put("token_type", "access");
        return createToken(claims, userDetails.getUsername(), expiration);
    }

    /**
     * Generate refresh token for user
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // SECURITY FIX: Add token_type claim to distinguish refresh tokens
        claims.put("token_type", "refresh");
        return createToken(claims, userDetails.getUsername(), refreshExpiration);
    }

    /**
     * Create JWT token with claims and subject SECURITY FIX: Added JTI (JWT ID) claim for token
     * revocation support
     */
    private String createToken(Map<String, Object> claims, String subject, Long expirationTime) {
        return Jwts.builder().claims(claims).subject(subject).id(UUID.randomUUID().toString()) // SECURITY
                                                                                               // FIX:
                                                                                               // Add
                                                                                               // unique
                                                                                               // JTI
                                                                                               // for
                                                                                               // revocation
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey()).compact();
    }

    /**
     * Validate JWT token
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * Get signing key for JWT
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
