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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "createdAt",
            "startedAt",
            "endedAt",
            "status",
            "id"
    );

    private final SimulationRunRepository runRepository;
    private final LiftSystemRepository liftSystemRepository;
    private final LiftSystemVersionRepository versionRepository;
    private final ScenarioRepository scenarioRepository;
    private final String artefactsBasePath;
    private final SimulationRunExecutionService executionService;
    private final ArtefactService artefactService;
    private final RunLifecycleManager lifecycleManager;
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
            RunLifecycleManager lifecycleManager,
            ObjectMapper objectMapper,
            @Value("${simulation.artefacts.base-path:./simulation-runs}") String artefactsBasePath) {
        this.runRepository = runRepository;
        this.liftSystemRepository = liftSystemRepository;
        this.versionRepository = versionRepository;
        this.scenarioRepository = scenarioRepository;
        this.executionService = executionService;
        this.artefactService = artefactService;
        this.lifecycleManager = lifecycleManager;
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
        run = lifecycleManager.configureRun(run.getId(), totalTicks, runSeed, artefactPath);

        // Start the run before submitting for async execution
        // This ensures the API returns RUNNING status immediately
        run = lifecycleManager.startRun(run.getId());

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
        return lifecycleManager.getByIdWithDetails(id);
    }

    /**
     * Get a run response by its ID while the persistence context is still open.
     *
     * @param id the run id
     * @return the run response
     * @throws ResourceNotFoundException if the run is not found
     */
    public SimulationRunResponse getRunResponseById(Long id) {
        return SimulationRunResponse.fromEntity(lifecycleManager.getByIdWithDetails(id));
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
     *
     * @param id the run id
     * @return the updated run
     */
    @Transactional
    public SimulationRun startRun(Long id) {
        return lifecycleManager.startRun(id);
    }

    /**
     * Mark a simulation run as succeeded.
     *
     * @param id the run id
     * @return the updated run
     */
    @Transactional
    public SimulationRun succeedRun(Long id) {
        return lifecycleManager.succeedRun(id);
    }

    /**
     * Mark a simulation run as failed.
     *
     * @param id the run id
     * @param errorMessage the error message describing the failure
     * @return the updated run
     */
    @Transactional
    public SimulationRun failRun(Long id, String errorMessage) {
        return lifecycleManager.failRun(id, errorMessage);
    }

    /**
     * Cancel a simulation run.
     *
     * @param id the run id
     * @return the updated run
     */
    @Transactional
    public SimulationRun cancelRun(Long id) {
        return lifecycleManager.cancelRun(id);
    }

    /**
     * Update the progress of a running simulation.
     *
     * @param id the run id
     * @param currentTick the current tick number
     * @return the updated run
     */
    public SimulationRun updateProgress(Long id, Long currentTick) {
        return lifecycleManager.updateProgress(id, currentTick);
    }

    /**
     * Set configuration for a run before starting.
     *
     * @param id the run id
     * @param totalTicks total number of ticks for the simulation (null to keep existing)
     * @param seed random seed for reproducibility (null to keep existing)
     * @param artefactBasePath base path for output artefacts (null to keep existing)
     * @return the updated run
     */
    @Transactional
    public SimulationRun configureRun(Long id, Long totalTicks, Long seed, String artefactBasePath) {
        return lifecycleManager.configureRun(id, totalTicks, seed, artefactBasePath);
    }

    /**
     * Reconciles non-terminal runs that cannot have a live executor task after application startup.
     *
     * @return counts of recovered RUNNING and CREATED runs
     */
    @Transactional
    public RunLifecycleManager.StartupRecoveryResult recoverOrphanedRunsOnStartup() {
        return lifecycleManager.recoverOrphanedRunsOnStartup();
    }

    /**
     * Delete a completed simulation run, removing both its database record and any
     * stored artefact files on a best-effort basis.
     *
     * <p>Only runs in a terminal state (SUCCEEDED, FAILED, CANCELLED) may be deleted;
     * attempting to delete an in-progress run (CREATED or RUNNING) fails fast so that
     * an active execution is never orphaned.</p>
     *
     * <p>The database record is deleted first, and artefacts are removed only after
     * the surrounding transaction commits. This prevents a rollback or commit
     * failure from leaving a surviving run row with permanently deleted files.
     * Post-commit artefact deletion failures are logged and do not fail the
     * already-committed delete request.</p>
     *
     * @param id the run id
     * @throws ResourceNotFoundException if the run is not found
     * @throws IllegalStateException if the run is not in a terminal state
     */
    @Transactional
    public void deleteRun(Long id) {
        SimulationRun run = lifecycleManager.getByIdWithDetails(id);
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
            LOGGER.warn(
                    "Best-effort artefact deletion failed for simulation run {} at {}",
                    id,
                    run.getArtefactBasePath(),
                    e
            );
        }
    }
}
