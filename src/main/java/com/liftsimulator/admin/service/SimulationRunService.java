package com.liftsimulator.admin.service;

import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.entity.SimulationRun.RunStatus;
import com.liftsimulator.admin.entity.SimulationScenario;
import com.liftsimulator.admin.repository.LiftSystemRepository;
import com.liftsimulator.admin.repository.LiftSystemVersionRepository;
import com.liftsimulator.admin.repository.SimulationRunRepository;
import com.liftsimulator.admin.repository.SimulationScenarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing simulation runs.
 */
@Service
@Transactional(readOnly = true)
public class SimulationRunService {

    private final SimulationRunRepository runRepository;
    private final LiftSystemRepository liftSystemRepository;
    private final LiftSystemVersionRepository versionRepository;
    private final SimulationScenarioRepository scenarioRepository;

    public SimulationRunService(
            SimulationRunRepository runRepository,
            LiftSystemRepository liftSystemRepository,
            LiftSystemVersionRepository versionRepository,
            SimulationScenarioRepository scenarioRepository) {
        this.runRepository = runRepository;
        this.liftSystemRepository = liftSystemRepository;
        this.versionRepository = versionRepository;
        this.scenarioRepository = scenarioRepository;
    }

    /**
     * Create a new simulation run without a scenario (ad-hoc run).
     *
     * @param liftSystemId the lift system id
     * @param versionId the version id
     * @return the created run
     * @throws ResourceNotFoundException if the lift system or version is not found
     * @throws IllegalArgumentException if the version does not belong to the lift system
     */
    @Transactional
    public SimulationRun createRun(Long liftSystemId, Long versionId) {
        LiftSystem liftSystem = liftSystemRepository.findById(liftSystemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lift system not found with id: " + liftSystemId));

        LiftSystemVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lift system version not found with id: " + versionId));

        if (!version.getLiftSystem().getId().equals(liftSystemId)) {
            throw new IllegalArgumentException(
                    "Version " + versionId + " does not belong to lift system " + liftSystemId);
        }

        SimulationRun run = new SimulationRun(liftSystem, version);
        return runRepository.save(run);
    }

    /**
     * Create a new simulation run with a scenario.
     *
     * @param liftSystemId the lift system id
     * @param versionId the version id
     * @param scenarioId the scenario id
     * @return the created run
     * @throws ResourceNotFoundException if any of the entities is not found
     * @throws IllegalArgumentException if the version does not belong to the lift system
     */
    @Transactional
    public SimulationRun createRunWithScenario(Long liftSystemId, Long versionId, Long scenarioId) {
        LiftSystem liftSystem = liftSystemRepository.findById(liftSystemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lift system not found with id: " + liftSystemId));

        LiftSystemVersion version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lift system version not found with id: " + versionId));

        if (!version.getLiftSystem().getId().equals(liftSystemId)) {
            throw new IllegalArgumentException(
                    "Version " + versionId + " does not belong to lift system " + liftSystemId);
        }

        SimulationScenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Simulation scenario not found with id: " + scenarioId));

        SimulationRun run = new SimulationRun(liftSystem, version, scenario);
        return runRepository.save(run);
    }

    /**
     * Get all simulation runs.
     *
     * @return list of all runs
     */
    public List<SimulationRun> getAllRuns() {
        return runRepository.findAll();
    }

    /**
     * Get a run by its ID.
     *
     * @param id the run id
     * @return the run
     * @throws ResourceNotFoundException if the run is not found
     */
    public SimulationRun getRunById(Long id) {
        return runRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Simulation run not found with id: " + id));
    }

    /**
     * Get all runs for a specific lift system.
     *
     * @param liftSystemId the lift system id
     * @return list of runs ordered by creation date descending
     */
    public List<SimulationRun> getRunsByLiftSystem(Long liftSystemId) {
        return runRepository.findByLiftSystemIdOrderByCreatedAtDesc(liftSystemId);
    }

    /**
     * Get all runs with a specific status.
     *
     * @param status the run status
     * @return list of runs ordered by creation date descending
     */
    public List<SimulationRun> getRunsByStatus(RunStatus status) {
        return runRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    /**
     * Get all active (CREATED or RUNNING) runs for a lift system.
     *
     * @param liftSystemId the lift system id
     * @return list of active runs ordered by creation date descending
     */
    public List<SimulationRun> getActiveRunsByLiftSystem(Long liftSystemId) {
        return runRepository.findActiveRunsByLiftSystemId(liftSystemId);
    }

    /**
     * Start a simulation run.
     * Transitions from CREATED to RUNNING.
     *
     * @param id the run id
     * @return the updated run
     * @throws ResourceNotFoundException if the run is not found
     * @throws IllegalStateException if the run is not in CREATED state
     */
    @Transactional
    public SimulationRun startRun(Long id) {
        SimulationRun run = getRunById(id);
        run.start();
        return runRepository.save(run);
    }

    /**
     * Mark a simulation run as succeeded.
     * Transitions from RUNNING to SUCCEEDED.
     *
     * @param id the run id
     * @return the updated run
     * @throws ResourceNotFoundException if the run is not found
     * @throws IllegalStateException if the run is not in RUNNING state
     */
    @Transactional
    public SimulationRun succeedRun(Long id) {
        SimulationRun run = getRunById(id);
        run.succeed();
        return runRepository.save(run);
    }

    /**
     * Mark a simulation run as failed.
     * Transitions from RUNNING to FAILED.
     *
     * @param id the run id
     * @param errorMessage the error message describing the failure
     * @return the updated run
     * @throws ResourceNotFoundException if the run is not found
     * @throws IllegalStateException if the run is not in RUNNING state
     */
    @Transactional
    public SimulationRun failRun(Long id, String errorMessage) {
        SimulationRun run = getRunById(id);
        run.fail(errorMessage);
        return runRepository.save(run);
    }

    /**
     * Cancel a simulation run.
     * Can transition from CREATED or RUNNING to CANCELLED.
     *
     * @param id the run id
     * @return the updated run
     * @throws ResourceNotFoundException if the run is not found
     * @throws IllegalStateException if the run is not in CREATED or RUNNING state
     */
    @Transactional
    public SimulationRun cancelRun(Long id) {
        SimulationRun run = getRunById(id);
        run.cancel();
        return runRepository.save(run);
    }

    /**
     * Update the progress of a running simulation.
     *
     * @param id the run id
     * @param currentTick the current tick number
     * @return the updated run
     * @throws ResourceNotFoundException if the run is not found
     */
    @Transactional
    public SimulationRun updateProgress(Long id, Long currentTick) {
        SimulationRun run = getRunById(id);
        run.updateProgress(currentTick);
        return runRepository.save(run);
    }

    /**
     * Set configuration for a run before starting.
     *
     * @param id the run id
     * @param totalTicks total number of ticks for the simulation
     * @param seed random seed for reproducibility
     * @param artefactBasePath base path for output artefacts
     * @return the updated run
     * @throws ResourceNotFoundException if the run is not found
     */
    @Transactional
    public SimulationRun configureRun(Long id, Long totalTicks, Long seed, String artefactBasePath) {
        SimulationRun run = getRunById(id);
        run.setTotalTicks(totalTicks);
        run.setSeed(seed);
        run.setArtefactBasePath(artefactBasePath);
        return runRepository.save(run);
    }

    /**
     * Delete a simulation run.
     *
     * @param id the run id
     * @throws ResourceNotFoundException if the run is not found
     */
    @Transactional
    public void deleteRun(Long id) {
        if (!runRepository.existsById(id)) {
            throw new ResourceNotFoundException("Simulation run not found with id: " + id);
        }
        runRepository.deleteById(id);
    }
}
