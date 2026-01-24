package com.liftsimulator.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.liftsimulator.admin.dto.PassengerFlowDTO;
import com.liftsimulator.admin.dto.ScenarioDefinitionDTO;
import com.liftsimulator.admin.dto.ScenarioValidationResponse;
import com.liftsimulator.admin.dto.ValidationIssue;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service for validating UI scenario JSON payloads.
 */
@Service
public class ScenarioValidationService {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring-managed beans injected via constructor. "
                    + "Lifecycle and immutability managed by Spring container."
    )
    public ScenarioValidationService(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    /**
     * Validates a scenario JSON string.
     *
     * @param scenarioJson the scenario JSON to validate
     * @return ScenarioValidationResponse containing validation results
     */
    public ScenarioValidationResponse validate(JsonNode scenarioJson) {
        List<ValidationIssue> errors = new ArrayList<>();
        List<ValidationIssue> warnings = new ArrayList<>();

        ScenarioDefinitionDTO scenario;
        try {
            scenario = objectMapper.readerFor(ScenarioDefinitionDTO.class)
                .readValue(scenarioJson);
        } catch (UnrecognizedPropertyException e) {
            String fieldName = e.getPropertyName();
            errors.add(new ValidationIssue(
                fieldName,
                "Unknown property '" + fieldName + "' is not allowed in scenario schema",
                ValidationIssue.Severity.ERROR
            ));
            return new ScenarioValidationResponse(false, errors, warnings);
        } catch (InvalidFormatException e) {
            String fieldName = getFieldNameFromPath(e.getPath());
            String targetType = e.getTargetType().getSimpleName();
            String value = e.getValue() != null ? e.getValue().toString() : "null";
            String errorMessage;
            if (targetType.equals("Integer")) {
                errorMessage = "Field '" + fieldName + "' must be a numeric value, got '" + value + "'";
            } else {
                errorMessage = "Field '" + fieldName + "' has invalid format. Expected type: " + targetType;
            }
            errors.add(new ValidationIssue(fieldName, errorMessage, ValidationIssue.Severity.ERROR));
            return new ScenarioValidationResponse(false, errors, warnings);
        } catch (MismatchedInputException e) {
            String fieldName = getFieldNameFromPath(e.getPath());
            String targetType = e.getTargetType().getSimpleName();
            String errorMessage;
            if (targetType.equals("Integer")) {
                errorMessage = "Field '" + fieldName + "' must be a numeric value";
            } else {
                errorMessage = "Field '" + fieldName + "' has incorrect type. Expected: " + targetType;
            }
            errors.add(new ValidationIssue(fieldName, errorMessage, ValidationIssue.Severity.ERROR));
            return new ScenarioValidationResponse(false, errors, warnings);
        } catch (JsonProcessingException e) {
            errors.add(new ValidationIssue(
                "scenario",
                "Invalid JSON format: " + e.getOriginalMessage(),
                ValidationIssue.Severity.ERROR
            ));
            return new ScenarioValidationResponse(false, errors, warnings);
        } catch (IOException e) {
            errors.add(new ValidationIssue(
                "scenario",
                "Unable to read scenario payload: " + e.getMessage(),
                ValidationIssue.Severity.ERROR
            ));
            return new ScenarioValidationResponse(false, errors, warnings);
        }

        Set<ConstraintViolation<ScenarioDefinitionDTO>> violations = validator.validate(scenario);
        for (ConstraintViolation<ScenarioDefinitionDTO> violation : violations) {
            errors.add(new ValidationIssue(
                violation.getPropertyPath().toString(),
                violation.getMessage(),
                ValidationIssue.Severity.ERROR
            ));
        }

        performDomainValidation(scenario, errors);

        boolean valid = errors.isEmpty();
        return new ScenarioValidationResponse(valid, errors, warnings);
    }

    private String getFieldNameFromPath(List<JsonMappingException.Reference> path) {
        if (path == null || path.isEmpty()) {
            return "scenario";
        }
        JsonMappingException.Reference lastRef = path.get(path.size() - 1);
        String fieldName = lastRef.getFieldName();
        return fieldName != null ? fieldName : "scenario";
    }

    private void performDomainValidation(ScenarioDefinitionDTO scenario, List<ValidationIssue> errors) {
        if (scenario.durationTicks() == null || scenario.passengerFlows() == null) {
            return;
        }

        int duration = scenario.durationTicks();
        List<PassengerFlowDTO> flows = scenario.passengerFlows();
        for (int i = 0; i < flows.size(); i++) {
            PassengerFlowDTO flow = flows.get(i);
            if (flow == null) {
                errors.add(new ValidationIssue(
                    "passengerFlows[" + i + "]",
                    "Passenger flow entry must not be null",
                    ValidationIssue.Severity.ERROR
                ));
                continue;
            }
            if (flow.startTick() != null && flow.startTick() >= duration) {
                errors.add(new ValidationIssue(
                    "passengerFlows[" + i + "].startTick",
                    "startTick must be less than durationTicks (" + duration + ")",
                    ValidationIssue.Severity.ERROR
                ));
            }
            if (flow.originFloor() != null && flow.destinationFloor() != null
                && flow.originFloor().equals(flow.destinationFloor())) {
                errors.add(new ValidationIssue(
                    "passengerFlows[" + i + "].destinationFloor",
                    "destinationFloor must be different from originFloor",
                    ValidationIssue.Severity.ERROR
                ));
            }
        }
    }
}
