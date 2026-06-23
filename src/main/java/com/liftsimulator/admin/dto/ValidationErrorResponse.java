package com.liftsimulator.admin.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Standard validation error response with field-level errors.
 */
public record ValidationErrorResponse(
    int status,
    String message,
    Map<String, List<String>> fieldErrors,
    OffsetDateTime timestamp
) {
    public ValidationErrorResponse {
        fieldErrors = Map.copyOf(fieldErrors);
    }
}
