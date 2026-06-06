package com.liftsimulator;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for integration tests that need a real PostgreSQL database.
 *
 * <p>Database wiring is supplied automatically by
 * {@code com.liftsimulator.testsupport.TestcontainersContextCustomizerFactory}
 * (registered globally in {@code META-INF/spring.factories}):
 * <ul>
 *   <li><b>Local dev:</b> a shared Testcontainers PostgreSQL instance is started
 *       on demand, so {@code mvn test} runs without any pre-existing database.</li>
 *   <li><b>CI/CD:</b> when {@code SPRING_DATASOURCE_URL} is set, the customizer
 *       stands aside and the workflow's service-container datasource is used.</li>
 * </ul>
 *
 * <p>In both environments Flyway migrations run during startup and Hibernate is
 * in {@code validate} mode (see {@code application-test.yml}).
 *
 * <p>The {@link DynamicPropertySource} below is retained only as a defensive
 * override for explicit {@code SPRING_DATASOURCE_*} environment variables; the
 * Testcontainers customizer handles the local case.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @DynamicPropertySource
    static void registerEnvironmentProperties(DynamicPropertyRegistry registry) {
        // Explicitly register environment variables as Spring properties so that
        // CI/CD environment variables override YAML defaults.
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
