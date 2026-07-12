package com.liftsimulator.admin.service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cooperative cancellation flag shared between a run's executor bookkeeping
 * and its in-flight execution.
 */
final class CancellationToken {
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    void cancel() {
        cancelled.set(true);
    }

    boolean isCancelled() {
        return cancelled.get();
    }
}
