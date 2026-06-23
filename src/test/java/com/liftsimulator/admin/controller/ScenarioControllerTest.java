package com.liftsimulator.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftsimulator.LocalIntegrationTest;
import com.liftsimulator.admin.controller.fixtures.ControllerApiFixtures;
import com.liftsimulator.admin.dto.ScenarioRequest;
import com.liftsimulator.admin.dto.ScenarioValidateRequest;
import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.repository.LiftSystemRepository;
import com.liftsimulator.admin.repository.LiftSystemVersionRepository;
import com.liftsimulator.admin.repository.ScenarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-level coverage for scenario CRUD and validation endpoints.
 */
@AutoConfigureMockMvc
@Transactional
public class ScenarioControllerTest extends LocalIntegrationTest {

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
    private ScenarioRepository scenarioRepository;

    private LiftSystemVersion version;

    @BeforeEach
    public void setUp() {
        scenarioRepository.deleteAll();
        versionRepository.deleteAll();
        liftSystemRepository.deleteAll();

        LiftSystem system = liftSystemRepository.save(new LiftSystem(
            "scenario-test-system",
            "Scenario Test System",
            "System used by scenario controller tests"
        ));
        version = versionRepository.save(new LiftSystemVersion(
            system,
            1,
            ControllerApiFixtures.validLiftConfig()
        ));
    }

    @Test
    public void testCreateScenario_Success() throws Exception {
        ScenarioRequest request = scenarioRequest("Morning Rush", ControllerApiFixtures.validScenario());

        mockMvc.perform(post("/api/v1/scenarios")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Morning Rush"))
            .andExpect(jsonPath("$.liftSystemVersionId").value(version.getId()))
            .andExpect(jsonPath("$.versionInfo.systemKey").value("scenario-test-system"))
            .andExpect(jsonPath("$.versionInfo.minFloor").value(0))
            .andExpect(jsonPath("$.versionInfo.maxFloor").value(9))
            .andExpect(jsonPath("$.scenarioJson.durationTicks").value(120))
            .andExpect(jsonPath("$.scenarioJson.passengerFlows[0].destinationFloor").value(4))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    public void testUpdateScenario_Success() throws Exception {
        Long scenarioId = createScenario("Original Scenario");
        ScenarioRequest request = scenarioRequest("Updated Scenario", ControllerApiFixtures.updatedScenario());

        mockMvc.perform(put("/api/v1/scenarios/{id}", scenarioId)
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(scenarioId))
            .andExpect(jsonPath("$.name").value("Updated Scenario"))
            .andExpect(jsonPath("$.scenarioJson.durationTicks").value(180))
            .andExpect(jsonPath("$.scenarioJson.passengerFlows[0].originFloor").value(3));
    }

    @Test
    public void testGetAndListScenarios_Success() throws Exception {
        Long firstScenarioId = createScenario("First Scenario");
        createScenario("Second Scenario");

        mockMvc.perform(get("/api/v1/scenarios/{id}", firstScenarioId)
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(firstScenarioId))
            .andExpect(jsonPath("$.name").value("First Scenario"));

        mockMvc.perform(get("/api/v1/scenarios")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[0].scenarioJson.passengerFlows", hasSize(1)));
    }

    @Test
    public void testDeleteScenario_Success() throws Exception {
        Long scenarioId = createScenario("Delete Me");

        mockMvc.perform(delete("/api/v1/scenarios/{id}", scenarioId)
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/scenarios/{id}", scenarioId)
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Scenario not found with id: " + scenarioId));
    }

    @Test
    public void testCreateScenario_InvalidFloorRange_ReturnsValidationBody() throws Exception {
        ScenarioRequest request = scenarioRequest(
            "Invalid Floor Scenario",
            ControllerApiFixtures.scenarioWithOutOfRangeDestination()
        );

        mockMvc.perform(post("/api/v1/scenarios")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.errors", hasSize(1)))
            .andExpect(jsonPath("$.errors[0].field").value("passengerFlows[0].destinationFloor"))
            .andExpect(jsonPath("$.errors[0].severity").value("ERROR"));
    }

    @Test
    public void testValidateScenario_UnknownProperty_ReturnsValidationResponse() throws Exception {
        ScenarioValidateRequest request = validateRequest(ControllerApiFixtures.scenarioWithUnknownProperty());

        mockMvc.perform(post("/api/v1/scenarios/validate")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.errors", hasSize(1)))
            .andExpect(jsonPath("$.errors[0].field").value("unsupportedMode"))
            .andExpect(jsonPath("$.errors[0].message")
                .value("Unknown property 'unsupportedMode' is not allowed in scenario schema"));
    }

    @Test
    public void testValidateScenario_WithoutName_Succeeds() throws Exception {
        ScenarioValidateRequest request = validateRequest(ControllerApiFixtures.validScenario());

        mockMvc.perform(post("/api/v1/scenarios/validate")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    public void testValidateScenario_MissingRequiredFields_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/scenarios/validate")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fieldErrors.scenarioJson[0]").value("Scenario JSON is required"))
            .andExpect(jsonPath("$.fieldErrors.liftSystemVersionId[0]")
                .value("Lift system version ID is required"));
    }


    @Test
    public void testCopyScenario_Success() throws Exception {
        Long scenarioId = createScenario("Copy Source");

        mockMvc.perform(post("/api/v1/scenarios/{id}/copy", scenarioId)
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"targetLiftSystemVersionId\":" + version.getId() + "}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(scenarioId.intValue())))
            .andExpect(jsonPath("$.name").value("Copy of Copy Source"))
            .andExpect(jsonPath("$.liftSystemVersionId").value(version.getId()))
            .andExpect(jsonPath("$.scenarioJson.passengerFlows[0].destinationFloor").value(4));
    }

    @Test
    public void testCreateScenario_BlankName_ReturnsBadRequest() throws Exception {
        ScenarioRequest request = scenarioRequest("   ", ControllerApiFixtures.validScenario());

        mockMvc.perform(post("/api/v1/scenarios")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fieldErrors.name[0]").value("Name is required"));
    }

    @Test
    public void testCreateScenario_NameExceedsMaxLength_ReturnsBadRequest() throws Exception {
        String longName = "A".repeat(201);
        ScenarioRequest request = scenarioRequest(longName, ControllerApiFixtures.validScenario());

        mockMvc.perform(post("/api/v1/scenarios")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fieldErrors.name[0]").value("Name must not exceed 200 characters"));
    }

    @Test
    public void testCreateScenario_MissingRequiredRequestFields() throws Exception {
        mockMvc.perform(post("/api/v1/scenarios")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fieldErrors.name[0]").value("Name is required"))
            .andExpect(jsonPath("$.fieldErrors.scenarioJson[0]").value("Scenario JSON is required"))
            .andExpect(jsonPath("$.fieldErrors.liftSystemVersionId[0]")
                .value("Lift system version ID is required"));
    }

    private Long createScenario(String name) throws Exception {
        String response = mockMvc.perform(post("/api/v1/scenarios")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                    scenarioRequest(name, ControllerApiFixtures.validScenario())
                )))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    private ScenarioRequest scenarioRequest(String name, String scenarioJson) throws Exception {
        return new ScenarioRequest(name, objectMapper.readTree(scenarioJson), version.getId());
    }

    private ScenarioValidateRequest validateRequest(String scenarioJson) throws Exception {
        return new ScenarioValidateRequest(objectMapper.readTree(scenarioJson), version.getId());
    }
}
