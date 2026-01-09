package com.liftsimulator.scenario;

import com.liftsimulator.domain.IdleParkingMode;

import java.util.Collections;
import java.util.List;

public final class ScenarioDefinition {
    private final String name;
    private final int totalTicks;
    private final List<ScenarioEvent> events;
    private final Integer minFloor;
    private final Integer maxFloor;
    private final Integer configuredMinFloor;
    private final Integer configuredMaxFloor;
    private final Integer initialFloor;
    private final Integer travelTicksPerFloor;
    private final Integer doorTransitionTicks;
    private final Integer doorDwellTicks;
    private final Integer doorReopenWindowTicks;
    private final Integer homeFloor;
    private final Integer idleTimeoutTicks;
    private final IdleParkingMode idleParkingMode;

    public ScenarioDefinition(String name, int totalTicks, List<ScenarioEvent> events) {
        this(name, totalTicks, events, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public ScenarioDefinition(
            String name,
            int totalTicks,
            List<ScenarioEvent> events,
            Integer minFloor,
            Integer maxFloor,
            Integer configuredMinFloor,
            Integer configuredMaxFloor,
            Integer initialFloor,
            Integer travelTicksPerFloor,
            Integer doorTransitionTicks,
            Integer doorDwellTicks,
            Integer doorReopenWindowTicks,
            Integer homeFloor,
            Integer idleTimeoutTicks,
            IdleParkingMode idleParkingMode
    ) {
        if (totalTicks <= 0) {
            throw new IllegalArgumentException("totalTicks must be > 0");
        }
        this.name = name;
        this.totalTicks = totalTicks;
        this.events = List.copyOf(events);
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
        this.configuredMinFloor = configuredMinFloor;
        this.configuredMaxFloor = configuredMaxFloor;
        this.initialFloor = initialFloor;
        this.travelTicksPerFloor = travelTicksPerFloor;
        this.doorTransitionTicks = doorTransitionTicks;
        this.doorDwellTicks = doorDwellTicks;
        this.doorReopenWindowTicks = doorReopenWindowTicks;
        this.homeFloor = homeFloor;
        this.idleTimeoutTicks = idleTimeoutTicks;
        this.idleParkingMode = idleParkingMode;
    }

    public String getName() {
        return name;
    }

    public int getTotalTicks() {
        return totalTicks;
    }

    public List<ScenarioEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public Integer getMinFloor() {
        return minFloor;
    }

    public Integer getMaxFloor() {
        return maxFloor;
    }

    public Integer getConfiguredMinFloor() {
        return configuredMinFloor;
    }

    public Integer getConfiguredMaxFloor() {
        return configuredMaxFloor;
    }

    public Integer getInitialFloor() {
        return initialFloor;
    }

    public Integer getTravelTicksPerFloor() {
        return travelTicksPerFloor;
    }

    public Integer getDoorTransitionTicks() {
        return doorTransitionTicks;
    }

    public Integer getDoorDwellTicks() {
        return doorDwellTicks;
    }

    public Integer getDoorReopenWindowTicks() {
        return doorReopenWindowTicks;
    }

    public Integer getHomeFloor() {
        return homeFloor;
    }

    public Integer getIdleTimeoutTicks() {
        return idleTimeoutTicks;
    }

    public IdleParkingMode getIdleParkingMode() {
        return idleParkingMode;
    }
}
