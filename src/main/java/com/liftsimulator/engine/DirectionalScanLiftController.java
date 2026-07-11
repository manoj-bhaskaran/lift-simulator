package com.liftsimulator.engine;

import com.liftsimulator.domain.Action;
import com.liftsimulator.domain.Direction;
import com.liftsimulator.domain.DoorState;
import com.liftsimulator.domain.IdleParkingMode;
import com.liftsimulator.domain.LiftRequest;
import com.liftsimulator.domain.LiftState;
import com.liftsimulator.domain.LiftStatus;
import com.liftsimulator.domain.RequestState;
import com.liftsimulator.domain.RequestType;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implements a directional scan controller.
 * The controller maintains travel direction and services all requests in that direction
 * before reversing (or idling if none).
 */
public final class DirectionalScanLiftController extends AbstractRequestManagingLiftController {
    private Direction currentDirection = Direction.IDLE;

    public DirectionalScanLiftController() {
        this(DEFAULT_HOME_FLOOR, DEFAULT_IDLE_TIMEOUT_TICKS, DEFAULT_IDLE_PARKING_MODE);
    }

    public DirectionalScanLiftController(int homeFloor, int idleTimeoutTicks) {
        this(homeFloor, idleTimeoutTicks, DEFAULT_IDLE_PARKING_MODE);
    }

    public DirectionalScanLiftController(int homeFloor, int idleTimeoutTicks, IdleParkingMode idleParkingMode) {
        super(homeFloor, idleTimeoutTicks, idleParkingMode);
    }

    /**
     * Completes all requests for the given floor.
     * This is called when the lift arrives at a floor and opens doors.
     * Transitions requests from SERVING to COMPLETED.
     *
     * @param floor the floor to complete requests for
     */
    private void completeRequestsForFloor(int floor, Direction direction) {
        Set<LiftRequest> requestsToComplete = getActiveRequests().stream()
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
        return getActiveRequests().stream()
                .filter(request -> !request.isTerminal())
                .filter(request -> isEligibleForDirection(request, direction))
                .anyMatch(request -> request.getTargetFloor() == floor);
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
        // Phase 1: prefer the nearest floor whose request is eligible for the direction
        // required to reach it (immediately serviceable on arrival).
        Comparator<Integer> byDistance = (f1, f2) -> {
            int dist1 = Math.abs(f1 - currentFloor);
            int dist2 = Math.abs(f2 - currentFloor);
            if (dist1 != dist2) {
                return Integer.compare(dist1, dist2);
            }
            return Integer.compare(f1, f2);
        };
        Optional<Integer> directlyServiceable = getActiveRequests().stream()
                .filter(request -> {
                    int target = request.getTargetFloor();
                    if (target > currentFloor) {
                        return isEligibleForDirection(request, Direction.UP);
                    } else if (target < currentFloor) {
                        return isEligibleForDirection(request, Direction.DOWN);
                    }
                    return true;
                })
                .map(LiftRequest::getTargetFloor)
                .min(byDistance);
        if (directlyServiceable.isPresent()) {
            return directlyServiceable;
        }
        // Phase 2: no directly serviceable request exists (e.g. only an opposite-direction
        // hall call on the far side). Fall back to nearest regardless of eligibility so
        // the lift still travels toward it and self-corrects on arrival via reversal.
        return getActiveRequests().stream()
                .map(LiftRequest::getTargetFloor)
                .min(byDistance);
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
        if (findNextRequestedFloorInDirection(currentFloor, currentDirection).isPresent()) {
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
        getActiveRequests().stream()
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
        getActiveRequests().stream()
                .filter(request -> request.getTargetFloor() == targetFloor)
                .filter(request -> isEligibleForDirection(request, direction))
                .filter(request -> request.getState() == RequestState.ASSIGNED)
                .forEach(request -> request.transitionTo(RequestState.SERVING));
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

    @Override
    public void takeOutOfService() {
        super.takeOutOfService();
        currentDirection = Direction.IDLE;
    }

    @Override
    public void returnToService() {
        super.returnToService();
        currentDirection = Direction.IDLE;
    }
}
