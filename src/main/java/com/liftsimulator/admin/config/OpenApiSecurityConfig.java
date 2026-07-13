package com.liftsimulator.admin.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Security configuration for OpenAPI/Swagger documentation endpoints.
 *
 * <p>Governs access to {@code /api/v1/api-docs/**} and the Swagger UI. When
 * {@code security.openapi.public-access} is enabled the documentation is
 * reachable without authentication; otherwise it requires ADMIN-role HTTP Basic
 * authentication (using the same entry point and access denied handler as the
 * admin chain, so 401/403 responses and the WWW-Authenticate challenge stay
 * consistent).
 *
 * <p>Processed after the API-key chain (Order 1) and before the broader admin
 * chain (Order 3) so documentation routes are resolved by their dedicated rules.
 *
 * @see SecurityConfig
 * @see SecurityProperties
 */
@Configuration
public class OpenApiSecurityConfig {

    private static final String[] OPEN_API_MATCHERS = {
        "/api/v1/api-docs/**",
        "/api/v1/swagger-ui/**",
        "/api/v1/swagger-ui.html"
    };

    private final SecurityProperties securityProperties;
    private final AuthenticationEntryPoint adminAuthenticationEntryPoint;
    private final AccessDeniedHandler accessDeniedHandler;
    private final CsrfProperties csrfProperties;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singleton beans injected via constructor. "
                    + "These collaborators are not modified externally and defensive copying "
                    + "would break Spring's dependency injection pattern."
    )
    public OpenApiSecurityConfig(SecurityProperties securityProperties,
                                 @Qualifier("adminAuthenticationEntryPoint")
                                 AuthenticationEntryPoint adminAuthenticationEntryPoint,
                                 AccessDeniedHandler accessDeniedHandler,
                                 CsrfProperties csrfProperties) {
        this.securityProperties = securityProperties;
        this.adminAuthenticationEntryPoint = adminAuthenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.csrfProperties = csrfProperties;
    }

    /**
     * Security filter chain for OpenAPI/Swagger endpoints.
     * Permits unauthenticated access when {@code security.openapi.public-access}
     * is enabled; otherwise requires ADMIN-role HTTP Basic authentication.
     * Processed at Order 2, between the API-key and admin chains.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain openApiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(OPEN_API_MATCHERS)
            .cors(Customizer.withDefaults())
            .csrf(csrf -> SecurityCsrfSupport.configure(csrf, csrfProperties))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(adminAuthenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .httpBasic(httpBasic -> httpBasic
                .realmName(SecurityConfig.ADMIN_REALM)
                .authenticationEntryPoint(adminAuthenticationEntryPoint))
            .authorizeHttpRequests(auth -> {
                if (securityProperties.getOpenapi().isPublicAccess()) {
                    auth.anyRequest().permitAll();
                } else {
                    auth.anyRequest().hasRole("ADMIN");
                }
            });

        return http.build();
    }
}
