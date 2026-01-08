package com.liftsimulator.scenario;

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

    public static void main(String[] args) throws IOException {
        ScenarioParser parser = new ScenarioParser();
        ScenarioDefinition scenario;

        if (args.length > 0) {
            scenario = parser.parse(Path.of(args[0]));
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

        NaiveLiftController controller = new NaiveLiftController(homeFloor, idleTimeoutTicks);
        SimulationEngine engine = new SimulationEngine(
                controller,
                minFloor,
                maxFloor,
                initialFloor,
                travelTicksPerFloor,
                doorTransitionTicks,
                doorDwellTicks,
                doorReopenWindowTicks
        );

        ScenarioRunner runner = new ScenarioRunner(engine, controller);
        runner.run(scenario);
    }
}
