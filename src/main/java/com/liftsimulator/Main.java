package com.liftsimulator;

import com.liftsimulator.engine.LiftController;
import com.liftsimulator.engine.SimpleLiftController;
import com.liftsimulator.engine.SimulationEngine;
import com.liftsimulator.domain.LiftState;

/**
 * Main entry point for the lift simulator.
 * Runs a simple demonstration of the simulation engine.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("=== Lift Simulator ===");
        System.out.println("Starting simulation...\n");

        // Create controller and engine
        LiftController controller = new SimpleLiftController();
        SimulationEngine engine = new SimulationEngine(controller, 0, 5);

        // Run simulation for 20 ticks
        int tickCount = 20;
        System.out.println(String.format("%-6s %-8s %-12s %-10s", "Tick", "Floor", "Direction", "Door"));
        System.out.println("------------------------------------------------");

        for (int i = 0; i < tickCount; i++) {
            LiftState state = engine.getCurrentState();
            System.out.println(String.format("%-6d %-8d %-12s %-10s",
                    engine.getCurrentTick(),
                    state.getFloor(),
                    state.getDirection(),
                    state.getDoorState()));

            engine.tick();
        }

        System.out.println("\nSimulation completed successfully!");
    }
}
