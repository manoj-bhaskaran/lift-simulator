package com.liftsimulator.runtime.service;

import com.liftsimulator.runtime.dto.RuntimeConfigDTO;
import com.liftsimulator.runtime.dto.SimulationLaunchResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service to launch local simulator processes using published configurations.
 */
@Service
public class RuntimeSimulationService {
    private static final Logger logger = LoggerFactory.getLogger(RuntimeSimulationService.class);

    private final RuntimeConfigService runtimeConfigService;

    public RuntimeSimulationService(RuntimeConfigService runtimeConfigService) {
        this.runtimeConfigService = runtimeConfigService;
    }

    /**
     * Launches the simulator with the currently published configuration for the provided system key.
     *
     * @param systemKey the lift system key
     * @return launch response with process metadata
     */
    public SimulationLaunchResponse launchPublishedSimulation(String systemKey) {
        RuntimeConfigDTO config = runtimeConfigService.getPublishedConfig(systemKey);
        Path configPath = writeConfigToTempFile(systemKey, config.config());
        Process process = startSimulationProcess(configPath);
        String message = "Simulator started for system " + systemKey + " using config " + configPath.getFileName();
        return new SimulationLaunchResponse(true, message, process.pid());
    }

    private Path writeConfigToTempFile(String systemKey, String configJson) {
        try {
            Path configPath = Files.createTempFile("lift-simulator-" + systemKey + "-", ".json");
            Files.writeString(configPath, configJson, StandardCharsets.UTF_8);
            configPath.toFile().deleteOnExit();
            logger.info("Wrote runtime config for system {} to {}", systemKey, configPath);
            return configPath;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write configuration to temp file", ex);
        }
    }

    private Process startSimulationProcess(Path configPath) {
        List<String> command = new ArrayList<>();
        command.add(resolveJavaBinary());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add("com.liftsimulator.runtime.LocalSimulationMain");
        command.add("--config=" + configPath.toAbsolutePath());

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        processBuilder.inheritIO();

        try {
            Process process = processBuilder.start();
            logger.info("Launched simulator process pid={} with config {}", process.pid(), configPath);
            return process;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to launch simulator process", ex);
        }
    }

    private String resolveJavaBinary() {
        String javaHome = System.getProperty("java.home");
        Path javaPath = Paths.get(javaHome, "bin", "java");
        return javaPath.toString();
    }
}
