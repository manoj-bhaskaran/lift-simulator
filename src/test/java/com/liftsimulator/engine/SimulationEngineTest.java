package com.liftsimulator.engine;

import com.liftsimulator.domain.Action;
import com.liftsimulator.domain.Direction;
import com.liftsimulator.domain.DoorState;
import com.liftsimulator.domain.LiftState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for lift movement and door constraints.
 * These tests lock down the mechanical rules of the lift system.
 */
public class SimulationEngineTest {

    /**
     * Simple controller that always returns the same action.
     * Used for testing specific behaviors in isolation.
     */
    private static class FixedActionController implements LiftController {
        private final Action action;

        public FixedActionController(Action action) {
            this.action = action;
        }

        @Override
        public Action decideNextAction(LiftState state, long tick) {
            return action;
        }
    }

    @Test
    public void testMoveUpIncrementsFloorByOne() {
        // Given: lift at floor 0 with doors closed
        SimulationEngine engine = new SimulationEngine(
                new FixedActionController(Action.MOVE_UP),
                0,  // minFloor
                5   // maxFloor
        );

        // When: tick with MOVE_UP action
        engine.tick();

        // Then: floor should increment by 1
        assertEquals(1, engine.getCurrentState().getFloor());
        assertEquals(Direction.UP, engine.getCurrentState().getDirection());
    }

    @Test
    public void testMoveUpDoesNotExceedTopFloor() {
        // Given: lift at top floor (5) with doors closed
        SimulationEngine engine = new SimulationEngine(
                new FixedActionController(Action.MOVE_UP),
                0,
                5
        );

        // Move to top floor
        for (int i = 0; i < 5; i++) {
            engine.tick();
        }
        assertEquals(5, engine.getCurrentState().getFloor());

        // When: attempt to move up beyond top floor
        engine.tick();

        // Then: floor should remain at top floor
        assertEquals(5, engine.getCurrentState().getFloor());
    }

    @Test
    public void testMoveDownDecrementsFloorByOne() {
        // Given: lift at floor 3 with doors closed
        SimulationEngine engine = new SimulationEngine(
                new FixedActionController(Action.MOVE_UP),
                0,
                5
        );

        // Move up to floor 3
        for (int i = 0; i < 3; i++) {
            engine.tick();
        }
        assertEquals(3, engine.getCurrentState().getFloor());

        // Switch to MOVE_DOWN controller
        engine = new SimulationEngine(
                new FixedActionController(Action.IDLE),
                0,
                5
        );
        // Manually advance to floor 3
        for (int i = 0; i < 3; i++) {
            engine = new SimulationEngine(
                    new FixedActionController(Action.MOVE_UP),
                    0,
                    5
            );
            for (int j = 0; j <= i; j++) {
                engine.tick();
            }
        }

        // Create new engine at floor 3 going down
        engine = new SimulationEngine(
                new FixedActionController(Action.MOVE_DOWN),
                0,
                5
        );
        // Move to floor 3 first
        for (int i = 0; i < 3; i++) {
            engine = new SimulationEngine(
                    new FixedActionController(Action.MOVE_UP),
                    0,
                    5
            );
            for (int j = 0; j < 3; j++) {
                engine.tick();
            }
        }

        // Actually, let me use a better approach with a custom controller
        LiftController moveToFloor3ThenDown = new LiftController() {
            private int tickCount = 0;

            @Override
            public Action decideNextAction(LiftState state, long tick) {
                if (tickCount < 3) {
                    tickCount++;
                    return Action.MOVE_UP;
                }
                return Action.MOVE_DOWN;
            }
        };

        engine = new SimulationEngine(moveToFloor3ThenDown, 0, 5);

        // Move up to floor 3
        for (int i = 0; i < 3; i++) {
            engine.tick();
        }
        assertEquals(3, engine.getCurrentState().getFloor());

        // When: tick with MOVE_DOWN action
        engine.tick();

        // Then: floor should decrement by 1
        assertEquals(2, engine.getCurrentState().getFloor());
        assertEquals(Direction.DOWN, engine.getCurrentState().getDirection());
    }

    @Test
    public void testMoveDownDoesNotGoBelowGroundFloor() {
        // Given: lift at ground floor (0) with doors closed
        SimulationEngine engine = new SimulationEngine(
                new FixedActionController(Action.MOVE_DOWN),
                0,
                5
        );

        // When: attempt to move down from ground floor
        engine.tick();

        // Then: floor should remain at ground floor
        assertEquals(0, engine.getCurrentState().getFloor());
    }

    @Test
    public void testLiftDoesNotMoveUpWhenDoorIsOpen() {
        // Given: lift at floor 1 with doors open
        LiftController openDoorThenMoveUp = new LiftController() {
            private int tickCount = 0;

            @Override
            public Action decideNextAction(LiftState state, long tick) {
                if (tickCount == 0) {
                    tickCount++;
                    return Action.OPEN_DOOR;
                }
                return Action.MOVE_UP;
            }
        };

        SimulationEngine engine = new SimulationEngine(openDoorThenMoveUp, 0, 5);

        // Open the door
        engine.tick();
        assertEquals(DoorState.OPEN, engine.getCurrentState().getDoorState());
        int floorBeforeMove = engine.getCurrentState().getFloor();

        // When: attempt to move up with door open
        engine.tick();

        // Then: floor should not change
        assertEquals(floorBeforeMove, engine.getCurrentState().getFloor());
        assertEquals(DoorState.OPEN, engine.getCurrentState().getDoorState());
    }

    @Test
    public void testLiftDoesNotMoveDownWhenDoorIsOpen() {
        // Given: lift at floor 2 with doors open
        LiftController moveUpTwiceThenOpenThenDown = new LiftController() {
            private int tickCount = 0;

            @Override
            public Action decideNextAction(LiftState state, long tick) {
                if (tickCount < 2) {
                    tickCount++;
                    return Action.MOVE_UP;
                } else if (tickCount == 2) {
                    tickCount++;
                    return Action.OPEN_DOOR;
                }
                return Action.MOVE_DOWN;
            }
        };

        SimulationEngine engine = new SimulationEngine(moveUpTwiceThenOpenThenDown, 0, 5);

        // Move to floor 2
        engine.tick();
        engine.tick();
        assertEquals(2, engine.getCurrentState().getFloor());

        // Open the door
        engine.tick();
        assertEquals(DoorState.OPEN, engine.getCurrentState().getDoorState());
        int floorBeforeMove = engine.getCurrentState().getFloor();

        // When: attempt to move down with door open
        engine.tick();

        // Then: floor should not change
        assertEquals(floorBeforeMove, engine.getCurrentState().getFloor());
        assertEquals(DoorState.OPEN, engine.getCurrentState().getDoorState());
    }

    @Test
    public void testOpenDoorSetsDoorToOpen() {
        // Given: lift with doors closed
        SimulationEngine engine = new SimulationEngine(
                new FixedActionController(Action.OPEN_DOOR),
                0,
                5
        );

        assertEquals(DoorState.CLOSED, engine.getCurrentState().getDoorState());

        // When: OPEN_DOOR action
        engine.tick();

        // Then: door should be open
        assertEquals(DoorState.OPEN, engine.getCurrentState().getDoorState());
        assertEquals(Direction.IDLE, engine.getCurrentState().getDirection());
    }

    @Test
    public void testCloseDoorSetsDoorToClosed() {
        // Given: lift with doors open
        LiftController openThenClose = new LiftController() {
            private boolean doorOpened = false;

            @Override
            public Action decideNextAction(LiftState state, long tick) {
                if (!doorOpened) {
                    doorOpened = true;
                    return Action.OPEN_DOOR;
                }
                return Action.CLOSE_DOOR;
            }
        };

        SimulationEngine engine = new SimulationEngine(openThenClose, 0, 5);

        // Open the door
        engine.tick();
        assertEquals(DoorState.OPEN, engine.getCurrentState().getDoorState());

        // When: CLOSE_DOOR action
        engine.tick();

        // Then: door should be closed
        assertEquals(DoorState.CLOSED, engine.getCurrentState().getDoorState());
    }

    @Test
    public void testMoveUpSequence() {
        // Test multiple moves up in sequence
        SimulationEngine engine = new SimulationEngine(
                new FixedActionController(Action.MOVE_UP),
                0,
                10
        );

        for (int expectedFloor = 1; expectedFloor <= 10; expectedFloor++) {
            engine.tick();
            assertEquals(expectedFloor, engine.getCurrentState().getFloor());
            assertEquals(DoorState.CLOSED, engine.getCurrentState().getDoorState());
        }
    }

    @Test
    public void testMoveDownSequence() {
        // Test multiple moves down in sequence
        LiftController moveUpThenDown = new LiftController() {
            private int tickCount = 0;

            @Override
            public Action decideNextAction(LiftState state, long tick) {
                if (tickCount < 5) {
                    tickCount++;
                    return Action.MOVE_UP;
                }
                return Action.MOVE_DOWN;
            }
        };

        SimulationEngine engine = new SimulationEngine(moveUpThenDown, 0, 10);

        // Move up to floor 5
        for (int i = 0; i < 5; i++) {
            engine.tick();
        }
        assertEquals(5, engine.getCurrentState().getFloor());

        // Move down back to floor 0
        for (int expectedFloor = 4; expectedFloor >= 0; expectedFloor--) {
            engine.tick();
            assertEquals(expectedFloor, engine.getCurrentState().getFloor());
            assertEquals(DoorState.CLOSED, engine.getCurrentState().getDoorState());
        }
    }
}
