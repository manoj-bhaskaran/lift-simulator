package com.liftsimulator.admin.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson configuration for JSON serialization/deserialization.
 * Configures ObjectMapper to enforce strict schema validation.
 */
@Configuration
public class JacksonConfiguration {

    /**
     * Configures the primary ObjectMapper bean with strict validation settings.
     * Enables FAIL_ON_UNKNOWN_PROPERTIES to reject JSON with unexpected fields,
     * helping catch typos and schema violations in configuration payloads.
     *
     * @return Configured ObjectMapper instance
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Enforce strict schema validation - reject unknown properties
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        return mapper;
    }
}
