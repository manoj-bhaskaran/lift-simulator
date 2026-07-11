package com.liftsimulator.admin.service;

import com.liftsimulator.admin.dto.LiftConfigDTO;
import com.liftsimulator.admin.dto.ScenarioDefinitionDTO;
import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.service.metrics.RunMetrics;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Writes the files produced by a simulation run into the run artefact directory.
 */
@Service
public class SimulationArtefactWriter {
    static final String CONFIG_FILE_NAME = "config.json";
    static final String SCENARIO_FILE_NAME = "scenario.json";
    static final String INPUT_SCENARIO_FILE_NAME = "input.scenario";
    static final String LOG_FILE_NAME = "run.log";
    static final String RESULTS_FILE_NAME = "results.json";

    private static final Logger logger = LoggerFactory.getLogger(SimulationArtefactWriter.class);

    private final ObjectMapper objectMapper;
    private final BatchInputGenerator batchInputGenerator;
    private final RunLifecycleManager lifecycleManager;

    public SimulationArtefactWriter(ObjectMapper objectMapper,
                                    BatchInputGenerator batchInputGenerator,
                                    RunLifecycleManager lifecycleManager) {
        this.objectMapper = objectMapper;
        this.batchInputGenerator = batchInputGenerator;
        this.lifecycleManager = lifecycleManager;
    }

    public BufferedWriter openLog(Path runDir) throws IOException {
        return Files.newBufferedWriter(runDir.resolve(LOG_FILE_NAME), StandardCharsets.UTF_8);
    }

    public void log(BufferedWriter writer, String message) throws IOException {
        writer.write("[" + OffsetDateTime.now() + "] " + message);
        writer.newLine();
        writer.flush();
    }

    public void appendLog(Path logPath, String message) {
        try (BufferedWriter logWriter = Files.newBufferedWriter(
                logPath,
                StandardCharsets.UTF_8,
                java.nio.file.StandardOpenOption.APPEND)) {
            log(logWriter, message);
        } catch (IOException ex) {
            logger.warn("Failed to append message to run log at {}", logPath, ex);
        }
    }

    public void writeInputFiles(SimulationRunner.RunExecutionRequest request,
                                Path runDir,
                                BufferedWriter logWriter) throws IOException {
        Files.writeString(runDir.resolve(CONFIG_FILE_NAME), request.configJson(), StandardCharsets.UTF_8);
        log(logWriter, "Wrote config input to " + CONFIG_FILE_NAME);

        if (request.scenarioJson() != null) {
            Files.writeString(runDir.resolve(SCENARIO_FILE_NAME), request.scenarioJson(), StandardCharsets.UTF_8);
            log(logWriter, "Wrote scenario input to " + SCENARIO_FILE_NAME);

            ScenarioDefinitionDTO scenarioDefinition = objectMapper.readValue(
                    request.scenarioJson(), ScenarioDefinitionDTO.class);
            String name = request.scenarioName() != null ? request.scenarioName() : "run";
            batchInputGenerator.generateBatchInputFile(
                    request.configJson(), scenarioDefinition, name, runDir.resolve(INPUT_SCENARIO_FILE_NAME));
            log(logWriter, "Wrote batch input to " + INPUT_SCENARIO_FILE_NAME);
        }
    }

    public void writeResults(Long runId,
                             Path runDir,
                             LiftConfigDTO config,
                             ScenarioDefinitionDTO scenario,
                             RunMetrics metrics,
                             String status,
                             String message) throws IOException {
        ObjectNode results = objectMapper.createObjectNode();
        ObjectNode runSummary = results.putObject("runSummary");
        runSummary.put("runId", runId);
        runSummary.put("status", status);
        runSummary.put("generatedAt", OffsetDateTime.now().toString());
        if (message != null) {
            runSummary.put("message", message);
        }
        if (scenario != null) {
            runSummary.put("durationTicks", scenario.durationTicks());
            if (scenario.seed() != null) {
                runSummary.put("seed", scenario.seed());
            }
        }
        if (metrics != null) {
            runSummary.put("ticks", metrics.totalTicks());
        }
        try {
            SimulationRun run = lifecycleManager.getByIdWithDetails(runId);
            runSummary.put("liftSystemId", run.getLiftSystem().getId());
            runSummary.put("versionNumber", run.getVersion().getVersionNumber());
        } catch (Exception ex) {
            logger.warn("Failed to resolve run summary metadata for run {}", runId, ex);
        }

        if (metrics != null && config != null) {
            results.set("kpis", metrics.toKpisNode(objectMapper));
            results.set("perLift", metrics.toPerLiftNode(objectMapper, config));
            results.set("perFloor", metrics.toPerFloorNode(objectMapper));
        }

        objectMapper.writerWithDefaultPrettyPrinter()
            .writeValue(runDir.resolve(RESULTS_FILE_NAME).toFile(), results);
    }
}
