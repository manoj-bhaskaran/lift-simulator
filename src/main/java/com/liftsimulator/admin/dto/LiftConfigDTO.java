package com.liftsimulator.admin.dto;

import com.liftsimulator.domain.ControllerStrategy;
import com.liftsimulator.domain.IdleParkingMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Data Transfer Object for lift system configuration.
 * This DTO represents the structure of configuration JSON stored in LiftSystemVersion.config.
 * All fields are validated using Jakarta Bean Validation annotations.
 */
public record LiftConfigDTO(
    @NotNull(message = "Number of floors is required")
    @Min(value = 2, message = "Number of floors must be at least 2")
    Integer floors,

    @NotNull(message = "Number of lifts is required")
    @Min(value = 1, message = "Number of lifts must be at least 1")
    Integer lifts,

    @NotNull(message = "Travel ticks per floor is required")
    @Min(value = 1, message = "Travel ticks per floor must be at least 1")
    Integer travelTicksPerFloor,

    @NotNull(message = "Door transition ticks is required")
    @Min(value = 1, message = "Door transition ticks must be at least 1")
    Integer doorTransitionTicks,

    @NotNull(message = "Door dwell ticks is required")
    @Min(value = 1, message = "Door dwell ticks must be at least 1")
    Integer doorDwellTicks,

    @NotNull(message = "Door reopen window ticks is required")
    @Min(value = 0, message = "Door reopen window ticks must be at least 0")
    Integer doorReopenWindowTicks,

    @NotNull(message = "Home floor is required")
    @Min(value = 0, message = "Home floor must be at least 0")
    Integer homeFloor,

    @NotNull(message = "Idle timeout ticks is required")
    @Min(value = 0, message = "Idle timeout ticks must be at least 0")
    Integer idleTimeoutTicks,

    @NotNull(message = "Controller strategy is required")
    ControllerStrategy controllerStrategy,

    @NotNull(message = "Idle parking mode is required")
    IdleParkingMode idleParkingMode
) {
}
