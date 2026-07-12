package com.liftsimulator.admin.service;

import com.liftsimulator.admin.dto.LiftConfigDTO;
import com.liftsimulator.admin.dto.ScenarioRequest;
import com.liftsimulator.admin.dto.ScenarioResponse;
import com.liftsimulator.admin.dto.ScenarioValidationResponse;
import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.entity.Scenario;
import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.repository.LiftSystemVersionRepository;
import com.liftsimulator.admin.repository.ScenarioRepository;
import com.liftsimulator.admin.repository.SimulationRunRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.stream.Collectors;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Service for managing stored scenarios.
 */
@Service
@Transactional(readOnly = true)
public class ScenarioService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScenarioService.class);

    private static final int SCENARIO_NAME_MAX_LENGTH = 200;
    private static final String COPY_NAME_PREFIX = "Copy of ";
    private static final int MAX_COPY_NAME_ATTEMPTS = 100;

    private final ScenarioRepository scenarioRepository;
    private final LiftSystemVersionRepository versionRepository;
    private final ScenarioValidationService scenarioValidationService;
    private final SimulationRunRepository runRepository;
    private final ArtefactService artefactService;
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
            SimulationRunRepository runRepository,
            ArtefactService artefactService,
            ObjectMapper objectMapper) {
        this.scenarioRepository = scenarioRepository;
        this.versionRepository = versionRepository;
        this.scenarioValidationService = scenarioValidationService;
        this.runRepository = runRepository;
        this.artefactService = artefactService;
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

        // Reject duplicate names within the same version before validating/saving
        if (scenarioRepository.existsByLiftSystemVersionIdAndName(
                request.liftSystemVersionId(), request.name())) {
            throw new IllegalStateException(duplicateNameMessage(request.name()));
        }

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

        // Reject duplicate names within the same version, allowing this scenario to keep its name
        if (scenarioRepository.existsByLiftSystemVersionIdAndNameAndIdNot(
                request.liftSystemVersionId(), request.name(), id)) {
            throw new IllegalStateException(duplicateNameMessage(request.name()));
        }

        // Validate scenario JSON with floor range validation
        validateScenarioJson(request.scenarioJson(), request.liftSystemVersionId());

        scenario.setName(request.name());
        scenario.setScenarioJson(serializeScenarioJson(request.scenarioJson()));
        scenario.setLiftSystemVersion(version);
        Scenario savedScenario = scenarioRepository.save(scenario);

        return toResponse(savedScenario);
    }

    /**
     * Copies a scenario to another lift system version after validating the existing payload
     * against the target version constraints.
     *
     * @param sourceScenarioId source scenario id
     * @param targetLiftSystemVersionId target lift system version id
     * @return copied scenario response
     * @throws ResourceNotFoundException if source scenario or target version is not found
     * @throws ScenarioValidationException if scenario validation fails for the target version
     */
    @Transactional
    public ScenarioResponse copyScenario(Long sourceScenarioId, Long targetLiftSystemVersionId) {
        Scenario sourceScenario = scenarioRepository.findById(sourceScenarioId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Scenario not found with id: " + sourceScenarioId
            ));

        LiftSystemVersion targetVersion = versionRepository.findById(targetLiftSystemVersionId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Lift system version not found with id: " + targetLiftSystemVersionId
            ));

        JsonNode scenarioJson = parseStoredScenarioJson(sourceScenario);
        validateScenarioJson(scenarioJson, targetLiftSystemVersionId);

        Scenario copiedScenario = new Scenario(
            uniqueCopyName(sourceScenario.getName(), targetLiftSystemVersionId),
            serializeScenarioJson(scenarioJson),
            targetVersion
        );
        Scenario savedScenario = scenarioRepository.save(copiedScenario);

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
     * <p>Uses JOIN-FETCH to eagerly load relationships and prevent N+1 queries
     * when accessing scenario.liftSystemVersion.liftSystem in toResponse().</p>
     *
     * @return list of scenario responses
     */
    public List<ScenarioResponse> getAllScenarios() {
        return scenarioRepository.findAllWithDetails().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Deletes a scenario by id.
     *
     * <p>The scenario row is write-locked before capturing associated runs so
     * concurrent run creation cannot insert a new child row after the capture
     * query but before the delete commits. This keeps post-commit artefact cleanup
     * aligned with the database cascade. Cleanup failures are logged and do not
     * fail an already-committed cascade delete.</p>
     *
     * @param id scenario id
     */
    @Transactional
    public void deleteScenario(Long id) {
        Scenario scenario = scenarioRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Scenario not found with id: " + id
            ));

        List<SimulationRun> runs = runRepository.findByScenarioId(id);
        scenarioRepository.delete(scenario);
        deleteRunArtefactsAfterCommit(runs);
    }

    /**
     * Counts simulation runs associated with a scenario.
     *
     * @param id scenario id
     * @return number of runs that will be removed by scenario deletion
     */
    public long countSimulationRuns(Long id) {
        if (!scenarioRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                "Scenario not found with id: " + id
            );
        }
        return runRepository.countByScenarioId(id);
    }

    private void deleteRunArtefactsAfterCommit(List<SimulationRun> runs) {
        if (runs.isEmpty()) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteRunArtefacts(runs);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteRunArtefacts(runs);
            }
        });
    }

    private void deleteRunArtefacts(List<SimulationRun> runs) {
        for (SimulationRun run : runs) {
            try {
                artefactService.deleteArtefacts(run);
            } catch (Exception e) {
                LOGGER.warn(
                    "Best-effort cascade artefact deletion failed for simulation run {} at {}",
                    run.getId(),
                    run.getArtefactBasePath(),
                    e
                );
            }
        }
    }

    private void validateScenarioJson(JsonNode scenarioJson, Long liftSystemVersionId) {
        ScenarioValidationResponse validationResponse =
            scenarioValidationService.validate(scenarioJson, liftSystemVersionId);
        if (validationResponse.hasErrors()) {
            throw new ScenarioValidationException("Scenario validation failed", validationResponse);
        }
    }

    private JsonNode parseStoredScenarioJson(Scenario scenario) {
        try {
            return objectMapper.readTree(scenario.getScenarioJson());
        } catch (JacksonException e) {
            throw new IllegalStateException("Stored scenario JSON could not be parsed", e);
        }
    }

    private String copiedScenarioName(String sourceName) {
        int sourceNameLimit = SCENARIO_NAME_MAX_LENGTH - COPY_NAME_PREFIX.length();
        String boundedSourceName = sourceName.length() > sourceNameLimit
            ? sourceName.substring(0, sourceNameLimit)
            : sourceName;
        return COPY_NAME_PREFIX + boundedSourceName;
    }

    /**
     * Generates a copy name that does not collide with an existing scenario name in the target
     * version. Falls back to numbered suffixes (e.g. "Copy of X (2)") when the base name is taken,
     * keeping the unique constraint on (lift_system_version_id, name) satisfied.
     */
    private String uniqueCopyName(String sourceName, Long targetLiftSystemVersionId) {
        String baseName = copiedScenarioName(sourceName);
        if (!scenarioRepository.existsByLiftSystemVersionIdAndName(targetLiftSystemVersionId, baseName)) {
            return baseName;
        }
        for (int suffix = 2; suffix <= MAX_COPY_NAME_ATTEMPTS; suffix++) {
            String candidate = withNumericSuffix(baseName, suffix);
            if (!scenarioRepository.existsByLiftSystemVersionIdAndName(targetLiftSystemVersionId, candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
            "Unable to generate a unique name for the copied scenario; "
                + "please rename existing copies and try again"
        );
    }

    private String withNumericSuffix(String baseName, int suffix) {
        String suffixText = " (" + suffix + ")";
        int baseLimit = SCENARIO_NAME_MAX_LENGTH - suffixText.length();
        String boundedBase = baseName.length() > baseLimit
            ? baseName.substring(0, baseLimit)
            : baseName;
        return boundedBase + suffixText;
    }

    private String duplicateNameMessage(String name) {
        return "A scenario named '" + name + "' already exists for this lift system version";
    }

    private String serializeScenarioJson(JsonNode scenarioJson) {
        try {
            return objectMapper.writeValueAsString(scenarioJson);
        } catch (JacksonException e) {
            throw new IllegalArgumentException("Scenario JSON could not be serialized");
        }
    }

    private ScenarioResponse toResponse(Scenario scenario) {
        try {
            JsonNode scenarioJson = parseStoredScenarioJson(scenario);

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
                runRepository.countByScenarioId(scenario.getId()),
                scenario.getCreatedAt(),
                scenario.getUpdatedAt()
            );
        } catch (JacksonException e) {
            throw new IllegalStateException("Stored scenario JSON could not be parsed", e);
        }
    }
}
