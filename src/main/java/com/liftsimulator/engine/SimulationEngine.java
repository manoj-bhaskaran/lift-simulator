package com.liftsimulator.engine;

import com.liftsimulator.domain.Action;
import com.liftsimulator.domain.Direction;
import com.liftsimulator.domain.DoorState;
import com.liftsimulator.domain.LiftState;

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
        this.currentState = new LiftState(minFloor, Direction.IDLE, DoorState.CLOSED);
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
     */
    private LiftState applyAction(LiftState state, Action action) {
        int newFloor = state.getFloor();
        Direction newDirection = state.getDirection();
        DoorState newDoorState = state.getDoorState();

        switch (action) {
            case MOVE_UP:
                if (state.getDoorState() == DoorState.CLOSED) {
                    if (newFloor < maxFloor) {
                        newFloor++;
                        newDirection = Direction.UP;
                    } else if (newFloor == maxFloor) {
                        newDirection = Direction.IDLE;
                    }
                }
                break;
            case MOVE_DOWN:
                if (state.getDoorState() == DoorState.CLOSED) {
                    if (newFloor > minFloor) {
                        newFloor--;
                        newDirection = Direction.DOWN;
                    } else if (newFloor == minFloor) {
                        newDirection = Direction.IDLE;
                    }
                }
                break;
            case OPEN_DOOR:
                newDoorState = DoorState.OPEN;
                newDirection = Direction.IDLE;
                break;
            case CLOSE_DOOR:
                newDoorState = DoorState.CLOSED;
                break;
            case IDLE:
                newDirection = Direction.IDLE;
                break;
        }

        return new LiftState(newFloor, newDirection, newDoorState);
    }

    public long getCurrentTick() {
        return currentTick;
    }

    public LiftState getCurrentState() {
        return currentState;
    }
}
