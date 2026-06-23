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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for SimulationRunController.
 * Simulation run endpoints use API key authentication (not HTTP Basic).
 */
@AutoConfigureMockMvc
@Transactional
public class SimulationRunControllerTest extends LocalIntegrationTest {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String API_KEY_VALUE = "test-api-key";

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
            testVersion.getVersionNumber(),
            null,
            12345L
        );

        mockMvc.perform(post("/api/v1/simulation-runs")
                .header(API_KEY_HEADER, API_KEY_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.liftSystemId").value(testSystem.getId()))
            .andExpect(jsonPath("$.versionNumber").value(testVersion.getVersionNumber()))
            .andExpect(jsonPath("$.status").value("RUNNING"))
            .andExpect(jsonPath("$.seed").value(12345L))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.startedAt").exists());
    }

    @Test
    public void testCreateSimulationRun_InvalidLiftSystem() throws Exception {
        CreateSimulationRunRequest request = new CreateSimulationRunRequest(
            999L,
            testVersion.getVersionNumber(),
            null,
            null
        );

        mockMvc.perform(post("/api/v1/simulation-runs")
                .header(API_KEY_HEADER, API_KEY_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Lift system not found with id: 999"));
    }

    @Test
    public void testCreateSimulationRun_MissingRequiredFields() throws Exception {
        String invalidRequest = "{}";

        mockMvc.perform(post("/api/v1/simulation-runs")
                .header(API_KEY_HEADER, API_KEY_VALUE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void testGetSimulationRun_Success() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        run = runRepository.save(run);

        mockMvc.perform(get("/api/v1/simulation-runs/" + run.getId())
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(run.getId()))
            .andExpect(jsonPath("$.liftSystemId").value(testSystem.getId()))
            .andExpect(jsonPath("$.versionNumber").value(testVersion.getVersionNumber()))
            .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    public void testGetSimulationRun_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/simulation-runs/999")
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Simulation run not found with id: 999"));
    }

    @Test
    public void testGetSimulationResults_Running() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        run.start();
        run = runRepository.save(run);

        mockMvc.perform(get("/api/v1/simulation-runs/" + run.getId() + "/results")
                .header(API_KEY_HEADER, API_KEY_VALUE))
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

        mockMvc.perform(get("/api/v1/simulation-runs/" + run.getId() + "/results")
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.runId").value(run.getId()))
            .andExpect(jsonPath("$.status").value("FAILED"))
            .andExpect(jsonPath("$.errorMessage").value("Test error message"))
            .andExpect(jsonPath("$.logsUrl").value("/api/v1/simulation-runs/" + run.getId() + "/logs"));
    }

    @Test
    public void testGetSimulationResults_Created() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        run = runRepository.save(run);

        mockMvc.perform(get("/api/v1/simulation-runs/" + run.getId() + "/results")
                .header(API_KEY_HEADER, API_KEY_VALUE))
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

        mockMvc.perform(get("/api/v1/simulation-runs/" + run.getId() + "/results")
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.runId").value(run.getId()))
            .andExpect(jsonPath("$.status").value("SUCCEEDED"))
            .andExpect(jsonPath("$.results.totalPassengersServed").value(100))
            .andExpect(jsonPath("$.results.averageWaitTime").value(15.5))
            .andExpect(jsonPath("$.errorMessage").doesNotExist())
            .andExpect(jsonPath("$.logsUrl").value("/api/v1/simulation-runs/" + run.getId() + "/logs"));
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

        mockMvc.perform(get("/api/v1/simulation-runs/" + run.getId() + "/results")
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.runId").value(run.getId()))
            .andExpect(jsonPath("$.status").value("SUCCEEDED"))
            .andExpect(jsonPath("$.results").doesNotExist())
            .andExpect(jsonPath("$.errorMessage").value("Results file not available: No results file found for simulation run " + run.getId()))
            .andExpect(jsonPath("$.logsUrl").value("/api/v1/simulation-runs/" + run.getId() + "/logs"));
    }

    @Test
    public void testGetSimulationLogs_NoArtefactPath() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        run = runRepository.save(run);

        mockMvc.perform(get("/api/v1/simulation-runs/" + run.getId() + "/logs")
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value("Artefacts are not available for this run"));
    }

    @Test
    public void testGetSimulationLogs_WithTailParameter() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        run.setArtefactBasePath("./simulation-runs/run-test");
        run = runRepository.save(run);

        // Create test log file
        Path artefactDir = Paths.get(run.getArtefactBasePath());
        Files.createDirectories(artefactDir);
        Path logFile = artefactDir.resolve("run.log");
        Files.writeString(logFile, "Line 1\nLine 2\nLine 3\n");

        mockMvc.perform(get("/api/v1/simulation-runs/" + run.getId() + "/logs?tail=2")
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.runId").value(run.getId()))
            .andExpect(jsonPath("$.logs").value("Line 2\nLine 3"))
            .andExpect(jsonPath("$.tail").value(2));
    }

    @Test
    public void testGetSimulationLogs_RejectsNegativeTailParameter() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        run.setArtefactBasePath("./simulation-runs/run-negative-tail");
        run = runRepository.save(run);

        mockMvc.perform(get("/api/v1/simulation-runs/" + run.getId() + "/logs?tail=-1")
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fieldErrors.tail").value("tail must be greater than or equal to 1"));
    }

    @Test
    public void testGetSimulationLogs_RejectsZeroTailParameter() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        run.setArtefactBasePath("./simulation-runs/run-zero-tail");
        run = runRepository.save(run);

        mockMvc.perform(get("/api/v1/simulation-runs/" + run.getId() + "/logs?tail=0")
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fieldErrors.tail").value("tail must be greater than or equal to 1"));
    }

    @Test
    public void testGetSimulationLogs_RejectsExcessiveTailParameter() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        run.setArtefactBasePath("./simulation-runs/run-excessive-tail");
        run = runRepository.save(run);

        mockMvc.perform(get("/api/v1/simulation-runs/" + run.getId() + "/logs?tail=10001")
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fieldErrors.tail").value("tail must be less than or equal to 10000"));
    }

    @Test
    public void testListSimulationArtefacts_EmptyDirectory() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        run.setArtefactBasePath("./simulation-runs/run-empty");
        run = runRepository.save(run);

        // Create empty artefact directory
        Files.createDirectories(Paths.get(run.getArtefactBasePath()));

        mockMvc.perform(get("/api/v1/simulation-runs/" + run.getId() + "/artefacts")
                .header(API_KEY_HEADER, API_KEY_VALUE))
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
        Files.writeString(artefactDir.resolve("run.log"), "Log content");

        mockMvc.perform(get("/api/v1/simulation-runs/" + run.getId() + "/artefacts")
                .header(API_KEY_HEADER, API_KEY_VALUE))
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

        mockMvc.perform(get("/api/v1/simulation-runs/" + run.getId() + "/artefacts/results.json")
                .header(API_KEY_HEADER, API_KEY_VALUE))
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

        mockMvc.perform(get("/api/v1/simulation-runs/" + run.getId() + "/artefacts/missing.json")
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Artefact not found: missing.json"));
    }

    @Test
    // Run outside the class-level rollback transaction so afterCommit artefact cleanup executes.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void testDeleteSimulationRun_Success() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        run.setArtefactBasePath("./simulation-runs/run-delete");
        run.start();
        run.succeed();
        run = runRepository.save(run);

        // Create artefact directory with files that must be removed on deletion.
        Path artefactDir = Paths.get(run.getArtefactBasePath());
        Files.createDirectories(artefactDir);
        Files.writeString(artefactDir.resolve("results.json"), "{\"ok\": true}");
        Files.writeString(artefactDir.resolve("run.log"), "Log content");

        Long runId = run.getId();

        mockMvc.perform(delete("/api/v1/simulation-runs/" + runId)
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isNoContent());

        // Run history removed from the database.
        mockMvc.perform(get("/api/v1/simulation-runs/" + runId)
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isNotFound());

        // Stored artefacts removed from disk.
        assertFalse(Files.exists(artefactDir));
    }

    @Test
    // Run outside the class-level rollback transaction so afterCommit artefact cleanup executes.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void testDeleteSimulationRun_RemovesArtefactsAndReturns404Afterwards() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        run.setArtefactBasePath("./simulation-runs/run-delete-artefacts");
        run.start();
        run.succeed();
        run = runRepository.save(run);

        Path artefactDir = Paths.get(run.getArtefactBasePath());
        Files.createDirectories(artefactDir);
        Files.writeString(artefactDir.resolve("results.json"), "{\"ok\": true}");

        Long runId = run.getId();

        mockMvc.perform(delete("/api/v1/simulation-runs/" + runId)
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isNoContent());

        // Artefact links for a deleted run return a clear 404, not a 500.
        mockMvc.perform(get("/api/v1/simulation-runs/" + runId + "/artefacts/results.json")
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteSimulationRun_NotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/simulation-runs/999")
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Simulation run not found with id: 999"));
    }

    @Test
    public void testDeleteSimulationRun_RunningRejected() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        run.start();
        run = runRepository.save(run);

        mockMvc.perform(delete("/api/v1/simulation-runs/" + run.getId())
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isConflict());

        // Run still present after a rejected deletion.
        mockMvc.perform(get("/api/v1/simulation-runs/" + run.getId())
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isOk());
    }

    @Test
    public void testDeleteSimulationRun_RequiresApiKey() throws Exception {
        mockMvc.perform(delete("/api/v1/simulation-runs/1"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void testSimulationRunEndpoints_RequiresApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/simulation-runs"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void testSimulationRunEndpoints_InvalidApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/simulation-runs")
                .header(API_KEY_HEADER, "invalid-key"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void testListSimulationRuns_Pagination_PageSizeAndTotals() throws Exception {
        // Create 50 runs
        for (int i = 0; i < 50; i++) {
            SimulationRun run = new SimulationRun(testSystem, testVersion);
            runRepository.save(run);
        }

        // Page 0, size 20 => 20 items, 3 total pages, 50 total elements
        mockMvc.perform(get("/api/v1/simulation-runs?page=0&size=20&sort=createdAt,desc")
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(20))
            .andExpect(jsonPath("$.totalElements").value(50))
            .andExpect(jsonPath("$.totalPages").value(3))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(20));

        // Page 2, size 20 => 10 items (50 - 2*20 = 10)
        mockMvc.perform(get("/api/v1/simulation-runs?page=2&size=20&sort=createdAt,desc")
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(10))
            .andExpect(jsonPath("$.totalElements").value(50))
            .andExpect(jsonPath("$.number").value(2));
    }

    @Test
    public void testListSimulationRuns_Pagination_DefaultsToFirstPage() throws Exception {
        for (int i = 0; i < 5; i++) {
            SimulationRun run = new SimulationRun(testSystem, testVersion);
            runRepository.save(run);
        }

        // No pagination params => defaults to page 0, size 20
        mockMvc.perform(get("/api/v1/simulation-runs")
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(5))
            .andExpect(jsonPath("$.totalElements").value(5))
            .andExpect(jsonPath("$.number").value(0))
            .andExpect(jsonPath("$.size").value(20));
    }

    @Test
    public void testListSimulationRuns_Pagination_AllowsDocumentedSortProperties() throws Exception {
        SimulationRun run = new SimulationRun(testSystem, testVersion);
        runRepository.save(run);

        mockMvc.perform(get("/api/v1/simulation-runs"
                        + "?page=0&size=20&sort=startedAt,asc"
                        + "&sort=endedAt,desc&sort=status,asc&sort=id,desc")
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    public void testListSimulationRuns_Pagination_RejectsUnsupportedSortProperty() throws Exception {
        mockMvc.perform(get("/api/v1/simulation-runs?page=0&size=20&sort=liftSystem.name,asc")
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value(
                    "Unsupported simulation run sort property: liftSystem.name. "
                            + "Allowed sort properties: createdAt, endedAt, id, startedAt, status"));
    }

    @Test
    public void testListSimulationRuns_Pagination_RejectsIgnoreCaseSortModifier() throws Exception {
        mockMvc.perform(get("/api/v1/simulation-runs?page=0&size=20&sort=status,asc,ignorecase")
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value(
                    "Unsupported ignore-case simulation run sort modifier for: status. "
                            + "Allowed sort properties: createdAt, endedAt, id, startedAt, status; "
                            + "ignore-case sorting is not supported."));
    }

    @Test
    public void testListSimulationRuns_Pagination_MaxSizeEnforced() throws Exception {
        // Requesting size=200 should be capped at 100
        mockMvc.perform(get("/api/v1/simulation-runs?page=0&size=200")
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(100));
    }
}
