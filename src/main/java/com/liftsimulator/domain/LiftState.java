package com.liftsimulator.domain;

/**
 * Represents the current state of a lift.
 */
public class LiftState {
    private final int floor;
    private final Direction direction;
    private final DoorState doorState;
    private final LiftStatus status;

    public LiftState(int floor, Direction direction, DoorState doorState, LiftStatus status) {
        this.floor = floor;
        this.direction = direction;
        this.doorState = doorState;
        this.status = status;
    }

    public int getFloor() {
        return floor;
    }

    public Direction getDirection() {
        return direction;
    }

    public DoorState getDoorState() {
        return doorState;
    }

    public LiftStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "LiftState{" +
                "floor=" + floor +
                ", direction=" + direction +
                ", doorState=" + doorState +
                ", status=" + status +
                '}';
    }
}
