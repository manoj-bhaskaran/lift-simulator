package com.liftsimulator;

import com.liftsimulator.domain.CarCall;
import com.liftsimulator.domain.ControllerStrategy;
import com.liftsimulator.domain.Direction;
import com.liftsimulator.domain.DoorState;
import com.liftsimulator.domain.HallCall;
import com.liftsimulator.domain.IdleParkingMode;
import com.liftsimulator.domain.LiftRequest;
import com.liftsimulator.domain.LiftState;
import com.liftsimulator.domain.LiftStatus;
import com.liftsimulator.domain.RequestState;
import com.liftsimulator.engine.ControllerFactory;
import com.liftsimulator.engine.RequestManagingLiftController;
import com.liftsimulator.engine.SimulationEngine;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Main entry point for the lift simulator.
 * Runs a demonstration of a lift controller with realistic scenarios.
 */
public class Main {
    private static final ControllerStrategy DEFAULT_CONTROLLER_STRATEGY = ControllerStrategy.NEAREST_REQUEST_ROUTING;
    private static final IdleParkingMode DEFAULT_IDLE_PARKING_MODE = IdleParkingMode.PARK_TO_HOME_FLOOR;

    public static void main(String[] args) {
        // Parse command-line arguments.
        ControllerStrategy controllerStrategy = DEFAULT_CONTROLLER_STRATEGY;
        IdleParkingMode idleParkingMode = DEFAULT_IDLE_PARKING_MODE;

        for (String arg : args) {
            if (arg.equals("--help") || arg.equals("-h")) {
                printUsage();
                System.exit(0);
            } else if (arg.startsWith("--strategy=")) {
                String strategy = arg.substring("--strategy=".length());
                try {
                    controllerStrategy = parseControllerStrategy(strategy);
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid strategy: " + strategy);
                    System.err.println("Valid strategies: nearest-request, directional-scan");
                    printUsage();
                    System.exit(1);
                }
            } else {
                System.err.println("Unknown argument: " + arg);
                printUsage();
                System.exit(1);
            }
        }

        System.out.println("=== Lift Simulator - Controller Demo ===");
        System.out.println("Version: " + resolveVersion());
        System.out.println("Controller Strategy: " + controllerStrategy);
        System.out.println("Idle Parking Mode: " + idleParkingMode);
        System.out.println("Starting simulation...\n");

        // Create controller using factory with configured strategy and parking mode.
        RequestManagingLiftController controller = (RequestManagingLiftController) ControllerFactory.createController(
                controllerStrategy,
                0,                    // homeFloor
                5,                    // idleTimeoutTicks
                idleParkingMode
        );
        SimulationEngine engine = SimulationEngine.builder(controller, 0, 10).build();
        Map<Long, RequestLifecycle> lifecycles = new LinkedHashMap<>();

        // Add some requests to simulate real usage.
        System.out.println("Requests:");
        System.out.println("  - Car call to floor 3");
        System.out.println("  - Hall call from floor 7 going UP");
        System.out.println("  - Car call to floor 5");
        System.out.println("  - Car call to floor 8 (will be cancelled at tick 15)");
        System.out.println("  - Graceful out-of-service initiated at tick 25");
        System.out.println("  - Return to service at tick 35");
        System.out.println();

        controller.addCarCall(new CarCall(3));
        controller.addHallCall(new HallCall(7, Direction.UP));
        controller.addCarCall(new CarCall(5));

        // Add a request that we'll cancel later to demonstrate cancellation.
        LiftRequest requestToCancel = LiftRequest.carCall(8);
        controller.addRequest(requestToCancel);
        recordRequestCreations(controller, engine.getCurrentTick(), lifecycles);

        // Run simulation for 50 ticks to show out-of-service scenario.
        int tickCount = 50;
        System.out.println(String.format("%-6s %-8s %-15s %-12s %-10s %-12s %-15s",
                "Tick", "Floor", "Status", "Direction", "Door", "Requests", "Notes"));
        System.out.println("-------------------------------------------------------------------------------------------");

        for (int i = 0; i < tickCount; i++) {
            LiftState state = engine.getCurrentState();
            String notes = "";

            // Cancel request to floor 8 at tick 15 to demonstrate cancellation.
            if (i == 15 && !requestToCancel.isTerminal()) {
                controller.cancelRequest(requestToCancel.getId());
                notes = "*** CANCELLED request to floor 8 ***";
            }

            // Take lift out of service at tick 25 (graceful shutdown begins).
            if (i == 25 && state.getStatus() != LiftStatus.OUT_OF_SERVICE) {
                controller.takeOutOfService();
                engine.setOutOfService();
                notes = "*** INITIATING OUT-OF-SERVICE (graceful shutdown) ***";
            }

            // Return lift to service (whenever it's actually OUT_OF_SERVICE).
            if (i == 35 && state.getStatus() == LiftStatus.OUT_OF_SERVICE) {
                controller.returnToService();
                engine.returnToService();
                notes = "*** RETURNED TO SERVICE ***";
            }

            // Add a new request after returning to service.
            if (i == 36) {
                controller.addCarCall(new CarCall(4));
                notes = "*** NEW request to floor 4 ***";
            }

            recordRequestCreations(controller, engine.getCurrentTick(), lifecycles);
            recordTerminalRequests(controller, engine.getCurrentTick(), lifecycles);

            // Add contextual notes.
            if (i == 0) notes = "(Starting)";
            if (state.getFloor() == 3 && state.getDoorState() == DoorState.OPEN) {
                notes = "(Servicing floor 3)";
            }
            if (state.getFloor() == 4 && state.getDoorState() == DoorState.OPEN) {
                notes = "(Servicing floor 4)";
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

        recordTerminalRequests(controller, engine.getCurrentTick(), lifecycles);

        System.out.println("\nSimulation completed successfully!");
        System.out.println("All requests serviced using " + controllerStrategy + " scheduling.");
        System.out.println("\nKey events:");
        System.out.println("  - Request to floor 8 was cancelled at tick 15 and never serviced.");
        System.out.println("  - Out-of-service initiated at tick 25 with graceful shutdown sequence:");
        System.out.println("    * All pending requests cancelled immediately");
        System.out.println("    * Lift completes movement to next floor (if moving)");
        System.out.println("    * Doors open to allow passenger exit, then close");
        System.out.println("    * Transitions to OUT_OF_SERVICE state");
        System.out.println("  - Lift returned to service at tick 35.");
        System.out.println("  - New request to floor 4 added and serviced after returning to service.");

        System.out.println("\nRequest lifecycle summary:");
        System.out.println(String.format("%-40s %-14s %-24s", "Request", "Created Tick", "Completed/Cancelled Tick"));
        System.out.println("----------------------------------------------------------------------------------------");
        lifecycles.values().stream()
                .sorted(Comparator.comparingLong(RequestLifecycle::createdTick)
                        .thenComparing(lifecycle -> lifecycle.request().getId()))
                .forEach(lifecycle -> {
                    String completion = lifecycle.terminalTick() == null
                            ? "-"
                            : lifecycle.terminalTick() + " (" + lifecycle.terminalState() + ")";
                    System.out.println(String.format("%-40s %-14d %-24s",
                            formatRequestDescription(lifecycle.request()),
                            lifecycle.createdTick(),
                            completion));
                });
    }

    /**
     * Formats a compact summary of active request states.
     * Shows count of requests in each lifecycle state (e.g., "Q:2 A:1 S:1").
     */
    private static String formatRequestStatus(RequestManagingLiftController controller) {
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

    private static void recordRequestCreations(RequestManagingLiftController controller,
                                               long tick,
                                               Map<Long, RequestLifecycle> lifecycles) {
        for (LiftRequest request : controller.getRequests()) {
            lifecycles.computeIfAbsent(request.getId(), id -> new RequestLifecycle(request, tick));
        }
    }

    private static void recordTerminalRequests(RequestManagingLiftController controller,
                                               long tick,
                                               Map<Long, RequestLifecycle> lifecycles) {
        for (LiftRequest request : controller.getRequests()) {
            if (!request.isTerminal()) {
                continue;
            }
            RequestLifecycle lifecycle = lifecycles.computeIfAbsent(
                    request.getId(),
                    id -> new RequestLifecycle(request, tick));
            if (lifecycle.terminalTick() == null) {
                lifecycle.markTerminal(tick, request.getState());
            }
        }
    }

    private static String formatRequestDescription(LiftRequest request) {
        return switch (request.getType()) {
            case CAR_CALL -> {
                Integer origin = request.getOriginFloor();
                Integer destination = request.getDestinationFloor();
                if (origin != null && destination != null) {
                    yield String.format("Car call from floor %d to floor %d", origin, destination);
                }
                yield String.format("Car call to floor %d", Objects.requireNonNull(destination));
            }
            case HALL_CALL -> String.format("Hall call from floor %d (%s)",
                    request.getOriginFloor(),
                    request.getDirection());
        };
    }

    private static final class RequestLifecycle {
        private final LiftRequest request;
        private final long createdTick;
        private Long terminalTick;
        private RequestState terminalState;

        private RequestLifecycle(LiftRequest request, long createdTick) {
            this.request = request;
            this.createdTick = createdTick;
        }

        private LiftRequest request() {
            return request;
        }

        private long createdTick() {
            return createdTick;
        }

        private Long terminalTick() {
            return terminalTick;
        }

        private RequestState terminalState() {
            return terminalState;
        }

        private void markTerminal(long tick, RequestState state) {
            this.terminalTick = tick;
            this.terminalState = state;
        }
    }

    private static void printUsage() {
        System.out.println("Usage: java -jar lift-simulator.jar [OPTIONS]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -h, --help                      Show this help message");
        System.out.println("  --strategy=<strategy>           Controller strategy to use");
        System.out.println("                                  Valid values: nearest-request, directional-scan");
        System.out.println("                                  Default: nearest-request");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar lift-simulator.jar");
        System.out.println("  java -jar lift-simulator.jar --strategy=directional-scan");
        System.out.println("  java -jar lift-simulator.jar --strategy=nearest-request");
        System.out.println("  java -jar lift-simulator.jar --help");
    }

    private static ControllerStrategy parseControllerStrategy(String strategy) {
        return switch (strategy.toLowerCase(Locale.ROOT)) {
            case "nearest-request" -> ControllerStrategy.NEAREST_REQUEST_ROUTING;
            case "directional-scan" -> ControllerStrategy.DIRECTIONAL_SCAN;
            default -> throw new IllegalArgumentException("Unknown strategy: " + strategy);
        };
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
