package com.liftsimulator.admin.dto;

import com.liftsimulator.admin.entity.EventType;
import com.liftsimulator.admin.entity.ScenarioEvent;
import com.liftsimulator.domain.Direction;

import java.time.OffsetDateTime;

/**
 * Response DTO for scenario events.
 */
public record ScenarioEventResponse(
    Long id,
    Long tick,
    EventType eventType,
    String description,
    Integer originFloor,
    Integer destinationFloor,
    Direction direction,
    Integer eventOrder,
    OffsetDateTime createdAt
) {
    /**
     * Creates a response DTO from a ScenarioEvent entity.
     *
     * @param event the entity to convert
     * @return the response DTO
     */
    public static ScenarioEventResponse fromEntity(ScenarioEvent event) {
        return new ScenarioEventResponse(
            event.getId(),
            event.getTick(),
            event.getEventType(),
            event.getDescription(),
            event.getOriginFloor(),
            event.getDestinationFloor(),
            event.getDirection(),
            event.getEventOrder(),
            event.getCreatedAt()
        );
    }
}
