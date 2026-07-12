package com.liftsimulator.admin.service;

import com.liftsimulator.admin.dto.ConfigValidationResponse;
import com.liftsimulator.admin.dto.LiftConfigDTO;
import com.liftsimulator.admin.dto.PassengerFlowDTO;
import com.liftsimulator.admin.dto.ScenarioDefinitionDTO;
import com.liftsimulator.admin.dto.ScenarioValidationResponse;
import com.liftsimulator.admin.dto.ValidationIssue;
import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.entity.Scenario;
import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.service.metrics.RunMetrics;
import com.liftsimulator.domain.ControllerStrategy;
import com.liftsimulator.domain.IdleParkingMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SimulationRunner}: the pure tick loop directly, and the
 * failure-marking orchestration around it, without going through
 * {@link SimulationRunExecutionService}.
 */
@ExtendWith(MockitoExtension.class)
class SimulationRunnerTest {

    private static final LiftConfigDTO CONFIG = new LiftConfigDTO(
        0, 4, 1, 1, 1, 1, 1, 0, 2,
        ControllerStrategy.NEAREST_REQUEST_ROUTING, IdleParkingMode.PARK_TO_HOME_FLOOR
    );

    private static final String VALID_CONFIG_JSON = """
        {
            "minFloor": 0,
            "maxFloor": 4,
            "lifts": 1,
            "travelTicksPerFloor": 1,
            "doorTransitionTicks": 1,
            "doorDwellTicks": 1,
            "doorReopenWindowTicks": 1,
            "homeFloor": 0,
            "idleTimeoutTicks": 2,
            "controllerStrategy": "NEAREST_REQUEST_ROUTING",
            "idleParkingMode": "PARK_TO_HOME_FLOOR"
        }
        """.trim();

    @Mock
    private RunLifecycleManager lifecycleManager;

    @Mock
    private ConfigValidationService configValidationService;

    @Mock
    private ScenarioValidationService scenarioValidationService;

    @Mock
    private BatchInputGenerator batchInputGenerator;

    @TempDir
    private Path tempDir;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private SimulationRunner simulationRunner;

    @BeforeEach
    void setUp() {
        simulationRunner = new SimulationRunner(
            lifecycleManager,
            configValidationService,
            scenarioValidationService,
            objectMapper,
            new SimulationArtefactWriter(objectMapper, batchInputGenerator, lifecycleManager)
        );
    }

    private static ScenarioDefinitionDTO scenario(int durationTicks, PassengerFlowDTO... flows) {
        return new ScenarioDefinitionDTO(durationTicks, List.of(flows), 7);
    }

    @Test
    void runTickLoopProducesExactMetricsForDeterministicScenario() {
        ScenarioDefinitionDTO scenario = scenario(6, new PassengerFlowDTO(0, 0, 3, 1));

        RunMetrics metrics = SimulationRunner.runTickLoop(CONFIG, scenario, () -> false, tick -> { });

        assertEquals(6L, metrics.totalTicks());
        JsonNode kpis = metrics.toKpisNode(objectMapper);
        assertEquals(1, kpis.get("requestsTotal").asInt());
        assertEquals(1, kpis.get("pickupRequestsServed").asInt());
        assertEquals(0, kpis.get("pickupRequestsCancelled").asInt());
        assertEquals(1, kpis.get("passengersServed").asInt());
        assertEquals(0, kpis.get("passengersCancelled").asInt());
        assertEquals(2.0, kpis.get("avgPickupWaitTicks").asDouble());
        assertEquals(2L, kpis.get("maxPickupWaitTicks").asLong());
        assertEquals(5L, kpis.get("idleTicks").asLong());
        assertEquals(0L, kpis.get("movingTicks").asLong());
        assertEquals(1L, kpis.get("doorTicks").asLong());
        assertEquals(1.0 / 6.0, kpis.get("pickupLegUtilisation").asDouble(), 1e-9);

        JsonNode perFloor = metrics.toPerFloorNode(objectMapper);
        assertEquals(1, perFloor.get(0).get("originPassengers").asInt());
        assertEquals(1, perFloor.get(0).get("liftVisits").asInt());
        assertEquals(1, perFloor.get(3).get("destinationPassengers").asInt());
    }

    @Test
    void runTickLoopSkipsSameFloorPassengerFlows() {
        ScenarioDefinitionDTO scenario = scenario(4, new PassengerFlowDTO(0, 2, 2, 1));

        RunMetrics metrics = SimulationRunner.runTickLoop(CONFIG, scenario, () -> false, tick -> { });

        assertEquals(4L, metrics.totalTicks());
        JsonNode kpis = metrics.toKpisNode(objectMapper);
        assertEquals(0, kpis.get("requestsTotal").asInt());
        assertEquals(0L, kpis.get("doorTicks").asLong());
        assertEquals(0L, kpis.get("movingTicks").asLong());
        assertEquals(4L, kpis.get("idleTicks").asLong());

        JsonNode perFloor = metrics.toPerFloorNode(objectMapper);
        assertEquals(0, perFloor.get(2).get("originPassengers").asInt());
        assertEquals(0, perFloor.get(2).get("destinationPassengers").asInt());
    }

    @Test
    void runTickLoopSurfacesPartialMetricsWhenCancelledMidRun() {
        ScenarioDefinitionDTO scenario = scenario(6, new PassengerFlowDTO(0, 0, 3, 1));
        int[] calls = {0};
        List<Long> progressCalls = new ArrayList<>();

        RunMetrics metrics = SimulationRunner.runTickLoop(
            CONFIG, scenario, () -> calls[0]++ >= 3, progressCalls::add
        );

        assertEquals(3L, metrics.totalTicks());
        assertTrue(metrics.totalTicks() < scenario.durationTicks(),
            "Cancelled run must report fewer ticks than the full scenario duration");
        assertEquals(List.of(1L), progressCalls,
            "No final full-duration progress update should fire when cancelled mid-run");
        JsonNode kpis = metrics.toKpisNode(objectMapper);
        assertEquals(1, kpis.get("passengersServed").asInt(),
            "Partial metrics must still reflect requests completed before cancellation");
    }

    @Test
    void executeRunMarksFailedAndWritesFailedResultsFileWhenDurationTicksIsMissing() throws Exception {
        Path runDir = tempDir.resolve("run-null-duration");
        Files.createDirectories(runDir);
        String scenarioJson = """
            {
                "passengerFlows": [
                    {"startTick": 0, "originFloor": 0, "destinationFloor": 3, "passengers": 1}
                ]
            }
            """.trim();
        SimulationRun run = runWithArtefactDirectory(1L, runDir, scenarioJson);
        stubLifecycle(run);
        when(configValidationService.validate(VALID_CONFIG_JSON)).thenReturn(validConfigResponse());
        when(scenarioValidationService.validate(any(JsonNode.class))).thenReturn(validScenarioResponse());

        simulationRunner.executeRun(executionRequest(run, scenarioJson), new CancellationToken());

        verify(lifecycleManager).failRun(1L, "Scenario must have a valid durationTicks value.");
        JsonNode results = objectMapper.readTree(runDir.resolve("results.json").toFile());
        assertEquals("FAILED", results.at("/runSummary/status").asString());
        assertEquals("Scenario must have a valid durationTicks value.", results.at("/runSummary/message").asString());
    }

    @Test
    void executeRunMarksFailedAndWritesFailedResultsFileWhenConfigValidationFails() throws Exception {
        Path runDir = tempDir.resolve("run-config-invalid");
        Files.createDirectories(runDir);
        String scenarioJson = shortScenarioJson();
        SimulationRun run = runWithArtefactDirectory(2L, runDir, scenarioJson);
        stubLifecycle(run);
        when(configValidationService.validate(VALID_CONFIG_JSON)).thenReturn(invalidConfigResponse());

        simulationRunner.executeRun(executionRequest(run, scenarioJson), new CancellationToken());

        verify(lifecycleManager).failRun(2L, "Invalid configuration payload.");
        JsonNode results = objectMapper.readTree(runDir.resolve("results.json").toFile());
        assertEquals("FAILED", results.at("/runSummary/status").asString());
        assertEquals("Invalid configuration payload.", results.at("/runSummary/message").asString());
    }

    @Test
    void executeRunMarksFailedAndWritesFailedResultsFileWhenScenarioValidationFails() throws Exception {
        Path runDir = tempDir.resolve("run-scenario-invalid");
        Files.createDirectories(runDir);
        String scenarioJson = shortScenarioJson();
        SimulationRun run = runWithArtefactDirectory(3L, runDir, scenarioJson);
        stubLifecycle(run);
        when(configValidationService.validate(VALID_CONFIG_JSON)).thenReturn(validConfigResponse());
        when(scenarioValidationService.validate(any(JsonNode.class))).thenReturn(invalidScenarioResponse());

        simulationRunner.executeRun(executionRequest(run, scenarioJson), new CancellationToken());

        verify(lifecycleManager).failRun(3L, "Invalid scenario payload.");
        JsonNode results = objectMapper.readTree(runDir.resolve("results.json").toFile());
        assertEquals("FAILED", results.at("/runSummary/status").asString());
        assertEquals("Invalid scenario payload.", results.at("/runSummary/message").asString());
    }

    private static String shortScenarioJson() {
        return """
            {
                "durationTicks": 6,
                "seed": 7,
                "passengerFlows": [
                    {"startTick": 0, "originFloor": 0, "destinationFloor": 3, "passengers": 1}
                ]
            }
            """.trim();
    }

    private SimulationRunner.RunExecutionRequest executionRequest(SimulationRun run, String scenarioJson) {
        return new SimulationRunner.RunExecutionRequest(run.getId(), VALID_CONFIG_JSON, scenarioJson, "scenario");
    }

    private SimulationRun runWithArtefactDirectory(Long runId, Path runDir, String scenarioJson) {
        LiftSystem liftSystem = new LiftSystem("test-system", "Test System", "Test lift system");
        liftSystem.setId(10L);
        LiftSystemVersion version = new LiftSystemVersion();
        version.setId(20L);
        version.setLiftSystem(liftSystem);
        version.setVersionNumber(1);
        version.setConfig(VALID_CONFIG_JSON);

        SimulationRun run = new SimulationRun(liftSystem, version);
        run.setId(runId);
        run.setArtefactBasePath(runDir.toString());
        if (scenarioJson != null) {
            Scenario scenario = new Scenario("scenario", scenarioJson, version);
            scenario.setId(30L);
            run.setScenario(scenario);
        }
        return run;
    }

    private void stubLifecycle(SimulationRun run) {
        lenient().when(lifecycleManager.getByIdWithDetails(run.getId())).thenReturn(run);
        lenient().when(lifecycleManager.startRun(anyLong())).thenAnswer(invocation -> {
            run.start();
            return run;
        });
        lenient().when(lifecycleManager.failRun(anyLong(), any())).thenAnswer(invocation -> {
            run.fail(invocation.getArgument(1));
            return run;
        });
    }

    private ConfigValidationResponse validConfigResponse() {
        return new ConfigValidationResponse(true, List.of(), List.of());
    }

    private ConfigValidationResponse invalidConfigResponse() {
        return new ConfigValidationResponse(
            false,
            List.of(new ValidationIssue("maxFloor", "maxFloor must be greater than or equal to minFloor",
                ValidationIssue.Severity.ERROR)),
            List.of()
        );
    }

    private ScenarioValidationResponse validScenarioResponse() {
        return new ScenarioValidationResponse(true, List.of(), List.of());
    }

    private ScenarioValidationResponse invalidScenarioResponse() {
        return new ScenarioValidationResponse(
            false,
            List.of(new ValidationIssue("durationTicks", "durationTicks must be at least 1",
                ValidationIssue.Severity.ERROR)),
            List.of()
        );
    }
}
