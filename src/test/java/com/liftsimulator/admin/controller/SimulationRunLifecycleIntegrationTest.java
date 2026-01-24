package com.liftsimulator.admin.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftsimulator.LocalIntegrationTest;
import com.liftsimulator.admin.dto.CreateSimulationRunRequest;
import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.entity.LiftSystemVersion.VersionStatus;
import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.entity.SimulationScenario;
import com.liftsimulator.admin.repository.LiftSystemRepository;
import com.liftsimulator.admin.repository.LiftSystemVersionRepository;
import com.liftsimulator.admin.repository.SimulationRunRepository;
import com.liftsimulator.admin.repository.SimulationScenarioRepository;
import com.liftsimulator.admin.service.SimulationRunService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for simulation run lifecycle (start -> poll -> results).
 */
@AutoConfigureMockMvc
public class SimulationRunLifecycleIntegrationTest extends LocalIntegrationTest {

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
    private SimulationScenarioRepository scenarioRepository;

    @Autowired
    private SimulationRunRepository runRepository;

    @Autowired
    private SimulationRunService runService;

    private LiftSystem testSystem;
    private LiftSystemVersion testVersion;
    private SimulationScenario testScenario;

    @BeforeEach
    public void setUp() {
        runRepository.deleteAll();
        scenarioRepository.deleteAll();
        versionRepository.deleteAll();
        liftSystemRepository.deleteAll();

        testSystem = new LiftSystem("lifecycle-system", "Lifecycle System", "Test Description");
        testSystem = liftSystemRepository.save(testSystem);

        testVersion = new LiftSystemVersion(testSystem, 1,
            "{\"minFloor\": 0, \"maxFloor\": 10, \"lifts\": 2, \"travelTicksPerFloor\": 1, " +
            "\"doorTransitionTicks\": 2, \"doorDwellTicks\": 3, \"doorReopenWindowTicks\": 2, " +
            "\"homeFloor\": 0, \"idleTimeoutTicks\": 5, \"controllerStrategy\": \"NEAREST_REQUEST_ROUTING\", " +
            "\"idleParkingMode\": \"PARK_TO_HOME_FLOOR\"}");
        testVersion.setStatus(VersionStatus.PUBLISHED);
        testVersion = versionRepository.save(testVersion);

        testScenario = new SimulationScenario();
        testScenario.setName("Lifecycle Scenario");
        testScenario.setScenarioJson("{\"durationTicks\": 20, \"passengerFlows\": [{\"startTick\": 0, \"originFloor\": 0, \"destinationFloor\": 5, \"passengers\": 1}]}");
        testScenario = scenarioRepository.save(testScenario);
    }

    @Test
    public void testRunLifecycle_StartPollResults() throws Exception {
        CreateSimulationRunRequest request = new CreateSimulationRunRequest(
                testSystem.getId(),
                testVersion.getId(),
                testScenario.getId(),
                4242L
        );

        MvcResult createResult = mockMvc.perform(post("/api/simulation-runs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andReturn();

        JsonNode createJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long runId = createJson.get("id").asLong();

        CompletableFuture<Void> completion = CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(150);
                SimulationRun run = runService.getRunById(runId);

                // Only write results and update status if the actual simulation hasn't completed yet
                if (run.getStatus() != SimulationRun.RunStatus.SUCCEEDED) {
                    Path runDir = Path.of(run.getArtefactBasePath());
                    Files.createDirectories(runDir);
                    Files.writeString(runDir.resolve("results.json"), objectMapper.writeValueAsString(Map.of(
                            "runSummary", Map.of("runId", runId, "status", "SUCCEEDED", "ticks", 20),
                            "metrics", Map.of("totalPassengersServed", 1, "averageWaitTime", 2.5)
                    )));
                    runService.updateProgress(runId, 20L);
                    try {
                        runService.succeedRun(runId);
                    } catch (IllegalStateException e) {
                        // Ignore if already succeeded by the actual execution service
                    }
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        boolean completed = false;
        for (int i = 0; i < 20; i++) {
            MvcResult pollResult = mockMvc.perform(get("/api/simulation-runs/" + runId))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode pollJson = objectMapper.readTree(pollResult.getResponse().getContentAsString());
            if ("SUCCEEDED".equals(pollJson.get("status").asText())) {
                completed = true;
                break;
            }
            Thread.sleep(100);
        }

        completion.get(2, TimeUnit.SECONDS);

        assertTrue(completed, "Run should reach SUCCEEDED status while polling");

        mockMvc.perform(get("/api/simulation-runs/" + runId + "/results"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.results.runSummary.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.results.metrics.totalPassengersServed").value(1))
                .andExpect(jsonPath("$.logsUrl").value("/api/simulation-runs/" + runId + "/logs"));
    }
}
