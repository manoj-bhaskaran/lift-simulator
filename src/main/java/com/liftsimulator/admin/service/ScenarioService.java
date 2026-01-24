package com.liftsimulator.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftsimulator.admin.dto.LiftConfigDTO;
import com.liftsimulator.admin.dto.ScenarioRequest;
import com.liftsimulator.admin.dto.ScenarioResponse;
import com.liftsimulator.admin.dto.ScenarioValidationResponse;
import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.entity.Scenario;
import com.liftsimulator.admin.repository.LiftSystemVersionRepository;
import com.liftsimulator.admin.repository.ScenarioRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing stored scenarios.
 */
@Service
@Transactional(readOnly = true)
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final LiftSystemVersionRepository versionRepository;
    private final ScenarioValidationService scenarioValidationService;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring-managed beans injected via constructor. "
                    + "Lifecycle and immutability managed by Spring container."
    )
    public ScenarioService(
            ScenarioRepository scenarioRepository,
            LiftSystemVersionRepository versionRepository,
            ScenarioValidationService scenarioValidationService,
            ObjectMapper objectMapper) {
        this.scenarioRepository = scenarioRepository;
        this.versionRepository = versionRepository;
        this.scenarioValidationService = scenarioValidationService;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates a new scenario after validation.
     *
     * @param request scenario payload including lift system version ID
     * @return created scenario response
     * @throws ResourceNotFoundException if lift system version not found
     * @throws ScenarioValidationException if scenario validation fails
     */
    @Transactional
    public ScenarioResponse createScenario(ScenarioRequest request) {
        // Fetch and validate lift system version exists
        LiftSystemVersion version = versionRepository.findById(request.liftSystemVersionId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Lift system version not found with id: " + request.liftSystemVersionId()
            ));

        // Validate scenario JSON with floor range validation
        validateScenarioJson(request.scenarioJson(), request.liftSystemVersionId());

        Scenario scenario = new Scenario(
            request.name(),
            serializeScenarioJson(request.scenarioJson()),
            version
        );
        Scenario savedScenario = scenarioRepository.save(scenario);

        return toResponse(savedScenario);
    }

    /**
     * Updates an existing scenario after validation.
     *
     * @param id scenario id
     * @param request scenario payload including lift system version ID
     * @return updated scenario response
     * @throws ResourceNotFoundException if scenario or lift system version not found
     * @throws ScenarioValidationException if scenario validation fails
     */
    @Transactional
    public ScenarioResponse updateScenario(Long id, ScenarioRequest request) {
        Scenario scenario = scenarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Scenario not found with id: " + id
            ));

        // Fetch and validate lift system version exists
        LiftSystemVersion version = versionRepository.findById(request.liftSystemVersionId())
            .orElseThrow(() -> new ResourceNotFoundException(
                "Lift system version not found with id: " + request.liftSystemVersionId()
            ));

        // Validate scenario JSON with floor range validation
        validateScenarioJson(request.scenarioJson(), request.liftSystemVersionId());

        scenario.setName(request.name());
        scenario.setScenarioJson(serializeScenarioJson(request.scenarioJson()));
        scenario.setLiftSystemVersion(version);
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

    /**
     * Retrieves all scenarios.
     *
     * @return list of scenario responses
     */
    public List<ScenarioResponse> getAllScenarios() {
        return scenarioRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Deletes a scenario by id.
     *
     * @param id scenario id
     */
    @Transactional
    public void deleteScenario(Long id) {
        Scenario scenario = scenarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Scenario not found with id: " + id
            ));

        scenarioRepository.delete(scenario);
    }

    private void validateScenarioJson(JsonNode scenarioJson, Long liftSystemVersionId) {
        ScenarioValidationResponse validationResponse =
            scenarioValidationService.validate(scenarioJson, liftSystemVersionId);
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

            // Build version info if available
            ScenarioResponse.LiftSystemVersionInfo versionInfo = null;
            Long versionId = null;

            if (scenario.getLiftSystemVersion() != null) {
                LiftSystemVersion version = scenario.getLiftSystemVersion();
                LiftSystem liftSystem = version.getLiftSystem();
                versionId = version.getId();

                // Parse config to get floor range
                LiftConfigDTO config = objectMapper.readValue(
                    version.getConfig(),
                    LiftConfigDTO.class
                );

                versionInfo = new ScenarioResponse.LiftSystemVersionInfo(
                    liftSystem.getId(),
                    liftSystem.getSystemKey(),
                    liftSystem.getDisplayName(),
                    version.getVersionNumber(),
                    config.minFloor(),
                    config.maxFloor()
                );
            }

            return new ScenarioResponse(
                scenario.getId(),
                scenario.getName(),
                scenarioJson,
                versionId,
                versionInfo,
                scenario.getCreatedAt(),
                scenario.getUpdatedAt()
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored scenario JSON could not be parsed", e);
        }
    }
}
