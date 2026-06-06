package com.liftsimulator.testsupport;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Lazily-started singleton PostgreSQL container shared by every Spring test
 * context in the suite.
 *
 * <p>Using a single shared container (rather than one per test class) keeps the
 * suite fast: the container is created on first use and reused for the lifetime
 * of the JVM. Testcontainers' Ryuk reaper stops and removes it once the JVM
 * exits, so no explicit shutdown hook is required.
 *
 * <p>The image and credentials mirror the GitHub Actions service container in
 * {@code .github/workflows/ci.yml} so local runs and CI exercise the same
 * PostgreSQL version.
 */
final class SharedPostgresContainer {

    /** Matches the {@code postgres:15-alpine} service container used in CI. */
    private static final DockerImageName IMAGE = DockerImageName.parse("postgres:15-alpine");

    private static final String DATABASE = "lift_simulator_test";
    private static final String USERNAME = "lift_admin";
    private static final String PASSWORD = "liftpassword";

    /** Schema the migrations and entity mappings live in. */
    static final String SCHEMA = "lift_simulator";

    private static volatile PostgreSQLContainer<?> container;

    private SharedPostgresContainer() {
    }

    /**
     * Returns the running shared container, starting it on first call.
     */
    static PostgreSQLContainer<?> getStarted() {
        PostgreSQLContainer<?> local = container;
        if (local == null) {
            synchronized (SharedPostgresContainer.class) {
                local = container;
                if (local == null) {
                    local = new PostgreSQLContainer<>(IMAGE)
                            .withDatabaseName(DATABASE)
                            .withUsername(USERNAME)
                            .withPassword(PASSWORD)
                            // Resolve the connection against the application schema so
                            // Flyway/Hibernate operate on lift_simulator, not public.
                            .withUrlParam("currentSchema", SCHEMA);
                    local.start();
                    container = local;
                }
            }
        }
        return local;
    }
}
