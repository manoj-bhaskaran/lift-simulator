package com.liftsimulator.admin.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.OffsetDateTime;

/**
 * Response DTO for scenario data.
 */
public record ScenarioResponse(
    Long id,
    String name,
    JsonNode scenarioJson,
    Long liftSystemVersionId,
    LiftSystemVersionInfo versionInfo,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    /**
     * Nested DTO containing lift system version information for display purposes.
     */
    public record LiftSystemVersionInfo(
        Long liftSystemId,
        String systemKey,
        String displayName,
        Integer versionNumber,
        Integer minFloor,
        Integer maxFloor
    ) {
    }
}
