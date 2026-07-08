package com.liftsimulator.admin.service;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.liftsimulator.admin.dto.ScenarioValidationResponse;
import com.liftsimulator.admin.dto.ValidationIssue;
import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.repository.LiftSystemVersionRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for ScenarioValidationService.
 */
@ExtendWith(MockitoExtension.class)
public class ScenarioValidationServiceTest {

    private static final String VALID_CONFIG = """
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
    private LiftSystemVersionRepository versionRepository;

    private ObjectMapper objectMapper;
    private ScenarioValidationService validationService;

    @BeforeEach
    public void setUp() {
        objectMapper = tools.jackson.databind.json.JsonMapper.builder().build();
        // Match production strict-schema behaviour so unknown scenario fields become validation errors.
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        validationService = new ScenarioValidationService(objectMapper, validator, versionRepository);
    }

    @Test
    public void validateScenarioWithVersionAcceptsValidPassengerFlowWithinFloorRange() throws Exception {
        when(versionRepository.findById(10L)).thenReturn(Optional.of(version(VALID_CONFIG)));

        ScenarioValidationResponse response = validationService.validate(json(validScenario()), 10L);

        assertTrue(response.valid());
        assertFalse(response.hasErrors());
        assertEquals(0, response.warnings().size());
        verify(versionRepository).findById(10L);
    }

    @Test
    public void validateScenarioReportsBeanAndDomainValidationErrors() throws Exception {
        String scenario = """
            {
                "durationTicks": 5,
                "seed": -1,
                "passengerFlows": [
                    {
                        "startTick": 5,
                        "originFloor": 2,
                        "destinationFloor": 2,
                        "passengers": 0
                    }
                ]
            }
            """;

        ScenarioValidationResponse response = validationService.validate(json(scenario));

        assertFalse(response.valid());
        assertTrue(response.hasErrors());
        assertHasError(response, "seed", "seed must be 0 or greater");
        assertHasError(response, "passengerFlows[0].startTick", "startTick must be less than durationTicks (5)");
        assertHasError(response, "passengerFlows[0].destinationFloor",
                "destinationFloor must be different from originFloor");
        assertTrue(response.errors().stream()
                .anyMatch(issue -> issue.message().contains("passengers must be at least 1")));
        verifyNoInteractions(versionRepository);
    }

    @Test
    public void validateScenarioRejectsUnknownProperties() throws Exception {
        String scenario = """
            {
                "durationTicks": 5,
                "unsupportedMode": true,
                "passengerFlows": [
                    {
                        "startTick": 0,
                        "originFloor": 0,
                        "destinationFloor": 1,
                        "passengers": 1
                    }
                ]
            }
            """;

        ScenarioValidationResponse response = validationService.validate(json(scenario));

        assertFalse(response.valid());
        assertEquals(1, response.errors().size());
        assertHasError(response, "unsupportedMode",
                "Unknown property 'unsupportedMode' is not allowed in scenario schema");
        verifyNoInteractions(versionRepository);
    }

    @Test
    public void validateScenarioWithVersionReportsOutOfRangeOriginAndDestinationFloors() throws Exception {
        String scenario = """
            {
                "durationTicks": 10,
                "passengerFlows": [
                    {
                        "startTick": 0,
                        "originFloor": -1,
                        "destinationFloor": 10,
                        "passengers": 1
                    }
                ]
            }
            """;
        when(versionRepository.findById(10L)).thenReturn(Optional.of(version(VALID_CONFIG)));

        ScenarioValidationResponse response = validationService.validate(json(scenario), 10L);

        assertFalse(response.valid());
        assertEquals(2, response.errors().size());
        assertHasError(response, "passengerFlows[0].originFloor",
                "Origin floor -1 is outside the lift system's floor range [0, 9]");
        assertHasError(response, "passengerFlows[0].destinationFloor",
                "Destination floor 10 is outside the lift system's floor range [0, 9]");
        verify(versionRepository).findById(10L);
    }

    @Test
    public void validateScenarioWithVersionReturnsBaseValidationErrorsBeforeFloorRangeChecks() throws Exception {
        String scenario = """
            {
                "durationTicks": 0,
                "passengerFlows": []
            }
            """;
        when(versionRepository.findById(10L)).thenReturn(Optional.of(version(VALID_CONFIG)));

        ScenarioValidationResponse response = validationService.validate(json(scenario), 10L);

        assertFalse(response.valid());
        assertTrue(response.errors().stream().anyMatch(issue -> issue.field().equals("durationTicks")));
        assertTrue(response.errors().stream().anyMatch(issue -> issue.field().equals("passengerFlows")));
        verify(versionRepository).findById(10L);
    }

    @Test
    public void validateScenarioWithNullVersionIdFailsFast() throws Exception {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> validationService.validate(json(validScenario()), null)
        );

        assertEquals("Lift system version ID is required for validation", exception.getMessage());
        verifyNoInteractions(versionRepository);
    }

    @Test
    public void validateScenarioWithMissingVersionThrowsNotFound() throws Exception {
        when(versionRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> validationService.validate(json(validScenario()), 99L)
        );

        assertEquals("Lift system version not found with id: 99", exception.getMessage());
        verify(versionRepository).findById(99L);
    }

    @Test
    public void validateScenarioWithInvalidStoredVersionConfigThrowsIllegalState() throws Exception {
        when(versionRepository.findById(10L)).thenReturn(Optional.of(version("{ invalid config }")));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> validationService.validate(json(validScenario()), 10L)
        );

        assertTrue(exception.getMessage().contains("Failed to parse lift system version config"));
        verify(versionRepository).findById(10L);
    }

    private JsonNode json(String payload) throws Exception {
        return objectMapper.readTree(payload);
    }

    private LiftSystemVersion version(String config) {
        LiftSystemVersion version = new LiftSystemVersion();
        version.setId(10L);
        version.setConfig(config);
        return version;
    }

    private String validScenario() {
        return """
            {
                "durationTicks": 10,
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

    private void assertHasError(ScenarioValidationResponse response, String field, String message) {
        assertTrue(response.errors().stream().anyMatch(issue -> hasIssue(issue, field, message)),
                "Expected validation error for field " + field + " with message: " + message);
    }

    private boolean hasIssue(ValidationIssue issue, String field, String message) {
        return issue.field().equals(field) && issue.message().equals(message);
    }
}
