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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Scenario-based tests for both controller strategies.
 * These tests validate scheduling outcomes, service order, direction transitions,
 * and queue management for realistic multi-request scenarios.
 */
public class ControllerScenarioTest {

    /**
     * Test harness for running deterministic scenario tests.
     * Supports tick simulation, request injection at specific ticks, and assertion utilities.
     */
    private static class ScenarioHarness {
        private final RequestManagingLiftController controller;
        private final SimulationEngine engine;
        private final List<ServiceEvent> serviceLog = new ArrayList<>();
        private int currentTick = 0;
        private Direction lastMovingDirection = Direction.IDLE;

        ScenarioHarness(ControllerStrategy strategy, int initialFloor, int minFloor, int maxFloor) {
            this.controller = (RequestManagingLiftController)
                    ControllerFactory.createController(strategy);
            this.engine = SimulationEngine.builder(controller, minFloor, maxFloor)
                    .initialFloor(initialFloor)
                    .travelTicksPerFloor(1)
                    .doorTransitionTicks(2)
                    .doorDwellTicks(3)
                    .build();
        }

        /**
         * Runs simulation until specified tick or until idle with no pending requests.
         */
        void runUntilTick(int targetTick) {
            while (currentTick < targetTick) {
                tick();
            }
        }

        /**
         * Runs simulation until all requests are completed or max ticks reached.
         */
        void runUntilComplete(int maxTicks) {
            int tickCount = 0;
            while (tickCount < maxTicks && hasPendingRequests()) {
                tick();
                tickCount++;
            }
            if (tickCount >= maxTicks && hasPendingRequests()) {
                throw new AssertionError("Simulation did not complete within " + maxTicks + " ticks");
            }
        }

        /**
         * Executes a single simulation tick and logs service events.
         */
        void tick() {
            LiftState stateBefore = engine.getCurrentState();

            // Track the last moving direction
            Direction currentDirection = stateBefore.getDirection();
            if (currentDirection != Direction.IDLE) {
                lastMovingDirection = currentDirection;
            }

            engine.tick();
            LiftState stateAfter = engine.getCurrentState();
            currentTick++;

            // Log when doors open (service event)
            // Use the last moving direction to determine the direction of service
            if (stateBefore.getStatus() != LiftStatus.DOORS_OPEN
                    && stateAfter.getStatus() == LiftStatus.DOORS_OPEN) {
                serviceLog.add(new ServiceEvent(currentTick, stateAfter.getFloor(), lastMovingDirection));
            }
        }

        /**
         * Adds a car call request.
         */
        LiftRequest addCarCall(int destinationFloor) {
            LiftRequest request = LiftRequest.carCall(destinationFloor);
            controller.addRequest(request);
            return request;
        }

        /**
         * Adds a hall call request.
         */
        LiftRequest addHallCall(int floor, Direction direction) {
            LiftRequest request = LiftRequest.hallCall(floor, direction);
            controller.addRequest(request);
            return request;
        }

        /**
         * Returns true if there are pending (non-terminal) requests.
         */
        boolean hasPendingRequests() {
            return controller.getRequests().stream()
                    .anyMatch(r -> !r.isTerminal());
        }

        /**
         * Gets the current simulation tick.
         */
        int getCurrentTick() {
            return currentTick;
        }

        /**
         * Gets the current lift state.
         */
        LiftState getCurrentState() {
            return engine.getCurrentState();
        }

        /**
         * Gets the service log (floors serviced in order).
         */
        List<ServiceEvent> getServiceLog() {
            return new ArrayList<>(serviceLog);
        }

        /**
         * Asserts that floors were serviced in the expected order.
         */
        void assertServiceOrder(List<Integer> expectedFloors) {
            List<Integer> actualFloors = serviceLog.stream()
                    .map(e -> e.floor)
                    .collect(Collectors.toList());
            assertEquals(expectedFloors, actualFloors,
                    "Service order mismatch. Expected: " + expectedFloors + ", Actual: " + actualFloors);
        }

        /**
         * Asserts that specific floor was serviced with specific direction.
         */
        void assertServiceWithDirection(int floor, Direction expectedDirection) {
            ServiceEvent event = serviceLog.stream()
                    .filter(e -> e.floor == floor)
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Floor " + floor + " was never serviced"));

            assertEquals(expectedDirection, event.direction,
                    "Floor " + floor + " serviced with wrong direction. Expected: "
                    + expectedDirection + ", Actual: " + event.direction);
        }

        /**
         * Asserts that all requests are completed and queue is empty.
         */
        void assertQueueCleared() {
            List<LiftRequest> pending = controller.getRequests().stream()
                    .filter(r -> !r.isTerminal())
                    .collect(Collectors.toList());

            assertTrue(pending.isEmpty(),
                    "Expected empty queue but found pending requests: " + pending);

            assertEquals(0, controller.getRequests().size(),
                    "Expected no active requests");
        }

        /**
         * Asserts that a specific request is in the expected state.
         */
        void assertRequestState(LiftRequest request, RequestState expectedState) {
            assertEquals(expectedState, request.getState(),
                    "Request " + request.getId() + " state mismatch");
        }

        /**
         * Records a service event (when doors open at a floor).
         */
        private record ServiceEvent(int tick, int floor, Direction direction) {
            @Override
            public String toString() {
                return "Tick " + tick + ": Floor " + floor + " (" + direction + ")";
            }
        }
    }

    // ========================================
    // DirectionalScan Strategy Tests
    // ========================================

    @Test
    void testDirectionalScan_CanonicalScenario_FromReadme() {
        // Canonical example from README:
        // Lift at floor 0, requests:
        // - Hall call: floor 2, UP
        // - Car call: floor 5
        // - Hall call: floor 3, DOWN
        //
        // Expected execution:
        // 1. Select UP direction (floor 2 is nearest)
        // 2. Service floor 2 (hall call UP) ✓
        // 3. Continue to floor 5 (car call) ✓
        // 4. No more requests going UP, reverse to DOWN
        // 5. Service floor 3 (hall call DOWN) ✓

        ScenarioHarness harness = new ScenarioHarness(
                ControllerStrategy.DIRECTIONAL_SCAN, 0, 0, 10);

        LiftRequest req1 = harness.addHallCall(2, Direction.UP);
        LiftRequest req2 = harness.addCarCall(5);
        LiftRequest req3 = harness.addHallCall(3, Direction.DOWN);

        harness.runUntilComplete(100);

        // Assert all requests completed
        harness.assertRequestState(req1, RequestState.COMPLETED);
        harness.assertRequestState(req2, RequestState.COMPLETED);
        harness.assertRequestState(req3, RequestState.COMPLETED);

        // Assert service order: 2, 5, 3
        harness.assertServiceOrder(List.of(2, 5, 3));

        // Assert floor 3 was serviced with DOWN direction (after reversal)
        harness.assertServiceWithDirection(3, Direction.DOWN);

        // Assert queue is cleared
        harness.assertQueueCleared();
    }

    @Test
    void testDirectionalScan_MixedCallsAboveAndBelowWhileMoving() {
        // Scenario: Lift starts at floor 5, moving to floor 8
        // While moving, requests arrive:
        // - Floor 6 UP (ahead in current direction)
        // - Floor 7 UP (ahead in current direction)
        // - Floor 3 DOWN (below, opposite direction)
        // - Floor 2 UP (below, but wants to go up)
        //
        // Expected behavior:
        // 1. Continue UP to service 6, 7, 8
        // 2. Reverse to DOWN
        // 3. Service floor 3 DOWN
        // 4. Cannot service floor 2 UP while going DOWN (deferred)
        // 5. Reverse to UP
        // 6. Service floor 2 UP

        ScenarioHarness harness = new ScenarioHarness(
                ControllerStrategy.DIRECTIONAL_SCAN, 5, 0, 10);

        // Initial car call to get lift moving up
        LiftRequest initialCall = harness.addCarCall(8);

        // Run until lift starts moving
        harness.runUntilTick(5);

        // Now add requests while lift is moving up
        LiftRequest req6Up = harness.addHallCall(6, Direction.UP);
        LiftRequest req7Up = harness.addHallCall(7, Direction.UP);
        LiftRequest req3Down = harness.addHallCall(3, Direction.DOWN);
        LiftRequest req2Up = harness.addHallCall(2, Direction.UP);

        // Complete the scenario
        harness.runUntilComplete(200);

        // Assert all completed
        harness.assertRequestState(initialCall, RequestState.COMPLETED);
        harness.assertRequestState(req6Up, RequestState.COMPLETED);
        harness.assertRequestState(req7Up, RequestState.COMPLETED);
        harness.assertRequestState(req3Down, RequestState.COMPLETED);
        harness.assertRequestState(req2Up, RequestState.COMPLETED);

        // Assert service order: upward sweep (6, 7, 8), then down (3), then up again (2)
        harness.assertServiceOrder(List.of(6, 7, 8, 3, 2));

        harness.assertQueueCleared();
    }

    @Test
    void testDirectionalScan_IdleChooseDirectionCommitClearReverse() {
        // Scenario: Lift idle at floor 5
        // Requests:
        // - Floor 8 UP (hall call)
        // - Floor 9 car call
        // - Floor 7 DOWN (hall call)
        // - Floor 3 DOWN (hall call)
        // - Floor 2 car call
        //
        // Expected behavior:
        // 1. Idle at floor 5
        // 2. Choose UP direction (floor 7 is closer than floor 3)
        // 3. Commit to UP: service 7 UP (deferred), 8 UP, 9 car
        // 4. Clear upward requests
        // 5. Reverse to DOWN
        // 6. Service 7 DOWN, 3 DOWN, 2 car

        ScenarioHarness harness = new ScenarioHarness(
                ControllerStrategy.DIRECTIONAL_SCAN, 5, 0, 10);

        // All requests added while idle
        LiftRequest req8Up = harness.addHallCall(8, Direction.UP);
        LiftRequest req9Car = harness.addCarCall(9);
        LiftRequest req7Down = harness.addHallCall(7, Direction.DOWN);
        LiftRequest req3Down = harness.addHallCall(3, Direction.DOWN);
        LiftRequest req2Car = harness.addCarCall(2);

        // Verify initially idle
        assertEquals(LiftStatus.IDLE, harness.getCurrentState().getStatus());

        // Run simulation
        harness.runUntilComplete(300);

        // All requests completed
        harness.assertRequestState(req8Up, RequestState.COMPLETED);
        harness.assertRequestState(req9Car, RequestState.COMPLETED);
        harness.assertRequestState(req7Down, RequestState.COMPLETED);
        harness.assertRequestState(req3Down, RequestState.COMPLETED);
        harness.assertRequestState(req2Car, RequestState.COMPLETED);

        // Service order: UP sweep (8, 9), then DOWN sweep (7, 3, 2)
        harness.assertServiceOrder(List.of(8, 9, 7, 3, 2));

        // Floor 7 serviced with DOWN direction (after reversal)
        harness.assertServiceWithDirection(7, Direction.DOWN);

        harness.assertQueueCleared();
    }

    @Test
    void testDirectionalScan_SingleDirectionMultipleStops() {
        // Scenario: All requests in one direction
        // Lift at floor 0, all requests going UP
        // Floors: 2 UP, 4 UP, 6 UP, 8 UP

        ScenarioHarness harness = new ScenarioHarness(
                ControllerStrategy.DIRECTIONAL_SCAN, 0, 0, 10);

        LiftRequest req2 = harness.addHallCall(2, Direction.UP);
        LiftRequest req4 = harness.addHallCall(4, Direction.UP);
        LiftRequest req6 = harness.addHallCall(6, Direction.UP);
        LiftRequest req8 = harness.addHallCall(8, Direction.UP);

        harness.runUntilComplete(150);

        // All completed in order
        harness.assertRequestState(req2, RequestState.COMPLETED);
        harness.assertRequestState(req4, RequestState.COMPLETED);
        harness.assertRequestState(req6, RequestState.COMPLETED);
        harness.assertRequestState(req8, RequestState.COMPLETED);

        harness.assertServiceOrder(List.of(2, 4, 6, 8));
        harness.assertQueueCleared();
    }

    @Test
    void testDirectionalScan_AlternatingDirections() {
        // Scenario: Requests that cause multiple direction changes
        // Lift at floor 5
        // Requests: 8 UP, 2 DOWN, 9 car call, 1 car call
        //
        // Expected: 5 -> 8 (UP) -> 9 (car) -> 2 (DOWN) -> 1 (car)

        ScenarioHarness harness = new ScenarioHarness(
                ControllerStrategy.DIRECTIONAL_SCAN, 5, 0, 10);

        LiftRequest req8Up = harness.addHallCall(8, Direction.UP);
        LiftRequest req2Down = harness.addHallCall(2, Direction.DOWN);
        LiftRequest req9Car = harness.addCarCall(9);
        LiftRequest req1Car = harness.addCarCall(1);

        harness.runUntilComplete(250);

        harness.assertRequestState(req8Up, RequestState.COMPLETED);
        harness.assertRequestState(req2Down, RequestState.COMPLETED);
        harness.assertRequestState(req9Car, RequestState.COMPLETED);
        harness.assertRequestState(req1Car, RequestState.COMPLETED);

        harness.assertServiceOrder(List.of(8, 9, 2, 1));
        harness.assertQueueCleared();
    }

    // ========================================
    // NaiveLift Strategy Tests
    // ========================================

    @Test
    void testNaiveLift_NearestFirst_ProtectCurrentBehavior() {
        // Naive controller should service nearest request first,
        // regardless of direction.
        // Lift at floor 5, requests: floor 3, floor 7
        // Expected: 5 -> 3 -> 7 (nearest first)

        ScenarioHarness harness = new ScenarioHarness(
                ControllerStrategy.NAIVE, 5, 0, 10);

        LiftRequest req3 = harness.addCarCall(3);
        LiftRequest req7 = harness.addCarCall(7);

        harness.runUntilComplete(150);

        harness.assertRequestState(req3, RequestState.COMPLETED);
        harness.assertRequestState(req7, RequestState.COMPLETED);

        // Should service floor 3 first (distance 2) before floor 7 (distance 2)
        // Actually, both are equidistant, so order depends on insertion/tie-breaking
        harness.assertServiceOrder(List.of(3, 7));
        harness.assertQueueCleared();
    }

    @Test
    void testNaiveLift_BackAndForthMovement() {
        // Scenario demonstrating inefficiency of naive routing
        // Lift at floor 0
        // Requests: floor 3 DOWN, floor 5 UP, floor 7 car
        //
        // Naive should go: 0 -> 3 -> 5 -> 7
        // (not optimized for direction)

        ScenarioHarness harness = new ScenarioHarness(
                ControllerStrategy.NAIVE, 0, 0, 10);

        LiftRequest req3Down = harness.addHallCall(3, Direction.DOWN);
        LiftRequest req5Up = harness.addHallCall(5, Direction.UP);
        LiftRequest req7Car = harness.addCarCall(7);

        harness.runUntilComplete(200);

        harness.assertRequestState(req3Down, RequestState.COMPLETED);
        harness.assertRequestState(req5Up, RequestState.COMPLETED);
        harness.assertRequestState(req7Car, RequestState.COMPLETED);

        // Naive goes to nearest each time: 3, 5, 7
        harness.assertServiceOrder(List.of(3, 5, 7));
        harness.assertQueueCleared();
    }

    @Test
    void testNaiveLift_MixedCallTypes() {
        // Test naive controller with both hall and car calls
        // Lift at floor 5
        // Requests: hall 2 UP, car 8, hall 6 DOWN, car 4

        ScenarioHarness harness = new ScenarioHarness(
                ControllerStrategy.NAIVE, 5, 0, 10);

        LiftRequest req2Up = harness.addHallCall(2, Direction.UP);
        LiftRequest req8Car = harness.addCarCall(8);
        LiftRequest req6Down = harness.addHallCall(6, Direction.DOWN);
        LiftRequest req4Car = harness.addCarCall(4);

        harness.runUntilComplete(250);

        harness.assertRequestState(req2Up, RequestState.COMPLETED);
        harness.assertRequestState(req8Car, RequestState.COMPLETED);
        harness.assertRequestState(req6Down, RequestState.COMPLETED);
        harness.assertRequestState(req4Car, RequestState.COMPLETED);

        // Naive services in nearest-first order from floor 5:
        // Distance to 2: 3, to 8: 3, to 6: 1, to 4: 1
        // So order likely: 6, 4, 2, 8 (or 4, 6, 2, 8 depending on tie-breaking)
        List<Integer> serviceOrder = harness.getServiceLog().stream()
                .map(e -> e.floor)
                .collect(Collectors.toList());

        // Verify all 4 floors were serviced
        assertEquals(4, serviceOrder.size());
        assertTrue(serviceOrder.contains(2));
        assertTrue(serviceOrder.contains(4));
        assertTrue(serviceOrder.contains(6));
        assertTrue(serviceOrder.contains(8));

        harness.assertQueueCleared();
    }

    @Test
    void testNaiveLift_DynamicRequestAddition() {
        // Test that naive controller handles requests added during movement
        // Start with request to floor 8, then add floor 5 while moving

        ScenarioHarness harness = new ScenarioHarness(
                ControllerStrategy.NAIVE, 0, 0, 10);

        LiftRequest req8 = harness.addCarCall(8);

        // Run a few ticks to get moving
        harness.runUntilTick(10);

        // Add new request closer than current target
        LiftRequest req5 = harness.addCarCall(5);

        harness.runUntilComplete(200);

        harness.assertRequestState(req8, RequestState.COMPLETED);
        harness.assertRequestState(req5, RequestState.COMPLETED);

        // Should service both floors
        List<Integer> serviceOrder = harness.getServiceLog().stream()
                .map(e -> e.floor)
                .collect(Collectors.toList());
        assertTrue(serviceOrder.contains(5));
        assertTrue(serviceOrder.contains(8));

        harness.assertQueueCleared();
    }

    @Test
    void testNaiveLift_SingleRequest() {
        // Simple baseline test: single request
        // Lift at floor 0, request to floor 5

        ScenarioHarness harness = new ScenarioHarness(
                ControllerStrategy.NAIVE, 0, 0, 10);

        LiftRequest req = harness.addCarCall(5);

        harness.runUntilComplete(100);

        harness.assertRequestState(req, RequestState.COMPLETED);
        harness.assertServiceOrder(List.of(5));
        harness.assertQueueCleared();
    }

    // ========================================
    // Comparison Tests
    // ========================================

    @Test
    void testComparison_SameScenarioDifferentStrategies() {
        // Run the same scenario with both strategies to highlight differences
        // Scenario: Lift at 0, requests: 3 DOWN, 5 UP, 7 car

        // DirectionalScan
        ScenarioHarness directional = new ScenarioHarness(
                ControllerStrategy.DIRECTIONAL_SCAN, 0, 0, 10);
        LiftRequest d1 = directional.addHallCall(3, Direction.DOWN);
        LiftRequest d2 = directional.addHallCall(5, Direction.UP);
        LiftRequest d3 = directional.addCarCall(7);
        directional.runUntilComplete(200);

        // Naive
        ScenarioHarness naive = new ScenarioHarness(
                ControllerStrategy.NAIVE, 0, 0, 10);
        LiftRequest n1 = naive.addHallCall(3, Direction.DOWN);
        LiftRequest n2 = naive.addHallCall(5, Direction.UP);
        LiftRequest n3 = naive.addCarCall(7);
        naive.runUntilComplete(200);

        // Both should complete all requests
        directional.assertRequestState(d1, RequestState.COMPLETED);
        directional.assertRequestState(d2, RequestState.COMPLETED);
        directional.assertRequestState(d3, RequestState.COMPLETED);

        naive.assertRequestState(n1, RequestState.COMPLETED);
        naive.assertRequestState(n2, RequestState.COMPLETED);
        naive.assertRequestState(n3, RequestState.COMPLETED);

        // DirectionalScan: goes UP first (5, 7) then DOWN (3)
        directional.assertServiceOrder(List.of(5, 7, 3));

        // Naive: nearest first (3, 5, 7)
        naive.assertServiceOrder(List.of(3, 5, 7));

        // Both should clear queues
        directional.assertQueueCleared();
        naive.assertQueueCleared();
    }
}
