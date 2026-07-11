package com.liftsimulator.admin.service;

import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.repository.SimulationRunRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Single service responsible for simulation-run lifecycle transitions.
 */
@Service
@Transactional(readOnly = true)
public class RunLifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunLifecycleManager.class);

    private static final String STARTUP_RECOVERY_ERROR_MESSAGE =
            "Run was still RUNNING when the application started; marking it FAILED because no in-memory "
                    + "executor task can survive a JVM restart.";

    private final SimulationRunRepository runRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public RunLifecycleManager(SimulationRunRepository runRepository) {
        this.runRepository = runRepository;
    }

    public SimulationRun getByIdWithDetails(Long runId) {
        return runRepository.findByIdWithDetails(runId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Simulation run not found with id: " + runId));
    }

    @Transactional
    public SimulationRun configureRun(Long runId, Long totalTicks, Long seed, String artefactBasePath) {
        SimulationRun run = getByIdWithDetails(runId);
        if (totalTicks != null) {
            run.setTotalTicks(totalTicks);
        }
        if (seed != null) {
            run.setSeed(seed);
        }
        if (artefactBasePath != null) {
            run.setArtefactBasePath(artefactBasePath);
        }
        return runRepository.save(run);
    }

    @Transactional
    public SimulationRun startRun(Long runId) {
        SimulationRun run = getByIdWithDetails(runId);
        run.start();
        return runRepository.save(run);
    }

    @Transactional
    public SimulationRun succeedRun(Long runId) {
        SimulationRun run = getByIdWithDetails(runId);
        run.succeed();
        return runRepository.save(run);
    }

    @Transactional
    public SimulationRun failRun(Long runId, String message) {
        SimulationRun run = getByIdWithDetails(runId);
        run.fail(message);
        return runRepository.save(run);
    }

    @Transactional
    public SimulationRun cancelRun(Long runId) {
        SimulationRun run = getByIdWithDetails(runId);
        run.cancel();
        return runRepository.save(run);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SimulationRun updateProgress(Long runId, Long currentTick) {
        int updated = runRepository.updateCurrentTick(runId, currentTick);
        if (updated == 0) {
            throw new ResourceNotFoundException("Simulation run not found with id: " + runId);
        }
        entityManager.clear();
        return getByIdWithDetails(runId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int markRunningRunFailedSafely(Long runId, String message, IllegalStateException transitionFailure) {
        int updated = runRepository.markRunningRunFailed(runId, message, OffsetDateTime.now());
        if (updated > 0) {
            LOGGER.warn(
                    "Recovered run {} by marking RUNNING run as FAILED after lifecycle transition failure",
                    runId,
                    transitionFailure
            );
            return updated;
        }

        try {
            SimulationRun run = getByIdWithDetails(runId);
            LOGGER.warn(
                    "Run {} was not marked FAILED because its current status is {}; preserving existing lifecycle state",
                    runId,
                    run.getStatus(),
                    transitionFailure
            );
        } catch (Exception lookupFailure) {
            LOGGER.error("Failed to inspect run {} after failure transition error", runId, lookupFailure);
        }
        return updated;
    }

    @Transactional
    public StartupRecoveryResult recoverOrphanedRunsOnStartup() {
        OffsetDateTime recoveredAt = OffsetDateTime.now();
        int failedRunningRuns = runRepository.failOrphanedRunningRuns(
                STARTUP_RECOVERY_ERROR_MESSAGE,
                recoveredAt
        );
        int cancelledCreatedRuns = runRepository.cancelOrphanedCreatedRuns(recoveredAt);

        if (failedRunningRuns > 0 || cancelledCreatedRuns > 0) {
            LOGGER.warn(
                    "Recovered orphaned simulation runs on startup: {} RUNNING marked FAILED, "
                            + "{} CREATED marked CANCELLED",
                    failedRunningRuns,
                    cancelledCreatedRuns
            );
        }

        return new StartupRecoveryResult(failedRunningRuns, cancelledCreatedRuns);
    }

    public record StartupRecoveryResult(int failedRunningRuns, int cancelledCreatedRuns) {
    }
}
