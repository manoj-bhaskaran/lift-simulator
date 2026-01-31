package com.liftsimulator.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * Custom authentication entry point that returns consistent JSON error responses.
 *
 * <p>When authentication fails (missing or invalid credentials), this entry point
 * returns a JSON response matching the application's standard error format:
 * <pre>
 * {
 *   "status": 401,
 *   "message": "Authentication required",
 *   "timestamp": "2026-01-31T12:00:00Z"
 * }
 * </pre>
 *
 * <p>This ensures that all API error responses (including authentication errors)
 * have a consistent structure for client applications to handle.
 *
 * @see com.liftsimulator.admin.controller.GlobalExceptionHandler.ErrorResponse
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationEntryPoint.class);
    private final ObjectMapper objectMapper;

    public CustomAuthenticationEntryPoint() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        logger.info("Authentication failed for request {} {}: {}",
            request.getMethod(), request.getRequestURI(), authException.getMessage());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse error = new ErrorResponse(
            HttpStatus.UNAUTHORIZED.value(),
            "Authentication required",
            OffsetDateTime.now()
        );

        objectMapper.writeValue(response.getOutputStream(), error);
    }

    /**
     * Error response record matching the application's standard error format.
     * Duplicated here to avoid circular dependency with GlobalExceptionHandler.
     */
    public record ErrorResponse(
        int status,
        String message,
        OffsetDateTime timestamp
    ) {
    }
}
