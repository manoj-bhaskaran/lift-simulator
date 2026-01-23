package com.liftsimulator.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.entity.SimulationRun.RunStatus;
import com.liftsimulator.admin.entity.SimulationScenario;
import com.liftsimulator.admin.repository.LiftSystemRepository;
import com.liftsimulator.admin.repository.LiftSystemVersionRepository;
import com.liftsimulator.admin.repository.SimulationRunRepository;
import com.liftsimulator.admin.repository.SimulationScenarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
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
    private SimulationScenarioRepository scenarioRepository;

    @Mock
    private BatchInputGenerator batchInputGenerator;

    private SimulationRunService runService;
    private ObjectMapper objectMapper;

    private LiftSystem mockLiftSystem;
    private LiftSystemVersion mockVersion;
    private SimulationScenario mockScenario;
    private SimulationRun mockRun;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        runService = new SimulationRunService(
                runRepository,
                liftSystemRepository,
                versionRepository,
                scenarioRepository,
                batchInputGenerator,
                objectMapper
        );

        mockLiftSystem = new LiftSystem();
        mockLiftSystem.setId(1L);
        mockLiftSystem.setSystemKey("test-system");

        mockVersion = new LiftSystemVersion();
        mockVersion.setId(1L);
        mockVersion.setVersionNumber(1);
        mockVersion.setLiftSystem(mockLiftSystem);
        mockVersion.setConfig("{\"minFloor\":0,\"maxFloor\":10}");

        mockScenario = new SimulationScenario();
        mockScenario.setId(1L);
        mockScenario.setName("Test Scenario");
        mockScenario.setScenarioJson("{\"durationTicks\":100,\"passengerFlows\":[]}");

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

        SimulationRun result = runService.createRun(1L, 1L);

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
            () -> runService.createRun(999L, 1L)
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
            () -> runService.createRun(1L, 999L)
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
            () -> runService.createRun(1L, 5L)
        );

        assertEquals("Version 5 does not belong to lift system 1", exception.getMessage());
        verify(liftSystemRepository).findById(1L);
        verify(versionRepository).findById(5L);
    }

    @Test
    public void testCreateRunWithScenario_Success() {
        when(liftSystemRepository.findById(1L)).thenReturn(Optional.of(mockLiftSystem));
        when(versionRepository.findById(1L)).thenReturn(Optional.of(mockVersion));
        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(mockScenario));
        when(runRepository.save(any(SimulationRun.class))).thenReturn(mockRun);

        SimulationRun result = runService.createRunWithScenario(1L, 1L, 1L);

        assertNotNull(result);
        verify(liftSystemRepository).findById(1L);
        verify(versionRepository).findById(1L);
        verify(scenarioRepository).findById(1L);
        verify(runRepository).save(any(SimulationRun.class));
    }

    @Test
    public void testCreateRunWithScenario_ScenarioNotFound() {
        when(liftSystemRepository.findById(1L)).thenReturn(Optional.of(mockLiftSystem));
        when(versionRepository.findById(1L)).thenReturn(Optional.of(mockVersion));
        when(scenarioRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> runService.createRunWithScenario(1L, 1L, 999L)
        );

        assertEquals("Simulation scenario not found with id: 999", exception.getMessage());
        verify(scenarioRepository).findById(999L);
    }

    @Test
    public void testCreateRunWithScenario_VersionBelongsToDifferentSystem() {
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
            () -> runService.createRunWithScenario(1L, 5L, 1L)
        );

        assertEquals("Version 5 does not belong to lift system 1", exception.getMessage());
        verify(liftSystemRepository).findById(1L);
        verify(versionRepository).findById(5L);
    }

    @Test
    public void testGetAllRuns() {
        SimulationRun run1 = new SimulationRun();
        SimulationRun run2 = new SimulationRun();
        List<SimulationRun> runs = Arrays.asList(run1, run2);

        when(runRepository.findAll()).thenReturn(runs);

        List<SimulationRun> result = runService.getAllRuns();

        assertEquals(2, result.size());
        verify(runRepository).findAll();
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
        when(runRepository.findById(1L)).thenReturn(Optional.of(mockRun));
        when(runRepository.save(any(SimulationRun.class))).thenReturn(mockRun);

        SimulationRun result = runService.updateProgress(1L, 500L);

        assertNotNull(result);
        verify(runRepository).findById(1L);
        verify(runRepository).save(any(SimulationRun.class));
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
    public void testDeleteRun_Success() {
        when(runRepository.existsById(1L)).thenReturn(true);

        runService.deleteRun(1L);

        verify(runRepository).existsById(1L);
        verify(runRepository).deleteById(1L);
    }

    @Test
    public void testDeleteRun_NotFound() {
        when(runRepository.existsById(999L)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> runService.deleteRun(999L)
        );

        assertEquals("Simulation run not found with id: 999", exception.getMessage());
        verify(runRepository).existsById(999L);
    }

    @Test
    public void testGenerateBatchInputFile_Success() throws IOException {
        String artefactPath = tempDir.toString();
        mockRun.setScenario(mockScenario);
        mockRun.setArtefactBasePath(artefactPath);

        when(runRepository.findById(1L)).thenReturn(Optional.of(mockRun));

        Path result = runService.generateBatchInputFile(1L);

        assertNotNull(result);
        assertEquals(Path.of(artefactPath, "input.scenario"), result);
        verify(runRepository).findById(1L);
        verify(batchInputGenerator).generateBatchInputFile(
                any(String.class),
                any(),
                any(String.class),
                any(Path.class)
        );
    }

    @Test
    public void testGenerateBatchInputFile_NoScenario() {
        mockRun.setScenario(null);
        mockRun.setArtefactBasePath(tempDir.toString());

        when(runRepository.findById(1L)).thenReturn(Optional.of(mockRun));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> runService.generateBatchInputFile(1L)
        );

        assertTrue(exception.getMessage().contains("does not have a scenario"));
        verify(runRepository).findById(1L);
    }

    @Test
    public void testGenerateBatchInputFile_NoArtefactBasePath() {
        mockRun.setScenario(mockScenario);
        mockRun.setArtefactBasePath(null);

        when(runRepository.findById(1L)).thenReturn(Optional.of(mockRun));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> runService.generateBatchInputFile(1L)
        );

        assertTrue(exception.getMessage().contains("does not have an artefact base path"));
        verify(runRepository).findById(1L);
    }

    @Test
    public void testGenerateBatchInputFile_BlankArtefactBasePath() {
        mockRun.setScenario(mockScenario);
        mockRun.setArtefactBasePath("   ");

        when(runRepository.findById(1L)).thenReturn(Optional.of(mockRun));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> runService.generateBatchInputFile(1L)
        );

        assertTrue(exception.getMessage().contains("does not have an artefact base path"));
        verify(runRepository).findById(1L);
    }

    @Test
    public void testGenerateBatchInputFile_RunNotFound() {
        when(runRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> runService.generateBatchInputFile(999L)
        );

        assertEquals("Simulation run not found with id: 999", exception.getMessage());
        verify(runRepository).findById(999L);
    }
}
