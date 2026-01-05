package com.liftsimulator.domain;

/**
 * Represents a hall call request made from a floor (up or down button pressed).
 *
 * @param floor     The floor where the request was made
 * @param direction The direction the passenger wants to travel
 */
public record HallCall(int floor, Direction direction) {
}
