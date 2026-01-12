package com.liftsimulator.runtime.dto;

/**
 * Response for simulator launch requests.
 */
public record SimulationLaunchResponse(
    boolean success,
    String message,
    Long processId
) {
}
