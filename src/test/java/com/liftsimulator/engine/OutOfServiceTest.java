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
        // Lift starts at floor 0, idle
        assertEquals(0, engine.getCurrentState().getFloor());
        assertEquals(LiftStatus.IDLE, engine.getCurrentState().getStatus());

        // Take out of service - should initiate graceful shutdown
        controller.takeOutOfService();
        engine.setOutOfService();

        // Still IDLE, pending sequence hasn't started yet
        assertEquals(LiftStatus.IDLE, engine.getCurrentState().getStatus());

        // Tick 1: Start opening doors
        engine.tick();
        assertEquals(LiftStatus.DOORS_OPENING, engine.getCurrentState().getStatus());

        // Tick 2: Doors fully open
        engine.tick();
        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());

        // Tick 3: Dwell time
        engine.tick();
        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());

        // Tick 4: Start closing doors
        engine.tick();
        assertEquals(LiftStatus.DOORS_CLOSING, engine.getCurrentState().getStatus());

        // Tick 5: Doors fully closed, transition to OUT_OF_SERVICE
        engine.tick();
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
        assertEquals(1, engine.getCurrentState().getFloor());

        // Take out of service while moving
        controller.takeOutOfService();
        engine.setOutOfService();
        assertEquals(0, controller.getRequests().size());

        // Should still be moving, completing to next floor
        assertEquals(LiftStatus.MOVING_UP, engine.getCurrentState().getStatus());

        // Tick: Complete movement to floor 2
        engine.tick();
        assertEquals(2, engine.getCurrentState().getFloor());
        assertEquals(LiftStatus.MOVING_UP, engine.getCurrentState().getStatus());

        // Tick: Now stopped at floor 2, start opening doors
        engine.tick();
        assertEquals(LiftStatus.DOORS_OPENING, engine.getCurrentState().getStatus());
        assertEquals(2, engine.getCurrentState().getFloor());

        // Tick: Doors fully open
        engine.tick();
        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());

        // Tick: Dwell
        engine.tick();
        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());

        // Tick: Start closing
        engine.tick();
        assertEquals(LiftStatus.DOORS_CLOSING, engine.getCurrentState().getStatus());

        // Tick: Doors closed, now OUT_OF_SERVICE
        engine.tick();
        assertEquals(LiftStatus.OUT_OF_SERVICE, engine.getCurrentState().getStatus());
        assertEquals(2, engine.getCurrentState().getFloor());
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
        assertEquals(0, controller.getRequests().size());

        // Still doors open during dwell
        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());

        // Tick: Dwell time passes, start closing
        engine.tick();
        assertEquals(LiftStatus.DOORS_CLOSING, engine.getCurrentState().getStatus());

        // Tick: Doors closed, transition to OUT_OF_SERVICE
        engine.tick();
        assertEquals(LiftStatus.OUT_OF_SERVICE, engine.getCurrentState().getStatus());
        assertEquals(0, engine.getCurrentState().getFloor());
    }

    @Test
    public void testCannotMoveWhenOutOfService() {
        // Take lift out of service and complete graceful shutdown
        controller.takeOutOfService();
        engine.setOutOfService();

        // Go through graceful shutdown sequence
        for (int i = 0; i < 10; i++) {
            engine.tick();
            if (engine.getCurrentState().getStatus() == LiftStatus.OUT_OF_SERVICE) {
                break;
            }
        }

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
        // Take lift out of service and complete graceful shutdown
        controller.takeOutOfService();
        engine.setOutOfService();

        // Go through graceful shutdown sequence
        for (int i = 0; i < 10; i++) {
            engine.tick();
            if (engine.getCurrentState().getStatus() == LiftStatus.OUT_OF_SERVICE) {
                break;
            }
        }

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
        // Take lift out of service and complete graceful shutdown
        controller.takeOutOfService();
        engine.setOutOfService();

        // Go through graceful shutdown sequence
        for (int i = 0; i < 10; i++) {
            engine.tick();
            if (engine.getCurrentState().getStatus() == LiftStatus.OUT_OF_SERVICE) {
                break;
            }
        }

        assertEquals(LiftStatus.OUT_OF_SERVICE, engine.getCurrentState().getStatus());

        // Return to service
        controller.returnToService();
        engine.returnToService();

        assertEquals(LiftStatus.IDLE, engine.getCurrentState().getStatus());
    }

    @Test
    public void testCanAcceptRequestsAfterReturningToService() {
        // Take lift out of service and complete graceful shutdown
        controller.takeOutOfService();
        engine.setOutOfService();

        // Go through graceful shutdown sequence
        for (int i = 0; i < 10; i++) {
            engine.tick();
            if (engine.getCurrentState().getStatus() == LiftStatus.OUT_OF_SERVICE) {
                break;
            }
        }

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
        // Take lift out of service and complete graceful shutdown
        engine.setOutOfService();

        // Go through graceful shutdown sequence
        for (int i = 0; i < 10; i++) {
            engine.tick();
            if (engine.getCurrentState().getStatus() == LiftStatus.OUT_OF_SERVICE) {
                break;
            }
        }

        // Try to take out of service again
        assertThrows(IllegalStateException.class, () -> engine.setOutOfService());
    }

    @Test
    public void testSetOutOfServiceThrowsWhenAlreadyPending() {
        // Take lift out of service
        engine.setOutOfService();

        // Try to take out of service again while pending
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
        assertEquals(LiftStatus.MOVING_UP, engine.getCurrentState().getStatus());

        // Take out of service while moving between floor 3 and 4
        controller.takeOutOfService();
        engine.setOutOfService();
        assertEquals(0, controller.getRequests().size());

        // Should complete movement to floor 4
        engine.tick();
        assertEquals(4, engine.getCurrentState().getFloor());

        // Go through door open/close sequence
        for (int i = 0; i < 10; i++) {
            engine.tick();
            if (engine.getCurrentState().getStatus() == LiftStatus.OUT_OF_SERVICE) {
                break;
            }
        }

        // Should be out of service at floor 4 (the next floor it reached)
        assertEquals(LiftStatus.OUT_OF_SERVICE, engine.getCurrentState().getStatus());
        assertEquals(4, engine.getCurrentState().getFloor());
    }

    @Test
    public void testOutOfServiceDoorStateIsClosed() {
        // Take lift out of service and complete graceful shutdown
        controller.takeOutOfService();
        engine.setOutOfService();

        // Go through graceful shutdown sequence
        for (int i = 0; i < 10; i++) {
            engine.tick();
            if (engine.getCurrentState().getStatus() == LiftStatus.OUT_OF_SERVICE) {
                break;
            }
        }

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

        // Go through graceful shutdown sequence
        for (int i = 0; i < 10; i++) {
            engine.tick();
            if (engine.getCurrentState().getStatus() == LiftStatus.OUT_OF_SERVICE) {
                break;
            }
        }

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
        assertEquals(3, engine.getCurrentState().getFloor());
        assertEquals(LiftStatus.IDLE, engine.getCurrentState().getStatus());

        engine.tick(); // Open doors
        assertEquals(LiftStatus.DOORS_OPENING, engine.getCurrentState().getStatus());

        // Emergency: take out of service immediately
        controller.takeOutOfService();
        engine.setOutOfService();

        // All requests cancelled
        assertEquals(0, controller.getRequests().size());

        // Complete door opening sequence
        for (int i = 0; i < 10; i++) {
            engine.tick();
            if (engine.getCurrentState().getStatus() == LiftStatus.OUT_OF_SERVICE) {
                break;
            }
        }

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
    public void testGracefulShutdownSequence() {
        // Verify the complete graceful shutdown sequence
        controller.addCarCall(new CarCall(5));

        // Move partway there
        engine.tick(); // Floor 1
        engine.tick(); // Floor 2 (moving)

        assertEquals(2, engine.getCurrentState().getFloor());
        assertEquals(LiftStatus.MOVING_UP, engine.getCurrentState().getStatus());

        // Initiate out-of-service while moving
        controller.takeOutOfService();
        engine.setOutOfService();

        // Step 1: Complete movement to next floor (floor 3)
        engine.tick();
        assertEquals(3, engine.getCurrentState().getFloor());
        assertEquals(LiftStatus.MOVING_UP, engine.getCurrentState().getStatus());

        // Step 2: Stop at floor 3, start opening doors
        engine.tick();
        assertEquals(LiftStatus.DOORS_OPENING, engine.getCurrentState().getStatus());

        // Step 3: Doors fully open
        engine.tick();
        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());

        // Step 4: Dwell time
        engine.tick();
        assertEquals(LiftStatus.DOORS_OPEN, engine.getCurrentState().getStatus());

        // Step 5: Start closing doors
        engine.tick();
        assertEquals(LiftStatus.DOORS_CLOSING, engine.getCurrentState().getStatus());

        // Step 6: Doors closed, transition to OUT_OF_SERVICE
        engine.tick();
        assertEquals(LiftStatus.OUT_OF_SERVICE, engine.getCurrentState().getStatus());
        assertEquals(3, engine.getCurrentState().getFloor());
    }
}
