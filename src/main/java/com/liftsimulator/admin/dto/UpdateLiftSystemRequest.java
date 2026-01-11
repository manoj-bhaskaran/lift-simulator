package com.liftsimulator.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating lift system metadata.
 */
public record UpdateLiftSystemRequest(
    @NotBlank(message = "Display name is required")
    @Size(min = 1, max = 200, message = "Display name must be between 1 and 200 characters")
    String displayName,

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    String description
) {
}
