package com.liftsimulator.admin.repository;

import com.liftsimulator.admin.entity.Scenario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Scenario entities.
 */
@Repository
public interface ScenarioRepository extends JpaRepository<Scenario, Long> {

    /**
     * Find a scenario by id while holding a database write lock for serialized deletion.
     *
     * @param id the scenario id
     * @return an Optional containing the locked Scenario if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Scenario s WHERE s.id = :id")
    Optional<Scenario> findByIdForUpdate(@Param("id") Long id);

    /**
     * Counts scenarios associated with any version of the specified lift system.
     *
     * @param liftSystemId the lift system id
     * @return number of scenarios referencing the lift system's versions
     */
    @Query("SELECT COUNT(s) FROM Scenario s WHERE s.liftSystemVersion.liftSystem.id = :liftSystemId")
    long countByLiftSystemId(@Param("liftSystemId") Long liftSystemId);

    /**
     * Checks whether a scenario with the given name already exists for the lift system version.
     *
     * @param liftSystemVersionId the lift system version id
     * @param name the scenario name
     * @return {@code true} if a matching scenario exists
     */
    boolean existsByLiftSystemVersionIdAndName(Long liftSystemVersionId, String name);

    /**
     * Checks whether another scenario (excluding the given id) with the same name already exists
     * for the lift system version. Used to allow a scenario to keep its own name on update.
     *
     * @param liftSystemVersionId the lift system version id
     * @param name the scenario name
     * @param id the id of the scenario being updated, which is excluded from the check
     * @return {@code true} if a different scenario with the same name exists
     */
    boolean existsByLiftSystemVersionIdAndNameAndIdNot(Long liftSystemVersionId, String name, Long id);

    /**
     * Finds all scenarios with relationships eagerly loaded.
     *
     * <p>This method uses JOIN-FETCH to prevent N+1 queries when traversing
     * scenario.liftSystemVersion.liftSystem relationships.</p>
     *
     * @return list of all scenarios with loaded relationships
     */
    @Query("SELECT s FROM Scenario s "
            + "JOIN FETCH s.liftSystemVersion v "
            + "JOIN FETCH v.liftSystem")
    List<Scenario> findAllWithDetails();
}
