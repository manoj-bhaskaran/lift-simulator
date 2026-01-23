package com.liftsimulator.scenario;

import com.liftsimulator.domain.ControllerStrategy;
import com.liftsimulator.domain.IdleParkingMode;
import com.liftsimulator.engine.ControllerFactory;
import com.liftsimulator.engine.RequestManagingLiftController;
import com.liftsimulator.engine.SimulationEngine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Backwards compatibility test for known-good CLI scenario input.
 */
public class ScenarioCliCompatibilityTest {
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

    @Test
    void testDemoScenarioStillRuns() throws Exception {
        ScenarioParser parser = new ScenarioParser();
        ScenarioDefinition scenario = parser.parseResource("/scenarios/demo.scenario");

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
        IdleParkingMode idleParkingMode = scenario.getIdleParkingMode() != null
                ? scenario.getIdleParkingMode()
                : DEFAULT_IDLE_PARKING_MODE;
        ControllerStrategy controllerStrategy = scenario.getControllerStrategy() != null
                ? scenario.getControllerStrategy()
                : DEFAULT_CONTROLLER_STRATEGY;

        RequestManagingLiftController controller = (RequestManagingLiftController) ControllerFactory.createController(
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

        assertNotNull(engine.getCurrentState());
        assertEquals(scenario.getTotalTicks(), engine.getCurrentTick());
    }
}
