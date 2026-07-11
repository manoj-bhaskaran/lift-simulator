package com.liftsimulator.admin.service;

import com.liftsimulator.admin.dto.ConfigValidationResponse;
import com.liftsimulator.admin.dto.LiftConfigDTO;
import com.liftsimulator.admin.dto.PassengerFlowDTO;
import com.liftsimulator.admin.dto.ScenarioDefinitionDTO;
import com.liftsimulator.admin.dto.ScenarioValidationResponse;
import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.service.metrics.RunMetrics;
import com.liftsimulator.domain.Direction;
import com.liftsimulator.domain.LiftRequest;
import com.liftsimulator.engine.ControllerFactory;
import com.liftsimulator.engine.RequestManagingLiftController;
import com.liftsimulator.engine.SimulationEngine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Runs a validated simulation request and coordinates lifecycle state transitions.
 */
@Service
public class SimulationRunner {
    private static final Logger logger = LoggerFactory.getLogger(SimulationRunner.class);
    private static final int PROGRESS_UPDATE_INTERVAL = 5;

    private final RunLifecycleManager lifecycleManager;
    private final ConfigValidationService configValidationService;
    private final ScenarioValidationService scenarioValidationService;
    private final ObjectMapper objectMapper;
    private final SimulationArtefactWriter artefactWriter;

    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Injected collaborators are Spring-managed singleton services/configuration objects."
    )
    public SimulationRunner(RunLifecycleManager lifecycleManager,
                            ConfigValidationService configValidationService,
                            ScenarioValidationService scenarioValidationService,
                            ObjectMapper objectMapper,
                            SimulationArtefactWriter artefactWriter) {
        this.lifecycleManager = lifecycleManager;
        this.configValidationService = configValidationService;
        this.scenarioValidationService = scenarioValidationService;
        this.objectMapper = objectMapper;
        this.artefactWriter = artefactWriter;
    }
    public void executeRun(RunExecutionRequest request, SimulationRunExecutionService.CancellationToken cancelToken) {
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
        Path logPath = runDir.resolve(SimulationArtefactWriter.LOG_FILE_NAME);
        logger.info("Run {} using artefact directory: {}", request.runId(), runDir);

        try (BufferedWriter logWriter = artefactWriter.openLog(runDir)) {
            artefactWriter.log(logWriter, "Run directory: " + runDir.toAbsolutePath());

            if (request.scenarioJson() == null) {
                artefactWriter.log(logWriter, "Missing scenario payload for run " + request.runId());
                failRunWithMessage(request.runId(), logWriter, "Missing scenario payload for run.", false);
                artefactWriter.writeResults(request.runId(), runDir, null, null, null, "FAILED", "Missing scenario payload for run.");
                return;
            }

            ConfigValidationResponse configValidation = configValidationService.validate(request.configJson());
            if (!configValidation.valid()) {
                artefactWriter.log(logWriter, "Configuration validation failed: " + configValidation.errors());
                failRunWithMessage(request.runId(), logWriter, "Invalid configuration payload.", false);
                artefactWriter.writeResults(request.runId(), runDir, null, null, null, "FAILED", "Invalid configuration payload.");
                return;
            }

            JsonNode scenarioNode = objectMapper.readTree(request.scenarioJson());
            ScenarioValidationResponse scenarioValidation = scenarioValidationService.validate(scenarioNode);
            if (!scenarioValidation.valid()) {
                artefactWriter.log(logWriter, "Scenario validation failed: " + scenarioValidation.errors());
                failRunWithMessage(request.runId(), logWriter, "Invalid scenario payload.", false);
                artefactWriter.writeResults(request.runId(), runDir, null, null, null, "FAILED", "Invalid scenario payload.");
                return;
            }

            LiftConfigDTO config = objectMapper.readValue(request.configJson(), LiftConfigDTO.class);
            ScenarioDefinitionDTO scenario = objectMapper.readerFor(ScenarioDefinitionDTO.class)
                .readValue(scenarioNode);

            if (scenario.durationTicks() == null) {
                artefactWriter.log(logWriter, "Scenario has null durationTicks");
                SimulationRun runAtCheck = getRunById(request.runId());
                boolean runAlreadyStarted = runAtCheck.getStatus() == SimulationRun.RunStatus.RUNNING;
                failRunWithMessage(request.runId(), logWriter, "Scenario must have a valid durationTicks value.", runAlreadyStarted);
                artefactWriter.writeResults(request.runId(), runDir, null, null, null, "FAILED", "Scenario must have a valid durationTicks value.");
                return;
            }

            artefactWriter.writeInputFiles(request, runDir, logWriter);

            lifecycleManager.configureRun(
                request.runId(),
                scenario.durationTicks().longValue(),
                scenario.seed() != null ? scenario.seed().longValue() : null,
                null
            );

            // Start the run if not already started (it may have been started by createAndStartRun)
            SimulationRun currentRun = getRunById(request.runId());
            if (currentRun.getStatus() == SimulationRun.RunStatus.CANCELLED || cancelToken.isCancelled()) {
                artefactWriter.log(logWriter, "Run cancelled before start for run " + request.runId());
                cancelRunSafely(request.runId(), logWriter, "Run cancelled before start.");
                artefactWriter.writeResults(request.runId(), runDir, config, scenario, null, "CANCELLED", "Run cancelled before start.");
                return;
            }
            if (currentRun.getStatus() != SimulationRun.RunStatus.RUNNING) {
                lifecycleManager.startRun(request.runId());
            }
            started = true;
            lifecycleManager.updateProgress(request.runId(), 0L);
            artefactWriter.log(logWriter, "Simulation started for run " + request.runId());

            RunMetrics metrics;
            try {
                metrics = runSimulation(request.runId(), config, scenario, logWriter, cancelToken);
            } catch (RunCancelledException ex) {
                cancelRunSafely(request.runId(), logWriter, ex.getMessage());
                artefactWriter.writeResults(request.runId(), runDir, ex.getConfig(), ex.getScenario(), ex.getMetrics(), "CANCELLED", ex.getMessage());
                return;
            }

            if (cancelToken.isCancelled() || getRunById(request.runId()).getStatus() == SimulationRun.RunStatus.CANCELLED) {
                artefactWriter.log(logWriter, "Run cancelled for run " + request.runId());
                artefactWriter.writeResults(request.runId(), runDir, config, scenario, metrics, "CANCELLED", "Run cancelled.");
                return;
            }
            artefactWriter.log(logWriter, "Simulation succeeded for run " + request.runId());
            artefactWriter.writeResults(request.runId(), runDir, config, scenario, metrics, "SUCCEEDED", null);
            lifecycleManager.succeedRun(request.runId());
        } catch (IOException | RuntimeException ex) {
            String errorMessage = safeMessage(ex);
            logger.error("Simulation run {} failed", request.runId(), ex);
            failRunWithMessage(request.runId(), logPath, errorMessage, started);
            try {
                artefactWriter.writeResults(request.runId(), runDir, null, null, null, "FAILED", errorMessage);
            } catch (IOException ioEx) {
                logger.warn("Failed to write results file for run {}", request.runId(), ioEx);
            }
        }
    }

    private RunMetrics runSimulation(Long runId,
                                     LiftConfigDTO config,
                                     ScenarioDefinitionDTO scenario,
                                     BufferedWriter logWriter,
                                     SimulationRunExecutionService.CancellationToken cancelToken) throws IOException {
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

        artefactWriter.log(logWriter, "Starting simulation for " + scenario.durationTicks() + " ticks.");
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

                metrics.recordPassengerFlow(flow, passengers);

                // One hall call per flow; passenger count is carried on the request for KPI accounting
                LiftRequest request = LiftRequest.hallCall(flow.originFloor(), direction, passengers);
                metrics.recordRequestCreation(request, currentTick);
                controller.addRequest(request);
            }

            metrics.recordActiveRequests(controller.getRequests(), currentTick);
            engine.tick();
            metrics.recordLiftState(engine.getCurrentState());
            metrics.recordTerminalRequests(engine.getCurrentTick());

            long progressTick = tick + 1L;
            if (tick % PROGRESS_UPDATE_INTERVAL == 0) {
                lifecycleManager.updateProgress(runId, progressTick);
            }
        }

        lifecycleManager.updateProgress(runId, (long) scenario.durationTicks());
        artefactWriter.log(logWriter, "Simulation completed at tick " + engine.getCurrentTick());
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
        return lifecycleManager.getByIdWithDetails(runId);
    }


    private void failRunWithMessage(Long runId, BufferedWriter logWriter, String message, boolean started) throws IOException {
        artefactWriter.log(logWriter, "Run failed: " + message);
        failRunWithMessage(runId, message, started);
    }

    private void failRunWithMessage(Long runId, Path logPath, String message, boolean started) {
        artefactWriter.appendLog(logPath, "Run failed: " + message);
        failRunWithMessage(runId, message, started);
    }

    private void failRunWithMessage(Long runId, String message, boolean started) {
        try {
            if (!started) {
                lifecycleManager.startRun(runId);
            }
            lifecycleManager.failRun(runId, message);
        } catch (IllegalStateException ex) {
            lifecycleManager.markRunningRunFailedSafely(runId, message, ex);
        } catch (Exception ex) {
            logger.error("Failed to mark run {} as failed", runId, ex);
        }
    }

    private void cancelRunSafely(Long runId, BufferedWriter logWriter, String message) throws IOException {
        artefactWriter.log(logWriter, message);
        cancelRunSafely(runId, message);
    }

    private void cancelRunSafely(Long runId, String message) {
        try {
            SimulationRun run = getRunById(runId);
            if (run.getStatus() == SimulationRun.RunStatus.CANCELLED) {
                return;
            }
            lifecycleManager.cancelRun(runId);
        } catch (Exception ex) {
            logger.error("Failed to mark run {} as cancelled", runId, ex);
        }
    }

    private String safeMessage(Exception ex) {
        return ex.getMessage() != null ? ex.getMessage() : "Unexpected simulation failure";
    }

    private static final class RunCancelledException extends CancellationException {
        private static final long serialVersionUID = 1L;
        private final transient RunMetrics metrics;
        private final transient LiftConfigDTO config;
        private final transient ScenarioDefinitionDTO scenario;

        private RunCancelledException(String message,
                                      RunMetrics metrics,
                                      LiftConfigDTO config,
                                      ScenarioDefinitionDTO scenario) {
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

    public record RunExecutionRequest(Long runId, String configJson, String scenarioJson, String scenarioName) {
    }
}
