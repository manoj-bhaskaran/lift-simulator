package com.liftsimulator.admin.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for SimulationRunStartupRecovery.
 */
public class SimulationRunStartupRecoveryTest {

    @Test
    public void testApplicationReadyEventTriggersRecovery() {
        RunLifecycleManager lifecycleManager = mock(RunLifecycleManager.class);
        SimulationRunStartupRecovery recovery = new SimulationRunStartupRecovery(lifecycleManager);

        recovery.recoverOrphanedSimulationRuns();

        verify(lifecycleManager).recoverOrphanedRunsOnStartup();
    }
}
