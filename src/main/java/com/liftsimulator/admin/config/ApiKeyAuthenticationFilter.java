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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

        if (!secureCompareApiKeys(expectedApiKey, providedApiKey)) {
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
     * Securely compares two API keys using SHA-256 hashing to prevent timing attacks.
     * Uses hash comparison rather than direct equality to prevent credential leakage
     * through timing analysis.
     */
    private boolean secureCompareApiKeys(String expected, String provided) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] expectedHash = digest.digest(expected.getBytes(StandardCharsets.UTF_8));
            byte[] providedHash = digest.digest(provided.getBytes(StandardCharsets.UTF_8));
            return constantTimeEquals(expectedHash, providedHash);
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Compares two byte arrays in constant time to prevent timing attacks.
     * This method takes the same time regardless of where the arrays differ.
     */
    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
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
