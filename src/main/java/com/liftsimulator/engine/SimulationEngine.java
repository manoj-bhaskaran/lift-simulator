package com.liftsimulator.engine;

import com.liftsimulator.domain.Action;
import com.liftsimulator.domain.Direction;
import com.liftsimulator.domain.DoorState;
import com.liftsimulator.domain.LiftState;
import com.liftsimulator.domain.LiftStatus;

/**
 * Core simulation engine that advances time in discrete ticks.
 * The engine owns time and coordinates state updates.
 */
public class SimulationEngine {
    private long currentTick;
    private LiftState currentState;
    private final LiftController controller;
    private final int minFloor;
    private final int maxFloor;

    public SimulationEngine(LiftController controller, int minFloor, int maxFloor) {
        this.controller = controller;
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
        this.currentTick = 0;
        // Initialize lift at minimum floor, idle, doors closed
        this.currentState = new LiftState(minFloor, Direction.IDLE, DoorState.CLOSED, LiftStatus.IDLE);
    }

    /**
     * Advances the simulation by one tick.
     * The controller decides the next action, which is then applied to update the state.
     */
    public void tick() {
        // Ask controller for next action
        Action action = controller.decideNextAction(currentState, currentTick);

        // Apply action to get new state
        currentState = applyAction(currentState, action);

        // Increment tick counter
        currentTick++;
    }

    /**
     * Applies an action to the current state and returns the new state.
     * Enforces state machine transitions and ensures lift never moves with doors open.
     */
    private LiftState applyAction(LiftState state, Action action) {
        int newFloor = state.getFloor();
        Direction newDirection = state.getDirection();
        DoorState newDoorState = state.getDoorState();
        LiftStatus currentStatus = state.getStatus();
        LiftStatus newStatus = currentStatus;

        // Prevent movement if doors are not closed (enforced by state machine)
        if ((action == Action.MOVE_UP || action == Action.MOVE_DOWN) &&
            state.getDoorState() != DoorState.CLOSED) {
            // Invalid action - doors must be closed to move
            // Keep current state and log warning
            StateTransitionValidator.isActionAllowed(currentStatus, action);
            return state;
        }

        // Prevent movement from invalid states
        if ((action == Action.MOVE_UP || action == Action.MOVE_DOWN) &&
            (currentStatus == LiftStatus.DOORS_OPEN ||
             currentStatus == LiftStatus.DOORS_CLOSING ||
             currentStatus == LiftStatus.OUT_OF_SERVICE)) {
            // Invalid state for movement
            StateTransitionValidator.isActionAllowed(currentStatus, action);
            return state;
        }

        switch (action) {
            case MOVE_UP:
                if (state.getDoorState() == DoorState.CLOSED) {
                    if (newFloor < maxFloor) {
                        newFloor++;
                        newDirection = Direction.UP;
                        newStatus = LiftStatus.MOVING_UP;
                    } else if (newFloor == maxFloor) {
                        newDirection = Direction.IDLE;
                        newStatus = LiftStatus.IDLE;
                    }
                }
                break;
            case MOVE_DOWN:
                if (state.getDoorState() == DoorState.CLOSED) {
                    if (newFloor > minFloor) {
                        newFloor--;
                        newDirection = Direction.DOWN;
                        newStatus = LiftStatus.MOVING_DOWN;
                    } else if (newFloor == minFloor) {
                        newDirection = Direction.IDLE;
                        newStatus = LiftStatus.IDLE;
                    }
                }
                break;
            case OPEN_DOOR:
                // Can open doors when stopped or moving (lift stops first)
                if (currentStatus != LiftStatus.OUT_OF_SERVICE) {
                    newDoorState = DoorState.OPEN;
                    newDirection = Direction.IDLE;
                    newStatus = LiftStatus.DOORS_OPEN;
                } else {
                    // Invalid - can't open doors when out of service
                    StateTransitionValidator.isActionAllowed(currentStatus, action);
                    return state;
                }
                break;
            case CLOSE_DOOR:
                if (currentStatus == LiftStatus.DOORS_OPEN) {
                    newDoorState = DoorState.CLOSED;
                    // For now, transition directly to IDLE (instant door closing)
                    // In future, could use DOORS_CLOSING for more realistic timing
                    newStatus = LiftStatus.IDLE;
                } else if (currentStatus == LiftStatus.DOORS_CLOSING) {
                    // Complete the closing transition
                    newDoorState = DoorState.CLOSED;
                    newStatus = LiftStatus.IDLE;
                }
                break;
            case IDLE:
                if (currentStatus == LiftStatus.DOORS_CLOSING) {
                    // Transition from DOORS_CLOSING to IDLE
                    newStatus = LiftStatus.IDLE;
                } else if (currentStatus == LiftStatus.MOVING_UP ||
                           currentStatus == LiftStatus.MOVING_DOWN) {
                    // Stop movement
                    newStatus = LiftStatus.IDLE;
                }
                newDirection = Direction.IDLE;
                break;
        }

        // Validate the state transition
        if (!StateTransitionValidator.isValidTransition(currentStatus, newStatus)) {
            // Invalid transition - return current state unchanged
            return state;
        }

        return new LiftState(newFloor, newDirection, newDoorState, newStatus);
    }

    public long getCurrentTick() {
        return currentTick;
    }

    public LiftState getCurrentState() {
        return currentState;
    }
}
