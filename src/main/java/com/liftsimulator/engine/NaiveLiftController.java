package com.liftsimulator.engine;

import com.liftsimulator.domain.Action;
import com.liftsimulator.domain.CarCall;
import com.liftsimulator.domain.DoorState;
import com.liftsimulator.domain.HallCall;
import com.liftsimulator.domain.LiftRequest;
import com.liftsimulator.domain.LiftState;
import com.liftsimulator.domain.LiftStatus;
import com.liftsimulator.domain.RequestState;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
public final class NaiveLiftController implements LiftController {
    private static final int DEFAULT_HOME_FLOOR = 0;
    private static final int DEFAULT_IDLE_TIMEOUT_TICKS = 5;

    private final Map<Long, LiftRequest> requestsById = new HashMap<>();
    private final Set<LiftRequest> activeRequests = new HashSet<>();
    private final Set<LiftRequest> completedRequests = new HashSet<>();
    private final int homeFloor;
    private final int idleTimeoutTicks;
    private Long idleStartTick;
    private boolean parkingInProgress;
    private boolean outOfService;

    public NaiveLiftController() {
        this(DEFAULT_HOME_FLOOR, DEFAULT_IDLE_TIMEOUT_TICKS);
    }

    public NaiveLiftController(int homeFloor, int idleTimeoutTicks) {
        if (idleTimeoutTicks < 0) {
            throw new IllegalArgumentException("idleTimeoutTicks must be >= 0");
        }
        this.homeFloor = homeFloor;
        this.idleTimeoutTicks = idleTimeoutTicks;
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
    private void completeRequestsForFloor(int floor) {
        Set<LiftRequest> requestsToComplete = activeRequests.stream()
                .filter(request -> !request.isTerminal() && request.getTargetFloor() == floor)
                .collect(Collectors.toSet());

        for (LiftRequest request : requestsToComplete) {
            if (request.getState() == RequestState.SERVING) {
                request.transitionTo(RequestState.COMPLETED);
            } else if (request.getState() == RequestState.ASSIGNED) {
                request.transitionTo(RequestState.SERVING);
                request.transitionTo(RequestState.COMPLETED);
            } else if (request.getState() == RequestState.QUEUED) {
                request.transitionTo(RequestState.ASSIGNED);
                request.transitionTo(RequestState.SERVING);
                request.transitionTo(RequestState.COMPLETED);
            }
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
        return activeRequests.stream()
                .filter(request -> !request.isTerminal())
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
            if (parkingInProgress) {
                if (currentFloor == homeFloor) {
                    parkingInProgress = false;
                    idleStartTick = currentTick;
                    return Action.IDLE;
                }
                return moveTowardHome(currentFloor);
            }

            if (shouldTrackIdle(currentState)) {
                if (idleStartTick == null) {
                    idleStartTick = currentTick;
                }
                long idleTicks = currentTick - idleStartTick;
                if (idleTicks >= idleTimeoutTicks && currentFloor != homeFloor) {
                    parkingInProgress = true;
                    return moveTowardHome(currentFloor);
                }
            } else {
                idleStartTick = null;
            }
        }

        // No requests, stay idle.
        return Action.IDLE;
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
