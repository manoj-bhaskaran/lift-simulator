package com.liftsimulator.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a passenger flow entry in the UI scenario contract.
 */
public record PassengerFlowDTO(
    @NotNull(message = "startTick is required")
    @Min(value = 0, message = "startTick must be 0 or greater")
    Integer startTick,

    @NotNull(message = "originFloor is required")
    Integer originFloor,

    @NotNull(message = "destinationFloor is required")
    Integer destinationFloor,

    @NotNull(message = "passengers is required")
    @Min(value = 1, message = "passengers must be at least 1")
    Integer passengers
) {
}
