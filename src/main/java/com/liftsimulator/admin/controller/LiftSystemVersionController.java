package com.liftsimulator.admin.controller;

import com.liftsimulator.admin.dto.CreateVersionRequest;
import com.liftsimulator.admin.dto.UpdateVersionConfigRequest;
import com.liftsimulator.admin.dto.VersionResponse;
import com.liftsimulator.admin.service.LiftSystemVersionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for managing lift system versions.
 */
@RestController
@RequestMapping("/api/lift-systems/{systemId}/versions")
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
    @ResponseStatus(HttpStatus.CREATED)
    public VersionResponse createVersion(
            @PathVariable Long systemId,
            @Valid @RequestBody CreateVersionRequest request) {
        return versionService.createVersion(systemId, request);
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
    public VersionResponse updateVersionConfig(
            @PathVariable Long systemId,
            @PathVariable Integer versionNumber,
            @Valid @RequestBody UpdateVersionConfigRequest request) {
        return versionService.updateVersionConfig(systemId, versionNumber, request);
    }

    /**
     * Lists all versions for a lift system.
     *
     * @param systemId the lift system ID
     * @return list of versions ordered by version number descending
     */
    @GetMapping
    public List<VersionResponse> listVersions(@PathVariable Long systemId) {
        return versionService.listVersions(systemId);
    }

    /**
     * Retrieves a specific version.
     *
     * @param systemId the lift system ID
     * @param versionNumber the version number
     * @return the version details
     */
    @GetMapping("/{versionNumber}")
    public VersionResponse getVersion(
            @PathVariable Long systemId,
            @PathVariable Integer versionNumber) {
        return versionService.getVersion(systemId, versionNumber);
    }
}
