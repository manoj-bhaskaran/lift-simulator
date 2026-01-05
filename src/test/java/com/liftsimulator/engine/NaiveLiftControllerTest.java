package com.liftsimulator.engine;

import com.liftsimulator.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NaiveLiftControllerTest {
    private NaiveLiftController controller;

    @BeforeEach
    public void setUp() {
        controller = new NaiveLiftController();
    }

    @Test
    public void testOpenDoorWhenAtRequestedFloorWithCarCall() {
        // Add a car call to floor 3
        controller.addCarCall(new CarCall(3));

        // Lift is at floor 3 with doors closed
        LiftState state = new LiftState(3, Direction.IDLE, DoorState.CLOSED);

        // Should open door when at requested floor
        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.OPEN_DOOR, action);
    }

    @Test
    public void testOpenDoorWhenAtRequestedFloorWithHallCall() {
        // Add a hall call at floor 2
        controller.addHallCall(new HallCall(2, Direction.UP));

        // Lift is at floor 2 with doors closed
        LiftState state = new LiftState(2, Direction.IDLE, DoorState.CLOSED);

        // Should open door when at requested floor
        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.OPEN_DOOR, action);
    }

    @Test
    public void testIdleWhileDoorsOpenDuringDwellTime() {
        // Add a car call to floor 5 and simulate arriving there
        controller.addCarCall(new CarCall(5));

        // First, lift arrives and opens door (tick 0)
        LiftState closedState = new LiftState(5, Direction.IDLE, DoorState.CLOSED);
        Action openAction = controller.decideNextAction(closedState, 0);
        assertEquals(Action.OPEN_DOOR, openAction);

        // Doors are now open, should idle during dwell time (ticks 1-2)
        LiftState openState = new LiftState(5, Direction.IDLE, DoorState.OPEN);

        Action idleAction1 = controller.decideNextAction(openState, 1);
        assertEquals(Action.IDLE, idleAction1);

        Action idleAction2 = controller.decideNextAction(openState, 2);
        assertEquals(Action.IDLE, idleAction2);
    }

    @Test
    public void testCloseDoorAfterDwellTime() {
        // Add a car call to floor 4 and simulate arriving there
        controller.addCarCall(new CarCall(4));

        // First, lift arrives and opens door (tick 0)
        LiftState closedState = new LiftState(4, Direction.IDLE, DoorState.CLOSED);
        controller.decideNextAction(closedState, 0);

        // Doors are now open, simulate dwell time passing
        LiftState openState = new LiftState(4, Direction.IDLE, DoorState.OPEN);

        // Tick 1: still dwelling
        controller.decideNextAction(openState, 1);

        // Tick 2: still dwelling
        controller.decideNextAction(openState, 2);

        // Tick 3: dwell time elapsed (3 ticks passed), should close
        Action closeAction = controller.decideNextAction(openState, 3);
        assertEquals(Action.CLOSE_DOOR, closeAction);
    }

    @Test
    public void testMoveUpToNearestRequestedFloor() {
        // Add a car call to floor 5
        controller.addCarCall(new CarCall(5));

        // Lift is at floor 2 with doors closed
        LiftState state = new LiftState(2, Direction.IDLE, DoorState.CLOSED);

        // Should move up towards floor 5
        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.MOVE_UP, action);
    }

    @Test
    public void testMoveDownToNearestRequestedFloor() {
        // Add a hall call at floor 1
        controller.addHallCall(new HallCall(1, Direction.UP));

        // Lift is at floor 4 with doors closed
        LiftState state = new LiftState(4, Direction.IDLE, DoorState.CLOSED);

        // Should move down towards floor 1
        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.MOVE_DOWN, action);
    }

    @Test
    public void testIdleWhenNoRequests() {
        // No requests added

        // Lift is at floor 0 with doors closed
        LiftState state = new LiftState(0, Direction.IDLE, DoorState.CLOSED);

        // Should idle when no requests
        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.IDLE, action);
    }

    @Test
    public void testNearestFloorSelection() {
        // Add multiple requests
        controller.addCarCall(new CarCall(1));
        controller.addCarCall(new CarCall(7));

        // Lift is at floor 3 with doors closed
        LiftState state = new LiftState(3, Direction.IDLE, DoorState.CLOSED);

        // Floor 1 is distance 2, floor 7 is distance 4
        // Should move towards floor 1 (nearest)
        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.MOVE_DOWN, action);
    }

    @Test
    public void testRequestsClearedWhenServiced() {
        // Add a car call to floor 2
        controller.addCarCall(new CarCall(2));

        // Lift arrives at floor 2, doors closed
        LiftState closedState = new LiftState(2, Direction.IDLE, DoorState.CLOSED);

        // First decision: open door (this clears the request)
        Action openAction = controller.decideNextAction(closedState, 0);
        assertEquals(Action.OPEN_DOOR, openAction);

        // Simulate doors opening
        LiftState openState = new LiftState(2, Direction.IDLE, DoorState.OPEN);
        controller.decideNextAction(openState, 1);
        controller.decideNextAction(openState, 2);

        // After dwell time, close doors
        Action closeAction = controller.decideNextAction(openState, 3);
        assertEquals(Action.CLOSE_DOOR, closeAction);

        // Now doors are closed again, no more requests
        LiftState closedAgainState = new LiftState(2, Direction.IDLE, DoorState.CLOSED);
        Action idleAction = controller.decideNextAction(closedAgainState, 4);

        // Should be idle since request was cleared
        assertEquals(Action.IDLE, idleAction);
    }

    @Test
    public void testMultipleRequestsHandled() {
        // Add multiple car calls
        controller.addCarCall(new CarCall(3));
        controller.addCarCall(new CarCall(5));

        // Lift at floor 0
        LiftState state = new LiftState(0, Direction.IDLE, DoorState.CLOSED);

        // Should move up towards nearest (floor 3)
        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.MOVE_UP, action);
    }

    @Test
    public void testMixedCarAndHallCalls() {
        // Add both car call and hall call
        controller.addCarCall(new CarCall(4));
        controller.addHallCall(new HallCall(2, Direction.DOWN));

        // Lift at floor 0
        LiftState state = new LiftState(0, Direction.IDLE, DoorState.CLOSED);

        // Floor 2 is nearer (distance 2) than floor 4 (distance 4)
        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.MOVE_UP, action);
    }

    @Test
    public void testClearsAllRequestsForFloor() {
        // Add multiple requests for the same floor
        controller.addCarCall(new CarCall(3));
        controller.addHallCall(new HallCall(3, Direction.UP));
        controller.addHallCall(new HallCall(3, Direction.DOWN));

        // Lift arrives at floor 3
        LiftState closedState = new LiftState(3, Direction.IDLE, DoorState.CLOSED);

        // Opens door and clears all requests for floor 3
        Action openAction = controller.decideNextAction(closedState, 0);
        assertEquals(Action.OPEN_DOOR, openAction);

        // After closing doors, no requests left
        LiftState openState = new LiftState(3, Direction.IDLE, DoorState.OPEN);
        controller.decideNextAction(openState, 1);
        controller.decideNextAction(openState, 2);
        controller.decideNextAction(openState, 3); // Close door

        LiftState closedAgainState = new LiftState(3, Direction.IDLE, DoorState.CLOSED);
        Action idleAction = controller.decideNextAction(closedAgainState, 4);
        assertEquals(Action.IDLE, idleAction);
    }
}
