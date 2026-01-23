package com.liftsimulator.admin.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;

/**
 * Response DTO for scenario data.
 */
public record ScenarioResponse(
    Long id,
    JsonNode scenarioJson,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
}
