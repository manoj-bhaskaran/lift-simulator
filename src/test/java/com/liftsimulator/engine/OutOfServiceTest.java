package com.liftsimulator.engine;

import com.liftsimulator.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for out-of-service functionality.
 * Verifies that lifts can be taken out of service safely,
 * requests are cancelled, and lifts can return to service.
 */
public class OutOfServiceTest {
    private NaiveLiftController controller;
    private SimulationEngine engine;

    @BeforeEach
    public void setUp() {
        controller = new NaiveLiftController();
        engine = new SimulationEngine(controller, 0, 10);
    }

    @Test
    public void testTakeOutOfServiceCancelsAllRequests() {
        // Add multiple requests
        LiftRequest request1 = LiftRequest.carCall(3);
        LiftRequest request2 = LiftRequest.carCall(5);
        LiftRequest request3 = LiftRequest.hallCall(7, Direction.UP);

        controller.addRequest(request1);
        controller.addRequest(request2);
        controller.addRequest(request3);

        assertEquals(3, controller.getRequests().size());

        // Take lift out of service
        controller.takeOutOfService();

        // All requests should be cancelled and removed
        assertEquals(0, controller.getRequests().size());
        assertEquals(RequestState.CANCELLED, request1.getState());
        assertEquals(RequestState.CANCELLED, request2.getState());
        assertEquals(RequestState.CANCELLED, request3.getState());
    }

    @Test
    public void testTakeOutOfServiceFromIdle() {
        // Lift at floor 2, idle
        engine.tick();
        engine.tick();

        assertEquals(0, engine.getCurrentState().getFloor());
        assertEquals(LiftStatus.IDLE, engine.getCurrentState().getStatus());

        // Take out of service
        controller.takeOutOfService();
        engine.setOutOfService();

        assertEquals(LiftStatus.OUT_OF_SERVICE, engine.getCurrentState().getStatus());
        assertEquals(0, engine.getCurrentState().getFloor());
    }

    @Test
    public void testTakeOutOfServiceFromMoving() {
        // Add request to floor 5
        controller.addCarCall(new CarCall(5));

        // Start moving
        engine.tick();
        assertEquals(LiftStatus.MOVING_UP, engine.getCurrentState().getStatus());

        // Take out of service while moving
        controller.takeOutOfService();
        engine.setOutOfService();

        // Should be out of service
        assertEquals(LiftStatus.OUT_OF_SERVICE, engine.getCurrentState().getStatus());
        assertEquals(0, controller.getRequests().size());
    }

    @Test
    public void testTakeOutOfServiceFromDoorsOpen() {
        // Add request to current floor
        controller.addCarCall(new CarCall(0));

        // Open doors
        engine.tick(); // Start opening
        engine.tick(); // Doors open

        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());

        // Take out of service while doors open
        controller.takeOutOfService();
        engine.setOutOfService();

        assertEquals(LiftStatus.OUT_OF_SERVICE, engine.getCurrentState().getStatus());
        assertEquals(0, controller.getRequests().size());
    }

    @Test
    public void testCannotMoveWhenOutOfService() {
        // Take lift out of service
        controller.takeOutOfService();
        engine.setOutOfService();

        assertEquals(LiftStatus.OUT_OF_SERVICE, engine.getCurrentState().getStatus());

        // Add a request (should not be accepted by controller)
        controller.addCarCall(new CarCall(5));

        // Tick the engine
        engine.tick();

        // Lift should remain out of service and not move
        assertEquals(LiftStatus.OUT_OF_SERVICE, engine.getCurrentState().getStatus());
        assertEquals(0, engine.getCurrentState().getFloor());
    }

    @Test
    public void testCannotOpenDoorsWhenOutOfService() {
        // Take lift out of service
        controller.takeOutOfService();
        engine.setOutOfService();

        assertEquals(LiftStatus.OUT_OF_SERVICE, engine.getCurrentState().getStatus());

        // Try to tick (doors should not open)
        engine.tick();
        engine.tick();
        engine.tick();

        assertEquals(LiftStatus.OUT_OF_SERVICE, engine.getCurrentState().getStatus());
        assertEquals(DoorState.CLOSED, engine.getCurrentState().getDoorState());
    }

    @Test
    public void testReturnToService() {
        // Take lift out of service
        controller.takeOutOfService();
        engine.setOutOfService();

        assertEquals(LiftStatus.OUT_OF_SERVICE, engine.getCurrentState().getStatus());

        // Return to service
        controller.returnToService();
        engine.returnToService();

        assertEquals(LiftStatus.IDLE, engine.getCurrentState().getStatus());
    }

    @Test
    public void testCanAcceptRequestsAfterReturningToService() {
        // Take lift out of service
        controller.takeOutOfService();
        engine.setOutOfService();

        // Return to service
        controller.returnToService();
        engine.returnToService();

        assertEquals(LiftStatus.IDLE, engine.getCurrentState().getStatus());

        // Add a new request
        controller.addCarCall(new CarCall(3));

        // Lift should start moving
        engine.tick();

        assertEquals(LiftStatus.MOVING_UP, engine.getCurrentState().getStatus());
        assertEquals(1, controller.getRequests().size());
    }

    @Test
    public void testSetOutOfServiceThrowsWhenAlreadyOutOfService() {
        // Take lift out of service
        engine.setOutOfService();

        // Try to take out of service again
        assertThrows(IllegalStateException.class, () -> engine.setOutOfService());
    }

    @Test
    public void testReturnToServiceThrowsWhenNotOutOfService() {
        // Lift is in service
        assertEquals(LiftStatus.IDLE, engine.getCurrentState().getStatus());

        // Try to return to service when not out of service
        assertThrows(IllegalStateException.class, () -> engine.returnToService());
    }

    @Test
    public void testOutOfServiceStopsAtNearestFloor() {
        // Add request to floor 5
        controller.addCarCall(new CarCall(5));

        // Move partway there
        engine.tick(); // Floor 1
        engine.tick(); // Floor 2
        engine.tick(); // Floor 3

        assertEquals(3, engine.getCurrentState().getFloor());

        // Take out of service while moving
        controller.takeOutOfService();
        engine.setOutOfService();

        // Should be out of service at floor 3
        assertEquals(LiftStatus.OUT_OF_SERVICE, engine.getCurrentState().getStatus());
        assertEquals(3, engine.getCurrentState().getFloor());
        assertEquals(0, controller.getRequests().size());
    }

    @Test
    public void testOutOfServiceDoorStateIsClosed() {
        // Take lift out of service
        controller.takeOutOfService();
        engine.setOutOfService();

        // Door state should be CLOSED (derived from OUT_OF_SERVICE status)
        assertEquals(DoorState.CLOSED, engine.getCurrentState().getDoorState());
    }

    @Test
    public void testOutOfServiceDirectionIsIdle() {
        // Take lift out of service while moving up
        controller.addCarCall(new CarCall(5));
        engine.tick();
        assertEquals(LiftStatus.MOVING_UP, engine.getCurrentState().getStatus());

        controller.takeOutOfService();
        engine.setOutOfService();

        // Direction should be IDLE (derived from OUT_OF_SERVICE status)
        assertEquals(Direction.IDLE, engine.getCurrentState().getDirection());
    }

    @Test
    public void testIntegrationOutOfServiceScenario() {
        // Add multiple requests
        controller.addCarCall(new CarCall(3));
        controller.addCarCall(new CarCall(5));
        controller.addHallCall(new HallCall(7, Direction.UP));

        // Lift starts servicing floor 3
        engine.tick(); // Floor 1
        engine.tick(); // Floor 2
        engine.tick(); // Floor 3, stop
        engine.tick(); // Open doors

        assertEquals(3, engine.getCurrentState().getFloor());
        assertEquals(LiftStatus.DOORS_OPENING, engine.getCurrentState().getStatus());

        // Emergency: take out of service immediately
        controller.takeOutOfService();
        engine.setOutOfService();

        // All requests cancelled
        assertEquals(0, controller.getRequests().size());

        // Lift is out of service at floor 3
        assertEquals(LiftStatus.OUT_OF_SERVICE, engine.getCurrentState().getStatus());
        assertEquals(3, engine.getCurrentState().getFloor());

        // Tick several times - lift should remain out of service
        engine.tick();
        engine.tick();
        engine.tick();

        assertEquals(LiftStatus.OUT_OF_SERVICE, engine.getCurrentState().getStatus());
        assertEquals(3, engine.getCurrentState().getFloor());

        // Return to service
        controller.returnToService();
        engine.returnToService();

        assertEquals(LiftStatus.IDLE, engine.getCurrentState().getStatus());

        // Add new request and verify it works
        controller.addCarCall(new CarCall(5));
        engine.tick();

        assertEquals(LiftStatus.MOVING_UP, engine.getCurrentState().getStatus());
    }

    @Test
    public void testOutOfServiceFromVariousStates() {
        // Test from DOORS_OPENING
        controller.addCarCall(new CarCall(0));
        engine.tick();
        assertEquals(LiftStatus.DOORS_OPENING, engine.getCurrentState().getStatus());

        controller.takeOutOfService();
        engine.setOutOfService();
        assertEquals(LiftStatus.OUT_OF_SERVICE, engine.getCurrentState().getStatus());

        // Reset
        engine.returnToService();

        // Test from DOORS_CLOSING
        controller.addCarCall(new CarCall(0));
        engine.tick(); // Open
        engine.tick(); // Fully open
        engine.tick(); // Dwell
        engine.tick(); // Start closing

        assertEquals(LiftStatus.DOORS_CLOSING, engine.getCurrentState().getStatus());

        controller.takeOutOfService();
        engine.setOutOfService();
        assertEquals(LiftStatus.OUT_OF_SERVICE, engine.getCurrentState().getStatus());

        // Reset
        engine.returnToService();

        // Test from MOVING_DOWN
        controller.addCarCall(new CarCall(5));
        for (int i = 0; i < 5; i++) engine.tick(); // Move to floor 5

        controller.addCarCall(new CarCall(2));
        engine.tick(); // Stop
        engine.tick(); // Open doors
        engine.tick(); // Fully open
        engine.tick(); // Dwell
        engine.tick(); // Close
        engine.tick(); // Fully closed, start moving down

        assertEquals(LiftStatus.MOVING_DOWN, engine.getCurrentState().getStatus());

        controller.takeOutOfService();
        engine.setOutOfService();
        assertEquals(LiftStatus.OUT_OF_SERVICE, engine.getCurrentState().getStatus());
    }
}
