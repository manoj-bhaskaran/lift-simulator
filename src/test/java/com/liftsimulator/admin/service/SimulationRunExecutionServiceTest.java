package com.liftsimulator.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftsimulator.admin.dto.ConfigValidationResponse;
import com.liftsimulator.admin.dto.ScenarioValidationResponse;
import com.liftsimulator.admin.dto.ValidationIssue;
import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.entity.Scenario;
import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.repository.SimulationRunRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SimulationRunExecutionService.
 */
@ExtendWith(MockitoExtension.class)
public class SimulationRunExecutionServiceTest {

    private static final String VALID_CONFIG = """
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

    private static final String SHORT_SCENARIO = """
        {
            "durationTicks": 6,
            "seed": 7,
            "passengerFlows": [
                {
                    "startTick": 0,
                    "originFloor": 0,
                    "destinationFloor": 3,
                    "passengers": 1
                }
            ]
        }
        """.trim();

    @Mock
    private SimulationRunRepository runRepository;

    @Mock
    private ConfigValidationService configValidationService;

    @Mock
    private ScenarioValidationService scenarioValidationService;

    @TempDir
    private Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private SimulationRunExecutionService executionService;

    @BeforeEach
    public void setUp() {
        executionService = new SimulationRunExecutionService(
                runRepository,
                configValidationService,
                scenarioValidationService,
                objectMapper
        );
    }

    @AfterEach
    public void tearDown() {
        executionService.shutdownExecutor();
    }

    @Test
    public void testUpdateProgressUsesRepositoryDirectly() throws Exception {
        SimulationRun run = new SimulationRun();
        run.setId(1L);
        run.setCurrentTick(500L);
        when(runRepository.updateCurrentTick(1L, 500L)).thenReturn(1);
        when(runRepository.findById(1L)).thenReturn(Optional.of(run));

        Method updateProgress = SimulationRunExecutionService.class.getDeclaredMethod(
                "updateProgress", Long.class, Long.class);
        updateProgress.setAccessible(true);
        SimulationRun result = (SimulationRun) updateProgress.invoke(executionService, 1L, 500L);

        assertNotNull(result);
        assertEquals(500L, result.getCurrentTick());
        verify(runRepository).updateCurrentTick(1L, 500L);
        verify(runRepository).findById(1L);
    }

    @Test
    public void submitRunForExecutionWritesArtefactsAndMarksRunSucceeded() throws Exception {
        Path runDir = tempDir.resolve("run-1");
        Files.createDirectories(runDir);
        SimulationRun run = runWithArtefactDirectory(1L, runDir, SHORT_SCENARIO);
        AtomicReference<SimulationRun> storedRun = prepareRepository(run);
        when(configValidationService.validate(VALID_CONFIG)).thenReturn(validConfigResponse());
        when(scenarioValidationService.validate(any(JsonNode.class))).thenReturn(validScenarioResponse());

        executionService.submitRunForExecution(1L);

        waitForExecutionToFinish(storedRun, SimulationRun.RunStatus.SUCCEEDED);
        assertEquals(6L, storedRun.get().getTotalTicks());
        assertEquals(6L, storedRun.get().getCurrentTick());
        assertEquals(7L, storedRun.get().getSeed());
        assertTrue(Files.exists(runDir.resolve("config.json")));
        assertTrue(Files.exists(runDir.resolve("scenario.json")));
        assertTrue(Files.readString(runDir.resolve("run.log")).contains("Simulation succeeded for run 1"));

        JsonNode results = objectMapper.readTree(runDir.resolve("results.json").toFile());
        assertEquals("SUCCEEDED", results.at("/runSummary/status").asText());
        assertEquals(1L, results.at("/runSummary/runId").asLong());
        assertTrue(results.has("kpis"));
        assertInternalStateCleared(1L);
    }

    @Test
    public void submitRunForExecutionFailsRunAndWritesResultsWhenScenarioIsMissing() throws Exception {
        Path runDir = tempDir.resolve("run-2");
        Files.createDirectories(runDir);
        SimulationRun run = runWithArtefactDirectory(2L, runDir, null);
        AtomicReference<SimulationRun> storedRun = prepareRepository(run);

        executionService.submitRunForExecution(2L);

        waitForExecutionToFinish(storedRun, SimulationRun.RunStatus.FAILED);
        assertEquals("Missing scenario payload for run.", storedRun.get().getErrorMessage());
        assertTrue(Files.exists(runDir.resolve("config.json")));
        assertFalse(Files.exists(runDir.resolve("scenario.json")));
        assertTrue(Files.readString(runDir.resolve("run.log")).contains("Missing scenario payload for run 2"));

        JsonNode results = objectMapper.readTree(runDir.resolve("results.json").toFile());
        assertEquals("FAILED", results.at("/runSummary/status").asText());
        assertEquals("Missing scenario payload for run.", results.at("/runSummary/message").asText());
        assertInternalStateCleared(2L);
    }

    @Test
    public void submitRunForExecutionFailsRunAndWritesResultsWhenScenarioValidationFails() throws Exception {
        Path runDir = tempDir.resolve("run-3");
        Files.createDirectories(runDir);
        SimulationRun run = runWithArtefactDirectory(3L, runDir, SHORT_SCENARIO);
        AtomicReference<SimulationRun> storedRun = prepareRepository(run);
        when(configValidationService.validate(VALID_CONFIG)).thenReturn(validConfigResponse());
        when(scenarioValidationService.validate(any(JsonNode.class))).thenReturn(invalidScenarioResponse());

        executionService.submitRunForExecution(3L);

        waitForExecutionToFinish(storedRun, SimulationRun.RunStatus.FAILED);
        assertEquals("Invalid scenario payload.", storedRun.get().getErrorMessage());
        assertTrue(Files.readString(runDir.resolve("run.log")).contains("Scenario validation failed"));

        JsonNode results = objectMapper.readTree(runDir.resolve("results.json").toFile());
        assertEquals("FAILED", results.at("/runSummary/status").asText());
        assertEquals("Invalid scenario payload.", results.at("/runSummary/message").asText());
        assertInternalStateCleared(3L);
    }

    private AtomicReference<SimulationRun> prepareRepository(SimulationRun run) {
        AtomicReference<SimulationRun> storedRun = new AtomicReference<>(run);
        when(runRepository.findById(run.getId())).thenAnswer(invocation -> Optional.of(storedRun.get()));
        when(runRepository.save(any(SimulationRun.class))).thenAnswer(invocation -> {
            SimulationRun savedRun = invocation.getArgument(0);
            storedRun.set(savedRun);
            return savedRun;
        });
        lenient().when(runRepository.updateCurrentTick(anyLong(), anyLong())).thenAnswer(invocation -> {
            storedRun.get().setCurrentTick(invocation.getArgument(1));
            return 1;
        });
        return storedRun;
    }

    private SimulationRun runWithArtefactDirectory(Long runId, Path runDir, String scenarioJson) {
        LiftSystem liftSystem = new LiftSystem("test-system", "Test System", "Test lift system");
        liftSystem.setId(10L);
        LiftSystemVersion version = new LiftSystemVersion();
        version.setId(20L);
        version.setLiftSystem(liftSystem);
        version.setVersionNumber(1);
        version.setConfig(VALID_CONFIG);

        SimulationRun run = new SimulationRun(liftSystem, version);
        run.setId(runId);
        run.setArtefactBasePath(runDir.toString());
        if (scenarioJson != null) {
            Scenario scenario = new Scenario("Short scenario", scenarioJson, version);
            scenario.setId(30L);
            run.setScenario(scenario);
        }
        return run;
    }

    private ConfigValidationResponse validConfigResponse() {
        return new ConfigValidationResponse(true, List.of(), List.of());
    }

    private ScenarioValidationResponse validScenarioResponse() {
        return new ScenarioValidationResponse(true, List.of(), List.of());
    }

    private ScenarioValidationResponse invalidScenarioResponse() {
        return new ScenarioValidationResponse(
                false,
                List.of(new ValidationIssue(
                        "durationTicks",
                        "durationTicks must be at least 1",
                        ValidationIssue.Severity.ERROR
                )),
                List.of()
        );
    }

    private void waitForExecutionToFinish(AtomicReference<SimulationRun> storedRun,
                                          SimulationRun.RunStatus expectedStatus) {
        assertTimeoutPreemptively(Duration.ofSeconds(5), () -> {
            while (storedRun.get().getStatus() != expectedStatus || hasInternalState(storedRun.get().getId())) {
                Thread.sleep(25);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private boolean hasInternalState(Long runId) throws ReflectiveOperationException {
        Field runningTasksField = SimulationRunExecutionService.class.getDeclaredField("runningTasks");
        runningTasksField.setAccessible(true);
        Map<Long, ?> runningTasks = (Map<Long, ?>) runningTasksField.get(executionService);

        Field cancellationTokensField = SimulationRunExecutionService.class.getDeclaredField("cancellationTokens");
        cancellationTokensField.setAccessible(true);
        Map<Long, ?> cancellationTokens = (Map<Long, ?>) cancellationTokensField.get(executionService);
        return runningTasks.containsKey(runId) || cancellationTokens.containsKey(runId);
    }

    private void assertInternalStateCleared(Long runId) throws ReflectiveOperationException {
        assertFalse(hasInternalState(runId));
    }
}
