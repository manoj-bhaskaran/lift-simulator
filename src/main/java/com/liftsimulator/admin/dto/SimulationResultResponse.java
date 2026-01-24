package com.liftsimulator.admin.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Response DTO for simulation run results.
 */
public record SimulationResultResponse(
    Long runId,
    String status,
    JsonNode results,
    String errorMessage,
    String logsUrl
) {
    /**
     * Creates a success response with results.
     *
     * @param runId the run ID
     * @param results the simulation results as JSON
     * @return the response DTO
     */
    public static SimulationResultResponse success(Long runId, JsonNode results) {
        return new SimulationResultResponse(
            runId,
            "SUCCEEDED",
            results,
            null,
            "/api/simulation-runs/" + runId + "/logs"
        );
    }

    /**
     * Creates a failure response.
     *
     * @param runId the run ID
     * @param errorMessage the error message
     * @return the response DTO
     */
    public static SimulationResultResponse failure(Long runId, String errorMessage) {
        return new SimulationResultResponse(
            runId,
            "FAILED",
            null,
            errorMessage,
            "/api/simulation-runs/" + runId + "/logs"
        );
    }

    /**
     * Creates a running response.
     *
     * @param runId the run ID
     * @return the response DTO
     */
    public static SimulationResultResponse running(Long runId) {
        return new SimulationResultResponse(
            runId,
            "RUNNING",
            null,
            "Simulation is still running",
            null
        );
    }

    /**
     * Creates a response for a succeeded run where results file is unavailable.
     * Preserves SUCCEEDED status while indicating results cannot be accessed.
     *
     * @param runId the run ID
     * @param errorMessage the error message explaining why results are unavailable
     * @return the response DTO
     */
    public static SimulationResultResponse succeededWithoutResults(Long runId, String errorMessage) {
        return new SimulationResultResponse(
            runId,
            "SUCCEEDED",
            null,
            errorMessage,
            "/api/simulation-runs/" + runId + "/logs"
        );
    }
}
