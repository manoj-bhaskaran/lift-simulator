package com.liftsimulator.engine;

import com.liftsimulator.domain.Action;
import com.liftsimulator.domain.CarCall;
import com.liftsimulator.domain.DoorState;
import com.liftsimulator.domain.HallCall;
import com.liftsimulator.domain.LiftState;
import com.liftsimulator.domain.LiftStatus;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * A naive lift controller that implements basic scheduling logic.
 * This controller manages car calls and hall calls, servicing requests
 * by moving to the nearest requested floor.
 *
 * Decision rules:
 * - When the lift reaches a requested floor, open the doors
 * - When doors are open and dwell time has elapsed, close the doors
 * - Otherwise, move toward the nearest floor with a pending request
 *
 * Optimization is out of scope; this focuses on correctness first.
 */
public class NaiveLiftController implements LiftController {
    private static final int DWELL_TIME_TICKS = 3;

    private final Set<CarCall> carCalls = new HashSet<>();
    private final Set<HallCall> hallCalls = new HashSet<>();
    private Long doorOpenedAt = null;

    /**
     * Adds a car call (destination request from inside the lift).
     *
     * @param carCall the car call to add
     */
    public void addCarCall(CarCall carCall) {
        carCalls.add(carCall);
    }

    /**
     * Adds a hall call (request from a floor to go up or down).
     *
     * @param hallCall the hall call to add
     */
    public void addHallCall(HallCall hallCall) {
        hallCalls.add(hallCall);
    }

    /**
     * Removes all requests for the given floor.
     * This is called when the lift arrives at a floor and opens doors.
     *
     * @param floor the floor to clear requests for
     */
    private void clearRequestsForFloor(int floor) {
        carCalls.removeIf(call -> call.destinationFloor() == floor);
        hallCalls.removeIf(call -> call.floor() == floor);
    }

    /**
     * Checks if there is any request for the given floor.
     *
     * @param floor the floor to check
     * @return true if there is a car call or hall call for this floor
     */
    private boolean hasRequestForFloor(int floor) {
        return carCalls.stream().anyMatch(call -> call.destinationFloor() == floor)
                || hallCalls.stream().anyMatch(call -> call.floor() == floor);
    }

    /**
     * Finds the nearest floor with a pending request.
     *
     * @param currentFloor the current floor of the lift
     * @return the nearest requested floor, or empty if no requests
     */
    private Optional<Integer> findNearestRequestedFloor(int currentFloor) {
        Set<Integer> requestedFloors = new TreeSet<>();

        // Collect all requested floors from car calls
        carCalls.forEach(call -> requestedFloors.add(call.destinationFloor()));

        // Collect all requested floors from hall calls
        hallCalls.forEach(call -> requestedFloors.add(call.floor()));

        // Find the nearest one
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

    @Override
    public Action decideNextAction(LiftState currentState, long currentTick) {
        int currentFloor = currentState.getFloor();
        DoorState doorState = currentState.getDoorState();
        LiftStatus currentStatus = currentState.getStatus();

        // If doors are open, manage dwell time
        if (doorState == DoorState.OPEN) {
            if (hasRequestForFloor(currentFloor)) {
                clearRequestsForFloor(currentFloor);
            }
            if (doorOpenedAt == null) {
                doorOpenedAt = currentTick;
            }

            long ticksOpen = currentTick - doorOpenedAt + 1;
            if (ticksOpen >= DWELL_TIME_TICKS) {
                doorOpenedAt = null;
                return Action.CLOSE_DOOR;
            }

            return Action.IDLE;
        }

        // If at a requested floor, stop first if moving, then open doors
        if (hasRequestForFloor(currentFloor)) {
            if (currentStatus == LiftStatus.MOVING_UP || currentStatus == LiftStatus.MOVING_DOWN) {
                return Action.IDLE;
            }
            if (currentStatus == LiftStatus.DOORS_OPENING) {
                clearRequestsForFloor(currentFloor);
                return Action.IDLE;
            }
            clearRequestsForFloor(currentFloor);
            return Action.OPEN_DOOR;
        }

        // Find nearest requested floor and move towards it
        Optional<Integer> nearestFloor = findNearestRequestedFloor(currentFloor);
        if (nearestFloor.isPresent()) {
            int targetFloor = nearestFloor.get();
            if (currentFloor < targetFloor) {
                return Action.MOVE_UP;
            } else if (currentFloor > targetFloor) {
                return Action.MOVE_DOWN;
            }
        }

        // No requests, stay idle
        return Action.IDLE;
    }
}
