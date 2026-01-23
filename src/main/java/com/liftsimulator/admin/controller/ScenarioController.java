package com.liftsimulator.admin.controller;

import com.liftsimulator.admin.dto.ScenarioRequest;
import com.liftsimulator.admin.dto.ScenarioResponse;
import com.liftsimulator.admin.dto.ScenarioValidationResponse;
import com.liftsimulator.admin.service.ScenarioService;
import com.liftsimulator.admin.service.ScenarioValidationService;
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
 * REST controller for managing scenarios.
 */
@RestController
@RequestMapping("/api/scenarios")
public class ScenarioController {

    private final ScenarioService scenarioService;
    private final ScenarioValidationService scenarioValidationService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring-managed services injected via constructor. "
                    + "Lifecycle and immutability managed by Spring container."
    )
    public ScenarioController(
            ScenarioService scenarioService,
            ScenarioValidationService scenarioValidationService) {
        this.scenarioService = scenarioService;
        this.scenarioValidationService = scenarioValidationService;
    }

    /**
     * Creates a scenario.
     *
     * @param request scenario payload
     * @return created scenario response
     */
    @PostMapping
    public ResponseEntity<ScenarioResponse> createScenario(
        @Valid @RequestBody ScenarioRequest request
    ) {
        ScenarioResponse response = scenarioService.createScenario(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates a scenario.
     *
     * @param id scenario id
     * @param request scenario payload
     * @return updated scenario response
     */
    @PutMapping("/{id}")
    public ResponseEntity<ScenarioResponse> updateScenario(
        @PathVariable Long id,
        @Valid @RequestBody ScenarioRequest request
    ) {
        ScenarioResponse response = scenarioService.updateScenario(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves all scenarios.
     *
     * @return list of scenario responses
     */
    @GetMapping
    public ResponseEntity<List<ScenarioResponse>> getAllScenarios() {
        List<ScenarioResponse> responses = scenarioService.getAllScenarios();
        return ResponseEntity.ok(responses);
    }

    /**
     * Retrieves a scenario by id.
     *
     * @param id scenario id
     * @return scenario response
     */
    @GetMapping("/{id}")
    public ResponseEntity<ScenarioResponse> getScenario(@PathVariable Long id) {
        ScenarioResponse response = scenarioService.getScenario(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a scenario by id.
     *
     * @param id scenario id
     * @return no content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteScenario(@PathVariable Long id) {
        scenarioService.deleteScenario(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Validates a scenario payload without persisting it.
     *
     * @param request scenario payload
     * @return validation response
     */
    @PostMapping("/validate")
    public ResponseEntity<ScenarioValidationResponse> validateScenario(
        @Valid @RequestBody ScenarioRequest request
    ) {
        ScenarioValidationResponse response = scenarioValidationService.validate(request.scenarioJson());
        return ResponseEntity.ok(response);
    }
}
