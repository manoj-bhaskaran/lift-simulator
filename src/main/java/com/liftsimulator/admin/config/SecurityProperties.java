package com.liftsimulator.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Configuration properties for admin credentials and OpenAPI access policy.
 *
 * <p>Configured via {@code security.*} properties in application configuration:
 * <ul>
 *   <li>{@code security.admin.username} / {@code security.admin.password} -
 *       legacy single-user admin credentials (used when {@code security.users}
 *       is not configured)</li>
 *   <li>{@code security.openapi.public-access} - whether Swagger UI and
 *       {@code /api-docs} are reachable without authentication
 *       (defaults to {@code false} for private access)</li>
 * </ul>
 *
 * @see AdminSecurityConfig
 * @see OpenApiSecurityConfig
 * @see SecurityUsersProperties
 */
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    /**
     * Legacy single-user admin credentials.
     */
    private final Admin admin = new Admin();

    /**
     * OpenAPI / Swagger access policy.
     */
    private final Openapi openapi = new Openapi();

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP",
            justification = "Nested @ConfigurationProperties object must be returned by reference "
                    + "so Spring can bind into the same instance; it is not exposed for external mutation."
    )
    public Admin getAdmin() {
        return admin;
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP",
            justification = "Nested @ConfigurationProperties object must be returned by reference "
                    + "so Spring can bind into the same instance; it is not exposed for external mutation."
    )
    public Openapi getOpenapi() {
        return openapi;
    }

    /**
     * Legacy single-user admin credentials ({@code security.admin.*}).
     */
    public static class Admin {
        private String username = "admin";
        private String password = "";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
     * OpenAPI / Swagger access policy ({@code security.openapi.*}).
     */
    public static class Openapi {
        private boolean publicAccess = false;

        public boolean isPublicAccess() {
            return publicAccess;
        }

        public void setPublicAccess(boolean publicAccess) {
            this.publicAccess = publicAccess;
        }
    }
}
