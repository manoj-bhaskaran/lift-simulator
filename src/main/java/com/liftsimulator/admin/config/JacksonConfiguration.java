package com.liftsimulator.admin.config;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
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

    private static final int MAX_JSON_NESTING_DEPTH = 100;
    private static final int MAX_JSON_STRING_LENGTH = 1_048_576;

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
        StreamReadConstraints readConstraints = StreamReadConstraints.builder()
            .maxNestingDepth(MAX_JSON_NESTING_DEPTH)
            .maxStringLength(MAX_JSON_STRING_LENGTH)
            .build();
        JsonFactory jsonFactory = JsonFactory.builder()
            .streamReadConstraints(readConstraints)
            .build();

        return builder -> builder
            .factory(jsonFactory)
            .featuresToEnable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
