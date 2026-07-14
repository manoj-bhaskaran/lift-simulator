package com.liftsimulator.admin.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Security configuration for the Admin HTTP Basic and Actuator filter chains.
 *
 * <p>Admin APIs ({@code /api/v1/**}) use HTTP Basic authentication with
 * role-based access control:
 * <ul>
 *   <li>GET requests: allowed for ADMIN and VIEWER roles</li>
 *   <li>POST, PUT, DELETE, PATCH requests: restricted to ADMIN role only</li>
 * </ul>
 *
 * <p>Actuator endpoints ({@code /actuator/**}) require ADMIN-role HTTP Basic
 * authentication before exposing operational state.
 *
 * <p>Both chains include a WWW-Authenticate header in 401 responses per RFC 7235.
 *
 * @see SecurityConfig
 * @see SecurityUsersProperties
 * @see SecurityProperties
 */
@Configuration
public class AdminSecurityConfig {

    private final SecurityUsersProperties securityUsersProperties;
    private final SecurityProperties securityProperties;
    private final AuthenticationEntryPoint adminAuthenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;
    private final PasswordEncoder passwordEncoder;
    private final CsrfProperties csrfProperties;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singleton beans injected via constructor. "
                    + "These collaborators are not modified externally and defensive copying "
                    + "would break Spring's dependency injection pattern."
    )
    public AdminSecurityConfig(SecurityUsersProperties securityUsersProperties,
                               SecurityProperties securityProperties,
                               @Qualifier("adminAuthenticationEntryPoint")
                               AuthenticationEntryPoint adminAuthenticationEntryPoint,
                               AccessDeniedHandler accessDeniedHandler,
                               PasswordEncoder passwordEncoder,
                               CsrfProperties csrfProperties) {
        this.securityUsersProperties = securityUsersProperties;
        this.securityProperties = securityProperties;
        this.adminAuthenticationEntryPoint = adminAuthenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.passwordEncoder = passwordEncoder;
        this.csrfProperties = csrfProperties;
    }

    /**
     * Security filter chain for Admin API endpoints.
     * Uses HTTP Basic authentication with role-based access control.
     *
     * <p>Authorization rules:
     * <ul>
     *   <li>GET requests: Allowed for ADMIN and VIEWER roles</li>
     *   <li>POST, PUT, DELETE, PATCH requests: Restricted to ADMIN role only</li>
     * </ul>
     *
     * <p>Includes WWW-Authenticate header in 401 responses per RFC 7235.
     * Processed after the API-key (Order 1) and OpenAPI (Order 2) chains.
     */
    @Bean
    @Order(3)
    public SecurityFilterChain adminApiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/v1/**")
            .cors(Customizer.withDefaults())
            .csrf(csrf -> SecurityCsrfSupport.configure(csrf, csrfProperties))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(adminAuthenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .httpBasic(httpBasic -> httpBasic
                .realmName(SecurityConfig.ADMIN_REALM)
                .authenticationEntryPoint(adminAuthenticationEntryPoint))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/health").permitAll()
                // Read operations allowed for both ADMIN and VIEWER roles
                .requestMatchers(HttpMethod.GET, "/api/v1/**").hasAnyRole("ADMIN", "VIEWER")
                // Write operations restricted to ADMIN role only
                .requestMatchers(HttpMethod.POST, "/api/v1/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/v1/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/**").hasRole("ADMIN")
                .anyRequest().authenticated());

        return http.build();
    }

    /**
     * Security filter chain for Spring Boot Actuator endpoints.
     * Requires ADMIN-role HTTP Basic authentication before exposing operational state.
     */
    @Bean
    @Order(4)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/actuator/**")
            .cors(Customizer.withDefaults())
            .csrf(csrf -> SecurityCsrfSupport.configure(csrf, csrfProperties))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(adminAuthenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .httpBasic(httpBasic -> httpBasic
                .realmName(SecurityConfig.ADMIN_REALM)
                .authenticationEntryPoint(adminAuthenticationEntryPoint))
            .authorizeHttpRequests(auth -> auth
                .anyRequest().hasRole("ADMIN"));

        return http.build();
    }

    /**
     * In-memory user details service for authentication.
     *
     * <p>Supports two configuration modes:
     * <ol>
     *   <li><strong>Multi-user mode</strong> (via {@code security.users}): Configure multiple users
     *       with different roles (ADMIN or VIEWER) for local development.</li>
     *   <li><strong>Legacy single-user mode</strong> (via {@code security.admin.*}): Single admin user
     *       with ADMIN role for backward compatibility.</li>
     * </ol>
     *
     * <p>When {@code security.users} is configured, those users take precedence.
     * Otherwise, falls back to the legacy single admin user configuration.
     *
     * <p>Fails startup if no users are configured to prevent insecure deployments.
     *
     * @return UserDetailsService with configured users
     * @throws IllegalStateException if no users are configured
     * @see SecurityUsersProperties
     * @see SecurityProperties
     */
    @Bean
    public UserDetailsService userDetailsService() {
        List<UserDetails> users = new ArrayList<>();

        // Check for multi-user configuration first
        List<SecurityUsersProperties.UserProperties> configuredUsers = securityUsersProperties.getUsers();
        if (!configuredUsers.isEmpty()) {
            for (SecurityUsersProperties.UserProperties userProps : configuredUsers) {
                if (userProps.getUsername() == null || userProps.getUsername().isBlank()) {
                    throw new IllegalStateException("Username must be configured for each user in security.users");
                }
                if (SecuritySecrets.isMissingOrPlaceholder(userProps.getPassword())) {
                    throw new IllegalStateException(
                        "Password must be configured with a non-placeholder value for user: "
                            + userProps.getUsername());
                }
                String role = userProps.getRole();
                if (role == null || role.isBlank()) {
                    role = "VIEWER"; // Default to least privilege
                }
                // Normalize role (remove ROLE_ prefix if present)
                if (role.startsWith("ROLE_")) {
                    role = role.substring(5);
                }
                UserDetails user = User.builder()
                    .username(userProps.getUsername())
                    .password(passwordEncoder.encode(userProps.getPassword()))
                    .roles(role.toUpperCase(Locale.ROOT))
                    .build();
                users.add(user);
            }
        } else {
            // Fall back to legacy single admin user configuration
            SecurityProperties.Admin admin = securityProperties.getAdmin();
            if (SecuritySecrets.isMissingOrPlaceholder(admin.getPassword())) {
                throw new IllegalStateException(
                    "Admin password must be configured with a non-placeholder value. " +
                    "Set security.admin.password property, ADMIN_PASSWORD environment variable, " +
                    "or configure users via security.users.");
            }
            UserDetails adminUser = User.builder()
                .username(admin.getUsername())
                .password(passwordEncoder.encode(admin.getPassword()))
                .roles("ADMIN")
                .build();
            users.add(adminUser);
        }

        return new InMemoryUserDetailsManager(users);
    }
}
