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

    // Define valid state transitions based on actions.
    private static final Map<LiftStatus, Set<LiftStatus>> VALID_TRANSITIONS = new EnumMap<>(LiftStatus.class);

    static {
        // IDLE can transition to: MOVING_UP, MOVING_DOWN, DOORS_OPENING, OUT_OF_SERVICE.
        VALID_TRANSITIONS.put(LiftStatus.IDLE, EnumSet.of(
                LiftStatus.IDLE,
                LiftStatus.MOVING_UP,
                LiftStatus.MOVING_DOWN,
                LiftStatus.DOORS_OPENING,
                LiftStatus.OUT_OF_SERVICE
        ));

        /*
         * MOVING_UP can transition to: MOVING_UP, IDLE, OUT_OF_SERVICE.
         * Cannot open doors while moving - must stop first.
         */
        VALID_TRANSITIONS.put(LiftStatus.MOVING_UP, EnumSet.of(
                LiftStatus.MOVING_UP,
                LiftStatus.IDLE,
                LiftStatus.OUT_OF_SERVICE
        ));

        /*
         * MOVING_DOWN can transition to: MOVING_DOWN, IDLE, OUT_OF_SERVICE.
         * Cannot open doors while moving - must stop first.
         */
        VALID_TRANSITIONS.put(LiftStatus.MOVING_DOWN, EnumSet.of(
                LiftStatus.MOVING_DOWN,
                LiftStatus.IDLE,
                LiftStatus.OUT_OF_SERVICE
        ));

        /*
         * DOORS_OPENING can transition to: DOORS_OPEN, DOORS_CLOSING (abort), OUT_OF_SERVICE.
         * Cannot go directly to IDLE - must close doors first.
         */
        VALID_TRANSITIONS.put(LiftStatus.DOORS_OPENING, EnumSet.of(
                LiftStatus.DOORS_OPENING,
                LiftStatus.DOORS_OPEN,
                LiftStatus.DOORS_CLOSING,
                LiftStatus.OUT_OF_SERVICE
        ));

        // DOORS_OPEN can transition to: DOORS_CLOSING, OUT_OF_SERVICE.
        VALID_TRANSITIONS.put(LiftStatus.DOORS_OPEN, EnumSet.of(
                LiftStatus.DOORS_OPEN,
                LiftStatus.DOORS_CLOSING,
                LiftStatus.OUT_OF_SERVICE
        ));

        // DOORS_CLOSING can transition to: IDLE, DOORS_OPENING (re-open), OUT_OF_SERVICE.
        VALID_TRANSITIONS.put(LiftStatus.DOORS_CLOSING, EnumSet.of(
                LiftStatus.DOORS_CLOSING,
                LiftStatus.IDLE,
                LiftStatus.DOORS_OPENING,
                LiftStatus.OUT_OF_SERVICE
        ));

        // OUT_OF_SERVICE can transition to: IDLE (for maintenance recovery).
        VALID_TRANSITIONS.put(LiftStatus.OUT_OF_SERVICE, EnumSet.of(
                LiftStatus.OUT_OF_SERVICE,
                LiftStatus.IDLE
        ));
    }

    /**
     * Determines the next lift status based on the current status and requested action.
     *
     * @param currentStatus the current lift status
     * @param action the action being performed
     * @return the next lift status after applying the action
     * @see LiftStatus for the available lift states
     * @see Action for the set of actions that drive transitions
     */
    public static LiftStatus getNextStatus(LiftStatus currentStatus, Action action) {
        return switch (action) {
            case MOVE_UP -> LiftStatus.MOVING_UP;
            case MOVE_DOWN -> LiftStatus.MOVING_DOWN;
            case OPEN_DOOR -> LiftStatus.DOORS_OPENING;
            case CLOSE_DOOR -> LiftStatus.DOORS_CLOSING;
            case IDLE -> {
                // When IDLE action is taken, transition based on current state.
                if (currentStatus == LiftStatus.DOORS_CLOSING) {
                    yield LiftStatus.IDLE;
                } else if (currentStatus == LiftStatus.DOORS_OPENING) {
                    yield LiftStatus.DOORS_OPEN;
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
     * Validates whether a transition between lift statuses is allowed.
     *
     * @param fromStatus the current status
     * @param toStatus the target status
     * @return true if the transition is valid, false otherwise
     * @see LiftStatus for the complete lift state machine
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
     * Validates whether an action is allowed in the current status.
     *
     * @param currentStatus the current lift status
     * @param action the action to validate
     * @return true if the action is allowed, false otherwise
     * @see Action for the action set validated by this method
     */
    public static boolean isActionAllowed(LiftStatus currentStatus, Action action) {
        LiftStatus nextStatus = getNextStatus(currentStatus, action);
        return isValidTransition(currentStatus, nextStatus);
    }

    /**
     * Gets the set of valid next states from the current status.
     *
     * @param currentStatus the current status
     * @return set of valid next states
     * @see LiftStatus for the defined states
     */
    public static Set<LiftStatus> getValidNextStates(LiftStatus currentStatus) {
        return VALID_TRANSITIONS.getOrDefault(currentStatus, EnumSet.noneOf(LiftStatus.class));
    }
}
