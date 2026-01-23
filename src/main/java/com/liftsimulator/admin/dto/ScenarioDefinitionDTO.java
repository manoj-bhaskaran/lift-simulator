package com.liftsimulator.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Minimal schema for UI scenario definitions (passenger flow contract).
 */
public record ScenarioDefinitionDTO(
    @NotNull(message = "durationTicks is required")
    @Min(value = 1, message = "durationTicks must be at least 1")
    Integer durationTicks,

    @NotNull(message = "passengerFlows is required")
    @Size(min = 1, message = "passengerFlows must contain at least one entry")
    @Valid
    List<PassengerFlowDTO> passengerFlows,

    @Min(value = 0, message = "seed must be 0 or greater")
    Integer seed
) {
}
