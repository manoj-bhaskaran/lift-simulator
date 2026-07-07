package com.liftsimulator.admin.controller;

import com.liftsimulator.LocalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller tests for the public health endpoint.
 */
@AutoConfigureMockMvc
class HealthControllerTest extends LocalIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthWithoutAuthenticationReturnsServiceStatus() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("WWW-Authenticate"))
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.service").value("Lift Config Service"))
            .andExpect(jsonPath("$.timestamp").value(matchesPattern(
                "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$")));
    }

    @Test
    void healthAllowsAuthenticatedAdminBecauseEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                .with(httpBasic("testadmin", "testpassword")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void healthPostWithoutAuthenticationIsNotRoutedAsAuthenticatedAdminWrite() throws Exception {
        mockMvc.perform(post("/api/v1/health"))
            .andExpect(status().isMethodNotAllowed());
    }
}
