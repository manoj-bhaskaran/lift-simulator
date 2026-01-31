package com.liftsimulator.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftsimulator.LocalIntegrationTest;
import com.liftsimulator.admin.dto.CreateSimulationRunRequest;
import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.entity.LiftSystemVersion.VersionStatus;
import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.repository.LiftSystemRepository;
import com.liftsimulator.admin.repository.LiftSystemVersionRepository;
import com.liftsimulator.admin.repository.SimulationRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for SimulationRunController.
 */
@AutoConfigureMockMvc
@Transactional
public class SimulationRunControllerTest extends LocalIntegrationTest {

    // Test credentials from application-test.yml
    private static final String TEST_ADMIN_USER = "testadmin";
    private static final String TEST_ADMIN_PASSWORD = "testpassword";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LiftSystemRepository liftSystemRepository;

    @Autowired
    private LiftSystemVersionRepository versionRepository;

    @Autowired
    private SimulationRunRepository runRepository;

    private LiftSystem testSystem;
    private LiftSystemVersion testVersion;

    @BeforeEach
    public void setUp() throws IOException {
        runRepository.deleteAll();
        versionRepository.deleteAll();
        liftSystemRepository.deleteAll();

        // Clean up any test artefact directories
        Path testRunsDir = Paths.get("./simulation-runs");
        if (Files.exists(testRunsDir)) {
            Files.walk(testRunsDir)
                .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        // Ignore cleanup errors in tests
                    }
                });
        }

        // Create test data
        testSystem = new LiftSystem("test-system", "Test System", "Test Description");
        testSystem = liftSystemRepository.save(testSystem);

        testVersion = new LiftSystemVersion(testSystem, 1,
            "{\"minFloor\": 0, \"maxFloor\": 10, \"lifts\": 2, \"travelTicksPerFloor\": 1, " +
            "\"doorTransitionTicks\": 2, \"doorDwellTicks\": 3, \"doorReopenWindowTicks\": 2, " +
            "\"homeFloor\": 0, \"idleTimeoutTicks\": 5, \"controllerStrategy\": \"NEAREST_REQUEST_ROUTING\", " +
            "\"idleParkingMode\": \"PARK_TO_HOME_FLOOR\"}");
        testVersion.setStatus(VersionStatus.PUBLISHED);
        testVersion = versionRepository.save(testVersion);
    }

    @Test
    public void testCreateSimulationRun_Success() throws Exception {
        CreateSimulationRunRequest request = new CreateSimulationRunRequest(
            testSystem.getId(),
            testVersion.getId(),
            null,
            12345L
        );

        mockMvc.perform(post("/api/simulation-runs")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.liftSystemId").value(testSystem.getId()))
            .andExpect(jsonPath("$.versionId").value(testVersion.getId()))
            .andExpect(jsonPath("$.status").value("RUNNING"))
            .andExpect(jsonPath("$.seed").value(12345L))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.startedAt").exists());
    }

    @Test
    public void testCreateSimulationRun_InvalidLiftSystem() throws Exception {
        CreateSimulationRunRequest request = new CreateSimulationRunRequest(
            999L,
            testVersion.getId(),
            null,
            null
        );

        mockMvc.perform(post("/api/simulation-runs")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Lift system not found with id: 999"));
    }

    @Test
    public void testCreateSimulationRun_MissingRequiredFields() throws Exception {
        String invalidRequest = "{}";

        mockMvc.perform(post("/api/simulation-runs")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetSimulationRun_Success() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        run = runRepository.save(run);

        mockMvc.perform(get("/api/simulation-runs/" + run.getId())
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(run.getId()))
            .andExpect(jsonPath("$.liftSystemId").value(testSystem.getId()))
            .andExpect(jsonPath("$.versionId").value(testVersion.getId()))
            .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    public void testGetSimulationRun_NotFound() throws Exception {
        mockMvc.perform(get("/api/simulation-runs/999")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Simulation run not found with id: 999"));
    }

    @Test
    public void testGetSimulationResults_Running() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        run.start();
        run = runRepository.save(run);

        mockMvc.perform(get("/api/simulation-runs/" + run.getId() + "/results")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.runId").value(run.getId()))
            .andExpect(jsonPath("$.status").value("RUNNING"))
            .andExpect(jsonPath("$.errorMessage").value("Simulation is still running"));
    }

    @Test
    public void testGetSimulationResults_Failed() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        run.start();
        run.fail("Test error message");
        run = runRepository.save(run);

        mockMvc.perform(get("/api/simulation-runs/" + run.getId() + "/results")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.runId").value(run.getId()))
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.errorMessage").value("Test error message"))
            .andExpect(jsonPath("$.logsUrl").value("/api/simulation-runs/" + run.getId() + "/logs"));
    }

    @Test
    public void testGetSimulationResults_Created() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        run = runRepository.save(run);

        mockMvc.perform(get("/api/simulation-runs/" + run.getId() + "/results")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    public void testGetSimulationResults_Succeeded_WithResults() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        run.setArtefactBasePath("./simulation-runs/run-succeeded");
        run.start();
        run.succeed();
        run = runRepository.save(run);

        // Create artefact directory with results file
        Path artefactDir = Paths.get(run.getArtefactBasePath());
        Files.createDirectories(artefactDir);
        Path resultsFile = artefactDir.resolve("results.json");
        Files.writeString(resultsFile, "{\"totalPassengersServed\": 100, \"averageWaitTime\": 15.5}");

        mockMvc.perform(get("/api/simulation-runs/" + run.getId() + "/results")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.runId").value(run.getId()))
            .andExpect(jsonPath("$.status").value("SUCCEEDED"))
            .andExpect(jsonPath("$.results.totalPassengersServed").value(100))
            .andExpect(jsonPath("$.results.averageWaitTime").value(15.5))
            .andExpect(jsonPath("$.errorMessage").doesNotExist())
            .andExpect(jsonPath("$.logsUrl").value("/api/simulation-runs/" + run.getId() + "/logs"));
    }

    @Test
    public void testGetSimulationResults_Succeeded_WithoutResults() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        run.setArtefactBasePath("./simulation-runs/run-no-results");
        run.start();
        run.succeed();
        run = runRepository.save(run);

        // Create artefact directory but no results file
        Path artefactDir = Paths.get(run.getArtefactBasePath());
        Files.createDirectories(artefactDir);

        mockMvc.perform(get("/api/simulation-runs/" + run.getId() + "/results")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.runId").value(run.getId()))
            .andExpect(jsonPath("$.status").value("SUCCEEDED"))
            .andExpect(jsonPath("$.results").doesNotExist())
            .andExpect(jsonPath("$.errorMessage").value("Results file not available: No results file found for simulation run " + run.getId()))
            .andExpect(jsonPath("$.logsUrl").value("/api/simulation-runs/" + run.getId() + "/logs"));
    }

    @Test
    public void testGetSimulationLogs_NoArtefactPath() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        run = runRepository.save(run);

        mockMvc.perform(get("/api/simulation-runs/" + run.getId() + "/logs")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Failed to read logs: Artefact base path is not set for run "
                    + run.getId()));
    }

    @Test
    public void testGetSimulationLogs_WithTailParameter() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        run.setArtefactBasePath("./simulation-runs/run-test");
        run = runRepository.save(run);

        // Create test log file
        Path artefactDir = Paths.get(run.getArtefactBasePath());
        Files.createDirectories(artefactDir);
        Path logFile = artefactDir.resolve("simulation.log");
        Files.writeString(logFile, "Line 1\nLine 2\nLine 3\n");

        mockMvc.perform(get("/api/simulation-runs/" + run.getId() + "/logs?tail=2")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.runId").value(run.getId().toString()))
            .andExpect(jsonPath("$.logs").value("Line 2\nLine 3"))
            .andExpect(jsonPath("$.tail").value("2"));
    }

    @Test
    public void testListSimulationArtefacts_EmptyDirectory() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        run.setArtefactBasePath("./simulation-runs/run-empty");
        run = runRepository.save(run);

        // Create empty artefact directory
        Files.createDirectories(Paths.get(run.getArtefactBasePath()));

        mockMvc.perform(get("/api/simulation-runs/" + run.getId() + "/artefacts")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void testListSimulationArtefacts_WithFiles() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        run.setArtefactBasePath("./simulation-runs/run-with-files");
        run = runRepository.save(run);

        // Create artefact directory with test files
        Path artefactDir = Paths.get(run.getArtefactBasePath());
        Files.createDirectories(artefactDir);
        Files.writeString(artefactDir.resolve("results.json"), "{\"test\": \"data\"}");
        Files.writeString(artefactDir.resolve("simulation.log"), "Log content");

        mockMvc.perform(get("/api/simulation-runs/" + run.getId() + "/artefacts")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    public void testDownloadSimulationArtefact_Success() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        run.setArtefactBasePath("./simulation-runs/run-download");
        run = runRepository.save(run);

        Path artefactDir = Paths.get(run.getArtefactBasePath());
        Files.createDirectories(artefactDir);
        Path resultsFile = artefactDir.resolve("results.json");
        Files.writeString(resultsFile, "{\"ok\": true}");

        mockMvc.perform(get("/api/simulation-runs/" + run.getId() + "/artefacts/results.json")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", "attachment; filename=\"results.json\""))
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(content().string("{\"ok\": true}"));
    }

    @Test
    public void testDownloadSimulationArtefact_NotFound() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        run.setArtefactBasePath("./simulation-runs/run-download-missing");
        run = runRepository.save(run);

        Path artefactDir = Paths.get(run.getArtefactBasePath());
        Files.createDirectories(artefactDir);

        mockMvc.perform(get("/api/simulation-runs/" + run.getId() + "/artefacts/missing.json")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Artefact not found: missing.json"));
    }
}
