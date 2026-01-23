package com.liftsimulator.admin.repository;

import com.liftsimulator.admin.entity.Scenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Scenario entities.
 */
@Repository
public interface ScenarioRepository extends JpaRepository<Scenario, Long> {
}
