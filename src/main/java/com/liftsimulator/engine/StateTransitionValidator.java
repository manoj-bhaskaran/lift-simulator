package com.liftsimulator.engine;

import com.liftsimulator.domain.Action;
import com.liftsimulator.domain.LiftStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Validates and manages state transitions for the lift state machine.
 * Ensures that only valid transitions occur and logs any invalid attempts.
 */
public class StateTransitionValidator {
    private static final Logger LOGGER = Logger.getLogger(StateTransitionValidator.class.getName());

    // Define valid state transitions based on actions
    private static final Map<LiftStatus, Set<LiftStatus>> VALID_TRANSITIONS = new EnumMap<>(LiftStatus.class);

    static {
        // IDLE can transition to: MOVING_UP, MOVING_DOWN, DOORS_OPEN, OUT_OF_SERVICE
        VALID_TRANSITIONS.put(LiftStatus.IDLE, EnumSet.of(
                LiftStatus.IDLE,
                LiftStatus.MOVING_UP,
                LiftStatus.MOVING_DOWN,
                LiftStatus.DOORS_OPEN,
                LiftStatus.OUT_OF_SERVICE
        ));

        // MOVING_UP can transition to: MOVING_UP, IDLE, DOORS_OPEN, OUT_OF_SERVICE
        VALID_TRANSITIONS.put(LiftStatus.MOVING_UP, EnumSet.of(
                LiftStatus.MOVING_UP,
                LiftStatus.IDLE,
                LiftStatus.DOORS_OPEN,
                LiftStatus.OUT_OF_SERVICE
        ));

        // MOVING_DOWN can transition to: MOVING_DOWN, IDLE, DOORS_OPEN, OUT_OF_SERVICE
        VALID_TRANSITIONS.put(LiftStatus.MOVING_DOWN, EnumSet.of(
                LiftStatus.MOVING_DOWN,
                LiftStatus.IDLE,
                LiftStatus.DOORS_OPEN,
                LiftStatus.OUT_OF_SERVICE
        ));

        // DOORS_OPEN can transition to: IDLE (instant close), DOORS_CLOSING, OUT_OF_SERVICE
        VALID_TRANSITIONS.put(LiftStatus.DOORS_OPEN, EnumSet.of(
                LiftStatus.DOORS_OPEN,
                LiftStatus.IDLE,
                LiftStatus.DOORS_CLOSING,
                LiftStatus.OUT_OF_SERVICE
        ));

        // DOORS_CLOSING can transition to: IDLE, DOORS_OPEN, OUT_OF_SERVICE
        VALID_TRANSITIONS.put(LiftStatus.DOORS_CLOSING, EnumSet.of(
                LiftStatus.DOORS_CLOSING,
                LiftStatus.IDLE,
                LiftStatus.DOORS_OPEN,
                LiftStatus.OUT_OF_SERVICE
        ));

        // OUT_OF_SERVICE can transition to: IDLE (for maintenance recovery)
        VALID_TRANSITIONS.put(LiftStatus.OUT_OF_SERVICE, EnumSet.of(
                LiftStatus.OUT_OF_SERVICE,
                LiftStatus.IDLE
        ));
    }

    /**
     * Determines the next status based on current status and action.
     *
     * @param currentStatus The current lift status
     * @param action The action being performed
     * @return The next lift status after applying the action
     */
    public static LiftStatus getNextStatus(LiftStatus currentStatus, Action action) {
        return switch (action) {
            case MOVE_UP -> LiftStatus.MOVING_UP;
            case MOVE_DOWN -> LiftStatus.MOVING_DOWN;
            case OPEN_DOOR -> LiftStatus.DOORS_OPEN;
            case CLOSE_DOOR -> LiftStatus.DOORS_CLOSING;
            case IDLE -> {
                // When IDLE action is taken, transition based on current state
                if (currentStatus == LiftStatus.DOORS_CLOSING) {
                    yield LiftStatus.IDLE;
                } else if (currentStatus == LiftStatus.MOVING_UP ||
                           currentStatus == LiftStatus.MOVING_DOWN) {
                    yield LiftStatus.IDLE;
                } else {
                    yield currentStatus;
                }
            }
        };
    }

    /**
     * Validates if a transition from one status to another is valid.
     *
     * @param fromStatus The current status
     * @param toStatus The target status
     * @return true if the transition is valid, false otherwise
     */
    public static boolean isValidTransition(LiftStatus fromStatus, LiftStatus toStatus) {
        Set<LiftStatus> validNextStates = VALID_TRANSITIONS.get(fromStatus);
        if (validNextStates == null) {
            LOGGER.warning("No valid transitions defined for status: " + fromStatus);
            return false;
        }

        boolean isValid = validNextStates.contains(toStatus);
        if (!isValid) {
            LOGGER.warning(String.format(
                "Invalid state transition attempted: %s -> %s",
                fromStatus,
                toStatus
            ));
        }

        return isValid;
    }

    /**
     * Validates if an action is allowed in the current status.
     *
     * @param currentStatus The current lift status
     * @param action The action to validate
     * @return true if the action is allowed, false otherwise
     */
    public static boolean isActionAllowed(LiftStatus currentStatus, Action action) {
        LiftStatus nextStatus = getNextStatus(currentStatus, action);
        return isValidTransition(currentStatus, nextStatus);
    }

    /**
     * Gets the set of valid next states from the current status.
     *
     * @param currentStatus The current status
     * @return Set of valid next states
     */
    public static Set<LiftStatus> getValidNextStates(LiftStatus currentStatus) {
        return VALID_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(LiftStatus.class));
    }
}
