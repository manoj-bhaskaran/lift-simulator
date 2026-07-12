package com.liftsimulator.admin.service;

import tools.jackson.databind.ObjectMapper;
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
    private RunLifecycleManager lifecycleManager;

    private LiftSystem mockLiftSystem;
    private LiftSystemVersion mockVersion;
    private SimulationRun mockRun;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() throws Exception {
        lifecycleManager = new RunLifecycleManager(runRepository);
        java.lang.reflect.Field lifecycleEntityManagerField = RunLifecycleManager.class.getDeclaredField("entityManager");
        lifecycleEntityManagerField.setAccessible(true);
        lifecycleEntityManagerField.set(lifecycleManager, entityManager);

        runService = new SimulationRunService(
                runRepository,
                liftSystemRepository,
                versionRepository,
                scenarioRepository,
                executionService,
                artefactService,
                lifecycleManager,
                objectMapper,
                tempDir.toString()
        );

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
        when(versionRepository.findByLiftSystemIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(mockVersion));
        when(runRepository.save(any(SimulationRun.class))).thenReturn(mockRun);

        SimulationRun result = runService.createRun(1L, 1, null);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(liftSystemRepository).findById(1L);
        verify(versionRepository).findByLiftSystemIdAndVersionNumber(1L, 1);
        verify(runRepository).save(any(SimulationRun.class));
    }

    @Test
    public void testCreateRun_LiftSystemNotFound() {
        when(liftSystemRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> runService.createRun(999L, 1, null)
        );

        assertEquals("Lift system not found with id: 999", exception.getMessage());
        verify(liftSystemRepository).findById(999L);
    }

    @Test
    public void testCreateRun_VersionNotFound() {
        when(liftSystemRepository.findById(1L)).thenReturn(Optional.of(mockLiftSystem));
        when(versionRepository.findByLiftSystemIdAndVersionNumber(1L, 999)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> runService.createRun(1L, 999, null)
        );

        assertEquals("Lift system version not found with lift system id 1 and version number: 999", exception.getMessage());
        verify(versionRepository).findByLiftSystemIdAndVersionNumber(1L, 999);
    }

    @Test
    public void testCreateRun_VersionNumberNotFoundForLiftSystem() {
        when(liftSystemRepository.findById(1L)).thenReturn(Optional.of(mockLiftSystem));
        when(versionRepository.findByLiftSystemIdAndVersionNumber(1L, 5)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> runService.createRun(1L, 5, null)
        );

        assertEquals("Lift system version not found with lift system id 1 and version number: 5",
                exception.getMessage());
        verify(liftSystemRepository).findById(1L);
        verify(versionRepository).findByLiftSystemIdAndVersionNumber(1L, 5);
    }

    @Test
    public void testCreateRun_WithScenarioAttachesScenarioWhenVersionMatches() {
        Scenario scenario = new Scenario("Morning Rush", "{}", mockVersion);
        scenario.setId(7L);
        when(liftSystemRepository.findById(1L)).thenReturn(Optional.of(mockLiftSystem));
        when(versionRepository.findByLiftSystemIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(mockVersion));
        when(scenarioRepository.findById(7L)).thenReturn(Optional.of(scenario));
        when(runRepository.save(any(SimulationRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SimulationRun result = runService.createRun(1L, 1, 7L);

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
        when(versionRepository.findByLiftSystemIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(mockVersion));
        when(scenarioRepository.findById(7L)).thenReturn(Optional.of(scenario));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> runService.createRun(1L, 1, 7L)
        );

        assertEquals("Scenario 7 does not belong to version 1 of lift system 1", exception.getMessage());
        verify(runRepository, never()).save(any(SimulationRun.class));
    }

    @Test
    public void testCreateAndStartRun_ConfiguresRunStartsAndSubmitsExecution() throws Exception {
        when(liftSystemRepository.findById(1L)).thenReturn(Optional.of(mockLiftSystem));
        when(versionRepository.findByLiftSystemIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(mockVersion));
        java.util.concurrent.atomic.AtomicReference<SimulationRun> savedRunRef = new java.util.concurrent.atomic.AtomicReference<>();
        when(runRepository.save(any(SimulationRun.class))).thenAnswer(invocation -> {
            SimulationRun savedRun = invocation.getArgument(0);
            if (savedRun.getId() == null) {
                savedRun.setId(42L);
            }
            savedRunRef.set(savedRun);
            return savedRun;
        });
        when(runRepository.findByIdWithDetails(42L)).thenAnswer(invocation -> Optional.of(savedRunRef.get()));

        SimulationRun result = runService.createAndStartRun(1L, 1, null, 12345L);

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
        when(versionRepository.findByLiftSystemIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(mockVersion));
        java.util.concurrent.atomic.AtomicReference<SimulationRun> savedRunRef = new java.util.concurrent.atomic.AtomicReference<>();
        when(runRepository.save(any(SimulationRun.class))).thenAnswer(invocation -> {
            SimulationRun savedRun = invocation.getArgument(0);
            if (savedRun.getId() == null) {
                savedRun.setId(42L);
            }
            savedRunRef.set(savedRun);
            return savedRun;
        });
        when(runRepository.findByIdWithDetails(42L)).thenAnswer(invocation -> Optional.of(savedRunRef.get()));

        TransactionSynchronizationManager.initSynchronization();
        try {
            SimulationRun result = runService.createAndStartRun(1L, 1, null, 12345L);

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
        when(versionRepository.findByLiftSystemIdAndVersionNumber(1L, 1)).thenReturn(Optional.of(mockVersion));
        when(scenarioRepository.findById(5L)).thenReturn(Optional.of(mockScenario));
        when(objectMapper.readValue(
            "{\"durationTicks\": 5000, \"passengerFlows\": []}",
            com.liftsimulator.admin.dto.ScenarioDefinitionDTO.class
        )).thenReturn(new com.liftsimulator.admin.dto.ScenarioDefinitionDTO(5000, List.of(), null));
        java.util.concurrent.atomic.AtomicReference<SimulationRun> savedRunRef = new java.util.concurrent.atomic.AtomicReference<>();
        when(runRepository.save(any(SimulationRun.class))).thenAnswer(invocation -> {
            SimulationRun savedRun = invocation.getArgument(0);
            if (savedRun.getId() == null) {
                savedRun.setId(42L);
            }
            savedRunRef.set(savedRun);
            return savedRun;
        });
        when(runRepository.findByIdWithDetails(42L)).thenAnswer(invocation -> Optional.of(savedRunRef.get()));

        SimulationRun result = runService.createAndStartRun(1L, 1, 5L, 12345L);

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
        when(runRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(mockRun));

        SimulationRun result = runService.getRunById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(runRepository).findByIdWithDetails(1L);
    }

    @Test
    public void testGetRunById_NotFound() {
        when(runRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> runService.getRunById(999L)
        );

        assertEquals("Simulation run not found with id: 999", exception.getMessage());
        verify(runRepository).findByIdWithDetails(999L);
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
    public void testDeleteRun_Success() throws Exception {
        mockRun.setStatus(RunStatus.SUCCEEDED);
        when(runRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(mockRun));

        runService.deleteRun(1L);

        verify(runRepository).findByIdWithDetails(1L);
        verify(runRepository).deleteById(1L);
        verify(artefactService).deleteArtefacts(mockRun);
    }

    @Test
    public void testDeleteRun_DefersArtefactDeletionUntilAfterCommit() throws Exception {
        mockRun.setStatus(RunStatus.SUCCEEDED);
        when(runRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(mockRun));

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
        when(runRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> runService.deleteRun(999L)
        );

        assertEquals("Simulation run not found with id: 999", exception.getMessage());
        verify(runRepository).findByIdWithDetails(999L);
        verify(runRepository, never()).deleteById(any());
    }

    @Test
    public void testDeleteRun_RunningRunRejected() throws Exception {
        mockRun.setStatus(RunStatus.RUNNING);
        when(runRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(mockRun));

        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> runService.deleteRun(1L)
        );

        assertTrue(exception.getMessage().contains("RUNNING"));
        verify(artefactService, never()).deleteArtefacts(any());
        verify(runRepository, never()).deleteById(any());
    }

    @Test
    public void testDeleteRun_ArtefactDeletionFailureAfterCommitIsBestEffort() throws Exception {
        mockRun.setStatus(RunStatus.FAILED);
        mockRun.setArtefactBasePath(tempDir.resolve("run-1").toString());
        when(runRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(mockRun));
        doThrow(new java.io.IOException("disk error")).when(artefactService).deleteArtefacts(mockRun);

        TransactionSynchronizationManager.initSynchronization();
        try {
            runService.deleteRun(1L);
            TransactionSynchronization synchronization = TransactionSynchronizationManager.getSynchronizations().get(0);

            synchronization.afterCommit();

            verify(runRepository).deleteById(1L);
            verify(artefactService).deleteArtefacts(mockRun);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }


    @Test
    public void testDeleteRun_UncheckedArtefactDeletionFailureAfterCommitIsBestEffort() throws Exception {
        mockRun.setStatus(RunStatus.FAILED);
        mockRun.setArtefactBasePath(tempDir.resolve("run-1").toString());
        when(runRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(mockRun));
        doThrow(new ArtefactStateException("not a directory")).when(artefactService).deleteArtefacts(mockRun);

        TransactionSynchronizationManager.initSynchronization();
        try {
            runService.deleteRun(1L);
            TransactionSynchronization synchronization = TransactionSynchronizationManager.getSynchronizations().get(0);

            synchronization.afterCommit();

            verify(runRepository).deleteById(1L);
            verify(artefactService).deleteArtefacts(mockRun);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }


}
