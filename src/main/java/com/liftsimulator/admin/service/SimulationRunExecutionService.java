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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private static final String LOG_FILE_NAME = "run.log";
    private static final String RESULTS_FILE_NAME = "results.json";

    private final SimulationRunService runService;
    private final ConfigValidationService configValidationService;
    private final ScenarioValidationService scenarioValidationService;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;
    private final Path artefactsRoot;

    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Injected dependencies and configuration managed by Spring."
    )
    public SimulationRunExecutionService(
            SimulationRunService runService,
            ConfigValidationService configValidationService,
            ScenarioValidationService scenarioValidationService,
            ObjectMapper objectMapper,
            @Value("${simulation.runs.artefacts-root:run-artefacts}") String artefactsRoot) {
        this.runService = runService;
        this.configValidationService = configValidationService;
        this.scenarioValidationService = scenarioValidationService;
        this.objectMapper = objectMapper;
        this.executor = Executors.newCachedThreadPool(new SimulationRunnerThreadFactory());
        this.artefactsRoot = Paths.get(artefactsRoot);
    }

    /**
     * Creates a simulation run and executes it asynchronously.
     *
     * @param liftSystemId the lift system id
     * @param versionId the version id
     * @param scenarioId optional scenario id
     * @return the created simulation run (CREATED)
     */
    @Transactional
    public SimulationRun startAsyncRun(Long liftSystemId, Long versionId, Long scenarioId) {
        SimulationRun run = scenarioId == null
            ? runService.createRun(liftSystemId, versionId)
            : runService.createRunWithScenario(liftSystemId, versionId, scenarioId);

        String configJson = run.getVersion().getConfig();
        String scenarioJson = run.getScenario() != null ? run.getScenario().getScenarioJson() : null;

        RunExecutionRequest request = new RunExecutionRequest(run.getId(), configJson, scenarioJson);
        executor.execute(() -> executeRun(request));
        return run;
    }

    /**
     * Submits an existing simulation run for asynchronous execution.
     * The run should be in CREATED state with configuration already set.
     *
     * @param runId the run id to execute
     */
    @Transactional
    public void submitRunForExecution(Long runId) {
        SimulationRun run = runService.getRunById(runId);
        String configJson = run.getVersion().getConfig();
        String scenarioJson = run.getScenario() != null ? run.getScenario().getScenarioJson() : null;

        RunExecutionRequest request = new RunExecutionRequest(run.getId(), configJson, scenarioJson);
        executor.execute(() -> executeRun(request));
    }

    private void executeRun(RunExecutionRequest request) {
        Path runDir = buildRunDirectory(request.runId());
        Path logPath = runDir.resolve(LOG_FILE_NAME);
        boolean started = false;

        try {
            Files.createDirectories(runDir);
            runService.configureRun(request.runId(), null, null, runDir.toAbsolutePath().toString());
        } catch (Exception ex) {
            logger.error("Failed to create run artefact directory for run {}", request.runId(), ex);
            failRunWithMessage(request.runId(),
                "Failed to initialize run artefacts: " + safeMessage(ex),
                false);
            return;
        }

        try (BufferedWriter logWriter = Files.newBufferedWriter(logPath, StandardCharsets.UTF_8)) {
            log(logWriter, "Run directory initialized at " + runDir.toAbsolutePath());
            writeInputFiles(request, runDir, logWriter);

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

            runService.configureRun(
                request.runId(),
                scenario.durationTicks() != null ? scenario.durationTicks().longValue() : null,
                scenario.seed() != null ? scenario.seed().longValue() : null,
                runDir.toAbsolutePath().toString()
            );

            // Start the run if not already started (it may have been started by createAndStartRun)
            SimulationRun currentRun = runService.getRunById(request.runId());
            if (currentRun.getStatus() != SimulationRun.RunStatus.RUNNING) {
                runService.startRun(request.runId());
            }
            started = true;
            log(logWriter, "Simulation started for run " + request.runId());

            RunMetrics metrics = runSimulation(request.runId(), config, scenario, logWriter);

            runService.succeedRun(request.runId());
            log(logWriter, "Simulation succeeded for run " + request.runId());
            writeResults(request.runId(), runDir, config, scenario, metrics, "SUCCEEDED", null);
        } catch (Exception ex) {
            String errorMessage = safeMessage(ex);
            logger.error("Simulation run {} failed", request.runId(), ex);
            failRunWithMessage(request.runId(), logPath, errorMessage, started);
            try {
                writeResults(request.runId(), runDir, null, null, null, "FAILED", errorMessage);
            } catch (IOException ioEx) {
                logger.warn("Failed to write results file for run {}", request.runId(), ioEx);
            }
        }
    }

    private RunMetrics runSimulation(Long runId,
                                     LiftConfigDTO config,
                                     ScenarioDefinitionDTO scenario,
                                     BufferedWriter logWriter) throws IOException {
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

                // Create hall calls (not car calls) to properly model passenger pickup
                for (int i = 0; i < passengers; i++) {
                    LiftRequest request = LiftRequest.hallCall(flow.originFloor(), direction);
                    metrics.recordRequestCreation(request, currentTick);
                    controller.addRequest(request);
                }
            }

            metrics.recordLiftState(engine.getCurrentState());
            metrics.recordActiveRequests(controller.getRequests(), currentTick);
            engine.tick();
            metrics.recordTerminalRequests(currentTick);

            if (tick % PROGRESS_UPDATE_INTERVAL == 0) {
                runService.updateProgress(runId, engine.getCurrentTick());
            }
        }

        runService.updateProgress(runId, engine.getCurrentTick());
        log(logWriter, "Simulation completed at tick " + engine.getCurrentTick());
        metrics.recordTerminalRequests(engine.getCurrentTick());
        return metrics;
    }

    private Map<Integer, List<PassengerFlowDTO>> groupFlowsByTick(ScenarioDefinitionDTO scenario) {
        Map<Integer, List<PassengerFlowDTO>> flowsByTick = new HashMap<>();
        if (scenario.passengerFlows() == null) {
            return flowsByTick;
        }

        for (PassengerFlowDTO flow : scenario.passengerFlows()) {
            int tick = flow.startTick() != null ? flow.startTick() : 0;
            flowsByTick.computeIfAbsent(tick, key -> new ArrayList<>()).add(flow);
        }
        return flowsByTick;
    }

    private void writeInputFiles(RunExecutionRequest request, Path runDir, BufferedWriter logWriter) throws IOException {
        Files.writeString(runDir.resolve(CONFIG_FILE_NAME), request.configJson(), StandardCharsets.UTF_8);
        log(logWriter, "Wrote config input to " + CONFIG_FILE_NAME);

        if (request.scenarioJson() != null) {
            Files.writeString(runDir.resolve(SCENARIO_FILE_NAME), request.scenarioJson(), StandardCharsets.UTF_8);
            log(logWriter, "Wrote scenario input to " + SCENARIO_FILE_NAME);
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
                runService.startRun(runId);
            }
            runService.failRun(runId, message);
        } catch (Exception ex) {
            logger.error("Failed to mark run {} as failed", runId, ex);
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
            SimulationRun run = runService.getRunById(runId);
            runSummary.put("liftSystemId", run.getLiftSystem().getId());
            runSummary.put("versionId", run.getVersion().getId());
            if (run.getScenario() != null) {
                runSummary.put("scenarioId", run.getScenario().getId());
            }
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

    private Path buildRunDirectory(Long runId) {
        return artefactsRoot.resolve("run-" + runId);
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

    private record RunExecutionRequest(Long runId, String configJson, String scenarioJson) {
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

    private static final class RunMetrics {
        private final Map<Long, RequestLifecycle> lifecycles = new LinkedHashMap<>();
        private final Map<com.liftsimulator.domain.LiftStatus, Long> statusCounts = new EnumMap<>(com.liftsimulator.domain.LiftStatus.class);
        private final Map<Integer, FloorMetrics> floorMetrics = new HashMap<>();
        private final int minFloor;
        private final int maxFloor;
        private long totalTicks;
        private Integer lastRecordedFloor;

        private RunMetrics(int minFloor, int maxFloor) {
            this.minFloor = minFloor;
            this.maxFloor = maxFloor;
        }

        private void recordPassengerFlow(PassengerFlowDTO flow, int passengers) {
            if (flow.originFloor() != null) {
                floorMetrics.computeIfAbsent(flow.originFloor(), FloorMetrics::new)
                    .addOrigins(passengers);
            }
            if (flow.destinationFloor() != null) {
                floorMetrics.computeIfAbsent(flow.destinationFloor(), FloorMetrics::new)
                    .addDestinations(passengers);
            }
        }

        private void recordLiftState(com.liftsimulator.domain.LiftState state) {
            statusCounts.merge(state.getStatus(), 1L, Long::sum);
            if (lastRecordedFloor == null || lastRecordedFloor != state.getFloor()) {
                floorMetrics.computeIfAbsent(state.getFloor(), FloorMetrics::new)
                    .addVisit();
                lastRecordedFloor = state.getFloor();
            }
            totalTicks++;
        }

        private void recordRequestCreation(LiftRequest request, long tick) {
            lifecycles.computeIfAbsent(request.getId(), id -> new RequestLifecycle(request, tick));
        }

        private void recordActiveRequests(Set<LiftRequest> requests, long tick) {
            for (LiftRequest request : requests) {
                recordRequestCreation(request, tick);
            }
        }

        private void recordTerminalRequests(long tick) {
            for (RequestLifecycle lifecycle : lifecycles.values()) {
                if (lifecycle.terminalTick() != null || !lifecycle.request().isTerminal()) {
                    continue;
                }
                lifecycle.markTerminal(tick, lifecycle.request().getState());
            }
        }

        private long totalTicks() {
            return totalTicks;
        }

        private ObjectNode toKpisNode(ObjectMapper objectMapper) {
            ObjectNode kpis = objectMapper.createObjectNode();
            long completed = lifecycles.values().stream()
                .filter(lifecycle -> lifecycle.terminalState() == com.liftsimulator.domain.RequestState.COMPLETED)
                .count();
            long cancelled = lifecycles.values().stream()
                .filter(lifecycle -> lifecycle.terminalState() == com.liftsimulator.domain.RequestState.CANCELLED)
                .count();
            long maxWait = lifecycles.values().stream()
                .filter(lifecycle -> lifecycle.terminalState() == com.liftsimulator.domain.RequestState.COMPLETED)
                .mapToLong(RequestLifecycle::waitTicks)
                .max()
                .orElse(0L);
            double avgWait = lifecycles.values().stream()
                .filter(lifecycle -> lifecycle.terminalState() == com.liftsimulator.domain.RequestState.COMPLETED)
                .mapToLong(RequestLifecycle::waitTicks)
                .average()
                .orElse(0.0);

            long idleTicks = statusCounts.getOrDefault(com.liftsimulator.domain.LiftStatus.IDLE, 0L)
                + statusCounts.getOrDefault(com.liftsimulator.domain.LiftStatus.OUT_OF_SERVICE, 0L);
            long movingTicks = statusCounts.getOrDefault(com.liftsimulator.domain.LiftStatus.MOVING_UP, 0L)
                + statusCounts.getOrDefault(com.liftsimulator.domain.LiftStatus.MOVING_DOWN, 0L);
            long doorTicks = statusCounts.getOrDefault(com.liftsimulator.domain.LiftStatus.DOORS_OPENING, 0L)
                + statusCounts.getOrDefault(com.liftsimulator.domain.LiftStatus.DOORS_OPEN, 0L)
                + statusCounts.getOrDefault(com.liftsimulator.domain.LiftStatus.DOORS_CLOSING, 0L);
            double utilisation = totalTicks == 0 ? 0.0 : (double) (movingTicks + doorTicks) / (double) totalTicks;

            kpis.put("requestsTotal", lifecycles.size());
            kpis.put("passengersServed", completed);
            kpis.put("passengersCancelled", cancelled);
            kpis.put("avgWaitTicks", avgWait);
            kpis.put("maxWaitTicks", maxWait);
            kpis.put("idleTicks", idleTicks);
            kpis.put("movingTicks", movingTicks);
            kpis.put("doorTicks", doorTicks);
            kpis.put("utilisation", utilisation);
            return kpis;
        }

        private com.fasterxml.jackson.databind.node.ArrayNode toPerLiftNode(ObjectMapper objectMapper, LiftConfigDTO config) {
            com.fasterxml.jackson.databind.node.ArrayNode lifts = objectMapper.createArrayNode();
            ObjectNode lift = objectMapper.createObjectNode();
            lift.put("liftId", "lift-1");
            lift.put("minFloor", minFloor);
            lift.put("maxFloor", maxFloor);
            lift.put("homeFloor", config.homeFloor());
            lift.put("travelTicksPerFloor", config.travelTicksPerFloor());
            lift.put("doorTransitionTicks", config.doorTransitionTicks());
            lift.put("doorDwellTicks", config.doorDwellTicks());
            lift.put("doorReopenWindowTicks", config.doorReopenWindowTicks());
            lift.put("controllerStrategy", config.controllerStrategy().name());
            lift.put("idleParkingMode", config.idleParkingMode().name());

            ObjectNode statusNode = objectMapper.createObjectNode();
            for (Map.Entry<com.liftsimulator.domain.LiftStatus, Long> entry : statusCounts.entrySet()) {
                statusNode.put(entry.getKey().name(), entry.getValue());
            }
            lift.set("statusCounts", statusNode);

            long idleTicks = statusCounts.getOrDefault(com.liftsimulator.domain.LiftStatus.IDLE, 0L)
                + statusCounts.getOrDefault(com.liftsimulator.domain.LiftStatus.OUT_OF_SERVICE, 0L);
            long movingTicks = statusCounts.getOrDefault(com.liftsimulator.domain.LiftStatus.MOVING_UP, 0L)
                + statusCounts.getOrDefault(com.liftsimulator.domain.LiftStatus.MOVING_DOWN, 0L);
            long doorTicks = statusCounts.getOrDefault(com.liftsimulator.domain.LiftStatus.DOORS_OPENING, 0L)
                + statusCounts.getOrDefault(com.liftsimulator.domain.LiftStatus.DOORS_OPEN, 0L)
                + statusCounts.getOrDefault(com.liftsimulator.domain.LiftStatus.DOORS_CLOSING, 0L);
            double utilisation = totalTicks == 0 ? 0.0 : (double) (movingTicks + doorTicks) / (double) totalTicks;

            lift.put("totalTicks", totalTicks);
            lift.put("idleTicks", idleTicks);
            lift.put("movingTicks", movingTicks);
            lift.put("doorTicks", doorTicks);
            lift.put("utilisation", utilisation);

            lifts.add(lift);
            return lifts;
        }

        private com.fasterxml.jackson.databind.node.ArrayNode toPerFloorNode(ObjectMapper objectMapper) {
            com.fasterxml.jackson.databind.node.ArrayNode floors = objectMapper.createArrayNode();
            for (int floor = minFloor; floor <= maxFloor; floor++) {
                FloorMetrics metrics = floorMetrics.getOrDefault(floor, new FloorMetrics(floor));
                ObjectNode floorNode = objectMapper.createObjectNode();
                floorNode.put("floor", floor);
                floorNode.put("originPassengers", metrics.originPassengers());
                floorNode.put("destinationPassengers", metrics.destinationPassengers());
                floorNode.put("liftVisits", metrics.liftVisits());
                floors.add(floorNode);
            }
            return floors;
        }
    }

    private static final class FloorMetrics {
        private long originPassengers;
        private long destinationPassengers;
        private long liftVisits;

        private FloorMetrics(int floor) {
        }

        private void addOrigins(long count) {
            originPassengers += count;
        }

        private void addDestinations(long count) {
            destinationPassengers += count;
        }

        private void addVisit() {
            liftVisits++;
        }

        private long originPassengers() {
            return originPassengers;
        }

        private long destinationPassengers() {
            return destinationPassengers;
        }

        private long liftVisits() {
            return liftVisits;
        }
    }

    private static final class RequestLifecycle {
        private final LiftRequest request;
        private final long createdTick;
        private Long terminalTick;
        private com.liftsimulator.domain.RequestState terminalState;

        private RequestLifecycle(LiftRequest request, long createdTick) {
            this.request = request;
            this.createdTick = createdTick;
        }

        private LiftRequest request() {
            return request;
        }

        private Long terminalTick() {
            return terminalTick;
        }

        private com.liftsimulator.domain.RequestState terminalState() {
            return terminalState;
        }

        private long waitTicks() {
            if (terminalTick == null) {
                return 0L;
            }
            return Math.max(0L, terminalTick - createdTick);
        }

        private void markTerminal(long tick, com.liftsimulator.domain.RequestState state) {
            this.terminalTick = tick;
            this.terminalState = state;
        }
    }
}
