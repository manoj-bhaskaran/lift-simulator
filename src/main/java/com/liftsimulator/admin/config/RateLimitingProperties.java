package com.liftsimulator.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for API rate limiting.
 *
 * <p>Configures token-bucket rate limits for admin and runtime API endpoints separately.
 * Each endpoint group has its own bucket with configurable capacity and refill rate.
 *
 * <p>Example configuration in {@code application.yml}:
 * <pre>
 * rate-limiting:
 *   enabled: true
 *   admin:
 *     capacity: 100
 *     refill-tokens: 100
 *     refill-period-seconds: 60
 *   runtime:
 *     capacity: 1000
 *     refill-tokens: 1000
 *     refill-period-seconds: 60
 * </pre>
 *
 * @see RateLimitingFilter
 */
@Component
@ConfigurationProperties(prefix = "rate-limiting")
public class RateLimitingProperties {

    private boolean enabled = true;
    private EndpointLimits admin = new EndpointLimits(100, 100, 60);
    private EndpointLimits runtime = new EndpointLimits(1000, 1000, 60);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public EndpointLimits getAdmin() {
        return admin;
    }

    public void setAdmin(EndpointLimits admin) {
        this.admin = admin;
    }

    public EndpointLimits getRuntime() {
        return runtime;
    }

    public void setRuntime(EndpointLimits runtime) {
        this.runtime = runtime;
    }

    /**
     * Per-endpoint rate limit configuration using a token bucket model.
     */
    public static class EndpointLimits {

        private int capacity;
        private int refillTokens;
        private long refillPeriodSeconds;

        public EndpointLimits() {
        }

        public EndpointLimits(int capacity, int refillTokens, long refillPeriodSeconds) {
            this.capacity = capacity;
            this.refillTokens = refillTokens;
            this.refillPeriodSeconds = refillPeriodSeconds;
        }

        public int getCapacity() {
            return capacity;
        }

        public void setCapacity(int capacity) {
            this.capacity = capacity;
        }

        public int getRefillTokens() {
            return refillTokens;
        }

        public void setRefillTokens(int refillTokens) {
            this.refillTokens = refillTokens;
        }

        public long getRefillPeriodSeconds() {
            return refillPeriodSeconds;
        }

        public void setRefillPeriodSeconds(long refillPeriodSeconds) {
            this.refillPeriodSeconds = refillPeriodSeconds;
        }
    }
}
