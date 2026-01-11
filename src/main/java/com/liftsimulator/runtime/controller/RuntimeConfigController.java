package com.liftsimulator.runtime.controller;

import com.liftsimulator.runtime.dto.RuntimeConfigDTO;
import com.liftsimulator.runtime.service.RuntimeConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for runtime configuration access.
 * Provides read-only access to published configurations.
 */
@RestController
@RequestMapping("/api/runtime/systems")
public class RuntimeConfigController {

    private final RuntimeConfigService runtimeConfigService;

    public RuntimeConfigController(RuntimeConfigService runtimeConfigService) {
        this.runtimeConfigService = runtimeConfigService;
    }

    /**
     * Retrieves the currently published configuration for a lift system.
     * Only returns configurations that are in PUBLISHED status.
     *
     * @param systemKey the unique system key
     * @return the published configuration
     */
    @GetMapping("/{systemKey}/config")
    public RuntimeConfigDTO getPublishedConfig(@PathVariable String systemKey) {
        return runtimeConfigService.getPublishedConfig(systemKey);
    }

    /**
     * Retrieves a specific published version by system key and version number.
     * Only returns the version if it is currently published.
     *
     * @param systemKey the unique system key
     * @param versionNumber the version number
     * @return the published version
     */
    @GetMapping("/{systemKey}/versions/{versionNumber}")
    public RuntimeConfigDTO getPublishedVersion(
            @PathVariable String systemKey,
            @PathVariable Integer versionNumber) {
        return runtimeConfigService.getPublishedVersion(systemKey, versionNumber);
    }
}
