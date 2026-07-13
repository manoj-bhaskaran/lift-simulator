package com.liftsimulator.admin.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Shared Spring Security infrastructure for the Lift Simulator application.
 *
 * <p>Enables web security and the typed configuration properties, and contributes
 * the beans reused across the per-chain configurations:
 * <ul>
 *   <li>{@link AuthenticationEntryPoint} beans for HTTP Basic and API key failures</li>
 *   <li>the shared {@link AccessDeniedHandler} and {@link PasswordEncoder}</li>
 *   <li>the explicit {@link CorsConfigurationSource} used by every chain</li>
 *   <li>the default (lowest-precedence) public/static-resource filter chain</li>
 * </ul>
 *
 * <p>The authenticated chains live in dedicated configurations so each filter
 * chain owns one concern:
 * <ul>
 *   <li>{@link RuntimeApiSecurityConfig} — API-key chain (Order 1)</li>
 *   <li>{@link OpenApiSecurityConfig} — Swagger/api-docs access (Order 2)</li>
 *   <li>{@link AdminSecurityConfig} — Admin HTTP Basic and Actuator chains
 *       (Order 3 and 4)</li>
 *   <li>this class — public/static-resource chain (Order 5)</li>
 * </ul>
 *
 * @see AdminSecurityConfig
 * @see RuntimeApiSecurityConfig
 * @see OpenApiSecurityConfig
 * @see CustomAuthenticationEntryPoint
 * @see CustomAccessDeniedHandler
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({ApiAuthProperties.class, SecurityProperties.class})
public class SecurityConfig {

    /** Realm advertised on HTTP Basic challenges for admin-protected endpoints. */
    static final String ADMIN_REALM = "Lift Simulator Admin";

    private final CorsProperties corsProperties;
    private final CsrfProperties csrfProperties;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singleton beans injected via constructor. "
                    + "These properties objects are not modified externally and defensive copying "
                    + "would break Spring's dependency injection pattern."
    )
    public SecurityConfig(CorsProperties corsProperties, CsrfProperties csrfProperties) {
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
     * Password encoder using BCrypt hashing algorithm.
     * Used for encoding and verifying admin passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Security filter chain for public endpoints and static resources.
     * Permits unauthenticated access to static files and frontend routes.
     * Processed last (Order 5) as the default filter chain.
     */
    @Bean
    @Order(5)
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> SecurityCsrfSupport.configure(csrf, csrfProperties))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/assets/**", "/favicon.ico").permitAll()
                // Allow frontend SPA routes
                .requestMatchers("/lift-systems/**", "/versions/**", "/health/**",
                    "/validate/**", "/scenarios/**", "/simulator/**", "/runs/**").permitAll()
                .anyRequest().permitAll());

        return http.build();
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
}
