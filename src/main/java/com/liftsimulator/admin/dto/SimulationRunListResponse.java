package com.liftsimulator.admin.dto;

import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.entity.SimulationRun.RunStatus;

import java.time.OffsetDateTime;

/**
 * Response DTO for simulation run list items.
 * Includes additional display information like system name, version number, and scenario name.
 */
public record SimulationRunListResponse(
    Long id,
    Long liftSystemId,
    String liftSystemName,
    Long versionId,
    Integer versionNumber,
    Long scenarioId,
    String scenarioName,
    RunStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime startedAt,
    OffsetDateTime endedAt,
    Long totalTicks,
    Long currentTick,
    Long seed,
    String errorMessage
) {
    /**
     * Creates a list response DTO from a SimulationRun entity.
     *
     * @param run the entity to convert
     * @return the list response DTO
     */
    public static SimulationRunListResponse fromEntity(SimulationRun run) {
        return new SimulationRunListResponse(
            run.getId(),
            run.getLiftSystem() != null ? run.getLiftSystem().getId() : null,
            run.getLiftSystem() != null ? run.getLiftSystem().getDisplayName() : null,
            run.getVersion() != null ? run.getVersion().getId() : null,
            run.getVersion() != null ? run.getVersion().getVersionNumber() : null,
            run.getScenario() != null ? run.getScenario().getId() : null,
            run.getScenario() != null ? run.getScenario().getName() : null,
            run.getStatus(),
            run.getCreatedAt(),
            run.getStartedAt(),
            run.getEndedAt(),
            run.getTotalTicks(),
            run.getCurrentTick(),
            run.getSeed(),
            run.getErrorMessage()
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
