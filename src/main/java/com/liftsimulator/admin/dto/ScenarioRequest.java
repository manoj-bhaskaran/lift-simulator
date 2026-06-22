package com.liftsimulator.admin.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating or updating scenarios.
 */
public record ScenarioRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    String name,
    @NotNull(message = "Scenario JSON is required")
    JsonNode scenarioJson,
    @NotNull(message = "Lift system version ID is required")
    Long liftSystemVersionId
) {
}
