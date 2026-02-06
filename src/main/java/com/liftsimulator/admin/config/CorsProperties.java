package com.liftsimulator.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for Cross-Origin Resource Sharing (CORS).
 *
 * <p>Configured via {@code security.cors.*} properties in application configuration.
 */
@Component
@ConfigurationProperties(prefix = "security.cors")
public class CorsProperties {

    /**
     * Allowed origins for cross-origin requests.
     */
    private List<String> allowedOrigins = new ArrayList<>();

    /**
     * Allowed HTTP methods for cross-origin requests.
     */
    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");

    /**
     * Allowed headers for cross-origin requests.
     */
    private List<String> allowedHeaders = List.of(
        "Authorization",
        "Content-Type",
        "X-API-Key",
        "X-Requested-With",
        "Accept",
        "Origin");

    /**
     * Headers exposed to the browser.
     */
    private List<String> exposedHeaders = List.of("WWW-Authenticate");

    /**
     * Whether to allow credentials (cookies/HTTP auth) in cross-origin requests.
     */
    private boolean allowCredentials = true;

    /**
     * How long (in seconds) the results of a preflight request can be cached.
     */
    private long maxAge = 3600;

    public List<String> getAllowedOrigins() {
        return new ArrayList<>(allowedOrigins);
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins != null ? new ArrayList<>(allowedOrigins) : new ArrayList<>();
    }

    public List<String> getAllowedMethods() {
        return new ArrayList<>(allowedMethods);
    }

    public void setAllowedMethods(List<String> allowedMethods) {
        this.allowedMethods = allowedMethods != null ? new ArrayList<>(allowedMethods) : new ArrayList<>();
    }

    public List<String> getAllowedHeaders() {
        return new ArrayList<>(allowedHeaders);
    }

    public void setAllowedHeaders(List<String> allowedHeaders) {
        this.allowedHeaders = allowedHeaders != null ? new ArrayList<>(allowedHeaders) : new ArrayList<>();
    }

    public List<String> getExposedHeaders() {
        return new ArrayList<>(exposedHeaders);
    }

    public void setExposedHeaders(List<String> exposedHeaders) {
        this.exposedHeaders = exposedHeaders != null ? new ArrayList<>(exposedHeaders) : new ArrayList<>();
    }

    public boolean isAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }
}
