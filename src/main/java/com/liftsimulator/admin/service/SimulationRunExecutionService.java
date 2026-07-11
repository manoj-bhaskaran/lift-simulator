package com.liftsimulator.admin.service;

import com.liftsimulator.admin.entity.SimulationRun;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

/**
 * Service to submit simulations asynchronously and manage queued/running tasks.
 */
@Service
public class SimulationRunExecutionService {
    private static final Logger logger = LoggerFactory.getLogger(SimulationRunExecutionService.class);

    private final RunLifecycleManager lifecycleManager;
    private final SimulationRunner simulationRunner;
    private final ThreadPoolExecutor executor;
    private final Map<Long, FutureTask<?>> runningTasks = new ConcurrentHashMap<>();
    private final Map<Long, CancellationToken> cancellationTokens = new ConcurrentHashMap<>();

    @Autowired
    public SimulationRunExecutionService(
            RunLifecycleManager lifecycleManager,
            SimulationRunner simulationRunner,
            @Value("${simulation.execution.max-concurrent-runs:4}") int maxConcurrentRuns,
            @Value("${simulation.execution.queue-capacity:20}") int queueCapacity) {
        this.lifecycleManager = lifecycleManager;
        this.simulationRunner = simulationRunner;
        this.executor = new ThreadPoolExecutor(
            maxConcurrentRuns,
            maxConcurrentRuns,
            60L,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(queueCapacity),
            new SimulationRunnerThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy()
        );
    }

    public SimulationRunExecutionService(
            RunLifecycleManager lifecycleManager,
            ConfigValidationService configValidationService,
            ScenarioValidationService scenarioValidationService,
            BatchInputGenerator batchInputGenerator,
            ObjectMapper objectMapper,
            int maxConcurrentRuns,
            int queueCapacity) {
        this(
            lifecycleManager,
            new SimulationRunner(
                lifecycleManager,
                configValidationService,
                scenarioValidationService,
                objectMapper,
                new SimulationArtefactWriter(objectMapper, batchInputGenerator, lifecycleManager)
            ),
            maxConcurrentRuns,
            queueCapacity
        );
    }

    /**
     * Submits an existing simulation run for asynchronous execution.
     * The run should be in CREATED state with configuration already set.
     *
     * @param runId the run id to execute
     */
    public void submitRunForExecution(Long runId) {
        SimulationRun run = getRunById(runId);
        String configJson = run.getVersion().getConfig();
        String scenarioJson = run.getScenario() != null ? run.getScenario().getScenarioJson() : null;
        String scenarioName = run.getScenario() != null ? run.getScenario().getName() : null;

        SimulationRunner.RunExecutionRequest request = new SimulationRunner.RunExecutionRequest(
            run.getId(),
            configJson,
            scenarioJson,
            scenarioName
        );
        submitExecution(request);
    }

    /**
     * Cancels a running simulation and interrupts execution.
     *
     * @param runId the run id
     * @return the updated run
     */
    public SimulationRun cancelRun(Long runId) {
        SimulationRun run = lifecycleManager.cancelRun(runId);
        CancellationToken token = cancellationTokens.computeIfAbsent(runId, id -> new CancellationToken());
        token.cancel();
        FutureTask<?> future = runningTasks.get(runId);
        if (future != null && future.cancel(false)) {
            executor.remove(future);
            runningTasks.remove(runId, future);
            cancellationTokens.remove(runId);
        }
        return run;
    }

    /**
     * Indicates whether any simulation run is currently submitted or executing.
     *
     * @return {@code true} if at least one run is in flight
     */
    public boolean hasActiveRuns() {
        return !runningTasks.isEmpty();
    }

    private SimulationRun getRunById(Long runId) {
        return lifecycleManager.getByIdWithDetails(runId);
    }

    @PreDestroy
    public void shutdownExecutor() {
        executor.shutdown();
    }

    private void submitExecution(SimulationRunner.RunExecutionRequest request) {
        CancellationToken token = cancellationTokens.computeIfAbsent(request.runId(), id -> new CancellationToken());
        FutureTask<?> task = new FutureTask<>(() -> {
            try {
                simulationRunner.executeRun(request, token);
            } finally {
                runningTasks.remove(request.runId());
                cancellationTokens.remove(request.runId());
            }
            return null;
        });
        try {
            executor.execute(task);
            runningTasks.put(request.runId(), task);
            if (task.isDone()) {
                runningTasks.remove(request.runId(), task);
                cancellationTokens.remove(request.runId());
            }
        } catch (RejectedExecutionException ex) {
            logger.warn("Run {} rejected: simulation execution queue is full", request.runId());
            cancellationTokens.remove(request.runId(), token);
            failRunWithMessage(request.runId(), "Simulation queue is full; run rejected.", false);
        }
    }

    private void failRunWithMessage(Long runId, String message, boolean started) {
        try {
            if (!started) {
                lifecycleManager.startRun(runId);
            }
            lifecycleManager.failRun(runId, message);
        } catch (IllegalStateException ex) {
            lifecycleManager.markRunningRunFailedSafely(runId, message, ex);
        } catch (Exception ex) {
            logger.error("Failed to mark run {} as failed", runId, ex);
        }
    }

    static final class CancellationToken {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        void cancel() {
            cancelled.set(true);
        }

        boolean isCancelled() {
            return cancelled.get();
        }
    }

    private static final class SimulationRunnerThreadFactory implements ThreadFactory {
        private int counter = 1;

        @Override
        public synchronized Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "simulation-runner-" + counter++);
            thread.setDaemon(true);
            return thread;
        }
    }
}
