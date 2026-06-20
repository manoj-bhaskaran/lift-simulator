package com.liftsimulator.admin.repository;

import com.liftsimulator.admin.entity.LiftSystem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for LiftSystem entities.
 */
@Repository
public interface LiftSystemRepository extends JpaRepository<LiftSystem, Long> {

    /**
     * Find a lift system by id while holding a database write lock for serialized version mutations.
     *
     * @param id the lift system id
     * @return an Optional containing the locked LiftSystem if found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM LiftSystem s WHERE s.id = :id")
    Optional<LiftSystem> findByIdForUpdate(@Param("id") Long id);

    /**
     * Find a lift system by its unique system key.
     *
     * @param systemKey the unique system key
     * @return an Optional containing the LiftSystem if found
     */
    Optional<LiftSystem> findBySystemKey(String systemKey);

    /**
     * Check if a lift system with the given system key exists.
     *
     * @param systemKey the unique system key
     * @return true if a system with this key exists
     */
    boolean existsBySystemKey(String systemKey);
}
