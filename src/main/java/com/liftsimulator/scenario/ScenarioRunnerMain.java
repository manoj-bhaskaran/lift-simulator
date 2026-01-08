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

    public static void main(String[] args) throws IOException {
        ScenarioParser parser = new ScenarioParser();
        ScenarioDefinition scenario;

        if (args.length > 0) {
            scenario = parser.parse(Path.of(args[0]));
        } else {
            scenario = parser.parseResource(DEFAULT_SCENARIO_RESOURCE);
        }

        NaiveLiftController controller = new NaiveLiftController();
        int minFloor = DEFAULT_MIN_FLOOR;
        int maxFloor = DEFAULT_MAX_FLOOR;
        if (scenario.getMinFloor() != null) {
            minFloor = Math.min(minFloor, scenario.getMinFloor());
        }
        if (scenario.getMaxFloor() != null) {
            maxFloor = Math.max(maxFloor, scenario.getMaxFloor());
        }
        int initialFloor = Math.min(Math.max(DEFAULT_INITIAL_FLOOR, minFloor), maxFloor);
        SimulationEngine engine = new SimulationEngine(controller, minFloor, maxFloor, initialFloor);

        ScenarioRunner runner = new ScenarioRunner(engine, controller);
        runner.run(scenario);
    }
}
