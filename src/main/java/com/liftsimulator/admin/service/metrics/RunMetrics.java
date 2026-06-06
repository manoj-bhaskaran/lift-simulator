package com.liftsimulator.admin.service.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.liftsimulator.admin.dto.LiftConfigDTO;
import com.liftsimulator.admin.dto.PassengerFlowDTO;
import com.liftsimulator.domain.LiftRequest;
import com.liftsimulator.domain.LiftState;
import com.liftsimulator.domain.LiftStatus;
import com.liftsimulator.domain.RequestState;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class RunMetrics {
    private final Map<Long, RequestLifecycle> lifecycles = new LinkedHashMap<>();
    private final Map<LiftStatus, Long> statusCounts = new EnumMap<>(LiftStatus.class);
    private final Map<Integer, FloorMetrics> floorMetrics = new HashMap<>();
    private final int minFloor;
    private final int maxFloor;
    private long totalTicks;
    private Integer lastRecordedFloor;

    public RunMetrics(int minFloor, int maxFloor) {
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
    }

    public void recordPassengerFlow(PassengerFlowDTO flow, int passengers) {
        if (flow.originFloor() != null) {
            floorMetrics.computeIfAbsent(flow.originFloor(), FloorMetrics::new)
                .addOrigins(passengers);
        }
        if (flow.destinationFloor() != null) {
            floorMetrics.computeIfAbsent(flow.destinationFloor(), FloorMetrics::new)
                .addDestinations(passengers);
        }
    }

    public void recordLiftState(LiftState state) {
        statusCounts.merge(state.getStatus(), 1L, Long::sum);
        if (lastRecordedFloor == null || lastRecordedFloor != state.getFloor()) {
            floorMetrics.computeIfAbsent(state.getFloor(), FloorMetrics::new)
                .addVisit();
            lastRecordedFloor = state.getFloor();
        }
        totalTicks++;
    }

    public void recordRequestCreation(LiftRequest request, long tick) {
        lifecycles.computeIfAbsent(request.getId(), id -> new RequestLifecycle(request, tick));
    }

    public void recordActiveRequests(Set<LiftRequest> requests, long tick) {
        for (LiftRequest request : requests) {
            recordRequestCreation(request, tick);
        }
    }

    public void recordTerminalRequests(long tick) {
        for (RequestLifecycle lifecycle : lifecycles.values()) {
            if (lifecycle.terminalTick() != null || !lifecycle.request().isTerminal()) {
                continue;
            }
            lifecycle.markTerminal(tick, lifecycle.request().getState());
        }
    }

    public long totalTicks() {
        return totalTicks;
    }

    public ObjectNode toKpisNode(ObjectMapper objectMapper) {
        ObjectNode kpis = objectMapper.createObjectNode();
        long completed = lifecycles.values().stream()
            .filter(lifecycle -> lifecycle.terminalState() == RequestState.COMPLETED)
            .count();
        long cancelled = lifecycles.values().stream()
            .filter(lifecycle -> lifecycle.terminalState() == RequestState.CANCELLED)
            .count();
        long maxWait = lifecycles.values().stream()
            .filter(lifecycle -> lifecycle.terminalState() == RequestState.COMPLETED)
            .mapToLong(RequestLifecycle::waitTicks)
            .max()
            .orElse(0L);
        double avgWait = lifecycles.values().stream()
            .filter(lifecycle -> lifecycle.terminalState() == RequestState.COMPLETED)
            .mapToLong(RequestLifecycle::waitTicks)
            .average()
            .orElse(0.0);

        long idleTicks = statusCounts.getOrDefault(LiftStatus.IDLE, 0L)
            + statusCounts.getOrDefault(LiftStatus.OUT_OF_SERVICE, 0L);
        long movingTicks = statusCounts.getOrDefault(LiftStatus.MOVING_UP, 0L)
            + statusCounts.getOrDefault(LiftStatus.MOVING_DOWN, 0L);
        long doorTicks = statusCounts.getOrDefault(LiftStatus.DOORS_OPENING, 0L)
            + statusCounts.getOrDefault(LiftStatus.DOORS_OPEN, 0L)
            + statusCounts.getOrDefault(LiftStatus.DOORS_CLOSING, 0L);
        double utilisation = totalTicks == 0 ? 0.0 : (double) (movingTicks + doorTicks) / (double) totalTicks;

        kpis.put("requestsTotal", lifecycles.size());
        kpis.put("passengersServed", completed);
        kpis.put("passengersCancelled", cancelled);
        kpis.put("avgWaitTicks", avgWait);
        kpis.put("maxWaitTicks", maxWait);
        kpis.put("idleTicks", idleTicks);
        kpis.put("movingTicks", movingTicks);
        kpis.put("doorTicks", doorTicks);
        kpis.put("utilisation", utilisation);
        return kpis;
    }

    public ArrayNode toPerLiftNode(ObjectMapper objectMapper, LiftConfigDTO config) {
        ArrayNode lifts = objectMapper.createArrayNode();
        ObjectNode lift = objectMapper.createObjectNode();
        lift.put("liftId", "lift-1");
        lift.put("minFloor", minFloor);
        lift.put("maxFloor", maxFloor);
        lift.put("homeFloor", config.homeFloor());
        lift.put("travelTicksPerFloor", config.travelTicksPerFloor());
        lift.put("doorTransitionTicks", config.doorTransitionTicks());
        lift.put("doorDwellTicks", config.doorDwellTicks());
        lift.put("doorReopenWindowTicks", config.doorReopenWindowTicks());
        lift.put("controllerStrategy", config.controllerStrategy().name());
        lift.put("idleParkingMode", config.idleParkingMode().name());

        ObjectNode statusNode = objectMapper.createObjectNode();
        for (Map.Entry<LiftStatus, Long> entry : statusCounts.entrySet()) {
            statusNode.put(entry.getKey().name(), entry.getValue());
        }
        lift.set("statusCounts", statusNode);

        long idleTicks = statusCounts.getOrDefault(LiftStatus.IDLE, 0L)
            + statusCounts.getOrDefault(LiftStatus.OUT_OF_SERVICE, 0L);
        long movingTicks = statusCounts.getOrDefault(LiftStatus.MOVING_UP, 0L)
            + statusCounts.getOrDefault(LiftStatus.MOVING_DOWN, 0L);
        long doorTicks = statusCounts.getOrDefault(LiftStatus.DOORS_OPENING, 0L)
            + statusCounts.getOrDefault(LiftStatus.DOORS_OPEN, 0L)
            + statusCounts.getOrDefault(LiftStatus.DOORS_CLOSING, 0L);
        double utilisation = totalTicks == 0 ? 0.0 : (double) (movingTicks + doorTicks) / (double) totalTicks;

        lift.put("totalTicks", totalTicks);
        lift.put("idleTicks", idleTicks);
        lift.put("movingTicks", movingTicks);
        lift.put("doorTicks", doorTicks);
        lift.put("utilisation", utilisation);

        lifts.add(lift);
        return lifts;
    }

    public ArrayNode toPerFloorNode(ObjectMapper objectMapper) {
        ArrayNode floors = objectMapper.createArrayNode();
        for (int floor = minFloor; floor <= maxFloor; floor++) {
            FloorMetrics metrics = floorMetrics.getOrDefault(floor, new FloorMetrics(floor));
            ObjectNode floorNode = objectMapper.createObjectNode();
            floorNode.put("floor", floor);
            floorNode.put("originPassengers", metrics.originPassengers());
            floorNode.put("destinationPassengers", metrics.destinationPassengers());
            floorNode.put("liftVisits", metrics.liftVisits());
            floors.add(floorNode);
        }
        return floors;
    }
}
