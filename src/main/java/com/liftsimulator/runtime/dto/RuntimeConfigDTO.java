package com.liftsimulator.runtime.dto;

import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.LiftSystemVersion;

import java.time.OffsetDateTime;

/**
 * DTO for runtime configuration responses.
 * Contains only published configuration data.
 *
 * @param systemKey unique identifier for the lift system
 * @param displayName display name of the lift system
 * @param versionNumber the version number
 * @param config the configuration JSON
 * @param publishedAt when the version was published
 */
public record RuntimeConfigDTO(
    String systemKey,
    String displayName,
    Integer versionNumber,
    String config,
    OffsetDateTime publishedAt
) {

    /**
     * Creates a RuntimeConfigDTO from entities.
     *
     * @param liftSystem the lift system entity
     * @param version the published version entity
     * @return the runtime config DTO
     */
    public static RuntimeConfigDTO fromEntity(LiftSystem liftSystem, LiftSystemVersion version) {
        return new RuntimeConfigDTO(
            liftSystem.getSystemKey(),
            liftSystem.getDisplayName(),
            version.getVersionNumber(),
            version.getConfig(),
            version.getPublishedAt()
        );
    }
}
