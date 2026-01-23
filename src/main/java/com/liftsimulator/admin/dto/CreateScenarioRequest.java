package com.liftsimulator.admin.dto;

import com.liftsimulator.domain.ControllerStrategy;
import com.liftsimulator.domain.IdleParkingMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for creating a new scenario.
 */
public record CreateScenarioRequest(
    @NotBlank(message = "Scenario name is required")
    @Size(min = 1, max = 200, message = "Scenario name must be between 1 and 200 characters")
    String name,

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    String description,

    @NotNull(message = "Total ticks is required")
    @Min(value = 1, message = "Total ticks must be at least 1")
    Integer totalTicks,

    @NotNull(message = "Min floor is required")
    Integer minFloor,

    @NotNull(message = "Max floor is required")
    Integer maxFloor,

    Integer initialFloor,

    Integer homeFloor,

    @Min(value = 1, message = "Travel ticks per floor must be at least 1")
    Integer travelTicksPerFloor,

    @Min(value = 1, message = "Door transition ticks must be at least 1")
    Integer doorTransitionTicks,

    @Min(value = 1, message = "Door dwell ticks must be at least 1")
    Integer doorDwellTicks,

    ControllerStrategy controllerStrategy,

    IdleParkingMode idleParkingMode,

    Long seed,

    @Valid
    List<ScenarioEventRequest> events
) {
}
