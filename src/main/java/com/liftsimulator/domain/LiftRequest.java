package com.liftsimulator.domain;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a lift request as a first-class entity with explicit lifecycle states.
 * Tracks requests from creation through completion or cancellation.
 */
public class LiftRequest {
    private static final AtomicLong ID_GENERATOR = new AtomicLong(0);

    private final long id;
    private final RequestType type;
    private final Integer originFloor;
    private final Integer destinationFloor;
    private final Direction direction;
    private RequestState state;

    /**
     * Valid state transitions for a lift request.
     * Each state can only transition to specific next states.
     */
    private static final Set<RequestState> TERMINAL_STATES = Set.of(
        RequestState.COMPLETED,
        RequestState.CANCELLED
    );

    private LiftRequest(long id, RequestType type, Integer originFloor, Integer destinationFloor, Direction direction) {
        this.id = id;
        this.type = type;
        this.originFloor = originFloor;
        this.destinationFloor = destinationFloor;
        this.direction = direction;
        this.state = RequestState.CREATED;
    }

    /**
     * Creates a hall call request.
     *
     * @param floor     The floor where the call was made
     * @param direction The direction the passenger wants to travel
     * @return A new LiftRequest for the hall call
     */
    public static LiftRequest hallCall(int floor, Direction direction) {
        if (direction == Direction.IDLE) {
            throw new IllegalArgumentException("Hall call direction cannot be IDLE");
        }
        return new LiftRequest(ID_GENERATOR.incrementAndGet(), RequestType.HALL_CALL, floor, null, direction);
    }

    /**
     * Creates a car call request.
     *
     * @param destinationFloor The destination floor selected by the passenger
     * @return A new LiftRequest for the car call
     */
    public static LiftRequest carCall(int destinationFloor) {
        return new LiftRequest(ID_GENERATOR.incrementAndGet(), RequestType.CAR_CALL, null, destinationFloor, null);
    }

    /**
     * Creates a car call request with origin floor information.
     *
     * @param originFloor      The floor where the passenger entered the lift
     * @param destinationFloor The destination floor selected by the passenger
     * @return A new LiftRequest for the car call
     */
    public static LiftRequest carCall(int originFloor, int destinationFloor) {
        Direction direction = originFloor < destinationFloor ? Direction.UP :
                            originFloor > destinationFloor ? Direction.DOWN : Direction.IDLE;
        return new LiftRequest(ID_GENERATOR.incrementAndGet(), RequestType.CAR_CALL, originFloor, destinationFloor, direction);
    }

    /**
     * Advances the request to the next state.
     *
     * @param newState The state to transition to
     * @throws IllegalStateException if the transition is invalid
     */
    public void transitionTo(RequestState newState) {
        if (!isValidTransition(this.state, newState)) {
            throw new IllegalStateException(
                String.format("Invalid state transition for request %d: %s -> %s", id, state, newState)
            );
        }
        this.state = newState;
    }

    /**
     * Validates whether a state transition is allowed.
     *
     * @param from The current state
     * @param to   The target state
     * @return true if the transition is valid, false otherwise
     */
    private boolean isValidTransition(RequestState from, RequestState to) {
        if (from == to) {
            return false; // No self-transitions
        }

        if (TERMINAL_STATES.contains(from)) {
            return false; // Cannot transition from terminal states
        }

        return switch (from) {
            case CREATED -> to == RequestState.QUEUED || to == RequestState.CANCELLED;
            case QUEUED -> to == RequestState.ASSIGNED || to == RequestState.CANCELLED;
            case ASSIGNED -> to == RequestState.SERVING || to == RequestState.QUEUED || to == RequestState.CANCELLED;
            case SERVING -> to == RequestState.COMPLETED || to == RequestState.CANCELLED;
            default -> false;
        };
    }

    /**
     * Checks if this request is in a terminal state.
     *
     * @return true if the request is completed or cancelled
     */
    public boolean isTerminal() {
        return TERMINAL_STATES.contains(state);
    }

    /**
     * Gets the target floor for this request.
     * For hall calls, this is the origin floor.
     * For car calls, this is the destination floor.
     *
     * @return The target floor
     */
    public int getTargetFloor() {
        return type == RequestType.HALL_CALL ? originFloor : destinationFloor;
    }

    public long getId() {
        return id;
    }

    public RequestType getType() {
        return type;
    }

    public Integer getOriginFloor() {
        return originFloor;
    }

    public Integer getDestinationFloor() {
        return destinationFloor;
    }

    public Direction getDirection() {
        return direction;
    }

    public RequestState getState() {
        return state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LiftRequest that = (LiftRequest) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "LiftRequest{" +
                "id=" + id +
                ", type=" + type +
                ", originFloor=" + originFloor +
                ", destinationFloor=" + destinationFloor +
                ", direction=" + direction +
                ", state=" + state +
                '}';
    }
}
