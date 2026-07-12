package com.liftsimulator.admin.service;

import com.liftsimulator.admin.dto.LiftConfigDTO;
import com.liftsimulator.admin.dto.PassengerFlowDTO;
import com.liftsimulator.admin.dto.ScenarioDefinitionDTO;
import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.service.metrics.RunMetrics;
import com.liftsimulator.domain.ControllerStrategy;
import com.liftsimulator.domain.IdleParkingMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SimulationArtefactWriter} against a temp directory,
 * with no real database or run execution involved.
 */
@ExtendWith(MockitoExtension.class)
class SimulationArtefactWriterTest {

    private static final LiftConfigDTO CONFIG = new LiftConfigDTO(
        0, 4, 1, 1, 1, 1, 1, 0, 2,
        ControllerStrategy.NEAREST_REQUEST_ROUTING, IdleParkingMode.PARK_TO_HOME_FLOOR
    );

    @Mock
    private RunLifecycleManager lifecycleManager;

    @Mock
    private BatchInputGenerator batchInputGenerator;

    @TempDir
    private Path tempDir;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private SimulationArtefactWriter writer;

    @BeforeEach
    void setUp() {
        writer = new SimulationArtefactWriter(objectMapper, batchInputGenerator, lifecycleManager);
    }

    @Test
    void logWritesTimestampedLineToOpenedFile() throws Exception {
        try (BufferedWriter logWriter = writer.openLog(tempDir)) {
            writer.log(logWriter, "hello run log");
        }

        String content = Files.readString(tempDir.resolve("run.log"), StandardCharsets.UTF_8);
        assertTrue(content.contains("hello run log"), "Log line must contain the message");
        assertTrue(content.matches("(?s)^\\[.*] hello run log\\R?$"), "Log line must be timestamp-prefixed");
    }

    @Test
    void appendLogAddsToAnExistingLogFileWithoutOverwriting() throws Exception {
        Path logPath = tempDir.resolve("run.log");
        try (BufferedWriter logWriter = writer.openLog(tempDir)) {
            writer.log(logWriter, "first line");
        }

        writer.appendLog(logPath, "second line");

        String content = Files.readString(logPath, StandardCharsets.UTF_8);
        assertTrue(content.contains("first line"));
        assertTrue(content.contains("second line"));
        assertTrue(content.indexOf("first line") < content.indexOf("second line"));
    }

    @Test
    void writeInputFilesWritesConfigScenarioAndBatchInput() throws Exception {
        String configJson = "{\"minFloor\":0}";
        String scenarioJson = "{\"durationTicks\":6,\"passengerFlows\":[{\"startTick\":0,"
            + "\"originFloor\":0,\"destinationFloor\":3,\"passengers\":1}]}";
        SimulationRunner.RunExecutionRequest request = new SimulationRunner.RunExecutionRequest(
            1L, configJson, scenarioJson, "my-scenario"
        );

        try (BufferedWriter logWriter = writer.openLog(tempDir)) {
            writer.writeInputFiles(request, tempDir, logWriter);
        }

        assertEquals(configJson, Files.readString(tempDir.resolve("config.json"), StandardCharsets.UTF_8));
        assertEquals(scenarioJson, Files.readString(tempDir.resolve("scenario.json"), StandardCharsets.UTF_8));
        verify(batchInputGenerator).generateBatchInputFile(
            eq(configJson), any(ScenarioDefinitionDTO.class), eq("my-scenario"), eq(tempDir.resolve("input.scenario"))
        );
        String logContents = Files.readString(tempDir.resolve("run.log"), StandardCharsets.UTF_8);
        assertTrue(logContents.contains("Wrote config input to config.json"));
        assertTrue(logContents.contains("Wrote scenario input to scenario.json"));
        assertTrue(logContents.contains("Wrote batch input to input.scenario"));
    }

    @Test
    void writeInputFilesSkipsScenarioAndBatchInputWhenScenarioJsonMissing() throws Exception {
        SimulationRunner.RunExecutionRequest request = new SimulationRunner.RunExecutionRequest(
            1L, "{\"minFloor\":0}", null, null
        );

        try (BufferedWriter logWriter = writer.openLog(tempDir)) {
            writer.writeInputFiles(request, tempDir, logWriter);
        }

        assertTrue(Files.exists(tempDir.resolve("config.json")));
        assertFalse(Files.exists(tempDir.resolve("scenario.json")));
        assertFalse(Files.exists(tempDir.resolve("input.scenario")));
    }

    @Test
    void writeResultsWritesSucceededResultsWithKpisPerLiftAndPerFloor() throws Exception {
        stubRunMetadata(42L, 10L, 3);
        ScenarioDefinitionDTO scenario = new ScenarioDefinitionDTO(
            6, List.of(new PassengerFlowDTO(0, 0, 3, 1)), 7
        );
        RunMetrics metrics = SimulationRunner.runTickLoop(CONFIG, scenario, () -> false, tick -> { });

        writer.writeResults(42L, tempDir, CONFIG, scenario, metrics, "SUCCEEDED", null);

        JsonNode results = objectMapper.readTree(tempDir.resolve("results.json").toFile());
        assertEquals("SUCCEEDED", results.at("/runSummary/status").asString());
        assertEquals(42L, results.at("/runSummary/runId").asLong());
        assertEquals(6, results.at("/runSummary/durationTicks").asInt());
        assertEquals(7, results.at("/runSummary/seed").asInt());
        assertEquals(6L, results.at("/runSummary/ticks").asLong());
        assertEquals(10L, results.at("/runSummary/liftSystemId").asLong());
        assertEquals(3, results.at("/runSummary/versionNumber").asInt());
        assertFalse(results.at("/runSummary").has("message"), "No message expected on a succeeded run");
        assertTrue(results.has("kpis"));
        assertTrue(results.has("perLift"));
        assertTrue(results.has("perFloor"));
        assertEquals(1, results.at("/kpis/pickupRequestsServed").asInt());
    }

    @Test
    void writeResultsWritesFailedResultsWithMessageAndNoKpis() throws Exception {
        stubRunMetadata(5L, 11L, 1);

        writer.writeResults(5L, tempDir, null, null, null, "FAILED", "Invalid configuration payload.");

        JsonNode results = objectMapper.readTree(tempDir.resolve("results.json").toFile());
        assertEquals("FAILED", results.at("/runSummary/status").asString());
        assertEquals("Invalid configuration payload.", results.at("/runSummary/message").asString());
        assertFalse(results.has("kpis"));
        assertFalse(results.has("perLift"));
        assertFalse(results.has("perFloor"));
    }

    @Test
    void writeResultsWritesCancelledResultsWithPartialMetrics() throws Exception {
        stubRunMetadata(7L, 12L, 2);
        ScenarioDefinitionDTO scenario = new ScenarioDefinitionDTO(
            6, List.of(new PassengerFlowDTO(0, 0, 3, 1)), 7
        );
        int[] calls = {0};
        RunMetrics metrics = SimulationRunner.runTickLoop(CONFIG, scenario, () -> calls[0]++ >= 3, tick -> { });

        writer.writeResults(7L, tempDir, CONFIG, scenario, metrics, "CANCELLED", "Run cancelled at tick 3.");

        JsonNode results = objectMapper.readTree(tempDir.resolve("results.json").toFile());
        assertEquals("CANCELLED", results.at("/runSummary/status").asString());
        assertEquals("Run cancelled at tick 3.", results.at("/runSummary/message").asString());
        assertEquals(3L, results.at("/runSummary/ticks").asLong());
        assertTrue(results.has("kpis"), "Partial metrics are still rendered when config/scenario are present");
    }

    private void stubRunMetadata(Long runId, Long liftSystemId, int versionNumber) {
        LiftSystem liftSystem = new LiftSystem("sys", "Sys", "desc");
        liftSystem.setId(liftSystemId);
        LiftSystemVersion version = new LiftSystemVersion(liftSystem, versionNumber, "{}");
        SimulationRun run = new SimulationRun(liftSystem, version);
        run.setId(runId);
        when(lifecycleManager.getByIdWithDetails(runId)).thenReturn(run);
    }
}
