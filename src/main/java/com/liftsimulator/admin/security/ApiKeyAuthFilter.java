package com.liftsimulator.admin.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Enforces API key authentication for runtime and simulation execution endpoints.
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String DEFAULT_HEADER = "X-API-Key";

    private final String apiKey;
    private final String headerName;

    public ApiKeyAuthFilter(String apiKey) {
        this(apiKey, DEFAULT_HEADER);
    }

    public ApiKeyAuthFilter(String apiKey, String headerName) {
        this.apiKey = apiKey;
        this.headerName = headerName;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if (apiKey == null || apiKey.isBlank()) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "API key is not configured");
            return;
        }

        String providedKey = request.getHeader(headerName);
        if (providedKey == null || !providedKey.equals(apiKey)) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid API key");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
