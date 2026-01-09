package com.liftsimulator.domain;

/**
 * Represents the parking behavior when the lift becomes idle after a timeout.
 */
public enum IdleParkingMode {
    /**
     * Lift stays at its current floor when idle, no parking movement occurs.
     */
    STAY_AT_CURRENT_FLOOR,
    /**
     * Lift moves to the designated home floor when idle after timeout.
     */
    PARK_TO_HOME_FLOOR
}
