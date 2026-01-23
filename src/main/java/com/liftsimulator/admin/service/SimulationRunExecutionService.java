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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                writeResultsPlaceholder(request.runId(), runDir, "FAILED", "Missing scenario payload for run.");
                return;
            }

            ConfigValidationResponse configValidation = configValidationService.validate(request.configJson());
            if (!configValidation.valid()) {
                log(logWriter, "Configuration validation failed: " + configValidation.errors());
                failRunWithMessage(request.runId(), logWriter, "Invalid configuration payload.", false);
                writeResultsPlaceholder(request.runId(), runDir, "FAILED", "Invalid configuration payload.");
                return;
            }

            JsonNode scenarioNode = objectMapper.readTree(request.scenarioJson());
            ScenarioValidationResponse scenarioValidation = scenarioValidationService.validate(scenarioNode);
            if (!scenarioValidation.valid()) {
                log(logWriter, "Scenario validation failed: " + scenarioValidation.errors());
                failRunWithMessage(request.runId(), logWriter, "Invalid scenario payload.", false);
                writeResultsPlaceholder(request.runId(), runDir, "FAILED", "Invalid scenario payload.");
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

            runService.startRun(request.runId());
            started = true;
            log(logWriter, "Simulation started for run " + request.runId());

            runSimulation(request.runId(), config, scenario, logWriter);

            runService.succeedRun(request.runId());
            log(logWriter, "Simulation succeeded for run " + request.runId());
            writeResultsPlaceholder(request.runId(), runDir, "SUCCEEDED", "Results generation pending.");
        } catch (Exception ex) {
            String errorMessage = safeMessage(ex);
            logger.error("Simulation run {} failed", request.runId(), ex);
            failRunWithMessage(request.runId(), logPath, errorMessage, started);
            try {
                writeResultsPlaceholder(request.runId(), runDir, "FAILED", errorMessage);
            } catch (IOException ioEx) {
                logger.warn("Failed to write results placeholder for run {}", request.runId(), ioEx);
            }
        }
    }

    private void runSimulation(Long runId,
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

        log(logWriter, "Starting simulation for " + scenario.durationTicks() + " ticks.");
        for (int tick = 0; tick < scenario.durationTicks(); tick++) {
            long currentTick = engine.getCurrentTick();
            List<PassengerFlowDTO> flows = flowsByTick.getOrDefault((int) currentTick, List.of());
            for (PassengerFlowDTO flow : flows) {
                int passengers = flow.passengers() != null ? flow.passengers() : 1;
                for (int i = 0; i < passengers; i++) {
                    LiftRequest request = LiftRequest.carCall(flow.originFloor(), flow.destinationFloor());
                    controller.addRequest(request);
                }
            }

            engine.tick();

            if (tick % PROGRESS_UPDATE_INTERVAL == 0) {
                runService.updateProgress(runId, engine.getCurrentTick());
            }
        }

        runService.updateProgress(runId, engine.getCurrentTick());
        log(logWriter, "Simulation completed at tick " + engine.getCurrentTick());
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

    private void writeResultsPlaceholder(Long runId, Path runDir, String status, String message) throws IOException {
        ObjectNode results = objectMapper.createObjectNode();
        results.put("runId", runId);
        results.put("status", status);
        results.put("message", message);
        results.put("generatedAt", OffsetDateTime.now().toString());
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
}
