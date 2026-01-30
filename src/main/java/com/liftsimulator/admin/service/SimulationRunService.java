package com.liftsimulator.admin.service;

import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.entity.Scenario;
import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.entity.SimulationRun.RunStatus;
import com.liftsimulator.admin.repository.LiftSystemRepository;
import com.liftsimulator.admin.repository.LiftSystemVersionRepository;
import com.liftsimulator.admin.repository.ScenarioRepository;
import com.liftsimulator.admin.repository.SimulationRunRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service for managing simulation runs.
 */
@Service
@Transactional(readOnly = true)
public class SimulationRunService {

    private final SimulationRunRepository runRepository;
    private final LiftSystemRepository liftSystemRepository;
    private final LiftSystemVersionRepository versionRepository;
    private final ScenarioRepository scenarioRepository;
    private final String artefactsBasePath;
    private final SimulationRunExecutionService executionService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring-managed services are injected and treated as shared dependencies."
    )
    public SimulationRunService(
            SimulationRunRepository runRepository,
            LiftSystemRepository liftSystemRepository,
            LiftSystemVersionRepository versionRepository,
            ScenarioRepository scenarioRepository,
            @Lazy SimulationRunExecutionService executionService,
            @Value("${simulation.artefacts.base-path:./simulation-runs}") String artefactsBasePath) {
        this.runRepository = runRepository;
        this.liftSystemRepository = liftSystemRepository;
        this.versionRepository = versionRepository;
        this.scenarioRepository = scenarioRepository;
        this.executionService = executionService;
        this.artefactsBasePath = artefactsBasePath;
    }

    /**
     * Create a new simulation run.
     *
     * @param liftSystemId the lift system id
     * @param versionId the version id
     * @param scenarioId the scenario id (optional)
     * @return the created run
     * @throws ResourceNotFoundException if the lift system, version, or scenario is not found
     * @throws IllegalArgumentException if the version does not belong to the lift system
     *                                  or the scenario does not belong to the version
     */
    @Transactional
    public SimulationRun createRun(Long liftSystemId, Long versionId, Long scenarioId) {
        LiftSystem liftSystem = liftSystemRepository.findById(liftSystemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lift system not found with id: " + liftSystemId));

        LiftSystemVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lift system version not found with id: " + versionId));

        if (!version.getLiftSystem().getId().equals(liftSystemId)) {
            throw new IllegalArgumentException(
                    "Version " + versionId + " does not belong to lift system " + liftSystemId);
        }

        SimulationRun run = new SimulationRun(liftSystem, version);

        // Set scenario if provided
        if (scenarioId != null) {
            Scenario scenario = scenarioRepository.findById(scenarioId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Scenario not found with id: " + scenarioId));

            // Validate that the scenario belongs to the same version
            if (!scenario.getLiftSystemVersion().getId().equals(versionId)) {
                throw new IllegalArgumentException(
                        "Scenario " + scenarioId + " does not belong to version " + versionId);
            }

            run.setScenario(scenario);
        }

        return runRepository.save(run);
    }

    /**
     * Create and immediately start a simulation run.
     * This method creates the run, sets up the artefact directory, configures it, and starts execution.
     *
     * @param liftSystemId the lift system id
     * @param versionId the version id
     * @param scenarioId the scenario id (optional)
     * @param seed the random seed (optional, will be generated if not provided)
     * @return the started run
     * @throws ResourceNotFoundException if any of the entities is not found
     * @throws IllegalArgumentException if the version does not belong to the lift system
     *                                  or the scenario does not belong to the version
     * @throws IOException if artefact directory creation fails
     */
    @Transactional
    public SimulationRun createAndStartRun(Long liftSystemId, Long versionId, Long scenarioId, Long seed)
            throws IOException {
        // Create the run
        SimulationRun run = createRun(liftSystemId, versionId, scenarioId);

        // Generate seed if not provided
        Long runSeed = seed != null ? seed : ThreadLocalRandom.current().nextLong();

        // Set up artefact directory
        String artefactPath = setupArtefactDirectory(run.getId());

        // Configure the run with default total ticks (this can be updated later)
        run.setTotalTicks(10000L); // Default value, can be configured
        run.setSeed(runSeed);
        run.setArtefactBasePath(artefactPath);

        // Save configuration
        run = runRepository.save(run);

        // Start the run before submitting for async execution
        // This ensures the API returns RUNNING status immediately
        run.start();
        run = runRepository.save(run);

        // Submit the run for execution
        // The execution service will handle the actual simulation
        executionService.submitRunForExecution(run.getId());
        return run;
    }

    /**
     * Sets up the artefact directory for a simulation run.
     *
     * @param runId the run ID
     * @return the absolute path to the artefact directory
     * @throws IOException if directory creation fails
     */
    private String setupArtefactDirectory(Long runId) throws IOException {
        Path baseDir = Paths.get(artefactsBasePath);
        Path runDir = baseDir.resolve("run-" + runId);

        Files.createDirectories(runDir);

        return runDir.toAbsolutePath().toString();
    }

    /**
     * Get all simulation runs.
     *
     * @return list of all runs
     */
    public List<SimulationRun> getAllRuns() {
        return runRepository.findAll();
    }

    /**
     * Get all simulation runs with their related entities (lift system, version, scenario).
     *
     * @return list of all runs with details, ordered by creation date descending
     */
    public List<SimulationRun> getAllRunsWithDetails() {
        return runRepository.findAllWithDetails();
    }

    /**
     * Get runs with optional filtering by lift system ID and status.
     * Returns runs with their related entities eagerly loaded.
     *
     * @param liftSystemId optional lift system ID filter
     * @param status optional status filter
     * @return list of matching runs with details, ordered by creation date descending
     */
    public List<SimulationRun> getRunsWithDetails(Long liftSystemId, RunStatus status) {
        if (liftSystemId != null && status != null) {
            return runRepository.findByLiftSystemIdAndStatusWithDetails(liftSystemId, status);
        } else if (liftSystemId != null) {
            return runRepository.findByLiftSystemIdWithDetails(liftSystemId);
        } else if (status != null) {
            return runRepository.findByStatusWithDetails(status);
        } else {
            return runRepository.findAllWithDetails();
        }
    }

    /**
     * Get a run by its ID.
     *
     * @param id the run id
     * @return the run
     * @throws ResourceNotFoundException if the run is not found
     */
    public SimulationRun getRunById(Long id) {
        return runRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Simulation run not found with id: " + id));
    }

    /**
     * Get all runs for a specific lift system.
     *
     * @param liftSystemId the lift system id
     * @return list of runs ordered by creation date descending
     */
    public List<SimulationRun> getRunsByLiftSystem(Long liftSystemId) {
        return runRepository.findByLiftSystemIdOrderByCreatedAtDesc(liftSystemId);
    }

    /**
     * Get all runs with a specific status.
     *
     * @param status the run status
     * @return list of runs ordered by creation date descending
     */
    public List<SimulationRun> getRunsByStatus(RunStatus status) {
        return runRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    /**
     * Get all active (CREATED or RUNNING) runs for a lift system.
     *
     * @param liftSystemId the lift system id
     * @return list of active runs ordered by creation date descending
     */
    public List<SimulationRun> getActiveRunsByLiftSystem(Long liftSystemId) {
        return runRepository.findActiveRunsByLiftSystemId(liftSystemId);
    }

    /**
     * Start a simulation run.
     * Transitions from CREATED to RUNNING.
     *
     * @param id the run id
     * @return the updated run
     * @throws ResourceNotFoundException if the run is not found
     * @throws IllegalStateException if the run is not in CREATED state
     */
    @Transactional
    public SimulationRun startRun(Long id) {
        SimulationRun run = getRunById(id);
        run.start();
        return runRepository.save(run);
    }

    /**
     * Mark a simulation run as succeeded.
     * Transitions from RUNNING to SUCCEEDED.
     *
     * @param id the run id
     * @return the updated run
     * @throws ResourceNotFoundException if the run is not found
     * @throws IllegalStateException if the run is not in RUNNING state
     */
    @Transactional
    public SimulationRun succeedRun(Long id) {
        SimulationRun run = getRunById(id);
        run.succeed();
        return runRepository.save(run);
    }

    /**
     * Mark a simulation run as failed.
     * Transitions from RUNNING to FAILED.
     *
     * @param id the run id
     * @param errorMessage the error message describing the failure
     * @return the updated run
     * @throws ResourceNotFoundException if the run is not found
     * @throws IllegalStateException if the run is not in RUNNING state
     */
    @Transactional
    public SimulationRun failRun(Long id, String errorMessage) {
        SimulationRun run = getRunById(id);
        run.fail(errorMessage);
        return runRepository.save(run);
    }

    /**
     * Cancel a simulation run.
     * Can transition from CREATED or RUNNING to CANCELLED.
     *
     * @param id the run id
     * @return the updated run
     * @throws ResourceNotFoundException if the run is not found
     * @throws IllegalStateException if the run is not in CREATED or RUNNING state
     */
    @Transactional
    public SimulationRun cancelRun(Long id) {
        SimulationRun run = getRunById(id);
        run.cancel();
        return runRepository.save(run);
    }

    /**
     * Update the progress of a running simulation.
     * Uses REQUIRES_NEW propagation to ensure a fresh transaction is created,
     * which is important when called from async threads. Uses saveAndFlush
     * to ensure the update is immediately written to the database for
     * visibility by other transactions (e.g., polling requests).
     *
     * @param id the run id
     * @param currentTick the current tick number
     * @return the updated run
     * @throws ResourceNotFoundException if the run is not found
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SimulationRun updateProgress(Long id, Long currentTick) {
        SimulationRun run = getRunById(id);
        run.setCurrentTick(currentTick);
        return runRepository.saveAndFlush(run);
    }

    /**
     * Set configuration for a run before starting.
     * Only updates non-null values, preserving existing configuration.
     *
     * @param id the run id
     * @param totalTicks total number of ticks for the simulation (null to keep existing)
     * @param seed random seed for reproducibility (null to keep existing)
     * @param artefactBasePath base path for output artefacts (null to keep existing)
     * @return the updated run
     * @throws ResourceNotFoundException if the run is not found
     */
    @Transactional
    public SimulationRun configureRun(Long id, Long totalTicks, Long seed, String artefactBasePath) {
        SimulationRun run = getRunById(id);
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

    /**
     * Delete a simulation run.
     *
     * @param id the run id
     * @throws ResourceNotFoundException if the run is not found
     */
    @Transactional
    public void deleteRun(Long id) {
        if (!runRepository.existsById(id)) {
            throw new ResourceNotFoundException("Simulation run not found with id: " + id);
        }
        runRepository.deleteById(id);
    }
}
