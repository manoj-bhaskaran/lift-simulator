package com.liftsimulator.admin.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger configuration for the Lift Simulator API.
 * Provides API documentation accessible via Swagger UI and OpenAPI spec endpoint.
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:lift-config-service}")
    private String applicationName;

    /**
     * Configures the OpenAPI specification with API metadata and security schemes.
     *
     * @return configured OpenAPI instance
     */
    @Bean
    public OpenAPI liftSimulatorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Lift Simulator API")
                        .description("REST API for managing lift systems, scenarios, and simulation runs. "
                                + "The Lift Simulator provides endpoints for creating and managing lift "
                                + "configurations, running simulations, and retrieving results.")
                        .version("0.46.0")
                        .contact(new Contact()
                                .name("Lift Simulator Team")
                                .url("https://github.com/manoj-bhaskaran/lift-simulator"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://github.com/manoj-bhaskaran/lift-simulator/blob/main/LICENSE")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local development server"),
                        new Server()
                                .url("/")
                                .description("Current server")))
                .components(new Components()
                        .addSecuritySchemes("basicAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description("HTTP Basic Authentication for admin endpoints. "
                                        + "Use configured admin username and password."))
                        .addSecuritySchemes("apiKey", new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-API-Key")
                                .description("API Key authentication for runtime and simulation endpoints. "
                                        + "Provide the configured API key in the X-API-Key header.")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"))
                .addSecurityItem(new SecurityRequirement().addList("apiKey"));
    }
}
