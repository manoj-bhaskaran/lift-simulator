package com.liftsimulator.admin.dto;

import com.liftsimulator.admin.entity.EventType;
import com.liftsimulator.domain.Direction;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for scenario events.
 */
public record ScenarioEventRequest(
    @NotNull(message = "Tick is required")
    @Min(value = 0, message = "Tick must be non-negative")
    Long tick,

    @NotNull(message = "Event type is required")
    EventType eventType,

    @Size(max = 200, message = "Description must not exceed 200 characters")
    String description,

    Integer originFloor,

    Integer destinationFloor,

    Direction direction,

    Integer eventOrder
) {
}
