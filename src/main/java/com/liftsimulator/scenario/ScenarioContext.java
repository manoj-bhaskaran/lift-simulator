package com.liftsimulator.scenario;

import com.liftsimulator.domain.LiftRequest;
import com.liftsimulator.engine.RequestManagingLiftController;
import com.liftsimulator.engine.SimulationEngine;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ScenarioContext {
    private final SimulationEngine engine;
    private final RequestManagingLiftController controller;
    private final Consumer<LiftRequest> requestTracker;
    private final Map<String, Long> requestAliases = new HashMap<>();

    public ScenarioContext(SimulationEngine engine, RequestManagingLiftController controller) {
        this(engine, controller, request -> {});
    }

    public ScenarioContext(SimulationEngine engine,
                           RequestManagingLiftController controller,
                           Consumer<LiftRequest> requestTracker) {
        this.engine = engine;
        this.controller = controller;
        this.requestTracker = requestTracker;
    }

    public SimulationEngine getEngine() {
        return engine;
    }

    public RequestManagingLiftController getController() {
        return controller;
    }

    public void addRequest(LiftRequest request) {
        controller.addRequest(request);
        requestTracker.accept(request);
    }

    public void registerAlias(String alias, long requestId) {
        requestAliases.put(alias, requestId);
    }

    public Long resolveAlias(String alias) {
        return requestAliases.get(alias);
    }
}
