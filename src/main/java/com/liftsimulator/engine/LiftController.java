package com.liftsimulator.engine;

import com.liftsimulator.domain.Action;
import com.liftsimulator.domain.LiftState;

/**
 * Interface for lift controllers that decide what action to take next.
 * The controller is responsible for the lift's behavior logic.
 */
public interface LiftController {
    /**
     * Decides the next action based on the current state and tick.
     *
     * @param currentState the current state of the lift
     * @param currentTick the current simulation tick
     * @return the action to perform in this tick
     */
    Action decideNextAction(LiftState currentState, long currentTick);
}
