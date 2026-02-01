package com.liftsimulator.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Custom access denied handler for authorization failures.
 *
 * <p>Returns a JSON error response when an authenticated user attempts to access
 * a resource they are not authorized for (HTTP 403 Forbidden).
 *
 * <p>Response format:
 * <pre>
 * {
 *   "status": 403,
 *   "message": "Access denied",
 *   "timestamp": "2026-02-01T12:00:00Z"
 * }
 * </pre>
 *
 * @see SecurityConfig
 */
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomAccessDeniedHandler.class);
    private final ObjectMapper objectMapper;

    public CustomAccessDeniedHandler() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {

        logger.warn("Access denied for user '{}' attempting {} {} - {}",
            request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "unknown",
            request.getMethod(),
            request.getRequestURI(),
            accessDeniedException.getMessage());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("status", HttpServletResponse.SC_FORBIDDEN);
        errorResponse.put("message", "Access denied");
        errorResponse.put("timestamp", Instant.now().toString());

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
