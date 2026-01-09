package com.ammons.taskactivity.security;

import com.ammons.taskactivity.service.TokenRevocationService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * JWT Authentication Filter Intercepts requests and validates JWT tokens
 * 
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final TokenRevocationService tokenRevocationService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserDetailsService userDetailsService,
            TokenRevocationService tokenRevocationService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
        this.tokenRevocationService = tokenRevocationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // Get Authorization header
        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        // Extract JWT token from Bearer token
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                logger.error("Failed to extract username from JWT token: " + e.getMessage(), e);
            }
        }

        // Validate token and set authentication
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (Boolean.TRUE.equals(jwtUtil.validateToken(jwt, userDetails))) {
                // SECURITY FIX: Check if token is revoked (logout, password change, etc.)
                try {
                    Claims claims = jwtUtil.extractAllClaims(jwt);
                    String jti = claims.getId();
                    if (jti != null && tokenRevocationService.isTokenRevoked(jti)) {
                        logger.warn("Authentication attempt with revoked token: JTI={}, User={}",
                                jti, username);
                        filterChain.doFilter(request, response);
                        return;
                    }
                } catch (Exception e) {
                    logger.error("Failed to check token revocation status: {}", e.getMessage());
                    filterChain.doFilter(request, response);
                    return;
                }

                // SECURITY FIX: Check account status before granting authentication
                if (!userDetails.isEnabled()) {
                    logger.warn("Authentication attempt with disabled account: {}", username);
                    filterChain.doFilter(request, response);
                    return;
                }
                if (!userDetails.isAccountNonLocked()) {
                    logger.warn("Authentication attempt with locked account: {}", username);
                    filterChain.doFilter(request, response);
                    return;
                }
                if (!userDetails.isAccountNonExpired()) {
                    logger.warn("Authentication attempt with expired account: {}", username);
                    filterChain.doFilter(request, response);
                    return;
                }
                if (!userDetails.isCredentialsNonExpired()) {
                    logger.warn("Authentication attempt with expired credentials: {}", username);
                    filterChain.doFilter(request, response);
                    return;
                }

                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null,
                                userDetails.getAuthorities());
                authenticationToken
                        .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
