package com.ammons.taskactivity.controller;

import com.ammons.taskactivity.dto.LoginRequest;
import com.ammons.taskactivity.dto.LoginResponse;
import com.ammons.taskactivity.dto.RefreshTokenRequest;
import com.ammons.taskactivity.security.JwtUtil;
import com.ammons.taskactivity.service.TokenRevocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

/**
 * REST API Authentication Controller Provides JWT-based authentication endpoints for API access
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "JWT authentication endpoints for API access")
public class ApiAuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final TokenRevocationService tokenRevocationService;

    @Value("${jwt.expiration:86400000}")
    private Long jwtExpiration;

    public ApiAuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil,
                    UserDetailsService userDetailsService,
                    TokenRevocationService tokenRevocationService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.tokenRevocationService = tokenRevocationService;
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and obtain JWT tokens",
            description = "Authenticates user credentials and returns JWT access and refresh tokens for API access. "
                    + "Use the access token in the Authorization header as 'Bearer {token}' for subsequent API requests.",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Authentication successful, JWT tokens returned",
                            content = @Content(
                                    schema = @Schema(implementation = LoginResponse.class))),
                    @ApiResponse(responseCode = "401",
                            description = "Authentication failed - invalid credentials")})
    public ResponseEntity<Object> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Authenticate user
            Authentication authentication =
                    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(), loginRequest.getPassword()));

            // Load user details
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            // Generate tokens
            String accessToken = jwtUtil.generateToken(userDetails);
            String refreshToken = jwtUtil.generateRefreshToken(userDetails);

            // Create response
            LoginResponse response = new LoginResponse(accessToken, refreshToken, jwtExpiration,
                    userDetails.getUsername());

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid username or password");
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token",
            description = "Use a valid refresh token to obtain a new access token without re-authenticating. "
                    + "This is useful when the access token expires but the refresh token is still valid.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                            content = @Content(
                                    schema = @Schema(implementation = LoginResponse.class))),
                    @ApiResponse(responseCode = "401",
                            description = "Invalid or expired refresh token")})
    public ResponseEntity<Object> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            String refreshToken = request.getRefreshToken();
            String username = jwtUtil.extractUsername(refreshToken);

            // SECURITY FIX: Validate that this is actually a refresh token
            if (!Boolean.TRUE.equals(jwtUtil.isRefreshToken(refreshToken))) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                    .body("Invalid token type - refresh token required");
            }

            // Load user details
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            // Validate refresh token
            if (Boolean.TRUE.equals(jwtUtil.validateToken(refreshToken, userDetails))) {
                // Generate new access token
                String newAccessToken = jwtUtil.generateToken(userDetails);

                // Create response with new access token and same refresh token
                LoginResponse response = new LoginResponse(newAccessToken, refreshToken,
                        jwtExpiration, userDetails.getUsername());

                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke token",
                    description = "Revokes the current JWT token by adding it to the revoked tokens blacklist. "
                                    + "The token will no longer be accepted for authentication. "
                                    + "Provide the token in the Authorization header as 'Bearer {token}'.",
                    responses = {@ApiResponse(responseCode = "200",
                                    description = "Token revoked successfully"),
                                    @ApiResponse(responseCode = "401",
                                                    description = "Invalid or missing token")})
    public ResponseEntity<Object> logout(@RequestHeader("Authorization") String authHeader) {
            try {
                    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                            .body("Missing or invalid Authorization header");
                    }

                    String token = authHeader.substring(7);
                    boolean revoked = tokenRevocationService.revokeToken(token, "logout");

                    if (revoked) {
                            return ResponseEntity.ok(
                                            "Token revoked successfully. You have been logged out.");
                    } else {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                            .body("Token could not be revoked");
                    }

            } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body("Failed to revoke token: " + e.getMessage());
            }
    }
}
