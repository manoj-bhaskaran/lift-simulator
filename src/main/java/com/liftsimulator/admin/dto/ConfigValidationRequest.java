package com.liftsimulator.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to validate a configuration JSON without saving it.
 */
public record ConfigValidationRequest(
    @NotBlank(message = "Config JSON is required")
    String config
) {
}
