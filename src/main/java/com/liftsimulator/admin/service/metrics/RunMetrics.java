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
    private CachedKpis cachedKpis;

    public RunMetrics(int minFloor, int maxFloor) {
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
    }

    public void recordPassengerFlow(PassengerFlowDTO flow, int passengers) {
        invalidateCachedKpis();
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
        invalidateCachedKpis();
        statusCounts.merge(state.getStatus(), 1L, Long::sum);
        if (lastRecordedFloor == null || lastRecordedFloor != state.getFloor()) {
            floorMetrics.computeIfAbsent(state.getFloor(), FloorMetrics::new)
                .addVisit();
            lastRecordedFloor = state.getFloor();
        }
        totalTicks++;
    }

    public void recordRequestCreation(LiftRequest request, long tick) {
        if (!lifecycles.containsKey(request.getId())) {
            lifecycles.put(request.getId(), new RequestLifecycle(request, tick));
            invalidateCachedKpis();
        }
    }

    public void recordActiveRequests(Set<LiftRequest> requests, long tick) {
        for (LiftRequest request : requests) {
            recordRequestCreation(request, tick);
        }
    }

    public void recordTerminalRequests(long tick) {
        boolean changed = false;
        for (RequestLifecycle lifecycle : lifecycles.values()) {
            if (lifecycle.terminalTick() != null || !lifecycle.request().isTerminal()) {
                continue;
            }
            lifecycle.markTerminal(tick, lifecycle.request().getState());
            changed = true;
        }
        if (changed) {
            invalidateCachedKpis();
        }
    }

    public long totalTicks() {
        return totalTicks;
    }

    public ObjectNode toKpisNode(ObjectMapper objectMapper) {
        CachedKpis kpiValues = getCachedKpis();
        ObjectNode kpis = objectMapper.createObjectNode();
        kpis.put("requestsTotal", kpiValues.requestsTotal());
        kpis.put("passengersServed", kpiValues.passengersServed());
        kpis.put("passengersCancelled", kpiValues.passengersCancelled());
        kpis.put("avgWaitTicks", kpiValues.avgWaitTicks());
        kpis.put("maxWaitTicks", kpiValues.maxWaitTicks());
        kpis.put("idleTicks", kpiValues.idleTicks());
        kpis.put("movingTicks", kpiValues.movingTicks());
        kpis.put("doorTicks", kpiValues.doorTicks());
        kpis.put("utilisation", kpiValues.utilisation());
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

        CachedKpis kpiValues = getCachedKpis();
        lift.put("totalTicks", totalTicks);
        lift.put("idleTicks", kpiValues.idleTicks());
        lift.put("movingTicks", kpiValues.movingTicks());
        lift.put("doorTicks", kpiValues.doorTicks());
        lift.put("utilisation", kpiValues.utilisation());

        lifts.add(lift);
        return lifts;
    }

    private CachedKpis getCachedKpis() {
        if (cachedKpis == null) {
            cachedKpis = computeKpis();
        }
        return cachedKpis;
    }

    private CachedKpis computeKpis() {
        long completed = 0L;
        long cancelled = 0L;
        long maxWait = 0L;
        long totalWait = 0L;

        for (RequestLifecycle lifecycle : lifecycles.values()) {
            if (lifecycle.terminalState() == RequestState.COMPLETED) {
                completed++;
                long waitTicks = lifecycle.waitTicks();
                totalWait += waitTicks;
                maxWait = Math.max(maxWait, waitTicks);
            } else if (lifecycle.terminalState() == RequestState.CANCELLED) {
                cancelled++;
            }
        }

        double avgWait = completed == 0L ? 0.0 : (double) totalWait / (double) completed;
        long idleTicks = statusCounts.getOrDefault(LiftStatus.IDLE, 0L)
            + statusCounts.getOrDefault(LiftStatus.OUT_OF_SERVICE, 0L);
        long movingTicks = statusCounts.getOrDefault(LiftStatus.MOVING_UP, 0L)
            + statusCounts.getOrDefault(LiftStatus.MOVING_DOWN, 0L);
        long doorTicks = statusCounts.getOrDefault(LiftStatus.DOORS_OPENING, 0L)
            + statusCounts.getOrDefault(LiftStatus.DOORS_OPEN, 0L)
            + statusCounts.getOrDefault(LiftStatus.DOORS_CLOSING, 0L);
        double utilisation = totalTicks == 0 ? 0.0 : (double) (movingTicks + doorTicks) / (double) totalTicks;

        return new CachedKpis(
            lifecycles.size(),
            completed,
            cancelled,
            avgWait,
            maxWait,
            idleTicks,
            movingTicks,
            doorTicks,
            utilisation
        );
    }

    private void invalidateCachedKpis() {
        cachedKpis = null;
    }

    private record CachedKpis(
        int requestsTotal,
        long passengersServed,
        long passengersCancelled,
        double avgWaitTicks,
        long maxWaitTicks,
        long idleTicks,
        long movingTicks,
        long doorTicks,
        double utilisation
    ) {
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
