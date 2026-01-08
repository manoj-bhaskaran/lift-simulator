package com.liftsimulator.scenario;

import com.liftsimulator.engine.NaiveLiftController;
import com.liftsimulator.engine.SimulationEngine;

import java.io.IOException;
import java.nio.file.Path;

public class ScenarioRunnerMain {
    private static final String DEFAULT_SCENARIO_RESOURCE = "/scenarios/demo.scenario";

    public static void main(String[] args) throws IOException {
        ScenarioParser parser = new ScenarioParser();
        ScenarioDefinition scenario;

        if (args.length > 0) {
            scenario = parser.parse(Path.of(args[0]));
        } else {
            scenario = parser.parseResource(DEFAULT_SCENARIO_RESOURCE);
        }

        NaiveLiftController controller = new NaiveLiftController();
        SimulationEngine engine = new SimulationEngine(controller, 0, 10);

        ScenarioRunner runner = new ScenarioRunner(engine, controller);
        runner.run(scenario);
    }
}
