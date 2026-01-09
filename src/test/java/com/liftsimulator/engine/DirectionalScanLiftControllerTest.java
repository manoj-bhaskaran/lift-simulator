package com.liftsimulator.engine;

import com.liftsimulator.domain.Action;
import com.liftsimulator.domain.CarCall;
import com.liftsimulator.domain.Direction;
import com.liftsimulator.domain.HallCall;
import com.liftsimulator.domain.LiftState;
import com.liftsimulator.domain.LiftStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DirectionalScanLiftControllerTest {
    private DirectionalScanLiftController controller;

    @BeforeEach
    public void setUp() {
        controller = new DirectionalScanLiftController();
    }

    @Test
    public void testSelectsDirectionFromIdleUsingNearestRequest() {
        controller.addCarCall(new CarCall(6));
        controller.addCarCall(new CarCall(1));

        LiftState state = new LiftState(3, LiftStatus.IDLE);

        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.MOVE_DOWN, action);
    }

    @Test
    public void testContinuesInSameDirectionWhileRequestsRemainAhead() {
        controller.addCarCall(new CarCall(5));
        controller.addCarCall(new CarCall(0));

        LiftState state = new LiftState(3, LiftStatus.IDLE);
        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.MOVE_UP, action);

        LiftState movingState = new LiftState(4, LiftStatus.MOVING_UP);
        Action nextAction = controller.decideNextAction(movingState, 1);
        assertEquals(Action.MOVE_UP, nextAction);
    }

    @Test
    public void testReversesWhenNoRequestsRemainInCurrentDirection() {
        controller.addCarCall(new CarCall(5));
        controller.addCarCall(new CarCall(1));

        LiftState state = new LiftState(4, LiftStatus.IDLE);
        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.MOVE_UP, action);

        LiftState arrivingState = new LiftState(5, LiftStatus.MOVING_UP);
        Action stopAction = controller.decideNextAction(arrivingState, 1);
        assertEquals(Action.IDLE, stopAction);

        LiftState idleAtTop = new LiftState(5, LiftStatus.IDLE);
        Action openAction = controller.decideNextAction(idleAtTop, 2);
        assertEquals(Action.OPEN_DOOR, openAction);

        LiftState doorsOpen = new LiftState(5, LiftStatus.DOORS_OPEN);
        Action dwellAction = controller.decideNextAction(doorsOpen, 3);
        assertEquals(Action.IDLE, dwellAction);

        LiftState afterService = new LiftState(5, LiftStatus.IDLE);
        Action reverseAction = controller.decideNextAction(afterService, 4);
        assertEquals(Action.MOVE_DOWN, reverseAction);
    }

    @Test
    public void testInsertsUpHallCallBeforeCarCallWhileTravelingUp() {
        controller.addHallCall(new HallCall(1, Direction.UP));
        controller.addCarCall(new CarCall(3));

        LiftState idleAtZero = new LiftState(0, LiftStatus.IDLE);
        assertEquals(Action.MOVE_UP, controller.decideNextAction(idleAtZero, 0));

        LiftState arrivingAtOne = new LiftState(1, LiftStatus.MOVING_UP);
        assertEquals(Action.IDLE, controller.decideNextAction(arrivingAtOne, 1));

        LiftState idleAtOne = new LiftState(1, LiftStatus.IDLE);
        assertEquals(Action.OPEN_DOOR, controller.decideNextAction(idleAtOne, 2));

        LiftState doorsOpenAtOne = new LiftState(1, LiftStatus.DOORS_OPEN);
        controller.decideNextAction(doorsOpenAtOne, 3);

        controller.addHallCall(new HallCall(2, Direction.UP));

        LiftState leavingOne = new LiftState(1, LiftStatus.IDLE);
        assertEquals(Action.MOVE_UP, controller.decideNextAction(leavingOne, 4));

        LiftState arrivingAtTwo = new LiftState(2, LiftStatus.MOVING_UP);
        assertEquals(Action.IDLE, controller.decideNextAction(arrivingAtTwo, 5));
    }

    @Test
    public void testDefersOppositeHallCallUntilAfterReversal() {
        controller.addHallCall(new HallCall(1, Direction.UP));
        controller.addCarCall(new CarCall(3));

        LiftState idleAtZero = new LiftState(0, LiftStatus.IDLE);
        assertEquals(Action.MOVE_UP, controller.decideNextAction(idleAtZero, 0));

        LiftState arrivingAtOne = new LiftState(1, LiftStatus.MOVING_UP);
        assertEquals(Action.IDLE, controller.decideNextAction(arrivingAtOne, 1));

        LiftState idleAtOne = new LiftState(1, LiftStatus.IDLE);
        assertEquals(Action.OPEN_DOOR, controller.decideNextAction(idleAtOne, 2));

        LiftState doorsOpenAtOne = new LiftState(1, LiftStatus.DOORS_OPEN);
        controller.decideNextAction(doorsOpenAtOne, 3);

        controller.addHallCall(new HallCall(2, Direction.DOWN));

        LiftState leavingOne = new LiftState(1, LiftStatus.IDLE);
        assertEquals(Action.MOVE_UP, controller.decideNextAction(leavingOne, 4));

        LiftState arrivingAtTwo = new LiftState(2, LiftStatus.MOVING_UP);
        assertEquals(Action.MOVE_UP, controller.decideNextAction(arrivingAtTwo, 5));

        LiftState arrivingAtThree = new LiftState(3, LiftStatus.MOVING_UP);
        assertEquals(Action.IDLE, controller.decideNextAction(arrivingAtThree, 6));

        LiftState idleAtThree = new LiftState(3, LiftStatus.IDLE);
        assertEquals(Action.OPEN_DOOR, controller.decideNextAction(idleAtThree, 7));

        LiftState doorsOpenAtThree = new LiftState(3, LiftStatus.DOORS_OPEN);
        controller.decideNextAction(doorsOpenAtThree, 8);

        LiftState reversingFromThree = new LiftState(3, LiftStatus.IDLE);
        assertEquals(Action.MOVE_DOWN, controller.decideNextAction(reversingFromThree, 9));

        LiftState arrivingAtTwoDown = new LiftState(2, LiftStatus.MOVING_DOWN);
        assertEquals(Action.IDLE, controller.decideNextAction(arrivingAtTwoDown, 10));
    }

    @Test
    public void testCarCallsRemainEligibleWhileTravelingUp() {
        controller.addHallCall(new HallCall(1, Direction.UP));
        controller.addCarCall(new CarCall(3));

        LiftState idleAtZero = new LiftState(0, LiftStatus.IDLE);
        assertEquals(Action.MOVE_UP, controller.decideNextAction(idleAtZero, 0));

        LiftState arrivingAtOne = new LiftState(1, LiftStatus.MOVING_UP);
        assertEquals(Action.IDLE, controller.decideNextAction(arrivingAtOne, 1));

        LiftState idleAtOne = new LiftState(1, LiftStatus.IDLE);
        assertEquals(Action.OPEN_DOOR, controller.decideNextAction(idleAtOne, 2));

        LiftState doorsOpenAtOne = new LiftState(1, LiftStatus.DOORS_OPEN);
        controller.decideNextAction(doorsOpenAtOne, 3);

        controller.addCarCall(new CarCall(2));

        LiftState leavingOne = new LiftState(1, LiftStatus.IDLE);
        assertEquals(Action.MOVE_UP, controller.decideNextAction(leavingOne, 4));

        LiftState arrivingAtTwo = new LiftState(2, LiftStatus.MOVING_UP);
        assertEquals(Action.IDLE, controller.decideNextAction(arrivingAtTwo, 5));
    }
}
