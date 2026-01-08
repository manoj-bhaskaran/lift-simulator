package com.liftsimulator.domain;

/**
 * Represents the direction of lift movement or a request.
 */
public enum Direction {
    /**
     * Indicates upward movement or an upward request.
     */
    UP,
    /**
     * Indicates downward movement or a downward request.
     */
    DOWN,
    /**
     * Indicates no active movement.
     */
    IDLE
}
