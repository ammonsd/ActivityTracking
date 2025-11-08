package com.ammons.taskactivity.config;

import com.ammons.taskactivity.service.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.ammons.taskactivity.repository.UserRepository;

import java.util.List;

/**
 * SecurityConfig
 *
 * @author Dean Ammons
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    // URL Constants
    private static final String API_PATTERN = "/api/**";
    private static final String LOGIN_URL = "/login";
    private static final String LOGOUT_URL = "/logout";
    private static final String ADMIN_PATTERN = "/admin/**";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String USER_ROLE = "USER";
    private static final String GUEST_ROLE = "GUEST";

    private final UserDetailsServiceImpl userDetailsService;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;
    private final CustomAuthenticationFailureHandler customAuthenticationFailureHandler;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;
    private final ForcePasswordUpdateFilter forcePasswordUpdateFilter;
    private final UserRepository userRepository;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService,
                    CustomAccessDeniedHandler customAccessDeniedHandler,
                    CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler,
            CustomAuthenticationFailureHandler customAuthenticationFailureHandler,
            CustomLogoutSuccessHandler customLogoutSuccessHandler,
                    ForcePasswordUpdateFilter forcePasswordUpdateFilter,
                    UserRepository userRepository) {
        this.userDetailsService = userDetailsService;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
        this.customAuthenticationFailureHandler = customAuthenticationFailureHandler;
        this.customLogoutSuccessHandler = customLogoutSuccessHandler;
        this.forcePasswordUpdateFilter = forcePasswordUpdateFilter;
        this.userRepository = userRepository;
    }

    @Bean
    public CustomAuthenticationProvider customAuthenticationProvider(
                    PasswordEncoder passwordEncoder) {
            return new CustomAuthenticationProvider(userDetailsService, passwordEncoder,
                            userRepository);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                    CustomAuthenticationProvider customAuthenticationProvider) throws Exception {
        http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/v3/api-docs/**", "/swagger-ui/**",
                        "/swagger-ui.html", API_PATTERN) // Disable CSRF for API
                                                         // endpoints
        ).cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // Public resources
                        .requestMatchers("/", "/index.html", "/static/**", "/css/**", "/js/**",
                                                        "/images/**", "/favicon.ico", "/app/*.js",
                                                        "/app/*.css", "/app/*.woff2", "/app/*.ico",
                                                        "/app/*.json")
                                        .permitAll()
                                        .requestMatchers(LOGIN_URL, LOGOUT_URL, "/error",
                                                        "/access-denied",
                                                        "/clear-access-denied-session")
                        .permitAll()
                        // Health checks
                        .requestMatchers("/api/health/**", "/actuator/health").permitAll()

                                        // Swagger/OpenAPI endpoints - public access for
                                        // documentation
                                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                                        "/v3/api-docs/**", "/swagger-resources/**",
                                                        "/webjars/**")
                        .permitAll() // API endpoints - require authentication
                                        .requestMatchers(HttpMethod.GET, API_PATTERN)
                                        .hasAnyRole(USER_ROLE, ADMIN_ROLE, GUEST_ROLE)
                                        .requestMatchers(HttpMethod.POST, API_PATTERN)
                                        .hasAnyRole(USER_ROLE, ADMIN_ROLE, GUEST_ROLE)
                                        .requestMatchers(HttpMethod.PUT, API_PATTERN)
                                        .hasAnyRole(USER_ROLE, ADMIN_ROLE, GUEST_ROLE)
                                        .requestMatchers(HttpMethod.DELETE, API_PATTERN)
                                        .hasAnyRole(USER_ROLE, ADMIN_ROLE, GUEST_ROLE)

                                        // User Management - Admin only
                                        .requestMatchers("/task-activity/manage-users/**")
                        .hasRole(ADMIN_ROLE)

                        // Admin pages
                        .requestMatchers(ADMIN_PATTERN).hasRole(ADMIN_ROLE)

                                        // Task Activity screens - accessible to USER, ADMIN, and
                                        // GUEST
                                        .requestMatchers("/task-activity", "/task-activity/",
                                                        "/task-activity/list",
                                                        "/task-activity/detail/**",
                                                        "/task-activity/add",
                                                        "/task-activity/clone/**",
                                                        "/task-activity/submit",
                                                        "/task-activity/update/**",
                                                        "/task-activity/delete/**")
                                        .hasAnyRole(USER_ROLE, ADMIN_ROLE, GUEST_ROLE)

                        // Angular dashboard - requires authentication
                                        // .requestMatchers("/app", "/app/**").authenticated()
                                        .requestMatchers("/app", "/app/").authenticated()

                        // All other requests require authentication
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic
                                        .authenticationEntryPoint(
                                                        (request, response, authException) -> {
                                                                String requestUri = request
                                                                                .getRequestURI();
                                                                // Don't send WWW-Authenticate
                                                                // header for Swagger endpoints or
                                                                // Angular app
                                                                // This prevents the browser auth
                                                                // popup
                                                                if (requestUri.startsWith(
                                                                                "/swagger-ui")
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
                        .defaultSuccessUrl("/app", false) // false allows saved request to take
                                                          // precedence
                                        .successHandler(customAuthenticationSuccessHandler)
                        .failureHandler(customAuthenticationFailureHandler)
                        .usernameParameter("username")
                        .passwordParameter("password").permitAll())
                .logout(logout -> logout.logoutUrl(LOGOUT_URL)
                        .logoutSuccessHandler(customLogoutSuccessHandler)
                        .invalidateHttpSession(true).deleteCookies("JSESSIONID", "XSRF-TOKEN")
                        .clearAuthentication(true)
                        .logoutRequestMatcher(
                                new org.springframework.security.web.util.matcher.OrRequestMatcher(
                                        new org.springframework.security.web.util.matcher.AntPathRequestMatcher(
                                                LOGOUT_URL, "POST"),
                                        new org.springframework.security.web.util.matcher.AntPathRequestMatcher(
                                                LOGOUT_URL, "GET")))
                        .permitAll())
                        .exceptionHandling(exceptions -> exceptions
                                        .accessDeniedHandler(customAccessDeniedHandler)
                                        .authenticationEntryPoint(
                                                        (request, response, authException) -> {
                                                                // Handle session timeout
                                                                String requestUri = request
                                                                                .getRequestURI();

                                                                // Check if user had a session that
                                                                // expired
                                                                // (vs never having a session)
                                                                boolean hadSession = request
                                                                                .getRequestedSessionId() != null
                                                                                && !request.isRequestedSessionIdValid();

                                                                // For API calls, return 401
                                                                // (Angular interceptor will handle)
                                                                if (requestUri.startsWith(
                                                                                "/api/")) {
                                                                        response.setStatus(
                                                                                        HttpServletResponse.SC_UNAUTHORIZED);
                                                                        response.setContentType(
                                                                                        "application/json");
                                                                        response.getWriter().write(
                                                                                        "{\"error\":\"Session Expired\",\"message\":\"Your session has expired. Please log in again.\"}");
                                                                } else if (!requestUri.equals(
                                                                                LOGIN_URL)) {
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
                                                                        response.sendRedirect(
                                                                                        redirectUrl);
                                                                }
                                                        }))
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                                .maximumSessions(5).maxSessionsPreventsLogin(false)) // Allow
                                                                                     // multiple
                                                                                     // concurrent
                                                                                     // sessions
                                                                                     // for
                                                                                     // dual UI
                                                                                     // support
                        .authenticationProvider(customAuthenticationProvider)
                        .addFilterAfter(forcePasswordUpdateFilter,
                                        UsernamePasswordAuthenticationFilter.class);

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

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
