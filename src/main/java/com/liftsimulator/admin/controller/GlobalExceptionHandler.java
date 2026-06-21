package com.liftsimulator.admin.controller;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.liftsimulator.admin.dto.ConfigValidationResponse;
import com.liftsimulator.admin.dto.ErrorResponse;
import com.liftsimulator.admin.dto.ScenarioValidationResponse;
import com.liftsimulator.admin.dto.ValidationErrorResponse;
import com.liftsimulator.admin.service.ArtefactDeletionException;
import com.liftsimulator.admin.service.ArtefactStateException;
import com.liftsimulator.admin.service.ConfigValidationException;
import com.liftsimulator.admin.service.InvalidArtefactPathException;
import com.liftsimulator.admin.service.ResourceNotFoundException;
import com.liftsimulator.admin.service.ScenarioValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST API errors.
 * Provides centralized exception handling with comprehensive logging for monitoring and debugging.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles ResourceNotFoundException with 404 status.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        logger.info("Resource not found: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            OffsetDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles invalid artefact paths with 400 status.
     * Returns generic client-facing message; logs full details server-side.
     */
    @ExceptionHandler(InvalidArtefactPathException.class)
    public ResponseEntity<ErrorResponse> handleInvalidArtefactPath(InvalidArtefactPathException ex) {
        logger.warn("Invalid artefact path: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Invalid artefact path",
            OffsetDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles IllegalArgumentException with 400 status.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        logger.info("Illegal argument: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            OffsetDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles artefact state errors with 409 status.
     * Returns generic client-facing message; logs full details server-side.
     */
    @ExceptionHandler(ArtefactStateException.class)
    public ResponseEntity<ErrorResponse> handleArtefactState(ArtefactStateException ex) {
        logger.warn("Artefact state error: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "Artefacts are not available for this run",
            OffsetDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles IllegalStateException with 409 status.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        logger.info("Illegal state: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            ex.getMessage(),
            OffsetDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * Handles artefact deletion failures with 500 status.
     * Surfaces a clear, actionable message instead of a generic server error so callers
     * understand that the run was not deleted because its artefacts could not be removed.
     */
    @ExceptionHandler(ArtefactDeletionException.class)
    public ResponseEntity<ErrorResponse> handleArtefactDeletionError(ArtefactDeletionException ex) {
        logger.error("Artefact deletion failed: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            ex.getMessage(),
            OffsetDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    /**
     * Handles configuration validation errors with 400 status.
     */
    @ExceptionHandler(ConfigValidationException.class)
    public ResponseEntity<ConfigValidationResponse> handleConfigValidationError(
        ConfigValidationException ex
    ) {
        logger.info("Configuration validation error: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getValidationResponse());
    }

    /**
     * Handles scenario validation errors with 400 status.
     */
    @ExceptionHandler(ScenarioValidationException.class)
    public ResponseEntity<ScenarioValidationResponse> handleScenarioValidationError(
        ScenarioValidationException ex
    ) {
        logger.info("Scenario validation error: {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getValidationResponse());
    }

    /**
     * Handles HTTP message not readable exceptions with 400 status.
     * Specifically handles unknown property errors from strict JSON deserialization.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
        HttpMessageNotReadableException ex
    ) {
        String message = "Malformed JSON request";

        // Check if this is an unknown property error
        Throwable cause = ex.getCause();
        if (cause instanceof UnrecognizedPropertyException unrecognizedEx) {
            String fieldName = unrecognizedEx.getPropertyName();
            message = "Unknown property '" + fieldName + "' is not allowed";
            logger.warn("Malformed JSON request - unknown property: {}", fieldName);
        } else {
            logger.warn("Malformed JSON request: {}", ex.getMessage());
        }

        ErrorResponse error = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            message,
            OffsetDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles validation errors with 400 status.
     * Supports both field-level (FieldError) and object-level (ObjectError) validation constraints.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationErrors(
        MethodArgumentNotValidException ex
    ) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName;
            if (error instanceof FieldError fieldError) {
                // Field-level constraint violation
                fieldName = fieldError.getField();
            } else {
                // Object-level constraint violation (e.g., class-level validation)
                fieldName = error.getObjectName();
            }
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        logger.debug("Validation failed for {} field(s): {}", fieldErrors.size(), fieldErrors.keySet());

        ValidationErrorResponse error = new ValidationErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed",
            fieldErrors,
            OffsetDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * Handles ResponseStatusException with the original status code.
     * This preserves the intended HTTP status (e.g., 404) instead of converting to 500.
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        logger.info("Response status exception: {} - {}", ex.getStatusCode(), ex.getReason());

        ErrorResponse error = new ErrorResponse(
            ex.getStatusCode().value(),
            ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString(),
            OffsetDateTime.now()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    /**
     * Handles unsupported HTTP methods with 405 status.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {
        logger.info("HTTP method not supported: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
            HttpStatus.METHOD_NOT_ALLOWED.value(),
            ex.getMessage(),
            OffsetDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    /**
     * Handles Spring's NoResourceFoundException with 404 status.
     * This exception is thrown when no handler is found for a request.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        logger.info("No resource found: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Resource not found",
            OffsetDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * Handles unexpected exceptions with 500 status.
     * Logs full stack trace for debugging production issues.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        logger.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "An unexpected error occurred",
            OffsetDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
