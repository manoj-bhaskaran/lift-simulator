package com.liftsimulator.admin.config;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for API key authentication on runtime endpoints.
 *
 * <p>Configured via {@code api.auth.*} properties in application configuration:
 * <ul>
 *   <li>{@code api.auth.key} - the expected API key value (required)</li>
 *   <li>{@code api.auth.header} - the header carrying the API key
 *       (defaults to {@code X-API-Key})</li>
 * </ul>
 *
 * <p>Validation runs at startup so a missing or placeholder API key fails fast
 * before the application starts serving requests.
 *
 * @see RuntimeApiSecurityConfig
 * @see ApiKeyAuthenticationFilter
 */
@ConfigurationProperties(prefix = "api.auth")
public class ApiAuthProperties {

    /**
     * Expected API key value for runtime endpoints.
     */
    private String key = "";

    /**
     * Name of the header carrying the API key.
     */
    private String header = "X-API-Key";

    /**
     * Validate the API key at startup so insecure deployments fail fast.
     */
    @PostConstruct
    public void validate() {
        if (SecuritySecrets.isMissingOrPlaceholder(key)) {
            throw new IllegalStateException(
                "API key must be configured with a non-placeholder value. " +
                "Set api.auth.key property or API_KEY environment variable. " +
                "Example: export API_KEY=$(openssl rand -base64 32)");
        }
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }
}
