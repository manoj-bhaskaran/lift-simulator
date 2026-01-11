package com.liftsimulator.admin.repository;

import com.liftsimulator.admin.entity.LiftSystem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for LiftSystem entities.
 */
@Repository
public interface LiftSystemRepository extends JpaRepository<LiftSystem, Long> {

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
