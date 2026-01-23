package com.liftsimulator.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftsimulator.admin.dto.ScenarioRequest;
import com.liftsimulator.admin.dto.ScenarioResponse;
import com.liftsimulator.admin.dto.ScenarioValidationResponse;
import com.liftsimulator.admin.entity.Scenario;
import com.liftsimulator.admin.repository.ScenarioRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing stored scenarios.
 */
@Service
@Transactional(readOnly = true)
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioValidationService scenarioValidationService;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring-managed beans injected via constructor. "
                    + "Lifecycle and immutability managed by Spring container."
    )
    public ScenarioService(
            ScenarioRepository scenarioRepository,
            ScenarioValidationService scenarioValidationService,
            ObjectMapper objectMapper) {
        this.scenarioRepository = scenarioRepository;
        this.scenarioValidationService = scenarioValidationService;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a new scenario after validation.
     *
     * @param request scenario payload
     * @return created scenario response
     */
    @Transactional
    public ScenarioResponse createScenario(ScenarioRequest request) {
        validateScenarioJson(request.scenarioJson());

        Scenario scenario = new Scenario(serializeScenarioJson(request.scenarioJson()));
        Scenario savedScenario = scenarioRepository.save(scenario);

        return toResponse(savedScenario);
    }

    /**
     * Updates an existing scenario after validation.
     *
     * @param id scenario id
     * @param request scenario payload
     * @return updated scenario response
     */
    @Transactional
    public ScenarioResponse updateScenario(Long id, ScenarioRequest request) {
        Scenario scenario = scenarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Scenario not found with id: " + id
            ));

        validateScenarioJson(request.scenarioJson());

        scenario.setScenarioJson(serializeScenarioJson(request.scenarioJson()));
        Scenario savedScenario = scenarioRepository.save(scenario);

        return toResponse(savedScenario);
    }

    /**
     * Retrieves a scenario by id.
     *
     * @param id scenario id
     * @return scenario response
     */
    public ScenarioResponse getScenario(Long id) {
        Scenario scenario = scenarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Scenario not found with id: " + id
            ));

        return toResponse(scenario);
    }

    private void validateScenarioJson(JsonNode scenarioJson) {
        ScenarioValidationResponse validationResponse = scenarioValidationService.validate(scenarioJson);
        if (validationResponse.hasErrors()) {
            throw new ScenarioValidationException("Scenario validation failed", validationResponse);
        }
    }

    private String serializeScenarioJson(JsonNode scenarioJson) {
        try {
            return objectMapper.writeValueAsString(scenarioJson);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Scenario JSON could not be serialized");
        }
    }

    private ScenarioResponse toResponse(Scenario scenario) {
        try {
            JsonNode scenarioJson = objectMapper.readTree(scenario.getScenarioJson());
            return new ScenarioResponse(
                scenario.getId(),
                scenarioJson,
                scenario.getCreatedAt(),
                scenario.getUpdatedAt()
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored scenario JSON could not be parsed");
        }
    }
}
