package com.liftsimulator.admin.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the checked-in base OpenAPI access setting stays private by default
 * while remaining overridable via environment variables.
 */
class OpenApiBaseConfigurationTest {

    @Test
    void baseApplicationYaml_DefaultsOpenApiToPrivateWithEnvironmentOverrideHook() throws IOException {
        try (InputStream applicationYamlResource = getClass().getResourceAsStream("/application.yml")) {
            assertThat(applicationYamlResource).isNotNull();
            String applicationYaml = new String(applicationYamlResource.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(applicationYaml)
                .contains("public-access: ${SECURITY_OPENAPI_PUBLIC_ACCESS:false}")
                .doesNotContain("public-access: true");
        }
    }
}
