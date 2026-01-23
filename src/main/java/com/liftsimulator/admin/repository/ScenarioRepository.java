package com.liftsimulator.admin.repository;

import com.liftsimulator.admin.entity.Scenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for Scenario entities.
 */
@Repository
public interface ScenarioRepository extends JpaRepository<Scenario, Long> {

    /**
     * Find all scenarios ordered by creation date descending.
     *
     * @return list of scenarios sorted by creation date
     */
    List<Scenario> findAllByOrderByCreatedAtDesc();

    /**
     * Find scenarios by name containing the given string (case-insensitive).
     *
     * @param name the name substring to search for
     * @return list of matching scenarios
     */
    List<Scenario> findByNameContainingIgnoreCase(String name);
}
