package com.liftsimulator.domain;

/**
 * Represents the operational status of the lift in the state machine.
 * This enum is the single source of truth for lift state - all other state
 * properties (direction, door state) are derived from this status.
 */
public enum LiftStatus {
    /**
     * Lift is idle - not moving and doors are closed.
     * Ready to accept requests.
     */
    IDLE,

    /**
     * Lift is moving upward between floors.
     * Doors are closed and locked in this state.
     */
    MOVING_UP,

    /**
     * Lift is moving downward between floors.
     * Doors are closed and locked in this state.
     */
    MOVING_DOWN,

    /**
     * Lift doors are in the process of opening.
     * This is a transitional state. Lift is stationary.
     */
    DOORS_OPENING,

    /**
     * Lift doors are fully open at a floor.
     * Lift cannot move in this state. Passengers can enter/exit.
     */
    DOORS_OPEN,

    /**
     * Lift doors are in the process of closing.
     * This is a transitional state. Lift is stationary.
     */
    DOORS_CLOSING,

    /**
     * Lift is out of service and cannot accept or service requests.
     * This is used for maintenance or emergency situations.
     */
    OUT_OF_SERVICE
}
