package com.liftsimulator.runtime.controller;

import com.liftsimulator.LocalIntegrationTest;
import com.liftsimulator.admin.service.ResourceNotFoundException;
import com.liftsimulator.runtime.dto.RuntimeConfigDTO;
import com.liftsimulator.runtime.service.RuntimeConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for runtime configuration endpoints and API-key authentication.
 */
@AutoConfigureMockMvc
class RuntimeConfigControllerTest extends LocalIntegrationTest {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String API_KEY_VALUE = "test-api-key";
    private static final String INVALID_API_KEY_VALUE = "invalid-api-key";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RuntimeConfigService runtimeConfigService;

    @Test
    void getPublishedConfigWithoutApiKeyReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/runtime/systems/test-system/config"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist("WWW-Authenticate"))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("Authentication required"))
            .andExpect(jsonPath("$.timestamp").exists());

        verifyNoInteractions(runtimeConfigService);
    }

    @Test
    void getPublishedConfigWithHttpBasicInsteadOfApiKeyReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/runtime/systems/test-system/config")
                .with(httpBasic("testadmin", "testpassword")))
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist("WWW-Authenticate"))
            .andExpect(jsonPath("$.status").value(401));

        verifyNoInteractions(runtimeConfigService);
    }

    @Test
    void getPublishedConfigWithInvalidApiKeyReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/runtime/systems/test-system/config")
                .header(API_KEY_HEADER, INVALID_API_KEY_VALUE))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("Authentication required"));

        verifyNoInteractions(runtimeConfigService);
    }

    @Test
    void getPublishedConfigWithValidApiKeyReturnsRuntimeConfig() throws Exception {
        OffsetDateTime publishedAt = OffsetDateTime.parse("2026-02-10T12:00:00Z");
        when(runtimeConfigService.getPublishedConfig("test-system"))
            .thenReturn(new RuntimeConfigDTO(
                "test-system",
                "Test System",
                2,
                "{\"minFloor\":0,\"maxFloor\":10}",
                publishedAt));

        mockMvc.perform(get("/api/v1/runtime/systems/test-system/config")
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.systemKey").value("test-system"))
            .andExpect(jsonPath("$.displayName").value("Test System"))
            .andExpect(jsonPath("$.versionNumber").value(2))
            .andExpect(jsonPath("$.config").value("{\"minFloor\":0,\"maxFloor\":10}"))
            .andExpect(jsonPath("$.publishedAt").value("2026-02-10T12:00:00Z"));

        verify(runtimeConfigService).getPublishedConfig("test-system");
    }

    @Test
    void getPublishedVersionWithValidApiKeyReturnsSpecificPublishedVersion() throws Exception {
        when(runtimeConfigService.getPublishedVersion("test-system", 3))
            .thenReturn(new RuntimeConfigDTO(
                "test-system",
                "Test System",
                3,
                "{\"controllerStrategy\":\"DIRECTIONAL_SCAN\"}",
                OffsetDateTime.parse("2026-02-11T08:30:00Z")));

        mockMvc.perform(get("/api/v1/runtime/systems/test-system/versions/3")
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.systemKey").value("test-system"))
            .andExpect(jsonPath("$.versionNumber").value(3))
            .andExpect(jsonPath("$.config").value("{\"controllerStrategy\":\"DIRECTIONAL_SCAN\"}"))
            .andExpect(jsonPath("$.publishedAt").value("2026-02-11T08:30:00Z"));

        verify(runtimeConfigService).getPublishedVersion("test-system", 3);
    }

    @Test
    void getPublishedVersionWhenServiceReportsMissingResourceReturnsNotFound() throws Exception {
        when(runtimeConfigService.getPublishedVersion("test-system", 99))
            .thenThrow(new ResourceNotFoundException(
                "Version 99 not found for lift system: test-system"));

        mockMvc.perform(get("/api/v1/runtime/systems/test-system/versions/99")
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Version 99 not found for lift system: test-system"))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void simulateEndpointIsRemoved() throws Exception {
        mockMvc.perform(post("/api/v1/runtime/systems/test-system/simulate")
                .header(API_KEY_HEADER, API_KEY_VALUE))
            .andExpect(status().isNotFound());

        verifyNoInteractions(runtimeConfigService);
    }
}
