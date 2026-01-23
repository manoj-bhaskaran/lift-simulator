package com.liftsimulator.admin.dto;

import com.liftsimulator.admin.entity.Scenario;
import com.liftsimulator.domain.ControllerStrategy;
import com.liftsimulator.domain.IdleParkingMode;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO for scenario details.
 */
public record ScenarioResponse(
    Long id,
    String name,
    String description,
    Integer totalTicks,
    Integer minFloor,
    Integer maxFloor,
    Integer initialFloor,
    Integer homeFloor,
    Integer travelTicksPerFloor,
    Integer doorTransitionTicks,
    Integer doorDwellTicks,
    ControllerStrategy controllerStrategy,
    IdleParkingMode idleParkingMode,
    Long seed,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt,
    List<ScenarioEventResponse> events
) {
    /**
     * Creates a response DTO from a Scenario entity without events.
     *
     * @param scenario the entity to convert
     * @return the response DTO
     */
    public static ScenarioResponse fromEntity(Scenario scenario) {
        return fromEntity(scenario, false);
    }

    /**
     * Creates a response DTO from a Scenario entity.
     *
     * @param scenario the entity to convert
     * @param includeEvents whether to include the events list
     * @return the response DTO
     */
    public static ScenarioResponse fromEntity(Scenario scenario, boolean includeEvents) {
        List<ScenarioEventResponse> eventResponses = null;
        if (includeEvents) {
            eventResponses = scenario.getEvents().stream()
                .map(ScenarioEventResponse::fromEntity)
                .collect(Collectors.toList());
        }

        return new ScenarioResponse(
            scenario.getId(),
            scenario.getName(),
            scenario.getDescription(),
            scenario.getTotalTicks(),
            scenario.getMinFloor(),
            scenario.getMaxFloor(),
            scenario.getInitialFloor(),
            scenario.getHomeFloor(),
            scenario.getTravelTicksPerFloor(),
            scenario.getDoorTransitionTicks(),
            scenario.getDoorDwellTicks(),
            scenario.getControllerStrategy(),
            scenario.getIdleParkingMode(),
            scenario.getSeed(),
            scenario.getCreatedAt(),
            scenario.getUpdatedAt(),
            eventResponses
        );
    }
}
