package com.liftsimulator.engine;

import com.liftsimulator.domain.Action;
import com.liftsimulator.domain.CarCall;
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
}
