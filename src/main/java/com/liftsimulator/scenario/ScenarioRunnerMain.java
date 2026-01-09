package com.liftsimulator.scenario;

import com.liftsimulator.domain.ControllerStrategy;
import com.liftsimulator.domain.IdleParkingMode;
import com.liftsimulator.engine.ControllerFactory;
import com.liftsimulator.engine.LiftController;
import com.liftsimulator.engine.NaiveLiftController;
import com.liftsimulator.engine.SimulationEngine;

import java.io.IOException;
import java.nio.file.Path;

public class ScenarioRunnerMain {
    private static final String DEFAULT_SCENARIO_RESOURCE = "/scenarios/demo.scenario";
    private static final int DEFAULT_MIN_FLOOR = 0;
    private static final int DEFAULT_MAX_FLOOR = 10;
    private static final int DEFAULT_INITIAL_FLOOR = 0;
    private static final int DEFAULT_TRAVEL_TICKS_PER_FLOOR = 1;
    private static final int DEFAULT_DOOR_TRANSITION_TICKS = 2;
    private static final int DEFAULT_DOOR_DWELL_TICKS = 3;
    private static final int DEFAULT_DOOR_REOPEN_WINDOW_TICKS = -1;
    private static final int DEFAULT_HOME_FLOOR = 0;
    private static final int DEFAULT_IDLE_TIMEOUT_TICKS = 5;
    private static final IdleParkingMode DEFAULT_IDLE_PARKING_MODE = IdleParkingMode.PARK_TO_HOME_FLOOR;
    private static final ControllerStrategy DEFAULT_CONTROLLER_STRATEGY = ControllerStrategy.NEAREST_REQUEST_ROUTING;

    public static void main(String[] args) throws IOException {
        // Parse command-line arguments.
        String scenarioPath = null;
        ControllerStrategy controllerStrategyOverride = null;
        IdleParkingMode idleParkingModeOverride = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--help") || args[i].equals("-h")) {
                printUsage();
                System.exit(0);
            } else if ((args[i].equals("--controller") || args[i].equals("-c")) && i + 1 < args.length) {
                try {
                    controllerStrategyOverride = ControllerStrategy.valueOf(args[i + 1]);
                    i++;
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid controller strategy: " + args[i + 1]);
                    System.err.println("Valid options: NEAREST_REQUEST_ROUTING, DIRECTIONAL_SCAN");
                    System.exit(1);
                }
            } else if ((args[i].equals("--idle-parking") || args[i].equals("-p")) && i + 1 < args.length) {
                try {
                    idleParkingModeOverride = IdleParkingMode.valueOf(args[i + 1]);
                    i++;
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid idle parking mode: " + args[i + 1]);
                    System.err.println("Valid options: STAY_AT_CURRENT_FLOOR, PARK_TO_HOME_FLOOR");
                    System.exit(1);
                }
            } else if (!args[i].startsWith("-")) {
                // This is the scenario file path.
                scenarioPath = args[i];
            } else {
                System.err.println("Unknown argument: " + args[i]);
                printUsage();
                System.exit(1);
            }
        }

        ScenarioParser parser = new ScenarioParser();
        ScenarioDefinition scenario;

        if (scenarioPath != null) {
            scenario = parser.parse(Path.of(scenarioPath));
        } else {
            scenario = parser.parseResource(DEFAULT_SCENARIO_RESOURCE);
        }

        int minFloor = scenario.getConfiguredMinFloor() != null ? scenario.getConfiguredMinFloor() : DEFAULT_MIN_FLOOR;
        int maxFloor = scenario.getConfiguredMaxFloor() != null ? scenario.getConfiguredMaxFloor() : DEFAULT_MAX_FLOOR;
        if (scenario.getMinFloor() != null) {
            minFloor = Math.min(minFloor, scenario.getMinFloor());
        }
        if (scenario.getMaxFloor() != null) {
            maxFloor = Math.max(maxFloor, scenario.getMaxFloor());
        }
        int initialFloorValue = scenario.getInitialFloor() != null ? scenario.getInitialFloor() : DEFAULT_INITIAL_FLOOR;
        int initialFloor = Math.min(Math.max(initialFloorValue, minFloor), maxFloor);
        int travelTicksPerFloor = scenario.getTravelTicksPerFloor() != null
                ? scenario.getTravelTicksPerFloor()
                : DEFAULT_TRAVEL_TICKS_PER_FLOOR;
        int doorTransitionTicks = scenario.getDoorTransitionTicks() != null
                ? scenario.getDoorTransitionTicks()
                : DEFAULT_DOOR_TRANSITION_TICKS;
        int doorDwellTicks = scenario.getDoorDwellTicks() != null
                ? scenario.getDoorDwellTicks()
                : DEFAULT_DOOR_DWELL_TICKS;
        int doorReopenWindowTicks = scenario.getDoorReopenWindowTicks() != null
                ? scenario.getDoorReopenWindowTicks()
                : DEFAULT_DOOR_REOPEN_WINDOW_TICKS;
        int homeFloor = scenario.getHomeFloor() != null ? scenario.getHomeFloor() : DEFAULT_HOME_FLOOR;
        int idleTimeoutTicks = scenario.getIdleTimeoutTicks() != null
                ? scenario.getIdleTimeoutTicks()
                : DEFAULT_IDLE_TIMEOUT_TICKS;
        // Apply command-line overrides if provided, otherwise use scenario or defaults.
        IdleParkingMode idleParkingMode = idleParkingModeOverride != null
                ? idleParkingModeOverride
                : (scenario.getIdleParkingMode() != null
                    ? scenario.getIdleParkingMode()
                    : DEFAULT_IDLE_PARKING_MODE);
        ControllerStrategy controllerStrategy = controllerStrategyOverride != null
                ? controllerStrategyOverride
                : (scenario.getControllerStrategy() != null
                    ? scenario.getControllerStrategy()
                    : DEFAULT_CONTROLLER_STRATEGY);

        // Note: Scenarios currently require NaiveLiftController for request management.
        // If a non-NEAREST_REQUEST_ROUTING strategy is specified, we reject it for now.
        if (controllerStrategy != ControllerStrategy.NEAREST_REQUEST_ROUTING) {
            throw new UnsupportedOperationException(
                    "Scenarios currently only support NEAREST_REQUEST_ROUTING controller strategy. " +
                    "Strategy " + controllerStrategy + " is not compatible with scenario execution."
            );
        }

        // Print configuration.
        System.out.println("=== Lift Simulator - Scenario Runner ===");
        System.out.println("Scenario: " + (scenarioPath != null ? scenarioPath : DEFAULT_SCENARIO_RESOURCE));
        System.out.println("Controller Strategy: " + controllerStrategy
                + (controllerStrategyOverride != null ? " (overridden)" : ""));
        System.out.println("Idle Parking Mode: " + idleParkingMode
                + (idleParkingModeOverride != null ? " (overridden)" : ""));
        System.out.println();

        NaiveLiftController controller = (NaiveLiftController) ControllerFactory.createController(
                controllerStrategy,
                homeFloor,
                idleTimeoutTicks,
                idleParkingMode
        );
        SimulationEngine engine = SimulationEngine.builder(controller, minFloor, maxFloor)
                .initialFloor(initialFloor)
                .travelTicksPerFloor(travelTicksPerFloor)
                .doorTransitionTicks(doorTransitionTicks)
                .doorDwellTicks(doorDwellTicks)
                .doorReopenWindowTicks(doorReopenWindowTicks)
                .build();

        ScenarioRunner runner = new ScenarioRunner(engine, controller);
        runner.run(scenario);
    }

    private static void printUsage() {
        System.out.println("Usage: java -cp lift-simulator.jar com.liftsimulator.scenario.ScenarioRunnerMain [OPTIONS] [SCENARIO_FILE]");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  SCENARIO_FILE                   Path to scenario file (optional, uses demo.scenario if not provided)");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -c, --controller STRATEGY       Controller strategy to use (overrides scenario file)");
        System.out.println("                                  (NEAREST_REQUEST_ROUTING, DIRECTIONAL_SCAN)");
        System.out.println("  -p, --idle-parking MODE         Idle parking mode (overrides scenario file)");
        System.out.println("                                  (STAY_AT_CURRENT_FLOOR, PARK_TO_HOME_FLOOR)");
        System.out.println("  -h, --help                      Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -cp lift-simulator.jar com.liftsimulator.scenario.ScenarioRunnerMain");
        System.out.println("  java -cp lift-simulator.jar com.liftsimulator.scenario.ScenarioRunnerMain custom.scenario");
        System.out.println("  java -cp lift-simulator.jar com.liftsimulator.scenario.ScenarioRunnerMain --idle-parking STAY_AT_CURRENT_FLOOR");
        System.out.println("  java -cp lift-simulator.jar com.liftsimulator.scenario.ScenarioRunnerMain -c NEAREST_REQUEST_ROUTING -p PARK_TO_HOME_FLOOR custom.scenario");
    }
}
