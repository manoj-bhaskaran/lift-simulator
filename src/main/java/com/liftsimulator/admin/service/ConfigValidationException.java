package com.liftsimulator.admin.service;

import com.liftsimulator.admin.dto.ConfigValidationResponse;

/**
 * Exception thrown when configuration validation fails.
 * Contains the validation response with detailed errors and warnings.
 */
public class ConfigValidationException extends RuntimeException {

    private final ConfigValidationResponse validationResponse;

    public ConfigValidationException(String message, ConfigValidationResponse validationResponse) {
        super(message);
        this.validationResponse = validationResponse;
    }

    public ConfigValidationResponse getValidationResponse() {
        return validationResponse;
    }
}
