package com.liftsimulator.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for security users.
 *
 * <p>Supports configuring multiple users with different roles for local development.
 * Users can be defined in application.properties or application-dev.yml:
 *
 * <pre>
 * security.users[0].username=admin
 * security.users[0].password=adminpass
 * security.users[0].role=ADMIN
 * security.users[1].username=viewer
 * security.users[1].password=viewerpass
 * security.users[1].role=VIEWER
 * </pre>
 *
 * <p>Or in YAML format:
 *
 * <pre>
 * security:
 *   users:
 *     - username: admin
 *       password: adminpass
 *       role: ADMIN
 *     - username: viewer
 *       password: viewerpass
 *       role: VIEWER
 * </pre>
 *
 * <p>If no users are configured via {@code security.users}, the legacy single-user
 * configuration via {@code security.admin.username} and {@code security.admin.password}
 * is used as a fallback with ADMIN role.
 *
 * @see SecurityConfig
 */
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityUsersProperties {

    /**
     * List of configured users with their credentials and roles.
     */
    private List<UserProperties> users = new ArrayList<>();

    public List<UserProperties> getUsers() {
        return users;
    }

    public void setUsers(List<UserProperties> users) {
        this.users = users;
    }

    /**
     * Configuration properties for a single user.
     */
    public static class UserProperties {
        private String username;
        private String password;
        private String role = "VIEWER"; // Default to VIEWER (least privilege)

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

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}
