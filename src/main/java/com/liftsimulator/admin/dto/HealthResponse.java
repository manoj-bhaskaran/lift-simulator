package com.liftsimulator.admin.dto;

/**
 * Response DTO for the public API health endpoint.
 */
public record HealthResponse(
    String status,
    String service,
    String timestamp
) {
}
