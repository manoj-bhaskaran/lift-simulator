package com.liftsimulator.domain;

/**
 * Represents a car call request made from inside the lift (destination button pressed).
 * <p>
 * A car call indicates that a passenger who is already inside the lift has selected
 * their destination floor. The origin floor and direction are typically not tracked
 * (or can be inferred from the lift's current position).
 * <p>
 * <b>Key characteristics:</b>
 * <ul>
 *   <li>Made from car panels inside the lift</li>
 *   <li>Specifies destination floor, not origin or direction</li>
 *   <li>Passenger is already onboard when request is made</li>
 *   <li>Completed when lift arrives at destination floor with doors open</li>
 * </ul>
 * <p>
 * <b>Difference from hall calls:</b> Car calls don't include direction because the
 * passenger is already committed to this lift. The lift will travel to the destination
 * regardless of current direction. This differs from hall calls where direction matters
 * for efficient pickup scheduling.
 * <p>
 * Example: A passenger boards at floor 2 and presses the button for floor 8.
 *
 * @param destinationFloor The floor the passenger wants to go to
 * @see HallCall
 * @see LiftRequest#carCall(int)
 */
public record CarCall(int destinationFloor) {
}
