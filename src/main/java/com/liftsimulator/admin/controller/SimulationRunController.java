package com.liftsimulator.admin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.liftsimulator.admin.dto.ArtefactInfo;
import com.liftsimulator.admin.dto.CreateSimulationRunRequest;
import com.liftsimulator.admin.dto.SimulationResultResponse;
import com.liftsimulator.admin.dto.SimulationRunResponse;
import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.service.ArtefactService;
import com.liftsimulator.admin.service.SimulationRunService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing simulation runs.
 */
@RestController
@RequestMapping("/api/simulation-runs")
public class SimulationRunController {

    private final SimulationRunService simulationRunService;
    private final ArtefactService artefactService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring-managed services injected via constructor. "
                    + "Lifecycle and immutability managed by Spring container."
    )
    public SimulationRunController(
            SimulationRunService simulationRunService,
            ArtefactService artefactService) {
        this.simulationRunService = simulationRunService;
        this.artefactService = artefactService;
    }

    /**
     * Creates and starts a new simulation run.
     * Endpoint: POST /api/simulation-runs
     *
     * @param request the creation request containing liftSystemId, versionId, seed (optional)
     * @return the created and started simulation run with 201 status
     */
    @PostMapping
    public ResponseEntity<SimulationRunResponse> createSimulationRun(
            @Valid @RequestBody CreateSimulationRunRequest request
    ) throws IOException {
        SimulationRun run = simulationRunService.createAndStartRun(
                request.liftSystemId(),
                request.versionId(),
                request.seed()
        );
        SimulationRunResponse response = SimulationRunResponse.fromEntity(run);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves the status and details of a simulation run.
     * Endpoint: GET /api/simulation-runs/{id}
     *
     * @param id the run ID
     * @return the simulation run details including status, timestamps, and progress
     */
    @GetMapping("/{id}")
    public ResponseEntity<SimulationRunResponse> getSimulationRun(@PathVariable Long id) {
        SimulationRun run = simulationRunService.getRunById(id);
        SimulationRunResponse response = SimulationRunResponse.fromEntity(run);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the results of a simulation run.
     * Endpoint: GET /api/simulation-runs/{id}/results
     *
     * Returns:
     * - 200 with structured results JSON when SUCCEEDED
     * - 200 with error message and logs link when FAILED
     * - 409 when RUNNING (simulation still in progress)
     * - 400 when CREATED or CANCELLED
     *
     * @param id the run ID
     * @return the simulation results or appropriate status response
     */
    @GetMapping("/{id}/results")
    public ResponseEntity<SimulationResultResponse> getSimulationResults(@PathVariable Long id) {
        SimulationRun run = simulationRunService.getRunById(id);

        return switch (run.getStatus()) {
            case SUCCEEDED -> {
                try {
                    JsonNode results = artefactService.readResults(run);
                    yield ResponseEntity.ok(SimulationResultResponse.success(id, results));
                } catch (IOException e) {
                    // Results file not found or not readable - preserve SUCCEEDED status
                    // but indicate results are unavailable
                    yield ResponseEntity.ok(SimulationResultResponse.succeededWithoutResults(
                            id,
                            "Results file not available: " + e.getMessage()
                    ));
                }
            }
            case FAILED -> ResponseEntity.ok(SimulationResultResponse.failure(id, run.getErrorMessage()));
            case RUNNING -> ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(SimulationResultResponse.running(id));
            case CREATED, CANCELLED -> ResponseEntity.badRequest()
                    .body(new SimulationResultResponse(
                            id,
                            run.getStatus().name(),
                            null,
                            "Results not available for " + run.getStatus().name() + " runs",
                            null
                    ));
        };
    }

    /**
     * Retrieves the logs of a simulation run.
     * Endpoint: GET /api/simulation-runs/{id}/logs?tail=N
     *
     * @param id the run ID
     * @param tail optional number of lines to retrieve from the end of the log (default: all lines)
     * @return the log content as plain text
     */
    @GetMapping("/{id}/logs")
    public ResponseEntity<Map<String, String>> getSimulationLogs(
            @PathVariable Long id,
            @RequestParam(required = false) Integer tail
    ) {
        SimulationRun run = simulationRunService.getRunById(id);

        try {
            String logs = artefactService.readLogs(run, tail);
            Map<String, String> response = new HashMap<>();
            response.put("runId", id.toString());
            response.put("logs", logs);
            if (tail != null) {
                response.put("tail", tail.toString());
            }
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("runId", id.toString());
            errorResponse.put("error", "Failed to read logs: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Lists all artefacts (downloadable files) associated with a simulation run.
     * Endpoint: GET /api/simulation-runs/{id}/artefacts
     *
     * @param id the run ID
     * @return list of artefact information including name, path, size, and mime type
     */
    @GetMapping("/{id}/artefacts")
    public ResponseEntity<List<ArtefactInfo>> listSimulationArtefacts(@PathVariable Long id) {
        SimulationRun run = simulationRunService.getRunById(id);

        try {
            List<ArtefactInfo> artefacts = artefactService.listArtefacts(run);
            return ResponseEntity.ok(artefacts);
        } catch (IOException e) {
            // Return empty list if directory doesn't exist or can't be read
            return ResponseEntity.ok(List.of());
        }
    }
}
