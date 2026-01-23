package com.liftsimulator.admin.dto;

import java.util.List;

/**
 * Response DTO for scenario validation results.
 */
public record ScenarioValidationResponse(
    boolean valid,
    List<ValidationIssue> errors,
    List<ValidationIssue> warnings
) {
    /**
     * Creates a successful validation response.
     *
     * @return a validation response indicating success
     */
    public static ScenarioValidationResponse success() {
        return new ScenarioValidationResponse(true, List.of(), List.of());
    }

    /**
     * Creates a validation response with errors.
     *
     * @param errors the list of validation errors
     * @return a validation response with errors
     */
    public static ScenarioValidationResponse withErrors(List<ValidationIssue> errors) {
        return new ScenarioValidationResponse(false, errors, List.of());
    }

    /**
     * Creates a validation response with warnings.
     *
     * @param warnings the list of validation warnings
     * @return a validation response with warnings
     */
    public static ScenarioValidationResponse withWarnings(List<ValidationIssue> warnings) {
        return new ScenarioValidationResponse(true, List.of(), warnings);
    }

    /**
     * Creates a validation response with both errors and warnings.
     *
     * @param errors the list of validation errors
     * @param warnings the list of validation warnings
     * @return a validation response with errors and warnings
     */
    public static ScenarioValidationResponse withIssues(
        List<ValidationIssue> errors,
        List<ValidationIssue> warnings
    ) {
        return new ScenarioValidationResponse(errors.isEmpty(), errors, warnings);
    }
}
