package com.liftsimulator.admin.controller;

import com.liftsimulator.admin.dto.ConfigValidationRequest;
import com.liftsimulator.admin.dto.ConfigValidationResponse;
import com.liftsimulator.admin.service.ConfigValidationService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for validating lift system configurations.
 */
@RestController
@RequestMapping("/api/v1/config")
public class ConfigValidationController {

    private final ConfigValidationService configValidationService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring-managed service injected via constructor. "
                    + "Lifecycle and immutability managed by Spring container."
    )
    public ConfigValidationController(ConfigValidationService configValidationService) {
        this.configValidationService = configValidationService;
    }

    /**
     * Validates a lift system configuration JSON without persisting it.
     *
     * @param request the validation request containing config JSON
     * @return validation response with errors and warnings
     */
    @PostMapping("/validate")
    public ResponseEntity<ConfigValidationResponse> validateConfig(
        @Valid @RequestBody ConfigValidationRequest request
    ) {
        ConfigValidationResponse response = configValidationService.validate(request.config());
        return ResponseEntity.ok(response);
    }
}
