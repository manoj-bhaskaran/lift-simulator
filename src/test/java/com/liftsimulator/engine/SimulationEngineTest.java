package com.liftsimulator.engine;

import com.liftsimulator.domain.Action;
import com.liftsimulator.domain.Direction;
import com.liftsimulator.domain.DoorState;
import com.liftsimulator.domain.LiftState;
import com.liftsimulator.domain.LiftStatus;
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
    public void testInitializesAtMinFloor() {
        SimulationEngine engine = new SimulationEngine(
                new FixedActionController(Action.IDLE),
                2,
                6
        );

        assertEquals(2, engine.getCurrentState().getFloor());
        assertEquals(Direction.IDLE, engine.getCurrentState().getDirection());
        assertEquals(DoorState.CLOSED, engine.getCurrentState().getDoorState());
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
        assertEquals(Direction.IDLE, engine.getCurrentState().getDirection());
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
                tickCount++;
                if (tickCount <= 3) {
                    return Action.MOVE_UP;  // Move up to floor 3
                } else if (tickCount == 4) {
                    return Action.IDLE;  // Stop before changing direction
                } else {
                    return Action.MOVE_DOWN;  // Now move down
                }
            }
        };

        engine = new SimulationEngine(moveToFloor3ThenDown, 0, 5);

        // Move up to floor 3
        for (int i = 0; i < 3; i++) {
            engine.tick();
        }
        assertEquals(3, engine.getCurrentState().getFloor());

        // Stop the lift (required before changing direction)
        engine.tick();
        assertEquals(LiftStatus.IDLE, engine.getCurrentState().getStatus());
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
        assertEquals(Direction.IDLE, engine.getCurrentState().getDirection());
    }

    @Test
    public void testMoveDownAtBottomFloorSetsDirectionIdle() {
        LiftController moveUpThenDownPastMin = new LiftController() {
            private int tickCount = 0;

            @Override
            public Action decideNextAction(LiftState state, long tick) {
                tickCount++;
                if (tickCount == 1) {
                    return Action.MOVE_UP;  // Move up to floor 1
                } else if (tickCount == 2) {
                    return Action.IDLE;  // Stop before changing direction
                } else {
                    return Action.MOVE_DOWN;  // Move down
                }
            }
        };

        SimulationEngine engine = new SimulationEngine(moveUpThenDownPastMin, 0, 3);

        // Move up to floor 1
        engine.tick();
        assertEquals(1, engine.getCurrentState().getFloor());
        assertEquals(Direction.UP, engine.getCurrentState().getDirection());

        // Stop the lift (required before changing direction)
        engine.tick();
        assertEquals(1, engine.getCurrentState().getFloor());
        assertEquals(Direction.IDLE, engine.getCurrentState().getDirection());

        // Move down to floor 0
        engine.tick();
        assertEquals(0, engine.getCurrentState().getFloor());
        assertEquals(Direction.DOWN, engine.getCurrentState().getDirection());

        // Try to move down at bottom floor - should set direction to IDLE
        engine.tick();
        assertEquals(0, engine.getCurrentState().getFloor());
        assertEquals(Direction.IDLE, engine.getCurrentState().getDirection());
    }

    @Test
    public void testLiftDoesNotMoveUpWhenDoorIsOpen() {
        // Given: lift at floor 1 with doors open
        LiftController openDoorThenMoveUp = new LiftController() {
            private int tickCount = 0;

            @Override
            public Action decideNextAction(LiftState state, long tick) {
                tickCount++;
                if (tickCount == 1) {
                    return Action.OPEN_DOOR;  // Start opening
                } else if (tickCount == 2) {
                    return Action.IDLE;  // Complete opening
                } else {
                    return Action.MOVE_UP;  // Try to move with doors open
                }
            }
        };

        SimulationEngine engine = new SimulationEngine(openDoorThenMoveUp, 0, 5);

        // Open the door (2 ticks)
        engine.tick();  // IDLE -> DOORS_OPENING
        engine.tick();  // DOORS_OPENING -> DOORS_OPEN
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
                tickCount++;
                if (tickCount <= 2) {
                    return Action.MOVE_UP;  // Move up to floor 2
                } else if (tickCount == 3) {
                    return Action.OPEN_DOOR;  // Start opening doors
                } else if (tickCount == 4) {
                    return Action.IDLE;  // Complete opening doors
                } else {
                    return Action.MOVE_DOWN;  // Try to move down with doors open
                }
            }
        };

        SimulationEngine engine = new SimulationEngine(moveUpTwiceThenOpenThenDown, 0, 5);

        // Move to floor 2
        engine.tick();
        engine.tick();
        assertEquals(2, engine.getCurrentState().getFloor());

        // Open the door (2 ticks)
        engine.tick();  // IDLE -> DOORS_OPENING
        engine.tick();  // DOORS_OPENING -> DOORS_OPEN
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
        LiftController openThenIdle = new LiftController() {
            private int tickCount = 0;

            @Override
            public Action decideNextAction(LiftState state, long tick) {
                tickCount++;
                if (tickCount == 1) {
                    return Action.OPEN_DOOR;  // Start opening doors
                } else {
                    return Action.IDLE;  // Complete door opening
                }
            }
        };

        SimulationEngine engine = new SimulationEngine(openThenIdle, 0, 5);
        assertEquals(DoorState.CLOSED, engine.getCurrentState().getDoorState());

        // When: OPEN_DOOR action (starts opening)
        engine.tick();
        assertEquals(LiftStatus.DOORS_OPENING, engine.getCurrentState().getStatus());
        assertEquals(DoorState.CLOSED, engine.getCurrentState().getDoorState());

        // Then: IDLE action completes opening
        engine.tick();
        assertEquals(DoorState.OPEN, engine.getCurrentState().getDoorState());
        assertEquals(Direction.IDLE, engine.getCurrentState().getDirection());
    }

    @Test
    public void testCloseDoorSetsDoorToClosed() {
        // Given: lift with doors open
        LiftController openThenClose = new LiftController() {
            private int tickCount = 0;

            @Override
            public Action decideNextAction(LiftState state, long tick) {
                tickCount++;
                if (tickCount == 1) {
                    return Action.OPEN_DOOR;  // Start opening
                } else if (tickCount == 2) {
                    return Action.IDLE;  // Complete opening
                } else if (tickCount == 3) {
                    return Action.CLOSE_DOOR;  // Start closing
                } else {
                    return Action.IDLE;  // Complete closing
                }
            }
        };

        SimulationEngine engine = new SimulationEngine(openThenClose, 0, 5);

        // Open the door (2 ticks: OPEN_DOOR -> IDLE)
        engine.tick();  // IDLE -> DOORS_OPENING
        assertEquals(LiftStatus.DOORS_OPENING, engine.getCurrentState().getStatus());
        engine.tick();  // DOORS_OPENING -> DOORS_OPEN
        assertEquals(DoorState.OPEN, engine.getCurrentState().getDoorState());

        // When: CLOSE_DOOR action (2 ticks: CLOSE_DOOR -> IDLE)
        engine.tick();  // DOORS_OPEN -> DOORS_CLOSING
        assertEquals(LiftStatus.DOORS_CLOSING, engine.getCurrentState().getStatus());
        engine.tick();  // DOORS_CLOSING -> IDLE

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
                tickCount++;
                if (tickCount <= 5) {
                    return Action.MOVE_UP;  // Move up to floor 5
                } else if (tickCount == 6) {
                    return Action.IDLE;  // Stop before changing direction
                } else {
                    return Action.MOVE_DOWN;  // Move down
                }
            }
        };

        SimulationEngine engine = new SimulationEngine(moveUpThenDown, 0, 10);

        // Move up to floor 5
        for (int i = 0; i < 5; i++) {
            engine.tick();
        }
        assertEquals(5, engine.getCurrentState().getFloor());

        // Stop the lift (required before changing direction)
        engine.tick();
        assertEquals(5, engine.getCurrentState().getFloor());
        assertEquals(LiftStatus.IDLE, engine.getCurrentState().getStatus());

        // Move down back to floor 0
        for (int expectedFloor = 4; expectedFloor >= 0; expectedFloor--) {
            engine.tick();
            assertEquals(expectedFloor, engine.getCurrentState().getFloor());
            assertEquals(DoorState.CLOSED, engine.getCurrentState().getDoorState());
        }
    }
}
