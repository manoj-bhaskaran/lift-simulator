package com.liftsimulator.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftsimulator.LocalIntegrationTest;
import com.liftsimulator.admin.controller.fixtures.ControllerApiFixtures;
import com.liftsimulator.admin.dto.ConfigValidationRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller-level coverage for configuration validation request handling and error responses.
 */
@AutoConfigureMockMvc
@Transactional
public class ConfigValidationControllerTest extends LocalIntegrationTest {

    private static final String TEST_ADMIN_USER = "testadmin";
    private static final String TEST_ADMIN_PASSWORD = "testpassword";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testValidateConfig_ValidConfig_ReturnsSuccessBody() throws Exception {
        ConfigValidationRequest request = new ConfigValidationRequest(
            ControllerApiFixtures.validLiftConfig()
        );

        mockMvc.perform(post("/api/v1/config/validate")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true))
            .andExpect(jsonPath("$.errors", hasSize(0)))
            .andExpect(jsonPath("$.warnings", hasSize(0)));
    }

    @Test
    public void testValidateConfig_DomainErrors_ReturnsErrorIssues() throws Exception {
        ConfigValidationRequest request = new ConfigValidationRequest(
            ControllerApiFixtures.configWithDomainErrors()
        );

        mockMvc.perform(post("/api/v1/config/validate")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.errors", hasSize(3)))
            .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder(
                "doorReopenWindowTicks",
                "maxFloor",
                "homeFloor"
            )))
            .andExpect(jsonPath("$.errors[*].severity", containsInAnyOrder(
                "ERROR",
                "ERROR",
                "ERROR"
            )));
    }

    @Test
    public void testValidateConfig_UnknownProperty_ReturnsSchemaError() throws Exception {
        ConfigValidationRequest request = new ConfigValidationRequest(
            ControllerApiFixtures.configWithUnknownProperty()
        );

        mockMvc.perform(post("/api/v1/config/validate")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.errors", hasSize(1)))
            .andExpect(jsonPath("$.errors[0].field").value("unexpectedField"))
            .andExpect(jsonPath("$.errors[0].message")
                .value("Unknown property 'unexpectedField' is not allowed in configuration schema"))
            .andExpect(jsonPath("$.errors[0].severity").value("ERROR"));
    }

    @Test
    public void testValidateConfig_MalformedJsonString_ReturnsParseError() throws Exception {
        ConfigValidationRequest request = new ConfigValidationRequest("{\"minFloor\": 0,");

        mockMvc.perform(post("/api/v1/config/validate")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.errors", hasSize(1)))
            .andExpect(jsonPath("$.errors[0].field").value("config"))
            .andExpect(jsonPath("$.errors[0].message").value(org.hamcrest.Matchers.startsWith(
                "Invalid JSON format: Unexpected end-of-input"
            )))
            .andExpect(jsonPath("$.errors[0].severity").value("ERROR"));
    }

    @Test
    public void testValidateConfig_BlankConfig_ReturnsRequestValidationError() throws Exception {
        ConfigValidationRequest request = new ConfigValidationRequest(" ");

        mockMvc.perform(post("/api/v1/config/validate")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Validation failed"))
            .andExpect(jsonPath("$.fieldErrors.config").value("Config JSON is required"));
    }
}
