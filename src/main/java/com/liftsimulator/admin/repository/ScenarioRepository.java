package com.liftsimulator.admin.repository;

import com.liftsimulator.admin.entity.Scenario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Scenario entities.
 */
@Repository
public interface ScenarioRepository extends JpaRepository<Scenario, Long> {

    /**
     * Counts scenarios associated with any version of the specified lift system.
     *
     * @param liftSystemId the lift system id
     * @return number of scenarios referencing the lift system's versions
     */
    @Query("SELECT COUNT(s) FROM Scenario s WHERE s.liftSystemVersion.liftSystem.id = :liftSystemId")
    long countByLiftSystemId(@Param("liftSystemId") Long liftSystemId);
}
