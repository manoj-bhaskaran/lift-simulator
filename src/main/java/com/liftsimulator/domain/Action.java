package com.liftsimulator.domain;

/**
 * Represents an action that the lift can perform in a single tick.
 */
public enum Action {
    MOVE_UP,
    MOVE_DOWN,
    OPEN_DOOR,
    CLOSE_DOOR,
    IDLE
}
