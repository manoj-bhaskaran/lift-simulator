package com.liftsimulator.scenario;

import com.liftsimulator.domain.ControllerStrategy;
import com.liftsimulator.domain.LiftState;
import com.liftsimulator.domain.LiftStatus;
import com.liftsimulator.engine.ControllerFactory;
import com.liftsimulator.engine.LiftController;
import com.liftsimulator.engine.NaiveLiftController;
import com.liftsimulator.engine.SimulationEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ScenarioIntegrationTest {
    @Test
    void testScenarioFileParsingAndExecution() throws Exception {
        ScenarioParser parser = new ScenarioParser();
        ScenarioDefinition scenario = parser.parseResource("/scenarios/test.scenario");

        int minFloor = scenario.getConfiguredMinFloor() != null ? scenario.getConfiguredMinFloor() : 0;
        int maxFloor = scenario.getConfiguredMaxFloor() != null ? scenario.getConfiguredMaxFloor() : 10;
        if (scenario.getMinFloor() != null) {
            minFloor = Math.min(minFloor, scenario.getMinFloor());
        }
        if (scenario.getMaxFloor() != null) {
            maxFloor = Math.max(maxFloor, scenario.getMaxFloor());
        }
        int initialFloorValue = scenario.getInitialFloor() != null ? scenario.getInitialFloor() : 0;
        int initialFloor = Math.min(Math.max(initialFloorValue, minFloor), maxFloor);
        int travelTicksPerFloor = scenario.getTravelTicksPerFloor() != null
                ? scenario.getTravelTicksPerFloor()
                : 1;
        int doorTransitionTicks = scenario.getDoorTransitionTicks() != null
                ? scenario.getDoorTransitionTicks()
                : 2;
        int doorDwellTicks = scenario.getDoorDwellTicks() != null
                ? scenario.getDoorDwellTicks()
                : 3;
        int doorReopenWindowTicks = scenario.getDoorReopenWindowTicks() != null
                ? scenario.getDoorReopenWindowTicks()
                : -1;
        int homeFloor = scenario.getHomeFloor() != null ? scenario.getHomeFloor() : 0;
        int idleTimeoutTicks = scenario.getIdleTimeoutTicks() != null
                ? scenario.getIdleTimeoutTicks()
                : 5;

        NaiveLiftController controller = new NaiveLiftController(homeFloor, idleTimeoutTicks);
        SimulationEngine engine = SimulationEngine.builder(controller, minFloor, maxFloor)
                .initialFloor(initialFloor)
                .travelTicksPerFloor(travelTicksPerFloor)
                .doorTransitionTicks(doorTransitionTicks)
                .doorDwellTicks(doorDwellTicks)
                .doorReopenWindowTicks(doorReopenWindowTicks)
                .build();

        ScenarioRunner runner = new ScenarioRunner(engine, controller);
        runner.run(scenario);

        LiftState finalState = engine.getCurrentState();
        assertEquals(2, finalState.getFloor());
        assertEquals(LiftStatus.IDLE, finalState.getStatus());
        assertTrue(controller.getRequests().isEmpty());
    }

    @Test
    void testInvalidScenarioHandling() {
        ScenarioParser parser = new ScenarioParser();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> parser.parseResource("/scenarios/invalid.scenario"));

        assertTrue(exception.getMessage().contains("Unknown action"));
    }

    @Test
    void testScenarioWithControllerStrategy() throws Exception {
        ScenarioParser parser = new ScenarioParser();
        ScenarioDefinition scenario = parser.parseResource("/scenarios/demo.scenario");

        // Verify the controller strategy is parsed correctly
        assertNotNull(scenario.getControllerStrategy());
        assertEquals(ControllerStrategy.NEAREST_REQUEST_ROUTING, scenario.getControllerStrategy());
    }

    @Test
    void testScenarioWithoutControllerStrategyUsesNull() throws Exception {
        ScenarioParser parser = new ScenarioParser();
        ScenarioDefinition scenario = parser.parseResource("/scenarios/test.scenario");

        // Scenarios without controller_strategy should have null
        assertNull(scenario.getControllerStrategy());
    }

    @Test
    void testControllerFactoryCreatesCorrectInstance() {
        ControllerStrategy strategy = ControllerStrategy.NEAREST_REQUEST_ROUTING;
        LiftController controller = ControllerFactory.createController(strategy);

        assertNotNull(controller);
        assertTrue(controller instanceof NaiveLiftController);
    }

    @Test
    void testInvalidControllerStrategyInScenario() {
        ScenarioParser parser = new ScenarioParser();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> parser.parseResource("/scenarios/invalid_controller_strategy.scenario"));

        assertTrue(exception.getMessage().contains("Invalid controller_strategy"));
    }
}
