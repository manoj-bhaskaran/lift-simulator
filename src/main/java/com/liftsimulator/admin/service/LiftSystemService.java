package com.liftsimulator.admin.service;

import com.liftsimulator.admin.dto.CreateLiftSystemRequest;
import com.liftsimulator.admin.dto.LiftSystemResponse;
import com.liftsimulator.admin.dto.UpdateLiftSystemRequest;
import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.repository.LiftSystemRepository;
import com.liftsimulator.admin.repository.LiftSystemVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing lift systems.
 */
@Service
@Transactional(readOnly = true)
public class LiftSystemService {

    private final LiftSystemRepository liftSystemRepository;
    private final LiftSystemVersionRepository liftSystemVersionRepository;

    public LiftSystemService(
            LiftSystemRepository liftSystemRepository,
            LiftSystemVersionRepository liftSystemVersionRepository
    ) {
        this.liftSystemRepository = liftSystemRepository;
        this.liftSystemVersionRepository = liftSystemVersionRepository;
    }

    /**
     * Creates a new lift system.
     *
     * @param request the creation request
     * @return the created lift system
     * @throws IllegalArgumentException if system key already exists
     */
    @Transactional
    public LiftSystemResponse createLiftSystem(CreateLiftSystemRequest request) {
        if (liftSystemRepository.existsBySystemKey(request.systemKey())) {
            throw new IllegalArgumentException(
                "Lift system with key '" + request.systemKey() + "' already exists"
            );
        }

        LiftSystem liftSystem = new LiftSystem();
        liftSystem.setSystemKey(request.systemKey());
        liftSystem.setDisplayName(request.displayName());
        liftSystem.setDescription(request.description());

        LiftSystem savedSystem = liftSystemRepository.save(liftSystem);
        return LiftSystemResponse.fromEntity(savedSystem, 0);
    }

    /**
     * Retrieves all lift systems.
     *
     * @return list of all lift systems
     */
    public List<LiftSystemResponse> getAllLiftSystems() {
        List<LiftSystem> systems = liftSystemRepository.findAll();
        Map<Long, Long> versionCounts = loadVersionCounts();
        return systems.stream()
            .map(system -> LiftSystemResponse.fromEntity(
                system,
                versionCounts.getOrDefault(system.getId(), 0L)
            ))
            .toList();
    }

    /**
     * Retrieves a lift system by ID.
     *
     * @param id the system ID
     * @return the lift system
     * @throws ResourceNotFoundException if not found
     */
    public LiftSystemResponse getLiftSystemById(Long id) {
        LiftSystem liftSystem = liftSystemRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Lift system not found with id: " + id
            ));
        long versionCount = liftSystemVersionRepository.countByLiftSystemId(liftSystem.getId());
        return LiftSystemResponse.fromEntity(liftSystem, versionCount);
    }

    /**
     * Updates lift system metadata.
     *
     * @param id the system ID
     * @param request the update request
     * @return the updated lift system
     * @throws ResourceNotFoundException if not found
     */
    @Transactional
    public LiftSystemResponse updateLiftSystem(Long id, UpdateLiftSystemRequest request) {
        LiftSystem liftSystem = liftSystemRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Lift system not found with id: " + id
            ));

        liftSystem.setDisplayName(request.displayName());
        liftSystem.setDescription(request.description());

        LiftSystem updatedSystem = liftSystemRepository.save(liftSystem);
        long versionCount = liftSystemVersionRepository.countByLiftSystemId(updatedSystem.getId());
        return LiftSystemResponse.fromEntity(updatedSystem, versionCount);
    }

    /**
     * Deletes a lift system and all its versions.
     *
     * @param id the system ID
     * @throws ResourceNotFoundException if not found
     */
    @Transactional
    public void deleteLiftSystem(Long id) {
        if (!liftSystemRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                "Lift system not found with id: " + id
            );
        }
        liftSystemRepository.deleteById(id);
    }

    private Map<Long, Long> loadVersionCounts() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : liftSystemVersionRepository.countVersionsByLiftSystemId()) {
            Long systemId = ((Number) row[0]).longValue();
            Long count = ((Number) row[1]).longValue();
            counts.put(systemId, count);
        }
        return counts;
    }

}
