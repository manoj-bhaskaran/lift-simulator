package com.liftsimulator.admin.repository;

import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.entity.SimulationRun.RunStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for SimulationRunRepository.
 * Uses H2 in-memory database configured in application-test.yml.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
public class SimulationRunRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SimulationRunRepository runRepository;

    private LiftSystem liftSystem;
    private LiftSystemVersion version;

    @BeforeEach
    public void setUp() {
        liftSystem = new LiftSystem("test-system-" + System.currentTimeMillis(), "Test System", "Test");
        entityManager.persist(liftSystem);

        version = new LiftSystemVersion(liftSystem, 1, "{\"lifts\": 2}");
        entityManager.persist(version);

        entityManager.flush();
    }

    @Test
    public void testSaveAndFindById() {
        SimulationRun run = new SimulationRun(liftSystem, version);

        SimulationRun saved = runRepository.save(run);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertEquals(RunStatus.CREATED, saved.getStatus());
        assertEquals(0L, saved.getCurrentTick());
        assertNull(saved.getStartedAt());
        assertNull(saved.getEndedAt());

        Optional<SimulationRun> found = runRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
    }

    @Test
    public void testStatusTransitionToRunning() {
        SimulationRun run = new SimulationRun(liftSystem, version);
        entityManager.persist(run);
        entityManager.flush();
        entityManager.clear();

        Optional<SimulationRun> found = runRepository.findById(run.getId());
        assertTrue(found.isPresent());

        SimulationRun runToUpdate = found.get();
        runToUpdate.start();

        SimulationRun updated = runRepository.save(runToUpdate);

        assertEquals(RunStatus.RUNNING, updated.getStatus());
        assertNotNull(updated.getStartedAt());
        assertNull(updated.getEndedAt());
    }

    @Test
    public void testStatusTransitionToSucceeded() {
        SimulationRun run = new SimulationRun(liftSystem, version);
        run.start();
        entityManager.persist(run);
        entityManager.flush();
        entityManager.clear();

        Optional<SimulationRun> found = runRepository.findById(run.getId());
        assertTrue(found.isPresent());

        SimulationRun runToUpdate = found.get();
        runToUpdate.succeed();

        SimulationRun updated = runRepository.save(runToUpdate);

        assertEquals(RunStatus.SUCCEEDED, updated.getStatus());
        assertNotNull(updated.getStartedAt());
        assertNotNull(updated.getEndedAt());
    }

    @Test
    public void testStatusTransitionToFailed() {
        SimulationRun run = new SimulationRun(liftSystem, version);
        run.start();
        entityManager.persist(run);
        entityManager.flush();
        entityManager.clear();

        Optional<SimulationRun> found = runRepository.findById(run.getId());
        assertTrue(found.isPresent());

        SimulationRun runToUpdate = found.get();
        runToUpdate.fail("Simulation error occurred");

        SimulationRun updated = runRepository.save(runToUpdate);

        assertEquals(RunStatus.FAILED, updated.getStatus());
        assertEquals("Simulation error occurred", updated.getErrorMessage());
        assertNotNull(updated.getEndedAt());
    }

    @Test
    public void testStatusTransitionToCancelled() {
        SimulationRun run = new SimulationRun(liftSystem, version);
        run.start();
        entityManager.persist(run);
        entityManager.flush();
        entityManager.clear();

        Optional<SimulationRun> found = runRepository.findById(run.getId());
        assertTrue(found.isPresent());

        SimulationRun runToUpdate = found.get();
        runToUpdate.cancel();

        SimulationRun updated = runRepository.save(runToUpdate);

        assertEquals(RunStatus.CANCELLED, updated.getStatus());
        assertNotNull(updated.getEndedAt());
    }

    @Test
    public void testInvalidStatusTransition() {
        SimulationRun run = new SimulationRun(liftSystem, version);
        entityManager.persist(run);
        entityManager.flush();

        assertThrows(IllegalStateException.class, () -> {
            run.succeed();
        });
    }

    @Test
    public void testUpdateProgress() {
        SimulationRun run = new SimulationRun(liftSystem, version);
        run.setTotalTicks(1000L);
        entityManager.persist(run);
        entityManager.flush();
        entityManager.clear();

        Optional<SimulationRun> found = runRepository.findById(run.getId());
        assertTrue(found.isPresent());

        SimulationRun runToUpdate = found.get();
        runToUpdate.updateProgress(500L);

        SimulationRun updated = runRepository.save(runToUpdate);

        assertEquals(500L, updated.getCurrentTick());
    }

    @Test
    public void testFindByLiftSystemIdOrderByCreatedAtDesc() {
        SimulationRun run1 = new SimulationRun(liftSystem, version);
        SimulationRun run2 = new SimulationRun(liftSystem, version);

        entityManager.persist(run1);
        entityManager.persist(run2);
        entityManager.flush();

        List<SimulationRun> runs = runRepository.findByLiftSystemIdOrderByCreatedAtDesc(liftSystem.getId());

        assertTrue(runs.size() >= 2);
        assertEquals(liftSystem.getId(), runs.get(0).getLiftSystem().getId());
    }

    @Test
    public void testFindByVersionIdOrderByCreatedAtDesc() {
        SimulationRun run1 = new SimulationRun(liftSystem, version);
        SimulationRun run2 = new SimulationRun(liftSystem, version);

        entityManager.persist(run1);
        entityManager.persist(run2);
        entityManager.flush();

        List<SimulationRun> runs = runRepository.findByVersionIdOrderByCreatedAtDesc(version.getId());

        assertTrue(runs.size() >= 2);
        assertEquals(version.getId(), runs.get(0).getVersion().getId());
    }

    @Test
    public void testFindByStatusOrderByCreatedAtDesc() {
        SimulationRun run1 = new SimulationRun(liftSystem, version);
        SimulationRun run2 = new SimulationRun(liftSystem, version);

        entityManager.persist(run1);
        entityManager.persist(run2);
        entityManager.flush();

        List<SimulationRun> createdRuns = runRepository.findByStatusOrderByCreatedAtDesc(RunStatus.CREATED);

        assertTrue(createdRuns.size() >= 2);
        assertEquals(RunStatus.CREATED, createdRuns.get(0).getStatus());
    }

    @Test
    public void testFindByLiftSystemIdAndStatus() {
        SimulationRun run1 = new SimulationRun(liftSystem, version);
        SimulationRun run2 = new SimulationRun(liftSystem, version);
        run2.start();

        entityManager.persist(run1);
        entityManager.persist(run2);
        entityManager.flush();

        List<SimulationRun> createdRuns = runRepository.findByLiftSystemIdAndStatus(
                liftSystem.getId(), RunStatus.CREATED);
        List<SimulationRun> runningRuns = runRepository.findByLiftSystemIdAndStatus(
                liftSystem.getId(), RunStatus.RUNNING);

        assertTrue(createdRuns.size() >= 1);
        assertTrue(runningRuns.size() >= 1);
        assertEquals(RunStatus.CREATED, createdRuns.get(0).getStatus());
        assertEquals(RunStatus.RUNNING, runningRuns.get(0).getStatus());
    }

    @Test
    public void testFindActiveRunsByLiftSystemId() {
        SimulationRun run1 = new SimulationRun(liftSystem, version);
        SimulationRun run2 = new SimulationRun(liftSystem, version);
        run2.start();
        SimulationRun run3 = new SimulationRun(liftSystem, version);
        run3.start();
        run3.succeed();

        entityManager.persist(run1);
        entityManager.persist(run2);
        entityManager.persist(run3);
        entityManager.flush();

        List<SimulationRun> activeRuns = runRepository.findActiveRunsByLiftSystemId(liftSystem.getId());

        assertTrue(activeRuns.size() >= 2);
        assertTrue(activeRuns.stream().allMatch(
                r -> r.getStatus() == RunStatus.CREATED || r.getStatus() == RunStatus.RUNNING));
    }

    @Test
    public void testCountActiveRunsByLiftSystemId() {
        SimulationRun createdRun = new SimulationRun(liftSystem, version);
        SimulationRun runningRun = new SimulationRun(liftSystem, version);
        runningRun.start();
        SimulationRun succeededRun = new SimulationRun(liftSystem, version);
        succeededRun.start();
        succeededRun.succeed();

        entityManager.persist(createdRun);
        entityManager.persist(runningRun);
        entityManager.persist(succeededRun);
        entityManager.flush();

        long activeCount = runRepository.countActiveRunsByLiftSystemId(liftSystem.getId());

        assertEquals(2, activeCount);
    }

    @Test
    public void testStartupRecoveryBulkUpdatesActiveRunsOnly() {
        SimulationRun createdRun = new SimulationRun(liftSystem, version);
        SimulationRun runningRun = new SimulationRun(liftSystem, version);
        runningRun.start();
        SimulationRun succeededRun = new SimulationRun(liftSystem, version);
        succeededRun.start();
        succeededRun.succeed();

        entityManager.persist(createdRun);
        entityManager.persist(runningRun);
        entityManager.persist(succeededRun);
        entityManager.flush();
        entityManager.clear();

        OffsetDateTime recoveredAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
        int failedRunning = runRepository.failOrphanedRunningRuns("Recovered after restart", recoveredAt);
        int cancelledCreated = runRepository.cancelOrphanedCreatedRuns(recoveredAt);
        entityManager.flush();
        entityManager.clear();

        assertEquals(1, failedRunning);
        assertEquals(1, cancelledCreated);

        SimulationRun recoveredRunning = runRepository.findById(runningRun.getId()).orElseThrow();
        assertEquals(RunStatus.FAILED, recoveredRunning.getStatus());
        assertEquals("Recovered after restart", recoveredRunning.getErrorMessage());
        assertEquals(recoveredAt, recoveredRunning.getEndedAt());

        SimulationRun recoveredCreated = runRepository.findById(createdRun.getId()).orElseThrow();
        assertEquals(RunStatus.CANCELLED, recoveredCreated.getStatus());
        assertEquals(recoveredAt, recoveredCreated.getEndedAt());

        SimulationRun untouchedSucceeded = runRepository.findById(succeededRun.getId()).orElseThrow();
        assertEquals(RunStatus.SUCCEEDED, untouchedSucceeded.getStatus());
    }

    @Test
    public void testCountByLiftSystemId() {
        SimulationRun run1 = new SimulationRun(liftSystem, version);
        SimulationRun run2 = new SimulationRun(liftSystem, version);

        entityManager.persist(run1);
        entityManager.persist(run2);
        entityManager.flush();

        long count = runRepository.countByLiftSystemId(liftSystem.getId());

        assertTrue(count >= 2);
    }

    @Test
    public void testCountByStatus() {
        SimulationRun run1 = new SimulationRun(liftSystem, version);
        SimulationRun run2 = new SimulationRun(liftSystem, version);

        entityManager.persist(run1);
        entityManager.persist(run2);
        entityManager.flush();

        long count = runRepository.countByStatus(RunStatus.CREATED);

        assertTrue(count >= 2);
    }

    @Test
    public void testDeleteRun() {
        SimulationRun run = new SimulationRun(liftSystem, version);
        entityManager.persist(run);
        entityManager.flush();

        Long id = run.getId();
        assertTrue(runRepository.existsById(id));

        runRepository.deleteById(id);

        assertFalse(runRepository.existsById(id));
    }

    @Test
    public void testCascadeDeleteFromLiftSystem() {
        SimulationRun run = new SimulationRun(liftSystem, version);
        entityManager.persist(run);
        entityManager.flush();

        Long runId = run.getId();
        assertTrue(runRepository.existsById(runId));

        // Clear the persistence context before deleting so Hibernate does not retain
        // the managed run while the database-level ON DELETE CASCADE removes it.
        Long liftSystemId = liftSystem.getId();
        entityManager.clear();

        LiftSystem persistedLiftSystem = entityManager.find(LiftSystem.class, liftSystemId);
        entityManager.remove(persistedLiftSystem);
        entityManager.flush();

        assertFalse(runRepository.existsById(runId));
    }

    @Test
    public void testCascadeDeleteFromLiftSystemVersion() {
        SimulationRun run1 = new SimulationRun(liftSystem, version);
        SimulationRun run2 = new SimulationRun(liftSystem, version);
        entityManager.persist(run1);
        entityManager.persist(run2);
        entityManager.flush();

        Long run1Id = run1.getId();
        Long run2Id = run2.getId();
        assertTrue(runRepository.existsById(run1Id));
        assertTrue(runRepository.existsById(run2Id));

        // Clear the persistence context before deleting so Hibernate does not retain
        // managed runs while the database-level ON DELETE CASCADE removes them.
        Long versionId = version.getId();
        entityManager.clear();

        // Delete the version - runs should cascade delete
        LiftSystemVersion persistedVersion = entityManager.find(LiftSystemVersion.class, versionId);
        entityManager.remove(persistedVersion);
        entityManager.flush();

        assertFalse(runRepository.existsById(run1Id));
        assertFalse(runRepository.existsById(run2Id));
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    public void testMarkRunningRunFailed_UpdatesRunningToFailed() {
        LiftSystem uniqueLiftSystem = new LiftSystem(
            "test-system-" + System.currentTimeMillis(),
            "Test System",
            "Test"
        );
        entityManager.persist(uniqueLiftSystem);
        LiftSystemVersion uniqueVersion = new LiftSystemVersion(uniqueLiftSystem, 1, "{\"lifts\": 2}");
        entityManager.persist(uniqueVersion);
        entityManager.flush();

        SimulationRun run = new SimulationRun(uniqueLiftSystem, uniqueVersion);
        run.start();
        runRepository.save(run);
        entityManager.flush();

        TestTransaction.flagForCommit();
        TestTransaction.end();

        OffsetDateTime failedAt = OffsetDateTime.now();
        int rowsUpdated = runRepository.markRunningRunFailed(
            run.getId(),
            "Test failure",
            failedAt
        );

        assertEquals(1, rowsUpdated);

        SimulationRun updated = runRepository.findById(run.getId()).orElseThrow();
        assertEquals(RunStatus.FAILED, updated.getStatus());
        assertEquals("Test failure", updated.getErrorMessage());
        assertNotNull(updated.getEndedAt());
        assertTrue(updated.getEndedAt().isAfter(failedAt.minusSeconds(1)),
                "EndedAt should be close to when markRunningRunFailed was called");
    }

    @Test
    public void testMarkRunningRunFailed_IgnoresNonRunningRun() {
        SimulationRun run = new SimulationRun(liftSystem, version);
        run.start();
        run.succeed();
        entityManager.persist(run);
        entityManager.flush();
        entityManager.clear();

        OffsetDateTime failedAt = OffsetDateTime.now();
        int rowsUpdated = runRepository.markRunningRunFailed(
            run.getId(),
            "Test failure",
            failedAt
        );

        assertEquals(0, rowsUpdated, "Should not update non-RUNNING run");

        SimulationRun unchanged = entityManager.find(SimulationRun.class, run.getId());
        assertEquals(RunStatus.SUCCEEDED, unchanged.getStatus());
    }

    @Test
    public void testMarkRunningRunFailed_IgnoresCancelledRun() {
        SimulationRun run = new SimulationRun(liftSystem, version);
        run.cancel();
        entityManager.persist(run);
        entityManager.flush();
        entityManager.clear();

        OffsetDateTime failedAt = OffsetDateTime.now();
        int rowsUpdated = runRepository.markRunningRunFailed(
            run.getId(),
            "Test failure",
            failedAt
        );

        assertEquals(0, rowsUpdated, "Should not update CANCELLED run");

        SimulationRun unchanged = entityManager.find(SimulationRun.class, run.getId());
        assertEquals(RunStatus.CANCELLED, unchanged.getStatus());
    }
}
