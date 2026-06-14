package com.liftsimulator.admin.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for copying a scenario to another lift system version.
 */
public record CopyScenarioRequest(
    @NotNull(message = "Target lift system version ID is required")
    Long targetLiftSystemVersionId
) {
}
