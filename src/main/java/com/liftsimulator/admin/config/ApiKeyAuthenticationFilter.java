package com.liftsimulator.admin.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Authentication filter for API key-based authentication on runtime endpoints.
 *
 * <p>Validates the API key header (configurable via {@code api.auth.header},
 * defaults to {@code X-API-Key}) against the configured API key.
 * If valid, authenticates the request with a "RUNTIME" role. If invalid or
 * missing, delegates to the authentication entry point for error handling.
 *
 * <p>This filter is designed for machine-to-machine communication where
 * username/password authentication is not practical.
 *
 * <p>Usage:
 * <pre>
 * curl -H "X-API-Key: your-api-key" http://localhost:8080/api/runtime/systems/my-system/config
 * </pre>
 *
 * @see SecurityConfig
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyAuthenticationFilter.class);

    private final String apiKeyHeader;
    private final String expectedApiKey;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    /**
     * Creates a new API key authentication filter.
     *
     * @param apiKeyHeader the name of the header containing the API key
     * @param expectedApiKey the expected API key value
     * @param authenticationEntryPoint entry point for handling authentication failures
     */
    public ApiKeyAuthenticationFilter(String apiKeyHeader, String expectedApiKey, AuthenticationEntryPoint authenticationEntryPoint) {
        this.apiKeyHeader = apiKeyHeader;
        this.expectedApiKey = expectedApiKey;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String providedApiKey = request.getHeader(apiKeyHeader);

        // If API key is not configured (empty), reject all requests
        if (expectedApiKey == null || expectedApiKey.isBlank()) {
            logger.warn("API key not configured - rejecting runtime API request to {}",
                request.getRequestURI());
            authenticationEntryPoint.commence(request, response,
                new ApiKeyNotConfiguredException("API key authentication is not configured"));
            return;
        }

        // Validate the provided API key
        if (providedApiKey == null || providedApiKey.isBlank()) {
            logger.info("Missing API key header for request {}", request.getRequestURI());
            authenticationEntryPoint.commence(request, response,
                new MissingApiKeyException("API key is required"));
            return;
        }

        if (!expectedApiKey.equals(providedApiKey)) {
            logger.warn("Invalid API key provided for request {}", request.getRequestURI());
            authenticationEntryPoint.commence(request, response,
                new InvalidApiKeyException("Invalid API key"));
            return;
        }

        // Create authentication token for valid API key
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            "api-client",
            null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_RUNTIME"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        logger.debug("API key authentication successful for request {}", request.getRequestURI());
        filterChain.doFilter(request, response);
    }

    /**
     * Exception thrown when API key is not configured on the server.
     */
    public static class ApiKeyNotConfiguredException
            extends org.springframework.security.core.AuthenticationException {
        public ApiKeyNotConfiguredException(String msg) {
            super(msg);
        }
    }

    /**
     * Exception thrown when API key header is missing from request.
     */
    public static class MissingApiKeyException
            extends org.springframework.security.core.AuthenticationException {
        public MissingApiKeyException(String msg) {
            super(msg);
        }
    }

    /**
     * Exception thrown when provided API key is invalid.
     */
    public static class InvalidApiKeyException
            extends org.springframework.security.core.AuthenticationException {
        public InvalidApiKeyException(String msg) {
            super(msg);
        }
    }
}
