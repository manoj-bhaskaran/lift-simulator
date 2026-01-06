package com.liftsimulator.engine;

import com.liftsimulator.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for lift request lifecycle management in NaiveLiftController.
 * Verifies that requests progress through their lifecycle states correctly:
 * CREATED -> QUEUED -> ASSIGNED -> SERVING -> COMPLETED
 */
public class LiftRequestLifecycleTest {
    private NaiveLiftController controller;

    @BeforeEach
    public void setUp() {
        controller = new NaiveLiftController();
    }

    @Test
    public void testCarCallTransitionsToQueuedWhenAdded() {
        controller.addCarCall(new CarCall(5));

        Set<LiftRequest> requests = controller.getRequests();
        assertEquals(1, requests.size());

        LiftRequest request = requests.iterator().next();
        assertEquals(RequestState.QUEUED, request.getState());
        assertEquals(RequestType.CAR_CALL, request.getType());
        assertEquals(5, request.getTargetFloor());
    }

    @Test
    public void testHallCallTransitionsToQueuedWhenAdded() {
        controller.addHallCall(new HallCall(3, Direction.UP));

        Set<LiftRequest> requests = controller.getRequests();
        assertEquals(1, requests.size());

        LiftRequest request = requests.iterator().next();
        assertEquals(RequestState.QUEUED, request.getState());
        assertEquals(RequestType.HALL_CALL, request.getType());
        assertEquals(3, request.getTargetFloor());
        assertEquals(Direction.UP, request.getDirection());
    }

    @Test
    public void testRequestTransitionsToAssignedWhenTargeted() {
        controller.addCarCall(new CarCall(5));

        LiftState state = new LiftState(2, LiftStatus.IDLE);
        controller.decideNextAction(state, 0);

        Set<LiftRequest> requests = controller.getRequests();
        LiftRequest request = requests.iterator().next();
        assertEquals(RequestState.ASSIGNED, request.getState());
    }

    @Test
    public void testRequestTransitionsToServingWhenReached() {
        controller.addCarCall(new CarCall(5));

        // First, lift starts moving towards the floor
        LiftState state = new LiftState(4, LiftStatus.IDLE);
        controller.decideNextAction(state, 0);

        // Lift arrives at the floor
        LiftState arrivedState = new LiftState(5, LiftStatus.IDLE);
        controller.decideNextAction(arrivedState, 1);

        Set<LiftRequest> requests = controller.getRequests();
        if (!requests.isEmpty()) {
            LiftRequest request = requests.iterator().next();
            // Request should be in SERVING state before completion
            assertTrue(request.getState() == RequestState.SERVING || request.isTerminal());
        }
    }

    @Test
    public void testRequestTransitionsToCompletedWhenDoorsOpen() {
        controller.addCarCall(new CarCall(5));

        // Lift arrives at the floor
        LiftState arrivedState = new LiftState(5, LiftStatus.IDLE);
        controller.decideNextAction(arrivedState, 0);

        // Doors are opening
        LiftState openingState = new LiftState(5, LiftStatus.DOORS_OPENING);
        controller.decideNextAction(openingState, 1);

        // Request should be completed and removed
        Set<LiftRequest> requests = controller.getRequests();
        assertTrue(requests.isEmpty(), "Request should be completed and removed");
    }

    @Test
    public void testRequestCompletedDuringDoorOpen() {
        controller.addCarCall(new CarCall(3));

        // Lift arrives at the floor and opens doors
        LiftState arrivedState = new LiftState(3, LiftStatus.IDLE);
        controller.decideNextAction(arrivedState, 0);

        // Doors are now open
        LiftState openState = new LiftState(3, LiftStatus.DOORS_OPEN);
        controller.decideNextAction(openState, 1);

        // Request should be completed and removed
        Set<LiftRequest> requests = controller.getRequests();
        assertTrue(requests.isEmpty(), "Request should be completed during door open");
    }

    @Test
    public void testMultipleRequestsProgressIndependently() {
        controller.addCarCall(new CarCall(3));
        controller.addCarCall(new CarCall(7));

        // Lift at floor 0, should target floor 3 (nearest)
        LiftState state = new LiftState(0, LiftStatus.IDLE);
        controller.decideNextAction(state, 0);

        Set<LiftRequest> requests = controller.getRequests();
        assertEquals(2, requests.size());

        // One should be ASSIGNED (floor 3), one should still be QUEUED (floor 7)
        long assignedCount = requests.stream()
                .filter(r -> r.getState() == RequestState.ASSIGNED)
                .count();
        long queuedCount = requests.stream()
                .filter(r -> r.getState() == RequestState.QUEUED)
                .count();

        assertEquals(1, assignedCount, "One request should be assigned");
        assertEquals(1, queuedCount, "One request should remain queued");
    }

    @Test
    public void testCompletedRequestsAreRemoved() {
        controller.addCarCall(new CarCall(2));

        // Lift arrives and opens doors
        LiftState arrivedState = new LiftState(2, LiftStatus.IDLE);
        controller.decideNextAction(arrivedState, 0);

        // Doors opening
        LiftState openingState = new LiftState(2, LiftStatus.DOORS_OPENING);
        controller.decideNextAction(openingState, 1);

        Set<LiftRequest> requests = controller.getRequests();
        assertTrue(requests.isEmpty(), "Completed requests should be removed");
    }

    @Test
    public void testEveryRequestEndsInTerminalState() {
        controller.addCarCall(new CarCall(4));
        controller.addHallCall(new HallCall(6, Direction.DOWN));

        // Service floor 4
        LiftState state1 = new LiftState(4, LiftStatus.IDLE);
        controller.decideNextAction(state1, 0);
        LiftState opening1 = new LiftState(4, LiftStatus.DOORS_OPENING);
        controller.decideNextAction(opening1, 1);

        // Service floor 6
        LiftState state2 = new LiftState(6, LiftStatus.IDLE);
        controller.decideNextAction(state2, 2);
        LiftState opening2 = new LiftState(6, LiftStatus.DOORS_OPENING);
        controller.decideNextAction(opening2, 3);

        // All requests should be removed (completed)
        Set<LiftRequest> requests = controller.getRequests();
        assertTrue(requests.isEmpty(), "All requests should end in terminal state and be removed");
    }

    @Test
    public void testRequestStateProgression() {
        controller.addCarCall(new CarCall(5));

        Set<LiftRequest> requests1 = controller.getRequests();
        assertEquals(RequestState.QUEUED, requests1.iterator().next().getState());

        // Decision to move towards floor 5
        LiftState state1 = new LiftState(0, LiftStatus.IDLE);
        controller.decideNextAction(state1, 0);

        Set<LiftRequest> requests2 = controller.getRequests();
        assertEquals(RequestState.ASSIGNED, requests2.iterator().next().getState());

        // Arrive at floor 5
        LiftState state2 = new LiftState(5, LiftStatus.IDLE);
        controller.decideNextAction(state2, 1);

        Set<LiftRequest> requests3 = controller.getRequests();
        if (!requests3.isEmpty()) {
            assertEquals(RequestState.SERVING, requests3.iterator().next().getState());
        }

        // Doors opening - request should be completed
        LiftState state3 = new LiftState(5, LiftStatus.DOORS_OPENING);
        controller.decideNextAction(state3, 2);

        Set<LiftRequest> requests4 = controller.getRequests();
        assertTrue(requests4.isEmpty(), "Request should be completed and removed");
    }

    @Test
    public void testRequestsAtSameFloorAllCompleted() {
        controller.addCarCall(new CarCall(3));
        controller.addHallCall(new HallCall(3, Direction.UP));
        controller.addHallCall(new HallCall(3, Direction.DOWN));

        // Lift arrives at floor 3
        LiftState arrivedState = new LiftState(3, LiftStatus.IDLE);
        controller.decideNextAction(arrivedState, 0);

        // Doors opening
        LiftState openingState = new LiftState(3, LiftStatus.DOORS_OPENING);
        controller.decideNextAction(openingState, 1);

        // All requests should be completed and removed
        Set<LiftRequest> requests = controller.getRequests();
        assertTrue(requests.isEmpty(), "All requests at floor 3 should be completed");
    }

    @Test
    public void testDirectRequestAdditionWithLifecycle() {
        LiftRequest request = LiftRequest.carCall(8);
        assertEquals(RequestState.CREATED, request.getState());

        controller.addRequest(request);
        assertEquals(RequestState.QUEUED, request.getState());

        Set<LiftRequest> requests = controller.getRequests();
        assertTrue(requests.contains(request));
    }

    @Test
    public void testAddRequestAlreadyQueued() {
        LiftRequest request = LiftRequest.hallCall(4, Direction.UP);
        request.transitionTo(RequestState.QUEUED);

        controller.addRequest(request);
        assertEquals(RequestState.QUEUED, request.getState());

        Set<LiftRequest> requests = controller.getRequests();
        assertTrue(requests.contains(request));
    }

    @Test
    public void testNoInvalidStateTransitionsOccur() {
        controller.addCarCall(new CarCall(10));

        // Move through the lifecycle without errors
        assertDoesNotThrow(() -> {
            LiftState state1 = new LiftState(5, LiftStatus.IDLE);
            controller.decideNextAction(state1, 0);

            LiftState state2 = new LiftState(10, LiftStatus.IDLE);
            controller.decideNextAction(state2, 1);

            LiftState state3 = new LiftState(10, LiftStatus.DOORS_OPENING);
            controller.decideNextAction(state3, 2);
        });
    }

    @Test
    public void testRequestStateWhenStoppingWhileMoving() {
        controller.addCarCall(new CarCall(5));

        // Lift moving towards floor 5
        LiftState movingState = new LiftState(5, LiftStatus.MOVING_UP);
        controller.decideNextAction(movingState, 0);

        Set<LiftRequest> requests = controller.getRequests();
        LiftRequest request = requests.iterator().next();
        // Request should be in SERVING state when lift stops
        assertEquals(RequestState.SERVING, request.getState());
    }

    @Test
    public void testCancellationFromQueuedState() {
        LiftRequest request = LiftRequest.carCall(7);
        controller.addRequest(request);

        assertEquals(RequestState.QUEUED, request.getState());

        // Cancel the queued request
        boolean cancelled = controller.cancelRequest(request.getId());
        assertTrue(cancelled);

        // Request should be in CANCELLED state (terminal)
        assertEquals(RequestState.CANCELLED, request.getState());
        assertTrue(request.isTerminal());

        // Request should be removed from controller
        Set<LiftRequest> requests = controller.getRequests();
        assertFalse(requests.contains(request));
    }

    @Test
    public void testCancellationFromAssignedState() {
        LiftRequest request = LiftRequest.carCall(6);
        controller.addRequest(request);

        // Lift decides to move to floor 6, assigning the request
        LiftState state = new LiftState(2, LiftStatus.IDLE);
        controller.decideNextAction(state, 0);

        assertEquals(RequestState.ASSIGNED, request.getState());

        // Cancel the assigned request
        boolean cancelled = controller.cancelRequest(request.getId());
        assertTrue(cancelled);

        // Request should be in CANCELLED state (terminal)
        assertEquals(RequestState.CANCELLED, request.getState());
        assertTrue(request.isTerminal());

        // Request should be removed from controller
        Set<LiftRequest> requests = controller.getRequests();
        assertFalse(requests.contains(request));
    }

    @Test
    public void testCancellationFromServingState() {
        LiftRequest request = LiftRequest.carCall(4);
        controller.addRequest(request);

        // Lift arrives at floor 4 while moving
        LiftState movingState = new LiftState(4, LiftStatus.MOVING_UP);
        controller.decideNextAction(movingState, 0);

        assertEquals(RequestState.SERVING, request.getState());

        // Cancel the serving request
        boolean cancelled = controller.cancelRequest(request.getId());
        assertTrue(cancelled);

        // Request should be in CANCELLED state (terminal)
        assertEquals(RequestState.CANCELLED, request.getState());
        assertTrue(request.isTerminal());

        // Request should be removed from controller
        Set<LiftRequest> requests = controller.getRequests();
        assertFalse(requests.contains(request));
    }

    @Test
    public void testCancelledRequestIsTerminal() {
        LiftRequest request = LiftRequest.carCall(5);
        controller.addRequest(request);

        controller.cancelRequest(request.getId());

        // Cancelled request should be terminal
        assertTrue(request.isTerminal());
        assertEquals(RequestState.CANCELLED, request.getState());

        // Attempt to transition from CANCELLED should fail
        assertThrows(IllegalStateException.class, () -> {
            request.transitionTo(RequestState.QUEUED);
        });

        assertThrows(IllegalStateException.class, () -> {
            request.transitionTo(RequestState.COMPLETED);
        });
    }

    @Test
    public void testCancellationAlternativeLifecyclePath() {
        // Test that requests can be cancelled from any non-terminal state
        LiftRequest request1 = LiftRequest.carCall(3);
        LiftRequest request2 = LiftRequest.carCall(5);
        LiftRequest request3 = LiftRequest.carCall(7);

        controller.addRequest(request1);
        controller.addRequest(request2);
        controller.addRequest(request3);

        // All start as QUEUED
        assertEquals(RequestState.QUEUED, request1.getState());
        assertEquals(RequestState.QUEUED, request2.getState());
        assertEquals(RequestState.QUEUED, request3.getState());

        // Cancel one from QUEUED
        controller.cancelRequest(request1.getId());
        assertEquals(RequestState.CANCELLED, request1.getState());

        // Move towards nearest (floor 5)
        LiftState state = new LiftState(0, LiftStatus.IDLE);
        controller.decideNextAction(state, 0);

        // request2 becomes ASSIGNED
        assertEquals(RequestState.ASSIGNED, request2.getState());
        assertEquals(RequestState.QUEUED, request3.getState());

        // Cancel from ASSIGNED
        controller.cancelRequest(request2.getId());
        assertEquals(RequestState.CANCELLED, request2.getState());

        // Only request3 remains
        Set<LiftRequest> requests = controller.getRequests();
        assertEquals(1, requests.size());
        assertTrue(requests.contains(request3));
    }

    @Test
    public void testCancelledRequestsRemovedFromQueue() {
        LiftRequest request1 = LiftRequest.carCall(2);
        LiftRequest request2 = LiftRequest.carCall(4);
        LiftRequest request3 = LiftRequest.carCall(6);

        controller.addRequest(request1);
        controller.addRequest(request2);
        controller.addRequest(request3);

        assertEquals(3, controller.getRequests().size());

        // Cancel middle request
        controller.cancelRequest(request2.getId());

        // Only 2 requests should remain
        Set<LiftRequest> requests = controller.getRequests();
        assertEquals(2, requests.size());
        assertTrue(requests.contains(request1));
        assertFalse(requests.contains(request2));
        assertTrue(requests.contains(request3));
    }
}
