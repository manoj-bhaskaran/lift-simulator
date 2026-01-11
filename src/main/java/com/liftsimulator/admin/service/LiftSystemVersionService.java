package com.liftsimulator.admin.service;

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

    public LiftSystemVersionService(
            LiftSystemRepository liftSystemRepository,
            LiftSystemVersionRepository versionRepository) {
        this.liftSystemRepository = liftSystemRepository;
        this.versionRepository = versionRepository;
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
