package com.liftsimulator.engine;

import com.liftsimulator.domain.Action;
import com.liftsimulator.domain.CarCall;
import com.liftsimulator.domain.Direction;
import com.liftsimulator.domain.DoorState;
import com.liftsimulator.domain.HallCall;
import com.liftsimulator.domain.IdleParkingMode;
import com.liftsimulator.domain.LiftRequest;
import com.liftsimulator.domain.LiftState;
import com.liftsimulator.domain.LiftStatus;
import com.liftsimulator.domain.RequestState;
import com.liftsimulator.domain.RequestType;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Implements a directional scan controller.
 * The controller maintains travel direction and services all requests in that direction
 * before reversing (or idling if none).
 */
public final class DirectionalScanLiftController implements RequestManagingLiftController {
    private static final int DEFAULT_HOME_FLOOR = 0;
    private static final int DEFAULT_IDLE_TIMEOUT_TICKS = 5;
    private static final IdleParkingMode DEFAULT_IDLE_PARKING_MODE = IdleParkingMode.PARK_TO_HOME_FLOOR;

    private final Map<Long, LiftRequest> requestsById = new HashMap<>();
    private final Set<LiftRequest> activeRequests = new HashSet<>();
    private final Set<LiftRequest> completedRequests = new HashSet<>();
    private final int homeFloor;
    private final int idleTimeoutTicks;
    private final IdleParkingMode idleParkingMode;
    private Long idleStartTick;
    private boolean parkingInProgress;
    private boolean outOfService;
    private Direction currentDirection = Direction.IDLE;

    public DirectionalScanLiftController() {
        this(DEFAULT_HOME_FLOOR, DEFAULT_IDLE_TIMEOUT_TICKS, DEFAULT_IDLE_PARKING_MODE);
    }

    public DirectionalScanLiftController(int homeFloor, int idleTimeoutTicks) {
        this(homeFloor, idleTimeoutTicks, DEFAULT_IDLE_PARKING_MODE);
    }

    public DirectionalScanLiftController(int homeFloor, int idleTimeoutTicks, IdleParkingMode idleParkingMode) {
        if (idleTimeoutTicks < 0) {
            throw new IllegalArgumentException("idleTimeoutTicks must be >= 0");
        }
        if (idleParkingMode == null) {
            throw new IllegalArgumentException("idleParkingMode must not be null");
        }
        this.homeFloor = homeFloor;
        this.idleTimeoutTicks = idleTimeoutTicks;
        this.idleParkingMode = idleParkingMode;
    }

    /**
     * Adds a car call (destination request from inside the lift).
     *
     * @param carCall the car call to add
     */
    public void addCarCall(CarCall carCall) {
        if (outOfService) {
            return;
        }
        LiftRequest request = LiftRequest.carCall(carCall.destinationFloor());
        request.transitionTo(RequestState.QUEUED);
        trackRequest(request);
    }

    /**
     * Adds a hall call (request from a floor to go up or down).
     *
     * @param hallCall the hall call to add
     */
    public void addHallCall(HallCall hallCall) {
        if (outOfService) {
            return;
        }
        LiftRequest request = LiftRequest.hallCall(hallCall.floor(), hallCall.direction());
        request.transitionTo(RequestState.QUEUED);
        trackRequest(request);
    }

    /**
     * Adds a lift request directly.
     *
     * @param request the lift request to add
     */
    public void addRequest(LiftRequest request) {
        if (outOfService) {
            return;
        }
        if (request.getState() == RequestState.CREATED) {
            request.transitionTo(RequestState.QUEUED);
        }
        trackRequest(request);
    }

    /**
     * Cancels a lift request by its ID.
     * The request will be transitioned to CANCELLED state and removed from the queue.
     * This method is safe to call for requests in any state.
     *
     * @param requestId the ID of the request to cancel
     * @return true if the request was found and cancelled, false if not found or already terminal
     */
    public boolean cancelRequest(long requestId) {
        LiftRequest request = requestsById.get(requestId);
        if (request == null) {
            return false;
        }

        // If already terminal (completed or cancelled), nothing to do.
        if (request.isTerminal()) {
            return false;
        }

        // Transition to cancelled state.
        request.transitionTo(RequestState.CANCELLED);

        archiveRequest(request);

        return true;
    }

    /**
     * Completes all requests for the given floor.
     * This is called when the lift arrives at a floor and opens doors.
     * Transitions requests from SERVING to COMPLETED.
     *
     * @param floor the floor to complete requests for
     */
    private void completeRequestsForFloor(int floor, Direction direction) {
        Set<LiftRequest> requestsToComplete = activeRequests.stream()
                .filter(request -> !request.isTerminal() && request.getTargetFloor() == floor)
                .filter(request -> isEligibleForDirection(request, direction))
                .collect(Collectors.toSet());

        for (LiftRequest request : requestsToComplete) {
            request.completeRequest();
        }

        // Remove completed requests.
        for (LiftRequest request : requestsToComplete) {
            if (request.isTerminal()) {
                archiveRequest(request);
            }
        }
    }

    /**
     * Checks if there is any active request for the given floor.
     *
     * @param floor the floor to check
     * @return true if there is an active request for this floor
     */
    private boolean hasRequestForFloor(int floor, Direction direction) {
        return activeRequests.stream()
                .filter(request -> !request.isTerminal())
                .filter(request -> isEligibleForDirection(request, direction))
                .anyMatch(request -> request.getTargetFloor() == floor);
    }

    /**
     * Gets all active (non-terminal) requests.
     *
     * @return set of active requests
     */
    private Set<LiftRequest> getActiveRequests() {
        return Collections.unmodifiableSet(activeRequests);
    }

    private boolean hasRequestsInDirection(int currentFloor, Direction direction) {
        if (direction == Direction.IDLE) {
            return false;
        }
        return getActiveRequests().stream()
                .filter(request -> !request.isTerminal())
                .anyMatch(request -> direction == Direction.UP
                        ? request.getTargetFloor() > currentFloor
                        : request.getTargetFloor() < currentFloor);
    }

    /**
     * Finds the nearest floor with a pending request.
     * Assigns requests to the lift when they are selected for service.
     *
     * @param currentFloor the current floor of the lift
     * @return the nearest requested floor, or empty if no requests
     */
    private Optional<Integer> findNearestRequestedFloor(int currentFloor) {
        Set<Integer> requestedFloors = new TreeSet<>();

        // Collect all requested floors from active requests.
        for (LiftRequest request : getActiveRequests()) {
            requestedFloors.add(request.getTargetFloor());
        }

        // Find the nearest one.
        return requestedFloors.stream()
                .min((f1, f2) -> {
                    int dist1 = Math.abs(f1 - currentFloor);
                    int dist2 = Math.abs(f2 - currentFloor);
                    if (dist1 != dist2) {
                        return Integer.compare(dist1, dist2);
                    }
                    return Integer.compare(f1, f2);
                });
    }

    private Optional<Integer> findNextRequestedFloorInDirection(int currentFloor, Direction direction) {
        if (direction == Direction.IDLE) {
            return Optional.empty();
        }

        return getActiveRequests().stream()
                .filter(request -> isEligibleForDirection(request, direction))
                .map(LiftRequest::getTargetFloor)
                .filter(targetFloor -> direction == Direction.UP
                        ? targetFloor > currentFloor
                        : targetFloor < currentFloor)
                .sorted(direction == Direction.UP ? Integer::compareTo : (f1, f2) -> Integer.compare(f2, f1))
                .findFirst();
    }

    private Optional<Integer> findTurnaroundFloorInDirection(int currentFloor, Direction direction) {
        if (direction == Direction.IDLE) {
            return Optional.empty();
        }

        return getActiveRequests().stream()
                .filter(request -> !request.isTerminal())
                .map(LiftRequest::getTargetFloor)
                .filter(targetFloor -> direction == Direction.UP
                        ? targetFloor > currentFloor
                        : targetFloor < currentFloor)
                .reduce((floor1, floor2) -> direction == Direction.UP
                        ? Math.max(floor1, floor2)
                        : Math.min(floor1, floor2));
    }

    private boolean shouldReverseAtCurrentFloor(int currentFloor) {
        if (currentDirection == Direction.IDLE) {
            return false;
        }
        if (findTurnaroundFloorInDirection(currentFloor, currentDirection).isPresent()) {
            return false;
        }
        Direction oppositeDirection = currentDirection == Direction.UP ? Direction.DOWN : Direction.UP;
        return hasRequestForFloor(currentFloor, oppositeDirection);
    }

    /**
     * Assigns requests for the target floor to the lift.
     * Changes state from QUEUED to ASSIGNED.
     *
     * @param targetFloor floor that lift will go to.
     */
    private void assignRequestsForFloor(int targetFloor, Direction direction) {
        activeRequests.stream()
                .filter(request -> request.getTargetFloor() == targetFloor)
                .filter(request -> isEligibleForDirection(request, direction))
                .filter(request -> request.getState() == RequestState.QUEUED)
                .forEach(request -> request.transitionTo(RequestState.ASSIGNED));
    }

    /**
     * Marks assigned requests for the target floor as SERVING.
     *
     * @param targetFloor floor that lift is now serving.
     */
    private void serveRequestsForFloor(int targetFloor, Direction direction) {
        activeRequests.stream()
                .filter(request -> request.getTargetFloor() == targetFloor)
                .filter(request -> isEligibleForDirection(request, direction))
                .filter(request -> request.getState() == RequestState.ASSIGNED)
                .forEach(request -> request.transitionTo(RequestState.SERVING));
    }

    private void resetIdleTracking() {
        idleStartTick = null;
        parkingInProgress = false;
    }

    private boolean shouldTrackIdle(LiftState currentState) {
        return currentState.getStatus() == LiftStatus.IDLE && currentState.getDoorState() == DoorState.CLOSED;
    }

    private Action moveTowardHome(int currentFloor) {
        if (currentFloor < homeFloor) {
            return Action.MOVE_UP;
        }
        if (currentFloor > homeFloor) {
            return Action.MOVE_DOWN;
        }
        return Action.IDLE;
    }

    private Action handleIdleParking(LiftState currentState, long currentTick) {
        int currentFloor = currentState.getFloor();
        if (parkingInProgress) {
            if (currentFloor == homeFloor) {
                parkingInProgress = false;
                idleStartTick = currentTick;
                return Action.IDLE;
            }
            return moveTowardHome(currentFloor);
        }

        if (shouldTrackIdle(currentState)) {
            Long trackedIdleStartTick = idleStartTick;
            if (trackedIdleStartTick == null) {
                trackedIdleStartTick = currentTick;
                idleStartTick = currentTick;
            }
            long idleTicks = currentTick - trackedIdleStartTick;
            if (idleTicks >= idleTimeoutTicks && currentFloor != homeFloor
                    && idleParkingMode == IdleParkingMode.PARK_TO_HOME_FLOOR) {
                parkingInProgress = true;
                return moveTowardHome(currentFloor);
            }
        } else {
            idleStartTick = null;
        }
        return Action.IDLE;
    }

    private boolean isEligibleForDirection(LiftRequest request, Direction direction) {
        if (direction == Direction.IDLE) {
            return true;
        }
        if (request.getType() == RequestType.HALL_CALL) {
            return request.getDirection() == direction;
        }
        return true;
    }

    @Override
    public Action decideNextAction(LiftState currentState, long currentTick) {
        int currentFloor = currentState.getFloor();
        DoorState doorState = currentState.getDoorState();
        LiftStatus currentStatus = currentState.getStatus();
        boolean hasActiveRequests = !getActiveRequests().isEmpty();

        if (currentStatus == LiftStatus.OUT_OF_SERVICE || outOfService) {
            return Action.IDLE;
        }

        if (hasActiveRequests) {
            resetIdleTracking();
        }

        // Allow door transitions to complete before evaluating movement.
        if (currentStatus == LiftStatus.DOORS_OPENING) {
            if (hasRequestForFloor(currentFloor, currentDirection)) {
                serveRequestsForFloor(currentFloor, currentDirection);
                completeRequestsForFloor(currentFloor, currentDirection);
            }
            return Action.IDLE;
        }

        if (currentStatus == LiftStatus.DOORS_CLOSING) {
            // Check if a new request arrived for current floor while doors closing.
            if (hasRequestForFloor(currentFloor, currentDirection)) {
                // Attempt to reopen doors (will succeed only if within reopen window).
                return Action.OPEN_DOOR;
            }
            return Action.IDLE;
        }

        // If doors are open, let the engine manage the dwell/close cycle.
        if (doorState == DoorState.OPEN) {
            if (hasRequestForFloor(currentFloor, currentDirection)) {
                completeRequestsForFloor(currentFloor, currentDirection);
            }
            return Action.IDLE;
        }

        if (shouldReverseAtCurrentFloor(currentFloor)) {
            currentDirection = currentDirection == Direction.UP ? Direction.DOWN : Direction.UP;
        }

        // If at a requested floor, stop first if moving, then open doors.
        if (hasRequestForFloor(currentFloor, currentDirection)) {
            if (currentStatus == LiftStatus.MOVING_UP || currentStatus == LiftStatus.MOVING_DOWN) {
                assignRequestsForFloor(currentFloor, currentDirection);
                serveRequestsForFloor(currentFloor, currentDirection);
                return Action.IDLE;
            }
            if (currentStatus == LiftStatus.DOORS_OPENING) {
                completeRequestsForFloor(currentFloor, currentDirection);
                return Action.IDLE;
            }
            assignRequestsForFloor(currentFloor, currentDirection);
            serveRequestsForFloor(currentFloor, currentDirection);
            return Action.OPEN_DOOR;
        }

        if (!hasActiveRequests) {
            currentDirection = Direction.IDLE;
            return handleIdleParking(currentState, currentTick);
        }

        if (currentDirection == Direction.IDLE) {
            Optional<Integer> nearestFloor = findNearestRequestedFloor(currentFloor);
            if (nearestFloor.isPresent()) {
                int targetFloor = nearestFloor.get();
                currentDirection = targetFloor > currentFloor ? Direction.UP : Direction.DOWN;
            }
        }

        Optional<Integer> turnaroundFloor = findTurnaroundFloorInDirection(currentFloor, currentDirection);
        Optional<Integer> nextFloor = findNextRequestedFloorInDirection(currentFloor, currentDirection);
        if (nextFloor.isEmpty() && turnaroundFloor.isPresent()) {
            nextFloor = turnaroundFloor;
        }
        if (nextFloor.isEmpty()) {
            Direction oppositeDirection = currentDirection == Direction.UP ? Direction.DOWN : Direction.UP;
            if (hasRequestsInDirection(currentFloor, oppositeDirection)) {
                currentDirection = oppositeDirection;
                turnaroundFloor = findTurnaroundFloorInDirection(currentFloor, currentDirection);
                nextFloor = findNextRequestedFloorInDirection(currentFloor, currentDirection);
                if (nextFloor.isEmpty() && turnaroundFloor.isPresent()) {
                    nextFloor = turnaroundFloor;
                }
            } else {
                currentDirection = Direction.IDLE;
                return handleIdleParking(currentState, currentTick);
            }
        }

        int targetFloor = nextFloor.orElse(currentFloor);
        assignRequestsForFloor(targetFloor, currentDirection);

        if (currentDirection == Direction.UP) {
            if (currentStatus == LiftStatus.MOVING_DOWN) {
                return Action.IDLE;
            }
            return currentFloor < targetFloor ? Action.MOVE_UP : Action.IDLE;
        }

        if (currentStatus == LiftStatus.MOVING_UP) {
            return Action.IDLE;
        }
        return currentFloor > targetFloor ? Action.MOVE_DOWN : Action.IDLE;
    }

    /**
     * Takes the lift out of service by cancelling all active requests.
     * This method cancels all non-terminal requests (QUEUED, ASSIGNED, SERVING).
     * After calling this method, the lift should be transitioned to OUT_OF_SERVICE
     * state via the SimulationEngine.
     *
     * The lift will:
     * - Stop at the nearest floor if moving
     * - Open and close doors if at a floor
     * - Cancel all pending requests
     */
    public void takeOutOfService() {
        outOfService = true;
        // Cancel all active (non-terminal) requests.
        Set<LiftRequest> activeRequests = new HashSet<>(getActiveRequests());
        for (LiftRequest request : activeRequests) {
            if (!request.isTerminal()) {
                request.transitionTo(RequestState.CANCELLED);
                archiveRequest(request);
            }
        }

        // Reset idle tracking and parking state.
        resetIdleTracking();
        currentDirection = Direction.IDLE;
    }

    /**
     * Prepares the lift to return to service.
     * After calling this method, the lift should be transitioned from OUT_OF_SERVICE
     * to IDLE state via the SimulationEngine.
     */
    public void returnToService() {
        outOfService = false;
        // Reset idle tracking when returning to service.
        resetIdleTracking();
        currentDirection = Direction.IDLE;
    }

    /**
     * Gets all requests (for testing purposes).
     *
     * @return a copy of all requests
     */
    public Set<LiftRequest> getRequests() {
        return new HashSet<>(activeRequests);
    }

    private void archiveRequest(LiftRequest request) {
        activeRequests.remove(request);
        completedRequests.add(request);
        requestsById.remove(request.getId());
    }

    private void trackRequest(LiftRequest request) {
        activeRequests.add(request);
        requestsById.put(request.getId(), request);
    }
}
