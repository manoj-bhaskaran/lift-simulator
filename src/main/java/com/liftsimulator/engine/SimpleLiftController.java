package com.liftsimulator.engine;

import com.liftsimulator.domain.Action;
import com.liftsimulator.domain.DoorState;
import com.liftsimulator.domain.LiftState;

/**
 * A simple demonstration lift controller that follows a predefined pattern.
 * This controller demonstrates basic lift operations in a predictable sequence.
 */
public class SimpleLiftController implements LiftController {
    private int targetFloor = 3;
    private boolean movingUp = true;

    @Override
    public Action decideNextAction(LiftState currentState, long currentTick) {
        int currentFloor = currentState.getFloor();
        DoorState doorState = currentState.getDoorState();

        // If doors are open, let the engine manage the dwell/close cycle
        if (doorState == DoorState.OPEN) {
            return Action.IDLE;
        }

        // If we've reached the target floor, open doors
        if (currentFloor == targetFloor) {
            // Switch direction and target
            if (movingUp) {
                movingUp = false;
                targetFloor = 0;
            } else {
                movingUp = true;
                targetFloor = 3;
            }
            return Action.OPEN_DOOR;
        }

        // Move towards target floor
        if (currentFloor < targetFloor) {
            return Action.MOVE_UP;
        } else if (currentFloor > targetFloor) {
            return Action.MOVE_DOWN;
        }

        return Action.IDLE;
    }
}
