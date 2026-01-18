package com.liftsimulator.admin.repository;

import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.entity.LiftSystemVersion.VersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for LiftSystemVersion entities.
 */
@Repository
public interface LiftSystemVersionRepository extends JpaRepository<LiftSystemVersion, Long> {

    /**
     * Find all versions for a specific lift system.
     *
     * @param liftSystemId the lift system id
     * @return list of versions ordered by version number descending
     */
    @Query("SELECT v FROM LiftSystemVersion v WHERE v.liftSystem.id = :liftSystemId "
            + "ORDER BY v.versionNumber DESC")
    List<LiftSystemVersion> findByLiftSystemIdOrderByVersionNumberDesc(
            @Param("liftSystemId") Long liftSystemId);

    /**
     * Find a specific version by lift system id and version number.
     *
     * @param liftSystemId the lift system id
     * @param versionNumber the version number
     * @return an Optional containing the version if found
     */
    Optional<LiftSystemVersion> findByLiftSystemIdAndVersionNumber(
            Long liftSystemId, Integer versionNumber);

    /**
     * Find all published versions for a specific lift system.
     *
     * @param liftSystemId the lift system id
     * @return list of published versions
     */
    List<LiftSystemVersion> findByLiftSystemIdAndIsPublishedTrue(Long liftSystemId);

    /**
     * Find all versions with a specific status.
     *
     * @param status the version status
     * @return list of versions with the given status
     */
    List<LiftSystemVersion> findByStatus(VersionStatus status);

    /**
     * Get the latest version number for a lift system.
     *
     * @param liftSystemId the lift system id
     * @return the maximum version number, or null if no versions exist
     */
    @Query("SELECT MAX(v.versionNumber) FROM LiftSystemVersion v "
            + "WHERE v.liftSystem.id = :liftSystemId")
    Integer findMaxVersionNumberByLiftSystemId(@Param("liftSystemId") Long liftSystemId);

    /**
     * Count versions for a specific lift system.
     *
     * @param liftSystemId the lift system id
     * @return total number of versions
     */
    long countByLiftSystemId(Long liftSystemId);

    /**
     * Count versions grouped by lift system.
     *
     * @return list of system id and version count pairs
     */
    @Query("SELECT v.liftSystem.id, COUNT(v) FROM LiftSystemVersion v GROUP BY v.liftSystem.id")
    List<Object[]> countVersionsByLiftSystemId();
}
