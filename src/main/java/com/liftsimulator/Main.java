package com.liftsimulator;

import com.liftsimulator.engine.NaiveLiftController;
import com.liftsimulator.engine.SimulationEngine;
import com.liftsimulator.domain.CarCall;
import com.liftsimulator.domain.Direction;
import com.liftsimulator.domain.DoorState;
import com.liftsimulator.domain.HallCall;
import com.liftsimulator.domain.LiftRequest;
import com.liftsimulator.domain.LiftState;
import com.liftsimulator.domain.RequestState;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Main entry point for the lift simulator.
 * Runs a demonstration of the NaiveLiftController with realistic scenarios.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("=== Lift Simulator - NaiveLiftController Demo ===");
        System.out.println("Version: " + resolveVersion());
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
        System.out.println(String.format("%-6s %-8s %-15s %-12s %-10s %-12s %-15s",
                "Tick", "Floor", "Status", "Direction", "Door", "Requests", "Notes"));
        System.out.println("-------------------------------------------------------------------------------------------");

        for (int i = 0; i < tickCount; i++) {
            LiftState state = engine.getCurrentState();
            String notes = "";

            // Add contextual notes
            if (i == 0) notes = "(Starting)";
            if (state.getFloor() == 3 && state.getDoorState() == DoorState.OPEN) {
                notes = "(Servicing floor 3)";
            }
            if (state.getFloor() == 5 && state.getDoorState() == DoorState.OPEN) {
                notes = "(Servicing floor 5)";
            }
            if (state.getFloor() == 7 && state.getDoorState() == DoorState.OPEN) {
                notes = "(Servicing floor 7)";
            }

            String requestStatus = formatRequestStatus(controller);

            System.out.println(String.format("%-6d %-8d %-15s %-12s %-10s %-12s %-15s",
                    engine.getCurrentTick(),
                    state.getFloor(),
                    state.getStatus(),
                    state.getDirection(),
                    state.getDoorState(),
                    requestStatus,
                    notes));

            engine.tick();
        }

        System.out.println("\nSimulation completed successfully!");
        System.out.println("All requests serviced using naive scheduling (nearest floor first).");
    }

    /**
     * Formats a compact summary of active request states.
     * Shows count of requests in each lifecycle state (e.g., "Q:2 A:1 S:1").
     */
    private static String formatRequestStatus(NaiveLiftController controller) {
        Map<RequestState, Long> stateCounts = controller.getRequests().stream()
                .filter(request -> !request.isTerminal())
                .collect(Collectors.groupingBy(LiftRequest::getState, Collectors.counting()));

        if (stateCounts.isEmpty()) {
            return "-";
        }

        StringBuilder sb = new StringBuilder();
        long queued = stateCounts.getOrDefault(RequestState.QUEUED, 0L);
        long assigned = stateCounts.getOrDefault(RequestState.ASSIGNED, 0L);
        long serving = stateCounts.getOrDefault(RequestState.SERVING, 0L);

        if (queued > 0) sb.append("Q:").append(queued).append(" ");
        if (assigned > 0) sb.append("A:").append(assigned).append(" ");
        if (serving > 0) sb.append("S:").append(serving).append(" ");

        return sb.toString().trim();
    }

    private static String resolveVersion() {
        Properties properties = new Properties();
        try (InputStream inputStream = Main.class.getResourceAsStream("/version.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
                String version = properties.getProperty("version");
                if (version != null && !version.isBlank()) {
                    return version.trim();
                }
            }
        } catch (IOException e) {
            return "unknown";
        }
        return "unknown";
    }
}
