package com.liftsimulator.admin.service;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Reconciles persisted simulation run state once the application is ready.
 */
@Component
public class SimulationRunStartupRecovery {

    private final RunLifecycleManager lifecycleManager;

    public SimulationRunStartupRecovery(RunLifecycleManager lifecycleManager) {
        this.lifecycleManager = lifecycleManager;
    }

    /**
     * Marks non-terminal runs left behind by a previous JVM shutdown as terminal.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverOrphanedSimulationRuns() {
        lifecycleManager.recoverOrphanedRunsOnStartup();
    }
}
