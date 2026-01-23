package com.liftsimulator.admin.dto;

import java.util.List;

/**
 * Response containing the results of scenario validation.
 */
public record ScenarioValidationResponse(
    boolean valid,
    List<ValidationIssue> errors,
    List<ValidationIssue> warnings
) {
    public ScenarioValidationResponse {
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
