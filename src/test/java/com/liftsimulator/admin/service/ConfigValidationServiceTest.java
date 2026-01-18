package com.liftsimulator.admin.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftsimulator.admin.dto.ConfigValidationResponse;
import com.liftsimulator.admin.dto.ValidationIssue;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for ConfigValidationService.
 */
public class ConfigValidationServiceTest {

    private ConfigValidationService validationService;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        // Configure ObjectMapper to match production settings - fail on unknown properties
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        validationService = new ConfigValidationService(objectMapper, validator);
    }

    @Test
    public void testValidate_ValidConfig() {
        String validConfig = """
            {
                "floors": 10,
                "lifts": 2,
                "travelTicksPerFloor": 1,
                "doorTransitionTicks": 2,
                "doorDwellTicks": 3,
                "doorReopenWindowTicks": 2,
                "homeFloor": 0,
                "idleTimeoutTicks": 5,
                "controllerStrategy": "NEAREST_REQUEST_ROUTING",
                "idleParkingMode": "PARK_TO_HOME_FLOOR"
            }
            """;

        ConfigValidationResponse response = validationService.validate(validConfig);

        assertTrue(response.valid());
        assertFalse(response.hasErrors());
        assertEquals(0, response.errors().size());
    }

    @Test
    public void testValidate_InvalidJson() {
        String invalidJson = "{ invalid json }";

        ConfigValidationResponse response = validationService.validate(invalidJson);

        assertFalse(response.valid());
        assertTrue(response.hasErrors());
        assertEquals(1, response.errors().size());
        assertTrue(response.errors().get(0).message().contains("Invalid JSON format"));
    }

    @Test
    public void testValidate_MissingRequiredFields() {
        String configWithMissingFields = """
            {
                "floors": 10
            }
            """;

        ConfigValidationResponse response = validationService.validate(configWithMissingFields);

        assertFalse(response.valid());
        assertTrue(response.hasErrors());
        assertTrue(response.errors().size() > 0);
    }

    @Test
    public void testValidate_NegativeFloors() {
        String config = """
            {
                "floors": 1,
                "lifts": 2,
                "travelTicksPerFloor": 1,
                "doorTransitionTicks": 2,
                "doorDwellTicks": 3,
                "doorReopenWindowTicks": 2,
                "homeFloor": 0,
                "idleTimeoutTicks": 5,
                "controllerStrategy": "NEAREST_REQUEST_ROUTING",
                "idleParkingMode": "PARK_TO_HOME_FLOOR"
            }
            """;

        ConfigValidationResponse response = validationService.validate(config);

        assertFalse(response.valid());
        assertTrue(response.hasErrors());
        boolean hasFloorsError = response.errors().stream()
            .anyMatch(issue -> issue.field().equals("floors") && issue.message().contains("at least 2"));
        assertTrue(hasFloorsError);
    }

    @Test
    public void testValidate_NegativeLifts() {
        String config = """
            {
                "floors": 10,
                "lifts": 0,
                "travelTicksPerFloor": 1,
                "doorTransitionTicks": 2,
                "doorDwellTicks": 3,
                "doorReopenWindowTicks": 2,
                "homeFloor": 0,
                "idleTimeoutTicks": 5,
                "controllerStrategy": "NEAREST_REQUEST_ROUTING",
                "idleParkingMode": "PARK_TO_HOME_FLOOR"
            }
            """;

        ConfigValidationResponse response = validationService.validate(config);

        assertFalse(response.valid());
        assertTrue(response.hasErrors());
        boolean hasLiftsError = response.errors().stream()
            .anyMatch(issue -> issue.field().equals("lifts") && issue.message().contains("at least 1"));
        assertTrue(hasLiftsError);
    }

    @Test
    public void testValidate_DoorReopenWindowExceedsTransition() {
        String config = """
            {
                "floors": 10,
                "lifts": 2,
                "travelTicksPerFloor": 1,
                "doorTransitionTicks": 2,
                "doorDwellTicks": 3,
                "doorReopenWindowTicks": 5,
                "homeFloor": 0,
                "idleTimeoutTicks": 5,
                "controllerStrategy": "NEAREST_REQUEST_ROUTING",
                "idleParkingMode": "PARK_TO_HOME_FLOOR"
            }
            """;

        ConfigValidationResponse response = validationService.validate(config);

        assertFalse(response.valid());
        assertTrue(response.hasErrors());
        boolean hasDoorReopenError = response.errors().stream()
            .anyMatch(issue -> issue.field().equals("doorReopenWindowTicks")
                && issue.message().contains("must not exceed door transition ticks"));
        assertTrue(hasDoorReopenError);
    }

    @Test
    public void testValidate_HomeFloorOutOfRange() {
        String config = """
            {
                "floors": 10,
                "lifts": 2,
                "travelTicksPerFloor": 1,
                "doorTransitionTicks": 2,
                "doorDwellTicks": 3,
                "doorReopenWindowTicks": 2,
                "homeFloor": 15,
                "idleTimeoutTicks": 5,
                "controllerStrategy": "NEAREST_REQUEST_ROUTING",
                "idleParkingMode": "PARK_TO_HOME_FLOOR"
            }
            """;

        ConfigValidationResponse response = validationService.validate(config);

        assertFalse(response.valid());
        assertTrue(response.hasErrors());
        boolean hasHomeFloorError = response.errors().stream()
            .anyMatch(issue -> issue.field().equals("homeFloor")
                && issue.message().contains("within valid floor range"));
        assertTrue(hasHomeFloorError);
    }

    @Test
    public void testValidate_InvalidControllerStrategy() {
        String config = """
            {
                "floors": 10,
                "lifts": 2,
                "travelTicksPerFloor": 1,
                "doorTransitionTicks": 2,
                "doorDwellTicks": 3,
                "doorReopenWindowTicks": 2,
                "homeFloor": 0,
                "idleTimeoutTicks": 5,
                "controllerStrategy": "INVALID_STRATEGY",
                "idleParkingMode": "PARK_TO_HOME_FLOOR"
            }
            """;

        ConfigValidationResponse response = validationService.validate(config);

        assertFalse(response.valid());
        assertTrue(response.hasErrors());
    }

    @Test
    public void testValidate_WarningForLowIdleTimeout() {
        String config = """
            {
                "floors": 10,
                "lifts": 2,
                "travelTicksPerFloor": 1,
                "doorTransitionTicks": 2,
                "doorDwellTicks": 3,
                "doorReopenWindowTicks": 2,
                "homeFloor": 0,
                "idleTimeoutTicks": 1,
                "controllerStrategy": "NEAREST_REQUEST_ROUTING",
                "idleParkingMode": "PARK_TO_HOME_FLOOR"
            }
            """;

        ConfigValidationResponse response = validationService.validate(config);

        assertTrue(response.valid());
        assertTrue(response.hasWarnings());
        boolean hasIdleTimeoutWarning = response.warnings().stream()
            .anyMatch(issue -> issue.field().equals("idleTimeoutTicks")
                && issue.severity() == ValidationIssue.Severity.WARNING);
        assertTrue(hasIdleTimeoutWarning);
    }

    @Test
    public void testValidate_WarningForLowDoorDwellTicks() {
        String config = """
            {
                "floors": 10,
                "lifts": 2,
                "travelTicksPerFloor": 1,
                "doorTransitionTicks": 2,
                "doorDwellTicks": 1,
                "doorReopenWindowTicks": 2,
                "homeFloor": 0,
                "idleTimeoutTicks": 5,
                "controllerStrategy": "NEAREST_REQUEST_ROUTING",
                "idleParkingMode": "PARK_TO_HOME_FLOOR"
            }
            """;

        ConfigValidationResponse response = validationService.validate(config);

        assertTrue(response.valid());
        assertTrue(response.hasWarnings());
        boolean hasDoorDwellWarning = response.warnings().stream()
            .anyMatch(issue -> issue.field().equals("doorDwellTicks")
                && issue.severity() == ValidationIssue.Severity.WARNING);
        assertTrue(hasDoorDwellWarning);
    }

    @Test
    public void testValidate_WarningForMoreLiftsThanFloors() {
        String config = """
            {
                "floors": 5,
                "lifts": 10,
                "travelTicksPerFloor": 1,
                "doorTransitionTicks": 2,
                "doorDwellTicks": 3,
                "doorReopenWindowTicks": 2,
                "homeFloor": 0,
                "idleTimeoutTicks": 5,
                "controllerStrategy": "NEAREST_REQUEST_ROUTING",
                "idleParkingMode": "PARK_TO_HOME_FLOOR"
            }
            """;

        ConfigValidationResponse response = validationService.validate(config);

        assertTrue(response.valid());
        assertTrue(response.hasWarnings());
        boolean hasLiftsWarning = response.warnings().stream()
            .anyMatch(issue -> issue.field().equals("lifts")
                && issue.severity() == ValidationIssue.Severity.WARNING);
        assertTrue(hasLiftsWarning);
    }

    @Test
    public void testValidate_WarningForZeroDoorReopenWindow() {
        String config = """
            {
                "floors": 10,
                "lifts": 2,
                "travelTicksPerFloor": 1,
                "doorTransitionTicks": 2,
                "doorDwellTicks": 3,
                "doorReopenWindowTicks": 0,
                "homeFloor": 0,
                "idleTimeoutTicks": 5,
                "controllerStrategy": "NEAREST_REQUEST_ROUTING",
                "idleParkingMode": "PARK_TO_HOME_FLOOR"
            }
            """;

        ConfigValidationResponse response = validationService.validate(config);

        assertTrue(response.valid());
        assertTrue(response.hasWarnings());
        boolean hasDoorReopenWarning = response.warnings().stream()
            .anyMatch(issue -> issue.field().equals("doorReopenWindowTicks")
                && issue.severity() == ValidationIssue.Severity.WARNING);
        assertTrue(hasDoorReopenWarning);
    }

    @Test
    public void testValidate_MultipleErrors() {
        String config = """
            {
                "floors": 1,
                "lifts": 0,
                "travelTicksPerFloor": 0,
                "doorTransitionTicks": 0,
                "doorDwellTicks": 0,
                "doorReopenWindowTicks": 0,
                "homeFloor": 0,
                "idleTimeoutTicks": 0,
                "controllerStrategy": "NEAREST_REQUEST_ROUTING",
                "idleParkingMode": "PARK_TO_HOME_FLOOR"
            }
            """;

        ConfigValidationResponse response = validationService.validate(config);

        assertFalse(response.valid());
        assertTrue(response.hasErrors());
        assertTrue(response.errors().size() > 1);
    }

    @Test
    public void testValidate_ConfigValidationResponseStructure() {
        String validConfig = """
            {
                "floors": 10,
                "lifts": 2,
                "travelTicksPerFloor": 1,
                "doorTransitionTicks": 2,
                "doorDwellTicks": 3,
                "doorReopenWindowTicks": 2,
                "homeFloor": 0,
                "idleTimeoutTicks": 5,
                "controllerStrategy": "NEAREST_REQUEST_ROUTING",
                "idleParkingMode": "PARK_TO_HOME_FLOOR"
            }
            """;

        ConfigValidationResponse response = validationService.validate(validConfig);

        assertNotNull(response);
        assertNotNull(response.errors());
        assertNotNull(response.warnings());
        assertTrue(response.valid());
    }

    @Test
    public void testValidate_UnknownFieldRejected() {
        String configWithUnknownField = """
            {
                "floors": 10,
                "lifts": 2,
                "travelTicksPerFloor": 1,
                "doorTransitionTicks": 2,
                "doorDwellTicks": 3,
                "doorReopenWindowTicks": 2,
                "homeFloor": 0,
                "idleTimeoutTicks": 5,
                "controllerStrategy": "NEAREST_REQUEST_ROUTING",
                "idleParkingMode": "PARK_TO_HOME_FLOOR",
                "unknownField": "someValue"
            }
            """;

        ConfigValidationResponse response = validationService.validate(configWithUnknownField);

        assertFalse(response.valid());
        assertTrue(response.hasErrors());
        assertEquals(1, response.errors().size());
        ValidationIssue error = response.errors().get(0);
        assertEquals("unknownField", error.field());
        assertTrue(error.message().contains("Unknown property"));
        assertTrue(error.message().contains("unknownField"));
    }

    @Test
    public void testValidate_TypoInFieldNameRejected() {
        String configWithTypo = """
            {
                "floor": 10,
                "lifts": 2,
                "travelTicksPerFloor": 1,
                "doorTransitionTicks": 2,
                "doorDwellTicks": 3,
                "doorReopenWindowTicks": 2,
                "homeFloor": 0,
                "idleTimeoutTicks": 5,
                "controllerStrategy": "NEAREST_REQUEST_ROUTING",
                "idleParkingMode": "PARK_TO_HOME_FLOOR"
            }
            """;

        ConfigValidationResponse response = validationService.validate(configWithTypo);

        assertFalse(response.valid());
        assertTrue(response.hasErrors());
        // Should have error for unknown property "floor" (typo of "floors")
        boolean hasUnknownPropertyError = response.errors().stream()
            .anyMatch(issue -> issue.field().equals("floor") && issue.message().contains("Unknown property"));
        assertTrue(hasUnknownPropertyError);
    }

    @Test
    public void testValidate_MultipleUnknownFieldsRejected() {
        String configWithMultipleUnknownFields = """
            {
                "floors": 10,
                "lifts": 2,
                "travelTicksPerFloor": 1,
                "doorTransitionTicks": 2,
                "doorDwellTicks": 3,
                "doorReopenWindowTicks": 2,
                "homeFloor": 0,
                "idleTimeoutTicks": 5,
                "controllerStrategy": "NEAREST_REQUEST_ROUTING",
                "idleParkingMode": "PARK_TO_HOME_FLOOR",
                "extraField1": "value1",
                "extraField2": "value2"
            }
            """;

        ConfigValidationResponse response = validationService.validate(configWithMultipleUnknownFields);

        assertFalse(response.valid());
        assertTrue(response.hasErrors());
        // Jackson will report the first unknown property it encounters
        assertTrue(response.errors().size() >= 1);
        ValidationIssue error = response.errors().get(0);
        assertTrue(error.message().contains("Unknown property"));
    }

    @Test
    public void testValidate_UnknownFieldWithValidData() {
        // Ensure that even when all known fields are valid, unknown fields cause rejection
        String config = """
            {
                "floors": 10,
                "lifts": 2,
                "travelTicksPerFloor": 5,
                "doorTransitionTicks": 3,
                "doorDwellTicks": 4,
                "doorReopenWindowTicks": 2,
                "homeFloor": 5,
                "idleTimeoutTicks": 10,
                "controllerStrategy": "DIRECTIONAL_SCAN",
                "idleParkingMode": "STAY_AT_CURRENT_FLOOR",
                "newFeature": true
            }
            """;

        ConfigValidationResponse response = validationService.validate(config);

        assertFalse(response.valid());
        assertTrue(response.hasErrors());
        boolean hasUnknownPropertyError = response.errors().stream()
            .anyMatch(issue -> issue.field().equals("newFeature") && issue.message().contains("Unknown property"));
        assertTrue(hasUnknownPropertyError);
    }

    @Test
    public void testValidate_NonNumericValueForFloors() {
        String config = """
            {
                "floors": "A",
                "lifts": 2,
                "travelTicksPerFloor": 1,
                "doorTransitionTicks": 2,
                "doorDwellTicks": 3,
                "doorReopenWindowTicks": 2,
                "homeFloor": 0,
                "idleTimeoutTicks": 5,
                "controllerStrategy": "NEAREST_REQUEST_ROUTING",
                "idleParkingMode": "PARK_TO_HOME_FLOOR"
            }
            """;

        ConfigValidationResponse response = validationService.validate(config);

        assertFalse(response.valid());
        assertTrue(response.hasErrors());
        assertEquals(1, response.errors().size());
        ValidationIssue error = response.errors().get(0);
        assertEquals("floors", error.field());
        assertTrue(error.message().contains("numeric value") || error.message().contains("must be a number"));
    }

    @Test
    public void testValidate_NonNumericValueForLifts() {
        String config = """
            {
                "floors": 10,
                "lifts": "abc",
                "travelTicksPerFloor": 1,
                "doorTransitionTicks": 2,
                "doorDwellTicks": 3,
                "doorReopenWindowTicks": 2,
                "homeFloor": 0,
                "idleTimeoutTicks": 5,
                "controllerStrategy": "NEAREST_REQUEST_ROUTING",
                "idleParkingMode": "PARK_TO_HOME_FLOOR"
            }
            """;

        ConfigValidationResponse response = validationService.validate(config);

        assertFalse(response.valid());
        assertTrue(response.hasErrors());
        boolean hasTypeError = response.errors().stream()
            .anyMatch(issue -> issue.field().equals("lifts")
                && (issue.message().contains("numeric value") || issue.message().contains("must be a number")));
        assertTrue(hasTypeError);
    }

    @Test
    public void testValidate_MultipleNonNumericValues() {
        String config = """
            {
                "floors": "X",
                "lifts": "Y",
                "travelTicksPerFloor": "Z",
                "doorTransitionTicks": 2,
                "doorDwellTicks": 3,
                "doorReopenWindowTicks": 2,
                "homeFloor": 0,
                "idleTimeoutTicks": 5,
                "controllerStrategy": "NEAREST_REQUEST_ROUTING",
                "idleParkingMode": "PARK_TO_HOME_FLOOR"
            }
            """;

        ConfigValidationResponse response = validationService.validate(config);

        assertFalse(response.valid());
        assertTrue(response.hasErrors());
        // Jackson will report the first type mismatch it encounters
        assertTrue(response.errors().size() >= 1);
        ValidationIssue error = response.errors().get(0);
        assertTrue(error.message().contains("numeric value") || error.message().contains("must be a number"));
    }

    @Test
    public void testValidate_BooleanValueForNumericField() {
        String config = """
            {
                "floors": true,
                "lifts": 2,
                "travelTicksPerFloor": 1,
                "doorTransitionTicks": 2,
                "doorDwellTicks": 3,
                "doorReopenWindowTicks": 2,
                "homeFloor": 0,
                "idleTimeoutTicks": 5,
                "controllerStrategy": "NEAREST_REQUEST_ROUTING",
                "idleParkingMode": "PARK_TO_HOME_FLOOR"
            }
            """;

        ConfigValidationResponse response = validationService.validate(config);

        assertFalse(response.valid());
        assertTrue(response.hasErrors());
        boolean hasTypeError = response.errors().stream()
            .anyMatch(issue -> issue.field().equals("floors")
                && (issue.message().contains("numeric value") || issue.message().contains("must be a number")));
        assertTrue(hasTypeError);
    }
}
