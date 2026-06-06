package com.liftsimulator.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.liftsimulator.LocalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifies that the Flyway migrations are actually executed during test startup
 * (rather than the schema being created by Hibernate {@code ddl-auto}).
 *
 * <p>Runs against the shared Testcontainers PostgreSQL instance locally and the
 * service-container database in CI, so a broken migration fails the build in
 * either environment.
 *
 * <p>Lives in {@code com.liftsimulator.admin} so {@code @SpringBootTest} (via
 * {@link LocalIntegrationTest}) discovers the {@code LiftConfigServiceApplication}
 * configuration class.
 */
class FlywayMigrationIntegrationTest extends LocalIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywaySchemaHistoryIsPopulated() {
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lift_simulator.flyway_schema_history "
                        + "WHERE type = 'SQL'",
                Integer.class);
        assertTrue(applied != null && applied >= 1,
                "Expected Flyway to record applied SQL migrations");
    }

    @Test
    void allMigrationsSucceeded() {
        Integer failures = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lift_simulator.flyway_schema_history "
                        + "WHERE success = false",
                Integer.class);
        assertEquals(0, failures, "No Flyway migration should be marked as failed");
    }

    @Test
    void baselineAndLatestMigrationsAreApplied() {
        assertMigrationApplied("1");
        assertMigrationApplied("9");
    }

    @Test
    void migratedTablesExist() {
        // lift_system is created by V1; its presence proves the migration ran.
        Integer tableCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables "
                        + "WHERE table_schema = 'lift_simulator' AND table_name = 'lift_system'",
                Integer.class);
        assertEquals(1, tableCount, "Expected lift_system table created by migration V1");
    }

    private void assertMigrationApplied(String version) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM lift_simulator.flyway_schema_history "
                        + "WHERE version = ? AND success = true",
                Integer.class, version);
        assertEquals(1, count, "Expected migration version " + version + " to be applied successfully");
    }
}
