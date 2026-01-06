package com.liftsimulator.domain;

/**
 * Represents the type of lift request, distinguishing between requests made from
 * outside versus inside the lift.
 * <p>
 * This distinction is critical for future smart scheduling algorithms:
 * <ul>
 *   <li><b>Hall calls</b> include direction information, enabling direction-aware
 *       routing (e.g., SCAN, LOOK algorithms)</li>
 *   <li><b>Car calls</b> specify exact destinations, allowing efficient drop-off
 *       sequencing</li>
 * </ul>
 * <p>
 * The distinction models real elevator hardware:
 * <ul>
 *   <li>Hall panels have up/down buttons (direction matters)</li>
 *   <li>Car panels have floor buttons (destination matters)</li>
 * </ul>
 *
 * @see LiftRequest
 * @see HallCall
 * @see CarCall
 */
public enum RequestType {
    /**
     * A request made from a floor (hall call).
     * <p>
     * The user is waiting at a floor and wants to travel in a specific direction.
     * The destination floor is unknown until the user boards and presses a car call button.
     * <p>
     * Completed when the lift arrives at the origin floor with doors open,
     * allowing the passenger to board.
     */
    HALL_CALL,

    /**
     * A request made from inside the lift (car call).
     * <p>
     * The user is already in the lift and has selected a destination floor.
     * The origin floor is typically unknown or irrelevant.
     * <p>
     * Completed when the lift arrives at the destination floor with doors open,
     * allowing the passenger to exit.
     */
    CAR_CALL
}
