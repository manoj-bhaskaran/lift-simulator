package com.liftsimulator.admin.controller;

import com.liftsimulator.admin.dto.CreateVersionRequest;
import com.liftsimulator.admin.dto.UpdateVersionConfigRequest;
import com.liftsimulator.admin.dto.VersionResponse;
import com.liftsimulator.admin.service.LiftSystemVersionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing lift system versions.
 */
@RestController
@RequestMapping("/api/v1/lift-systems/{systemId}/versions")
public class LiftSystemVersionController {

    private final LiftSystemVersionService versionService;

    public LiftSystemVersionController(LiftSystemVersionService versionService) {
        this.versionService = versionService;
    }

    /**
     * Creates a new version for a lift system.
     * Optionally clones configuration from an existing version.
     *
     * @param systemId the lift system ID
     * @param request the creation request
     * @return the created version
     */
    @PostMapping
    public ResponseEntity<VersionResponse> createVersion(
            @PathVariable Long systemId,
            @Valid @RequestBody CreateVersionRequest request) {
        VersionResponse response = versionService.createVersion(systemId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates the configuration of an existing version.
     *
     * @param systemId the lift system ID
     * @param versionNumber the version number
     * @param request the update request
     * @return the updated version
     */
    @PutMapping("/{versionNumber}")
    public ResponseEntity<VersionResponse> updateVersionConfig(
            @PathVariable Long systemId,
            @PathVariable Integer versionNumber,
            @Valid @RequestBody UpdateVersionConfigRequest request) {
        VersionResponse response = versionService.updateVersionConfig(systemId, versionNumber, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Lists all versions for a lift system.
     *
     * @param systemId the lift system ID
     * @return list of versions ordered by version number descending
     */
    @GetMapping
    public ResponseEntity<List<VersionResponse>> listVersions(@PathVariable Long systemId) {
        List<VersionResponse> responses = versionService.listVersions(systemId);
        return ResponseEntity.ok(responses);
    }

    /**
     * Retrieves a specific version.
     *
     * @param systemId the lift system ID
     * @param versionNumber the version number
     * @return the version details
     */
    @GetMapping("/{versionNumber}")
    public ResponseEntity<VersionResponse> getVersion(
            @PathVariable Long systemId,
            @PathVariable Integer versionNumber) {
        VersionResponse response = versionService.getVersion(systemId, versionNumber);
        return ResponseEntity.ok(response);
    }

    /**
     * Publishes a version after validating its configuration.
     * Only versions with valid configurations can be published.
     *
     * @param systemId the lift system ID
     * @param versionNumber the version number
     * @return the published version
     */
    @PostMapping("/{versionNumber}/publish")
    public ResponseEntity<VersionResponse> publishVersion(
            @PathVariable Long systemId,
            @PathVariable Integer versionNumber) {
        VersionResponse response = versionService.publishVersion(systemId, versionNumber);
        return ResponseEntity.ok(response);
    }
}
