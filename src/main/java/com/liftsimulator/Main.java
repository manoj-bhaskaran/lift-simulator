package com.liftsimulator;

import com.liftsimulator.engine.LiftController;
import com.liftsimulator.engine.NaiveLiftController;
import com.liftsimulator.engine.SimulationEngine;
import com.liftsimulator.domain.CarCall;
import com.liftsimulator.domain.Direction;
import com.liftsimulator.domain.HallCall;
import com.liftsimulator.domain.LiftState;

/**
 * Main entry point for the lift simulator.
 * Runs a demonstration of the NaiveLiftController with realistic scenarios.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("=== Lift Simulator - NaiveLiftController Demo ===");
        System.out.println("Starting simulation...\n");

        // Create naive controller and engine
        NaiveLiftController controller = new NaiveLiftController();
        SimulationEngine engine = new SimulationEngine(controller, 0, 10);

        // Add some requests to simulate real usage
        System.out.println("Requests:");
        System.out.println("  - Car call to floor 3");
        System.out.println("  - Hall call from floor 7 going UP");
        System.out.println("  - Car call to floor 5");
        System.out.println();

        controller.addCarCall(new CarCall(3));
        controller.addHallCall(new HallCall(7, Direction.UP));
        controller.addCarCall(new CarCall(5));

        // Run simulation for 40 ticks
        int tickCount = 40;
        System.out.println(String.format("%-6s %-8s %-12s %-10s %-15s", "Tick", "Floor", "Direction", "Door", "Notes"));
        System.out.println("------------------------------------------------------------------------");

        for (int i = 0; i < tickCount; i++) {
            LiftState state = engine.getCurrentState();
            String notes = "";

            // Add contextual notes
            if (i == 0) notes = "(Starting)";
            if (state.getFloor() == 3 && state.getDoorState().toString().equals("OPEN")) {
                notes = "(Servicing floor 3)";
            }
            if (state.getFloor() == 5 && state.getDoorState().toString().equals("OPEN")) {
                notes = "(Servicing floor 5)";
            }
            if (state.getFloor() == 7 && state.getDoorState().toString().equals("OPEN")) {
                notes = "(Servicing floor 7)";
            }

            System.out.println(String.format("%-6d %-8d %-12s %-10s %-15s",
                    engine.getCurrentTick(),
                    state.getFloor(),
                    state.getDirection(),
                    state.getDoorState(),
                    notes));

            engine.tick();
        }

        System.out.println("\nSimulation completed successfully!");
        System.out.println("All requests serviced using naive scheduling (nearest floor first).");
    }
}
