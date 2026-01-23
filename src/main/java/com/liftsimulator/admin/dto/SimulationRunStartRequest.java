package com.liftsimulator.admin.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for starting an asynchronous simulation run.
 */
public record SimulationRunStartRequest(
    @NotNull(message = "liftSystemId is required")
    Long liftSystemId,

    @NotNull(message = "versionId is required")
    Long versionId,

    Long scenarioId
) {
}
