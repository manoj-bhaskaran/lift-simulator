package com.liftsimulator.domain;

/**
 * Represents the current state of a lift.
 * Uses single source of truth pattern - only floor and status are stored.
 * Direction and door state are derived from the status.
 */
public class LiftState {
    private final int floor;
    private final LiftStatus status;

    public LiftState(int floor, LiftStatus status) {
        this.floor = floor;
        this.status = status;
    }

    public int getFloor() {
        return floor;
    }

    public LiftStatus getStatus() {
        return status;
    }

    /**
     * Derives the direction from the lift status.
     * Direction is computed, not stored.
     */
    public Direction getDirection() {
        return switch(status) {
            case MOVING_UP -> Direction.UP;
            case MOVING_DOWN -> Direction.DOWN;
            case IDLE, DOORS_OPENING, DOORS_OPEN, DOORS_CLOSING, OUT_OF_SERVICE -> Direction.IDLE;
        };
    }

    /**
     * Derives the door state from the lift status.
     * Door state is computed, not stored.
     */
    public DoorState getDoorState() {
        return switch(status) {
            case DOORS_OPEN -> DoorState.OPEN;
            case IDLE, MOVING_UP, MOVING_DOWN, DOORS_OPENING, DOORS_CLOSING, OUT_OF_SERVICE -> DoorState.CLOSED;
        };
    }

    @Override
    public String toString() {
        return "LiftState{" +
                "floor=" + floor +
                ", status=" + status +
                ", direction=" + getDirection() +
                ", doorState=" + getDoorState() +
                '}';
    }
}
