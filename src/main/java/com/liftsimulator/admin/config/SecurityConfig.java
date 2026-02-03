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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Security configuration for the Lift Simulator application.
 *
 * <p>Configures authentication mechanisms:
 * <ul>
 *   <li><strong>Admin APIs</strong> ({@code /api/v1/**} excluding runtime and simulation-runs):
 *       HTTP Basic authentication with environment-configured username and password.
 *       Role-based access control with ADMIN and VIEWER roles.</li>
 *   <li><strong>Runtime APIs</strong> ({@code /api/v1/runtime/**}): API key authentication via
 *       {@code X-API-Key} header for machine-to-machine communication.</li>
 *   <li><strong>Simulation Run APIs</strong> ({@code /api/v1/simulation-runs/**}): API key authentication
 *       via {@code X-API-Key} header for CLI tools and automation.</li>
 * </ul>
 *
 * <p>Role-based access control (RBAC):
 * <ul>
 *   <li><strong>ADMIN</strong>: Full access to all operations (read and write)</li>
 *   <li><strong>VIEWER</strong>: Read-only access (GET requests only)</li>
 * </ul>
 *
 * <p>Authorization rules for Admin APIs:
 * <ul>
 *   <li>GET requests: Allowed for ADMIN and VIEWER roles</li>
 *   <li>POST, PUT, DELETE, PATCH requests: Restricted to ADMIN role only</li>
 *   <li>Unauthorized access returns HTTP 403 Forbidden</li>
 * </ul>
 *
 * <p>Public endpoints (no authentication required):
 * <ul>
 *   <li>{@code /api/v1/health} - Application health check</li>
 *   <li>{@code /actuator/**} - Spring Boot Actuator endpoints</li>
 *   <li>Static resources - Frontend assets</li>
 * </ul>
 *
 * <p>Security features:
 * <ul>
 *   <li>Stateless session management (no session cookies)</li>
 *   <li>CSRF policy configurable (disabled by default for stateless REST APIs)</li>
 *   <li>Custom error responses using {@link CustomAuthenticationEntryPoint}</li>
 *   <li>Custom access denied handler using {@link CustomAccessDeniedHandler}</li>
 *   <li>WWW-Authenticate header for HTTP Basic challenges (RFC 7235)</li>
 *   <li>Explicit CORS policy for frontend-backend interaction</li>
 *   <li>Explicit CSRF policy (disabled by default for stateless APIs)</li>
 * </ul>
 *
 * @see CustomAuthenticationEntryPoint
 * @see CustomAccessDeniedHandler
 * @see ApiKeyAuthenticationFilter
 * @see SecurityUsersProperties
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

    @Value("${api.auth.header:X-API-Key}")
    private String apiKeyHeader;

    private final SecurityUsersProperties securityUsersProperties;
    private final CorsProperties corsProperties;
    private final CsrfProperties csrfProperties;

    public SecurityConfig(SecurityUsersProperties securityUsersProperties,
                          CorsProperties corsProperties,
                          CsrfProperties csrfProperties) {
        this.securityUsersProperties = securityUsersProperties;
        this.corsProperties = corsProperties;
        this.csrfProperties = csrfProperties;
    }

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
     * Access denied handler for authorization failures.
     * Returns HTTP 403 with JSON error response when authenticated users
     * attempt operations they are not authorized to perform.
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return new CustomAccessDeniedHandler();
    }

    /**
     * Request matcher for API key protected endpoints.
     * Matches runtime configuration and simulation run APIs.
     */
    private RequestMatcher apiKeyProtectedMatcher() {
        return new OrRequestMatcher(
            new AntPathRequestMatcher("/api/v1/runtime/**"),
            new AntPathRequestMatcher("/api/v1/simulation-runs/**")
        );
    }

    /**
     * Security filter chain for API key protected endpoints.
     * Covers runtime configuration and simulation execution APIs.
     * Uses API key authentication via configurable header (api.auth.header, defaults to X-API-Key).
     * Processed first (Order 1) to handle these requests before admin filter chain.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiKeySecurityFilterChain(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint entryPoint = apiKeyAuthenticationEntryPoint();

        http
            .securityMatcher(apiKeyProtectedMatcher())
            .cors(Customizer.withDefaults())
            .csrf(csrf -> configureCsrf(csrf))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(entryPoint))
            .addFilterBefore(
                new ApiKeyAuthenticationFilter(apiKeyHeader, apiKey, entryPoint),
                UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated());

        return http.build();
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
     * Processed second (Order 2) after API key filter chain.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain adminApiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/api/v1/**")
            .cors(Customizer.withDefaults())
            .csrf(csrf -> configureCsrf(csrf))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(adminAuthenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler()))
            .httpBasic(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/health").permitAll()
                // OpenAPI/Swagger endpoints
                .requestMatchers("/api/v1/api-docs/**", "/api/v1/swagger-ui/**", "/api/v1/swagger-ui.html").permitAll()
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
     * Security filter chain for public endpoints and static resources.
     * Permits unauthenticated access to actuator, static files, and frontend routes.
     * Processed last (Order 3) as the default filter chain.
     */
    @Bean
    @Order(3)
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> configureCsrf(csrf))
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
     * Configures CSRF based on the explicit security.csrf configuration.
     */
    private void configureCsrf(org.springframework.security.config.annotation.web.configurers.CsrfConfigurer<HttpSecurity> csrf) {
        if (!csrfProperties.isEnabled()) {
            csrf.disable();
            return;
        }

        CookieCsrfTokenRepository tokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
        RequestMatcher[] ignoredMatchers = csrfProperties.getIgnoredPaths().stream()
            .map(AntPathRequestMatcher::new)
            .toArray(RequestMatcher[]::new);

        csrf.csrfTokenRepository(tokenRepository)
            .csrfTokenRequestHandler(requestHandler)
            .ignoringRequestMatchers(ignoredMatchers);
    }

    /**
     * Explicit CORS configuration for API endpoints.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(corsProperties.getAllowedMethods());
        configuration.setAllowedHeaders(corsProperties.getAllowedHeaders());
        configuration.setExposedHeaders(corsProperties.getExposedHeaders());
        configuration.setAllowCredentials(corsProperties.isAllowCredentials());
        configuration.setMaxAge(corsProperties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
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
     */
    @Bean
    public UserDetailsService userDetailsService() {
        List<UserDetails> users = new ArrayList<>();
        PasswordEncoder encoder = passwordEncoder();

        // Check for multi-user configuration first
        List<SecurityUsersProperties.UserProperties> configuredUsers = securityUsersProperties.getUsers();
        if (configuredUsers != null && !configuredUsers.isEmpty()) {
            for (SecurityUsersProperties.UserProperties userProps : configuredUsers) {
                if (userProps.getUsername() == null || userProps.getUsername().isBlank()) {
                    throw new IllegalStateException("Username must be configured for each user in security.users");
                }
                if (userProps.getPassword() == null || userProps.getPassword().isBlank()) {
                    throw new IllegalStateException(
                        "Password must be configured for user: " + userProps.getUsername());
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
                    .password(encoder.encode(userProps.getPassword()))
                    .roles(role.toUpperCase())
                    .build();
                users.add(user);
            }
        } else {
            // Fall back to legacy single admin user configuration
            if (adminPassword == null || adminPassword.isBlank()) {
                throw new IllegalStateException(
                    "Admin password must be configured. Set security.admin.password property, " +
                    "ADMIN_PASSWORD environment variable, or configure users via security.users.");
            }
            UserDetails admin = User.builder()
                .username(adminUsername)
                .password(encoder.encode(adminPassword))
                .roles("ADMIN")
                .build();
            users.add(admin);
        }

        return new InMemoryUserDetailsManager(users);
    }
}
