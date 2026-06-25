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
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
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

    private static final String MULTI_PASSENGER_SCENARIO = """
        {
            "durationTicks": 20,
            "seed": 42,
            "passengerFlows": [
                {
                    "startTick": 0,
                    "originFloor": 0,
                    "destinationFloor": 3,
                    "passengers": 5
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

    @Mock
    private BatchInputGenerator batchInputGenerator;

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
                batchInputGenerator,
                objectMapper,
                2,
                10
        );
    }

    @AfterEach
    public void tearDown() {
        executionService.shutdownExecutor();
    }

    @Test
    public void testUpdateProgressUsesRepositoryDirectlyWithoutReloadingRun() throws Exception {
        when(runRepository.updateCurrentTick(1L, 500L)).thenReturn(1);

        Method updateProgress = SimulationRunExecutionService.class.getDeclaredMethod(
                "updateProgress", Long.class, Long.class);
        updateProgress.setAccessible(true);
        updateProgress.invoke(executionService, 1L, 500L);

        verify(runRepository).updateCurrentTick(1L, 500L);
        verify(runRepository, never()).findById(1L);
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
        assertFalse(Files.exists(runDir.resolve("config.json")));
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

    @Test
    public void submitRunForExecutionDoesNotWriteInputArtefactsWhenScenarioValidationFails() throws Exception {
        Path runDir = tempDir.resolve("run-validation-fails");
        Files.createDirectories(runDir);
        SimulationRun run = runWithArtefactDirectory(13L, runDir, SHORT_SCENARIO);
        AtomicReference<SimulationRun> storedRun = prepareRepository(run);
        when(configValidationService.validate(VALID_CONFIG)).thenReturn(validConfigResponse());
        when(scenarioValidationService.validate(any(JsonNode.class))).thenReturn(invalidScenarioResponse());

        executionService.submitRunForExecution(13L);

        waitForExecutionToFinish(storedRun, SimulationRun.RunStatus.FAILED);
        assertFalse(Files.exists(runDir.resolve("config.json")));
        assertFalse(Files.exists(runDir.resolve("scenario.json")));
        assertFalse(Files.exists(runDir.resolve("input.scenario")));
        verify(batchInputGenerator, never()).generateBatchInputFile(any(), any(), any(), any());
        assertInternalStateCleared(13L);
    }

    @Test
    public void submitRunForExecutionDoesNotWriteInputArtefactsWhenConfigValidationFails() throws Exception {
        Path runDir = tempDir.resolve("run-config-validation-fails");
        Files.createDirectories(runDir);
        SimulationRun run = runWithArtefactDirectory(14L, runDir, SHORT_SCENARIO);
        AtomicReference<SimulationRun> storedRun = prepareRepository(run);
        when(configValidationService.validate(VALID_CONFIG)).thenReturn(invalidConfigResponse());

        executionService.submitRunForExecution(14L);

        waitForExecutionToFinish(storedRun, SimulationRun.RunStatus.FAILED);
        assertFalse(Files.exists(runDir.resolve("config.json")));
        assertFalse(Files.exists(runDir.resolve("scenario.json")));
        assertFalse(Files.exists(runDir.resolve("input.scenario")));
        verify(batchInputGenerator, never()).generateBatchInputFile(any(), any(), any(), any());
        assertInternalStateCleared(14L);
    }


    @Test
    public void multiPassengerFlowProducesOneRequestButCorrectPassengerKpis() throws Exception {
        Path runDir = tempDir.resolve("run-mp");
        Files.createDirectories(runDir);
        SimulationRun run = runWithArtefactDirectory(20L, runDir, MULTI_PASSENGER_SCENARIO);
        AtomicReference<SimulationRun> storedRun = prepareRepository(run);
        when(configValidationService.validate(VALID_CONFIG)).thenReturn(validConfigResponse());
        when(scenarioValidationService.validate(any(JsonNode.class))).thenReturn(validScenarioResponse());

        executionService.submitRunForExecution(20L);

        waitForExecutionToFinish(storedRun, SimulationRun.RunStatus.SUCCEEDED);
        JsonNode results = objectMapper.readTree(runDir.resolve("results.json").toFile());
        JsonNode kpis = results.get("kpis");
        // One hall call for the 5-passenger flow → 1 request lifecycle
        assertEquals(1, kpis.get("pickupRequestsServed").asInt());
        // But 5 passengers represented → passengersServed must be 5
        assertEquals(5, kpis.get("passengersServed").asInt());
        assertEquals(0, kpis.get("passengersCancelled").asInt());
        assertInternalStateCleared(20L);
    }

    @Test
    public void sameTickRequestReportsAtLeastOneTickWait() throws Exception {
        Path runDir = tempDir.resolve("run-wait");
        Files.createDirectories(runDir);
        SimulationRun run = runWithArtefactDirectory(21L, runDir, SHORT_SCENARIO);
        AtomicReference<SimulationRun> storedRun = prepareRepository(run);
        when(configValidationService.validate(VALID_CONFIG)).thenReturn(validConfigResponse());
        when(scenarioValidationService.validate(any(JsonNode.class))).thenReturn(validScenarioResponse());

        executionService.submitRunForExecution(21L);

        waitForExecutionToFinish(storedRun, SimulationRun.RunStatus.SUCCEEDED);
        JsonNode results = objectMapper.readTree(runDir.resolve("results.json").toFile());
        double avgWait = results.at("/kpis/avgPickupWaitTicks").asDouble();
        assertTrue(avgWait >= 1.0,
            "A request created and served within the simulation must report at least 1 tick wait, got " + avgWait);
    }

    @Test
    public void finalPostTickLiftStateIsRecorded() throws Exception {
        Path runDir = tempDir.resolve("run-final-state");
        Files.createDirectories(runDir);
        SimulationRun run = runWithArtefactDirectory(22L, runDir, SHORT_SCENARIO);
        AtomicReference<SimulationRun> storedRun = prepareRepository(run);
        when(configValidationService.validate(VALID_CONFIG)).thenReturn(validConfigResponse());
        when(scenarioValidationService.validate(any(JsonNode.class))).thenReturn(validScenarioResponse());

        executionService.submitRunForExecution(22L);

        waitForExecutionToFinish(storedRun, SimulationRun.RunStatus.SUCCEEDED);
        JsonNode results = objectMapper.readTree(runDir.resolve("results.json").toFile());
        // recordLiftState fires after engine.tick() inside the loop, so totalTicks == durationTicks
        long perLiftTotalTicks = results.at("/perLift/0/totalTicks").asLong();
        assertEquals(6L, perLiftTotalTicks,
            "totalTicks must equal durationTicks so metrics and run record stay consistent");
    }

    @Test
    public void failRunWithMessageUsesDirectFailedUpdateWhenLifecycleTransitionRaces() throws Exception {
        SimulationRun run = new SimulationRun();
        run.setId(9L);
        run.setStatus(SimulationRun.RunStatus.RUNNING);
        when(runRepository.findById(9L)).thenReturn(Optional.of(run));
        when(runRepository.save(any(SimulationRun.class))).thenThrow(new IllegalStateException("stale state"));
        when(runRepository.markRunningRunFailed(anyLong(), any(String.class), any())).thenReturn(1);

        Method failRunWithMessage = SimulationRunExecutionService.class.getDeclaredMethod(
                "failRunWithMessage", Long.class, String.class, boolean.class);
        failRunWithMessage.setAccessible(true);
        failRunWithMessage.invoke(executionService, 9L, "boom", true);

        verify(runRepository).markRunningRunFailed(anyLong(), any(String.class), any());
    }

    @Test
    public void cancellingQueuedRunReleasesQueueSlot() throws Exception {
        // Pool=1, queue=1: one thread running, one slot queued.
        // Cancelling the queued run should free the slot immediately so a new submission succeeds.
        SimulationRunExecutionService tightService = new SimulationRunExecutionService(
                runRepository,
                configValidationService,
                scenarioValidationService,
                batchInputGenerator,
                objectMapper,
                1,
                1
        );
        try {
            java.lang.reflect.Field executorField =
                SimulationRunExecutionService.class.getDeclaredField("executor");
            executorField.setAccessible(true);
            java.util.concurrent.ThreadPoolExecutor tpe =
                (java.util.concurrent.ThreadPoolExecutor) executorField.get(tightService);

            java.util.concurrent.CountDownLatch blockLatch = new java.util.concurrent.CountDownLatch(1);

            // Fill pool thread with a blocking task
            tpe.submit(() -> {
                try { blockLatch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });

            // Submit a run that will sit in the queue (pool thread is blocked)
            Path runDir = tempDir.resolve("run-cancel-queue");
            Files.createDirectories(runDir);
            SimulationRun queuedRun = runWithArtefactDirectory(201L, runDir, SHORT_SCENARIO);
            AtomicReference<SimulationRun> storedQueued = new AtomicReference<>(queuedRun);
            lenient().when(runRepository.findById(201L)).thenAnswer(inv -> Optional.of(storedQueued.get()));
            lenient().when(runRepository.save(any(SimulationRun.class))).thenAnswer(inv -> {
                SimulationRun saved = inv.getArgument(0);
                if (Long.valueOf(201L).equals(saved.getId())) storedQueued.set(saved);
                return saved;
            });

            tightService.submitRunForExecution(201L);
            // Queue slot is now occupied by run 201

            // Cancel the queued run — should dequeue it and release the slot
            tightService.cancelRun(201L);

            // Queue slot is free; a 3rd submission must NOT be rejected
            boolean rejected = false;
            try {
                tpe.submit(() -> {}); // should fit in the now-free slot
            } catch (java.util.concurrent.RejectedExecutionException ex) {
                rejected = true;
            }
            assertFalse(rejected, "Cancelling a queued run must release its queue slot");

            blockLatch.countDown();
        } finally {
            tightService.shutdownExecutor();
        }
    }

    @Test
    public void submissionsExceedingPoolPlusQueueAreRejectedWithFailedStatus() throws Exception {
        // Pool size=2, queue=10 → capacity=12. Submit 13 runs; the 13th must be rejected cleanly.
        SimulationRunExecutionService tightService = new SimulationRunExecutionService(
                runRepository,
                configValidationService,
                scenarioValidationService,
                batchInputGenerator,
                objectMapper,
                1,
                1
        );
        // Capacity = 1 thread + 1 queue slot = 2. A 3rd submission must be rejected.
        int overCapacityRunId = 99;
        SimulationRun overCapacityRun = new SimulationRun();
        overCapacityRun.setId((long) overCapacityRunId);
        overCapacityRun.setStatus(SimulationRun.RunStatus.CREATED);

        LiftSystem liftSystem = new LiftSystem("sys", "Sys", "desc");
        liftSystem.setId(10L);
        LiftSystemVersion version = new LiftSystemVersion();
        version.setId(20L);
        version.setLiftSystem(liftSystem);
        version.setVersionNumber(1);
        version.setConfig(VALID_CONFIG);
        overCapacityRun.setLiftSystem(liftSystem);
        overCapacityRun.setVersion(version);

        AtomicReference<SimulationRun> storedOverCapacity = new AtomicReference<>(overCapacityRun);

        // Block the pool thread so queue fills up too
        java.util.concurrent.CountDownLatch block = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.CountDownLatch executing = new java.util.concurrent.CountDownLatch(1);

        // IDs 97 and 98 will occupy the thread and queue slot
        for (int id : new int[]{97, 98}) {
            SimulationRun blocker = new SimulationRun();
            blocker.setId((long) id);
            blocker.setStatus(SimulationRun.RunStatus.CREATED);
            blocker.setLiftSystem(liftSystem);
            blocker.setVersion(version);
            lenient().when(runRepository.findById((long) id)).thenReturn(Optional.of(blocker));
            lenient().when(runRepository.save(any(SimulationRun.class))).thenAnswer(inv -> inv.getArgument(0));
            lenient().when(runRepository.updateCurrentTick(anyLong(), anyLong())).thenReturn(1);
        }

        // Use a scenario that blocks mid-way via the CountDownLatch approach is complex;
        // instead we just verify the 3rd submission fails the run immediately.
        lenient().when(runRepository.findById((long) overCapacityRunId)).thenReturn(Optional.of(overCapacityRun));
        lenient().when(runRepository.save(any(SimulationRun.class))).thenAnswer(inv -> {
            SimulationRun saved = inv.getArgument(0);
            if (saved.getId() == overCapacityRunId) {
                storedOverCapacity.set(saved);
            }
            return saved;
        });

        // Fill the pool (1 thread) and queue (1 slot) with long-running tasks
        tightService.getClass(); // just to ensure class is loaded; real work below

        // Submit via reflection to avoid needing scenario setup; instead verify rejection at executor level
        // by using a dedicated service with a saturated pool/queue.
        // We submit two tasks that block, then the third should be rejected.
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        java.lang.reflect.Field executorField = SimulationRunExecutionService.class.getDeclaredField("executor");
        executorField.setAccessible(true);
        java.util.concurrent.ThreadPoolExecutor tpe =
            (java.util.concurrent.ThreadPoolExecutor) executorField.get(tightService);

        // Fill pool and queue with blocking runnables
        tpe.submit(() -> { try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });
        tpe.submit(() -> { try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } });

        // Now submit the overCapacity run — executor is full, should be rejected cleanly
        lenient().when(runRepository.findById((long) overCapacityRunId)).thenReturn(Optional.of(overCapacityRun));

        Scenario scenario = new Scenario("s", SHORT_SCENARIO, version);
        overCapacityRun.setScenario(scenario);
        overCapacityRun.setArtefactBasePath(tempDir.resolve("run-99").toString());
        Files.createDirectories(tempDir.resolve("run-99"));

        tightService.submitRunForExecution((long) overCapacityRunId);

        latch.countDown();
        tightService.shutdownExecutor();

        assertEquals(SimulationRun.RunStatus.FAILED, storedOverCapacity.get().getStatus(),
            "Run rejected by a full queue must be marked FAILED, not left in CREATED state");
        assertEquals("Simulation queue is full; run rejected.", storedOverCapacity.get().getErrorMessage(),
            "Rejected run must carry a descriptive error message");
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

    private ConfigValidationResponse invalidConfigResponse() {
        return new ConfigValidationResponse(
                false,
                List.of(new ValidationIssue(
                        "maxFloor",
                        "maxFloor must be greater than or equal to minFloor",
                        ValidationIssue.Severity.ERROR
                )),
                List.of()
        );
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
