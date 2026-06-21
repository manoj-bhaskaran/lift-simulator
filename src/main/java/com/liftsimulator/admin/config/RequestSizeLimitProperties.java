package com.liftsimulator.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for inbound request body size limits.
 */
@ConfigurationProperties(prefix = "request-size-limits")
public class RequestSizeLimitProperties {

    private boolean enabled = true;
    private long maxBodyBytes = 1_048_576;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getMaxBodyBytes() {
        return maxBodyBytes;
    }

    public void setMaxBodyBytes(long maxBodyBytes) {
        this.maxBodyBytes = maxBodyBytes;
    }
}
