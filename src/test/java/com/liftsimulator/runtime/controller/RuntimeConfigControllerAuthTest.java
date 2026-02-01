package com.liftsimulator.runtime.controller;

import com.liftsimulator.runtime.dto.RuntimeConfigDTO;
import com.liftsimulator.runtime.dto.SimulationLaunchResponse;
import com.liftsimulator.runtime.service.RuntimeConfigService;
import com.liftsimulator.runtime.service.RuntimeSimulationService;
import com.liftsimulator.admin.security.ApiKeyAuthConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RuntimeConfigController.class)
@Import(ApiKeyAuthConfiguration.class)
@TestPropertySource(properties = {
        "api.auth.key=test-api-key",
        "api.auth.header=X-API-Key"
})
class RuntimeConfigControllerAuthTest {

    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String API_KEY_VALUE = "test-api-key";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RuntimeConfigService runtimeConfigService;

    @MockBean
    private RuntimeSimulationService runtimeSimulationService;

    @Test
    void runtimeEndpointsRequireApiKey() throws Exception {
        mockMvc.perform(get("/api/v1/runtime/systems/test-system/config"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void runtimeEndpointsAcceptValidApiKey() throws Exception {
        when(runtimeConfigService.getPublishedConfig("test-system"))
                .thenReturn(new RuntimeConfigDTO(
                        "test-system",
                        "Test System",
                        2,
                        "{\"minFloor\":0,\"maxFloor\":10}",
                        OffsetDateTime.parse("2026-02-10T12:00:00Z")));

        mockMvc.perform(get("/api/v1/runtime/systems/test-system/config")
                        .header(API_KEY_HEADER, API_KEY_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemKey").value("test-system"))
                .andExpect(jsonPath("$.versionNumber").value(2));
    }

    @Test
    void simulateEndpointAcceptsValidApiKey() throws Exception {
        when(runtimeSimulationService.launchPublishedSimulation("test-system"))
                .thenReturn(new SimulationLaunchResponse(true, "started", 42L));

        mockMvc.perform(post("/api/v1/runtime/systems/test-system/simulate")
                        .header(API_KEY_HEADER, API_KEY_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.processId").value(42));
    }
}
