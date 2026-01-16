package com.liftsimulator.runtime.service;

import com.liftsimulator.runtime.dto.RuntimeConfigDTO;
import com.liftsimulator.runtime.dto.SimulationLaunchResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

/**
 * Service to launch local simulator processes using published configurations.
 */
@Service
public class RuntimeSimulationService {
    private static final Logger logger = LoggerFactory.getLogger(RuntimeSimulationService.class);
    private static final long SHUTDOWN_WAIT_SECONDS = 5L;

    private final RuntimeConfigService runtimeConfigService;
    private final Map<Long, ManagedProcess> activeProcesses = new ConcurrentHashMap<>();
    private final ExecutorService logReaderExecutor = Executors.newCachedThreadPool(new SimulatorThreadFactory());

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
        Process process = startSimulationProcess(systemKey, configPath);
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

    private Process startSimulationProcess(String systemKey, Path configPath) {
        List<String> command = buildLaunchCommand(configPath);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            registerProcess(systemKey, process, command, configPath);
            return process;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to launch simulator process", ex);
        }
    }

    private List<String> buildLaunchCommand(Path configPath) {
        Path sourcePath = resolveApplicationSource();
        if (isPackagedJar(sourcePath)) {
            return buildPackagedCommand(sourcePath, configPath);
        }

        List<String> command = new ArrayList<>();
        command.add(resolveJavaBinary());
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add("com.liftsimulator.runtime.LocalSimulationMain");
        command.add("--config=" + configPath.toAbsolutePath());
        return command;
    }

    private List<String> buildPackagedCommand(Path sourcePath, Path configPath) {
        List<String> command = new ArrayList<>();
        command.add(resolveJavaBinary());
        command.add("-cp");
        command.add(sourcePath.toAbsolutePath().toString());
        command.add("org.springframework.boot.loader.launch.PropertiesLauncher");
        command.add("--loader.main=com.liftsimulator.runtime.LocalSimulationMain");
        command.add("--config=" + configPath.toAbsolutePath());
        return command;
    }

    private void registerProcess(String systemKey, Process process, List<String> command, Path configPath) {
        long pid = process.pid();
        activeProcesses.put(pid, new ManagedProcess(systemKey, process, configPath));
        logger.info("Launched simulator process pid={} for system {} with config {} using command: {}",
            pid,
            systemKey,
            configPath.getFileName(),
            String.join(" ", command));

        logReaderExecutor.submit(() -> streamProcessOutput(systemKey, pid, process));

        process.onExit().thenAccept(exitedProcess -> {
            activeProcesses.remove(pid);
            logger.info("Simulator process pid={} for system {} exited with code {}",
                pid,
                systemKey,
                exitedProcess.exitValue());
        });
    }

    private void streamProcessOutput(String systemKey, long pid, Process process) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(),
            StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("[simulator:{}:{}] {}", systemKey, pid, line);
            }
        } catch (IOException ex) {
            logger.warn("Failed to read output from simulator process pid={} for system {}", pid, systemKey, ex);
        }
    }

    private String resolveJavaBinary() {
        String javaHome = System.getProperty("java.home");
        Path javaPath = Paths.get(javaHome, "bin", "java");
        return javaPath.toString();
    }

    private Path resolveApplicationSource() {
        ApplicationHome home = new ApplicationHome(RuntimeSimulationService.class);
        if (home.getSource() == null) {
            return null;
        }
        return home.getSource().toPath();
    }

    private boolean isPackagedJar(Path sourcePath) {
        return sourcePath != null
            && Files.isRegularFile(sourcePath)
            && sourcePath.getFileName().toString().endsWith(".jar");
    }

    @PreDestroy
    public void shutdownActiveProcesses() {
        if (activeProcesses.isEmpty()) {
            logReaderExecutor.shutdown();
            return;
        }

        logger.info("Shutting down {} active simulator process(es)", activeProcesses.size());
        List<ManagedProcess> processes = new ArrayList<>(activeProcesses.values());
        for (ManagedProcess managedProcess : processes) {
            shutdownProcess(managedProcess);
        }
        logReaderExecutor.shutdown();
    }

    private void shutdownProcess(ManagedProcess managedProcess) {
        Process process = managedProcess.process();
        if (!process.isAlive()) {
            return;
        }

        logger.info("Stopping simulator process pid={} for system {}", process.pid(), managedProcess.systemKey());
        process.destroy();
        try {
            if (!process.waitFor(SHUTDOWN_WAIT_SECONDS, TimeUnit.SECONDS)) {
                logger.warn("Simulator process pid={} did not stop in time; forcing shutdown", process.pid());
                process.destroyForcibly();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while stopping simulator process pid={}", process.pid(), ex);
            process.destroyForcibly();
        }
    }

    private record ManagedProcess(String systemKey, Process process, Path configPath) {
    }

    private static final class SimulatorThreadFactory implements ThreadFactory {
        private int counter = 1;

        @Override
        public synchronized Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "simulator-output-" + counter++);
            thread.setDaemon(true);
            return thread;
        }
    }
}
