package com.liftsimulator.engine;

import com.liftsimulator.domain.Action;
import com.liftsimulator.domain.DoorState;
import com.liftsimulator.domain.IdleParkingMode;
import com.liftsimulator.domain.LiftRequest;
import com.liftsimulator.domain.LiftState;
import com.liftsimulator.domain.LiftStatus;
import com.liftsimulator.domain.RequestState;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A naive lift controller that implements basic scheduling logic.
 * This controller manages lift requests as first-class entities with
 * explicit lifecycle states, servicing requests by moving to the nearest requested floor.
 *
 * Decision rules:
 * - When the lift reaches a requested floor, open the doors
 * - When doors are open and dwell time has elapsed, close the doors
 * - Otherwise, move toward the nearest floor with a pending request
 *
 * Request lifecycle:
 * - CREATED: Request is created
 * - QUEUED: Request is added to the controller's queue
 * - ASSIGNED: Request is assigned to the lift for service
 * - SERVING: Lift is actively serving the request
 * - COMPLETED: Request has been successfully fulfilled
 * - CANCELLED: Request was cancelled before completion
 *
 * Optimization is out of scope; this focuses on correctness first.
 */
public final class NaiveLiftController extends AbstractRequestManagingLiftController {

    public NaiveLiftController() {
        this(DEFAULT_HOME_FLOOR, DEFAULT_IDLE_TIMEOUT_TICKS, DEFAULT_IDLE_PARKING_MODE);
    }

    public NaiveLiftController(int homeFloor, int idleTimeoutTicks) {
        this(homeFloor, idleTimeoutTicks, DEFAULT_IDLE_PARKING_MODE);
    }

    public NaiveLiftController(int homeFloor, int idleTimeoutTicks, IdleParkingMode idleParkingMode) {
        super(homeFloor, idleTimeoutTicks, idleParkingMode);
    }

    /**
     * Completes all requests for the given floor.
     * This is called when the lift arrives at a floor and opens doors.
     * Transitions requests from SERVING to COMPLETED.
     *
     * @param floor the floor to complete requests for
     */
    private void completeRequestsForFloor(int floor) {
        Set<LiftRequest> requestsToComplete = getActiveRequests().stream()
                .filter(request -> !request.isTerminal() && request.getTargetFloor() == floor)
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
    private boolean hasRequestForFloor(int floor) {
        return getActiveRequests().stream()
                .filter(request -> !request.isTerminal())
                .anyMatch(request -> request.getTargetFloor() == floor);
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

    /**
     * Assigns requests for the target floor to the lift.
     * Transitions requests from QUEUED to ASSIGNED.
     *
     * @param targetFloor the floor being targeted
     */
    private void assignRequestsForFloor(int targetFloor) {
        getActiveRequests().stream()
                .filter(request -> request.getTargetFloor() == targetFloor)
                .filter(request -> request.getState() == RequestState.QUEUED)
                .forEach(request -> request.transitionTo(RequestState.ASSIGNED));
    }

    /**
     * Marks assigned requests as being served.
     * Transitions requests from ASSIGNED to SERVING.
     *
     * @param targetFloor the floor being served
     */
    private void serveRequestsForFloor(int targetFloor) {
        getActiveRequests().stream()
                .filter(request -> request.getTargetFloor() == targetFloor)
                .filter(request -> request.getState() == RequestState.ASSIGNED)
                .forEach(request -> request.transitionTo(RequestState.SERVING));
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
            if (hasRequestForFloor(currentFloor)) {
                serveRequestsForFloor(currentFloor);
                completeRequestsForFloor(currentFloor);
            }
            return Action.IDLE;
        }

        if (currentStatus == LiftStatus.DOORS_CLOSING) {
            // Check if a new request arrived for current floor while doors closing.
            if (hasRequestForFloor(currentFloor)) {
                // Attempt to reopen doors (will succeed only if within reopen window).
                return Action.OPEN_DOOR;
            }
            return Action.IDLE;
        }

        // If doors are open, let the engine manage the dwell/close cycle.
        if (doorState == DoorState.OPEN) {
            if (hasRequestForFloor(currentFloor)) {
                completeRequestsForFloor(currentFloor);
            }
            return Action.IDLE;
        }

        // If at a requested floor, stop first if moving, then open doors.
        if (hasRequestForFloor(currentFloor)) {
            if (currentStatus == LiftStatus.MOVING_UP || currentStatus == LiftStatus.MOVING_DOWN) {
                assignRequestsForFloor(currentFloor);
                serveRequestsForFloor(currentFloor);
                return Action.IDLE;
            }
            if (currentStatus == LiftStatus.DOORS_OPENING) {
                completeRequestsForFloor(currentFloor);
                return Action.IDLE;
            }
            assignRequestsForFloor(currentFloor);
            serveRequestsForFloor(currentFloor);
            return Action.OPEN_DOOR;
        }

        // Find nearest requested floor and move towards it.
        Optional<Integer> nearestFloor = findNearestRequestedFloor(currentFloor);
        if (nearestFloor.isPresent()) {
            int targetFloor = nearestFloor.get();
            assignRequestsForFloor(targetFloor);
            if (currentStatus == LiftStatus.MOVING_UP && targetFloor < currentFloor) {
                return Action.IDLE;
            }
            if (currentStatus == LiftStatus.MOVING_DOWN && targetFloor > currentFloor) {
                return Action.IDLE;
            }
            if (currentFloor < targetFloor) {
                return Action.MOVE_UP;
            } else if (currentFloor > targetFloor) {
                return Action.MOVE_DOWN;
            }
        }

        if (!hasActiveRequests) {
            return handleIdleParking(currentState, currentTick);
        }

        // No requests, stay idle.
        return Action.IDLE;
    }
}
