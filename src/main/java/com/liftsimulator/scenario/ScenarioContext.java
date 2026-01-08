package com.liftsimulator.scenario;

import com.liftsimulator.engine.NaiveLiftController;
import com.liftsimulator.engine.SimulationEngine;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.HashMap;
import java.util.Map;

@SuppressFBWarnings(
        value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"},
        justification = "ScenarioContext is a coordination helper that intentionally exposes shared engine/controller."
)
public class ScenarioContext {
    private final SimulationEngine engine;
    private final NaiveLiftController controller;
    private final Map<String, Long> requestAliases = new HashMap<>();

    public ScenarioContext(SimulationEngine engine, NaiveLiftController controller) {
        this.engine = engine;
        this.controller = controller;
    }

    public SimulationEngine getEngine() {
        return engine;
    }

    public NaiveLiftController getController() {
        return controller;
    }

    public void registerAlias(String alias, long requestId) {
        requestAliases.put(alias, requestId);
    }

    public Long resolveAlias(String alias) {
        return requestAliases.get(alias);
    }
}
