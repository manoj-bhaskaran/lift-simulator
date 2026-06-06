package com.liftsimulator.admin.service.metrics;

import com.liftsimulator.domain.LiftRequest;
import com.liftsimulator.domain.RequestState;

public final class RequestLifecycle {
    private final LiftRequest request;
    private final long createdTick;
    private Long terminalTick;
    private RequestState terminalState;

    public RequestLifecycle(LiftRequest request, long createdTick) {
        this.request = request;
        this.createdTick = createdTick;
    }

    public LiftRequest request() {
        return request;
    }

    public Long terminalTick() {
        return terminalTick;
    }

    public RequestState terminalState() {
        return terminalState;
    }

    public long waitTicks() {
        if (terminalTick == null) {
            return 0L;
        }
        return Math.max(0L, terminalTick - createdTick);
    }

    public void markTerminal(long tick, RequestState state) {
        this.terminalTick = tick;
        this.terminalState = state;
    }
}
