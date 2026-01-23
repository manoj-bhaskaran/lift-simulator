package com.liftsimulator.admin.service;

import com.liftsimulator.admin.entity.SimulationScenario;
import com.liftsimulator.admin.repository.SimulationScenarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SimulationScenarioService.
 */
@ExtendWith(MockitoExtension.class)
public class SimulationScenarioServiceTest {

    @Mock
    private SimulationScenarioRepository scenarioRepository;

    @InjectMocks
    private SimulationScenarioService scenarioService;

    private SimulationScenario mockScenario;

    @BeforeEach
    public void setUp() {
        mockScenario = new SimulationScenario();
        mockScenario.setId(1L);
        mockScenario.setName("Test Scenario");
        mockScenario.setScenarioJson("{\"passengers\": 100}");
        mockScenario.setCreatedAt(OffsetDateTime.now());
        mockScenario.setUpdatedAt(OffsetDateTime.now());
    }

    @Test
    public void testCreateScenario_Success() {
        when(scenarioRepository.save(any(SimulationScenario.class))).thenReturn(mockScenario);

        SimulationScenario result = scenarioService.createScenario("Test Scenario", "{\"passengers\": 100}");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Scenario", result.getName());
        verify(scenarioRepository).save(any(SimulationScenario.class));
    }

    @Test
    public void testGetAllScenarios() {
        SimulationScenario scenario1 = new SimulationScenario("Scenario 1", "{}");
        SimulationScenario scenario2 = new SimulationScenario("Scenario 2", "{}");
        List<SimulationScenario> scenarios = Arrays.asList(scenario1, scenario2);

        when(scenarioRepository.findAll()).thenReturn(scenarios);

        List<SimulationScenario> result = scenarioService.getAllScenarios();

        assertEquals(2, result.size());
        verify(scenarioRepository).findAll();
    }

    @Test
    public void testGetScenarioById_Success() {
        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(mockScenario));

        SimulationScenario result = scenarioService.getScenarioById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(scenarioRepository).findById(1L);
    }

    @Test
    public void testGetScenarioById_NotFound() {
        when(scenarioRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> scenarioService.getScenarioById(999L)
        );

        assertEquals("Simulation scenario not found with id: 999", exception.getMessage());
        verify(scenarioRepository).findById(999L);
    }

    @Test
    public void testGetScenarioByName_Success() {
        when(scenarioRepository.findByName("Test Scenario")).thenReturn(Optional.of(mockScenario));

        SimulationScenario result = scenarioService.getScenarioByName("Test Scenario");

        assertNotNull(result);
        assertEquals("Test Scenario", result.getName());
        verify(scenarioRepository).findByName("Test Scenario");
    }

    @Test
    public void testGetScenarioByName_NotFound() {
        when(scenarioRepository.findByName("Unknown")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> scenarioService.getScenarioByName("Unknown")
        );

        assertEquals("Simulation scenario not found with name: Unknown", exception.getMessage());
        verify(scenarioRepository).findByName("Unknown");
    }

    @Test
    public void testUpdateScenario_Success() {
        when(scenarioRepository.findById(1L)).thenReturn(Optional.of(mockScenario));
        when(scenarioRepository.save(any(SimulationScenario.class))).thenReturn(mockScenario);

        SimulationScenario result = scenarioService.updateScenario(
                1L, "Updated Scenario", "{\"passengers\": 200}");

        assertNotNull(result);
        verify(scenarioRepository).findById(1L);
        verify(scenarioRepository).save(any(SimulationScenario.class));
    }

    @Test
    public void testUpdateScenario_NotFound() {
        when(scenarioRepository.findById(999L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> scenarioService.updateScenario(999L, "Updated", "{}")
        );

        assertEquals("Simulation scenario not found with id: 999", exception.getMessage());
        verify(scenarioRepository).findById(999L);
    }

    @Test
    public void testDeleteScenario_Success() {
        when(scenarioRepository.existsById(1L)).thenReturn(true);

        scenarioService.deleteScenario(1L);

        verify(scenarioRepository).existsById(1L);
        verify(scenarioRepository).deleteById(1L);
    }

    @Test
    public void testDeleteScenario_NotFound() {
        when(scenarioRepository.existsById(999L)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> scenarioService.deleteScenario(999L)
        );

        assertEquals("Simulation scenario not found with id: 999", exception.getMessage());
        verify(scenarioRepository).existsById(999L);
    }
}
