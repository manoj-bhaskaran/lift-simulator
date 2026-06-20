package com.liftsimulator.admin.service;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Reconciles persisted simulation run state once the application is ready.
 */
@Component
public class SimulationRunStartupRecovery {

    private final SimulationRunService simulationRunService;

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "Spring-managed services are injected and treated as shared dependencies."
    )
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
