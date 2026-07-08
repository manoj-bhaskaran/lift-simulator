package com.liftsimulator.admin.controller;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.liftsimulator.LocalIntegrationTest;
import com.liftsimulator.admin.dto.CreateSimulationRunRequest;
import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.entity.LiftSystemVersion.VersionStatus;
import com.liftsimulator.admin.entity.Scenario;
import com.liftsimulator.admin.repository.LiftSystemRepository;
import com.liftsimulator.admin.repository.LiftSystemVersionRepository;
import com.liftsimulator.admin.repository.ScenarioRepository;
import com.liftsimulator.admin.repository.SimulationRunRepository;
import com.liftsimulator.admin.service.SimulationRunExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for simulation run lifecycle (start -> poll -> results).
 * Simulation run endpoints use API key authentication (not HTTP Basic).
 */
@AutoConfigureMockMvc
public class SimulationRunLifecycleIntegrationTest extends LocalIntegrationTest {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String API_KEY_VALUE = "test-api-key";

    /** Upper bound for the asynchronous simulation to reach a terminal state and flush artefacts. */
    private static final Duration COMPLETION_TIMEOUT = Duration.ofSeconds(20);

    @TempDir
    static Path artefactsRoot;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("simulation.artefacts.base-path", () -> artefactsRoot.toString());
    }

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

    @Autowired
    private ScenarioRepository scenarioRepository;

    @Autowired
    private SimulationRunExecutionService executionService;

    private LiftSystem testSystem;
    private LiftSystemVersion testVersion;

    @BeforeEach
    public void setUp() {
        runRepository.deleteAll();
        scenarioRepository.deleteAll();
        versionRepository.deleteAll();
        liftSystemRepository.deleteAll();

        testSystem = new LiftSystem("lifecycle-system", "Lifecycle System", "Test Description");
        testSystem = liftSystemRepository.save(testSystem);

        testVersion = new LiftSystemVersion(testSystem, 1,
            "{\"minFloor\": 0, \"maxFloor\": 10, \"lifts\": 1, \"travelTicksPerFloor\": 1, " +
            "\"doorTransitionTicks\": 2, \"doorDwellTicks\": 3, \"doorReopenWindowTicks\": 2, " +
            "\"homeFloor\": 0, \"idleTimeoutTicks\": 5, \"controllerStrategy\": \"NEAREST_REQUEST_ROUTING\", " +
            "\"idleParkingMode\": \"PARK_TO_HOME_FLOOR\"}");
        testVersion.setStatus(VersionStatus.PUBLISHED);
        testVersion = versionRepository.save(testVersion);
    }

    @Test
    public void testRunLifecycle_StartPollResults() throws Exception {
        Scenario scenario = scenarioRepository.save(new Scenario(
                "Lifecycle scenario",
                "{\"durationTicks\": 20, \"seed\": 4242, \"passengerFlows\": "
                        + "[{\"startTick\": 0, \"originFloor\": 0, \"destinationFloor\": 3, \"passengers\": 1}]}",
                testVersion
        ));

        CreateSimulationRunRequest request = new CreateSimulationRunRequest(
                testSystem.getId(),
                testVersion.getVersionNumber(),
                scenario.getId(),
                4242L
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/simulation-runs")
                        .header(API_KEY_HEADER, API_KEY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andReturn();

        JsonNode createJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long runId = createJson.get("id").asLong();

        // The simulation runs asynchronously on the execution service's own thread pool.
        // Poll the status endpoint until it reports SUCCEEDED, driven solely by the real
        // execution (no competing writer that would race the runner on the same row).
        boolean completed = pollUntilSucceeded(runId);
        assertTrue(completed, "Run should reach SUCCEEDED status while polling");

        // The runner sets status to SUCCEEDED before it finishes flushing results.json, so wait
        // for the background execution to fully drain before asserting on the artefact contents.
        awaitExecutorIdle();

        mockMvc.perform(get("/api/v1/simulation-runs/" + runId + "/results")
                .header(API_KEY_HEADER, API_KEY_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.results.runSummary.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.results.kpis.pickupRequestsServed").value(1))
                .andExpect(jsonPath("$.results.kpis.passengersServed").value(1))
                .andExpect(jsonPath("$.results.kpis.avgPickupWaitTicks").isNotEmpty())
                .andExpect(jsonPath("$.results.kpis.maxPickupWaitTicks").isNotEmpty())
                .andExpect(jsonPath("$.results.kpis.idleTicks").isNotEmpty())
                .andExpect(jsonPath("$.results.kpis.movingTicks").isNotEmpty())
                .andExpect(jsonPath("$.results.kpis.doorTicks").isNotEmpty())
                .andExpect(jsonPath("$.results.kpis.pickupLegUtilisation").isNotEmpty())
                .andExpect(jsonPath("$.logsUrl").value("/api/v1/simulation-runs/" + runId + "/logs"));
    }

    private boolean pollUntilSucceeded(long runId) throws Exception {
        long deadlineNanos = System.nanoTime() + COMPLETION_TIMEOUT.toNanos();
        while (System.nanoTime() < deadlineNanos) {
            MvcResult pollResult = mockMvc.perform(get("/api/v1/simulation-runs/" + runId)
                    .header(API_KEY_HEADER, API_KEY_VALUE))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode pollJson = objectMapper.readTree(pollResult.getResponse().getContentAsString());
            if ("SUCCEEDED".equals(pollJson.get("status").asText())) {
                return true;
            }
            Thread.sleep(100);
        }
        return false;
    }

    private void awaitExecutorIdle() throws InterruptedException {
        long deadlineNanos = System.nanoTime() + COMPLETION_TIMEOUT.toNanos();
        while (executionService.hasActiveRuns() && System.nanoTime() < deadlineNanos) {
            Thread.sleep(25);
        }
    }
}
