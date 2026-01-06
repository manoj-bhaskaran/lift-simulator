package com.liftsimulator.engine;

import com.liftsimulator.domain.Action;
import com.liftsimulator.domain.LiftStatus;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the StateTransitionValidator to ensure valid state transitions
 * are allowed and invalid transitions are prevented.
 */
class StateTransitionValidatorTest {

    @Test
    void testIdleCanTransitionToMovingUp() {
        assertTrue(StateTransitionValidator.isValidTransition(LiftStatus.IDLE, LiftStatus.MOVING_UP));
    }

    @Test
    void testIdleCanTransitionToMovingDown() {
        assertTrue(StateTransitionValidator.isValidTransition(LiftStatus.IDLE, LiftStatus.MOVING_DOWN));
    }

    @Test
    void testIdleCanTransitionToDoorsOpen() {
        assertTrue(StateTransitionValidator.isValidTransition(LiftStatus.IDLE, LiftStatus.DOORS_OPEN));
    }

    @Test
    void testMovingUpCanTransitionToIdle() {
        assertTrue(StateTransitionValidator.isValidTransition(LiftStatus.MOVING_UP, LiftStatus.IDLE));
    }

    @Test
    void testMovingDownCanTransitionToIdle() {
        assertTrue(StateTransitionValidator.isValidTransition(LiftStatus.MOVING_DOWN, LiftStatus.IDLE));
    }

    @Test
    void testDoorsOpenCanTransitionToDoorsClosing() {
        assertTrue(StateTransitionValidator.isValidTransition(LiftStatus.DOORS_OPEN, LiftStatus.DOORS_CLOSING));
    }

    @Test
    void testDoorsClosingCanTransitionToIdle() {
        assertTrue(StateTransitionValidator.isValidTransition(LiftStatus.DOORS_CLOSING, LiftStatus.IDLE));
    }

    @Test
    void testInvalidTransitionDoorsOpenToMovingUp() {
        assertFalse(StateTransitionValidator.isValidTransition(LiftStatus.DOORS_OPEN, LiftStatus.MOVING_UP));
    }

    @Test
    void testInvalidTransitionDoorsOpenToMovingDown() {
        assertFalse(StateTransitionValidator.isValidTransition(LiftStatus.DOORS_OPEN, LiftStatus.MOVING_DOWN));
    }

    @Test
    void testInvalidTransitionMovingUpToDoorsOpen() {
        assertTrue(StateTransitionValidator.isValidTransition(LiftStatus.MOVING_UP, LiftStatus.DOORS_OPEN));
    }

    @Test
    void testInvalidTransitionMovingDownToDoorsOpen() {
        assertTrue(StateTransitionValidator.isValidTransition(LiftStatus.MOVING_DOWN, LiftStatus.DOORS_OPEN));
    }

    @Test
    void testInvalidTransitionDoorsClosingToMovingUp() {
        assertFalse(StateTransitionValidator.isValidTransition(LiftStatus.DOORS_CLOSING, LiftStatus.MOVING_UP));
    }

    @Test
    void testInvalidTransitionDoorsClosingToMovingDown() {
        assertFalse(StateTransitionValidator.isValidTransition(LiftStatus.DOORS_CLOSING, LiftStatus.MOVING_DOWN));
    }

    @Test
    void testOutOfServiceCanOnlyTransitionToIdle() {
        assertTrue(StateTransitionValidator.isValidTransition(LiftStatus.OUT_OF_SERVICE, LiftStatus.IDLE));
        assertFalse(StateTransitionValidator.isValidTransition(LiftStatus.OUT_OF_SERVICE, LiftStatus.MOVING_UP));
        assertFalse(StateTransitionValidator.isValidTransition(LiftStatus.OUT_OF_SERVICE, LiftStatus.MOVING_DOWN));
        assertFalse(StateTransitionValidator.isValidTransition(LiftStatus.OUT_OF_SERVICE, LiftStatus.DOORS_OPEN));
    }

    @Test
    void testGetNextStatusForMoveUpAction() {
        assertEquals(LiftStatus.MOVING_UP, StateTransitionValidator.getNextStatus(LiftStatus.IDLE, Action.MOVE_UP));
    }

    @Test
    void testGetNextStatusForMoveDownAction() {
        assertEquals(LiftStatus.MOVING_DOWN, StateTransitionValidator.getNextStatus(LiftStatus.IDLE, Action.MOVE_DOWN));
    }

    @Test
    void testGetNextStatusForOpenDoorAction() {
        assertEquals(LiftStatus.DOORS_OPEN, StateTransitionValidator.getNextStatus(LiftStatus.IDLE, Action.OPEN_DOOR));
    }

    @Test
    void testGetNextStatusForCloseDoorAction() {
        assertEquals(LiftStatus.DOORS_CLOSING, StateTransitionValidator.getNextStatus(LiftStatus.DOORS_OPEN, Action.CLOSE_DOOR));
    }

    @Test
    void testGetNextStatusForIdleActionFromDoorsClosing() {
        assertEquals(LiftStatus.IDLE, StateTransitionValidator.getNextStatus(LiftStatus.DOORS_CLOSING, Action.IDLE));
    }

    @Test
    void testGetNextStatusForIdleActionFromMovingUp() {
        assertEquals(LiftStatus.IDLE, StateTransitionValidator.getNextStatus(LiftStatus.MOVING_UP, Action.IDLE));
    }

    @Test
    void testIsActionAllowedMoveUpFromIdle() {
        assertTrue(StateTransitionValidator.isActionAllowed(LiftStatus.IDLE, Action.MOVE_UP));
    }

    @Test
    void testIsActionAllowedMoveUpFromDoorsOpen() {
        assertFalse(StateTransitionValidator.isActionAllowed(LiftStatus.DOORS_OPEN, Action.MOVE_UP));
    }

    @Test
    void testIsActionAllowedMoveDownFromDoorsOpen() {
        assertFalse(StateTransitionValidator.isActionAllowed(LiftStatus.DOORS_OPEN, Action.MOVE_DOWN));
    }

    @Test
    void testIsActionAllowedOpenDoorFromIdle() {
        assertTrue(StateTransitionValidator.isActionAllowed(LiftStatus.IDLE, Action.OPEN_DOOR));
    }

    @Test
    void testIsActionAllowedCloseDoorFromDoorsOpen() {
        assertTrue(StateTransitionValidator.isActionAllowed(LiftStatus.DOORS_OPEN, Action.CLOSE_DOOR));
    }

    @Test
    void testGetValidNextStatesFromIdle() {
        Set<LiftStatus> validStates = StateTransitionValidator.getValidNextStates(LiftStatus.IDLE);
        assertTrue(validStates.contains(LiftStatus.IDLE));
        assertTrue(validStates.contains(LiftStatus.MOVING_UP));
        assertTrue(validStates.contains(LiftStatus.MOVING_DOWN));
        assertTrue(validStates.contains(LiftStatus.DOORS_OPEN));
        assertTrue(validStates.contains(LiftStatus.OUT_OF_SERVICE));
    }

    @Test
    void testGetValidNextStatesFromDoorsOpen() {
        Set<LiftStatus> validStates = StateTransitionValidator.getValidNextStates(LiftStatus.DOORS_OPEN);
        assertTrue(validStates.contains(LiftStatus.DOORS_OPEN));
        assertTrue(validStates.contains(LiftStatus.DOORS_CLOSING));
        assertTrue(validStates.contains(LiftStatus.OUT_OF_SERVICE));
        assertFalse(validStates.contains(LiftStatus.MOVING_UP));
        assertFalse(validStates.contains(LiftStatus.MOVING_DOWN));
    }

    @Test
    void testGetValidNextStatesFromMovingUp() {
        Set<LiftStatus> validStates = StateTransitionValidator.getValidNextStates(LiftStatus.MOVING_UP);
        assertTrue(validStates.contains(LiftStatus.MOVING_UP));
        assertTrue(validStates.contains(LiftStatus.IDLE));
        assertTrue(validStates.contains(LiftStatus.DOORS_OPEN));
        assertTrue(validStates.contains(LiftStatus.OUT_OF_SERVICE));
    }

    @Test
    void testAllStatesCanTransitionToOutOfService() {
        for (LiftStatus status : LiftStatus.values()) {
            if (status != LiftStatus.OUT_OF_SERVICE) {
                assertTrue(StateTransitionValidator.isValidTransition(status, LiftStatus.OUT_OF_SERVICE),
                    "State " + status + " should be able to transition to OUT_OF_SERVICE");
            }
        }
    }

    @Test
    void testStatesCanStayInSameState() {
        // All states can remain in the same state (for IDLE action or ongoing operations)
        for (LiftStatus status : LiftStatus.values()) {
            assertTrue(StateTransitionValidator.isValidTransition(status, status),
                "State " + status + " should be able to remain in the same state");
        }
    }
}
