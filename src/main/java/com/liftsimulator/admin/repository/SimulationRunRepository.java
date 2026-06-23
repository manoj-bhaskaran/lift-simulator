package com.liftsimulator.admin.repository;

import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.entity.SimulationRun.RunStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for SimulationRun entities.
 */
@Repository
public interface SimulationRunRepository extends JpaRepository<SimulationRun, Long> {

    /**
     * Find a run by id with its relationships eagerly loaded.
     *
     * @param id the simulation run id
     * @return the run with lift system, version, and scenario loaded if found
     */
    @Query("SELECT r FROM SimulationRun r "
            + "LEFT JOIN FETCH r.liftSystem "
            + "LEFT JOIN FETCH r.version "
            + "LEFT JOIN FETCH r.scenario "
            + "WHERE r.id = :id")
    Optional<SimulationRun> findByIdWithDetails(@Param("id") Long id);

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
     * Find all runs with their relationships eagerly loaded, with pagination support.
     *
     * @param pageable pagination and sorting parameters
     * @return page of runs
     */
    @Query(value = "SELECT r FROM SimulationRun r "
            + "LEFT JOIN FETCH r.liftSystem "
            + "LEFT JOIN FETCH r.version "
            + "LEFT JOIN FETCH r.scenario",
           countQuery = "SELECT count(r) FROM SimulationRun r")
    Page<SimulationRun> findAllWithDetails(Pageable pageable);

    /**
     * Find all runs for a specific lift system with relationships eagerly loaded, with pagination.
     *
     * @param liftSystemId the lift system id
     * @param pageable pagination and sorting parameters
     * @return page of runs
     */
    @Query(value = "SELECT r FROM SimulationRun r "
            + "LEFT JOIN FETCH r.liftSystem "
            + "LEFT JOIN FETCH r.version "
            + "LEFT JOIN FETCH r.scenario "
            + "WHERE r.liftSystem.id = :liftSystemId",
           countQuery = "SELECT count(r) FROM SimulationRun r WHERE r.liftSystem.id = :liftSystemId")
    Page<SimulationRun> findByLiftSystemIdWithDetails(@Param("liftSystemId") Long liftSystemId, Pageable pageable);

    /**
     * Find all runs with a specific status with relationships eagerly loaded, with pagination.
     *
     * @param status the run status
     * @param pageable pagination and sorting parameters
     * @return page of runs
     */
    @Query(value = "SELECT r FROM SimulationRun r "
            + "LEFT JOIN FETCH r.liftSystem "
            + "LEFT JOIN FETCH r.version "
            + "LEFT JOIN FETCH r.scenario "
            + "WHERE r.status = :status",
           countQuery = "SELECT count(r) FROM SimulationRun r WHERE r.status = :status")
    Page<SimulationRun> findByStatusWithDetails(@Param("status") RunStatus status, Pageable pageable);

    /**
     * Find all runs for a specific lift system with a specific status, with pagination.
     *
     * @param liftSystemId the lift system id
     * @param status the run status
     * @param pageable pagination and sorting parameters
     * @return page of runs
     */
    @Query(value = "SELECT r FROM SimulationRun r "
            + "LEFT JOIN FETCH r.liftSystem "
            + "LEFT JOIN FETCH r.version "
            + "LEFT JOIN FETCH r.scenario "
            + "WHERE r.liftSystem.id = :liftSystemId AND r.status = :status",
           countQuery = "SELECT count(r) FROM SimulationRun r "
            + "WHERE r.liftSystem.id = :liftSystemId AND r.status = :status")
    Page<SimulationRun> findByLiftSystemIdAndStatusWithDetails(
            @Param("liftSystemId") Long liftSystemId,
            @Param("status") RunStatus status,
            Pageable pageable);

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
     * Count active (CREATED or RUNNING) runs for a lift system.
     *
     * @param liftSystemId the lift system id
     * @return number of active runs
     */
    @Query("SELECT count(r) FROM SimulationRun r WHERE r.liftSystem.id = :liftSystemId "
            + "AND r.status IN ('CREATED', 'RUNNING')")
    long countActiveRunsByLiftSystemId(@Param("liftSystemId") Long liftSystemId);

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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SimulationRun r SET r.currentTick = :currentTick WHERE r.id = :id")
    int updateCurrentTick(@Param("id") Long id, @Param("currentTick") Long currentTick);

    /**
     * Mark a currently running simulation run as failed without loading the entity.
     * This best-effort update is used by asynchronous failure handling to avoid
     * leaving runs orphaned in RUNNING if an entity state-machine transition races
     * with another lifecycle update.
     *
     * @param id the run id
     * @param errorMessage the failure message
     * @param endedAt completion timestamp
     * @return number of rows updated
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SimulationRun r SET r.status = 'FAILED', r.errorMessage = :errorMessage, "
            + "r.endedAt = :endedAt WHERE r.id = :id AND r.status = 'RUNNING'")
    int markRunningRunFailed(
            @Param("id") Long id,
            @Param("errorMessage") String errorMessage,
            @Param("endedAt") OffsetDateTime endedAt);

    /**
     * Mark all orphaned RUNNING runs as FAILED during application startup recovery.
     *
     * @param errorMessage the recovery failure message
     * @param endedAt recovery timestamp
     * @return number of rows updated
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SimulationRun r SET r.status = 'FAILED', r.errorMessage = :errorMessage, "
            + "r.endedAt = :endedAt WHERE r.status = 'RUNNING'")
    int failOrphanedRunningRuns(
            @Param("errorMessage") String errorMessage,
            @Param("endedAt") OffsetDateTime endedAt);

    /**
     * Mark all orphaned CREATED runs as CANCELLED during application startup recovery.
     *
     * @param endedAt recovery timestamp
     * @return number of rows updated
     */
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SimulationRun r SET r.status = 'CANCELLED', r.endedAt = :endedAt WHERE r.status = 'CREATED'")
    int cancelOrphanedCreatedRuns(@Param("endedAt") OffsetDateTime endedAt);
}
