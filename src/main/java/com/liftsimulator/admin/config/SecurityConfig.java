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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the Lift Simulator application.
 *
 * <p>Configures two authentication mechanisms:
 * <ul>
 *   <li><strong>Admin APIs</strong> ({@code /api/**} excluding runtime): HTTP Basic authentication
 *       with environment-configured username and password. Role-based access control with ADMIN role.</li>
 *   <li><strong>Runtime APIs</strong> ({@code /api/runtime/**}): API key authentication via
 *       {@code X-API-Key} header for machine-to-machine communication.</li>
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
 * </ul>
 *
 * @see CustomAuthenticationEntryPoint
 * @see ApiKeyAuthenticationFilter
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Value("${security.admin.username:admin}")
    private String adminUsername;

    @Value("${security.admin.password:}")
    private String adminPassword;

    @Value("${security.api-key:}")
    private String apiKey;

    private final CustomAuthenticationEntryPoint authenticationEntryPoint;

    public SecurityConfig(CustomAuthenticationEntryPoint authenticationEntryPoint) {
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    /**
     * Security filter chain for Runtime API endpoints.
     * Uses API key authentication via X-API-Key header.
     * Processed first (Order 1) to handle runtime requests before admin filter chain.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain runtimeApiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/runtime/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint))
            .addFilterBefore(
                new ApiKeyAuthenticationFilter(apiKey, authenticationEntryPoint),
                UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated());

        return http.build();
    }

    /**
     * Security filter chain for Admin API endpoints.
     * Uses HTTP Basic authentication with ADMIN role requirement.
     * Processed second (Order 2) after runtime filter chain.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain adminApiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/**")
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint))
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
