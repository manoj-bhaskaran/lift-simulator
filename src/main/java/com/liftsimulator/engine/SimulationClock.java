package com.liftsimulator.engine;

/**
 * Simple simulation clock that advances in deterministic ticks.
 */
public class SimulationClock {
    private long currentTick;

    public SimulationClock() {
        this.currentTick = 0;
    }

    public void tick() {
        currentTick++;
    }

    public long getCurrentTick() {
        return currentTick;
    }
}
