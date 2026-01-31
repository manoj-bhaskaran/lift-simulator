package com.liftsimulator.admin.config;

import com.liftsimulator.LocalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Spring Security configuration.
 * Tests authentication and authorization for admin and runtime APIs.
 */
@AutoConfigureMockMvc
public class SecurityConfigTest extends LocalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // Test credentials from application-test.yml
    private static final String TEST_ADMIN_USER = "testadmin";
    private static final String TEST_ADMIN_PASSWORD = "testpassword";
    private static final String TEST_API_KEY = "test-api-key-12345";

    // ========== Health Endpoint Tests ==========

    @Test
    void healthEndpoint_NoAuth_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    // ========== Admin API Authentication Tests ==========

    @Test
    void adminApi_NoAuth_Returns401() throws Exception {
        mockMvc.perform(get("/api/lift-systems"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status", is(401)))
            .andExpect(jsonPath("$.message", is("Authentication required")))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void adminApi_InvalidCredentials_Returns401() throws Exception {
        mockMvc.perform(get("/api/lift-systems")
                .with(httpBasic("wronguser", "wrongpassword")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status", is(401)))
            .andExpect(jsonPath("$.message", is("Authentication required")));
    }

    @Test
    void adminApi_ValidCredentials_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/lift-systems")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isOk());
    }

    @Test
    void adminApi_ValidCredentials_PostRequest_ReturnsOk() throws Exception {
        String validSystemJson = """
            {
                "systemKey": "test-security-system",
                "displayName": "Test Security System",
                "description": "A test system for security tests"
            }
            """;

        mockMvc.perform(post("/api/lift-systems")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validSystemJson))
            .andExpect(status().isCreated());
    }

    // ========== Runtime API Authentication Tests ==========

    @Test
    void runtimeApi_NoApiKey_Returns401() throws Exception {
        mockMvc.perform(get("/api/runtime/systems/test-system/config"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status", is(401)))
            .andExpect(jsonPath("$.message", is("Authentication required")));
    }

    @Test
    void runtimeApi_InvalidApiKey_Returns401() throws Exception {
        mockMvc.perform(get("/api/runtime/systems/test-system/config")
                .header("X-API-Key", "invalid-key"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status", is(401)))
            .andExpect(jsonPath("$.message", is("Authentication required")));
    }

    @Test
    void runtimeApi_ValidApiKey_ResourceNotFound_Returns404() throws Exception {
        // Valid API key but non-existent system
        mockMvc.perform(get("/api/runtime/systems/nonexistent-system/config")
                .header("X-API-Key", TEST_API_KEY))
            .andExpect(status().isNotFound());
    }

    @Test
    void runtimeApi_HttpBasicNotAccepted_Returns401() throws Exception {
        // Runtime API should not accept HTTP Basic auth
        mockMvc.perform(get("/api/runtime/systems/test-system/config")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isUnauthorized());
    }

    // ========== Actuator Endpoint Tests ==========

    @Test
    void actuatorHealth_NoAuth_ReturnsOk() throws Exception {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());
    }

    @Test
    void actuatorInfo_NoAuth_ReturnsOk() throws Exception {
        mockMvc.perform(get("/actuator/info"))
            .andExpect(status().isOk());
    }

    // ========== Error Response Format Tests ==========

    @Test
    void unauthenticatedRequest_ReturnsConsistentErrorFormat() throws Exception {
        mockMvc.perform(get("/api/lift-systems"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").isNumber())
            .andExpect(jsonPath("$.message").isString())
            .andExpect(jsonPath("$.timestamp").isString());
    }

    // ========== Admin API with API Key (Should Not Work) ==========

    @Test
    void adminApi_ApiKeyNotAccepted_Returns401() throws Exception {
        // Admin API should not accept API key auth
        mockMvc.perform(get("/api/lift-systems")
                .header("X-API-Key", TEST_API_KEY))
            .andExpect(status().isUnauthorized());
    }
}
