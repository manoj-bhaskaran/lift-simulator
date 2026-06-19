package com.liftsimulator;

import com.liftsimulator.admin.LiftConfigServiceApplication;
import com.liftsimulator.admin.repository.LiftSystemRepository;
import com.liftsimulator.admin.repository.LiftSystemVersionRepository;
import com.liftsimulator.admin.repository.ScenarioRepository;
import com.liftsimulator.admin.repository.SimulationRunRepository;
import com.liftsimulator.admin.service.SimulationRunExecutionService;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
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
@SpringBootTest(classes = LiftConfigServiceApplication.class)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    /**
     * Maximum time to wait for asynchronous simulation runs to finish before the
     * shared database state is reset for the next test.
     */
    private static final Duration ASYNC_RUN_DRAIN_TIMEOUT = Duration.ofSeconds(15);

    @Autowired(required = false)
    private SimulationRunExecutionService simulationRunExecutionService;

    @Autowired(required = false)
    private SimulationRunRepository simulationRunRepository;

    @Autowired(required = false)
    private ScenarioRepository scenarioRepository;

    @Autowired(required = false)
    private LiftSystemVersionRepository liftSystemVersionRepository;

    @Autowired(required = false)
    private LiftSystemRepository liftSystemRepository;

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

    /**
     * Resets the shared simulation tables before every test.
     *
     * <p>All integration tests share a single application context, so an
     * asynchronous simulation run started by one test can still be executing when
     * the next test begins. If such a background execution mutates a
     * {@code simulation_run} row while the next test's cleanup deletes it, the two
     * transactions can block each other indefinitely (manifesting as a CI hang on
     * a Hibernate {@code UPDATE}) or fail with foreign-key violations. To make the
     * suite deterministic this hook first waits for any in-flight runs to drain,
     * then clears the simulation tables in foreign-key-safe order before the
     * subclass {@code @BeforeEach} seeds its own data.</p>
     *
     * <p>JUnit 5 runs superclass {@code @BeforeEach} methods before subclass ones,
     * so this executes ahead of any per-test setup.</p>
     *
     * @throws InterruptedException if interrupted while waiting for runs to drain
     */
    @BeforeEach
    void resetSimulationState() throws InterruptedException {
        drainAsyncRuns();

        // Delete children before parents: simulation_run -> scenario -> version -> system.
        if (simulationRunRepository != null) {
            simulationRunRepository.deleteAll();
        }
        if (scenarioRepository != null) {
            scenarioRepository.deleteAll();
        }
        if (liftSystemVersionRepository != null) {
            liftSystemVersionRepository.deleteAll();
        }
        if (liftSystemRepository != null) {
            liftSystemRepository.deleteAll();
        }
    }

    private void drainAsyncRuns() throws InterruptedException {
        if (simulationRunExecutionService == null) {
            return;
        }
        long deadlineNanos = System.nanoTime() + ASYNC_RUN_DRAIN_TIMEOUT.toNanos();
        while (simulationRunExecutionService.hasActiveRuns() && System.nanoTime() < deadlineNanos) {
            Thread.sleep(25);
        }
    }
}
