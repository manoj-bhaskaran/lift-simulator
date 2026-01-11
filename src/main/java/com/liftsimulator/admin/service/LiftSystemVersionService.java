package com.liftsimulator.admin.service;

import com.liftsimulator.admin.dto.ConfigValidationResponse;
import com.liftsimulator.admin.dto.CreateVersionRequest;
import com.liftsimulator.admin.dto.UpdateVersionConfigRequest;
import com.liftsimulator.admin.dto.VersionResponse;
import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.repository.LiftSystemRepository;
import com.liftsimulator.admin.repository.LiftSystemVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing lift system versions.
 */
@Service
@Transactional(readOnly = true)
public class LiftSystemVersionService {

    private final LiftSystemRepository liftSystemRepository;
    private final LiftSystemVersionRepository versionRepository;
    private final ConfigValidationService configValidationService;

    public LiftSystemVersionService(
            LiftSystemRepository liftSystemRepository,
            LiftSystemVersionRepository versionRepository,
            ConfigValidationService configValidationService) {
        this.liftSystemRepository = liftSystemRepository;
        this.versionRepository = versionRepository;
        this.configValidationService = configValidationService;
    }

    /**
     * Creates a new version for a lift system.
     * Optionally clones configuration from an existing version.
     *
     * @param systemId the lift system ID
     * @param request the creation request
     * @return the created version
     * @throws ResourceNotFoundException if lift system or clone source not found
     */
    @Transactional
    public VersionResponse createVersion(Long systemId, CreateVersionRequest request) {
        LiftSystem liftSystem = liftSystemRepository.findById(systemId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Lift system not found with id: " + systemId
            ));

        // Determine the config to use
        String config;
        if (request.cloneFromVersionNumber() != null) {
            // Clone from existing version
            LiftSystemVersion sourceVersion = versionRepository
                .findByLiftSystemIdAndVersionNumber(systemId, request.cloneFromVersionNumber())
                .orElseThrow(() -> new ResourceNotFoundException(
                    "Version " + request.cloneFromVersionNumber()
                    + " not found for lift system " + systemId
                ));
            config = sourceVersion.getConfig();
        } else {
            // Use provided config
            config = request.config();
        }

        // Validate configuration
        ConfigValidationResponse validationResponse = configValidationService.validate(config);
        if (validationResponse.hasErrors()) {
            throw new ConfigValidationException("Configuration validation failed", validationResponse);
        }

        // Get next version number
        Integer nextVersionNumber = getNextVersionNumber(systemId);

        // Create new version
        LiftSystemVersion version = new LiftSystemVersion(liftSystem, nextVersionNumber, config);
        LiftSystemVersion savedVersion = versionRepository.save(version);

        return VersionResponse.fromEntity(savedVersion);
    }

    /**
     * Updates the configuration of an existing version.
     *
     * @param systemId the lift system ID
     * @param versionNumber the version number
     * @param request the update request
     * @return the updated version
     * @throws ResourceNotFoundException if version not found
     */
    @Transactional
    public VersionResponse updateVersionConfig(
            Long systemId, Integer versionNumber, UpdateVersionConfigRequest request) {
        LiftSystemVersion version = versionRepository
            .findByLiftSystemIdAndVersionNumber(systemId, versionNumber)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Version " + versionNumber + " not found for lift system " + systemId
            ));

        // Validate configuration
        ConfigValidationResponse validationResponse = configValidationService.validate(request.config());
        if (validationResponse.hasErrors()) {
            throw new ConfigValidationException("Configuration validation failed", validationResponse);
        }

        version.setConfig(request.config());
        LiftSystemVersion updatedVersion = versionRepository.save(version);

        return VersionResponse.fromEntity(updatedVersion);
    }

    /**
     * Lists all versions for a lift system.
     *
     * @param systemId the lift system ID
     * @return list of versions ordered by version number descending
     * @throws ResourceNotFoundException if lift system not found
     */
    public List<VersionResponse> listVersions(Long systemId) {
        // Verify lift system exists
        if (!liftSystemRepository.existsById(systemId)) {
            throw new ResourceNotFoundException(
                "Lift system not found with id: " + systemId
            );
        }

        return versionRepository.findByLiftSystemIdOrderByVersionNumberDesc(systemId)
            .stream()
            .map(VersionResponse::fromEntity)
            .toList();
    }

    /**
     * Retrieves a specific version.
     *
     * @param systemId the lift system ID
     * @param versionNumber the version number
     * @return the version details
     * @throws ResourceNotFoundException if version not found
     */
    public VersionResponse getVersion(Long systemId, Integer versionNumber) {
        LiftSystemVersion version = versionRepository
            .findByLiftSystemIdAndVersionNumber(systemId, versionNumber)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Version " + versionNumber + " not found for lift system " + systemId
            ));

        return VersionResponse.fromEntity(version);
    }

    /**
     * Publishes a version after validating its configuration.
     * Only versions with valid configurations can be published.
     * Automatically archives any previously published version for the same lift system.
     *
     * @param systemId the lift system ID
     * @param versionNumber the version number
     * @return the published version
     * @throws ResourceNotFoundException if version not found
     * @throws ConfigValidationException if configuration is invalid
     * @throws IllegalStateException if version is already published
     */
    @Transactional
    public VersionResponse publishVersion(Long systemId, Integer versionNumber) {
        LiftSystemVersion version = versionRepository
            .findByLiftSystemIdAndVersionNumber(systemId, versionNumber)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Version " + versionNumber + " not found for lift system " + systemId
            ));

        // Check if already published
        if (version.getIsPublished()) {
            throw new IllegalStateException(
                "Version " + versionNumber + " is already published"
            );
        }

        // Validate configuration before publishing
        ConfigValidationResponse validationResponse = configValidationService.validate(version.getConfig());
        if (validationResponse.hasErrors()) {
            throw new ConfigValidationException(
                "Cannot publish version with validation errors",
                validationResponse
            );
        }

        // Archive any previously published versions for this lift system
        List<LiftSystemVersion> publishedVersions = versionRepository
            .findByLiftSystemIdAndIsPublishedTrue(systemId);
        for (LiftSystemVersion publishedVersion : publishedVersions) {
            publishedVersion.archive();
            versionRepository.save(publishedVersion);
        }

        // Publish the version
        version.publish();
        LiftSystemVersion publishedVersion = versionRepository.save(version);

        return VersionResponse.fromEntity(publishedVersion);
    }

    /**
     * Gets the next version number for a lift system.
     *
     * @param systemId the lift system ID
     * @return the next version number (1 if no versions exist)
     */
    private Integer getNextVersionNumber(Long systemId) {
        Integer maxVersion = versionRepository.findMaxVersionNumberByLiftSystemId(systemId);
        return (maxVersion == null) ? 1 : maxVersion + 1;
    }
}
