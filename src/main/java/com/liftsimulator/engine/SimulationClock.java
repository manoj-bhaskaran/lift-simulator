package com.liftsimulator.engine;

/**
 * Simple simulation clock that advances in deterministic ticks.
 */
public class SimulationClock {
    private long currentTick;

    /**
     * Creates a simulation clock starting at tick zero.
     */
    public SimulationClock() {
        this.currentTick = 0;
    }

    /**
     * Advances the simulation clock by one tick.
     */
    public void tick() {
        currentTick++;
    }

    /**
     * Returns the current tick value.
     *
     * @return the current tick
     */
    public long getCurrentTick() {
        return currentTick;
    }
}
