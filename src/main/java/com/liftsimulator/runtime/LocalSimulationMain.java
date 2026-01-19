package com.liftsimulator.runtime;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftsimulator.admin.dto.LiftConfigDTO;
import com.liftsimulator.engine.ControllerFactory;
import com.liftsimulator.engine.RequestManagingLiftController;
import com.liftsimulator.engine.SimulationEngine;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * CLI entry point for running a local simulation using a configuration JSON file.
 */
public final class LocalSimulationMain {
    private static final int DEFAULT_TICKS = 25;

    private LocalSimulationMain() {
    }

    public static void main(String[] args) {
        Arguments arguments;
        try {
            arguments = Arguments.parse(args);
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            printUsage();
            System.exit(1);
            return;
        }

        if (arguments.showHelp()) {
            printUsage();
            return;
        }

        if (arguments.configPath() == null) {
            System.err.println("Missing required --config argument");
            printUsage();
            System.exit(1);
        }

        try {
            LiftConfigDTO config = readConfig(arguments.configPath());
            SimulationEngine engine = buildEngine(config);
            runSimulation(engine, arguments.ticks());
            System.out.println("Simulation completed successfully after " + arguments.ticks() + " ticks.");
        } catch (IOException ex) {
            System.err.println("Failed to read config: " + ex.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException ex) {
            System.err.println("Invalid config: " + ex.getMessage());
            System.exit(1);
        }
    }

    private static LiftConfigDTO readConfig(Path configPath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        // Configure ObjectMapper to enforce strict schema validation - reject unknown properties
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        String json = Files.readString(configPath);
        return mapper.readValue(json, LiftConfigDTO.class);
    }

    private static SimulationEngine buildEngine(LiftConfigDTO config) {
        RequestManagingLiftController controller = (RequestManagingLiftController) ControllerFactory.createController(
            config.controllerStrategy(),
            config.homeFloor(),
            config.idleTimeoutTicks(),
            config.idleParkingMode()
        );

        return SimulationEngine.builder(controller, config.minFloor(), config.maxFloor())
            .initialFloor(config.homeFloor())
            .travelTicksPerFloor(config.travelTicksPerFloor())
            .doorTransitionTicks(config.doorTransitionTicks())
            .doorDwellTicks(config.doorDwellTicks())
            .doorReopenWindowTicks(config.doorReopenWindowTicks())
            .build();
    }

    private static void runSimulation(SimulationEngine engine, int ticks) {
        System.out.println("Starting simulation for " + ticks + " ticks...");
        for (int i = 0; i < ticks; i++) {
            engine.tick();
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -cp lift-simulator.jar com.liftsimulator.runtime.LocalSimulationMain --config=PATH [--ticks=N]");
    }

    private record Arguments(Path configPath, int ticks, boolean showHelp) {
        private static Arguments parse(String[] args) {
            Path configPath = null;
            int ticks = DEFAULT_TICKS;
            boolean showHelp = false;

            for (String arg : args) {
                if (arg.equals("--help") || arg.equals("-h")) {
                    showHelp = true;
                } else if (arg.startsWith("--config=")) {
                    configPath = Paths.get(arg.substring("--config=".length()));
                } else if (arg.startsWith("--ticks=")) {
                    ticks = Integer.parseInt(arg.substring("--ticks=".length()));
                } else {
                    throw new IllegalArgumentException("Unknown argument: " + arg);
                }
            }

            if (ticks <= 0) {
                throw new IllegalArgumentException("Ticks must be greater than zero");
            }

            return new Arguments(configPath, ticks, showHelp);
        }
    }
}
