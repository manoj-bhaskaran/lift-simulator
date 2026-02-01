package com.liftsimulator.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftsimulator.LocalIntegrationTest;
import com.liftsimulator.admin.dto.CreateLiftSystemRequest;
import com.liftsimulator.admin.dto.UpdateLiftSystemRequest;
import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.repository.LiftSystemRepository;
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
 * Integration tests for LiftSystemController.
 */
@AutoConfigureMockMvc
@Transactional
public class LiftSystemControllerTest extends LocalIntegrationTest {

    // Test credentials from application-test.yml
    private static final String TEST_ADMIN_USER = "testadmin";
    private static final String TEST_ADMIN_PASSWORD = "testpassword";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LiftSystemRepository liftSystemRepository;

    @BeforeEach
    public void setUp() {
        liftSystemRepository.deleteAll();
    }

    @Test
    public void testCreateLiftSystem_Success() throws Exception {
        CreateLiftSystemRequest request = new CreateLiftSystemRequest(
            "test-system-1",
            "Test System 1",
            "Test Description"
        );

        mockMvc.perform(post("/api/lift-systems")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.systemKey").value("test-system-1"))
            .andExpect(jsonPath("$.displayName").value("Test System 1"))
            .andExpect(jsonPath("$.description").value("Test Description"))
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    public void testCreateLiftSystem_DuplicateKey() throws Exception {
        LiftSystem existing = new LiftSystem("existing-system", "Existing", "Description");
        liftSystemRepository.save(existing);

        CreateLiftSystemRequest request = new CreateLiftSystemRequest(
            "existing-system",
            "Duplicate System",
            "Description"
        );

        mockMvc.perform(post("/api/lift-systems")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Lift system with key 'existing-system' already exists"));
    }

    @Test
    public void testCreateLiftSystem_ValidationError() throws Exception {
        CreateLiftSystemRequest request = new CreateLiftSystemRequest(
            "",  // Empty system key
            "",  // Empty display name
            "Description"
        );

        mockMvc.perform(post("/api/lift-systems")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fieldErrors").exists());
    }

    @Test
    public void testGetAllLiftSystems() throws Exception {
        liftSystemRepository.save(new LiftSystem("system-1", "System 1", "Description 1"));
        liftSystemRepository.save(new LiftSystem("system-2", "System 2", "Description 2"));

        mockMvc.perform(get("/api/lift-systems")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].systemKey").exists())
            .andExpect(jsonPath("$[1].systemKey").exists());
    }

    @Test
    public void testGetLiftSystemById_Success() throws Exception {
        LiftSystem system = new LiftSystem("test-system", "Test System", "Test Description");
        LiftSystem saved = liftSystemRepository.save(system);

        mockMvc.perform(get("/api/lift-systems/{id}", saved.getId())
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(saved.getId()))
            .andExpect(jsonPath("$.systemKey").value("test-system"))
            .andExpect(jsonPath("$.displayName").value("Test System"))
            .andExpect(jsonPath("$.description").value("Test Description"));
    }

    @Test
    public void testGetLiftSystemById_NotFound() throws Exception {
        mockMvc.perform(get("/api/lift-systems/{id}", 999L)
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Lift system not found with id: 999"));
    }

    @Test
    public void testUpdateLiftSystem_Success() throws Exception {
        LiftSystem system = new LiftSystem("test-system", "Original Name", "Original Description");
        LiftSystem saved = liftSystemRepository.save(system);

        UpdateLiftSystemRequest request = new UpdateLiftSystemRequest(
            "Updated Name",
            "Updated Description"
        );

        mockMvc.perform(put("/api/lift-systems/{id}", saved.getId())
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(saved.getId()))
            .andExpect(jsonPath("$.systemKey").value("test-system"))  // Key should not change
            .andExpect(jsonPath("$.displayName").value("Updated Name"))
            .andExpect(jsonPath("$.description").value("Updated Description"));
    }

    @Test
    public void testUpdateLiftSystem_NotFound() throws Exception {
        UpdateLiftSystemRequest request = new UpdateLiftSystemRequest(
            "Updated Name",
            "Updated Description"
        );

        mockMvc.perform(put("/api/lift-systems/{id}", 999L)
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Lift system not found with id: 999"));
    }

    @Test
    public void testDeleteLiftSystem_Success() throws Exception {
        LiftSystem system = new LiftSystem("test-system", "Test System", "Test Description");
        LiftSystem saved = liftSystemRepository.save(system);

        mockMvc.perform(delete("/api/lift-systems/{id}", saved.getId())
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isNoContent());

        // Verify deletion
        mockMvc.perform(get("/api/lift-systems/{id}", saved.getId())
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isNotFound());
    }

    @Test
    public void testDeleteLiftSystem_NotFound() throws Exception {
        mockMvc.perform(delete("/api/lift-systems/{id}", 999L)
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Lift system not found with id: 999"));
    }
}
