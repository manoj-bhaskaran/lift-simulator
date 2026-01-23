package com.liftsimulator.admin.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter for JSON string columns.
 * Provides H2-compatible JSON storage for test environments.
 *
 * <p>For H2 test database compatibility, this converter handles JSON as plain text.
 * In production PostgreSQL, the database natively handles JSON validation and storage.
 *
 * <p>Note: This converter performs identity conversion (String to String) since our
 * entities store JSON as String fields. The ObjectMapper is available for validation
 * if needed, but currently we rely on application-level validation.
 */
@Converter
@Component
public class JsonStringConverter implements AttributeConverter<String, String> {

    private final ObjectMapper objectMapper;

    public JsonStringConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        // Optionally validate JSON format
        try {
            objectMapper.readTree(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON format", e);
        }
        return attribute;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData;
    }
}
