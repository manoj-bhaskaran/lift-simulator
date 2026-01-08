package com.liftsimulator.scenario;

@FunctionalInterface
public interface ScenarioCommand {
    void apply(ScenarioContext context);
}
