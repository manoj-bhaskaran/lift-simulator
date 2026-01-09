package com.liftsimulator.engine;

import com.liftsimulator.domain.CarCall;
import com.liftsimulator.domain.ControllerStrategy;
import com.liftsimulator.domain.Direction;
import com.liftsimulator.domain.HallCall;
import com.liftsimulator.domain.LiftRequest;
import com.liftsimulator.domain.LiftState;
import com.liftsimulator.domain.LiftStatus;
import com.liftsimulator.domain.RequestState;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end integration tests for DirectionalScanLiftController.
 * Tests the controller's integration with the simulation lifecycle,
 * verifying proper request handling, door cycles, and state transitions.
 */
public class DirectionalScanIntegrationTest {

    @Test
    void testDirectionalScanControllerEndToEndWithMultipleRequests() {
        // Create controller using factory
        RequestManagingLiftController controller = (RequestManagingLiftController)
                ControllerFactory.createController(ControllerStrategy.DIRECTIONAL_SCAN);

        SimulationEngine engine = SimulationEngine.builder(controller, 0, 10)
                .initialFloor(0)
                .travelTicksPerFloor(1)
                .doorTransitionTicks(2)
                .doorDwellTicks(3)
                .build();

        // Add requests that should be serviced in directional scan order
        LiftRequest req1 = LiftRequest.carCall(3);
        LiftRequest req2 = LiftRequest.hallCall(7, Direction.UP);
        LiftRequest req3 = LiftRequest.carCall(5);

        controller.addRequest(req1);
        controller.addRequest(req2);
        controller.addRequest(req3);

        // Verify requests were added
        assertEquals(3, controller.getRequests().size());

        // Run simulation until all requests are completed
        int maxTicks = 100;
        int tickCount = 0;

        while (tickCount < maxTicks && !allRequestsCompleted(controller)) {
            engine.tick();
            tickCount++;
        }

        // Verify all requests were completed by checking their individual states
        assertEquals(RequestState.COMPLETED, req1.getState(), "Request 1 should be completed");
        assertEquals(RequestState.COMPLETED, req2.getState(), "Request 2 should be completed");
        assertEquals(RequestState.COMPLETED, req3.getState(), "Request 3 should be completed");

        // Verify all requests removed from active set
        assertEquals(0, controller.getRequests().size(), "All requests should be removed from active set");

        // Verify simulation completed in reasonable time
        assertTrue(tickCount < maxTicks, "Simulation should complete within max ticks");
    }

    @Test
    void testDirectionalScanServicesRequestsInDirection() {
        RequestManagingLiftController controller = (RequestManagingLiftController)
                ControllerFactory.createController(ControllerStrategy.DIRECTIONAL_SCAN);

        SimulationEngine engine = SimulationEngine.builder(controller, 0, 10)
                .initialFloor(0)
                .build();

        // Add requests: 2 UP, then 5 UP, then 8 DOWN
        LiftRequest request2Up = LiftRequest.hallCall(2, Direction.UP);
        LiftRequest request5 = LiftRequest.carCall(5);
        LiftRequest request8Down = LiftRequest.hallCall(8, Direction.DOWN);

        controller.addRequest(request2Up);
        controller.addRequest(request5);
        controller.addRequest(request8Down);

        // Run until floor 2 is serviced
        runUntilFloorServiced(engine, 2, 50);
        assertEquals(RequestState.COMPLETED, request2Up.getState(),
                "Request at floor 2 UP should be completed first");

        // Continue until floor 5 is serviced
        runUntilFloorServiced(engine, 5, 50);
        assertEquals(RequestState.COMPLETED, request5.getState(),
                "Car call to floor 5 should be completed while going up");

        // Continue until floor 8 DOWN is serviced
        runUntilRequestCompleted(engine, request8Down, 50);
        assertEquals(RequestState.COMPLETED, request8Down.getState(),
                "Hall call at floor 8 DOWN should be completed after reversal");
    }

    @Test
    void testDirectionalScanAcceptsRequestsDuringMovement() {
        RequestManagingLiftController controller = (RequestManagingLiftController)
                ControllerFactory.createController(ControllerStrategy.DIRECTIONAL_SCAN);

        SimulationEngine engine = SimulationEngine.builder(controller, 0, 10)
                .initialFloor(0)
                .build();

        // Start with a request to floor 8
        controller.addCarCall(new CarCall(8));

        // Run for a few ticks to get the lift moving
        for (int i = 0; i < 5; i++) {
            engine.tick();
        }

        // Add a new request while the lift is moving
        LiftRequest newRequest = LiftRequest.carCall(5);
        controller.addRequest(newRequest);

        // Verify the new request is accepted and queued
        assertEquals(RequestState.QUEUED, newRequest.getState());

        // Continue until the new request is serviced
        runUntilRequestCompleted(engine, newRequest, 100);
        assertEquals(RequestState.COMPLETED, newRequest.getState(),
                "Request added during movement should be completed");
    }

    @Test
    void testDirectionalScanHandlesCancellation() {
        RequestManagingLiftController controller = (RequestManagingLiftController)
                ControllerFactory.createController(ControllerStrategy.DIRECTIONAL_SCAN);

        SimulationEngine engine = SimulationEngine.builder(controller, 0, 10)
                .initialFloor(0)
                .build();

        // Add requests
        LiftRequest request3 = LiftRequest.carCall(3);
        LiftRequest request8 = LiftRequest.carCall(8);

        controller.addRequest(request3);
        controller.addRequest(request8);

        // Run for a few ticks
        for (int i = 0; i < 3; i++) {
            engine.tick();
        }

        // Cancel request to floor 8
        boolean cancelled = controller.cancelRequest(request8.getId());
        assertTrue(cancelled, "Request should be successfully cancelled");
        assertEquals(RequestState.CANCELLED, request8.getState());

        // Continue simulation
        runUntilRequestCompleted(engine, request3, 100);

        // Verify request 3 was completed and request 8 remains cancelled
        assertEquals(RequestState.COMPLETED, request3.getState());
        assertEquals(RequestState.CANCELLED, request8.getState());
    }

    @Test
    void testDirectionalScanWithOutOfServiceAndReturnToService() {
        RequestManagingLiftController controller = (RequestManagingLiftController)
                ControllerFactory.createController(ControllerStrategy.DIRECTIONAL_SCAN);

        SimulationEngine engine = SimulationEngine.builder(controller, 0, 10)
                .initialFloor(0)
                .build();

        // Add some requests and track them
        LiftRequest req1 = LiftRequest.carCall(5);
        LiftRequest req2 = LiftRequest.carCall(7);
        controller.addRequest(req1);
        controller.addRequest(req2);

        // Run a few ticks
        for (int i = 0; i < 5; i++) {
            engine.tick();
        }

        // Take out of service
        controller.takeOutOfService();
        engine.setOutOfService();

        // Run until out of service
        int maxTicks = 50;
        for (int i = 0; i < maxTicks && engine.getCurrentState().getStatus() != LiftStatus.OUT_OF_SERVICE; i++) {
            engine.tick();
        }

        assertEquals(LiftStatus.OUT_OF_SERVICE, engine.getCurrentState().getStatus());

        // All requests should be cancelled by checking their states directly
        assertEquals(RequestState.CANCELLED, req1.getState(), "Request 1 should be cancelled");
        assertEquals(RequestState.CANCELLED, req2.getState(), "Request 2 should be cancelled");

        // Active requests should be empty
        assertEquals(0, controller.getRequests().size(), "All requests should be removed from active set");

        // Return to service
        controller.returnToService();
        engine.returnToService();

        assertEquals(LiftStatus.IDLE, engine.getCurrentState().getStatus());

        // Add new request after returning to service
        LiftRequest newRequest = LiftRequest.carCall(3);
        controller.addRequest(newRequest);

        // Verify new request can be serviced
        runUntilRequestCompleted(engine, newRequest, 100);
        assertEquals(RequestState.COMPLETED, newRequest.getState(),
                "New request should be completed after returning to service");
    }

    @Test
    void testDirectionalScanDefersOppositeDirectionHallCalls() {
        RequestManagingLiftController controller = (RequestManagingLiftController)
                ControllerFactory.createController(ControllerStrategy.DIRECTIONAL_SCAN);

        SimulationEngine engine = SimulationEngine.builder(controller, 0, 10)
                .initialFloor(0)
                .build();

        // Add hall call going UP from floor 2
        LiftRequest hallCallUp = LiftRequest.hallCall(2, Direction.UP);
        controller.addRequest(hallCallUp);

        // Add car call to floor 5
        LiftRequest carCall = LiftRequest.carCall(5);
        controller.addRequest(carCall);

        // Run until both are completed
        runUntilFloorServiced(engine, 2, 50);
        assertEquals(RequestState.COMPLETED, hallCallUp.getState());

        // While at floor 2, add a DOWN hall call at floor 3
        LiftRequest hallCallDown = LiftRequest.hallCall(3, Direction.DOWN);
        controller.addRequest(hallCallDown);

        // The lift should continue UP to floor 5 first
        runUntilFloorServiced(engine, 5, 50);
        assertEquals(RequestState.COMPLETED, carCall.getState());

        // Then it should reverse and service the DOWN hall call at floor 3
        runUntilRequestCompleted(engine, hallCallDown, 50);
        assertEquals(RequestState.COMPLETED, hallCallDown.getState());
    }

    @Test
    void testNoDuplicateServicedRequests() {
        RequestManagingLiftController controller = (RequestManagingLiftController)
                ControllerFactory.createController(ControllerStrategy.DIRECTIONAL_SCAN);

        SimulationEngine engine = SimulationEngine.builder(controller, 0, 10)
                .initialFloor(0)
                .build();

        // Add request
        LiftRequest request = LiftRequest.carCall(3);
        controller.addRequest(request);

        // Track state transitions
        RequestState previousState = request.getState();

        // Run simulation
        for (int i = 0; i < 100 && !request.isTerminal(); i++) {
            engine.tick();

            RequestState currentState = request.getState();

            // Verify state only moves forward
            if (currentState != previousState) {
                assertTrue(currentState.ordinal() > previousState.ordinal() || currentState == RequestState.CANCELLED,
                        "Request state should only move forward");
                previousState = currentState;
            }
        }

        // Verify request completed exactly once
        assertEquals(RequestState.COMPLETED, request.getState());
    }

    @Test
    void testNoLostRequestsDuringComplexScenario() {
        RequestManagingLiftController controller = (RequestManagingLiftController)
                ControllerFactory.createController(ControllerStrategy.DIRECTIONAL_SCAN);

        SimulationEngine engine = SimulationEngine.builder(controller, 0, 10)
                .initialFloor(5)
                .build();

        // Add complex set of requests
        LiftRequest req1 = LiftRequest.carCall(3);
        LiftRequest req2 = LiftRequest.hallCall(7, Direction.UP);
        LiftRequest req3 = LiftRequest.carCall(1);
        LiftRequest req4 = LiftRequest.hallCall(9, Direction.DOWN);

        controller.addRequest(req1);
        controller.addRequest(req2);

        // Run a bit
        for (int i = 0; i < 10; i++) {
            engine.tick();
        }

        // Add more requests mid-simulation
        controller.addRequest(req3);
        controller.addRequest(req4);

        // Run until all completed
        runUntilAllCompleted(engine, controller, 200);

        // Verify all requests completed
        assertEquals(RequestState.COMPLETED, req1.getState(), "Request 1 should be completed");
        assertEquals(RequestState.COMPLETED, req2.getState(), "Request 2 should be completed");
        assertEquals(RequestState.COMPLETED, req3.getState(), "Request 3 should be completed");
        assertEquals(RequestState.COMPLETED, req4.getState(), "Request 4 should be completed");
    }

    @Test
    void testControllerFactoryCreatesDirectionalScanController() {
        LiftController controller = ControllerFactory.createController(ControllerStrategy.DIRECTIONAL_SCAN);

        assertNotNull(controller);
        assertTrue(controller instanceof DirectionalScanLiftController,
                "Factory should create DirectionalScanLiftController for DIRECTIONAL_SCAN strategy");
    }

    // Helper methods

    private boolean allRequestsCompleted(RequestManagingLiftController controller) {
        Set<LiftRequest> requests = controller.getRequests();
        return requests.stream()
                .filter(r -> r.getState() != RequestState.CANCELLED)
                .allMatch(r -> r.getState() == RequestState.COMPLETED);
    }

    private void runUntilFloorServiced(SimulationEngine engine, int targetFloor, int maxTicks) {
        for (int i = 0; i < maxTicks; i++) {
            LiftState state = engine.getCurrentState();
            if (state.getFloor() == targetFloor && state.getStatus() == LiftStatus.DOORS_OPEN) {
                return;
            }
            engine.tick();
        }
    }

    private void runUntilRequestCompleted(SimulationEngine engine, LiftRequest request, int maxTicks) {
        for (int i = 0; i < maxTicks; i++) {
            if (request.getState() == RequestState.COMPLETED) {
                return;
            }
            engine.tick();
        }
    }

    private void runUntilAllCompleted(SimulationEngine engine, RequestManagingLiftController controller, int maxTicks) {
        for (int i = 0; i < maxTicks; i++) {
            if (allRequestsCompleted(controller)) {
                return;
            }
            engine.tick();
        }
    }
}
