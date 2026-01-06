package com.liftsimulator.engine;

import com.liftsimulator.domain.Action;
import com.liftsimulator.domain.LiftState;
import com.liftsimulator.domain.LiftStatus;

/**
 * Core simulation engine that advances time in discrete ticks.
 * The engine owns time and coordinates state updates.
 */
public class SimulationEngine {
    private final SimulationClock clock;
    private LiftState currentState;
    private final LiftController controller;
    private final int minFloor;
    private final int maxFloor;
    private final int travelTicksPerFloor;
    private final int doorTransitionTicks;
    private final int doorDwellTicks;
    private final int doorReopenWindowTicks;
    private int movementTicksRemaining;
    private int doorTicksRemaining;
    private int doorDwellTicksRemaining;
    private int doorClosingTicksElapsed;

    public SimulationEngine(LiftController controller, int minFloor, int maxFloor) {
        this(controller, minFloor, maxFloor, 1, 2, 3, 2);
    }

    public SimulationEngine(
            LiftController controller,
            int minFloor,
            int maxFloor,
            int travelTicksPerFloor,
            int doorTransitionTicks
    ) {
        this(controller, minFloor, maxFloor, travelTicksPerFloor, doorTransitionTicks, 3, 2);
    }

    public SimulationEngine(
            LiftController controller,
            int minFloor,
            int maxFloor,
            int travelTicksPerFloor,
            int doorTransitionTicks,
            int doorDwellTicks
    ) {
        this(controller, minFloor, maxFloor, travelTicksPerFloor, doorTransitionTicks, doorDwellTicks, 2);
    }

    public SimulationEngine(
            LiftController controller,
            int minFloor,
            int maxFloor,
            int travelTicksPerFloor,
            int doorTransitionTicks,
            int doorDwellTicks,
            int doorReopenWindowTicks
    ) {
        if (travelTicksPerFloor <= 0) {
            throw new IllegalArgumentException("travelTicksPerFloor must be >= 1");
        }
        if (doorTransitionTicks <= 0) {
            throw new IllegalArgumentException("doorTransitionTicks must be >= 1");
        }
        if (doorDwellTicks <= 0) {
            throw new IllegalArgumentException("doorDwellTicks must be >= 1");
        }
        if (doorReopenWindowTicks < 0) {
            throw new IllegalArgumentException("doorReopenWindowTicks must be >= 0");
        }
        if (doorReopenWindowTicks > doorTransitionTicks) {
            throw new IllegalArgumentException("doorReopenWindowTicks cannot exceed doorTransitionTicks");
        }
        this.controller = controller;
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
        this.travelTicksPerFloor = travelTicksPerFloor;
        this.doorTransitionTicks = doorTransitionTicks;
        this.doorDwellTicks = doorDwellTicks;
        this.doorReopenWindowTicks = doorReopenWindowTicks;
        this.clock = new SimulationClock();
        this.movementTicksRemaining = 0;
        this.doorTicksRemaining = 0;
        this.doorDwellTicksRemaining = 0;
        this.doorClosingTicksElapsed = 0;
        // Initialize lift at minimum floor, idle status
        this.currentState = new LiftState(minFloor, LiftStatus.IDLE);
    }

    /**
     * Advances the simulation by one tick.
     * The controller decides the next action, which is then applied to update the state.
     */
    public void tick() {
        // Ask controller for next action
        Action action = controller.decideNextAction(currentState, clock.getCurrentTick());

        // Advance any in-progress movement or door transition
        if (movementTicksRemaining > 0) {
            advanceMovement();
            clock.tick();
            return;
        }

        if (doorTicksRemaining > 0) {
            // Special case: check for door reopening during closing
            if (currentState.getStatus() == LiftStatus.DOORS_CLOSING &&
                action == Action.OPEN_DOOR &&
                doorClosingTicksElapsed < doorReopenWindowTicks) {
                // Interrupt closing and start reopening
                currentState = new LiftState(currentState.getFloor(), LiftStatus.DOORS_OPENING);
                doorTicksRemaining = doorTransitionTicks;
                doorClosingTicksElapsed = 0;
            }
            advanceDoorTransition();
            clock.tick();
            return;
        }

        if (doorDwellTicksRemaining > 0) {
            advanceDoorDwell();
            clock.tick();
            return;
        }

        // Apply action to get new state
        currentState = startAction(currentState, action);

        if (movementTicksRemaining > 0) {
            advanceMovement();
        } else if (doorTicksRemaining > 0) {
            advanceDoorTransition();
        } else if (doorDwellTicksRemaining > 0) {
            advanceDoorDwell();
        }

        // Increment tick counter
        clock.tick();
    }

    /**
     * Applies an action to the current state and returns the new state.
     * Enforces state machine transitions and ensures lift never moves with doors open.
     */
    private LiftState startAction(LiftState state, Action action) {
        int newFloor = state.getFloor();
        LiftStatus currentStatus = state.getStatus();
        LiftStatus newStatus = currentStatus;

        // Prevent movement if doors are not closed (enforced by state machine)
        if ((action == Action.MOVE_UP || action == Action.MOVE_DOWN) &&
            (currentStatus == LiftStatus.DOORS_OPENING ||
             currentStatus == LiftStatus.DOORS_OPEN ||
             currentStatus == LiftStatus.DOORS_CLOSING ||
             currentStatus == LiftStatus.OUT_OF_SERVICE)) {
            // Invalid state for movement - log warning and return unchanged state
            StateTransitionValidator.isActionAllowed(currentStatus, action);
            return state;
        }

        switch (action) {
            case MOVE_UP:
                if (newFloor < maxFloor) {
                    newStatus = LiftStatus.MOVING_UP;
                    movementTicksRemaining = travelTicksPerFloor;
                    doorTicksRemaining = 0;
                    doorDwellTicksRemaining = 0;
                } else if (newFloor == maxFloor) {
                    // At top floor, can't move up
                    newStatus = LiftStatus.IDLE;
                }
                break;
            case MOVE_DOWN:
                if (newFloor > minFloor) {
                    newStatus = LiftStatus.MOVING_DOWN;
                    movementTicksRemaining = travelTicksPerFloor;
                    doorTicksRemaining = 0;
                    doorDwellTicksRemaining = 0;
                } else if (newFloor == minFloor) {
                    // At bottom floor, can't move down
                    newStatus = LiftStatus.IDLE;
                }
                break;
            case OPEN_DOOR:
                if (currentStatus == LiftStatus.OUT_OF_SERVICE) {
                    // Can't open doors when out of service
                    StateTransitionValidator.isActionAllowed(currentStatus, action);
                    return state;
                } else if (currentStatus == LiftStatus.MOVING_UP || currentStatus == LiftStatus.MOVING_DOWN) {
                    // Must stop first before opening doors
                    newStatus = LiftStatus.IDLE;
                } else if (currentStatus == LiftStatus.IDLE) {
                    // Can now start opening doors
                    newStatus = LiftStatus.DOORS_OPENING;
                    doorTicksRemaining = doorTransitionTicks;
                    movementTicksRemaining = 0;
                    doorDwellTicksRemaining = 0;
                    doorClosingTicksElapsed = 0;
                }
                // Note: DOORS_CLOSING reopen handled in tick() to interrupt in-progress transition
                // If already DOORS_OPENING or DOORS_OPEN, stay in current state
                break;
            case CLOSE_DOOR:
                if (doorDwellTicksRemaining > 0) {
                    return state;
                }
                if (currentStatus == LiftStatus.DOORS_OPEN || currentStatus == LiftStatus.DOORS_OPENING) {
                    newStatus = LiftStatus.DOORS_CLOSING;
                    doorTicksRemaining = doorTransitionTicks;
                    movementTicksRemaining = 0;
                    doorDwellTicksRemaining = 0;
                    doorClosingTicksElapsed = 0;  // Reset when doors start closing
                }
                break;
            case IDLE:
                // Complete transitional states or stop movement
                if (currentStatus == LiftStatus.MOVING_UP ||
                           currentStatus == LiftStatus.MOVING_DOWN) {
                    newStatus = LiftStatus.IDLE;
                }
                // Otherwise stay in current status
                break;
        }

        // Validate the state transition
        if (!StateTransitionValidator.isValidTransition(currentStatus, newStatus)) {
            // Invalid transition - return current state unchanged
            return state;
        }

        return new LiftState(newFloor, newStatus);
    }

    public long getCurrentTick() {
        return clock.getCurrentTick();
    }

    public LiftState getCurrentState() {
        return currentState;
    }

    private void advanceMovement() {
        movementTicksRemaining--;
        if (movementTicksRemaining > 0) {
            return;
        }

        int newFloor = currentState.getFloor();
        LiftStatus newStatus = currentState.getStatus();

        if (currentState.getStatus() == LiftStatus.MOVING_UP) {
            newFloor = Math.min(maxFloor, newFloor + 1);
        } else if (currentState.getStatus() == LiftStatus.MOVING_DOWN) {
            newFloor = Math.max(minFloor, newFloor - 1);
        }

        if (StateTransitionValidator.isValidTransition(currentState.getStatus(), newStatus)) {
            currentState = new LiftState(newFloor, newStatus);
        }
    }

    private void advanceDoorTransition() {
        doorTicksRemaining--;

        // Track elapsed ticks during door closing
        if (currentState.getStatus() == LiftStatus.DOORS_CLOSING) {
            doorClosingTicksElapsed++;
        }

        if (doorTicksRemaining > 0) {
            return;
        }

        LiftStatus newStatus = currentState.getStatus();
        if (currentState.getStatus() == LiftStatus.DOORS_OPENING) {
            newStatus = LiftStatus.DOORS_OPEN;
        } else if (currentState.getStatus() == LiftStatus.DOORS_CLOSING) {
            newStatus = LiftStatus.IDLE;
            doorClosingTicksElapsed = 0;  // Reset when door closing completes
        }

        if (StateTransitionValidator.isValidTransition(currentState.getStatus(), newStatus)) {
            currentState = new LiftState(currentState.getFloor(), newStatus);
            if (newStatus == LiftStatus.DOORS_OPEN) {
                doorDwellTicksRemaining = doorDwellTicks;
            }
        }
    }

    private void advanceDoorDwell() {
        if (currentState.getStatus() != LiftStatus.DOORS_OPEN) {
            doorDwellTicksRemaining = 0;
            return;
        }

        doorDwellTicksRemaining--;
        if (doorDwellTicksRemaining > 0) {
            return;
        }

        LiftStatus newStatus = LiftStatus.DOORS_CLOSING;
        if (StateTransitionValidator.isValidTransition(currentState.getStatus(), newStatus)) {
            currentState = new LiftState(currentState.getFloor(), newStatus);
            doorTicksRemaining = doorTransitionTicks;
            doorClosingTicksElapsed = 0;  // Reset when automatic door closing starts
            advanceDoorTransition();
        }
    }
}
