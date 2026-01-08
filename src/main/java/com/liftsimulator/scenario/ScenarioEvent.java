package com.liftsimulator.scenario;

public record ScenarioEvent(long tick, String description, ScenarioCommand command) {
}
