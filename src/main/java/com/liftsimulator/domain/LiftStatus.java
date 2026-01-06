package com.liftsimulator.domain;

/**
 * Represents the operational status of the lift in the state machine.
 * This enum defines all possible states the lift can be in and helps enforce
 * valid state transitions.
 */
public enum LiftStatus {
    /**
     * Lift is idle - not moving and not servicing any requests.
     * Doors are closed.
     */
    IDLE,

    /**
     * Lift is moving upward between floors.
     * Doors must be closed in this state.
     */
    MOVING_UP,

    /**
     * Lift is moving downward between floors.
     * Doors must be closed in this state.
     */
    MOVING_DOWN,

    /**
     * Lift doors are open at a floor.
     * Lift cannot move in this state.
     */
    DOORS_OPEN,

    /**
     * Lift doors are in the process of closing.
     * This is a transitional state before movement can begin.
     */
    DOORS_CLOSING,

    /**
     * Lift is out of service and cannot accept or service requests.
     * This is used for maintenance or emergency situations.
     */
    OUT_OF_SERVICE
}
