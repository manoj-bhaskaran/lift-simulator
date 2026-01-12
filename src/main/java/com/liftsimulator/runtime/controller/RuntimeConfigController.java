package com.liftsimulator.runtime.controller;

import com.liftsimulator.runtime.dto.RuntimeConfigDTO;
import com.liftsimulator.runtime.dto.SimulationLaunchResponse;
import com.liftsimulator.runtime.service.RuntimeConfigService;
import com.liftsimulator.runtime.service.RuntimeSimulationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final RuntimeSimulationService runtimeSimulationService;

    @SuppressWarnings("EI_EXPOSE_REP2")
    public RuntimeConfigController(
            RuntimeConfigService runtimeConfigService,
            RuntimeSimulationService runtimeSimulationService) {
        this.runtimeConfigService = runtimeConfigService;
        this.runtimeSimulationService = runtimeSimulationService;
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

    /**
     * Launches a local simulator process using the currently published configuration.
     *
     * @param systemKey the unique system key
     * @return response with launch status and process metadata
     */
    @PostMapping("/{systemKey}/simulate")
    public SimulationLaunchResponse launchPublishedSimulation(@PathVariable String systemKey) {
        return runtimeSimulationService.launchPublishedSimulation(systemKey);
    }
}
