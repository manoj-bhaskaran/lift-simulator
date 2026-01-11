package com.liftsimulator.admin.dto;

import com.liftsimulator.admin.entity.LiftSystem;
import java.time.OffsetDateTime;

/**
 * Response DTO for lift system details.
 */
public record LiftSystemResponse(
    Long id,
    String systemKey,
    String displayName,
    String description,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    /**
     * Creates a response DTO from a LiftSystem entity.
     *
     * @param liftSystem the entity to convert
     * @return the response DTO
     */
    public static LiftSystemResponse fromEntity(LiftSystem liftSystem) {
        return new LiftSystemResponse(
            liftSystem.getId(),
            liftSystem.getSystemKey(),
            liftSystem.getDisplayName(),
            liftSystem.getDescription(),
            liftSystem.getCreatedAt(),
            liftSystem.getUpdatedAt()
        );
    }
}
