package com.liftsimulator.admin.dto;

/**
 * Response DTO for simulation run logs.
 */
public record SimulationLogResponse(
    Long runId,
    String logs,
    Integer tail,
    String error
) {
    public static SimulationLogResponse success(Long runId, String logs, Integer tail) {
        return new SimulationLogResponse(runId, logs, tail, null);
    }

    public static SimulationLogResponse failure(Long runId, String error) {
        return new SimulationLogResponse(runId, null, null, error);
    }
}
