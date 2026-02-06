package com.liftsimulator.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

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
 * <p>For HTTP Basic authentication, the entry point also sets the {@code WWW-Authenticate}
 * header to trigger browser authentication prompts per RFC 7235.
 *
 * <p>This ensures that all API error responses (including authentication errors)
 * have a consistent structure for client applications to handle.
 *
 * @see com.liftsimulator.admin.controller.GlobalExceptionHandler.ErrorResponse
 */
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(CustomAuthenticationEntryPoint.class);
    private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";

    private final ObjectMapper objectMapper;
    private final String realm;

    /**
     * Creates an entry point without HTTP Basic authentication challenge.
     * Suitable for API key or other non-Basic authentication mechanisms.
     */
    public CustomAuthenticationEntryPoint() {
        this(null);
    }

    /**
     * Creates an entry point with HTTP Basic authentication challenge.
     * When a realm is specified, the {@code WWW-Authenticate: Basic realm="..."} header
     * is included in 401 responses to trigger browser authentication prompts.
     *
     * @param realm the realm name for HTTP Basic authentication, or null to omit the header
     */
    public CustomAuthenticationEntryPoint(String realm) {
        this.realm = realm;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        // Serialize dates as ISO-8601 strings, not timestamps
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
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

        // Set WWW-Authenticate header for HTTP Basic authentication per RFC 7235
        if (realm != null && !realm.isBlank()) {
            response.setHeader(WWW_AUTHENTICATE_HEADER, "Basic realm=\"" + realm + "\"");
        }

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
