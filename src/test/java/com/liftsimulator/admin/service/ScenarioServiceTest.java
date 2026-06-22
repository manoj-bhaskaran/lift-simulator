package com.liftsimulator.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftsimulator.admin.dto.ScenarioRequest;
import com.liftsimulator.admin.dto.ScenarioResponse;
import com.liftsimulator.admin.dto.ScenarioValidationResponse;
import com.liftsimulator.admin.dto.ValidationIssue;
import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.entity.Scenario;
import com.liftsimulator.admin.repository.LiftSystemVersionRepository;
import com.liftsimulator.admin.repository.ScenarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ScenarioService.
 */
@ExtendWith(MockitoExtension.class)
public class ScenarioServiceTest {

    private static final String CONFIG_JSON = """
        {
            "minFloor": 0,
            "maxFloor": 9,
            "lifts": 1,
            "travelTicksPerFloor": 1,
            "doorTransitionTicks": 2,
            "doorDwellTicks": 3,
            "doorReopenWindowTicks": 1,
            "homeFloor": 0,
            "idleTimeoutTicks": 5,
            "controllerStrategy": "NEAREST_REQUEST_ROUTING",
            "idleParkingMode": "PARK_TO_HOME_FLOOR"
        }
        """;

    @Mock
    private ScenarioRepository scenarioRepository;

    @Mock
    private LiftSystemVersionRepository versionRepository;

    @Mock
    private ScenarioValidationService scenarioValidationService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ScenarioService scenarioService;
    private LiftSystemVersion version;

    @BeforeEach
    public void setUp() {
        scenarioService = new ScenarioService(
                scenarioRepository,
                versionRepository,
                scenarioValidationService,
                objectMapper
        );
        version = version(20L);
    }

    @Test
    public void createScenarioValidatesSerializesAndReturnsVersionInfo() throws Exception {
        ScenarioRequest request = new ScenarioRequest("Morning rush", json(validScenario()), 20L);
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(scenarioValidationService.validate(request.scenarioJson(), 20L))
                .thenReturn(validValidationResponse());
        when(scenarioRepository.save(any(Scenario.class))).thenAnswer(invocation -> {
            Scenario saved = invocation.getArgument(0);
            saved.setId(30L);
            saved.setCreatedAt(OffsetDateTime.parse("2026-06-12T10:00:00Z"));
            saved.setUpdatedAt(OffsetDateTime.parse("2026-06-12T10:00:00Z"));
            return saved;
        });

        ScenarioResponse response = scenarioService.createScenario(request);

        assertEquals(30L, response.id());
        assertEquals("Morning rush", response.name());
        assertEquals(2, response.scenarioJson().at("/passengerFlows/0/passengers").asInt());
        assertEquals(20L, response.liftSystemVersionId());
        assertNotNull(response.versionInfo());
        assertEquals(10L, response.versionInfo().liftSystemId());
        assertEquals("test-system", response.versionInfo().systemKey());
        assertEquals(1, response.versionInfo().versionNumber());
        assertEquals(0, response.versionInfo().minFloor());
        assertEquals(9, response.versionInfo().maxFloor());

        ArgumentCaptor<Scenario> scenarioCaptor = ArgumentCaptor.forClass(Scenario.class);
        verify(scenarioRepository).save(scenarioCaptor.capture());
        Scenario savedScenario = scenarioCaptor.getValue();
        assertEquals("Morning rush", savedScenario.getName());
        assertEquals(20L, savedScenario.getLiftSystemVersion().getId());
        assertEquals(request.scenarioJson(), objectMapper.readTree(savedScenario.getScenarioJson()));
        verify(scenarioValidationService).validate(request.scenarioJson(), 20L);
    }

    @Test
    public void createScenarioThrowsValidationExceptionAndDoesNotSaveWhenValidationFails() throws Exception {
        ScenarioRequest request = new ScenarioRequest("Invalid", json(validScenario()), 20L);
        ScenarioValidationResponse validationResponse = new ScenarioValidationResponse(
                false,
                List.of(new ValidationIssue(
                        "passengerFlows[0].destinationFloor",
                        "Destination floor 10 is outside the lift system's floor range [0, 9]",
                        ValidationIssue.Severity.ERROR
                )),
                List.of()
        );
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(scenarioValidationService.validate(request.scenarioJson(), 20L)).thenReturn(validationResponse);

        ScenarioValidationException exception = assertThrows(
                ScenarioValidationException.class,
                () -> scenarioService.createScenario(request)
        );

        assertEquals("Scenario validation failed", exception.getMessage());
        assertEquals(validationResponse, exception.getValidationResponse());
        verify(scenarioRepository, never()).save(any(Scenario.class));
    }

    @Test
    public void createScenarioThrowsConflictAndDoesNotSaveWhenDuplicateNameExists() throws Exception {
        ScenarioRequest request = new ScenarioRequest("Morning rush", json(validScenario()), 20L);
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(scenarioRepository.existsByLiftSystemVersionIdAndName(20L, "Morning rush")).thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> scenarioService.createScenario(request)
        );

        assertTrue(exception.getMessage().contains("Morning rush"));
        assertTrue(exception.getMessage().contains("already exists"));
        verify(scenarioValidationService, never()).validate(any(), any());
        verify(scenarioRepository, never()).save(any(Scenario.class));
    }

    @Test
    public void updateScenarioThrowsConflictWhenAnotherScenarioHasSameName() throws Exception {
        Scenario existing = new Scenario("Original", validScenario(), version);
        existing.setId(30L);
        ScenarioRequest request = new ScenarioRequest("Taken name", json(updatedScenario()), 20L);
        when(scenarioRepository.findById(30L)).thenReturn(Optional.of(existing));
        when(versionRepository.findById(20L)).thenReturn(Optional.of(version));
        when(scenarioRepository.existsByLiftSystemVersionIdAndNameAndIdNot(20L, "Taken name", 30L))
                .thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> scenarioService.updateScenario(30L, request)
        );

        assertTrue(exception.getMessage().contains("Taken name"));
        verify(scenarioValidationService, never()).validate(any(), any());
        verify(scenarioRepository, never()).save(any(Scenario.class));
    }

    @Test
    public void copyScenarioGeneratesNumberedSuffixWhenBaseCopyNameIsTaken() throws Exception {
        Scenario source = new Scenario("Morning rush", validScenario(), version);
        source.setId(30L);
        LiftSystemVersion targetVersion = version(22L);
        when(scenarioRepository.findById(30L)).thenReturn(Optional.of(source));
        when(versionRepository.findById(22L)).thenReturn(Optional.of(targetVersion));
        when(scenarioValidationService.validate(objectMapper.readTree(validScenario()), 22L))
                .thenReturn(validValidationResponse());
        when(scenarioRepository.existsByLiftSystemVersionIdAndName(22L, "Copy of Morning rush"))
                .thenReturn(true);
        when(scenarioRepository.existsByLiftSystemVersionIdAndName(22L, "Copy of Morning rush (2)"))
                .thenReturn(false);
        when(scenarioRepository.save(any(Scenario.class))).thenAnswer(invocation -> {
            Scenario saved = invocation.getArgument(0);
            saved.setId(31L);
            return saved;
        });

        ScenarioResponse response = scenarioService.copyScenario(30L, 22L);

        assertEquals("Copy of Morning rush (2)", response.name());
        ArgumentCaptor<Scenario> scenarioCaptor = ArgumentCaptor.forClass(Scenario.class);
        verify(scenarioRepository).save(scenarioCaptor.capture());
        assertEquals("Copy of Morning rush (2)", scenarioCaptor.getValue().getName());
    }

    @Test
    public void updateScenarioValidatesMutatesExistingScenarioAndReturnsUpdatedResponse() throws Exception {
        Scenario existing = new Scenario("Original", validScenario(), version);
        existing.setId(30L);
        existing.setCreatedAt(OffsetDateTime.parse("2026-06-12T09:00:00Z"));
        existing.setUpdatedAt(OffsetDateTime.parse("2026-06-12T09:00:00Z"));
        LiftSystemVersion replacementVersion = version(21L);
        ScenarioRequest request = new ScenarioRequest("Updated", json(updatedScenario()), 21L);
        when(scenarioRepository.findById(30L)).thenReturn(Optional.of(existing));
        when(versionRepository.findById(21L)).thenReturn(Optional.of(replacementVersion));
        when(scenarioValidationService.validate(request.scenarioJson(), 21L))
                .thenReturn(validValidationResponse());
        when(scenarioRepository.save(any(Scenario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScenarioResponse response = scenarioService.updateScenario(30L, request);

        assertEquals(30L, response.id());
        assertEquals("Updated", response.name());
        assertEquals(21L, response.liftSystemVersionId());
        assertEquals(180, response.scenarioJson().get("durationTicks").asInt());
        assertEquals("Updated", existing.getName());
        assertEquals(21L, existing.getLiftSystemVersion().getId());
        assertEquals(request.scenarioJson(), objectMapper.readTree(existing.getScenarioJson()));
        verify(scenarioValidationService).validate(request.scenarioJson(), 21L);
        verify(scenarioRepository).save(existing);
    }


    @Test
    public void copyScenarioValidatesAgainstTargetVersionAndCreatesNewRecord() throws Exception {
        Scenario source = new Scenario("Morning rush", validScenario(), version);
        source.setId(30L);
        LiftSystemVersion targetVersion = version(22L);
        ScenarioValidationResponse validationResponse = validValidationResponse();
        when(scenarioRepository.findById(30L)).thenReturn(Optional.of(source));
        when(versionRepository.findById(22L)).thenReturn(Optional.of(targetVersion));
        when(scenarioValidationService.validate(objectMapper.readTree(validScenario()), 22L))
                .thenReturn(validationResponse);
        when(scenarioRepository.save(any(Scenario.class))).thenAnswer(invocation -> {
            Scenario saved = invocation.getArgument(0);
            saved.setId(31L);
            saved.setCreatedAt(OffsetDateTime.parse("2026-06-14T10:00:00Z"));
            saved.setUpdatedAt(OffsetDateTime.parse("2026-06-14T10:00:00Z"));
            return saved;
        });

        ScenarioResponse response = scenarioService.copyScenario(30L, 22L);

        assertEquals(31L, response.id());
        assertEquals("Copy of Morning rush", response.name());
        assertEquals(22L, response.liftSystemVersionId());
        assertEquals(4, response.scenarioJson().at("/passengerFlows/0/destinationFloor").asInt());
        ArgumentCaptor<Scenario> scenarioCaptor = ArgumentCaptor.forClass(Scenario.class);
        verify(scenarioRepository).save(scenarioCaptor.capture());
        Scenario copied = scenarioCaptor.getValue();
        assertEquals("Copy of Morning rush", copied.getName());
        assertEquals(22L, copied.getLiftSystemVersion().getId());
        assertEquals(objectMapper.readTree(validScenario()), objectMapper.readTree(copied.getScenarioJson()));
    }

    @Test
    public void copyScenarioTruncatesGeneratedNameToColumnLimit() throws Exception {
        String sourceName = "A".repeat(200);
        Scenario source = new Scenario(sourceName, validScenario(), version);
        source.setId(30L);
        LiftSystemVersion targetVersion = version(22L);
        when(scenarioRepository.findById(30L)).thenReturn(Optional.of(source));
        when(versionRepository.findById(22L)).thenReturn(Optional.of(targetVersion));
        when(scenarioValidationService.validate(objectMapper.readTree(validScenario()), 22L))
                .thenReturn(validValidationResponse());
        when(scenarioRepository.save(any(Scenario.class))).thenAnswer(invocation -> {
            Scenario saved = invocation.getArgument(0);
            saved.setId(31L);
            return saved;
        });

        scenarioService.copyScenario(30L, 22L);

        ArgumentCaptor<Scenario> scenarioCaptor = ArgumentCaptor.forClass(Scenario.class);
        verify(scenarioRepository).save(scenarioCaptor.capture());
        String copiedName = scenarioCaptor.getValue().getName();
        assertEquals(200, copiedName.length());
        assertTrue(copiedName.startsWith("Copy of "));
    }

    @Test
    public void copyScenarioDoesNotSaveWhenTargetValidationFails() throws Exception {
        Scenario source = new Scenario("Invalid for target", validScenario(), version);
        source.setId(30L);
        LiftSystemVersion targetVersion = version(22L);
        ScenarioValidationResponse validationResponse = new ScenarioValidationResponse(
                false,
                List.of(new ValidationIssue(
                        "passengerFlows[0].destinationFloor",
                        "Destination floor 4 is outside the lift system's floor range [0, 3]",
                        ValidationIssue.Severity.ERROR
                )),
                List.of()
        );
        when(scenarioRepository.findById(30L)).thenReturn(Optional.of(source));
        when(versionRepository.findById(22L)).thenReturn(Optional.of(targetVersion));
        when(scenarioValidationService.validate(objectMapper.readTree(validScenario()), 22L))
                .thenReturn(validationResponse);

        ScenarioValidationException exception = assertThrows(
                ScenarioValidationException.class,
                () -> scenarioService.copyScenario(30L, 22L)
        );

        assertEquals("Scenario validation failed", exception.getMessage());
        assertEquals(validationResponse, exception.getValidationResponse());
        verify(scenarioRepository, never()).save(any(Scenario.class));
    }

    @Test
    public void updateScenarioThrowsNotFoundWhenScenarioIsMissing() throws Exception {
        ScenarioRequest request = new ScenarioRequest("Missing", json(validScenario()), 20L);
        when(scenarioRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> scenarioService.updateScenario(99L, request)
        );

        assertEquals("Scenario not found with id: 99", exception.getMessage());
        verify(versionRepository, never()).findById(any());
        verify(scenarioRepository, never()).save(any(Scenario.class));
    }

    @Test
    public void getScenarioThrowsIllegalStateWhenStoredScenarioJsonCannotBeParsed() {
        Scenario invalid = new Scenario("Corrupt", "{ invalid json }", version);
        invalid.setId(30L);
        when(scenarioRepository.findById(30L)).thenReturn(Optional.of(invalid));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> scenarioService.getScenario(30L)
        );

        assertTrue(exception.getMessage().contains("Stored scenario JSON could not be parsed"));
    }

    private JsonNode json(String payload) throws Exception {
        return objectMapper.readTree(payload);
    }

    private ScenarioValidationResponse validValidationResponse() {
        return new ScenarioValidationResponse(true, List.of(), List.of());
    }

    private LiftSystemVersion version(Long id) {
        LiftSystem liftSystem = new LiftSystem("test-system", "Test System", "Test lift system");
        liftSystem.setId(10L);
        LiftSystemVersion liftSystemVersion = new LiftSystemVersion();
        liftSystemVersion.setId(id);
        liftSystemVersion.setVersionNumber(1);
        liftSystemVersion.setLiftSystem(liftSystem);
        liftSystemVersion.setConfig(CONFIG_JSON);
        return liftSystemVersion;
    }

    private String validScenario() {
        return """
            {
                "durationTicks": 120,
                "seed": 42,
                "passengerFlows": [
                    {
                        "startTick": 0,
                        "originFloor": 0,
                        "destinationFloor": 4,
                        "passengers": 2
                    }
                ]
            }
            """;
    }

    private String updatedScenario() {
        return """
            {
                "durationTicks": 180,
                "passengerFlows": [
                    {
                        "startTick": 10,
                        "originFloor": 3,
                        "destinationFloor": 8,
                        "passengers": 1
                    }
                ]
            }
            """;
    }
}
