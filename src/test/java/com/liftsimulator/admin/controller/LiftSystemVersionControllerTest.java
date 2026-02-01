package com.liftsimulator.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftsimulator.LocalIntegrationTest;
import com.liftsimulator.admin.dto.CreateVersionRequest;
import com.liftsimulator.admin.dto.UpdateVersionConfigRequest;
import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.repository.LiftSystemRepository;
import com.liftsimulator.admin.repository.LiftSystemVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for LiftSystemVersionController.
 */
@AutoConfigureMockMvc
@Transactional
public class LiftSystemVersionControllerTest extends LocalIntegrationTest {

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

    private LiftSystem testSystem;

    @BeforeEach
    public void setUp() {
        versionRepository.deleteAll();
        liftSystemRepository.deleteAll();

        testSystem = new LiftSystem("test-system", "Test System", "Test Description");
        testSystem = liftSystemRepository.save(testSystem);
    }

    /**
     * Helper method to create a valid lift configuration JSON.
     */
    private String validConfig() {
        return """
            {
                "minFloor": 0,
                "maxFloor": 9,
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
            """.trim();
    }

    /**
     * Helper method to create a valid lift configuration JSON with custom floor range.
     */
    private String validConfigWithRange(int minFloor, int maxFloor) {
        return String.format("""
            {
                "minFloor": %d,
                "maxFloor": %d,
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
            """.trim(), minFloor, maxFloor);
    }

    @Test
    public void testCreateVersion_WithConfig() throws Exception {
        String config = validConfig();
        CreateVersionRequest request = new CreateVersionRequest(config, null);

        mockMvc.perform(post("/api/lift-systems/{systemId}/versions", testSystem.getId())
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.liftSystemId").value(testSystem.getId()))
            .andExpect(jsonPath("$.versionNumber").value(1))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.isPublished").value(false))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    public void testCreateVersion_CloneFromExisting() throws Exception {
        String originalConfig = validConfig();
        LiftSystemVersion version1 = new LiftSystemVersion(testSystem, 1, originalConfig);
        versionRepository.save(version1);

        CreateVersionRequest request = new CreateVersionRequest("{}", 1);

        mockMvc.perform(post("/api/lift-systems/{systemId}/versions", testSystem.getId())
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.versionNumber").value(2));
    }

    @Test
    public void testCreateVersion_CloneFromNonExistentVersion() throws Exception {
        CreateVersionRequest request = new CreateVersionRequest("{}", 999);

        mockMvc.perform(post("/api/lift-systems/{systemId}/versions", testSystem.getId())
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Version 999 not found for lift system " + testSystem.getId()));
    }

    @Test
    public void testCreateVersion_NonExistentSystem() throws Exception {
        String config = validConfig();
        CreateVersionRequest request = new CreateVersionRequest(config, null);

        mockMvc.perform(post("/api/lift-systems/{systemId}/versions", 999L)
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Lift system not found with id: 999"));
    }

    @Test
    public void testCreateVersion_VersionNumberAutoIncrement() throws Exception {
        versionRepository.save(new LiftSystemVersion(testSystem, 1, validConfig()));
        versionRepository.save(new LiftSystemVersion(testSystem, 2, validConfig()));

        CreateVersionRequest request = new CreateVersionRequest(validConfigWithRange(0, 14), null);

        mockMvc.perform(post("/api/lift-systems/{systemId}/versions", testSystem.getId())
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.versionNumber").value(3));
    }

    @Test
    public void testCreateVersion_ValidationError_EmptyConfig() throws Exception {
        CreateVersionRequest request = new CreateVersionRequest("", null);

        mockMvc.perform(post("/api/lift-systems/{systemId}/versions", testSystem.getId())
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    public void testUpdateVersionConfig_Success() throws Exception {
        String originalConfig = validConfig();
        LiftSystemVersion version = new LiftSystemVersion(testSystem, 1, originalConfig);
        versionRepository.save(version);

        String updatedConfig = validConfigWithRange(0, 14);
        UpdateVersionConfigRequest request = new UpdateVersionConfigRequest(updatedConfig);

        mockMvc.perform(put("/api/lift-systems/{systemId}/versions/{versionNumber}",
                testSystem.getId(), 1)
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.versionNumber").value(1));
    }

    @Test
    public void testUpdateVersionConfig_VersionNotFound() throws Exception {
        UpdateVersionConfigRequest request = new UpdateVersionConfigRequest(validConfigWithRange(0, 14));

        mockMvc.perform(put("/api/lift-systems/{systemId}/versions/{versionNumber}",
                testSystem.getId(), 999)
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Version 999 not found for lift system " + testSystem.getId()));
    }

    @Test
    public void testUpdateVersionConfig_ValidationError_EmptyConfig() throws Exception {
        versionRepository.save(new LiftSystemVersion(testSystem, 1, validConfig()));

        UpdateVersionConfigRequest request = new UpdateVersionConfigRequest("");

        mockMvc.perform(put("/api/lift-systems/{systemId}/versions/{versionNumber}",
                testSystem.getId(), 1)
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    public void testListVersions_Success() throws Exception {
        versionRepository.save(new LiftSystemVersion(testSystem, 1, validConfig()));
        versionRepository.save(new LiftSystemVersion(testSystem, 2, validConfigWithRange(0, 14)));
        versionRepository.save(new LiftSystemVersion(testSystem, 3, validConfigWithRange(0, 19)));

        mockMvc.perform(get("/api/lift-systems/{systemId}/versions", testSystem.getId())
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].versionNumber").value(3))
            .andExpect(jsonPath("$[1].versionNumber").value(2))
            .andExpect(jsonPath("$[2].versionNumber").value(1));
    }

    @Test
    public void testListVersions_EmptyList() throws Exception {
        mockMvc.perform(get("/api/lift-systems/{systemId}/versions", testSystem.getId())
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    public void testListVersions_NonExistentSystem() throws Exception {
        mockMvc.perform(get("/api/lift-systems/{systemId}/versions", 999L)
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Lift system not found with id: 999"));
    }

    @Test
    public void testGetVersion_Success() throws Exception {
        String config = validConfig();
        LiftSystemVersion version = new LiftSystemVersion(testSystem, 1, config);
        versionRepository.save(version);

        mockMvc.perform(get("/api/lift-systems/{systemId}/versions/{versionNumber}",
                testSystem.getId(), 1)
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.liftSystemId").value(testSystem.getId()))
            .andExpect(jsonPath("$.versionNumber").value(1))
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.isPublished").value(false));
    }

    @Test
    public void testGetVersion_NotFound() throws Exception {
        mockMvc.perform(get("/api/lift-systems/{systemId}/versions/{versionNumber}",
                testSystem.getId(), 999)
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Version 999 not found for lift system " + testSystem.getId()));
    }

    @Test
    public void testVersionWorkflow_CreateMultipleVersionsAndRetrieve() throws Exception {
        CreateVersionRequest request1 = new CreateVersionRequest(validConfig(), null);
        mockMvc.perform(post("/api/lift-systems/{systemId}/versions", testSystem.getId())
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.versionNumber").value(1));

        CreateVersionRequest request2 = new CreateVersionRequest("{}", 1);
        mockMvc.perform(post("/api/lift-systems/{systemId}/versions", testSystem.getId())
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.versionNumber").value(2));

        UpdateVersionConfigRequest updateRequest = new UpdateVersionConfigRequest(validConfigWithRange(0, 19));
        mockMvc.perform(put("/api/lift-systems/{systemId}/versions/{versionNumber}",
                testSystem.getId(), 2)
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/lift-systems/{systemId}/versions", testSystem.getId())
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));
    }
}
