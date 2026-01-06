package com.liftsimulator.engine;

import com.liftsimulator.domain.Action;
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
        // Initialize lift at minimum floor, idle status
        this.currentState = new LiftState(minFloor, LiftStatus.IDLE);
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
                    newFloor++;
                    newStatus = LiftStatus.MOVING_UP;
                } else if (newFloor == maxFloor) {
                    // At top floor, can't move up
                    newStatus = LiftStatus.IDLE;
                }
                break;
            case MOVE_DOWN:
                if (newFloor > minFloor) {
                    newFloor--;
                    newStatus = LiftStatus.MOVING_DOWN;
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
                } else if (currentStatus == LiftStatus.DOORS_CLOSING) {
                    // Abort door closing, re-open
                    newStatus = LiftStatus.DOORS_OPENING;
                }
                // If already DOORS_OPENING or DOORS_OPEN, stay in current state
                break;
            case CLOSE_DOOR:
                if (currentStatus == LiftStatus.DOORS_OPEN || currentStatus == LiftStatus.DOORS_OPENING) {
                    newStatus = LiftStatus.DOORS_CLOSING;
                }
                break;
            case IDLE:
                // Complete transitional states or stop movement
                if (currentStatus == LiftStatus.DOORS_OPENING) {
                    newStatus = LiftStatus.DOORS_OPEN;
                } else if (currentStatus == LiftStatus.DOORS_CLOSING) {
                    newStatus = LiftStatus.IDLE;
                } else if (currentStatus == LiftStatus.MOVING_UP ||
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
        return currentTick;
    }

    public LiftState getCurrentState() {
        return currentState;
    }
}
