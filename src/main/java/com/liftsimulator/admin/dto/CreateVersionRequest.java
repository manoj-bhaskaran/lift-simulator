package com.liftsimulator.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating a new lift system version.
 */
public record CreateVersionRequest(
    @NotBlank(message = "Config JSON is required")
    String config,

    Integer cloneFromVersionNumber
) {
}
