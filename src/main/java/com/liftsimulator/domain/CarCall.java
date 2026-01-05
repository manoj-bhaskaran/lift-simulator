package com.liftsimulator.domain;

/**
 * Represents a car call request made from inside the lift (destination button pressed).
 *
 * @param destinationFloor The floor the passenger wants to go to
 */
public record CarCall(int destinationFloor) {
}
