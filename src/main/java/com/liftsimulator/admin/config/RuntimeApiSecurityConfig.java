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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Security configuration for the API key protected runtime filter chain.
 *
 * <p>Covers runtime configuration ({@code /api/v1/runtime/**}) and simulation
 * execution ({@code /api/v1/simulation-runs/**}) APIs for machine-to-machine
 * and CLI callers. Requests authenticate with an API key supplied via the
 * configurable header ({@code api.auth.header}, defaults to {@code X-API-Key}).
 *
 * <p>Processed first (Order 1) so these requests are handled before the admin
 * HTTP Basic chain.
 *
 * @see SecurityConfig
 * @see ApiAuthProperties
 * @see ApiKeyAuthenticationFilter
 */
@Configuration
public class RuntimeApiSecurityConfig {

    private final ApiAuthProperties apiAuthProperties;
    private final AuthenticationEntryPoint apiKeyAuthenticationEntryPoint;
    private final CsrfProperties csrfProperties;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singleton beans injected via constructor. "
                    + "These collaborators are not modified externally and defensive copying "
                    + "would break Spring's dependency injection pattern."
    )
    public RuntimeApiSecurityConfig(ApiAuthProperties apiAuthProperties,
                                    @Qualifier("apiKeyAuthenticationEntryPoint")
                                    AuthenticationEntryPoint apiKeyAuthenticationEntryPoint,
                                    CsrfProperties csrfProperties) {
        this.apiAuthProperties = apiAuthProperties;
        this.apiKeyAuthenticationEntryPoint = apiKeyAuthenticationEntryPoint;
        this.csrfProperties = csrfProperties;
    }

    /**
     * Request matcher for API key protected endpoints.
     * Matches runtime configuration and simulation run APIs.
     */
    private RequestMatcher apiKeyProtectedMatcher() {
        PathPatternRequestMatcher.Builder matcherBuilder = PathPatternRequestMatcher.withDefaults();
        return new OrRequestMatcher(
            matcherBuilder.matcher("/api/v1/runtime/**"),
            matcherBuilder.matcher("/api/v1/simulation-runs/**")
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
        http
            .securityMatcher(apiKeyProtectedMatcher())
            .cors(Customizer.withDefaults())
            .csrf(csrf -> SecurityCsrfSupport.configure(csrf, csrfProperties))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(apiKeyAuthenticationEntryPoint))
            .addFilterBefore(
                new ApiKeyAuthenticationFilter(
                    apiAuthProperties.getHeader(),
                    apiAuthProperties.getKey(),
                    apiKeyAuthenticationEntryPoint),
                UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated());

        return http.build();
    }
}
