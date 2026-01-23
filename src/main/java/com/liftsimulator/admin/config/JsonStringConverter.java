package com.liftsimulator.admin.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA AttributeConverter for JSON string columns.
 * Provides H2-compatible JSON storage for test environments.
 *
 * <p>For H2 test database compatibility, this converter handles JSON as plain text.
 * In production PostgreSQL, the database natively handles JSON validation and storage.
 *
 * <p>This converter performs identity conversion (String to String) since our entities
 * store JSON as String fields. JSON validation is performed at the service layer before
 * persistence.
 *
 * <p><strong>Database Behavior:</strong>
 * <ul>
 *   <li>H2 (tests): Stores as VARCHAR/CLOB - plain text storage</li>
 *   <li>PostgreSQL (production): Flyway migrations create columns as JSONB for native
 *       JSON storage, indexing, and querying capabilities</li>
 * </ul>
 */
@Converter
public class JsonStringConverter implements AttributeConverter<String, String> {

    @Override
    public String convertToDatabaseColumn(String attribute) {
        // Pass-through: JSON validation happens at service layer
        return attribute;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        // Pass-through: Return as-is
        return dbData;
    }
}
