package com.liftsimulator.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftsimulator.admin.dto.LiftConfigDTO;
import com.liftsimulator.admin.dto.PassengerFlowDTO;
import com.liftsimulator.admin.dto.ScenarioDefinitionDTO;
import com.liftsimulator.domain.ControllerStrategy;
import com.liftsimulator.domain.IdleParkingMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for BatchInputGenerator.
 */
public class BatchInputGeneratorTest {

    private BatchInputGenerator generator;
    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        objectMapper = new ObjectMapper();
        generator = new BatchInputGenerator(objectMapper);
    }

    @Test
    public void testGenerateScenarioContent_BasicFlow() throws IOException {
        LiftConfigDTO liftConfig = new LiftConfigDTO(
                0,      // minFloor
                10,     // maxFloor
                2,      // lifts
                1,      // travelTicksPerFloor
                2,      // doorTransitionTicks
                3,      // doorDwellTicks
                2,      // doorReopenWindowTicks
                0,      // homeFloor
                5,      // idleTimeoutTicks
                ControllerStrategy.NEAREST_REQUEST_ROUTING,
                IdleParkingMode.PARK_TO_HOME_FLOOR
        );

        PassengerFlowDTO flow1 = new PassengerFlowDTO(0, 0, 5, 2);
        PassengerFlowDTO flow2 = new PassengerFlowDTO(10, 7, 2, 1);

        ScenarioDefinitionDTO scenario = new ScenarioDefinitionDTO(
                100,
                Arrays.asList(flow1, flow2),
                12345
        );

        String result = generator.generateScenarioContent(liftConfig, scenario, "Test Scenario");

        assertTrue(result.contains("name: Test Scenario"));
        assertTrue(result.contains("ticks: 100"));
        assertTrue(result.contains("min_floor: 0"));
        assertTrue(result.contains("max_floor: 10"));
        assertTrue(result.contains("initial_floor: 0"));
        assertTrue(result.contains("travel_ticks_per_floor: 1"));
        assertTrue(result.contains("door_transition_ticks: 2"));
        assertTrue(result.contains("door_dwell_ticks: 3"));
        assertTrue(result.contains("door_reopen_window_ticks: 2"));
        assertTrue(result.contains("home_floor: 0"));
        assertTrue(result.contains("idle_timeout_ticks: 5"));
        assertTrue(result.contains("controller_strategy: NEAREST_REQUEST_ROUTING"));
        assertTrue(result.contains("idle_parking_mode: PARK_TO_HOME_FLOOR"));

        // Check for hall call events (2 passengers at tick 0, origin 0, going UP to 5)
        assertTrue(result.contains("0, hall_call, p1, 0, UP"));
        assertTrue(result.contains("0, hall_call, p2, 0, UP"));

        // Check for hall call event (1 passenger at tick 10, origin 7, going DOWN to 2)
        assertTrue(result.contains("10, hall_call, p3, 7, DOWN"));
    }

    @Test
    public void testGenerateScenarioContent_GoldenFile() throws IOException {
        LiftConfigDTO liftConfig = new LiftConfigDTO(
                0, 10, 1, 1, 2, 3, 2, 0, 5,
                ControllerStrategy.NEAREST_REQUEST_ROUTING,
                IdleParkingMode.PARK_TO_HOME_FLOOR
        );

        PassengerFlowDTO flow1 = new PassengerFlowDTO(0, 0, 3, 1);
        PassengerFlowDTO flow2 = new PassengerFlowDTO(5, 8, 2, 2);

        ScenarioDefinitionDTO scenario = new ScenarioDefinitionDTO(
                30,
                Arrays.asList(flow1, flow2),
                null
        );

        String result = generator.generateScenarioContent(
                liftConfig,
                scenario,
                "Golden Test Scenario"
        );

        String expected = """
                name: Golden Test Scenario
                ticks: 30
                min_floor: 0
                max_floor: 10
                initial_floor: 0
                travel_ticks_per_floor: 1
                door_transition_ticks: 2
                door_dwell_ticks: 3
                door_reopen_window_ticks: 2
                home_floor: 0
                idle_timeout_ticks: 5
                controller_strategy: NEAREST_REQUEST_ROUTING
                idle_parking_mode: PARK_TO_HOME_FLOOR

                0, hall_call, p1, 0, UP
                5, hall_call, p2, 8, DOWN
                5, hall_call, p3, 8, DOWN
                """;

        assertEquals(expected, result);
    }

    @Test
    public void testGenerateScenarioContent_DirectionalScan() throws IOException {
        LiftConfigDTO liftConfig = new LiftConfigDTO(
                -2, 20, 3, 2, 3, 4, 1, 0, 10,
                ControllerStrategy.DIRECTIONAL_SCAN,
                IdleParkingMode.STAY_AT_CURRENT_FLOOR
        );

        PassengerFlowDTO flow = new PassengerFlowDTO(0, 5, 10, 1);

        ScenarioDefinitionDTO scenario = new ScenarioDefinitionDTO(
                50,
                List.of(flow),
                null
        );

        String result = generator.generateScenarioContent(liftConfig, scenario, "Scan Test");

        assertTrue(result.contains("controller_strategy: DIRECTIONAL_SCAN"));
        assertTrue(result.contains("idle_parking_mode: STAY_AT_CURRENT_FLOOR"));
        assertTrue(result.contains("0, hall_call, p1, 5, UP"));
    }

    @Test
    public void testGenerateScenarioContent_MultiplePassengersInSameFlow() throws IOException {
        LiftConfigDTO liftConfig = new LiftConfigDTO(
                0, 10, 1, 1, 1, 1, 0, 0, 5,
                ControllerStrategy.NEAREST_REQUEST_ROUTING,
                IdleParkingMode.PARK_TO_HOME_FLOOR
        );

        PassengerFlowDTO flow = new PassengerFlowDTO(0, 0, 5, 5);

        ScenarioDefinitionDTO scenario = new ScenarioDefinitionDTO(
                20,
                List.of(flow),
                null
        );

        String result = generator.generateScenarioContent(liftConfig, scenario, "Multi Passenger");

        // Verify all 5 passengers get unique aliases
        assertTrue(result.contains("0, hall_call, p1, 0, UP"));
        assertTrue(result.contains("0, hall_call, p2, 0, UP"));
        assertTrue(result.contains("0, hall_call, p3, 0, UP"));
        assertTrue(result.contains("0, hall_call, p4, 0, UP"));
        assertTrue(result.contains("0, hall_call, p5, 0, UP"));
    }

    @Test
    public void testGenerateScenarioContent_DownwardTravel() throws IOException {
        LiftConfigDTO liftConfig = new LiftConfigDTO(
                0, 10, 1, 1, 1, 1, 0, 0, 5,
                ControllerStrategy.NEAREST_REQUEST_ROUTING,
                IdleParkingMode.PARK_TO_HOME_FLOOR
        );

        PassengerFlowDTO flow = new PassengerFlowDTO(5, 8, 3, 2);

        ScenarioDefinitionDTO scenario = new ScenarioDefinitionDTO(
                20,
                List.of(flow),
                null
        );

        String result = generator.generateScenarioContent(liftConfig, scenario, "Downward");

        assertTrue(result.contains("5, hall_call, p1, 8, DOWN"));
        assertTrue(result.contains("5, hall_call, p2, 8, DOWN"));
    }

    @Test
    public void testGenerateScenarioContent_SortedByTick() throws IOException {
        LiftConfigDTO liftConfig = new LiftConfigDTO(
                0, 10, 1, 1, 1, 1, 0, 0, 5,
                ControllerStrategy.NEAREST_REQUEST_ROUTING,
                IdleParkingMode.PARK_TO_HOME_FLOOR
        );

        // Add flows in reverse tick order
        PassengerFlowDTO flow1 = new PassengerFlowDTO(20, 0, 5, 1);
        PassengerFlowDTO flow2 = new PassengerFlowDTO(10, 3, 7, 1);
        PassengerFlowDTO flow3 = new PassengerFlowDTO(5, 8, 2, 1);

        ScenarioDefinitionDTO scenario = new ScenarioDefinitionDTO(
                30,
                Arrays.asList(flow1, flow2, flow3),
                null
        );

        String result = generator.generateScenarioContent(liftConfig, scenario, "Sorted");

        String[] lines = result.split("\n");
        int line5Index = -1;
        int line10Index = -1;
        int line20Index = -1;

        for (int i = 0; i < lines.length; i++) {
            if (lines[i].startsWith("5,")) {
                line5Index = i;
            } else if (lines[i].startsWith("10,")) {
                line10Index = i;
            } else if (lines[i].startsWith("20,")) {
                line20Index = i;
            }
        }

        assertTrue(line5Index < line10Index && line10Index < line20Index,
                "Events should be sorted by tick");
    }

    @Test
    public void testGenerateBatchInputFile_CreatesFile() throws IOException {
        LiftConfigDTO liftConfig = new LiftConfigDTO(
                0, 5, 1, 1, 1, 1, 0, 0, 5,
                ControllerStrategy.NEAREST_REQUEST_ROUTING,
                IdleParkingMode.PARK_TO_HOME_FLOOR
        );

        String liftConfigJson = objectMapper.writeValueAsString(liftConfig);

        PassengerFlowDTO flow = new PassengerFlowDTO(0, 0, 3, 1);
        ScenarioDefinitionDTO scenario = new ScenarioDefinitionDTO(
                10,
                List.of(flow),
                null
        );

        Path outputPath = tempDir.resolve("test.scenario");

        generator.generateBatchInputFile(liftConfigJson, scenario, "File Test", outputPath);

        assertTrue(Files.exists(outputPath));
        String content = Files.readString(outputPath);
        assertTrue(content.contains("name: File Test"));
        assertTrue(content.contains("0, hall_call, p1, 0, UP"));
    }

    @Test
    public void testGenerateBatchInputFile_CreatesParentDirectory() throws IOException {
        LiftConfigDTO liftConfig = new LiftConfigDTO(
                0, 5, 1, 1, 1, 1, 0, 0, 5,
                ControllerStrategy.NEAREST_REQUEST_ROUTING,
                IdleParkingMode.PARK_TO_HOME_FLOOR
        );

        String liftConfigJson = objectMapper.writeValueAsString(liftConfig);

        PassengerFlowDTO flow = new PassengerFlowDTO(0, 0, 3, 1);
        ScenarioDefinitionDTO scenario = new ScenarioDefinitionDTO(
                10,
                List.of(flow),
                null
        );

        Path outputPath = tempDir.resolve("nested/dir/test.scenario");

        generator.generateBatchInputFile(liftConfigJson, scenario, "Nested Test", outputPath);

        assertTrue(Files.exists(outputPath));
        assertTrue(Files.exists(outputPath.getParent()));
    }

    @Test
    public void testGenerateBatchInputFile_InvalidLiftConfigJson() {
        String invalidJson = "{invalid json";

        PassengerFlowDTO flow = new PassengerFlowDTO(0, 0, 3, 1);
        ScenarioDefinitionDTO scenario = new ScenarioDefinitionDTO(
                10,
                List.of(flow),
                null
        );

        Path outputPath = tempDir.resolve("test.scenario");

        assertThrows(IOException.class, () ->
                generator.generateBatchInputFile(invalidJson, scenario, "Invalid", outputPath)
        );
    }

    @Test
    public void testValidateScenarioAgainstConfig_OriginFloorTooLow() {
        LiftConfigDTO liftConfig = new LiftConfigDTO(
                0, 10, 1, 1, 1, 1, 0, 0, 5,
                ControllerStrategy.NEAREST_REQUEST_ROUTING,
                IdleParkingMode.PARK_TO_HOME_FLOOR
        );

        PassengerFlowDTO flow = new PassengerFlowDTO(0, -1, 5, 1);
        ScenarioDefinitionDTO scenario = new ScenarioDefinitionDTO(10, List.of(flow), null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> generator.generateScenarioContent(liftConfig, scenario, "Invalid")
        );

        assertTrue(exception.getMessage().contains("Origin floor -1 is outside configured floor range"));
    }

    @Test
    public void testValidateScenarioAgainstConfig_OriginFloorTooHigh() {
        LiftConfigDTO liftConfig = new LiftConfigDTO(
                0, 10, 1, 1, 1, 1, 0, 0, 5,
                ControllerStrategy.NEAREST_REQUEST_ROUTING,
                IdleParkingMode.PARK_TO_HOME_FLOOR
        );

        PassengerFlowDTO flow = new PassengerFlowDTO(0, 15, 5, 1);
        ScenarioDefinitionDTO scenario = new ScenarioDefinitionDTO(10, List.of(flow), null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> generator.generateScenarioContent(liftConfig, scenario, "Invalid")
        );

        assertTrue(exception.getMessage().contains("Origin floor 15 is outside configured floor range"));
    }

    @Test
    public void testValidateScenarioAgainstConfig_DestinationFloorTooLow() {
        LiftConfigDTO liftConfig = new LiftConfigDTO(
                0, 10, 1, 1, 1, 1, 0, 0, 5,
                ControllerStrategy.NEAREST_REQUEST_ROUTING,
                IdleParkingMode.PARK_TO_HOME_FLOOR
        );

        PassengerFlowDTO flow = new PassengerFlowDTO(0, 5, -1, 1);
        ScenarioDefinitionDTO scenario = new ScenarioDefinitionDTO(10, List.of(flow), null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> generator.generateScenarioContent(liftConfig, scenario, "Invalid")
        );

        assertTrue(exception.getMessage().contains("Destination floor -1 is outside configured floor range"));
    }

    @Test
    public void testValidateScenarioAgainstConfig_DestinationFloorTooHigh() {
        LiftConfigDTO liftConfig = new LiftConfigDTO(
                0, 10, 1, 1, 1, 1, 0, 0, 5,
                ControllerStrategy.NEAREST_REQUEST_ROUTING,
                IdleParkingMode.PARK_TO_HOME_FLOOR
        );

        PassengerFlowDTO flow = new PassengerFlowDTO(0, 5, 20, 1);
        ScenarioDefinitionDTO scenario = new ScenarioDefinitionDTO(10, List.of(flow), null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> generator.generateScenarioContent(liftConfig, scenario, "Invalid")
        );

        assertTrue(exception.getMessage().contains("Destination floor 20 is outside configured floor range"));
    }

    @Test
    public void testValidateScenarioAgainstConfig_StartTickTooLarge() {
        LiftConfigDTO liftConfig = new LiftConfigDTO(
                0, 10, 1, 1, 1, 1, 0, 0, 5,
                ControllerStrategy.NEAREST_REQUEST_ROUTING,
                IdleParkingMode.PARK_TO_HOME_FLOOR
        );

        PassengerFlowDTO flow = new PassengerFlowDTO(100, 5, 8, 1);
        ScenarioDefinitionDTO scenario = new ScenarioDefinitionDTO(50, List.of(flow), null);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> generator.generateScenarioContent(liftConfig, scenario, "Invalid")
        );

        assertTrue(exception.getMessage().contains("Passenger flow start tick 100 must be less than duration 50"));
    }

    @Test
    public void testValidateScenarioAgainstConfig_NegativeFloorRange() {
        LiftConfigDTO liftConfig = new LiftConfigDTO(
                -2, 10, 1, 1, 1, 1, 0, 0, 5,
                ControllerStrategy.NEAREST_REQUEST_ROUTING,
                IdleParkingMode.PARK_TO_HOME_FLOOR
        );

        PassengerFlowDTO flow = new PassengerFlowDTO(0, -2, 5, 1);
        ScenarioDefinitionDTO scenario = new ScenarioDefinitionDTO(10, List.of(flow), null);

        String result = generator.generateScenarioContent(liftConfig, scenario, "Negative Floor");

        assertTrue(result.contains("min_floor: -2"));
        assertTrue(result.contains("0, hall_call, p1, -2, UP"));
    }

    @Test
    public void testGenerateScenarioContent_NoExtraFields() throws IOException {
        LiftConfigDTO liftConfig = new LiftConfigDTO(
                0, 10, 2, 1, 2, 3, 2, 0, 5,
                ControllerStrategy.NEAREST_REQUEST_ROUTING,
                IdleParkingMode.PARK_TO_HOME_FLOOR
        );

        PassengerFlowDTO flow = new PassengerFlowDTO(0, 0, 5, 1);
        ScenarioDefinitionDTO scenario = new ScenarioDefinitionDTO(20, List.of(flow), null);

        String result = generator.generateScenarioContent(liftConfig, scenario, "Format Check");

        // Verify no unexpected fields are present
        String[] validFields = {
                "name:", "ticks:", "min_floor:", "max_floor:", "initial_floor:",
                "travel_ticks_per_floor:", "door_transition_ticks:", "door_dwell_ticks:",
                "door_reopen_window_ticks:", "home_floor:", "idle_timeout_ticks:",
                "controller_strategy:", "idle_parking_mode:", "hall_call"
        };

        String[] lines = result.split("\n");
        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }

            boolean hasValidPrefix = false;
            for (String validField : validFields) {
                if (line.contains(validField)) {
                    hasValidPrefix = true;
                    break;
                }
            }

            assertTrue(hasValidPrefix,
                    "Line contains unexpected field: " + line);
        }
    }
}
