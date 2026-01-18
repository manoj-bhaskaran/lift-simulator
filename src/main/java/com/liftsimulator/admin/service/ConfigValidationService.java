package com.liftsimulator.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.liftsimulator.admin.dto.ConfigValidationResponse;
import com.liftsimulator.admin.dto.LiftConfigDTO;
import com.liftsimulator.admin.dto.ValidationIssue;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service for validating lift system configuration JSON.
 * Performs both structural (JSON parsing, required fields) and domain (business rules) validation.
 */
@Service
public class ConfigValidationService {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring-managed beans (ObjectMapper, Validator) injected via constructor. "
                    + "Lifecycle and immutability managed by Spring container."
    )
    public ConfigValidationService(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    /**
     * Validates a configuration JSON string.
     *
     * @param configJson The configuration JSON to validate
     * @return ConfigValidationResponse containing validation results
     */
    public ConfigValidationResponse validate(String configJson) {
        List<ValidationIssue> errors = new ArrayList<>();
        List<ValidationIssue> warnings = new ArrayList<>();

        // Step 1: Parse JSON and perform structural validation
        LiftConfigDTO config;
        try {
            config = objectMapper.readValue(configJson, LiftConfigDTO.class);
        } catch (UnrecognizedPropertyException e) {
            // Specific handling for unknown properties to provide clearer error messages
            String fieldName = e.getPropertyName();
            errors.add(new ValidationIssue(
                fieldName,
                "Unknown property '" + fieldName + "' is not allowed in configuration schema",
                ValidationIssue.Severity.ERROR
            ));
            return new ConfigValidationResponse(false, errors, warnings);
        } catch (InvalidFormatException e) {
            // Specific handling for type mismatch errors (e.g., string when number expected)
            String fieldName = getFieldNameFromPath(e.getPath());
            String targetType = e.getTargetType().getSimpleName();
            String value = e.getValue() != null ? e.getValue().toString() : "null";

            String errorMessage;
            if (targetType.equals("Integer")) {
                errorMessage = "Field '" + fieldName + "' must be a numeric value, got '" + value + "'";
            } else if (targetType.equals("ControllerStrategy") || targetType.equals("IdleParkingMode")) {
                errorMessage = "Field '" + fieldName + "' has invalid value '" + value + "'";
            } else {
                errorMessage = "Field '" + fieldName + "' has invalid format. Expected type: " + targetType;
            }

            errors.add(new ValidationIssue(
                fieldName,
                errorMessage,
                ValidationIssue.Severity.ERROR
            ));
            return new ConfigValidationResponse(false, errors, warnings);
        } catch (MismatchedInputException e) {
            // Handles cases where the input type doesn't match expected type (e.g., boolean for Integer)
            String fieldName = getFieldNameFromPath(e.getPath());
            String targetType = e.getTargetType().getSimpleName();

            String errorMessage;
            if (targetType.equals("Integer")) {
                errorMessage = "Field '" + fieldName + "' must be a numeric value";
            } else {
                errorMessage = "Field '" + fieldName + "' has incorrect type. Expected: " + targetType;
            }

            errors.add(new ValidationIssue(
                fieldName,
                errorMessage,
                ValidationIssue.Severity.ERROR
            ));
            return new ConfigValidationResponse(false, errors, warnings);
        } catch (JsonProcessingException e) {
            errors.add(new ValidationIssue(
                "config",
                "Invalid JSON format: " + e.getOriginalMessage(),
                ValidationIssue.Severity.ERROR
            ));
            return new ConfigValidationResponse(false, errors, warnings);
        }

        // Step 2: Perform Jakarta Bean Validation (structural validation)
        Set<ConstraintViolation<LiftConfigDTO>> violations = validator.validate(config);
        for (ConstraintViolation<LiftConfigDTO> violation : violations) {
            errors.add(new ValidationIssue(
                violation.getPropertyPath().toString(),
                violation.getMessage(),
                ValidationIssue.Severity.ERROR
            ));
        }

        // Step 3: Perform domain validation (cross-field validation)
        performDomainValidation(config, errors, warnings);

        // Determine if configuration is valid
        boolean valid = errors.isEmpty();

        return new ConfigValidationResponse(valid, errors, warnings);
    }

    /**
     * Extracts the field name from Jackson's JsonMappingException path.
     *
     * @param path The path from the JsonMappingException
     * @return The field name, or "config" if path is empty
     */
    private String getFieldNameFromPath(List<com.fasterxml.jackson.core.JsonProcessingException.Reference> path) {
        if (path == null || path.isEmpty()) {
            return "config";
        }
        // Get the last reference in the path (most specific field)
        com.fasterxml.jackson.core.JsonProcessingException.Reference lastRef = path.get(path.size() - 1);
        String fieldName = lastRef.getFieldName();
        return fieldName != null ? fieldName : "config";
    }

    /**
     * Performs domain-specific validation rules that span multiple fields.
     * Only performs checks if all required fields are non-null.
     *
     * @param config The parsed configuration DTO
     * @param errors List to add error issues to
     * @param warnings List to add warning issues to
     */
    private void performDomainValidation(LiftConfigDTO config, List<ValidationIssue> errors, List<ValidationIssue> warnings) {
        // Skip domain validation if structural validation failed (null fields present)
        if (config.doorReopenWindowTicks() == null || config.doorTransitionTicks() == null ||
            config.floors() == null || config.homeFloor() == null ||
            config.idleParkingMode() == null || config.idleTimeoutTicks() == null ||
            config.doorDwellTicks() == null || config.lifts() == null) {
            return; // Structural validation errors already reported
        }

        // Validate doorReopenWindowTicks <= doorTransitionTicks
        if (config.doorReopenWindowTicks() > config.doorTransitionTicks()) {
            errors.add(new ValidationIssue(
                "doorReopenWindowTicks",
                "Door reopen window ticks (" + config.doorReopenWindowTicks() +
                ") must not exceed door transition ticks (" + config.doorTransitionTicks() + ")",
                ValidationIssue.Severity.ERROR
            ));
        }

        // Validate homeFloor is within valid floor range (0 to floors-1)
        int maxFloor = config.floors() - 1;
        if (config.homeFloor() > maxFloor) {
            errors.add(new ValidationIssue(
                "homeFloor",
                "Home floor (" + config.homeFloor() +
                ") must be within valid floor range (0 to " + maxFloor + ")",
                ValidationIssue.Severity.ERROR
            ));
        }

        // Warning: If using PARK_TO_HOME_FLOOR but idleTimeoutTicks is very low
        if (config.idleParkingMode() == com.liftsimulator.domain.IdleParkingMode.PARK_TO_HOME_FLOOR
            && config.idleTimeoutTicks() < 3) {
            warnings.add(new ValidationIssue(
                "idleTimeoutTicks",
                "Idle timeout ticks (" + config.idleTimeoutTicks() +
                ") is very low for PARK_TO_HOME_FLOOR mode. Consider increasing to avoid excessive parking movements.",
                ValidationIssue.Severity.WARNING
            ));
        }

        // Warning: If doorDwellTicks is very low
        if (config.doorDwellTicks() < 2) {
            warnings.add(new ValidationIssue(
                "doorDwellTicks",
                "Door dwell ticks (" + config.doorDwellTicks() +
                ") is very low. Consider increasing to allow sufficient time for passengers.",
                ValidationIssue.Severity.WARNING
            ));
        }

        // Warning: If number of lifts is greater than number of floors
        if (config.lifts() > config.floors()) {
            warnings.add(new ValidationIssue(
                "lifts",
                "Number of lifts (" + config.lifts() +
                ") exceeds number of floors (" + config.floors() + "). This may be inefficient.",
                ValidationIssue.Severity.WARNING
            ));
        }

        // Warning: If doorReopenWindowTicks is 0
        if (config.doorReopenWindowTicks() == 0) {
            warnings.add(new ValidationIssue(
                "doorReopenWindowTicks",
                "Door reopen window ticks is set to 0. Doors will not respond to reopen requests during closing.",
                ValidationIssue.Severity.WARNING
            ));
        }
    }
}
