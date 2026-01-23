package com.liftsimulator.admin.dto;

import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.entity.SimulationRun.RunStatus;

import java.time.OffsetDateTime;

/**
 * Response DTO for simulation run details.
 */
public record SimulationRunResponse(
    Long id,
    Long liftSystemId,
    Long versionId,
    Long scenarioId,
    RunStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime startedAt,
    OffsetDateTime endedAt,
    Long totalTicks,
    Long currentTick,
    Long seed,
    String errorMessage,
    String artefactBasePath
) {
    /**
     * Creates a response DTO from a SimulationRun entity.
     *
     * @param run the entity to convert
     * @return the response DTO
     */
    public static SimulationRunResponse fromEntity(SimulationRun run) {
        return new SimulationRunResponse(
            run.getId(),
            run.getLiftSystem() != null ? run.getLiftSystem().getId() : null,
            run.getVersion() != null ? run.getVersion().getId() : null,
            run.getScenario() != null ? run.getScenario().getId() : null,
            run.getStatus(),
            run.getCreatedAt(),
            run.getStartedAt(),
            run.getEndedAt(),
            run.getTotalTicks(),
            run.getCurrentTick(),
            run.getSeed(),
            run.getErrorMessage(),
            run.getArtefactBasePath()
        );
    }

    /**
     * Calculates progress percentage for the simulation run.
     *
     * @return progress percentage (0-100) or null if not applicable
     */
    public Double getProgressPercentage() {
        if (totalTicks == null || totalTicks == 0 || currentTick == null) {
            return null;
        }
        return (currentTick.doubleValue() / totalTicks.doubleValue()) * 100.0;
    }
}
