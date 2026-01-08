package com.liftsimulator.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LiftRequest lifecycle and state management.
 */
class LiftRequestTest {

    @Test
    void hallCallCreatesRequestWithCorrectFields() {
        LiftRequest request = LiftRequest.hallCall(5, Direction.UP);

        assertEquals(RequestType.HALL_CALL, request.getType());
        assertEquals(5, request.getOriginFloor());
        assertNull(request.getDestinationFloor());
        assertEquals(Direction.UP, request.getDirection());
        assertEquals(RequestState.CREATED, request.getState());
        assertEquals(5, request.getTargetFloor());
        assertFalse(request.isTerminal());
    }

    @Test
    void carCallCreatesRequestWithCorrectFields() {
        LiftRequest request = LiftRequest.carCall(10);

        assertEquals(RequestType.CAR_CALL, request.getType());
        assertNull(request.getOriginFloor());
        assertEquals(10, request.getDestinationFloor());
        assertNull(request.getDirection());
        assertEquals(RequestState.CREATED, request.getState());
        assertEquals(10, request.getTargetFloor());
        assertFalse(request.isTerminal());
    }

    @Test
    void carCallWithOriginSetsDirection() {
        LiftRequest upRequest = LiftRequest.carCall(3, 7);
        assertEquals(Direction.UP, upRequest.getDirection());
        assertEquals(3, upRequest.getOriginFloor());
        assertEquals(7, upRequest.getDestinationFloor());

        LiftRequest downRequest = LiftRequest.carCall(8, 2);
        assertEquals(Direction.DOWN, downRequest.getDirection());
        assertEquals(8, downRequest.getOriginFloor());
        assertEquals(2, downRequest.getDestinationFloor());

        LiftRequest sameFloorRequest = LiftRequest.carCall(5, 5);
        assertEquals(Direction.IDLE, sameFloorRequest.getDirection());
        assertEquals(5, sameFloorRequest.getOriginFloor());
        assertEquals(5, sameFloorRequest.getDestinationFloor());
    }

    @Test
    void hallCallRejectsIdleDirection() {
        assertThrows(IllegalArgumentException.class, () ->
            LiftRequest.hallCall(5, Direction.IDLE)
        );
    }

    @Test
    void requestStartsInCreatedState() {
        LiftRequest hallCall = LiftRequest.hallCall(5, Direction.UP);
        LiftRequest carCall = LiftRequest.carCall(10);

        assertEquals(RequestState.CREATED, hallCall.getState());
        assertEquals(RequestState.CREATED, carCall.getState());
    }

    @Test
    void requestProgressesThroughValidLifecycle() {
        LiftRequest request = LiftRequest.hallCall(5, Direction.UP);

        // CREATED -> QUEUED.
        assertEquals(RequestState.CREATED, request.getState());
        request.transitionTo(RequestState.QUEUED);
        assertEquals(RequestState.QUEUED, request.getState());

        // QUEUED -> ASSIGNED.
        request.transitionTo(RequestState.ASSIGNED);
        assertEquals(RequestState.ASSIGNED, request.getState());

        // ASSIGNED -> SERVING.
        request.transitionTo(RequestState.SERVING);
        assertEquals(RequestState.SERVING, request.getState());
        assertFalse(request.isTerminal());

        // SERVING -> COMPLETED.
        request.transitionTo(RequestState.COMPLETED);
        assertEquals(RequestState.COMPLETED, request.getState());
        assertTrue(request.isTerminal());
    }

    @Test
    void requestCanBeCancelledFromNonTerminalStates() {
        // From CREATED.
        LiftRequest request1 = LiftRequest.carCall(5);
        request1.transitionTo(RequestState.CANCELLED);
        assertEquals(RequestState.CANCELLED, request1.getState());
        assertTrue(request1.isTerminal());

        // From QUEUED.
        LiftRequest request2 = LiftRequest.carCall(5);
        request2.transitionTo(RequestState.QUEUED);
        request2.transitionTo(RequestState.CANCELLED);
        assertEquals(RequestState.CANCELLED, request2.getState());
        assertTrue(request2.isTerminal());

        // From ASSIGNED.
        LiftRequest request3 = LiftRequest.carCall(5);
        request3.transitionTo(RequestState.QUEUED);
        request3.transitionTo(RequestState.ASSIGNED);
        request3.transitionTo(RequestState.CANCELLED);
        assertEquals(RequestState.CANCELLED, request3.getState());
        assertTrue(request3.isTerminal());

        // From SERVING.
        LiftRequest request4 = LiftRequest.carCall(5);
        request4.transitionTo(RequestState.QUEUED);
        request4.transitionTo(RequestState.ASSIGNED);
        request4.transitionTo(RequestState.SERVING);
        request4.transitionTo(RequestState.CANCELLED);
        assertEquals(RequestState.CANCELLED, request4.getState());
        assertTrue(request4.isTerminal());
    }

    @Test
    void assignedCanTransitionBackToQueued() {
        LiftRequest request = LiftRequest.hallCall(5, Direction.UP);
        request.transitionTo(RequestState.QUEUED);
        request.transitionTo(RequestState.ASSIGNED);
        request.transitionTo(RequestState.QUEUED);
        assertEquals(RequestState.QUEUED, request.getState());
    }

    @Test
    void invalidTransitionFromCreatedToAssignedThrowsException() {
        LiftRequest request = LiftRequest.carCall(5);
        assertThrows(IllegalStateException.class, () ->
            request.transitionTo(RequestState.ASSIGNED)
        );
    }

    @Test
    void invalidTransitionFromCreatedToServingThrowsException() {
        LiftRequest request = LiftRequest.carCall(5);
        assertThrows(IllegalStateException.class, () ->
            request.transitionTo(RequestState.SERVING)
        );
    }

    @Test
    void invalidTransitionFromCreatedToCompletedThrowsException() {
        LiftRequest request = LiftRequest.carCall(5);
        assertThrows(IllegalStateException.class, () ->
            request.transitionTo(RequestState.COMPLETED)
        );
    }

    @Test
    void invalidTransitionFromQueuedToServingThrowsException() {
        LiftRequest request = LiftRequest.carCall(5);
        request.transitionTo(RequestState.QUEUED);
        assertThrows(IllegalStateException.class, () ->
            request.transitionTo(RequestState.SERVING)
        );
    }

    @Test
    void invalidTransitionFromQueuedToCompletedThrowsException() {
        LiftRequest request = LiftRequest.carCall(5);
        request.transitionTo(RequestState.QUEUED);
        assertThrows(IllegalStateException.class, () ->
            request.transitionTo(RequestState.COMPLETED)
        );
    }

    @Test
    void invalidTransitionFromAssignedToCompletedThrowsException() {
        LiftRequest request = LiftRequest.carCall(5);
        request.transitionTo(RequestState.QUEUED);
        request.transitionTo(RequestState.ASSIGNED);
        assertThrows(IllegalStateException.class, () ->
            request.transitionTo(RequestState.COMPLETED)
        );
    }

    @Test
    void invalidTransitionFromServingToQueuedThrowsException() {
        LiftRequest request = LiftRequest.carCall(5);
        request.transitionTo(RequestState.QUEUED);
        request.transitionTo(RequestState.ASSIGNED);
        request.transitionTo(RequestState.SERVING);
        assertThrows(IllegalStateException.class, () ->
            request.transitionTo(RequestState.QUEUED)
        );
    }

    @Test
    void invalidTransitionFromServingToAssignedThrowsException() {
        LiftRequest request = LiftRequest.carCall(5);
        request.transitionTo(RequestState.QUEUED);
        request.transitionTo(RequestState.ASSIGNED);
        request.transitionTo(RequestState.SERVING);
        assertThrows(IllegalStateException.class, () ->
            request.transitionTo(RequestState.ASSIGNED)
        );
    }

    @Test
    void cannotTransitionFromCompletedState() {
        LiftRequest request = LiftRequest.carCall(5);
        request.transitionTo(RequestState.QUEUED);
        request.transitionTo(RequestState.ASSIGNED);
        request.transitionTo(RequestState.SERVING);
        request.transitionTo(RequestState.COMPLETED);

        assertThrows(IllegalStateException.class, () ->
            request.transitionTo(RequestState.QUEUED)
        );
        assertThrows(IllegalStateException.class, () ->
            request.transitionTo(RequestState.ASSIGNED)
        );
        assertThrows(IllegalStateException.class, () ->
            request.transitionTo(RequestState.SERVING)
        );
        assertThrows(IllegalStateException.class, () ->
            request.transitionTo(RequestState.CANCELLED)
        );
    }

    @Test
    void cannotTransitionFromCancelledState() {
        LiftRequest request = LiftRequest.carCall(5);
        request.transitionTo(RequestState.CANCELLED);

        assertThrows(IllegalStateException.class, () ->
            request.transitionTo(RequestState.QUEUED)
        );
        assertThrows(IllegalStateException.class, () ->
            request.transitionTo(RequestState.ASSIGNED)
        );
        assertThrows(IllegalStateException.class, () ->
            request.transitionTo(RequestState.SERVING)
        );
        assertThrows(IllegalStateException.class, () ->
            request.transitionTo(RequestState.COMPLETED)
        );
    }

    @Test
    void selfTransitionsAreNotAllowed() {
        LiftRequest request = LiftRequest.carCall(5);

        assertThrows(IllegalStateException.class, () ->
            request.transitionTo(RequestState.CREATED)
        );

        request.transitionTo(RequestState.QUEUED);
        assertThrows(IllegalStateException.class, () ->
            request.transitionTo(RequestState.QUEUED)
        );

        request.transitionTo(RequestState.ASSIGNED);
        assertThrows(IllegalStateException.class, () ->
            request.transitionTo(RequestState.ASSIGNED)
        );

        request.transitionTo(RequestState.SERVING);
        assertThrows(IllegalStateException.class, () ->
            request.transitionTo(RequestState.SERVING)
        );
    }

    @Test
    void eachRequestHasUniqueId() {
        LiftRequest request1 = LiftRequest.carCall(5);
        LiftRequest request2 = LiftRequest.carCall(5);
        LiftRequest request3 = LiftRequest.hallCall(3, Direction.UP);

        assertNotEquals(request1.getId(), request2.getId());
        assertNotEquals(request1.getId(), request3.getId());
        assertNotEquals(request2.getId(), request3.getId());
    }

    @Test
    void requestsWithSameIdAreEqual() {
        LiftRequest request1 = LiftRequest.carCall(5);
        LiftRequest request2 = LiftRequest.carCall(5);

        assertNotEquals(request1, request2); // Different IDs.
        assertEquals(request1, request1); // Same instance.
    }

    @Test
    void requestToStringIncludesAllFields() {
        LiftRequest request = LiftRequest.hallCall(5, Direction.UP);
        String toString = request.toString();

        assertTrue(toString.contains("id="));
        assertTrue(toString.contains("type=HALL_CALL"));
        assertTrue(toString.contains("originFloor=5"));
        assertTrue(toString.contains("direction=UP"));
        assertTrue(toString.contains("state=CREATED"));
    }
}
