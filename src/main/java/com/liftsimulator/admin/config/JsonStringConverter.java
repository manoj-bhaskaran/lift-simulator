package com.liftsimulator.admin.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for JSON string columns.
 * Provides H2-compatible JSON storage for test environments with validation.
 *
 * <p>For H2 test database compatibility, this converter handles JSON as plain text.
 * In production PostgreSQL, the database natively handles JSON validation and storage.
 *
 * <p>This converter performs identity conversion (String to String) since our entities
 * store JSON as String fields, but validates JSON format before persistence to prevent
 * malformed JSON from being stored.
 *
 * <p><strong>Why Validation is Needed:</strong>
 * Not all service methods validate JSON before persistence.
 * Without converter-level validation, invalid JSON can be stored and will only fail at
 * execution time when SimulationRunExecutionService attempts to parse it, causing runtime
 * failures instead of rejecting bad data at write time.
 *
 * <p><strong>Database Behavior:</strong>
 * <ul>
 *   <li>H2 (tests): Stores as VARCHAR/CLOB - plain text storage with validation</li>
 *   <li>PostgreSQL (production): Flyway migrations create columns as JSONB for native
 *       JSON storage, indexing, and querying capabilities</li>
 * </ul>
 */
@Converter
public class JsonStringConverter implements AttributeConverter<String, String> {

    // Static ObjectMapper instance - safe because ObjectMapper is thread-safe after configuration
    // JPA converters are instantiated by JPA, not Spring, so we can't use dependency injection
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }

        // Validate JSON format before persisting
        try {
            OBJECT_MAPPER.readTree(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON format: " + e.getMessage(), e);
        }

        return attribute;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        // Pass-through on read: assume DB data is valid
        return dbData;
    }
}
