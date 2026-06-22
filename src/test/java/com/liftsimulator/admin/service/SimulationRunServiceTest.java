package com.liftsimulator.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.entity.Scenario;
import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.entity.SimulationRun.RunStatus;
import com.liftsimulator.admin.repository.LiftSystemRepository;
import com.liftsimulator.admin.repository.LiftSystemVersionRepository;
import com.liftsimulator.admin.repository.ScenarioRepository;
import com.liftsimulator.admin.repository.SimulationRunRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SimulationRunService.
 */
@ExtendWith(MockitoExtension.class)
public class SimulationRunServiceTest {

    @Mock
    private SimulationRunRepository runRepository;

    @Mock
    private LiftSystemRepository liftSystemRepository;

    @Mock
    private LiftSystemVersionRepository versionRepository;

    @Mock
    private ScenarioRepository scenarioRepository;

    @Mock
    private SimulationRunExecutionService executionService;

    @Mock
    private ArtefactService artefactService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private EntityManager entityManager;

    private SimulationRunService runService;

    private LiftSystem mockLiftSystem;
    private LiftSystemVersion mockVersion;
    private SimulationRun mockRun;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        runService = new SimulationRunService(
                runRepository,
                liftSystemRepository,
                versionRepository,
                scenarioRepository,
                executionService,
                artefactService,
                objectMapper,
                tempDir.toString()
        );

        // Inject EntityManager mock via reflection (it's normally injected by @PersistenceContext)
        java.lang.reflect.Field entityManagerField = SimulationRunService.class.getDeclaredField("entityManager");
        entityManagerField.setAccessible(true);
        entityManagerField.set(runService, entityManager);

        mockLiftSystem = new LiftSystem();
        mockLiftSystem.setId(1L);
        mockLiftSystem.setSystemKey("test-system");

        mockVersion = new LiftSystemVersion();
        mockVersion.setId(1L);
        mockVersion.setVersionNumber(1);
        mockVersion.setLiftSystem(mockLiftSystem);
        mockVersion.setConfig("{\"minFloor\":0,\"maxFloor\":10}");

        mockRun = new SimulationRun();
        mockRun.setId(1L);
        mockRun.setLiftSystem(mockLiftSystem);
        mockRun.setVersion(mockVersion);
        mockRun.setStatus(RunStatus.CREATED);
        mockRun.setCreatedAt(OffsetDateTime.now());
    }

    @Test
    public void testCreateRun_Success() {
        when(liftSystemRepository.findById(1L)).thenReturn(Optional.of(mockLiftSystem));
        when(versionRepository.findById(1L)).thenReturn(Optional.of(mockVersion));
        when(runRepository.save(any(SimulationRun.class))).thenReturn(mockRun);

        SimulationRun result = runService.createRun(1L, 1L, null);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(liftSystemRepository).findById(1L);
        verify(versionRepository).findById(1L);
        verify(runRepository).save(any(SimulationRun.class));
    }

    @Test
    public void testCreateRun_LiftSystemNotFound() {
        when(liftSystemRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> runService.createRun(999L, 1L, null)
        );

        assertEquals("Lift system not found with id: 999", exception.getMessage());
        verify(liftSystemRepository).findById(999L);
    }

    @Test
    public void testCreateRun_VersionNotFound() {
        when(liftSystemRepository.findById(1L)).thenReturn(Optional.of(mockLiftSystem));
        when(versionRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> runService.createRun(1L, 999L, null)
        );

        assertEquals("Lift system version not found with id: 999", exception.getMessage());
        verify(versionRepository).findById(999L);
    }

    @Test
    public void testCreateRun_VersionBelongsToDifferentSystem() {
        LiftSystem otherSystem = new LiftSystem();
        otherSystem.setId(2L);
        otherSystem.setSystemKey("other-system");

        LiftSystemVersion versionFromOtherSystem = new LiftSystemVersion();
        versionFromOtherSystem.setId(5L);
        versionFromOtherSystem.setVersionNumber(1);
        versionFromOtherSystem.setLiftSystem(otherSystem);

        when(liftSystemRepository.findById(1L)).thenReturn(Optional.of(mockLiftSystem));
        when(versionRepository.findById(5L)).thenReturn(Optional.of(versionFromOtherSystem));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> runService.createRun(1L, 5L, null)
        );

        assertEquals("Version 5 does not belong to lift system 1", exception.getMessage());
        verify(liftSystemRepository).findById(1L);
        verify(versionRepository).findById(5L);
    }

    @Test
    public void testCreateRun_WithScenarioAttachesScenarioWhenVersionMatches() {
        Scenario scenario = new Scenario("Morning Rush", "{}", mockVersion);
        scenario.setId(7L);
        when(liftSystemRepository.findById(1L)).thenReturn(Optional.of(mockLiftSystem));
        when(versionRepository.findById(1L)).thenReturn(Optional.of(mockVersion));
        when(scenarioRepository.findById(7L)).thenReturn(Optional.of(scenario));
        when(runRepository.save(any(SimulationRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SimulationRun result = runService.createRun(1L, 1L, 7L);

        assertNotNull(result);
        assertEquals(scenario, result.getScenario());
        ArgumentCaptor<SimulationRun> runCaptor = ArgumentCaptor.forClass(SimulationRun.class);
        verify(runRepository).save(runCaptor.capture());
        assertEquals(7L, runCaptor.getValue().getScenario().getId());
        verify(scenarioRepository).findById(7L);
    }

    @Test
    public void testCreateRun_WithScenarioFromDifferentVersionFails() {
        LiftSystemVersion otherVersion = new LiftSystemVersion();
        otherVersion.setId(2L);
        otherVersion.setLiftSystem(mockLiftSystem);
        Scenario scenario = new Scenario("Other Scenario", "{}", otherVersion);
        scenario.setId(7L);
        when(liftSystemRepository.findById(1L)).thenReturn(Optional.of(mockLiftSystem));
        when(versionRepository.findById(1L)).thenReturn(Optional.of(mockVersion));
        when(scenarioRepository.findById(7L)).thenReturn(Optional.of(scenario));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> runService.createRun(1L, 1L, 7L)
        );

        assertEquals("Scenario 7 does not belong to version 1", exception.getMessage());
        verify(runRepository, never()).save(any(SimulationRun.class));
    }

    @Test
    public void testCreateAndStartRun_ConfiguresRunStartsAndSubmitsExecution() throws Exception {
        when(liftSystemRepository.findById(1L)).thenReturn(Optional.of(mockLiftSystem));
        when(versionRepository.findById(1L)).thenReturn(Optional.of(mockVersion));
        when(runRepository.save(any(SimulationRun.class))).thenAnswer(invocation -> {
            SimulationRun savedRun = invocation.getArgument(0);
            if (savedRun.getId() == null) {
                savedRun.setId(42L);
            }
            return savedRun;
        });

        SimulationRun result = runService.createAndStartRun(1L, 1L, null, 12345L);

        assertEquals(42L, result.getId());
        assertEquals(RunStatus.RUNNING, result.getStatus());
        // totalTicks is null when no scenario is provided (will be set during execution)
        assertEquals(null, result.getTotalTicks());
        assertEquals(12345L, result.getSeed());
        assertNotNull(result.getArtefactBasePath());
        assertTrue(Files.isDirectory(Path.of(result.getArtefactBasePath())));
        verify(executionService).submitRunForExecution(42L);
    }

    @Test
    public void testCreateAndStartRun_SubmitsExecutionAfterTransactionCommit() throws Exception {
        when(liftSystemRepository.findById(1L)).thenReturn(Optional.of(mockLiftSystem));
        when(versionRepository.findById(1L)).thenReturn(Optional.of(mockVersion));
        when(runRepository.save(any(SimulationRun.class))).thenAnswer(invocation -> {
            SimulationRun savedRun = invocation.getArgument(0);
            if (savedRun.getId() == null) {
                savedRun.setId(42L);
            }
            return savedRun;
        });

        TransactionSynchronizationManager.initSynchronization();
        try {
            SimulationRun result = runService.createAndStartRun(1L, 1L, null, 12345L);

            assertEquals(42L, result.getId());
            verify(executionService, never()).submitRunForExecution(42L);

            List<TransactionSynchronization> synchronizations =
                    TransactionSynchronizationManager.getSynchronizations();
            assertEquals(1, synchronizations.size());

            synchronizations.get(0).afterCommit();
            verify(executionService).submitRunForExecution(42L);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    public void testCreateAndStartRun_DerivesTotalTicksFromScenario() throws Exception {
        Scenario mockScenario = new Scenario();
        mockScenario.setId(5L);
        mockScenario.setName("Test Scenario");
        mockScenario.setScenarioJson("{\"durationTicks\": 5000, \"passengerFlows\": []}");
        mockScenario.setLiftSystemVersion(mockVersion);

        when(liftSystemRepository.findById(1L)).thenReturn(Optional.of(mockLiftSystem));
        when(versionRepository.findById(1L)).thenReturn(Optional.of(mockVersion));
        when(scenarioRepository.findById(5L)).thenReturn(Optional.of(mockScenario));
        when(objectMapper.readValue(
            "{\"durationTicks\": 5000, \"passengerFlows\": []}",
            com.liftsimulator.admin.dto.ScenarioDefinitionDTO.class
        )).thenReturn(new com.liftsimulator.admin.dto.ScenarioDefinitionDTO(5000, List.of(), null));
        when(runRepository.save(any(SimulationRun.class))).thenAnswer(invocation -> {
            SimulationRun savedRun = invocation.getArgument(0);
            if (savedRun.getId() == null) {
                savedRun.setId(42L);
            }
            return savedRun;
        });

        SimulationRun result = runService.createAndStartRun(1L, 1L, 5L, 12345L);

        assertEquals(42L, result.getId());
        assertEquals(RunStatus.RUNNING, result.getStatus());
        assertEquals(5000L, result.getTotalTicks());
        assertEquals(12345L, result.getSeed());
        assertNotNull(result.getArtefactBasePath());
        assertTrue(Files.isDirectory(Path.of(result.getArtefactBasePath())));
        verify(executionService).submitRunForExecution(42L);
    }

    @Test
    public void testGetRunById_Success() {
        when(runRepository.findById(1L)).thenReturn(Optional.of(mockRun));

        SimulationRun result = runService.getRunById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(runRepository).findById(1L);
    }

    @Test
    public void testGetRunById_NotFound() {
        when(runRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> runService.getRunById(999L)
        );

        assertEquals("Simulation run not found with id: 999", exception.getMessage());
        verify(runRepository).findById(999L);
    }

    @Test
    public void testGetRunsByLiftSystem() {
        List<SimulationRun> runs = Arrays.asList(mockRun);
        when(runRepository.findByLiftSystemIdOrderByCreatedAtDesc(1L)).thenReturn(runs);

        List<SimulationRun> result = runService.getRunsByLiftSystem(1L);

        assertEquals(1, result.size());
        verify(runRepository).findByLiftSystemIdOrderByCreatedAtDesc(1L);
    }

    @Test
    public void testGetRunsByStatus() {
        List<SimulationRun> runs = Arrays.asList(mockRun);
        when(runRepository.findByStatusOrderByCreatedAtDesc(RunStatus.CREATED)).thenReturn(runs);

        List<SimulationRun> result = runService.getRunsByStatus(RunStatus.CREATED);

        assertEquals(1, result.size());
        verify(runRepository).findByStatusOrderByCreatedAtDesc(RunStatus.CREATED);
    }

    @Test
    public void testGetActiveRunsByLiftSystem() {
        List<SimulationRun> runs = Arrays.asList(mockRun);
        when(runRepository.findActiveRunsByLiftSystemId(1L)).thenReturn(runs);

        List<SimulationRun> result = runService.getActiveRunsByLiftSystem(1L);

        assertEquals(1, result.size());
        verify(runRepository).findActiveRunsByLiftSystemId(1L);
    }

    @Test
    public void testStartRun_Success() {
        when(runRepository.findById(1L)).thenReturn(Optional.of(mockRun));
        when(runRepository.save(any(SimulationRun.class))).thenReturn(mockRun);

        SimulationRun result = runService.startRun(1L);

        assertNotNull(result);
        verify(runRepository).findById(1L);
        verify(runRepository).save(any(SimulationRun.class));
    }

    @Test
    public void testStartRun_InvalidStatus() {
        mockRun.setStatus(RunStatus.RUNNING);
        when(runRepository.findById(1L)).thenReturn(Optional.of(mockRun));

        assertThrows(
            IllegalStateException.class,
            () -> runService.startRun(1L)
        );

        verify(runRepository).findById(1L);
    }

    @Test
    public void testSucceedRun_Success() {
        mockRun.setStatus(RunStatus.RUNNING);
        when(runRepository.findById(1L)).thenReturn(Optional.of(mockRun));
        when(runRepository.save(any(SimulationRun.class))).thenReturn(mockRun);

        SimulationRun result = runService.succeedRun(1L);

        assertNotNull(result);
        verify(runRepository).findById(1L);
        verify(runRepository).save(any(SimulationRun.class));
    }

    @Test
    public void testSucceedRun_InvalidStatus() {
        mockRun.setStatus(RunStatus.CREATED);
        when(runRepository.findById(1L)).thenReturn(Optional.of(mockRun));

        assertThrows(
            IllegalStateException.class,
            () -> runService.succeedRun(1L)
        );

        verify(runRepository).findById(1L);
    }

    @Test
    public void testFailRun_Success() {
        mockRun.setStatus(RunStatus.RUNNING);
        when(runRepository.findById(1L)).thenReturn(Optional.of(mockRun));
        when(runRepository.save(any(SimulationRun.class))).thenReturn(mockRun);

        SimulationRun result = runService.failRun(1L, "Test error");

        assertNotNull(result);
        verify(runRepository).findById(1L);
        verify(runRepository).save(any(SimulationRun.class));
    }

    @Test
    public void testFailRun_InvalidStatus() {
        mockRun.setStatus(RunStatus.CREATED);
        when(runRepository.findById(1L)).thenReturn(Optional.of(mockRun));

        assertThrows(
            IllegalStateException.class,
            () -> runService.failRun(1L, "Test error")
        );

        verify(runRepository).findById(1L);
    }

    @Test
    public void testCancelRun_Success() {
        when(runRepository.findById(1L)).thenReturn(Optional.of(mockRun));
        when(runRepository.save(any(SimulationRun.class))).thenReturn(mockRun);

        SimulationRun result = runService.cancelRun(1L);

        assertNotNull(result);
        verify(runRepository).findById(1L);
        verify(runRepository).save(any(SimulationRun.class));
    }

    @Test
    public void testCancelRun_InvalidStatus() {
        mockRun.setStatus(RunStatus.SUCCEEDED);
        when(runRepository.findById(1L)).thenReturn(Optional.of(mockRun));

        assertThrows(
            IllegalStateException.class,
            () -> runService.cancelRun(1L)
        );

        verify(runRepository).findById(1L);
    }

    @Test
    public void testUpdateProgress_Success() {
        when(runRepository.updateCurrentTick(1L, 500L)).thenReturn(1);
        when(runRepository.findById(1L)).thenReturn(Optional.of(mockRun));

        SimulationRun result = runService.updateProgress(1L, 500L);

        assertNotNull(result);
        verify(runRepository).updateCurrentTick(1L, 500L);
        verify(runRepository).findById(1L);
    }

    @Test
    public void testConfigureRun_Success() {
        when(runRepository.findById(1L)).thenReturn(Optional.of(mockRun));
        when(runRepository.save(any(SimulationRun.class))).thenReturn(mockRun);

        SimulationRun result = runService.configureRun(1L, 1000L, 12345L, "/path/to/artefacts");

        assertNotNull(result);
        verify(runRepository).findById(1L);
        verify(runRepository).save(any(SimulationRun.class));
    }

    @Test
    public void testConfigureRun_PreservesExistingValuesWhenNullPassed() {
        // Set up a run with existing configuration
        mockRun.setTotalTicks(5000L);
        mockRun.setSeed(99999L);
        mockRun.setArtefactBasePath("/original/path");
        when(runRepository.findById(1L)).thenReturn(Optional.of(mockRun));
        when(runRepository.save(any(SimulationRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Configure with only artefactBasePath, keeping other values null
        SimulationRun result = runService.configureRun(1L, null, null, "/new/path");

        assertNotNull(result);
        // Existing values should be preserved
        assertEquals(5000L, result.getTotalTicks());
        assertEquals(99999L, result.getSeed());
        // New value should be updated
        assertEquals("/new/path", result.getArtefactBasePath());
        verify(runRepository).findById(1L);
        verify(runRepository).save(any(SimulationRun.class));
    }

    @Test
    public void testConfigureRun_PartialUpdate() {
        // Set up a run with existing configuration
        mockRun.setTotalTicks(5000L);
        mockRun.setSeed(99999L);
        mockRun.setArtefactBasePath("/original/path");
        when(runRepository.findById(1L)).thenReturn(Optional.of(mockRun));
        when(runRepository.save(any(SimulationRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Update only totalTicks, keeping seed null
        SimulationRun result = runService.configureRun(1L, 10000L, null, null);

        assertNotNull(result);
        // New value should be updated
        assertEquals(10000L, result.getTotalTicks());
        // Existing values should be preserved
        assertEquals(99999L, result.getSeed());
        assertEquals("/original/path", result.getArtefactBasePath());
        verify(runRepository).findById(1L);
        verify(runRepository).save(any(SimulationRun.class));
    }

    @Test
    public void testRecoverOrphanedRunsOnStartup_MarksActiveRunsTerminal() {
        when(runRepository.failOrphanedRunningRuns(any(String.class), any(OffsetDateTime.class))).thenReturn(2);
        when(runRepository.cancelOrphanedCreatedRuns(any(OffsetDateTime.class))).thenReturn(1);

        SimulationRunService.StartupRecoveryResult result = runService.recoverOrphanedRunsOnStartup();

        assertEquals(2, result.failedRunningRuns());
        assertEquals(1, result.cancelledCreatedRuns());
        verify(runRepository).failOrphanedRunningRuns(any(String.class), any(OffsetDateTime.class));
        verify(runRepository).cancelOrphanedCreatedRuns(any(OffsetDateTime.class));
    }

    @Test
    public void testRecoverOrphanedRunsOnStartup_UsesSameRecoveryTimestamp() {
        when(runRepository.failOrphanedRunningRuns(any(String.class), any(OffsetDateTime.class))).thenReturn(0);
        when(runRepository.cancelOrphanedCreatedRuns(any(OffsetDateTime.class))).thenReturn(0);

        runService.recoverOrphanedRunsOnStartup();

        ArgumentCaptor<OffsetDateTime> timestampCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(runRepository).failOrphanedRunningRuns(any(String.class), timestampCaptor.capture());
        verify(runRepository).cancelOrphanedCreatedRuns(timestampCaptor.capture());
        assertEquals(timestampCaptor.getAllValues().get(0), timestampCaptor.getAllValues().get(1));
    }

    @Test
    public void testDeleteRun_Success() throws Exception {
        mockRun.setStatus(RunStatus.SUCCEEDED);
        when(runRepository.findById(1L)).thenReturn(Optional.of(mockRun));

        runService.deleteRun(1L);

        verify(runRepository).findById(1L);
        verify(runRepository).deleteById(1L);
        verify(artefactService).deleteArtefacts(mockRun);
    }

    @Test
    public void testDeleteRun_DefersArtefactDeletionUntilAfterCommit() throws Exception {
        mockRun.setStatus(RunStatus.SUCCEEDED);
        when(runRepository.findById(1L)).thenReturn(Optional.of(mockRun));

        TransactionSynchronizationManager.initSynchronization();
        try {
            runService.deleteRun(1L);

            verify(runRepository).deleteById(1L);
            verify(artefactService, never()).deleteArtefacts(any());

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            verify(artefactService).deleteArtefacts(mockRun);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    public void testDeleteRun_NotFound() {
        when(runRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> runService.deleteRun(999L)
        );

        assertEquals("Simulation run not found with id: 999", exception.getMessage());
        verify(runRepository).findById(999L);
        verify(runRepository, never()).deleteById(any());
    }

    @Test
    public void testDeleteRun_RunningRunRejected() throws Exception {
        mockRun.setStatus(RunStatus.RUNNING);
        when(runRepository.findById(1L)).thenReturn(Optional.of(mockRun));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> runService.deleteRun(1L)
        );

        assertTrue(exception.getMessage().contains("RUNNING"));
        verify(artefactService, never()).deleteArtefacts(any());
        verify(runRepository, never()).deleteById(any());
    }

    @Test
    public void testDeleteRun_ArtefactDeletionFailureAfterCommitIsReported() throws Exception {
        mockRun.setStatus(RunStatus.FAILED);
        when(runRepository.findById(1L)).thenReturn(Optional.of(mockRun));
        doThrow(new java.io.IOException("disk error")).when(artefactService).deleteArtefacts(mockRun);

        TransactionSynchronizationManager.initSynchronization();
        try {
            runService.deleteRun(1L);
            TransactionSynchronization synchronization = TransactionSynchronizationManager.getSynchronizations().get(0);

            ArtefactDeletionException exception = assertThrows(
                ArtefactDeletionException.class,
                synchronization::afterCommit
            );

            assertTrue(exception.getMessage().contains("disk error"));
            verify(runRepository).deleteById(1L);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    public void testConfigureRun_WithNullDurationTicks() {
        mockRun.setTotalTicks(null);
        when(runRepository.findById(1L)).thenReturn(Optional.of(mockRun));
        when(runRepository.save(any(SimulationRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SimulationRun result = runService.configureRun(1L, null, 12345L, "/path/to/artefacts");

        assertNotNull(result);
        // Verify that the run can be configured even with null durationTicks
        // The null check occurs during execution, not configuration
        verify(runRepository).findById(1L);
        verify(runRepository).save(any(SimulationRun.class));
    }
}
