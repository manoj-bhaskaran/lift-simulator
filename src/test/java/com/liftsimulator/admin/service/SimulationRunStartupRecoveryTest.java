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
        SimulationRunService runService = mock(SimulationRunService.class);
        SimulationRunStartupRecovery recovery = new SimulationRunStartupRecovery(runService);

        recovery.recoverOrphanedSimulationRuns();

        verify(runService).recoverOrphanedRunsOnStartup();
    }
}
