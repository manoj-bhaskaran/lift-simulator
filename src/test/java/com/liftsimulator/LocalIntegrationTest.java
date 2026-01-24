package com.liftsimulator;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for local integration tests that use Testcontainers.
 * 
 * This class provides a PostgreSQL container for local development testing.
 * The container is only started when SPRING_DATASOURCE_URL is not set (i.e., not in CI/CD).
 * 
 * Use this class for integration tests in local development.
 * For CI/CD environments, the container is not started and the workflow's
 * PostgreSQL service is used via environment variables.
 */
public abstract class LocalIntegrationTest extends BaseIntegrationTest {

    private static final boolean USE_TESTCONTAINERS = System.getenv("SPRING_DATASOURCE_URL") == null;
    
    static PostgreSQLContainer<?> postgres;

    static {
        // Manually start container only in local development
        if (USE_TESTCONTAINERS) {
            postgres = new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("lift_simulator_test")
                    .withUsername("lift_admin")
                    .withPassword("liftpassword");
            postgres.start();
        }
    }

    @DynamicPropertySource
    static void registerLocalPostgresProperties(DynamicPropertyRegistry registry) {
        // Only register Testcontainers properties when no external datasource is provided
        if (USE_TESTCONTAINERS && postgres != null) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
    }
}
