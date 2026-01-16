package com.liftsimulator.admin.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson configuration for JSON serialization/deserialization.
 * Configures ObjectMapper to enforce strict schema validation.
 */
@Configuration
public class JacksonConfiguration {

    /**
     * Customizes the auto-configured ObjectMapper with strict validation settings.
     * Enables FAIL_ON_UNKNOWN_PROPERTIES to reject JSON with unexpected fields,
     * helping catch typos and schema violations in configuration payloads.
     *
     * <p>Uses Jackson2ObjectMapperBuilderCustomizer to customize Spring Boot's
     * auto-configured ObjectMapper rather than replacing it, preserving all
     * Spring Boot defaults while adding our strict validation.
     *
     * @return ObjectMapper customizer
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder.featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
