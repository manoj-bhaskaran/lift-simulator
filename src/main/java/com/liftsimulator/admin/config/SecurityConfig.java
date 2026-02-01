package com.liftsimulator.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Spring Security configuration for the Lift Simulator application.
 *
 * <p>Configures authentication mechanisms:
 * <ul>
 *   <li><strong>Admin APIs</strong> ({@code /api/**} excluding runtime and simulation-runs):
 *       HTTP Basic authentication with environment-configured username and password.
 *       Role-based access control with ADMIN role.</li>
 *   <li><strong>Runtime APIs</strong> ({@code /api/runtime/**}): API key authentication via
 *       {@code X-API-Key} header for machine-to-machine communication.</li>
 *   <li><strong>Simulation Run APIs</strong> ({@code /api/simulation-runs/**}): API key authentication
 *       via {@code X-API-Key} header for CLI tools and automation.</li>
 * </ul>
 *
 * <p>Public endpoints (no authentication required):
 * <ul>
 *   <li>{@code /api/health} - Application health check</li>
 *   <li>{@code /actuator/**} - Spring Boot Actuator endpoints</li>
 *   <li>Static resources - Frontend assets</li>
 * </ul>
 *
 * <p>Security features:
 * <ul>
 *   <li>Stateless session management (no session cookies)</li>
 *   <li>CSRF disabled (appropriate for stateless REST APIs)</li>
 *   <li>Custom error responses using {@link CustomAuthenticationEntryPoint}</li>
 *   <li>WWW-Authenticate header for HTTP Basic challenges (RFC 7235)</li>
 * </ul>
 *
 * @see CustomAuthenticationEntryPoint
 * @see ApiKeyAuthenticationFilter
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ADMIN_REALM = "Lift Simulator Admin";

    @Value("${security.admin.username:admin}")
    private String adminUsername;

    @Value("${security.admin.password:}")
    private String adminPassword;

    @Value("${security.api-key:}")
    private String apiKey;

    /**
     * Authentication entry point for Admin API endpoints.
     * Includes WWW-Authenticate header for HTTP Basic authentication per RFC 7235.
     */
    @Bean
    public AuthenticationEntryPoint adminAuthenticationEntryPoint() {
        return new CustomAuthenticationEntryPoint(ADMIN_REALM);
    }

    /**
     * Authentication entry point for API key protected endpoints.
     * Does not include WWW-Authenticate header (API key authentication).
     */
    @Bean
    public AuthenticationEntryPoint apiKeyAuthenticationEntryPoint() {
        return new CustomAuthenticationEntryPoint();
    }

    /**
     * Request matcher for API key protected endpoints.
     * Matches runtime configuration and simulation run APIs.
     */
    private RequestMatcher apiKeyProtectedMatcher() {
        return new OrRequestMatcher(
            new AntPathRequestMatcher("/api/runtime/**"),
            new AntPathRequestMatcher("/api/simulation-runs/**")
        );
    }

    /**
     * Security filter chain for API key protected endpoints.
     * Covers runtime configuration and simulation execution APIs.
     * Uses API key authentication via X-API-Key header.
     * Processed first (Order 1) to handle these requests before admin filter chain.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiKeySecurityFilterChain(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint entryPoint = apiKeyAuthenticationEntryPoint();

        http
            .securityMatcher(apiKeyProtectedMatcher())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(entryPoint))
            .addFilterBefore(
                new ApiKeyAuthenticationFilter(apiKey, entryPoint),
                UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated());

        return http.build();
    }

    /**
     * Security filter chain for Admin API endpoints.
     * Uses HTTP Basic authentication with ADMIN role requirement.
     * Includes WWW-Authenticate header in 401 responses per RFC 7235.
     * Processed second (Order 2) after API key filter chain.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain adminApiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(adminAuthenticationEntryPoint()))
            .httpBasic(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/**").hasRole("ADMIN")
                .anyRequest().authenticated());

        return http.build();
    }

    /**
     * Security filter chain for public endpoints and static resources.
     * Permits unauthenticated access to actuator, static files, and frontend routes.
     * Processed last (Order 3) as the default filter chain.
     */
    @Bean
    @Order(3)
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/", "/index.html", "/assets/**", "/favicon.ico").permitAll()
                // Allow frontend SPA routes
                .requestMatchers("/lift-systems/**", "/versions/**", "/health/**",
                    "/validate/**", "/scenarios/**", "/simulator/**", "/runs/**").permitAll()
                .anyRequest().permitAll());

        return http.build();
    }

    /**
     * Password encoder using BCrypt hashing algorithm.
     * Used for encoding and verifying admin passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * In-memory user details service for admin authentication.
     * Creates a single admin user with credentials from configuration.
     *
     * <p>The admin user has the ADMIN role, which is required for all admin API operations.
     *
     * @return UserDetailsService with configured admin user
     */
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
            .username(adminUsername)
            .password(passwordEncoder().encode(adminPassword))
            .roles("ADMIN")
            .build();

        return new InMemoryUserDetailsManager(admin);
    }
}
