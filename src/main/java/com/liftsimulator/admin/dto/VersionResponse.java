package com.liftsimulator.admin.dto;

import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.entity.LiftSystemVersion.VersionStatus;
import java.time.OffsetDateTime;

/**
 * Response DTO for lift system version details.
 */
public record VersionResponse(
    Long id,
    Long liftSystemId,
    Integer versionNumber,
    VersionStatus status,
    Boolean isPublished,
    OffsetDateTime publishedAt,
    String config,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    /**
     * Creates a response DTO from a LiftSystemVersion entity.
     *
     * @param version the entity to convert
     * @return the response DTO
     */
    public static VersionResponse fromEntity(LiftSystemVersion version) {
        return new VersionResponse(
            version.getId(),
            version.getLiftSystem().getId(),
            version.getVersionNumber(),
            version.getStatus(),
            version.getIsPublished(),
            version.getPublishedAt(),
            version.getConfig(),
            version.getCreatedAt(),
            version.getUpdatedAt()
        );
    }
}
