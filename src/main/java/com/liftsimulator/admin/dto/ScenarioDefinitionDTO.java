package com.liftsimulator.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collections;
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
    /**
     * Compact constructor that creates a defensive copy of the passenger flows list.
     */
    public ScenarioDefinitionDTO {
        passengerFlows = passengerFlows != null ? new ArrayList<>(passengerFlows) : null;
    }

    /**
     * Returns an unmodifiable view of the passenger flows list.
     */
    @Override
    public List<PassengerFlowDTO> passengerFlows() {
        return passengerFlows != null ? Collections.unmodifiableList(passengerFlows) : null;
    }
}
