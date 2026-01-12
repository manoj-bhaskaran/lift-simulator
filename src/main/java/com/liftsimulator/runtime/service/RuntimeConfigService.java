package com.liftsimulator.runtime.service;

import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.repository.LiftSystemRepository;
import com.liftsimulator.admin.repository.LiftSystemVersionRepository;
import com.liftsimulator.admin.service.ResourceNotFoundException;
import com.liftsimulator.runtime.dto.RuntimeConfigDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for runtime configuration retrieval.
 * Provides access to published configurations only.
 */
@Service
@Transactional(readOnly = true)
public class RuntimeConfigService {

    private final LiftSystemRepository liftSystemRepository;
    private final LiftSystemVersionRepository versionRepository;

    public RuntimeConfigService(
            LiftSystemRepository liftSystemRepository,
            LiftSystemVersionRepository versionRepository) {
        this.liftSystemRepository = liftSystemRepository;
        this.versionRepository = versionRepository;
    }

    /**
     * Retrieves the published configuration for a lift system by system key.
     * Only returns configurations that are currently published.
     *
     * @param systemKey the unique system key
     * @return the published configuration
     * @throws ResourceNotFoundException if system not found or no published version exists
     */
    public RuntimeConfigDTO getPublishedConfig(String systemKey) {
        var liftSystem = liftSystemRepository.findBySystemKey(systemKey)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Lift system not found with key: " + systemKey
            ));

        var publishedVersions = versionRepository
            .findByLiftSystemIdAndIsPublishedTrue(liftSystem.getId());

        if (publishedVersions.isEmpty()) {
            throw new ResourceNotFoundException(
                "No published version found for lift system: " + systemKey
            );
        }

        // Should only be one published version due to publish/archive workflow
        LiftSystemVersion publishedVersion = publishedVersions.get(0);

        return RuntimeConfigDTO.fromEntity(liftSystem, publishedVersion);
    }

    /**
     * Retrieves a specific published version by system key and version number.
     * Only returns the version if it is currently published.
     *
     * @param systemKey the unique system key
     * @param versionNumber the version number
     * @return the published version
     * @throws ResourceNotFoundException if system not found, version not found, or version not published
     */
    public RuntimeConfigDTO getPublishedVersion(String systemKey, Integer versionNumber) {
        var liftSystem = liftSystemRepository.findBySystemKey(systemKey)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Lift system not found with key: " + systemKey
            ));

        var version = versionRepository
            .findByLiftSystemIdAndVersionNumber(liftSystem.getId(), versionNumber)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Version " + versionNumber + " not found for lift system: " + systemKey
            ));

        if (!version.getIsPublished()) {
            throw new ResourceNotFoundException(
                "Version " + versionNumber + " is not published for lift system: " + systemKey
            );
        }

        return RuntimeConfigDTO.fromEntity(liftSystem, version);
    }
}
