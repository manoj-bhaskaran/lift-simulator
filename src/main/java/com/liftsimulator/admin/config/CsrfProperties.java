package com.liftsimulator.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for Cross-Site Request Forgery (CSRF) protection.
 *
 * <p>Configured via {@code security.csrf.*} properties in application configuration.
 */
@Component
@ConfigurationProperties(prefix = "security.csrf")
public class CsrfProperties {

    /**
     * Whether CSRF protection is enabled.
     */
    private boolean enabled = false;

    /**
     * Paths that should bypass CSRF protection when it is enabled.
     */
    private List<String> ignoredPaths = List.of("/api/**", "/actuator/**");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getIgnoredPaths() {
        return new ArrayList<>(ignoredPaths);
    }

    public void setIgnoredPaths(List<String> ignoredPaths) {
        this.ignoredPaths = ignoredPaths != null ? new ArrayList<>(ignoredPaths) : new ArrayList<>();
    }
}
