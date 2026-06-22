package com.liftsimulator.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.liftsimulator.admin.dto.ConfigValidationResponse;
import com.liftsimulator.admin.dto.LiftConfigDTO;
import com.liftsimulator.admin.dto.PassengerFlowDTO;
import com.liftsimulator.admin.dto.ScenarioDefinitionDTO;
import com.liftsimulator.admin.dto.ScenarioValidationResponse;
import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.repository.SimulationRunRepository;
import com.liftsimulator.admin.service.metrics.RunMetrics;
import com.liftsimulator.domain.Direction;
import com.liftsimulator.domain.LiftRequest;
import com.liftsimulator.engine.ControllerFactory;
import com.liftsimulator.engine.RequestManagingLiftController;
import com.liftsimulator.engine.SimulationEngine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PreDestroy;

/**
 * Service to run simulations asynchronously and persist run artefacts.
 */
@Service
public class SimulationRunExecutionService {
    private static final Logger logger = LoggerFactory.getLogger(SimulationRunExecutionService.class);
    private static final int PROGRESS_UPDATE_INTERVAL = 5;
    private static final String CONFIG_FILE_NAME = "config.json";
    private static final String SCENARIO_FILE_NAME = "scenario.json";
    private static final String INPUT_SCENARIO_FILE_NAME = "input.scenario";
    private static final String LOG_FILE_NAME = "run.log";
    private static final String RESULTS_FILE_NAME = "results.json";

    private final SimulationRunRepository runRepository;
    private final ConfigValidationService configValidationService;
    private final ScenarioValidationService scenarioValidationService;
    private final BatchInputGenerator batchInputGenerator;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;
    private final Map<Long, Future<?>> runningTasks = new ConcurrentHashMap<>();
    private final Map<Long, CancellationToken> cancellationTokens = new ConcurrentHashMap<>();

    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Injected dependencies and configuration managed by Spring."
    )
    public SimulationRunExecutionService(
            SimulationRunRepository runRepository,
            ConfigValidationService configValidationService,
            ScenarioValidationService scenarioValidationService,
            BatchInputGenerator batchInputGenerator,
            ObjectMapper objectMapper) {
        this.runRepository = runRepository;
        this.configValidationService = configValidationService;
        this.scenarioValidationService = scenarioValidationService;
        this.batchInputGenerator = batchInputGenerator;
        this.objectMapper = objectMapper;
        this.executor = Executors.newCachedThreadPool(new SimulationRunnerThreadFactory());
    }

    /**
     * Submits an existing simulation run for asynchronous execution.
     * The run should be in CREATED state with configuration already set.
     *
     * @param runId the run id to execute
     */
    @Transactional
    public void submitRunForExecution(Long runId) {
        SimulationRun run = getRunById(runId);
        String configJson = run.getVersion().getConfig();
        String scenarioJson = run.getScenario() != null ? run.getScenario().getScenarioJson() : null;
        String scenarioName = run.getScenario() != null ? run.getScenario().getName() : null;

        RunExecutionRequest request = new RunExecutionRequest(run.getId(), configJson, scenarioJson, scenarioName);
        submitExecution(request);
    }

    /**
     * Cancels a running simulation and interrupts execution.
     *
     * @param runId the run id
     * @return the updated run
     */
    @Transactional
    public SimulationRun cancelRun(Long runId) {
        SimulationRun run = cancelRunStatus(runId);
        CancellationToken token = cancellationTokens.computeIfAbsent(runId, id -> new CancellationToken());
        token.cancel();
        return run;
    }

    /**
     * Indicates whether any simulation run is currently submitted or executing.
     *
     * <p>Runs are tracked from submission until their asynchronous execution
     * completes (including artefact writing and lifecycle finalisation). This is
     * primarily used to let integration tests wait for background executions to
     * drain before they reset shared database state, preventing a concurrent
     * {@code UPDATE} from the runner thread from colliding with cleanup deletes.</p>
     *
     * @return {@code true} if at least one run is in flight
     */
    public boolean hasActiveRuns() {
        return !runningTasks.isEmpty();
    }

    private void executeRun(RunExecutionRequest request) {
        CancellationToken cancelToken = cancellationTokens.computeIfAbsent(request.runId(), id -> new CancellationToken());
        boolean started = false;

        SimulationRun runEntity;
        try {
            runEntity = getRunById(request.runId());
        } catch (Exception ex) {
            logger.error("Failed to load run {} for execution", request.runId(), ex);
            return;
        }

        String artefactPath = runEntity.getArtefactBasePath();
        if (artefactPath == null || artefactPath.isBlank()) {
            logger.error("Run {} has no artefact path configured; cannot execute", request.runId());
            failRunWithMessage(request.runId(), "Artefact path not configured for run.", false);
            return;
        }

        Path runDir = Paths.get(artefactPath);
        Path logPath = runDir.resolve(LOG_FILE_NAME);
        logger.info("Run {} using artefact directory: {}", request.runId(), runDir);

        try (BufferedWriter logWriter = Files.newBufferedWriter(logPath, StandardCharsets.UTF_8)) {
            log(logWriter, "Run directory: " + runDir.toAbsolutePath());

            if (request.scenarioJson() == null) {
                log(logWriter, "Missing scenario payload for run " + request.runId());
                failRunWithMessage(request.runId(), logWriter, "Missing scenario payload for run.", false);
                writeResults(request.runId(), runDir, null, null, null, "FAILED", "Missing scenario payload for run.");
                return;
            }

            ConfigValidationResponse configValidation = configValidationService.validate(request.configJson());
            if (!configValidation.valid()) {
                log(logWriter, "Configuration validation failed: " + configValidation.errors());
                failRunWithMessage(request.runId(), logWriter, "Invalid configuration payload.", false);
                writeResults(request.runId(), runDir, null, null, null, "FAILED", "Invalid configuration payload.");
                return;
            }

            JsonNode scenarioNode = objectMapper.readTree(request.scenarioJson());
            ScenarioValidationResponse scenarioValidation = scenarioValidationService.validate(scenarioNode);
            if (!scenarioValidation.valid()) {
                log(logWriter, "Scenario validation failed: " + scenarioValidation.errors());
                failRunWithMessage(request.runId(), logWriter, "Invalid scenario payload.", false);
                writeResults(request.runId(), runDir, null, null, null, "FAILED", "Invalid scenario payload.");
                return;
            }

            LiftConfigDTO config = objectMapper.readValue(request.configJson(), LiftConfigDTO.class);
            ScenarioDefinitionDTO scenario = objectMapper.readerFor(ScenarioDefinitionDTO.class)
                .readValue(scenarioNode);

            if (scenario.durationTicks() == null) {
                log(logWriter, "Scenario has null durationTicks");
                SimulationRun runAtCheck = getRunById(request.runId());
                boolean runAlreadyStarted = runAtCheck.getStatus() == SimulationRun.RunStatus.RUNNING;
                failRunWithMessage(request.runId(), logWriter, "Scenario must have a valid durationTicks value.", runAlreadyStarted);
                writeResults(request.runId(), runDir, null, null, null, "FAILED", "Scenario must have a valid durationTicks value.");
                return;
            }

            writeInputFiles(request, runDir, logWriter);

            configureRun(
                request.runId(),
                scenario.durationTicks().longValue(),
                scenario.seed() != null ? scenario.seed().longValue() : null,
                null
            );

            // Start the run if not already started (it may have been started by createAndStartRun)
            SimulationRun currentRun = getRunById(request.runId());
            if (currentRun.getStatus() == SimulationRun.RunStatus.CANCELLED || cancelToken.isCancelled()) {
                log(logWriter, "Run cancelled before start for run " + request.runId());
                cancelRunSafely(request.runId(), logWriter, "Run cancelled before start.");
                writeResults(request.runId(), runDir, config, scenario, null, "CANCELLED", "Run cancelled before start.");
                return;
            }
            if (currentRun.getStatus() != SimulationRun.RunStatus.RUNNING) {
                startRun(request.runId());
            }
            started = true;
            updateProgress(request.runId(), 0L);
            log(logWriter, "Simulation started for run " + request.runId());

            RunMetrics metrics = runSimulation(request.runId(), config, scenario, logWriter, cancelToken);

            if (cancelToken.isCancelled() || getRunById(request.runId()).getStatus() == SimulationRun.RunStatus.CANCELLED) {
                log(logWriter, "Run cancelled for run " + request.runId());
                writeResults(request.runId(), runDir, config, scenario, metrics, "CANCELLED", "Run cancelled.");
                return;
            }
            succeedRun(request.runId());
            log(logWriter, "Simulation succeeded for run " + request.runId());
            writeResults(request.runId(), runDir, config, scenario, metrics, "SUCCEEDED", null);
        } catch (RunCancelledException ex) {
            try {
                cancelRunSafely(request.runId(), logPath, ex.getMessage());
                writeResults(request.runId(), runDir, ex.getConfig(), ex.getScenario(), ex.getMetrics(), "CANCELLED", ex.getMessage());
            } catch (IOException ioEx) {
                logger.warn("Failed to write cancellation results file for run {}", request.runId(), ioEx);
            }
        } catch (IOException | RuntimeException ex) {
            String errorMessage = safeMessage(ex);
            logger.error("Simulation run {} failed", request.runId(), ex);
            failRunWithMessage(request.runId(), logPath, errorMessage, started);
            try {
                writeResults(request.runId(), runDir, null, null, null, "FAILED", errorMessage);
            } catch (IOException ioEx) {
                logger.warn("Failed to write results file for run {}", request.runId(), ioEx);
            }
        } finally {
            runningTasks.remove(request.runId());
            cancellationTokens.remove(request.runId());
        }
    }

    private RunMetrics runSimulation(Long runId,
                                     LiftConfigDTO config,
                                     ScenarioDefinitionDTO scenario,
                                     BufferedWriter logWriter,
                                     CancellationToken cancelToken) throws IOException {
        RequestManagingLiftController controller = (RequestManagingLiftController) ControllerFactory.createController(
            config.controllerStrategy(),
            config.homeFloor(),
            config.idleTimeoutTicks(),
            config.idleParkingMode()
        );

        SimulationEngine engine = SimulationEngine.builder(controller, config.minFloor(), config.maxFloor())
            .initialFloor(config.homeFloor())
            .travelTicksPerFloor(config.travelTicksPerFloor())
            .doorTransitionTicks(config.doorTransitionTicks())
            .doorDwellTicks(config.doorDwellTicks())
            .doorReopenWindowTicks(config.doorReopenWindowTicks())
            .build();

        Map<Integer, List<PassengerFlowDTO>> flowsByTick = groupFlowsByTick(scenario);
        RunMetrics metrics = new RunMetrics(config.minFloor(), config.maxFloor());

        log(logWriter, "Starting simulation for " + scenario.durationTicks() + " ticks.");
        for (int tick = 0; tick < scenario.durationTicks(); tick++) {
            if (cancelToken.isCancelled() || Thread.currentThread().isInterrupted()) {
                throw new RunCancelledException("Run cancelled at tick " + engine.getCurrentTick() + ".",
                    metrics,
                    config,
                    scenario);
            }
            long currentTick = engine.getCurrentTick();
            List<PassengerFlowDTO> flows = flowsByTick.getOrDefault((int) currentTick, List.of());
            for (PassengerFlowDTO flow : flows) {
                int passengers = flow.passengers() != null ? flow.passengers() : 1;
                metrics.recordPassengerFlow(flow, passengers);

                // Determine direction based on origin and destination floors
                Direction direction;
                if (flow.destinationFloor() > flow.originFloor()) {
                    direction = Direction.UP;
                } else if (flow.destinationFloor() < flow.originFloor()) {
                    direction = Direction.DOWN;
                } else {
                    // Same floor - skip this flow as it doesn't require lift movement
                    continue;
                }

                // One hall call per flow; passenger count is carried on the request for KPI accounting
                LiftRequest request = LiftRequest.hallCall(flow.originFloor(), direction, passengers);
                metrics.recordRequestCreation(request, currentTick);
                controller.addRequest(request);
            }

            metrics.recordLiftState(engine.getCurrentState());
            metrics.recordActiveRequests(controller.getRequests(), currentTick);
            engine.tick();
            metrics.recordTerminalRequests(currentTick);

            long progressTick = tick + 1L;
            if (tick % PROGRESS_UPDATE_INTERVAL == 0) {
                updateProgress(runId, progressTick);
            }
        }

        updateProgress(runId, (long) scenario.durationTicks());
        log(logWriter, "Simulation completed at tick " + engine.getCurrentTick());
        metrics.recordTerminalRequests(engine.getCurrentTick());
        return metrics;
    }

    private Map<Integer, List<PassengerFlowDTO>> groupFlowsByTick(ScenarioDefinitionDTO scenario) {
        if (scenario.passengerFlows() == null || scenario.passengerFlows().isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Integer, List<PassengerFlowDTO>> flowsByTick = new HashMap<>(scenario.passengerFlows().size());
        for (PassengerFlowDTO flow : scenario.passengerFlows()) {
            int tick = flow.startTick() != null ? flow.startTick() : 0;
            flowsByTick.computeIfAbsent(tick, key -> new ArrayList<>()).add(flow);
        }
        return flowsByTick;
    }

    private SimulationRun getRunById(Long runId) {
        return runRepository.findById(runId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Simulation run not found with id: " + runId));
    }

    private SimulationRun configureRun(Long runId, Long totalTicks, Long seed, String artefactBasePath) {
        SimulationRun run = getRunById(runId);
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

    private SimulationRun startRun(Long runId) {
        SimulationRun run = getRunById(runId);
        run.start();
        return runRepository.save(run);
    }

    private SimulationRun succeedRun(Long runId) {
        SimulationRun run = getRunById(runId);
        run.succeed();
        return runRepository.save(run);
    }

    private SimulationRun failRun(Long runId, String message) {
        SimulationRun run = getRunById(runId);
        run.fail(message);
        return runRepository.save(run);
    }

    private SimulationRun cancelRunStatus(Long runId) {
        SimulationRun run = getRunById(runId);
        run.cancel();
        return runRepository.save(run);
    }

    private SimulationRun updateProgress(Long runId, Long currentTick) {
        int updated = runRepository.updateCurrentTick(runId, currentTick);
        if (updated == 0) {
            throw new ResourceNotFoundException("Simulation run not found with id: " + runId);
        }
        return getRunById(runId);
    }

    private void writeInputFiles(RunExecutionRequest request, Path runDir, BufferedWriter logWriter) throws IOException {
        Files.writeString(runDir.resolve(CONFIG_FILE_NAME), request.configJson(), StandardCharsets.UTF_8);
        log(logWriter, "Wrote config input to " + CONFIG_FILE_NAME);

        if (request.scenarioJson() != null) {
            Files.writeString(runDir.resolve(SCENARIO_FILE_NAME), request.scenarioJson(), StandardCharsets.UTF_8);
            log(logWriter, "Wrote scenario input to " + SCENARIO_FILE_NAME);

            ScenarioDefinitionDTO scenarioDefinition = objectMapper.readValue(
                    request.scenarioJson(), ScenarioDefinitionDTO.class);
            String name = request.scenarioName() != null ? request.scenarioName() : "run";
            batchInputGenerator.generateBatchInputFile(
                    request.configJson(), scenarioDefinition, name, runDir.resolve(INPUT_SCENARIO_FILE_NAME));
            log(logWriter, "Wrote batch input to " + INPUT_SCENARIO_FILE_NAME);
        }
    }

    private void failRunWithMessage(Long runId, BufferedWriter logWriter, String message, boolean started) throws IOException {
        log(logWriter, "Run failed: " + message);
        failRunWithMessage(runId, message, started);
    }

    private void failRunWithMessage(Long runId, Path logPath, String message, boolean started) {
        try (BufferedWriter logWriter = Files.newBufferedWriter(logPath, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND)) {
            log(logWriter, "Run failed: " + message);
        } catch (IOException ex) {
            logger.warn("Failed to append failure message to run log for run {}", runId, ex);
        }
        failRunWithMessage(runId, message, started);
    }

    private void failRunWithMessage(Long runId, String message, boolean started) {
        try {
            if (!started) {
                startRun(runId);
            }
            failRun(runId, message);
        } catch (IllegalStateException ex) {
            markRunningRunFailedSafely(runId, message, ex);
        } catch (Exception ex) {
            logger.error("Failed to mark run {} as failed", runId, ex);
        }
    }

    private void markRunningRunFailedSafely(Long runId, String message, IllegalStateException transitionFailure) {
        int updated = runRepository.markRunningRunFailed(runId, message, OffsetDateTime.now());
        if (updated > 0) {
            logger.warn(
                    "Recovered run {} by marking RUNNING run as FAILED after lifecycle transition failure",
                    runId,
                    transitionFailure
            );
            return;
        }

        try {
            SimulationRun run = getRunById(runId);
            logger.warn(
                    "Run {} was not marked FAILED because its current status is {}; preserving existing lifecycle state",
                    runId,
                    run.getStatus(),
                    transitionFailure
            );
        } catch (Exception lookupFailure) {
            logger.error("Failed to inspect run {} after failure transition error", runId, lookupFailure);
        }
    }

    private void cancelRunSafely(Long runId, BufferedWriter logWriter, String message) throws IOException {
        log(logWriter, message);
        cancelRunSafely(runId, message);
    }

    private void cancelRunSafely(Long runId, Path logPath, String message) {
        try (BufferedWriter logWriter = Files.newBufferedWriter(logPath, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND)) {
            log(logWriter, message);
        } catch (IOException ex) {
            logger.warn("Failed to append cancellation message to run log for run {}", runId, ex);
        }
        cancelRunSafely(runId, message);
    }

    private void cancelRunSafely(Long runId, String message) {
        try {
            SimulationRun run = getRunById(runId);
            if (run.getStatus() == SimulationRun.RunStatus.CANCELLED) {
                return;
            }
            cancelRunStatus(runId);
        } catch (Exception ex) {
            logger.error("Failed to mark run {} as cancelled", runId, ex);
        }
    }

    private void writeResults(Long runId,
                              Path runDir,
                              LiftConfigDTO config,
                              ScenarioDefinitionDTO scenario,
                              RunMetrics metrics,
                              String status,
                              String message) throws IOException {
        ObjectNode results = objectMapper.createObjectNode();
        ObjectNode runSummary = results.putObject("runSummary");
        runSummary.put("runId", runId);
        runSummary.put("status", status);
        runSummary.put("generatedAt", OffsetDateTime.now().toString());
        if (message != null) {
            runSummary.put("message", message);
        }
        if (scenario != null) {
            runSummary.put("durationTicks", scenario.durationTicks());
            if (scenario.seed() != null) {
                runSummary.put("seed", scenario.seed());
            }
        }
        if (metrics != null) {
            runSummary.put("ticks", metrics.totalTicks());
        }
        try {
            SimulationRun run = getRunById(runId);
            runSummary.put("liftSystemId", run.getLiftSystem().getId());
            runSummary.put("versionId", run.getVersion().getId());
        } catch (Exception ex) {
            logger.warn("Failed to resolve run summary metadata for run {}", runId, ex);
        }

        if (metrics != null && config != null) {
            results.set("kpis", metrics.toKpisNode(objectMapper));
            results.set("perLift", metrics.toPerLiftNode(objectMapper, config));
            results.set("perFloor", metrics.toPerFloorNode(objectMapper));
        }

        objectMapper.writerWithDefaultPrettyPrinter()
            .writeValue(runDir.resolve(RESULTS_FILE_NAME).toFile(), results);
    }

    private void log(BufferedWriter writer, String message) throws IOException {
        writer.write("[" + OffsetDateTime.now() + "] " + message);
        writer.newLine();
        writer.flush();
    }

    private String safeMessage(Exception ex) {
        return ex.getMessage() != null ? ex.getMessage() : "Unexpected simulation failure";
    }

    @PreDestroy
    public void shutdownExecutor() {
        executor.shutdown();
    }

    private record RunExecutionRequest(Long runId, String configJson, String scenarioJson, String scenarioName) {
    }

    private void submitExecution(RunExecutionRequest request) {
        Future<?> future = executor.submit(() -> executeRun(request));
        runningTasks.put(request.runId(), future);
        if (future.isDone()) {
            runningTasks.remove(request.runId(), future);
            cancellationTokens.remove(request.runId());
        }
    }

    private static final class CancellationToken {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        private void cancel() {
            cancelled.set(true);
        }

        private boolean isCancelled() {
            return cancelled.get();
        }
    }

    private static final class RunCancelledException extends CancellationException {
        private static final long serialVersionUID = 1L;
        private final transient RunMetrics metrics;
        private final transient LiftConfigDTO config;
        private final transient ScenarioDefinitionDTO scenario;

        private RunCancelledException(String message, RunMetrics metrics, LiftConfigDTO config, ScenarioDefinitionDTO scenario) {
            super(message);
            this.metrics = metrics;
            this.config = config;
            this.scenario = scenario;
        }

        private RunMetrics getMetrics() {
            return metrics;
        }

        private LiftConfigDTO getConfig() {
            return config;
        }

        private ScenarioDefinitionDTO getScenario() {
            return scenario;
        }
    }

    private static final class SimulationRunnerThreadFactory implements ThreadFactory {
        private int counter = 1;

        @Override
        public synchronized Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "simulation-runner-" + counter++);
            thread.setDaemon(true);
            return thread;
        }
    }

}
