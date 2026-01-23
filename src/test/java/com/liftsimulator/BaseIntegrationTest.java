package com.liftsimulator;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for integration tests that need a real PostgreSQL database.
 * 
 * In CI/CD environments, uses the PostgreSQL service provided by GitHub Actions.
 * In local development, uses Testcontainers to start a PostgreSQL container.
 * 
 * Detection: If SPRING_DATASOURCE_URL environment variable is set, assumes 
 * CI/CD environment and uses that configuration directly.
 */
@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = 
            // Only create container in local development (when CI/CD env var not set)
            System.getenv("SPRING_DATASOURCE_URL") == null 
                ? new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("lift_simulator_test")
                    .withUsername("lift_admin")
                    .withPassword("liftpassword")
                : null;

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        // Use CI/CD environment variables if available, otherwise use Testcontainers
        String datasourceUrl = System.getenv("SPRING_DATASOURCE_URL");
        String datasourceUsername = System.getenv("SPRING_DATASOURCE_USERNAME");
        String datasourcePassword = System.getenv("SPRING_DATASOURCE_PASSWORD");

        if (datasourceUrl != null) {
            // CI/CD environment: use provided credentials
            registry.add("spring.datasource.url", () -> datasourceUrl);
            registry.add("spring.datasource.username", () -> datasourceUsername);
            registry.add("spring.datasource.password", () -> datasourcePassword);
        } else if (postgres != null) {
            // Local development: use Testcontainers
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
    }
}
