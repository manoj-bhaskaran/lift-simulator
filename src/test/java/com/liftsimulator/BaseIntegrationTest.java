package com.liftsimulator;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for integration tests that need a real PostgreSQL database.
 * 
 * Explicitly registers environment variables as Spring properties so they
 * override application-test.yml defaults in CI/CD environments.
 * 
 * Uses application-test.yml configuration which defaults to localhost:5432
 * for local development, but can be overridden via SPRING_DATASOURCE_* env vars.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @DynamicPropertySource
    static void registerEnvironmentProperties(DynamicPropertyRegistry registry) {
        // Explicitly register environment variables as Spring properties
        // This ensures CI/CD environment variables override YAML defaults
        String datasourceUrl = System.getenv("SPRING_DATASOURCE_URL");
        String datasourceUsername = System.getenv("SPRING_DATASOURCE_USERNAME");
        String datasourcePassword = System.getenv("SPRING_DATASOURCE_PASSWORD");

        if (datasourceUrl != null) {
            registry.add("spring.datasource.url", () -> datasourceUrl);
        }
        if (datasourceUsername != null) {
            registry.add("spring.datasource.username", () -> datasourceUsername);
        }
        if (datasourcePassword != null) {
            registry.add("spring.datasource.password", () -> datasourcePassword);
        }
    }
}
