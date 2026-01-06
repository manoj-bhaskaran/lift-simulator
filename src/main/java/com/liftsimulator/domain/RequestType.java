package com.liftsimulator.domain;

/**
 * Represents the type of lift request.
 */
public enum RequestType {
    /**
     * A request made from a floor (hall call).
     * The user is waiting at a floor and wants to travel in a specific direction.
     */
    HALL_CALL,

    /**
     * A request made from inside the lift (car call).
     * The user is already in the lift and has selected a destination floor.
     */
    CAR_CALL
}
