package com.liftsimulator.admin.controller;

import com.liftsimulator.admin.dto.ScenarioRequest;
import com.liftsimulator.admin.dto.ScenarioResponse;
import com.liftsimulator.admin.dto.ScenarioValidationResponse;
import com.liftsimulator.admin.service.ScenarioService;
import com.liftsimulator.admin.service.ScenarioValidationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/scenarios")
@Tag(name = "Scenarios", description = "Scenario management endpoints")
@SecurityRequirement(name = "basicAuth")
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
    @Operation(
        summary = "Create a new scenario",
        description = "Creates a new scenario with the provided configuration"
    )
    @ApiResponse(responseCode = "201", description = "Scenario created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request payload")
    @ApiResponse(responseCode = "401", description = "Unauthorized - valid credentials required")
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
    @Operation(
        summary = "List all scenarios",
        description = "Retrieves a list of all scenarios in the system"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved scenarios list")
    @ApiResponse(responseCode = "401", description = "Unauthorized - valid credentials required")
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
     * Validates both structure and floor ranges against the specified lift system version.
     *
     * @param request scenario payload including lift system version ID
     * @return validation response
     */
    @PostMapping("/validate")
    public ResponseEntity<ScenarioValidationResponse> validateScenario(
        @Valid @RequestBody ScenarioRequest request
    ) {
        ScenarioValidationResponse response = scenarioValidationService.validate(
            request.scenarioJson(),
            request.liftSystemVersionId()
        );
        return ResponseEntity.ok(response);
    }
}
