package com.liftsimulator.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating a new simulation run.
 */
public record CreateSimulationRunRequest(
    @NotNull(message = "liftSystemId is required")
    @JsonProperty("liftSystemId")
    Long liftSystemId,

    @NotNull(message = "versionId is required")
    @JsonProperty("versionId")
    Long versionId,

    @JsonProperty("scenarioId")
    Long scenarioId,

    @JsonProperty("seed")
    Long seed
) {
}
