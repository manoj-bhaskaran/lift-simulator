package com.liftsimulator.admin.service;

import com.liftsimulator.admin.dto.ScenarioDefinitionDTO;
import com.liftsimulator.admin.dto.SimulationRunResponse;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Service for managing simulation runs.
 */
@Service
@Transactional(readOnly = true)
public class SimulationRunService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimulationRunService.class);

    private static final String STARTUP_RECOVERY_ERROR_MESSAGE =
            "Run was still RUNNING when the application started; marking it FAILED because no in-memory "
                    + "executor task can survive a JVM restart.";

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "createdAt",
            "startedAt",
            "endedAt",
            "status",
            "id"
    );

    @PersistenceContext
    private EntityManager entityManager;

    private final SimulationRunRepository runRepository;
    private final LiftSystemRepository liftSystemRepository;
    private final LiftSystemVersionRepository versionRepository;
    private final ScenarioRepository scenarioRepository;
    private final String artefactsBasePath;
    private final SimulationRunExecutionService executionService;
    private final ArtefactService artefactService;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring-managed services are injected and treated as shared dependencies."
    )
    public SimulationRunService(
            SimulationRunRepository runRepository,
            LiftSystemRepository liftSystemRepository,
            LiftSystemVersionRepository versionRepository,
            ScenarioRepository scenarioRepository,
            SimulationRunExecutionService executionService,
            ArtefactService artefactService,
            ObjectMapper objectMapper,
            @Value("${simulation.artefacts.base-path:./simulation-runs}") String artefactsBasePath) {
        this.runRepository = runRepository;
        this.liftSystemRepository = liftSystemRepository;
        this.versionRepository = versionRepository;
        this.scenarioRepository = scenarioRepository;
        this.executionService = executionService;
        this.artefactService = artefactService;
        this.objectMapper = objectMapper;
        this.artefactsBasePath = artefactsBasePath;
    }

    /**
     * Create a new simulation run.
     *
     * @param liftSystemId the lift system id
     * @param versionNumber the version number
     * @param scenarioId the scenario id (optional)
     * @return the created run
     * @throws ResourceNotFoundException if the lift system, version, or scenario is not found
     * @throws IllegalArgumentException if the version does not belong to the lift system
     *                                  or the scenario does not belong to the version
     */
    @Transactional
    public SimulationRun createRun(Long liftSystemId, Integer versionNumber, Long scenarioId) {
        LiftSystem liftSystem = liftSystemRepository.findById(liftSystemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lift system not found with id: " + liftSystemId));

        LiftSystemVersion version = versionRepository.findByLiftSystemIdAndVersionNumber(liftSystemId, versionNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lift system version not found with lift system id " + liftSystemId
                                + " and version number: " + versionNumber));

        SimulationRun run = new SimulationRun(liftSystem, version);

        // Set scenario if provided
        if (scenarioId != null) {
            Scenario scenario = scenarioRepository.findById(scenarioId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Scenario not found with id: " + scenarioId));

            // Validate that the scenario belongs to the same version
            if (!scenario.getLiftSystemVersion().getId().equals(version.getId())) {
                throw new IllegalArgumentException(
                        "Scenario " + scenarioId + " does not belong to version " + versionNumber
                                + " of lift system " + liftSystemId);
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
     * @param versionNumber the version number
     * @param scenarioId the scenario id (optional)
     * @param seed the random seed (optional, will be generated if not provided)
     * @return the started run
     * @throws ResourceNotFoundException if any of the entities is not found
     * @throws IllegalArgumentException if the version does not belong to the lift system
     *                                  or the scenario does not belong to the version
     * @throws IOException if artefact directory creation fails
     */
    @Transactional
    public SimulationRun createAndStartRun(Long liftSystemId, Integer versionNumber, Long scenarioId, Long seed)
            throws IOException {
        // Create the run
        SimulationRun run = createRun(liftSystemId, versionNumber, scenarioId);

        // Generate seed if not provided
        Long runSeed = seed != null ? seed : ThreadLocalRandom.current().nextLong();

        // Set up artefact directory
        String artefactPath = setupArtefactDirectory(run.getId());

        // Derive total ticks from scenario duration if scenario is provided
        Long totalTicks = null;
        if (scenarioId != null) {
            totalTicks = extractTotalTicksFromScenario(run.getScenario());
        }

        // Configure the run
        if (totalTicks != null) {
            run.setTotalTicks(totalTicks);
        }
        run.setSeed(runSeed);
        run.setArtefactBasePath(artefactPath);

        // Save configuration
        run = runRepository.save(run);

        // Start the run before submitting for async execution
        // This ensures the API returns RUNNING status immediately
        run.start();
        run = runRepository.save(run);

        // Submit the run for execution only after the surrounding transaction commits.
        // Otherwise, the async executor can start immediately and attempt to read the
        // run before the newly-created row is visible outside this transaction.
        submitRunForExecutionAfterCommit(run.getId());
        return run;
    }

    private void submitRunForExecutionAfterCommit(Long runId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            executionService.submitRunForExecution(runId);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                executionService.submitRunForExecution(runId);
            }
        });
    }

    private Long extractTotalTicksFromScenario(Scenario scenario) {
        if (scenario == null || scenario.getScenarioJson() == null) {
            return null;
        }

        try {
            ScenarioDefinitionDTO scenarioDefinition = objectMapper.readValue(
                    scenario.getScenarioJson(), ScenarioDefinitionDTO.class);
            if (scenarioDefinition.durationTicks() != null) {
                return scenarioDefinition.durationTicks().longValue();
            }
        } catch (JacksonException ex) {
            LOGGER.warn("Failed to extract durationTicks from scenario {}: {}", scenario.getId(), ex.getMessage());
        }
        return null;
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

    /** Maximum allowed page size to prevent excessively large result sets. */
    public static final int MAX_PAGE_SIZE = 100;

    /** Default page size when not specified by the caller. */
    public static final int DEFAULT_PAGE_SIZE = 20;

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
     * Get a page of runs with optional filtering by lift system ID and status.
     * Enforces a maximum page size of {@value #MAX_PAGE_SIZE}. Requests with a larger
     * page size are silently capped. Defaults to sorting by {@code createdAt} descending
     * when the caller does not supply an explicit sort. Sort properties are restricted
     * to {@code createdAt}, {@code startedAt}, {@code endedAt}, {@code status}, and {@code id}.
     *
     * @param liftSystemId optional lift system ID filter
     * @param status optional status filter
     * @param pageable pagination and sorting parameters supplied by the caller
     * @return page of matching runs with details
     */
    public Page<SimulationRun> getPagedRunsWithDetails(Long liftSystemId, RunStatus status, Pageable pageable) {
        Pageable effective = capPageSize(pageable);
        if (liftSystemId != null && status != null) {
            return runRepository.findByLiftSystemIdAndStatusWithDetails(liftSystemId, status, effective);
        } else if (liftSystemId != null) {
            return runRepository.findByLiftSystemIdWithDetails(liftSystemId, effective);
        } else if (status != null) {
            return runRepository.findByStatusWithDetails(status, effective);
        } else {
            return runRepository.findAllWithDetails(effective);
        }
    }

    /**
     * Returns a {@link Pageable} that is identical to the input but with the page size
     * capped at {@value #MAX_PAGE_SIZE}. Falls back to default sort (createdAt DESC)
     * when the caller provides no sort criteria. Rejects sort properties outside the
     * documented allowlist and ignore-case sort modifiers before they reach Spring Data
     * query construction.
     */
    private Pageable capPageSize(Pageable pageable) {
        Sort sort = pageable.getSort().isSorted()
                ? validateSort(pageable.getSort())
                : Sort.by(Sort.Direction.DESC, "createdAt");
        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }

    private Sort validateSort(Sort sort) {
        String allowedProperties = allowedSortPropertiesMessage();
        List<String> invalidProperties = sort.stream()
                .map(Sort.Order::getProperty)
                .filter(property -> !ALLOWED_SORT_PROPERTIES.contains(property))
                .distinct()
                .toList();
        if (!invalidProperties.isEmpty()) {
            throw new IllegalArgumentException("Unsupported simulation run sort property: "
                    + String.join(", ", invalidProperties)
                    + ". Allowed sort properties: " + allowedProperties);
        }

        List<String> ignoreCaseProperties = sort.stream()
                .filter(Sort.Order::isIgnoreCase)
                .map(Sort.Order::getProperty)
                .distinct()
                .toList();
        if (!ignoreCaseProperties.isEmpty()) {
            throw new IllegalArgumentException("Unsupported ignore-case simulation run sort modifier for: "
                    + String.join(", ", ignoreCaseProperties)
                    + ". Allowed sort properties: " + allowedProperties
                    + "; ignore-case sorting is not supported.");
        }
        return sort;
    }

    private String allowedSortPropertiesMessage() {
        return ALLOWED_SORT_PROPERTIES.stream()
                .sorted()
                .collect(Collectors.joining(", "));
    }

    /**
     * Get a run by its ID.
     *
     * @param id the run id
     * @return the run
     * @throws ResourceNotFoundException if the run is not found
     */
    public SimulationRun getRunById(Long id) {
        return findRunByIdWithDetails(id);
    }

    /**
     * Get a run response by its ID while the persistence context is still open.
     *
     * @param id the run id
     * @return the run response
     * @throws ResourceNotFoundException if the run is not found
     */
    public SimulationRunResponse getRunResponseById(Long id) {
        return SimulationRunResponse.fromEntity(findRunByIdWithDetails(id));
    }

    private SimulationRun findRunByIdWithDetails(Long id) {
        return runRepository.findByIdWithDetails(id)
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
     * which is important when called from async threads. Uses a targeted UPDATE
     * query to avoid overwriting concurrent status changes (e.g., cancel/fail/succeed).
     *
     * @param id the run id
     * @param currentTick the current tick number
     * @return the updated run
     * @throws ResourceNotFoundException if the run is not found
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SimulationRun updateProgress(Long id, Long currentTick) {
        int updated = runRepository.updateCurrentTick(id, currentTick);
        if (updated == 0) {
            throw new ResourceNotFoundException("Simulation run not found with id: " + id);
        }
        // Clear the persistence context to ensure fresh read from database
        entityManager.clear();
        // Retrieve the updated entity from database
        SimulationRun run = getRunById(id);
        return run;
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
     * Reconciles non-terminal runs that cannot have a live executor task after application startup.
     * RUNNING runs are marked FAILED because their in-memory worker was lost with the previous JVM,
     * while never-submitted CREATED runs are marked CANCELLED so they can be cleaned up.
     *
     * @return counts of recovered RUNNING and CREATED runs
     */
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

    /**
     * Result counts for startup reconciliation of orphaned simulation runs.
     *
     * @param failedRunningRuns number of RUNNING rows marked FAILED
     * @param cancelledCreatedRuns number of CREATED rows marked CANCELLED
     */
    public record StartupRecoveryResult(int failedRunningRuns, int cancelledCreatedRuns) {
    }

    /**
     * Delete a completed simulation run, removing both its database record and any
     * stored artefact files.
     *
     * <p>Only runs in a terminal state (SUCCEEDED, FAILED, CANCELLED) may be deleted;
     * attempting to delete an in-progress run (CREATED or RUNNING) fails fast so that
     * an active execution is never orphaned.</p>
     *
     * <p>The database record is deleted first, and artefacts are removed only after
     * the surrounding transaction commits. This prevents a rollback or commit
     * failure from leaving a surviving run row with permanently deleted files.</p>
     *
     * @param id the run id
     * @throws ResourceNotFoundException if the run is not found
     * @throws IllegalStateException if the run is not in a terminal state
     * @throws ArtefactDeletionException if the stored artefacts cannot be removed
     */
    @Transactional
    public void deleteRun(Long id) {
        SimulationRun run = getRunById(id);
        RunStatus status = run.getStatus();
        if (status != RunStatus.SUCCEEDED && status != RunStatus.FAILED && status != RunStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Cannot delete a simulation run in " + status + " state. "
                            + "Only completed runs (SUCCEEDED, FAILED, CANCELLED) can be deleted.");
        }

        runRepository.deleteById(id);
        deleteArtefactsAfterCommit(run, id);
    }

    private void deleteArtefactsAfterCommit(SimulationRun run, Long id) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteArtefacts(run, id);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteArtefacts(run, id);
            }
        });
    }

    private void deleteArtefacts(SimulationRun run, Long id) {
        try {
            artefactService.deleteArtefacts(run);
        } catch (IOException e) {
            throw new ArtefactDeletionException(
                    "Failed to delete artefacts for simulation run " + id + ": " + e.getMessage(), e);
        }
    }
}
