package com.liftsimulator.admin.dto;

import tools.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for the /scenarios/validate endpoint.
 * Omits {@code name} because validation does not persist the scenario.
 */
public record ScenarioValidateRequest(
    @NotNull(message = "Scenario JSON is required")
    JsonNode scenarioJson,
    @NotNull(message = "Lift system version ID is required")
    Long liftSystemVersionId
) {
}
