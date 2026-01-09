package com.liftsimulator.domain;

/**
 * Represents the controller algorithm used to decide lift actions.
 */
public enum ControllerStrategy {
    /**
     * Services the nearest request first (default behavior).
     * Uses NaiveLiftController implementation.
     */
    NEAREST_REQUEST_ROUTING,

    /**
     * Directional scan algorithm (future implementation).
     * Continues in the current direction while there are requests in that direction.
     */
    DIRECTIONAL_SCAN
}
