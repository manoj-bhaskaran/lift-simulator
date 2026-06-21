package com.liftsimulator.admin.service;

import com.liftsimulator.admin.dto.CreateLiftSystemRequest;
import com.liftsimulator.admin.dto.LiftSystemResponse;
import com.liftsimulator.admin.dto.UpdateLiftSystemRequest;
import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.repository.LiftSystemRepository;
import com.liftsimulator.admin.repository.LiftSystemVersionRepository;
import com.liftsimulator.admin.repository.ScenarioRepository;
import com.liftsimulator.admin.repository.SimulationRunRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
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
    private final ScenarioRepository scenarioRepository;
    private final SimulationRunRepository simulationRunRepository;
    private final ArtefactService artefactService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring-managed services are injected and treated as shared dependencies."
    )
    public LiftSystemService(
            LiftSystemRepository liftSystemRepository,
            LiftSystemVersionRepository liftSystemVersionRepository,
            ScenarioRepository scenarioRepository,
            SimulationRunRepository simulationRunRepository,
            ArtefactService artefactService
    ) {
        this.liftSystemRepository = liftSystemRepository;
        this.liftSystemVersionRepository = liftSystemVersionRepository;
        this.scenarioRepository = scenarioRepository;
        this.simulationRunRepository = simulationRunRepository;
        this.artefactService = artefactService;
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
     * <p>Deletion is blocked while any active (CREATED or RUNNING) simulation run
     * exists for the system, so that the database cascade never removes a run row
     * out from under an executing simulation thread. When deletion is allowed, the
     * on-disk artefacts of the (terminal) runs that cascade away are removed after
     * the transaction commits to avoid leaking artefact directories.</p>
     *
     * <p>The lift system row is write-locked up front so that the active-run check
     * and the delete are serialized against concurrent run creation: a run insert
     * takes a {@code FOR KEY SHARE} lock on the parent row, which conflicts with
     * this {@code FOR UPDATE} lock. This closes the window where a run could be
     * created and started after the count returns zero but before the delete
     * commits, leaving the cascade to remove a freshly active run.</p>
     *
     * @param id the system ID
     * @throws ResourceNotFoundException if not found
     * @throws IllegalStateException if scenarios or active runs exist
     */
    @Transactional
    public void deleteLiftSystem(Long id) {
        liftSystemRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Lift system not found with id: " + id
            ));
        long scenarioCount = scenarioRepository.countByLiftSystemId(id);
        if (scenarioCount > 0) {
            throw new IllegalStateException(
                "Cannot delete lift system because " + scenarioCount
                    + " scenario(s) exist for its versions. "
                    + "Delete the scenarios (or versions) first."
            );
        }
        long activeRunCount = simulationRunRepository.countActiveRunsByLiftSystemId(id);
        if (activeRunCount > 0) {
            throw new IllegalStateException(
                "Cannot delete lift system because " + activeRunCount
                    + " active simulation run(s) (CREATED/RUNNING) exist. "
                    + "Wait for them to complete or cancel them first."
            );
        }
        List<SimulationRun> runs = simulationRunRepository.findByLiftSystemIdOrderByCreatedAtDesc(id);
        liftSystemRepository.deleteById(id);
        deleteArtefactsAfterCommit(runs);
    }

    /**
     * Removes the on-disk artefacts of cascade-deleted runs once the surrounding
     * transaction commits, mirroring {@code SimulationRunService} so that a rollback
     * never deletes files belonging to a surviving system.
     *
     * @param runs the runs whose artefacts should be removed
     */
    private void deleteArtefactsAfterCommit(List<SimulationRun> runs) {
        if (runs.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteArtefacts(runs);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteArtefacts(runs);
            }
        });
    }

    private void deleteArtefacts(List<SimulationRun> runs) {
        for (SimulationRun run : runs) {
            try {
                artefactService.deleteArtefacts(run);
            } catch (IOException e) {
                throw new ArtefactDeletionException(
                    "Failed to delete artefacts for simulation run " + run.getId()
                        + ": " + e.getMessage(),
                    e
                );
            }
        }
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
