package com.liftsimulator.admin.entity;

/**
 * Represents the type of scenario event.
 */
public enum EventType {
    /**
     * A hall call event - passenger waiting at a floor wanting to go in a specific direction.
     */
    HALL_CALL,

    /**
     * A car call event - passenger inside the lift selecting a destination floor.
     */
    CAR_CALL,

    /**
     * A cancellation event - cancel a previously created request.
     */
    CANCEL
}
