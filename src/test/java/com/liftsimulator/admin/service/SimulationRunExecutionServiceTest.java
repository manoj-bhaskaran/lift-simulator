package com.liftsimulator.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.repository.SimulationRunRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SimulationRunExecutionService.
 */
@ExtendWith(MockitoExtension.class)
public class SimulationRunExecutionServiceTest {

    @Mock
    private SimulationRunRepository runRepository;

    @Mock
    private ConfigValidationService configValidationService;

    @Mock
    private ScenarioValidationService scenarioValidationService;

    private SimulationRunExecutionService executionService;

    @BeforeEach
    public void setUp() {
        executionService = new SimulationRunExecutionService(
                runRepository,
                configValidationService,
                scenarioValidationService,
                new ObjectMapper()
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
}
