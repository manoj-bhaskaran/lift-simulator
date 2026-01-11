package com.liftsimulator.admin.service;

import com.liftsimulator.admin.dto.ConfigValidationResponse;

/**
 * Exception thrown when configuration validation fails.
 * Contains the validation response with detailed errors and warnings.
 * The validation response is transient as it's intended for immediate error reporting,
 * not for exception serialization.
 */
public class ConfigValidationException extends RuntimeException {

    private final transient ConfigValidationResponse validationResponse;

    public ConfigValidationException(String message, ConfigValidationResponse validationResponse) {
        super(message);
        this.validationResponse = validationResponse;
    }

    public ConfigValidationResponse getValidationResponse() {
        return validationResponse;
    }
}
