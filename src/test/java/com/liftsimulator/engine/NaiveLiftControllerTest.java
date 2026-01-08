package com.liftsimulator.engine;

import com.liftsimulator.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NaiveLiftControllerTest {
    private NaiveLiftController controller;

    @BeforeEach
    public void setUp() {
        controller = new NaiveLiftController();
    }

    @Test
    public void testOpenDoorWhenAtRequestedFloorWithCarCall() {
        // Add a car call to floor 3.
        controller.addCarCall(new CarCall(3));

        // Lift is at floor 3 with doors closed.
        LiftState state = new LiftState(3, LiftStatus.IDLE);

        // Should open door when at requested floor.
        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.OPEN_DOOR, action);
    }

    @Test
    public void testOpenDoorWhenAtRequestedFloorWithHallCall() {
        // Add a hall call at floor 2.
        controller.addHallCall(new HallCall(2, Direction.UP));

        // Lift is at floor 2 with doors closed.
        LiftState state = new LiftState(2, LiftStatus.IDLE);

        // Should open door when at requested floor.
        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.OPEN_DOOR, action);
    }

    @Test
    public void testStopsThenOpensDoorWhenArrivingWhileMoving() {
        // Add a car call to floor 3.
        controller.addCarCall(new CarCall(3));

        // Lift arrives at floor 3 while moving up.
        LiftState movingState = new LiftState(3, LiftStatus.MOVING_UP);
        Action stopAction = controller.decideNextAction(movingState, 0);
        assertEquals(Action.IDLE, stopAction);

        // Next tick after stopping, should open doors and clear request.
        LiftState stoppedState = new LiftState(3, LiftStatus.IDLE);
        Action openAction = controller.decideNextAction(stoppedState, 1);
        assertEquals(Action.OPEN_DOOR, openAction);
    }

    @Test
    public void testRequestClearedWhenArrivingDuringDoorOpening() {
        // Lift is opening doors at floor 2, request arrives at the same floor.
        controller.addCarCall(new CarCall(2));

        LiftState openingState = new LiftState(2, LiftStatus.DOORS_OPENING);
        Action action = controller.decideNextAction(openingState, 0);
        assertEquals(Action.IDLE, action);

        // After doors finish opening and later close, request should be cleared.
        LiftState closedState = new LiftState(2, LiftStatus.IDLE);
        Action idleAction = controller.decideNextAction(closedState, 1);
        assertEquals(Action.IDLE, idleAction);
    }

    @Test
    public void testIdleWhileDoorsOpenDuringDwellTime() {
        // Add a car call to floor 5 and simulate arriving there.
        controller.addCarCall(new CarCall(5));

        // First, lift arrives and opens door (tick 0).
        LiftState closedState = new LiftState(5, LiftStatus.IDLE);
        Action openAction = controller.decideNextAction(closedState, 0);
        assertEquals(Action.OPEN_DOOR, openAction);

        // Doors are now open, should idle during dwell time (ticks 1-2).
        LiftState openState = new LiftState(5, LiftStatus.DOORS_OPEN);

        Action idleAction1 = controller.decideNextAction(openState, 1);
        assertEquals(Action.IDLE, idleAction1);

        Action idleAction2 = controller.decideNextAction(openState, 2);
        assertEquals(Action.IDLE, idleAction2);
    }

    @Test
    public void testCloseDoorAfterDwellTime() {
        // Add a car call to floor 4 and simulate arriving there.
        controller.addCarCall(new CarCall(4));

        // First, lift arrives and opens door (tick 0).
        LiftState closedState = new LiftState(4, LiftStatus.IDLE);
        controller.decideNextAction(closedState, 0);

        // Doors are now open, simulate dwell time passing.
        LiftState openState = new LiftState(4, LiftStatus.DOORS_OPEN);

        // Tick 1: still dwelling.
        controller.decideNextAction(openState, 1);

        // Tick 2: still dwelling.
        controller.decideNextAction(openState, 2);

        // Tick 3: controller should remain idle; engine handles close timing.
        Action idleAction = controller.decideNextAction(openState, 3);
        assertEquals(Action.IDLE, idleAction);
    }

    @Test
    public void testMoveUpToNearestRequestedFloor() {
        // Add a car call to floor 5.
        controller.addCarCall(new CarCall(5));

        // Lift is at floor 2 with doors closed.
        LiftState state = new LiftState(2, LiftStatus.IDLE);

        // Should move up towards floor 5.
        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.MOVE_UP, action);
    }

    @Test
    public void testMoveDownToNearestRequestedFloor() {
        // Add a hall call at floor 1.
        controller.addHallCall(new HallCall(1, Direction.UP));

        // Lift is at floor 4 with doors closed.
        LiftState state = new LiftState(4, LiftStatus.IDLE);

        // Should move down towards floor 1.
        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.MOVE_DOWN, action);
    }

    @Test
    public void testIdleWhenNoRequests() {
        // No requests added.

        // Lift is at floor 0 with doors closed.
        LiftState state = new LiftState(0, LiftStatus.IDLE);

        // Should idle when no requests.
        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.IDLE, action);
    }

    @Test
    public void testNearestFloorSelection() {
        // Add multiple requests.
        controller.addCarCall(new CarCall(1));
        controller.addCarCall(new CarCall(7));

        // Lift is at floor 3 with doors closed.
        LiftState state = new LiftState(3, LiftStatus.IDLE);

        /*
         * Floor 1 is distance 2, floor 7 is distance 4.
         * Should move towards floor 1 (nearest).
         */
        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.MOVE_DOWN, action);
    }

    @Test
    public void testTieBreakerPrefersLowerFloorForEquidistantCarCalls() {
        // Add equidistant requests around the current floor.
        controller.addCarCall(new CarCall(1));
        controller.addCarCall(new CarCall(5));

        // Lift is at floor 3 with doors closed.
        LiftState state = new LiftState(3, LiftStatus.IDLE);

        // Floors 1 and 5 are both distance 2; should prefer lower floor (1).
        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.MOVE_DOWN, action);
    }

    @Test
    public void testTieBreakerPrefersLowerFloorForEquidistantMixedCalls() {
        // Add equidistant mixed requests around the current floor.
        controller.addCarCall(new CarCall(2));
        controller.addHallCall(new HallCall(6, Direction.UP));

        // Lift is at floor 4 with doors closed.
        LiftState state = new LiftState(4, LiftStatus.IDLE);

        // Floors 2 and 6 are both distance 2; should prefer lower floor (2).
        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.MOVE_DOWN, action);
    }

    @Test
    public void testRequestsClearedWhenServiced() {
        // Add a car call to floor 2.
        controller.addCarCall(new CarCall(2));

        // Lift arrives at floor 2, doors closed.
        LiftState closedState = new LiftState(2, LiftStatus.IDLE);

        // First decision: open door (this clears the request).
        Action openAction = controller.decideNextAction(closedState, 0);
        assertEquals(Action.OPEN_DOOR, openAction);

        // Simulate doors opening.
        LiftState openState = new LiftState(2, LiftStatus.DOORS_OPEN);
        controller.decideNextAction(openState, 1);
        controller.decideNextAction(openState, 2);

        // After dwell time, controller still idles and requests should remain cleared.
        controller.decideNextAction(openState, 3);

        // Now doors are closed again (simulated), no more requests.
        LiftState closedAgainState = new LiftState(2, LiftStatus.IDLE);
        Action idleAction = controller.decideNextAction(closedAgainState, 4);

        // Should be idle since request was cleared.
        assertEquals(Action.IDLE, idleAction);
    }

    @Test
    public void testMultipleRequestsHandled() {
        // Add multiple car calls.
        controller.addCarCall(new CarCall(3));
        controller.addCarCall(new CarCall(5));

        // Lift at floor 0.
        LiftState state = new LiftState(0, LiftStatus.IDLE);

        // Should move up towards nearest (floor 3).
        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.MOVE_UP, action);
    }

    @Test
    public void testMixedCarAndHallCalls() {
        // Add both car call and hall call.
        controller.addCarCall(new CarCall(4));
        controller.addHallCall(new HallCall(2, Direction.DOWN));

        // Lift at floor 0.
        LiftState state = new LiftState(0, LiftStatus.IDLE);

        // Floor 2 is nearer (distance 2) than floor 4 (distance 4).
        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.MOVE_UP, action);
    }

    @Test
    public void testClearsAllRequestsForFloor() {
        // Add multiple requests for the same floor.
        controller.addCarCall(new CarCall(3));
        controller.addHallCall(new HallCall(3, Direction.UP));
        controller.addHallCall(new HallCall(3, Direction.DOWN));

        // Lift arrives at floor 3.
        LiftState closedState = new LiftState(3, LiftStatus.IDLE);

        // Opens door and clears all requests for floor 3.
        Action openAction = controller.decideNextAction(closedState, 0);
        assertEquals(Action.OPEN_DOOR, openAction);

        // After closing doors, no requests left.
        LiftState openState = new LiftState(3, LiftStatus.DOORS_OPEN);
        controller.decideNextAction(openState, 1);
        controller.decideNextAction(openState, 2);
        controller.decideNextAction(openState, 3); // Dwell tick.

        LiftState closedAgainState = new LiftState(3, LiftStatus.IDLE);
        Action idleAction = controller.decideNextAction(closedAgainState, 4);
        assertEquals(Action.IDLE, idleAction);
    }

    @Test
    public void testRequestToReopenDoorsWhileClosing() {
        /*
         * Controller should request to reopen doors if a request arrives for current floor.
         * while doors are closing (SimulationEngine will decide based on reopen window).
         */

        // No initial requests.
        LiftState closingState = new LiftState(5, LiftStatus.DOORS_CLOSING);

        // Add a car call for the current floor while doors are closing.
        controller.addCarCall(new CarCall(5));

        // Controller should request to OPEN_DOOR.
        Action action = controller.decideNextAction(closingState, 0);
        assertEquals(Action.OPEN_DOOR, action);
    }

    @Test
    public void testIdleWhenDoorsClosingWithNoRequestsForCurrentFloor() {
        // Controller should remain idle if no requests for current floor while doors closing.

        // Add a request for a different floor.
        controller.addCarCall(new CarCall(3));

        LiftState closingState = new LiftState(5, LiftStatus.DOORS_CLOSING);

        // Controller should remain idle (let doors finish closing).
        Action action = controller.decideNextAction(closingState, 0);
        assertEquals(Action.IDLE, action);
    }

    @Test
    public void testIntegrationDoorReopenWithinWindow() {
        // Integration test: request arrives while doors closing, within reopen window.
        SimulationEngine engine = SimulationEngine.builder(controller, 0, 10)
                .travelTicksPerFloor(1)
                .doorTransitionTicks(2)
                .doorDwellTicks(2)
                .doorReopenWindowTicks(2)
                .build();

        // Add initial request to floor 5.
        controller.addCarCall(new CarCall(5));

        // Move to floor 5.
        for (int i = 0; i < 5; i++) {
            engine.tick();
        }
        assertEquals(5, engine.getCurrentState().getFloor());

        // Stop then open doors.
        engine.tick(); // stop at floor 5 (MOVING_UP -> IDLE).
        engine.tick(); // start opening.
        engine.tick(); // doors open.
        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());

        // Dwell time.
        engine.tick(); // dwell 1.
        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());

        // Dwell completes, doors start closing.
        engine.tick(); // dwell 2 completes, closing starts and advances (elapsed = 1).
        assertEquals(LiftStatus.DOORS_CLOSING, engine.getCurrentState().getStatus());

        // New request arrives for same floor within reopen window.
        controller.addCarCall(new CarCall(5));

        // Next tick should reopen doors (elapsed=1 < window=2).
        engine.tick();
        assertEquals(LiftStatus.DOORS_OPENING, engine.getCurrentState().getStatus());

        // Doors should fully open.
        engine.tick();
        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());
    }

    @Test
    public void testIntegrationDoorDoesNotReopenOutsideWindow() {
        // Integration test: request arrives while doors closing, outside reopen window.
        SimulationEngine engine = SimulationEngine.builder(controller, 0, 10)
                .travelTicksPerFloor(1)
                .doorTransitionTicks(4)
                .doorDwellTicks(2)
                .doorReopenWindowTicks(2)
                .build();

        // Add initial request to floor 5.
        controller.addCarCall(new CarCall(5));

        // Move to floor 5.
        for (int i = 0; i < 5; i++) {
            engine.tick();
        }

        // Stop then open doors.
        engine.tick(); // stop at floor 5 (MOVING_UP -> IDLE).
        engine.tick(); // start opening.
        engine.tick();
        engine.tick();
        engine.tick(); // doors open (4 ticks to open).
        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());

        // Dwell time.
        engine.tick(); // dwell 1.
        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());

        // Doors start closing.
        engine.tick(); // dwell 2 completes, closing starts and advances (elapsed = 1).
        assertEquals(LiftStatus.DOORS_CLOSING, engine.getCurrentState().getStatus());

        engine.tick(); // closing continues (elapsed = 2, now outside window).
        assertEquals(LiftStatus.DOORS_CLOSING, engine.getCurrentState().getStatus());

        // New request arrives outside window (elapsed = 2 >= window = 2).
        controller.addCarCall(new CarCall(5));

        // Next tick should NOT reopen (elapsed=2 >= window=2).
        engine.tick(); // elapsed = 3, stays closing.
        assertEquals(LiftStatus.DOORS_CLOSING, engine.getCurrentState().getStatus());

        // Doors should finish closing.
        engine.tick();
        assertEquals(LiftStatus.IDLE, engine.getCurrentState().getStatus());

        // Request should still be pending, so lift should reopen.
        engine.tick();
        assertEquals(LiftStatus.DOORS_OPENING, engine.getCurrentState().getStatus());
    }

    @Test
    public void testCancelQueuedRequest() {
        // Add a car call.
        LiftRequest request = LiftRequest.carCall(5);
        controller.addRequest(request);

        // Verify request is queued.
        assertEquals(RequestState.QUEUED, request.getState());
        assertEquals(1, controller.getRequests().size());

        // Cancel the request.
        boolean cancelled = controller.cancelRequest(request.getId());
        assertTrue(cancelled);

        // Verify request is cancelled and removed.
        assertEquals(RequestState.CANCELLED, request.getState());
        assertEquals(0, controller.getRequests().size());

        // Lift should remain idle (no requests).
        LiftState state = new LiftState(0, LiftStatus.IDLE);
        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.IDLE, action);
    }

    @Test
    public void testCancelAssignedRequest() {
        // Add a car call to floor 5.
        LiftRequest request = LiftRequest.carCall(5);
        controller.addRequest(request);

        // Lift starts moving towards floor 5, which assigns the request.
        LiftState state = new LiftState(0, LiftStatus.IDLE);
        controller.decideNextAction(state, 0);

        // Verify request is assigned.
        assertEquals(RequestState.ASSIGNED, request.getState());

        // Cancel the request while assigned.
        boolean cancelled = controller.cancelRequest(request.getId());
        assertTrue(cancelled);

        // Verify request is cancelled and removed.
        assertEquals(RequestState.CANCELLED, request.getState());
        assertEquals(0, controller.getRequests().size());

        // Lift should now be idle (no requests left).
        Action action = controller.decideNextAction(state, 1);
        assertEquals(Action.IDLE, action);
    }

    @Test
    public void testCancelServingRequest() {
        // Add a car call to floor 3.
        LiftRequest request = LiftRequest.carCall(3);
        controller.addRequest(request);

        // Lift arrives at floor 3 while moving.
        LiftState movingState = new LiftState(3, LiftStatus.MOVING_UP);
        controller.decideNextAction(movingState, 0);

        // Request should be serving.
        assertEquals(RequestState.SERVING, request.getState());

        // Cancel the request while serving.
        boolean cancelled = controller.cancelRequest(request.getId());
        assertTrue(cancelled);

        // Verify request is cancelled and removed.
        assertEquals(RequestState.CANCELLED, request.getState());
        assertEquals(0, controller.getRequests().size());

        // Lift should now idle instead of opening doors.
        LiftState stoppedState = new LiftState(3, LiftStatus.IDLE);
        Action action = controller.decideNextAction(stoppedState, 1);
        assertEquals(Action.IDLE, action);
    }

    @Test
    public void testCancelNonExistentRequest() {
        // Try to cancel a request that doesn't exist.
        boolean cancelled = controller.cancelRequest(999L);
        assertFalse(cancelled);
    }

    @Test
    public void testCancelAlreadyCancelledRequest() {
        // Add and cancel a request.
        LiftRequest request = LiftRequest.carCall(5);
        controller.addRequest(request);
        controller.cancelRequest(request.getId());

        // Try to cancel again.
        boolean cancelled = controller.cancelRequest(request.getId());
        assertFalse(cancelled);
    }

    @Test
    public void testCancelCompletedRequest() {
        // Add a car call to floor 2.
        LiftRequest request = LiftRequest.carCall(2);
        controller.addRequest(request);

        // Lift serves the request - opens doors.
        LiftState state = new LiftState(2, LiftStatus.IDLE);
        controller.decideNextAction(state, 0);

        // Doors are opening - this completes the request.
        LiftState openingState = new LiftState(2, LiftStatus.DOORS_OPENING);
        controller.decideNextAction(openingState, 1);

        // Request should be completed and removed.
        assertEquals(RequestState.COMPLETED, request.getState());
        assertEquals(0, controller.getRequests().size());

        // Try to cancel the completed request.
        boolean cancelled = controller.cancelRequest(request.getId());
        assertFalse(cancelled);
    }

    @Test
    public void testParksAtHomeFloorAfterIdleTimeout() {
        NaiveLiftController parkingController = new NaiveLiftController(2, 3);
        SimulationEngine engine = SimulationEngine.builder(parkingController, 0, 5).build();

        engine.tick(); // tick 0.
        engine.tick(); // tick 1.
        engine.tick(); // tick 2.
        assertEquals(0, engine.getCurrentState().getFloor());

        engine.tick(); // tick 3 -> start parking.
        assertEquals(1, engine.getCurrentState().getFloor());

        engine.tick(); // tick 4 -> continue parking.
        assertEquals(2, engine.getCurrentState().getFloor());

        engine.tick(); // tick 5 -> stop at home.
        assertEquals(LiftStatus.IDLE, engine.getCurrentState().getStatus());
        assertEquals(2, engine.getCurrentState().getFloor());
    }

    @Test
    public void testParkingInterruptedByNewRequest() {
        NaiveLiftController parkingController = new NaiveLiftController(4, 1);
        SimulationEngine engine = SimulationEngine.builder(parkingController, 0, 5).build();

        engine.tick(); // tick 0.
        engine.tick(); // tick 1 -> start parking to floor 4.
        assertEquals(1, engine.getCurrentState().getFloor());

        parkingController.addCarCall(new CarCall(1));

        engine.tick(); // tick 2 -> stop for request.
        assertEquals(LiftStatus.IDLE, engine.getCurrentState().getStatus());
        assertEquals(1, engine.getCurrentState().getFloor());

        engine.tick(); // tick 3 -> open doors for request.
        assertEquals(LiftStatus.DOORS_OPENING, engine.getCurrentState().getStatus());
    }

    @Test
    public void testCancelledRequestNeverServed() {
        // Add multiple requests.
        LiftRequest request1 = LiftRequest.carCall(3);
        LiftRequest request2 = LiftRequest.carCall(5);
        LiftRequest request3 = LiftRequest.carCall(7);

        controller.addRequest(request1);
        controller.addRequest(request2);
        controller.addRequest(request3);

        // Cancel request2.
        controller.cancelRequest(request2.getId());

        // Verify request2 is cancelled.
        assertEquals(RequestState.CANCELLED, request2.getState());
        assertEquals(2, controller.getRequests().size());

        // Lift should move to floor 3 (nearest).
        LiftState state = new LiftState(0, LiftStatus.IDLE);
        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.MOVE_UP, action);

        // After serving floor 3, should move to floor 7 (skipping cancelled floor 5).
        LiftState state3 = new LiftState(3, LiftStatus.IDLE);
        controller.decideNextAction(state3, 1); // Open doors for floor 3.

        // Doors opening - completes floor 3 request.
        LiftState opening3 = new LiftState(3, LiftStatus.DOORS_OPENING);
        controller.decideNextAction(opening3, 2);

        // After clearing floor 3, verify floor 7 is the next target.
        assertEquals(1, controller.getRequests().size());
        LiftState afterFloor3 = new LiftState(3, LiftStatus.IDLE);
        Action nextAction = controller.decideNextAction(afterFloor3, 3);
        assertEquals(Action.MOVE_UP, nextAction);
    }

    @Test
    public void testCancellationDoesNotCorruptQueue() {
        // Add multiple requests.
        LiftRequest request1 = LiftRequest.carCall(2);
        LiftRequest request2 = LiftRequest.carCall(4);
        LiftRequest request3 = LiftRequest.carCall(6);
        LiftRequest request4 = LiftRequest.carCall(8);

        controller.addRequest(request1);
        controller.addRequest(request2);
        controller.addRequest(request3);
        controller.addRequest(request4);

        assertEquals(4, controller.getRequests().size());

        // Cancel middle requests.
        controller.cancelRequest(request2.getId());
        controller.cancelRequest(request3.getId());

        assertEquals(2, controller.getRequests().size());

        // Verify remaining requests are still valid.
        LiftState state = new LiftState(0, LiftStatus.IDLE);
        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.MOVE_UP, action);

        // Serve floor 2.
        LiftState state2 = new LiftState(2, LiftStatus.IDLE);
        controller.decideNextAction(state2, 1);

        // Doors opening - completes floor 2 request.
        LiftState opening2 = new LiftState(2, LiftStatus.DOORS_OPENING);
        controller.decideNextAction(opening2, 2);

        // Should go to floor 8 next (floor 4 and 6 were cancelled).
        assertEquals(1, controller.getRequests().size());
        LiftState afterFloor2 = new LiftState(2, LiftStatus.IDLE);
        Action nextAction = controller.decideNextAction(afterFloor2, 3);
        assertEquals(Action.MOVE_UP, nextAction);
    }

    @Test
    public void testIntegrationCancelWhileMoving() {
        // Integration test: cancel request while lift is moving towards it.
        SimulationEngine engine = SimulationEngine.builder(controller, 0, 10)
                .travelTicksPerFloor(1)
                .doorTransitionTicks(2)
                .doorDwellTicks(2)
                .doorReopenWindowTicks(2)
                .build();

        // Add request to floor 5.
        LiftRequest request = LiftRequest.carCall(5);
        controller.addRequest(request);

        // Move 3 floors up (movement completes in the same tick as MOVE_UP with ticksPerFloor=1).
        engine.tick(); // start moving, reaches floor 1.
        engine.tick(); // floor 2.
        engine.tick(); // floor 3.

        assertEquals(3, engine.getCurrentState().getFloor());
        assertEquals(LiftStatus.MOVING_UP, engine.getCurrentState().getStatus());

        // Cancel the request while lift is moving.
        boolean cancelled = controller.cancelRequest(request.getId());
        assertTrue(cancelled);
        assertEquals(RequestState.CANCELLED, request.getState());

        // Next tick: lift completes movement to floor 3, then stops.
        engine.tick();
        assertEquals(3, engine.getCurrentState().getFloor());
        assertEquals(LiftStatus.IDLE, engine.getCurrentState().getStatus());

        // Lift should remain idle (no requests).
        engine.tick();
        assertEquals(LiftStatus.IDLE, engine.getCurrentState().getStatus());
        assertEquals(3, engine.getCurrentState().getFloor()); // Stays at floor 3.
    }

    @Test
    public void testIntegrationCancelMultipleRequestsSameFloor() {
        // Add multiple requests for the same floor.
        LiftRequest hallCall = LiftRequest.hallCall(3, Direction.UP);
        LiftRequest carCall = LiftRequest.carCall(3);

        controller.addRequest(hallCall);
        controller.addRequest(carCall);

        assertEquals(2, controller.getRequests().size());

        // Cancel one request.
        controller.cancelRequest(hallCall.getId());

        assertEquals(1, controller.getRequests().size());

        // Lift should still go to floor 3 (one request remaining).
        LiftState state = new LiftState(0, LiftStatus.IDLE);
        Action action = controller.decideNextAction(state, 0);
        assertEquals(Action.MOVE_UP, action);

        // Cancel the second request.
        controller.cancelRequest(carCall.getId());

        assertEquals(0, controller.getRequests().size());

        // Lift should now idle (no requests).
        Action idleAction = controller.decideNextAction(state, 1);
        assertEquals(Action.IDLE, idleAction);
    }
}
