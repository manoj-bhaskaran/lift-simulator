package com.liftsimulator.admin.service;

import com.liftsimulator.admin.entity.SimulationScenario;
import com.liftsimulator.admin.repository.SimulationScenarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing simulation scenarios.
 */
@Service
@Transactional(readOnly = true)
public class SimulationScenarioService {

    private final SimulationScenarioRepository scenarioRepository;

    public SimulationScenarioService(SimulationScenarioRepository scenarioRepository) {
        this.scenarioRepository = scenarioRepository;
    }

    /**
     * Create a new simulation scenario.
     *
     * @param name the scenario name
     * @param scenarioJson the scenario JSON configuration
     * @return the created scenario
     */
    @Transactional
    public SimulationScenario createScenario(String name, String scenarioJson) {
        SimulationScenario scenario = new SimulationScenario(name, scenarioJson);
        return scenarioRepository.save(scenario);
    }

    /**
     * Get all simulation scenarios.
     *
     * @return list of all scenarios
     */
    public List<SimulationScenario> getAllScenarios() {
        return scenarioRepository.findAll();
    }

    /**
     * Get a scenario by its ID.
     *
     * @param id the scenario id
     * @return the scenario
     * @throws ResourceNotFoundException if the scenario is not found
     */
    public SimulationScenario getScenarioById(Long id) {
        return scenarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Simulation scenario not found with id: " + id));
    }

    /**
     * Get a scenario by its name.
     *
     * @param name the scenario name
     * @return the scenario
     * @throws ResourceNotFoundException if the scenario is not found
     */
    public SimulationScenario getScenarioByName(String name) {
        return scenarioRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Simulation scenario not found with name: " + name));
    }

    /**
     * Update a scenario's configuration.
     *
     * @param id the scenario id
     * @param name the new name
     * @param scenarioJson the new scenario JSON
     * @return the updated scenario
     * @throws ResourceNotFoundException if the scenario is not found
     */
    @Transactional
    public SimulationScenario updateScenario(Long id, String name, String scenarioJson) {
        SimulationScenario scenario = getScenarioById(id);
        scenario.setName(name);
        scenario.setScenarioJson(scenarioJson);
        return scenarioRepository.save(scenario);
    }

    /**
     * Delete a scenario.
     *
     * @param id the scenario id
     * @throws ResourceNotFoundException if the scenario is not found
     */
    @Transactional
    public void deleteScenario(Long id) {
        if (!scenarioRepository.existsById(id)) {
            throw new ResourceNotFoundException("Simulation scenario not found with id: " + id);
        }
        scenarioRepository.deleteById(id);
    }
}
