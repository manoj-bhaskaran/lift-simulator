package com.liftsimulator.engine;

import com.liftsimulator.domain.Action;
import com.liftsimulator.domain.CarCall;
import com.liftsimulator.domain.DoorState;
import com.liftsimulator.domain.HallCall;
import com.liftsimulator.domain.IdleParkingMode;
import com.liftsimulator.domain.LiftRequest;
import com.liftsimulator.domain.LiftState;
import com.liftsimulator.domain.LiftStatus;
import com.liftsimulator.domain.RequestState;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Base class shared by {@link RequestManagingLiftController} implementations.
 * Centralizes request tracking (car calls, hall calls, cancellation, archiving),
 * idle/home-parking bookkeeping, and the out-of-service lifecycle so that
 * scheduling strategies only need to implement {@link #decideNextAction}.
 */
public abstract class AbstractRequestManagingLiftController implements RequestManagingLiftController {
    protected static final int DEFAULT_HOME_FLOOR = 0;
    protected static final int DEFAULT_IDLE_TIMEOUT_TICKS = 5;
    protected static final IdleParkingMode DEFAULT_IDLE_PARKING_MODE = IdleParkingMode.PARK_TO_HOME_FLOOR;

    private final Map<Long, LiftRequest> requestsById = new HashMap<>();
    private final Set<LiftRequest> activeRequests = new HashSet<>();
    private final Set<LiftRequest> completedRequests = new HashSet<>();
    protected final int homeFloor;
    protected final int idleTimeoutTicks;
    protected final IdleParkingMode idleParkingMode;
    private Long idleStartTick;
    private boolean parkingInProgress;
    protected boolean outOfService;

    @SuppressFBWarnings(
            value = "CT_CONSTRUCTOR_THROW",
            justification = "Subclasses are controller strategies with no finalizers; there is no "
                    + "sensitive state to expose via a finalizer attack.")
    protected AbstractRequestManagingLiftController(int homeFloor, int idleTimeoutTicks, IdleParkingMode idleParkingMode) {
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
    @Override
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
    @Override
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
    @Override
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
    @Override
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
     * Gets all active (non-terminal) requests.
     *
     * @return set of active requests
     */
    protected Set<LiftRequest> getActiveRequests() {
        return Collections.unmodifiableSet(activeRequests);
    }

    protected void resetIdleTracking() {
        idleStartTick = null;
        parkingInProgress = false;
    }

    protected boolean shouldTrackIdle(LiftState currentState) {
        return currentState.getStatus() == LiftStatus.IDLE && currentState.getDoorState() == DoorState.CLOSED;
    }

    protected Action moveTowardHome(int currentFloor) {
        if (currentFloor < homeFloor) {
            return Action.MOVE_UP;
        }
        if (currentFloor > homeFloor) {
            return Action.MOVE_DOWN;
        }
        return Action.IDLE;
    }

    /**
     * Handles idle-time parking: once the lift has been idle at a non-home floor for
     * {@link #idleTimeoutTicks} ticks, moves it back toward {@link #homeFloor}.
     *
     * @param currentState the current lift state
     * @param currentTick the current simulation tick
     * @return the action to take while idle/parking
     */
    protected Action handleIdleParking(LiftState currentState, long currentTick) {
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
    @Override
    public void takeOutOfService() {
        outOfService = true;
        // Cancel all active (non-terminal) requests.
        Set<LiftRequest> requestsToCancel = new HashSet<>(getActiveRequests());
        for (LiftRequest request : requestsToCancel) {
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
    @Override
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
    @Override
    public Set<LiftRequest> getRequests() {
        return new HashSet<>(activeRequests);
    }

    protected void archiveRequest(LiftRequest request) {
        activeRequests.remove(request);
        completedRequests.add(request);
        requestsById.remove(request.getId());
    }

    protected void trackRequest(LiftRequest request) {
        activeRequests.add(request);
        requestsById.put(request.getId(), request);
    }
}
