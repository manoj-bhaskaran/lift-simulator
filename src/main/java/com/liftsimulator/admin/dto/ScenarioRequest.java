package com.liftsimulator.admin.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating or updating scenarios.
 */
public record ScenarioRequest(
    @NotNull(message = "Name is required")
    String name,
    @NotNull(message = "Scenario JSON is required")
    JsonNode scenarioJson
) {
}
