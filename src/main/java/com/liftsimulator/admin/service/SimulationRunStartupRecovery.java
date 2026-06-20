package com.liftsimulator.admin.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Reconciles persisted simulation run state once the application is ready.
 */
@Component
public class SimulationRunStartupRecovery {

    private final SimulationRunService simulationRunService;

    public SimulationRunStartupRecovery(SimulationRunService simulationRunService) {
        this.simulationRunService = simulationRunService;
    }

    /**
     * Marks non-terminal runs left behind by a previous JVM shutdown as terminal.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverOrphanedSimulationRuns() {
        simulationRunService.recoverOrphanedRunsOnStartup();
    }
}
