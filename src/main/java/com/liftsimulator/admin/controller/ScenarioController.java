package com.liftsimulator.admin.controller;

import com.liftsimulator.admin.dto.CreateScenarioRequest;
import com.liftsimulator.admin.dto.ScenarioResponse;
import com.liftsimulator.admin.dto.ScenarioValidationResponse;
import com.liftsimulator.admin.dto.UpdateScenarioRequest;
import com.liftsimulator.admin.service.ScenarioService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing passenger flow scenarios.
 */
@RestController
@RequestMapping("/api/scenarios")
public class ScenarioController {

    private final ScenarioService scenarioService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring-managed service injected via constructor. "
                    + "Lifecycle and immutability managed by Spring container."
    )
    public ScenarioController(ScenarioService scenarioService) {
        this.scenarioService = scenarioService;
    }

    /**
     * Creates a new scenario.
     *
     * @param request the creation request
     * @return the created scenario with 201 status
     */
    @PostMapping
    public ResponseEntity<ScenarioResponse> createScenario(
        @Valid @RequestBody CreateScenarioRequest request
    ) {
        ScenarioResponse response = scenarioService.createScenario(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves all scenarios.
     *
     * @return list of all scenarios
     */
    @GetMapping
    public ResponseEntity<List<ScenarioResponse>> getAllScenarios() {
        List<ScenarioResponse> scenarios = scenarioService.getAllScenarios();
        return ResponseEntity.ok(scenarios);
    }

    /**
     * Retrieves a scenario by ID with events.
     *
     * @param id the scenario ID
     * @return the scenario details with events
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScenarioResponse> getScenarioById(@PathVariable Long id) {
        ScenarioResponse response = scenarioService.getScenarioById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing scenario.
     *
     * @param id the scenario ID
     * @param request the update request
     * @return the updated scenario
     */
    @PutMapping("/{id}")
    public ResponseEntity<ScenarioResponse> updateScenario(
        @PathVariable Long id,
        @Valid @RequestBody UpdateScenarioRequest request
    ) {
        ScenarioResponse response = scenarioService.updateScenario(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a scenario.
     *
     * @param id the scenario ID
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScenario(@PathVariable Long id) {
        scenarioService.deleteScenario(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Validates a scenario creation request.
     *
     * @param request the creation request
     * @return validation response
     */
    @PostMapping("/validate")
    public ResponseEntity<ScenarioValidationResponse> validateScenario(
        @RequestBody CreateScenarioRequest request
    ) {
        ScenarioValidationResponse response = scenarioService.validateScenario(request);
        return ResponseEntity.ok(response);
    }
}
