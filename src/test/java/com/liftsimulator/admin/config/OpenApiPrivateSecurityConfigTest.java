package com.liftsimulator.admin.config;

import com.liftsimulator.LocalIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies private OpenAPI/Swagger routes require ADMIN, not just any read-only user.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = "security.openapi.public-access=false")
class OpenApiPrivateSecurityConfigTest extends LocalIntegrationTest {

    private static final String TEST_ADMIN_USER = "testadmin";
    private static final String TEST_ADMIN_PASSWORD = "testpassword";
    private static final String TEST_VIEWER_USER = "testviewer";
    private static final String TEST_VIEWER_PASSWORD = "viewerpassword";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void openApiDocs_PrivateAccess_NoAuth_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/api-docs"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string("WWW-Authenticate", containsString("Basic realm=\"Lift Simulator Admin\"")));
    }

    @Test
    void openApiDocs_PrivateAccess_ViewerRole_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/api-docs")
                .with(httpBasic(TEST_VIEWER_USER, TEST_VIEWER_PASSWORD)))
            .andExpect(status().isForbidden());
    }

    @Test
    void openApiDocs_PrivateAccess_AdminRole_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/api-docs")
                .with(httpBasic(TEST_ADMIN_USER, TEST_ADMIN_PASSWORD)))
            .andExpect(status().isOk());
    }

    @Test
    void swaggerUi_PrivateAccess_ViewerRole_Returns403() throws Exception {
        mockMvc.perform(get("/api/v1/swagger-ui.html")
                .with(httpBasic(TEST_VIEWER_USER, TEST_VIEWER_PASSWORD)))
            .andExpect(status().isForbidden());
    }
}
