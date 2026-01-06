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
                    return Action.IDLE;  // Stop before opening doors
                } else if (tickCount == 4) {
                    return Action.OPEN_DOOR;  // Start opening doors
                } else if (tickCount == 5) {
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

        // Open the door (3 ticks: stop, open, complete)
        engine.tick();  // MOVING_UP -> IDLE
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
    public void testLiftDoesNotMoveWhileDoorsClosing() {
        LiftController openThenMoveUpDuringClose = new LiftController() {
            @Override
            public Action decideNextAction(LiftState state, long tick) {
                if (tick == 0) {
                    return Action.OPEN_DOOR;
                }
                if (state.getStatus() == LiftStatus.DOORS_CLOSING) {
                    return Action.MOVE_UP;
                }
                return Action.IDLE;
            }
        };

        SimulationEngine engine = new SimulationEngine(openThenMoveUpDuringClose, 0, 5, 1, 2, 1);

        engine.tick(); // start opening
        engine.tick(); // doors open
        assertEquals(DoorState.OPEN, engine.getCurrentState().getDoorState());

        engine.tick(); // dwell completes -> closing starts
        assertEquals(LiftStatus.DOORS_CLOSING, engine.getCurrentState().getStatus());
        int floorBeforeMove = engine.getCurrentState().getFloor();

        engine.tick(); // attempt move during closing
        assertEquals(floorBeforeMove, engine.getCurrentState().getFloor());
        assertEquals(LiftStatus.IDLE, engine.getCurrentState().getStatus());
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
        LiftController openThenIdle = new LiftController() {
            private int tickCount = 0;

            @Override
            public Action decideNextAction(LiftState state, long tick) {
                tickCount++;
                if (tickCount == 1) {
                    return Action.OPEN_DOOR;  // Start opening
                }
                return Action.IDLE;  // Let dwell/close cycle proceed
            }
        };

        SimulationEngine engine = new SimulationEngine(openThenIdle, 0, 5, 1, 2, 2);

        // Open the door (2 ticks: OPEN_DOOR -> IDLE)
        engine.tick();  // IDLE -> DOORS_OPENING
        assertEquals(LiftStatus.DOORS_OPENING, engine.getCurrentState().getStatus());
        engine.tick();  // DOORS_OPENING -> DOORS_OPEN
        assertEquals(DoorState.OPEN, engine.getCurrentState().getDoorState());

        // When: dwell completes, doors begin closing automatically
        engine.tick();  // dwell tick 1
        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());
        engine.tick();  // dwell tick 2 -> closing starts
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

    @Test
    public void testMoveUpHonorsTravelTicksPerFloor() {
        SimulationEngine engine = new SimulationEngine(
                new FixedActionController(Action.MOVE_UP),
                0,
                5,
                3,
                2
        );

        engine.tick();
        assertEquals(0, engine.getCurrentState().getFloor());
        assertEquals(LiftStatus.MOVING_UP, engine.getCurrentState().getStatus());

        engine.tick();
        assertEquals(0, engine.getCurrentState().getFloor());
        assertEquals(LiftStatus.MOVING_UP, engine.getCurrentState().getStatus());

        engine.tick();
        assertEquals(1, engine.getCurrentState().getFloor());
        assertEquals(LiftStatus.MOVING_UP, engine.getCurrentState().getStatus());
    }

    @Test
    public void testDoorTransitionConsumesConfiguredTicks() {
        SimulationEngine engine = new SimulationEngine(
                new FixedActionController(Action.OPEN_DOOR),
                0,
                5,
                1,
                3,
                2
        );

        engine.tick();
        assertEquals(LiftStatus.DOORS_OPENING, engine.getCurrentState().getStatus());
        assertEquals(DoorState.CLOSED, engine.getCurrentState().getDoorState());

        engine.tick();
        assertEquals(LiftStatus.DOORS_OPENING, engine.getCurrentState().getStatus());

        engine.tick();
        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());
        assertEquals(DoorState.OPEN, engine.getCurrentState().getDoorState());
    }

    @Test
    public void testDoorDwellTriggersClosingAfterConfiguredTicks() {
        LiftController openThenIdle = new LiftController() {
            private int tickCount = 0;

            @Override
            public Action decideNextAction(LiftState state, long tick) {
                tickCount++;
                if (tickCount == 1) {
                    return Action.OPEN_DOOR;
                }
                return Action.IDLE;
            }
        };

        SimulationEngine engine = new SimulationEngine(openThenIdle, 0, 5, 1, 2, 3);

        engine.tick(); // start opening
        engine.tick(); // open
        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());

        engine.tick(); // dwell 1
        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());

        engine.tick(); // dwell 2
        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());

        engine.tick(); // dwell 3 -> closing starts
        assertEquals(LiftStatus.DOORS_CLOSING, engine.getCurrentState().getStatus());

        engine.tick(); // closing completes
        assertEquals(LiftStatus.IDLE, engine.getCurrentState().getStatus());
        assertEquals(DoorState.CLOSED, engine.getCurrentState().getDoorState());
    }

    @Test
    public void testDeterministicResultsForFixedInputs() {
        SimulationEngine firstRun = new SimulationEngine(
                new FixedActionController(Action.MOVE_UP),
                0,
                5,
                2,
                2
        );

        SimulationEngine secondRun = new SimulationEngine(
                new FixedActionController(Action.MOVE_UP),
                0,
                5,
                2,
                2
        );

        for (int i = 0; i < 6; i++) {
            assertEquals(firstRun.getCurrentTick(), secondRun.getCurrentTick());
            assertEquals(firstRun.getCurrentState().getFloor(), secondRun.getCurrentState().getFloor());
            assertEquals(firstRun.getCurrentState().getStatus(), secondRun.getCurrentState().getStatus());
            firstRun.tick();
            secondRun.tick();
        }
    }

    @Test
    public void testDoorsReopenWhenRequestArrivesWithinWindow() {
        // Controller that opens doors, waits for them to start closing,
        // then requests to reopen within the window
        LiftController doorReopenController = new LiftController() {
            private int tickCount = 0;

            @Override
            public Action decideNextAction(LiftState state, long tick) {
                tickCount++;
                if (tickCount == 1) {
                    return Action.OPEN_DOOR;  // Start opening doors
                }
                if (state.getStatus() == LiftStatus.DOORS_CLOSING && tickCount == 5) {
                    // Request to reopen within window (after 1 tick of closing)
                    return Action.OPEN_DOOR;
                }
                return Action.IDLE;
            }
        };

        // doorTransitionTicks=2, doorDwellTicks=2, doorReopenWindowTicks=2
        SimulationEngine engine = new SimulationEngine(doorReopenController, 0, 5, 1, 2, 2, 2);

        engine.tick(); // tick 1: start opening
        engine.tick(); // tick 2: doors open
        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());

        engine.tick(); // tick 3: dwell 1
        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());

        engine.tick(); // tick 4: dwell 2 completes, closing starts and advances (elapsed becomes 1)
        assertEquals(LiftStatus.DOORS_CLOSING, engine.getCurrentState().getStatus());

        engine.tick(); // tick 5: OPEN_DOOR action within window (elapsed=1 < window=2)
        assertEquals(LiftStatus.DOORS_OPENING, engine.getCurrentState().getStatus());

        engine.tick(); // tick 6: complete opening
        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());
    }

    @Test
    public void testDoorsRemainClosedWhenWindowHasPassed() {
        // Controller that opens doors, waits for them to start closing,
        // then requests to reopen after the window has passed
        LiftController doorReopenController = new LiftController() {
            private int tickCount = 0;

            @Override
            public Action decideNextAction(LiftState state, long tick) {
                tickCount++;
                if (tickCount == 1) {
                    return Action.OPEN_DOOR;  // Start opening doors
                }
                if (state.getStatus() == LiftStatus.DOORS_CLOSING && tickCount == 8) {
                    // Request to reopen after window (after 3 ticks of closing, window is 2)
                    return Action.OPEN_DOOR;
                }
                return Action.IDLE;
            }
        };

        // doorTransitionTicks=4, doorDwellTicks=2, doorReopenWindowTicks=2
        SimulationEngine engine = new SimulationEngine(doorReopenController, 0, 5, 1, 4, 2, 2);

        engine.tick(); // tick 1: start opening
        engine.tick(); // tick 2: opening continues
        engine.tick(); // tick 3: opening continues
        engine.tick(); // tick 4: doors open
        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());

        engine.tick(); // tick 5: dwell 1
        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());

        engine.tick(); // tick 6: dwell 2 completes, closing starts and advances (elapsed becomes 1)
        assertEquals(LiftStatus.DOORS_CLOSING, engine.getCurrentState().getStatus());

        engine.tick(); // tick 7: closing continues (elapsed = 2, now outside window)
        assertEquals(LiftStatus.DOORS_CLOSING, engine.getCurrentState().getStatus());

        // New request arrives but outside window (elapsed = 2 >= window = 2)
        engine.tick(); // tick 8: OPEN_DOOR but outside window, stays closing (elapsed = 3)
        assertEquals(LiftStatus.DOORS_CLOSING, engine.getCurrentState().getStatus());

        engine.tick(); // tick 9: closing completes
        assertEquals(LiftStatus.IDLE, engine.getCurrentState().getStatus());
    }

    @Test
    public void testReopenWindowAtBoundary() {
        // Test that reopen works exactly at the boundary of the window
        LiftController doorReopenController = new LiftController() {
            private int tickCount = 0;

            @Override
            public Action decideNextAction(LiftState state, long tick) {
                tickCount++;
                if (tickCount == 1) {
                    return Action.OPEN_DOOR;
                }
                // Request reopen at exactly doorClosingTicksElapsed = 1 (window is 2, so 0 and 1 are valid)
                if (state.getStatus() == LiftStatus.DOORS_CLOSING && tickCount == 5) {
                    return Action.OPEN_DOOR;
                }
                return Action.IDLE;
            }
        };

        SimulationEngine engine = new SimulationEngine(doorReopenController, 0, 5, 1, 2, 2, 2);

        engine.tick(); // start opening
        engine.tick(); // doors open
        engine.tick(); // dwell 1
        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());

        engine.tick(); // dwell 2 completes, closing starts and advances (elapsed becomes 1)
        assertEquals(LiftStatus.DOORS_CLOSING, engine.getCurrentState().getStatus());

        engine.tick(); // OPEN_DOOR with elapsed=1 < window=2, should reopen
        assertEquals(LiftStatus.DOORS_OPENING, engine.getCurrentState().getStatus());
    }

    @Test
    public void testZeroReopenWindowPreventsReopening() {
        // Test that setting reopen window to 0 prevents any reopening
        LiftController doorReopenController = new LiftController() {
            private int tickCount = 0;

            @Override
            public Action decideNextAction(LiftState state, long tick) {
                tickCount++;
                if (tickCount == 1) {
                    return Action.OPEN_DOOR;
                }
                if (state.getStatus() == LiftStatus.DOORS_CLOSING && tickCount == 5) {
                    return Action.OPEN_DOOR;
                }
                return Action.IDLE;
            }
        };

        // doorReopenWindowTicks=0 means no reopening allowed
        SimulationEngine engine = new SimulationEngine(doorReopenController, 0, 5, 1, 2, 1, 0);

        engine.tick(); // start opening
        engine.tick(); // doors open
        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());

        engine.tick(); // dwell completes, closing starts and advances (elapsed = 1)
        assertEquals(LiftStatus.DOORS_CLOSING, engine.getCurrentState().getStatus());

        engine.tick(); // closing completes (window=0 prevents any reopening)
        assertEquals(LiftStatus.IDLE, engine.getCurrentState().getStatus());
    }

    @Test
    public void testReopenWindowValidation() {
        // Test that doorReopenWindowTicks cannot exceed doorTransitionTicks
        assertThrows(IllegalArgumentException.class, () -> {
            new SimulationEngine(new FixedActionController(Action.IDLE), 0, 5, 1, 2, 3, 3);
        });

        // Test that doorReopenWindowTicks cannot be negative
        assertThrows(IllegalArgumentException.class, () -> {
            new SimulationEngine(new FixedActionController(Action.IDLE), 0, 5, 1, 2, 3, -1);
        });

        // Test that valid values are accepted
        assertDoesNotThrow(() -> {
            new SimulationEngine(new FixedActionController(Action.IDLE), 0, 5, 1, 2, 3, 2);
        });

        assertDoesNotThrow(() -> {
            new SimulationEngine(new FixedActionController(Action.IDLE), 0, 5, 1, 2, 3, 0);
        });
    }
}
