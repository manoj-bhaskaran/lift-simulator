package com.liftsimulator;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for local integration tests that use Testcontainers.
 * 
 * This class provides a PostgreSQL container for local development testing.
 * The container is automatically started and provides database credentials
 * to the Spring Boot test context via @DynamicPropertySource.
 * 
 * Use this class for integration tests in local development.
 * For CI/CD environments, tests should extend BaseIntegrationTest which
 * uses the GitHub Actions PostgreSQL service via environment variables.
 */
@Testcontainers
public abstract class LocalIntegrationTest extends BaseIntegrationTest {

    private static final boolean USE_TESTCONTAINERS = System.getenv("SPRING_DATASOURCE_URL") == null;

    @Container
    static PostgreSQLContainer<?> postgres = USE_TESTCONTAINERS
            ? new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("lift_simulator_test")
                    .withUsername("lift_admin")
                    .withPassword("liftpassword")
            : null;

    @DynamicPropertySource
    static void registerLocalPostgresProperties(DynamicPropertyRegistry registry) {
        // Only register and start Testcontainers when no external datasource is provided
        if (USE_TESTCONTAINERS && postgres != null) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
    }
}
