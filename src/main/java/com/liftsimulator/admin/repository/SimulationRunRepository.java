package com.liftsimulator.admin.repository;

import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.entity.SimulationRun.RunStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for SimulationRun entities.
 */
@Repository
public interface SimulationRunRepository extends JpaRepository<SimulationRun, Long> {

    /**
     * Find all runs with their relationships eagerly loaded.
     *
     * @return list of runs ordered by creation date descending
     */
    @Query("SELECT r FROM SimulationRun r "
            + "LEFT JOIN FETCH r.liftSystem "
            + "LEFT JOIN FETCH r.version "
            + "LEFT JOIN FETCH r.scenario "
            + "ORDER BY r.createdAt DESC")
    List<SimulationRun> findAllWithDetails();

    /**
     * Find all runs for a specific lift system with relationships eagerly loaded.
     *
     * @param liftSystemId the lift system id
     * @return list of runs ordered by creation date descending
     */
    @Query("SELECT r FROM SimulationRun r "
            + "LEFT JOIN FETCH r.liftSystem "
            + "LEFT JOIN FETCH r.version "
            + "LEFT JOIN FETCH r.scenario "
            + "WHERE r.liftSystem.id = :liftSystemId "
            + "ORDER BY r.createdAt DESC")
    List<SimulationRun> findByLiftSystemIdWithDetails(@Param("liftSystemId") Long liftSystemId);

    /**
     * Find all runs with a specific status with relationships eagerly loaded.
     *
     * @param status the run status
     * @return list of runs ordered by creation date descending
     */
    @Query("SELECT r FROM SimulationRun r "
            + "LEFT JOIN FETCH r.liftSystem "
            + "LEFT JOIN FETCH r.version "
            + "LEFT JOIN FETCH r.scenario "
            + "WHERE r.status = :status "
            + "ORDER BY r.createdAt DESC")
    List<SimulationRun> findByStatusWithDetails(@Param("status") RunStatus status);

    /**
     * Find all runs for a specific lift system with a specific status with relationships eagerly loaded.
     *
     * @param liftSystemId the lift system id
     * @param status the run status
     * @return list of runs ordered by creation date descending
     */
    @Query("SELECT r FROM SimulationRun r "
            + "LEFT JOIN FETCH r.liftSystem "
            + "LEFT JOIN FETCH r.version "
            + "LEFT JOIN FETCH r.scenario "
            + "WHERE r.liftSystem.id = :liftSystemId AND r.status = :status "
            + "ORDER BY r.createdAt DESC")
    List<SimulationRun> findByLiftSystemIdAndStatusWithDetails(
            @Param("liftSystemId") Long liftSystemId,
            @Param("status") RunStatus status);

    /**
     * Find all runs for a specific lift system.
     *
     * @param liftSystemId the lift system id
     * @return list of runs ordered by creation date descending
     */
    @Query("SELECT r FROM SimulationRun r WHERE r.liftSystem.id = :liftSystemId "
            + "ORDER BY r.createdAt DESC")
    List<SimulationRun> findByLiftSystemIdOrderByCreatedAtDesc(@Param("liftSystemId") Long liftSystemId);

    /**
     * Find all runs for a specific version.
     *
     * @param versionId the version id
     * @return list of runs ordered by creation date descending
     */
    @Query("SELECT r FROM SimulationRun r WHERE r.version.id = :versionId "
            + "ORDER BY r.createdAt DESC")
    List<SimulationRun> findByVersionIdOrderByCreatedAtDesc(@Param("versionId") Long versionId);

    /**
     * Find all runs with a specific status.
     *
     * @param status the run status
     * @return list of runs ordered by creation date descending
     */
    @Query("SELECT r FROM SimulationRun r WHERE r.status = :status "
            + "ORDER BY r.createdAt DESC")
    List<SimulationRun> findByStatusOrderByCreatedAtDesc(@Param("status") RunStatus status);

    /**
     * Find all runs for a lift system with a specific status.
     *
     * @param liftSystemId the lift system id
     * @param status the run status
     * @return list of runs ordered by creation date descending
     */
    @Query("SELECT r FROM SimulationRun r WHERE r.liftSystem.id = :liftSystemId "
            + "AND r.status = :status ORDER BY r.createdAt DESC")
    List<SimulationRun> findByLiftSystemIdAndStatus(
            @Param("liftSystemId") Long liftSystemId,
            @Param("status") RunStatus status);

    /**
     * Find all active (CREATED or RUNNING) runs for a lift system.
     *
     * @param liftSystemId the lift system id
     * @return list of active runs ordered by creation date descending
     */
    @Query("SELECT r FROM SimulationRun r WHERE r.liftSystem.id = :liftSystemId "
            + "AND r.status IN ('CREATED', 'RUNNING') ORDER BY r.createdAt DESC")
    List<SimulationRun> findActiveRunsByLiftSystemId(@Param("liftSystemId") Long liftSystemId);

    /**
     * Count runs for a specific lift system.
     *
     * @param liftSystemId the lift system id
     * @return total number of runs
     */
    long countByLiftSystemId(Long liftSystemId);

    /**
     * Count runs by status.
     *
     * @param status the run status
     * @return total number of runs with the given status
     */
    long countByStatus(RunStatus status);

    /**
     * Update the current tick for a simulation run.
     *
     * @param id the run id
     * @param currentTick the current tick
     * @return number of rows updated
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SimulationRun r SET r.currentTick = :currentTick WHERE r.id = :id")
    int updateCurrentTick(@Param("id") Long id, @Param("currentTick") Long currentTick);
}
