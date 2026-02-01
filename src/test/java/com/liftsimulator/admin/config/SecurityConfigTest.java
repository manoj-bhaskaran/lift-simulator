package com.liftsimulator.admin.config;

import com.liftsimulator.LocalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Spring Security configuration.
 * Tests authentication and authorization for admin and runtime APIs.
 *
 * <p>Test coverage includes:
 * <ul>
 *   <li>Authentication (HTTP Basic, API Key)</li>
 *   <li>Role-based access control (ADMIN vs VIEWER roles)</li>
 *   <li>Authorization failures (HTTP 403 responses)</li>
 *   <li>Error response format consistency</li>
 * </ul>
 */
@AutoConfigureMockMvc
public class SecurityConfigTest extends LocalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // Test credentials from application-test.yml
    private static final String TEST_ADMIN_USER = "testadmin";
    private static final String TEST_ADMIN_PASSWORD = "testpassword";
    private static final String TEST_VIEWER_USER = "testviewer";
    private static final String TEST_VIEWER_PASSWORD = "viewerpassword";
    private static final String TEST_API_KEY = "test-api-key";

    // ========== Health Endpoint Tests ==========

    @Test
    void healthEndpoint_NoAuth_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    // ========== CORS Tests ==========

    @Test
    void corsPreflight_AllowsConfiguredOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/lift-systems")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
            .andExpect(header().string("Access-Control-Allow-Methods", containsString("GET")))
            .andExpect(header().string("Access-Control-Allow-Headers", containsString("Authorization")));
    }

    @Test
    void corsPreflight_RejectsUnknownOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/lift-systems")
                .header("Origin", "https://invalid.example.com")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isForbidden());
    }

    @Test
    void csrfDisabled_AllowsStateChangingRequestWithoutToken() throws Exception {
        String validSystemJson = """
            {
                "systemKey": "test-csrf-disabled-system",
                "displayName": "Test CSRF Disabled System",
                "description": "A test system to confirm CSRF policy"
            }
            """;

        mockMvc.perform(post("/api/v1/lift-systems")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validSystemJson))
            .andExpect(status().isCreated());
    }

    // ========== Admin API Authentication Tests ==========

    @Test
    void adminApi_NoAuth_Returns401WithWwwAuthenticateHeader() throws Exception {
        mockMvc.perform(get("/api/v1/lift-systems"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string("WWW-Authenticate", containsString("Basic realm=\"Lift Simulator Admin\"")))
            .andExpect(jsonPath("$.status", is(401)))
            .andExpect(jsonPath("$.message", is("Authentication required")))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void adminApi_InvalidCredentials_Returns401WithWwwAuthenticateHeader() throws Exception {
        mockMvc.perform(get("/api/v1/lift-systems")
                .with(httpBasic("wronguser", "wrongpassword")))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string("WWW-Authenticate", containsString("Basic realm=\"Lift Simulator Admin\"")))
            .andExpect(jsonPath("$.status", is(401)))
            .andExpect(jsonPath("$.message", is("Authentication required")));
    }

    @Test
    void adminApi_ValidCredentials_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/lift-systems")
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

        mockMvc.perform(post("/api/v1/lift-systems")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validSystemJson))
            .andExpect(status().isCreated());
    }

    // ========== Role-Based Access Control (RBAC) Tests ==========

    @Test
    void viewerRole_GetRequest_ReturnsOk() throws Exception {
        // VIEWER role should be able to perform GET requests
        mockMvc.perform(get("/api/v1/lift-systems")
                .with(httpBasic(TEST_VIEWER_USER, TEST_VIEWER_PASSWORD)))
            .andExpect(status().isOk());
    }

    @Test
    void viewerRole_PostRequest_Returns403() throws Exception {
        // VIEWER role should NOT be able to perform POST requests
        String validSystemJson = """
            {
                "systemKey": "test-viewer-system",
                "displayName": "Test Viewer System",
                "description": "A test system for viewer role tests"
            }
            """;

        mockMvc.perform(post("/api/v1/lift-systems")
                .with(httpBasic(TEST_VIEWER_USER, TEST_VIEWER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(validSystemJson))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status", is(403)))
            .andExpect(jsonPath("$.message", is("Access denied")))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void viewerRole_PutRequest_Returns403() throws Exception {
        // VIEWER role should NOT be able to perform PUT requests
        String updateJson = """
            {
                "displayName": "Updated Name",
                "description": "Updated description"
            }
            """;

        mockMvc.perform(put("/api/v1/lift-systems/999")
                .with(httpBasic(TEST_VIEWER_USER, TEST_VIEWER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status", is(403)))
            .andExpect(jsonPath("$.message", is("Access denied")));
    }

    @Test
    void viewerRole_DeleteRequest_Returns403() throws Exception {
        // VIEWER role should NOT be able to perform DELETE requests
        mockMvc.perform(delete("/api/v1/lift-systems/999")
                .with(httpBasic(TEST_VIEWER_USER, TEST_VIEWER_PASSWORD)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status", is(403)))
            .andExpect(jsonPath("$.message", is("Access denied")));
    }

    @Test
    void adminRole_GetRequest_ReturnsOk() throws Exception {
        // ADMIN role should be able to perform GET requests
        mockMvc.perform(get("/api/v1/lift-systems")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isOk());
    }

    @Test
    void adminRole_DeleteRequest_AllowedButNotFound() throws Exception {
        // ADMIN role should be able to attempt DELETE (404 is expected for non-existent resource)
        mockMvc.perform(delete("/api/v1/lift-systems/999")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isNotFound());
    }

    @Test
    void viewerRole_ScenarioPostRequest_Returns403() throws Exception {
        // VIEWER cannot create scenarios
        String scenarioJson = """
            {
                "name": "Test Scenario",
                "description": "Test scenario for RBAC",
                "scenario": {"passengers": []}
            }
            """;

        mockMvc.perform(post("/api/v1/scenarios")
                .with(httpBasic(TEST_VIEWER_USER, TEST_VIEWER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(scenarioJson))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status", is(403)));
    }

    @Test
    void viewerRole_ScenarioGetRequest_ReturnsOk() throws Exception {
        // VIEWER can list scenarios
        mockMvc.perform(get("/api/v1/scenarios")
                .with(httpBasic(TEST_VIEWER_USER, TEST_VIEWER_PASSWORD)))
            .andExpect(status().isOk());
    }

    @Test
    void forbiddenResponse_HasConsistentFormat() throws Exception {
        // Verify 403 response format matches the error response structure
        mockMvc.perform(post("/api/v1/lift-systems")
                .with(httpBasic(TEST_VIEWER_USER, TEST_VIEWER_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").isNumber())
            .andExpect(jsonPath("$.message").isString())
            .andExpect(jsonPath("$.timestamp").isString());
    }

    // ========== Runtime API Authentication Tests ==========

    @Test
    void runtimeApi_NoApiKey_Returns401WithoutWwwAuthenticateHeader() throws Exception {
        mockMvc.perform(get("/api/v1/runtime/systems/test-system/config"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist("WWW-Authenticate"))
            .andExpect(jsonPath("$.status", is(401)))
            .andExpect(jsonPath("$.message", is("Authentication required")));
    }

    @Test
    void runtimeApi_InvalidApiKey_Returns401WithoutWwwAuthenticateHeader() throws Exception {
        mockMvc.perform(get("/api/v1/runtime/systems/test-system/config")
                .header("X-API-Key", "invalid-key"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist("WWW-Authenticate"))
            .andExpect(jsonPath("$.status", is(401)))
            .andExpect(jsonPath("$.message", is("Authentication required")));
    }

    @Test
    void runtimeApi_ValidApiKey_ResourceNotFound_Returns404() throws Exception {
        // Valid API key but non-existent system
        mockMvc.perform(get("/api/v1/runtime/systems/nonexistent-system/config")
                .header("X-API-Key", TEST_API_KEY))
            .andExpect(status().isNotFound());
    }

    @Test
    void runtimeApi_HttpBasicNotAccepted_Returns401() throws Exception {
        // Runtime API should not accept HTTP Basic auth
        mockMvc.perform(get("/api/v1/runtime/systems/test-system/config")
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
        mockMvc.perform(get("/api/v1/lift-systems"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").isNumber())
            .andExpect(jsonPath("$.message").isString())
            .andExpect(jsonPath("$.timestamp").isString());
    }

    // ========== Admin API with API Key (Should Not Work) ==========

    @Test
    void adminApi_ApiKeyNotAccepted_Returns401() throws Exception {
        // Admin API should not accept API key auth
        mockMvc.perform(get("/api/v1/lift-systems")
                .header("X-API-Key", TEST_API_KEY))
            .andExpect(status().isUnauthorized());
    }
}
