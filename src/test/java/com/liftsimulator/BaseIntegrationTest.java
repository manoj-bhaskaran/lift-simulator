package com.liftsimulator;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for integration tests that need a real PostgreSQL database.
 * 
 * In CI/CD environments, uses the PostgreSQL service provided by GitHub Actions.
 * In local development, uses Testcontainers to start a PostgreSQL container.
 * 
 * Detection: If SPRING_DATASOURCE_URL environment variable is set, assumes 
 * CI/CD environment and uses that configuration directly.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        // Use CI/CD environment variables if available
        String datasourceUrl = System.getenv("SPRING_DATASOURCE_URL");
        String datasourceUsername = System.getenv("SPRING_DATASOURCE_USERNAME");
        String datasourcePassword = System.getenv("SPRING_DATASOURCE_PASSWORD");

        if (datasourceUrl != null) {
            // CI/CD environment: use provided environment variables
            registry.add("spring.datasource.url", () -> datasourceUrl);
            registry.add("spring.datasource.username", () -> datasourceUsername);
            registry.add("spring.datasource.password", () -> datasourcePassword);
        }
        // Otherwise, application-test.yml defaults will be used (pointing to localhost)
    }
}
