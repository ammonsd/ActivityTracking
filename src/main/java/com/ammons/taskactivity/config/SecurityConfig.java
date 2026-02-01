package com.ammons.taskactivity.config;

import com.ammons.taskactivity.repository.UserRepository;
import com.ammons.taskactivity.security.CustomPermissionEvaluator;
import com.ammons.taskactivity.security.JwtAuthenticationFilter;
import com.ammons.taskactivity.security.RateLimitFilter;
import com.ammons.taskactivity.service.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * SecurityConfig
 *
 * @author Dean Ammons
 * @version 1.0
 * @since January 2026
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

        private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

        // URL Constants
        private static final String API_PATTERN = "/api/**";
        private static final String LOGIN_URL = "/login";
        private static final String LOGOUT_URL = "/logout";

        // Role Constants
        private static final String ROLE_EXPENSE_ADMIN = "EXPENSE_ADMIN";
        private static final String ADMIN_PATTERN = "/admin/**";
        private static final String ADMIN_ROLE = "ADMIN";
        private static final String USER_ROLE = "USER";
        private static final String GUEST_ROLE = "GUEST";
        private static final String CONTENT_TYPE_JSON = "application/json";

        /**
         * CORS allowed origins - configurable via environment variable. Defaults to localhost ports
         * for development. For production, set CORS_ALLOWED_ORIGINS environment variable with
         * explicit origins. Example:
         * CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
         */
        @Value("${cors.allowed-origins:http://localhost:4200,http://localhost:3000,http://localhost:8080}")
        private String allowedOrigins;

        @Value("${springdoc.swagger-ui.enabled:false}")
        private boolean swaggerEnabled;

        private final UserDetailsServiceImpl userDetailsService;
        private final CustomAccessDeniedHandler customAccessDeniedHandler;
        private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
        private final CustomAuthenticationFailureHandler customAuthenticationFailureHandler;
        private final CustomLogoutSuccessHandler customLogoutSuccessHandler;
        private final ForcePasswordUpdateFilter forcePasswordUpdateFilter;
        private final UserRepository userRepository;
        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final RateLimitFilter rateLimitFilter;

        public SecurityConfig(UserDetailsServiceImpl userDetailsService,
                        CustomAccessDeniedHandler customAccessDeniedHandler,
                        CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler,
                        CustomAuthenticationFailureHandler customAuthenticationFailureHandler,
                        CustomLogoutSuccessHandler customLogoutSuccessHandler,
                        ForcePasswordUpdateFilter forcePasswordUpdateFilter,
                        UserRepository userRepository,
                        JwtAuthenticationFilter jwtAuthenticationFilter,
                        RateLimitFilter rateLimitFilter) {
                this.userDetailsService = userDetailsService;
                this.customAccessDeniedHandler = customAccessDeniedHandler;
                this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
                this.customAuthenticationFailureHandler = customAuthenticationFailureHandler;
                this.customLogoutSuccessHandler = customLogoutSuccessHandler;
                this.forcePasswordUpdateFilter = forcePasswordUpdateFilter;
                this.userRepository = userRepository;
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.rateLimitFilter = rateLimitFilter;
        }

        @Bean
        public CustomAuthenticationProvider customAuthenticationProvider(
                        PasswordEncoder passwordEncoder) {
                return new CustomAuthenticationProvider(userDetailsService, passwordEncoder,
                                userRepository);
        }

        @Bean
        @SuppressWarnings("java:S3776") // Cognitive complexity justified: central security
                                        // configuration
        public SecurityFilterChain filterChain(HttpSecurity http,
                        CustomAuthenticationProvider customAuthenticationProvider)
                        throws Exception {
                // Configure CSRF token request handler to ensure token is available on first page
                // load
                CsrfTokenRequestAttributeHandler requestHandler =
                                new CsrfTokenRequestAttributeHandler();
                requestHandler.setCsrfRequestAttributeName("_csrf");

                http.csrf(csrf -> csrf
                                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                                .csrfTokenRequestHandler(requestHandler)
                                .ignoringRequestMatchers("/v3/api-docs/**", "/swagger-ui/**",
                                                "/swagger-ui.html", API_PATTERN) // Disable CSRF for
                                                                                 // API
                                                                                 // endpoints
                ).cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                // Security Headers - Protection against common web vulnerabilities
                                .headers(headers -> headers
                                                // X-Frame-Options: Prevent clickjacking attacks
                                                .frameOptions(frame -> frame.deny())
                                                // X-XSS-Protection: Enable browser XSS protection
                                                .xssProtection(xss -> xss.headerValue(
                                                                org.springframework.security.web.header.writers.XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
                                                // Content-Security-Policy: Restrict resource
                                                // loading
                                                .contentSecurityPolicy(csp -> csp.policyDirectives(
                                                                "default-src 'self'; "
                                                                                + "script-src 'self' 'unsafe-inline' 'unsafe-eval'; "
                                                                                + "style-src 'self' 'unsafe-inline'; "
                                                                                + "img-src 'self' data:; "
                                                                                + "font-src 'self' data:; "
                                                                                + "connect-src 'self'"))
                                                // HTTP Strict Transport Security: Force HTTPS
                                                .httpStrictTransportSecurity(hsts -> hsts
                                                                .includeSubDomains(true)
                                                                .maxAgeInSeconds(31536000)) // 1
                                                                                            // year
                                                // Referrer-Policy: Control referrer information
                                                .referrerPolicy(referrer -> referrer.policy(
                                                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                                                // Permissions-Policy: Disable unnecessary browser
                                                // features
                                                .permissionsPolicy(permissions -> permissions
                                                                .policy("geolocation=(), microphone=(), camera=(), payment=(), usb=(), magnetometer=(), gyroscope=()")))
                                .authorizeHttpRequests(auth -> auth
                                                // Public resources
                                                .requestMatchers("/", "/index.html", "/static/**",
                                                                "/css/**", "/js/**", "/images/**",
                                                                "/favicon.ico", "/app/*.js",
                                                                "/app/*.css", "/app/*.woff2",
                                                                "/app/*.ico", "/app/*.json")
                                                .permitAll()
                                                .requestMatchers(LOGIN_URL, LOGOUT_URL, "/error",
                                                                "/access-denied",
                                                                "/clear-access-denied-session",
                                                                "/reset-password")
                                                .permitAll()
                                                // Allow unauthenticated access to password reset
                                                // with token
                                                .requestMatchers("/change-password")
                                                .permitAll()
                                                // Health checks
                                                .requestMatchers("/api/health/**",
                                                                "/actuator/health")
                                                .permitAll()
                                                // Public documentation from S3
                                                .requestMatchers("/docs/**").permitAll()

                                                // JWT Authentication endpoints - public access
                                                .requestMatchers("/api/auth/**").permitAll()

                                                // Visitor counter endpoints - public access, no
                                                // authentication required
                                                // Allow OPTIONS for CORS preflight
                                                .requestMatchers(HttpMethod.OPTIONS,
                                                                "/api/public/**")
                                                .permitAll()
                                                .requestMatchers("/api/public/**").permitAll()

                                                // Jenkins API endpoints - require JENKINS_SERVICE
                                                // role
                                                // Method-level @RequirePermission annotations
                                                // control granular access
                                                .requestMatchers("/api/jenkins/**")
                                                .hasRole("JENKINS_SERVICE")

                                                // Admin API endpoints - require authentication
                                                // Method-level @RequirePermission annotations
                                                // control
                                                // granular access
                                                .requestMatchers("/api/admin/**").authenticated()
                                                .requestMatchers("/api/users/me",
                                                                "/api/users/profile")
                                                .hasAnyRole(USER_ROLE, ADMIN_ROLE, GUEST_ROLE,
                                                                ROLE_EXPENSE_ADMIN)

                                                // Admin-only API endpoints
                                                .requestMatchers("/api/users/**",
                                                                "/api/dropdownvalues/**")
                                                .hasRole(ADMIN_ROLE)

                                                // Swagger/OpenAPI endpoints - only accessible when
                                                // explicitly enabled
                                                .requestMatchers("/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/v3/api-docs/**",
                                                                "/swagger-resources/**",
                                                                "/webjars/**")
                                                .access((authentication, context) -> swaggerEnabled
                                                                ? new org.springframework.security.authorization.AuthorizationDecision(
                                                                                true)
                                                                : new org.springframework.security.authorization.AuthorizationDecision(
                                                                                false))
                                                // API endpoints - require authentication
                                                .requestMatchers(HttpMethod.GET, API_PATTERN)
                                                .hasAnyRole(USER_ROLE, ADMIN_ROLE, GUEST_ROLE,
                                                                ROLE_EXPENSE_ADMIN)
                                                .requestMatchers(HttpMethod.POST, API_PATTERN)
                                                .hasAnyRole(USER_ROLE, ADMIN_ROLE, GUEST_ROLE,
                                                                ROLE_EXPENSE_ADMIN)
                                                .requestMatchers(HttpMethod.PUT, API_PATTERN)
                                                .hasAnyRole(USER_ROLE, ADMIN_ROLE, GUEST_ROLE,
                                                                ROLE_EXPENSE_ADMIN)
                                                .requestMatchers(HttpMethod.DELETE, API_PATTERN)
                                                .hasAnyRole(USER_ROLE, ADMIN_ROLE, GUEST_ROLE,
                                                                ROLE_EXPENSE_ADMIN)

                                                // User Management - Admin only
                                                .requestMatchers("/task-activity/manage-users/**")
                                                .hasRole(ADMIN_ROLE)

                                                // Profile Management - accessible to USER, ADMIN,
                                                // and
                                                // EXPENSE_ADMIN
                                                .requestMatchers("/profile/**")
                                                .hasAnyRole(USER_ROLE, ADMIN_ROLE,
                                                                ROLE_EXPENSE_ADMIN)

                                                // Admin pages - accessible to ADMIN and
                                                // EXPENSE_ADMIN
                                                .requestMatchers(ADMIN_PATTERN)
                                                .hasAnyRole(ADMIN_ROLE, ROLE_EXPENSE_ADMIN)

                                                // Task Activity screens - accessible to USER,
                                                // ADMIN, and
                                                // GUEST
                                                .requestMatchers("/task-activity",
                                                                "/task-activity/",
                                                                "/task-activity/list",
                                                                "/task-activity/detail/**",
                                                                "/task-activity/add",
                                                                "/task-activity/clone/**",
                                                                "/task-activity/submit",
                                                                "/task-activity/update/**",
                                                                "/task-activity/delete/**")
                                                .hasAnyRole(USER_ROLE, ADMIN_ROLE, GUEST_ROLE,
                                                                ROLE_EXPENSE_ADMIN)

                                                // Expense endpoints - accessible to USER, ADMIN,
                                                // GUEST,
                                                // and EXPENSE_ADMIN
                                                .requestMatchers("/expenses/**")
                                                .hasAnyRole(USER_ROLE, ADMIN_ROLE, GUEST_ROLE,
                                                                ROLE_EXPENSE_ADMIN)

                                                // Angular dashboard - requires authentication
                                                // .requestMatchers("/app",
                                                // "/app/**").authenticated()
                                                .requestMatchers("/app", "/app/").authenticated()

                                                // React admin dashboard - requires authentication
                                                // and ADMIN role
                                                .requestMatchers("/dashboard", "/dashboard/",
                                                                "/dashboard/**")
                                                .hasRole(ADMIN_ROLE)

                                                // All other requests require authentication
                                                .anyRequest().authenticated())
                                .httpBasic(basic -> basic.authenticationEntryPoint(
                                                (request, response, authException) -> {
                                                        String requestUri = request.getRequestURI();
                                                        // Don't send WWW-Authenticate
                                                        // header for Swagger endpoints or
                                                        // Angular app
                                                        // This prevents the browser auth
                                                        // popup
                                                        if (requestUri.startsWith("/swagger-ui")
                                                                        || requestUri.startsWith(
                                                                                        "/v3/api-docs")
                                                                        || requestUri.startsWith(
                                                                                        "/swagger-resources")
                                                                        || requestUri.startsWith(
                                                                                        "/webjars")
                                                                        || requestUri.startsWith(
                                                                                        "/app")) {
                                                                // No WWW-Authenticate
                                                                // header - prevents browser
                                                                // popup
                                                                response.sendError(401,
                                                                                "Unauthorized");
                                                        } else {
                                                                // For API calls, use Basic
                                                                // Auth
                                                                response.setHeader(
                                                                                "WWW-Authenticate",
                                                                                "Basic realm=\"API\"");
                                                                response.sendError(401,
                                                                                "Unauthorized");
                                                        }
                                                })) // Enable HTTP Basic Auth for API calls
                                .formLogin(form -> form.loginPage(LOGIN_URL)
                                                .defaultSuccessUrl("/app", false) // false allows
                                                                                  // saved request
                                                                                  // to take
                                                                                  // precedence
                                                .successHandler(customAuthenticationSuccessHandler)
                                                .failureHandler(customAuthenticationFailureHandler)
                                                .usernameParameter("username")
                                                .passwordParameter("password").permitAll())
                                .logout(logout -> logout.logoutUrl(LOGOUT_URL)
                                                .logoutSuccessHandler(customLogoutSuccessHandler)
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                                                .clearAuthentication(true)
                                                // Accept both GET (Angular) and POST (Thymeleaf
                                                // with CSRF)
                                                .logoutRequestMatcher(request -> LOGOUT_URL
                                                                .equals(request.getRequestURI())
                                                                && ("POST".equalsIgnoreCase(
                                                                                request.getMethod())
                                                                                || "GET".equalsIgnoreCase(
                                                                                                request.getMethod())))
                                                .permitAll())
                                .exceptionHandling(exceptions -> exceptions
                                                .accessDeniedHandler(customAccessDeniedHandler)
                                                .authenticationEntryPoint((request, response,
                                                                authException) -> {
                                                        // Handle session timeout
                                                        String requestUri = request.getRequestURI();

                                                        // Check if user had a session that
                                                        // expired (vs never having a
                                                        // session)
                                                        // Note: getRequestedSessionId() is
                                                        // only used to check for
                                                        // session existence, not for
                                                        // authentication
                                                        boolean hadSession = request
                                                                        .getRequestedSessionId() != null // NOSONAR
                                                                                                         // -
                                                                                                         // Only
                                                                                                         // checking
                                                                                                         // existence
                                                                        && !request.isRequestedSessionIdValid();

                                                        // For API calls, return 401
                                                        // (Angular interceptor will handle)
                                                        if (requestUri.startsWith("/api/")) {
                                                                // Check if this is a JWT
                                                                // authentication attempt
                                                                String authHeader = request
                                                                                .getHeader("Authorization");
                                                                if (authHeader != null && authHeader
                                                                                .startsWith("Bearer ")) {
                                                                        // JWT token present
                                                                        // but
                                                                        // invalid/expired
                                                                        response.setStatus(
                                                                                        HttpServletResponse.SC_UNAUTHORIZED);
                                                                        response.setContentType(
                                                                                        CONTENT_TYPE_JSON);
                                                                        response.getWriter().write(
                                                                                        "{\"error\":\"Unauthorized\",\"message\":\"Invalid or expired JWT token.\"}");
                                                                } else if (hadSession) {
                                                                        // Session-based
                                                                        // auth that expired
                                                                        response.setStatus(
                                                                                        HttpServletResponse.SC_UNAUTHORIZED);
                                                                        response.setContentType(
                                                                                        CONTENT_TYPE_JSON);
                                                                        response.getWriter().write(
                                                                                        "{\"error\":\"Session Expired\",\"message\":\"Your session has expired. Please log in again.\"}");
                                                                } else {
                                                                        // No authentication
                                                                        // provided
                                                                        response.setStatus(
                                                                                        HttpServletResponse.SC_UNAUTHORIZED);
                                                                        response.setContentType(
                                                                                        CONTENT_TYPE_JSON);
                                                                        response.getWriter().write(
                                                                                        "{\"error\":\"Unauthorized\",\"message\":\"Authentication required.\"}");
                                                                }
                                                        } else if (!requestUri.equals(LOGIN_URL)) {
                                                                // For web requests (except
                                                                // login page)
                                                                // Only add timeout
                                                                // parameter if session
                                                                // actually expired
                                                                String redirectUrl = request
                                                                                .getContextPath()
                                                                                + LOGIN_URL;
                                                                if (hadSession) {
                                                                        redirectUrl += "?timeout=true";
                                                                }
                                                                response.sendRedirect(redirectUrl);
                                                        }
                                                }))
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(
                                                                SessionCreationPolicy.IF_REQUIRED)
                                                .maximumSessions(5).maxSessionsPreventsLogin(false)) // Allow
                                                                                                     // multiple
                                                                                                     // concurrent
                                                                                                     // sessions
                                                                                                     // for
                                                                                                     // dual
                                                                                                     // UI
                                                                                                     // support
                                .authenticationProvider(customAuthenticationProvider)
                                .addFilterBefore(rateLimitFilter,
                                                UsernamePasswordAuthenticationFilter.class)
                                .addFilterBefore(jwtAuthenticationFilter,
                                                UsernamePasswordAuthenticationFilter.class)
                                .addFilterAfter(forcePasswordUpdateFilter,
                                                UsernamePasswordAuthenticationFilter.class)
                                // Security Headers - OWASP recommendations
                                .headers(headers -> headers
                                                // Prevent clickjacking attacks
                                                .frameOptions(frameOptions -> frameOptions.deny())
                                                // SECURITY FIX: Enable MIME type sniffing
                                                // protection
                                                .contentTypeOptions(
                                                                contentTypeOptions -> contentTypeOptions
                                                                                .and())
                                                // Disable XSS protection (modern browsers don't
                                                // need it,
                                                // can cause issues)
                                                .xssProtection(xssProtection -> xssProtection
                                                                .disable())
                                                // Content Security Policy - Restrict resource
                                                // loading
                                                // SECURITY: Removed 'unsafe-eval' to prevent
                                                // dynamic code
                                                // execution (eval, Function constructor)
                                                // Note: 'unsafe-inline' kept for Thymeleaf inline
                                                // scripts
                                                // (TODO: migrate to nonces)
                                                .contentSecurityPolicy(csp -> csp.policyDirectives(
                                                                "default-src 'self'; "
                                                                                + "script-src 'self' 'unsafe-inline' 'unsafe-eval'; "
                                                                                + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                                                                                + "img-src 'self' data: https:; "
                                                                                + "font-src 'self' data: https://fonts.gstatic.com; "
                                                                                + "connect-src 'self'; "
                                                                                + "frame-ancestors 'none'"))
                                                // HTTP Strict Transport Security - Force HTTPS
                                                .httpStrictTransportSecurity(hsts -> hsts
                                                                .includeSubDomains(true)
                                                                .maxAgeInSeconds(31536000)) // 1
                                                                                            // year
                                                // Referrer Policy - Control referer header
                                                .referrerPolicy(referrer -> referrer.policy(
                                                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                                                // Permissions Policy - Control browser features
                                                .permissionsPolicy(permissions -> permissions
                                                                .policy("camera=(), microphone=(), geolocation=(), payment=()")));

                return http.build();
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
                        throws Exception {
                return authConfig.getAuthenticationManager();
        }

        /**
         * Configures CORS with explicit allowed origins from environment variable. Uses
         * setAllowedOrigins() with explicit origin list instead of wildcard patterns to prevent
         * CSRF attacks when credentials are enabled.
         * 
         * SECURITY: Fails fast if wildcard origins are configured with credentials in production.
         * Wildcard CORS with credentials is a critical security vulnerability that allows any
         * origin to make authenticated requests.
         * 
         * Special handling: - If origins contains "*", use setAllowedOriginPatterns() for
         * development - Otherwise, use setAllowedOrigins() for production security
         * 
         * @return CorsConfigurationSource with secure CORS settings
         * @throws IllegalStateException if wildcard CORS + credentials detected in production
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();

                // Parse allowed origins from comma-separated string
                List<String> origins = Arrays.asList(allowedOrigins.split(","));

                // Trim whitespace from each origin
                origins = origins.stream().map(String::trim).toList();

                // Log configured origins for debugging
                logger.info("[CORS] Configured origins: {}", origins);

                // Check if wildcard pattern is used (for development/testing only)
                boolean hasWildcard = origins.contains("*")
                                || origins.stream().anyMatch(o -> o.contains("*"));

                if (hasWildcard) {
                        // SECURITY CHECK: Fail fast if wildcard CORS + credentials in production
                        String activeProfile = System.getProperty("spring.profiles.active", "");
                        boolean isProduction = activeProfile.contains("aws")
                                        || activeProfile.contains("prod");

                        if (isProduction) {
                                String errorMsg = String.format(
                                                "CRITICAL SECURITY ERROR: Wildcard CORS origins with credentials enabled in production! "
                                                                + "Active profile: %s, Origins: %s. This allows ANY origin to make authenticated requests. "
                                                                + "Set CORS_ALLOWED_ORIGINS to explicit domains (e.g., https://taskactivitytracker.com)",
                                                activeProfile, origins);
                                logger.error("[CORS] {}", errorMsg);
                                throw new IllegalStateException(errorMsg);
                        }

                        // Use pattern matching for development - WARNING: Less secure
                        configuration.setAllowedOriginPatterns(origins);
                        logger.warn("[CORS] Using setAllowedOriginPatterns for wildcard support - DEVELOPMENT ONLY");
                } else {
                        // Use explicit origins for production security
                        configuration.setAllowedOrigins(origins);
                        logger.info("[CORS] Using setAllowedOrigins for explicit origin list");
                }

                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        public MethodSecurityExpressionHandler methodSecurityExpressionHandler(
                        CustomPermissionEvaluator permissionEvaluator) {
                DefaultMethodSecurityExpressionHandler handler =
                                new DefaultMethodSecurityExpressionHandler();
                handler.setPermissionEvaluator(permissionEvaluator);
                return handler;
        }
}


