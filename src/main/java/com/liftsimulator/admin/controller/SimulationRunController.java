package com.liftsimulator.admin.controller;

import com.liftsimulator.admin.dto.SimulationRunResponse;
import com.liftsimulator.admin.dto.SimulationRunStartRequest;
import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.service.SimulationRunExecutionService;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for launching and tracking simulation runs.
 */
@RestController
@RequestMapping("/api/simulation-runs")
public class SimulationRunController {

    private final SimulationRunExecutionService runExecutionService;
    private final SimulationRunService runService;

    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP2",
        justification = "Spring-managed services injected via constructor. "
            + "Lifecycle and immutability managed by Spring container."
    )
    public SimulationRunController(
            SimulationRunExecutionService runExecutionService,
            SimulationRunService runService) {
        this.runExecutionService = runExecutionService;
        this.runService = runService;
    }

    /**
     * Starts a simulation run asynchronously.
     *
     * @param request start request payload
     * @return created simulation run metadata
     */
    @PostMapping
    public ResponseEntity<SimulationRunResponse> startRun(
        @Valid @RequestBody SimulationRunStartRequest request
    ) {
        SimulationRun run = runExecutionService.startAsyncRun(
            request.liftSystemId(),
            request.versionId(),
            request.scenarioId()
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(SimulationRunResponse.fromEntity(run));
    }

    /**
     * Retrieves a simulation run by id.
     *
     * @param id run id
     * @return simulation run metadata
     */
    @GetMapping("/{id}")
    public ResponseEntity<SimulationRunResponse> getRun(@PathVariable Long id) {
        SimulationRun run = runService.getRunById(id);
        return ResponseEntity.ok(SimulationRunResponse.fromEntity(run));
    }
}
