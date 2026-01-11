package com.liftsimulator.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for updating lift system version configuration.
 */
public record UpdateVersionConfigRequest(
    @NotBlank(message = "Config JSON is required")
    String config
) {
}
