package com.liftsimulator.admin.repository;

import com.liftsimulator.admin.entity.ScenarioEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for ScenarioEvent entities.
 */
@Repository
public interface ScenarioEventRepository extends JpaRepository<ScenarioEvent, Long> {

    /**
     * Find all events for a specific scenario, ordered by tick and event order.
     *
     * @param scenarioId the scenario ID
     * @return list of events for the scenario
     */
    List<ScenarioEvent> findByScenarioIdOrderByTickAscEventOrderAsc(Long scenarioId);

    /**
     * Delete all events for a specific scenario.
     *
     * @param scenarioId the scenario ID
     */
    void deleteByScenarioId(Long scenarioId);
}
