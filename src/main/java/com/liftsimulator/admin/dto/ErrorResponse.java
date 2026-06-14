package com.liftsimulator.admin.dto;

import java.time.OffsetDateTime;

/**
 * Standard error response for API failures.
 */
public record ErrorResponse(
    int status,
    String message,
    OffsetDateTime timestamp
) {
}
