package com.liftsimulator.admin.dto;

import java.util.List;

/**
 * Response containing the results of configuration validation.
 * Includes both structural and domain validation issues.
 * Uses defensive copying to prevent external modification of validation issues.
 */
public record ConfigValidationResponse(
    boolean valid,
    List<ValidationIssue> errors,
    List<ValidationIssue> warnings
) {
    /**
     * Compact constructor that creates defensive copies of the validation issue lists.
     */
    public ConfigValidationResponse {
        errors = List.copyOf(errors);
        warnings = List.copyOf(warnings);
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
}
