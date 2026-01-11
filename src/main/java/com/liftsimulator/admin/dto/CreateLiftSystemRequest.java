package com.liftsimulator.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new lift system.
 */
public record CreateLiftSystemRequest(
    @NotBlank(message = "System key is required")
    @Size(min = 1, max = 120, message = "System key must be between 1 and 120 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "System key must contain only alphanumeric characters, hyphens, and underscores")
    String systemKey,

    @NotBlank(message = "Display name is required")
    @Size(min = 1, max = 200, message = "Display name must be between 1 and 200 characters")
    String displayName,

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    String description
) {
}
