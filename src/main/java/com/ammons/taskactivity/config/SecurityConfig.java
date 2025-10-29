package com.ammons.taskactivity.config;

import com.ammons.taskactivity.service.UserDetailsServiceImpl;
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
    private final ForcePasswordUpdateFilter forcePasswordUpdateFilter;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService,
                    CustomAccessDeniedHandler customAccessDeniedHandler,
                    CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler,
            CustomAuthenticationFailureHandler customAuthenticationFailureHandler,
                    ForcePasswordUpdateFilter forcePasswordUpdateFilter) {
        this.userDetailsService = userDetailsService;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
        this.customAuthenticationFailureHandler = customAuthenticationFailureHandler;
        this.forcePasswordUpdateFilter = forcePasswordUpdateFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/v3/api-docs/**", "/swagger-ui/**",
                                        "/swagger-ui.html")
        ).cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth
                        // Public resources
                        .requestMatchers("/", "/index.html", "/static/**", "/css/**", "/js/**",
                                                        "/images/**", "/favicon.ico")
                                        .permitAll()
                                        .requestMatchers(LOGIN_URL, LOGOUT_URL, "/error",
                                                        "/access-denied",
                                                        "/clear-access-denied-session")
                                        .permitAll() // Health checks
                        .requestMatchers("/api/health/**", "/actuator/health").permitAll()

                                        // Swagger/OpenAPI endpoints - public access for
                                        // documentation
                                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                                        "/v3/api-docs/**", "/swagger-resources/**",
                                                        "/webjars/**")
                                        .permitAll()

                        // API endpoints - require authentication
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

                        // All other requests require authentication
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage(LOGIN_URL)
                        .defaultSuccessUrl("/task-activity/list", true)
                                        .successHandler(customAuthenticationSuccessHandler)
                        .failureHandler(customAuthenticationFailureHandler)
                        .usernameParameter("username")
                        .passwordParameter("password").permitAll())
                .logout(logout -> logout.logoutUrl(LOGOUT_URL)
                        .logoutSuccessUrl(LOGIN_URL + "?logout=true").invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID").clearAuthentication(true).permitAll())
                        .exceptionHandling(exceptions -> exceptions
                                        .accessDeniedHandler(customAccessDeniedHandler))
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                                .maximumSessions(1).maxSessionsPreventsLogin(false))
                        .userDetailsService(userDetailsService)
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
