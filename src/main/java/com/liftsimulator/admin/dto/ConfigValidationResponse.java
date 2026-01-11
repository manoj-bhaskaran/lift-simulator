package com.liftsimulator.admin.dto;

import java.util.List;

/**
 * Response containing the results of configuration validation.
 * Includes both structural and domain validation issues.
 */
public record ConfigValidationResponse(
    boolean valid,
    List<ValidationIssue> errors,
    List<ValidationIssue> warnings
) {
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
}
