package com.liftsimulator.admin.service;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;
import com.liftsimulator.admin.dto.LiftConfigDTO;
import com.liftsimulator.admin.dto.PassengerFlowDTO;
import com.liftsimulator.admin.service.metrics.RunMetrics;
import com.liftsimulator.domain.ControllerStrategy;
import com.liftsimulator.domain.Direction;
import com.liftsimulator.domain.IdleParkingMode;
import com.liftsimulator.domain.LiftRequest;
import com.liftsimulator.domain.LiftState;
import com.liftsimulator.domain.LiftStatus;
import com.liftsimulator.domain.RequestState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RunMetricsTest {

    private RunMetrics metrics;
    private final ObjectMapper objectMapper = tools.jackson.databind.json.JsonMapper.builder().build();

    @BeforeEach
    void setUp() {
        metrics = new RunMetrics(1, 5);
    }

    @Test
    void initialTotalTicksIsZero() {
        assertEquals(0L, metrics.totalTicks());
    }

    @Test
    void recordLiftStateIncrementsTotalTicks() {
        metrics.recordLiftState(new LiftState(1, LiftStatus.IDLE));
        metrics.recordLiftState(new LiftState(2, LiftStatus.MOVING_UP));
        assertEquals(2L, metrics.totalTicks());
    }

    @Test
    void toKpisNodeZeroRequestsProducesZeroKpis() {
        metrics.recordLiftState(new LiftState(1, LiftStatus.IDLE));
        ObjectNode kpis = metrics.toKpisNode(objectMapper);

        assertEquals(0, kpis.get("requestsTotal").asInt());
        assertEquals(0, kpis.get("pickupRequestsServed").asInt());
        assertEquals(0, kpis.get("passengersServed").asInt());
        assertEquals(0.0, kpis.get("avgPickupWaitTicks").asDouble(), 1e-9);
        assertEquals(0L, kpis.get("maxPickupWaitTicks").asLong());
    }

    @Test
    void toKpisNodeCountsCompletedRequests() {
        LiftRequest req = LiftRequest.hallCall(1, Direction.UP);
        metrics.recordRequestCreation(req, 0L);
        req.completeRequest();
        metrics.recordTerminalRequests(10L);

        ObjectNode kpis = metrics.toKpisNode(objectMapper);
        assertEquals(1, kpis.get("pickupRequestsServed").asInt());
        assertEquals(1, kpis.get("passengersServed").asInt());
        assertEquals(10L, kpis.get("maxPickupWaitTicks").asLong());
        assertEquals(10.0, kpis.get("avgPickupWaitTicks").asDouble(), 1e-9);
    }

    @Test
    void toKpisNodeCountsCancelledRequests() {
        LiftRequest req = LiftRequest.hallCall(2, Direction.DOWN);
        metrics.recordRequestCreation(req, 5L);
        req.transitionTo(RequestState.QUEUED);
        req.transitionTo(RequestState.CANCELLED);
        metrics.recordTerminalRequests(15L);

        ObjectNode kpis = metrics.toKpisNode(objectMapper);
        assertEquals(1, kpis.get("pickupRequestsCancelled").asInt());
        assertEquals(1, kpis.get("passengersCancelled").asInt());
        assertEquals(0, kpis.get("pickupRequestsServed").asInt());
        assertEquals(0, kpis.get("passengersServed").asInt());
    }

    @Test
    void toKpisNodePassengersServedSumsPassengerCount() {
        LiftRequest req = LiftRequest.hallCall(1, Direction.UP, 5);
        metrics.recordRequestCreation(req, 0L);
        req.completeRequest();
        metrics.recordTerminalRequests(10L);

        ObjectNode kpis = metrics.toKpisNode(objectMapper);
        assertEquals(1, kpis.get("pickupRequestsServed").asInt());
        assertEquals(5, kpis.get("passengersServed").asInt());
    }

    @Test
    void toKpisNodePassengersCancelledSumsPassengerCount() {
        LiftRequest req = LiftRequest.hallCall(2, Direction.DOWN, 3);
        metrics.recordRequestCreation(req, 5L);
        req.transitionTo(RequestState.QUEUED);
        req.transitionTo(RequestState.CANCELLED);
        metrics.recordTerminalRequests(15L);

        ObjectNode kpis = metrics.toKpisNode(objectMapper);
        assertEquals(1, kpis.get("pickupRequestsCancelled").asInt());
        assertEquals(3, kpis.get("passengersCancelled").asInt());
    }

    @Test
    void pickupLegUtilisationIsZeroWhenNoTicks() {
        ObjectNode kpis = metrics.toKpisNode(objectMapper);
        assertEquals(0.0, kpis.get("pickupLegUtilisation").asDouble(), 1e-9);
    }

    @Test
    void pickupLegUtilisationAccountsForMovingAndDoorTicks() {
        metrics.recordLiftState(new LiftState(1, LiftStatus.MOVING_UP));
        metrics.recordLiftState(new LiftState(2, LiftStatus.MOVING_UP));
        metrics.recordLiftState(new LiftState(2, LiftStatus.DOORS_OPEN));
        metrics.recordLiftState(new LiftState(2, LiftStatus.IDLE));
        // 3 active ticks out of 4 total
        ObjectNode kpis = metrics.toKpisNode(objectMapper);
        assertEquals(0.75, kpis.get("pickupLegUtilisation").asDouble(), 1e-9);
    }

    @Test
    void cachedKpisAreInvalidatedWhenMetricsChange() {
        metrics.recordLiftState(new LiftState(1, LiftStatus.IDLE));
        ObjectNode initialKpis = metrics.toKpisNode(objectMapper);
        assertEquals(1L, initialKpis.get("idleTicks").asLong());

        metrics.recordLiftState(new LiftState(1, LiftStatus.MOVING_UP));
        ObjectNode updatedKpis = metrics.toKpisNode(objectMapper);

        assertEquals(1L, updatedKpis.get("idleTicks").asLong());
        assertEquals(1L, updatedKpis.get("movingTicks").asLong());
        assertEquals(0.5, updatedKpis.get("pickupLegUtilisation").asDouble(), 1e-9);
    }

    @Test
    void recordActiveRequestsDoesNotDuplicateKnownRequests() {
        LiftRequest req = LiftRequest.hallCall(1, Direction.UP);
        metrics.recordRequestCreation(req, 0L);
        metrics.recordActiveRequests(Set.of(req), 5L);
        req.completeRequest();
        metrics.recordTerminalRequests(8L);

        ObjectNode kpis = metrics.toKpisNode(objectMapper);
        assertEquals(1, kpis.get("requestsTotal").asInt());
        // wait should be 8 - 0 = 8, not 8 - 5
        assertEquals(8L, kpis.get("maxPickupWaitTicks").asLong());
    }

    @Test
    void toPerFloorNodeCoversAllFloorsInRange() {
        ArrayNode floors = metrics.toPerFloorNode(objectMapper);
        assertEquals(5, floors.size());
        assertEquals(1, floors.get(0).get("floor").asInt());
        assertEquals(5, floors.get(4).get("floor").asInt());
    }

    @Test
    void toPerFloorNodeRecordsPassengerFlowOrigins() {
        PassengerFlowDTO flow = new PassengerFlowDTO(0, 2, 4, 3);
        metrics.recordPassengerFlow(flow, 3);

        ArrayNode floors = metrics.toPerFloorNode(objectMapper);
        ObjectNode floor2 = (ObjectNode) floors.get(1); // floor 2 is index 1 (minFloor=1)
        assertEquals(3, floor2.get("originPassengers").asInt());
        ObjectNode floor4 = (ObjectNode) floors.get(3);
        assertEquals(3, floor4.get("destinationPassengers").asInt());
    }

    @Test
    void toPerFloorNodeRecordsLiftVisits() {
        metrics.recordLiftState(new LiftState(3, LiftStatus.IDLE));
        metrics.recordLiftState(new LiftState(3, LiftStatus.IDLE)); // same floor — no extra visit
        metrics.recordLiftState(new LiftState(4, LiftStatus.MOVING_UP));

        ArrayNode floors = metrics.toPerFloorNode(objectMapper);
        ObjectNode floor3 = (ObjectNode) floors.get(2);
        assertEquals(1, floor3.get("liftVisits").asInt());
        ObjectNode floor4 = (ObjectNode) floors.get(3);
        assertEquals(1, floor4.get("liftVisits").asInt());
    }

    @Test
    void toPerLiftNodeContainsLiftConfig() {
        LiftConfigDTO config = new LiftConfigDTO(
            1, 5, 1, 2, 3, 2, 1, 1, 5,
            ControllerStrategy.DIRECTIONAL_SCAN, IdleParkingMode.STAY_AT_CURRENT_FLOOR
        );
        metrics.recordLiftState(new LiftState(1, LiftStatus.MOVING_UP));

        ArrayNode lifts = metrics.toPerLiftNode(objectMapper, config);
        assertNotNull(lifts);
        assertEquals(1, lifts.size());
        ObjectNode lift = (ObjectNode) lifts.get(0);
        assertEquals("lift-1", lift.get("liftId").asString());
        assertEquals(1, lift.get("minFloor").asInt());
        assertEquals(5, lift.get("maxFloor").asInt());
        assertEquals(1L, lift.get("movingTicks").asLong());
    }

    @Test
    void recordTerminalRequestsIsIdempotent() {
        LiftRequest req = LiftRequest.hallCall(1, Direction.UP);
        metrics.recordRequestCreation(req, 0L);
        req.completeRequest();
        metrics.recordTerminalRequests(10L);
        metrics.recordTerminalRequests(20L); // second call should not overwrite

        ObjectNode kpis = metrics.toKpisNode(objectMapper);
        assertEquals(10L, kpis.get("maxPickupWaitTicks").asLong());
    }
}
