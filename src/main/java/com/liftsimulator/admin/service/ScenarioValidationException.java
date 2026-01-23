package com.liftsimulator.admin.service;

import com.liftsimulator.admin.dto.ScenarioValidationResponse;

/**
 * Exception thrown when scenario validation fails.
 */
public class ScenarioValidationException extends RuntimeException {

    private final transient ScenarioValidationResponse validationResponse;

    public ScenarioValidationException(String message, ScenarioValidationResponse validationResponse) {
        super(message);
        this.validationResponse = validationResponse;
    }

    public ScenarioValidationResponse getValidationResponse() {
        return validationResponse;
    }
}
