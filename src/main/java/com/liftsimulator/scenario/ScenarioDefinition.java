package com.liftsimulator.scenario;

import java.util.Collections;
import java.util.List;

public class ScenarioDefinition {
    private final String name;
    private final int totalTicks;
    private final List<ScenarioEvent> events;
    private final Integer minFloor;
    private final Integer maxFloor;

    public ScenarioDefinition(String name, int totalTicks, List<ScenarioEvent> events) {
        this(name, totalTicks, events, null, null);
    }

    public ScenarioDefinition(String name, int totalTicks, List<ScenarioEvent> events, Integer minFloor, Integer maxFloor) {
        if (totalTicks <= 0) {
            throw new IllegalArgumentException("totalTicks must be > 0");
        }
        this.name = name;
        this.totalTicks = totalTicks;
        this.events = List.copyOf(events);
        this.minFloor = minFloor;
        this.maxFloor = maxFloor;
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
}
