package com.liftsimulator.admin.service;

import com.liftsimulator.admin.entity.LiftSystem;
import com.liftsimulator.admin.entity.LiftSystemVersion;
import com.liftsimulator.admin.entity.SimulationRun;
import com.liftsimulator.admin.repository.SimulationRunRepository;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RunLifecycleManagerTest {

    @Mock
    private SimulationRunRepository runRepository;

    @Mock
    private EntityManager entityManager;

    private RunLifecycleManager lifecycleManager;
    private SimulationRun run;

    @BeforeEach
    void setUp() throws Exception {
        lifecycleManager = new RunLifecycleManager(runRepository);
        java.lang.reflect.Field entityManagerField = RunLifecycleManager.class.getDeclaredField("entityManager");
        entityManagerField.setAccessible(true);
        entityManagerField.set(lifecycleManager, entityManager);

        LiftSystem liftSystem = new LiftSystem("test-system", "Test System", "Test system");
        liftSystem.setId(1L);
        LiftSystemVersion version = new LiftSystemVersion();
        version.setId(2L);
        version.setLiftSystem(liftSystem);
        run = new SimulationRun(liftSystem, version);
        run.setId(3L);
    }

    @Test
    void startRunOwnsCreatedToRunningTransition() {
        when(runRepository.findByIdWithDetails(3L)).thenReturn(Optional.of(run));
        when(runRepository.save(any(SimulationRun.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SimulationRun result = lifecycleManager.startRun(3L);

        assertEquals(SimulationRun.RunStatus.RUNNING, result.getStatus());
        verify(runRepository).save(run);
    }

    @Test
    void updateProgressUsesTargetedUpdateAndReloads() {
        run.start();
        run.setCurrentTick(25L);
        when(runRepository.updateCurrentTick(3L, 25L)).thenReturn(1);
        when(runRepository.findByIdWithDetails(3L)).thenReturn(Optional.of(run));

        SimulationRun result = lifecycleManager.updateProgress(3L, 25L);

        assertEquals(25L, result.getCurrentTick());
        verify(entityManager).clear();
    }

    @Test
    void missingRunThrowsResourceNotFound() {
        when(runRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> lifecycleManager.getByIdWithDetails(99L));
    }
}
