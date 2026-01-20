package com.liftsimulator.admin.repository;

import com.liftsimulator.admin.entity.SimulationScenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for SimulationScenario entities.
 */
@Repository
public interface SimulationScenarioRepository extends JpaRepository<SimulationScenario, Long> {

    /**
     * Find a simulation scenario by its name.
     *
     * @param name the scenario name
     * @return an Optional containing the SimulationScenario if found
     */
    Optional<SimulationScenario> findByName(String name);

    /**
     * Find all scenarios whose name contains the given string (case-insensitive).
     *
     * @param name the search string
     * @return list of matching scenarios
     */
    List<SimulationScenario> findByNameContainingIgnoreCase(String name);

    /**
     * Check if a scenario with the given name exists.
     *
     * @param name the scenario name
     * @return true if a scenario with this name exists
     */
    boolean existsByName(String name);
}
