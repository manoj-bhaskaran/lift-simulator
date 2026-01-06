package com.liftsimulator.domain;

/**
 * Represents a hall call request made from outside the lift (up or down button pressed).
 * <p>
 * A hall call indicates that a passenger is waiting at a specific floor and wants to
 * travel in a particular direction (UP or DOWN). The exact destination is unknown until
 * the passenger boards and makes a car call.
 * <p>
 * <b>Key characteristics:</b>
 * <ul>
 *   <li>Made from hall panels outside the lift</li>
 *   <li>Specifies origin floor and direction, not destination</li>
 *   <li>Direction is critical for efficient scheduling in smart algorithms</li>
 *   <li>Completed when lift arrives at the origin floor with doors open</li>
 * </ul>
 * <p>
 * <b>Why direction matters:</b> In direction-aware algorithms (SCAN, LOOK), the lift
 * should ideally be traveling in the requested direction when it services a hall call.
 * This enables collecting multiple passengers going the same direction efficiently.
 * <p>
 * Example: A person at floor 5 presses UP, expecting the lift to take them to a higher floor.
 *
 * @param floor     The floor where the request was made (origin floor)
 * @param direction The direction the passenger wants to travel (UP or DOWN, never IDLE)
 * @see CarCall
 * @see LiftRequest#hallCall(int, Direction)
 */
public record HallCall(int floor, Direction direction) {
}
