package com.liftsimulator.scenario;

import com.liftsimulator.domain.LiftRequest;
import com.liftsimulator.domain.LiftState;
import com.liftsimulator.domain.RequestState;
import com.liftsimulator.engine.RequestManagingLiftController;
import com.liftsimulator.engine.SimulationEngine;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class ScenarioRunner {
    private final SimulationEngine engine;
    private final RequestManagingLiftController controller;

    public ScenarioRunner(SimulationEngine engine, RequestManagingLiftController controller) {
        this.engine = engine;
        this.controller = controller;
    }

    public void run(ScenarioDefinition scenario) {
        Map<Long, RequestLifecycle> lifecycles = new LinkedHashMap<>();
        ScenarioContext context = new ScenarioContext(engine, controller,
                request -> recordRequestCreation(request, engine.getCurrentTick(), lifecycles));
        Map<Long, List<ScenarioEvent>> eventsByTick = scenario.getEvents().stream()
                .collect(Collectors.groupingBy(ScenarioEvent::tick));

        System.out.println("=== Scenario Runner ===");
        System.out.println("Scenario: " + scenario.getName());
        System.out.println("Total ticks: " + scenario.getTotalTicks());
        System.out.println();
        System.out.println(String.format("%-6s %-6s %-15s %-30s %-30s",
                "Tick", "Floor", "State", "Pending Requests", "Events"));
        System.out.println("-------------------------------------------------------------------------------------------");

        for (int i = 0; i < scenario.getTotalTicks(); i++) {
            long currentTick = engine.getCurrentTick();
            List<ScenarioEvent> events = eventsByTick.getOrDefault(currentTick, List.of());
            String eventNotes = applyEvents(events, context);

            LiftState state = engine.getCurrentState();
            recordActiveRequests(controller, currentTick, lifecycles);
            String pendingRequests = formatPendingRequests(controller);

            System.out.println(String.format("%-6d %-6d %-15s %-30s %-30s",
                    currentTick,
                    state.getFloor(),
                    state.getStatus(),
                    pendingRequests,
                    eventNotes));

            engine.tick();
            recordTerminalRequests(currentTick, lifecycles);
        }

        System.out.println();
        System.out.println("Scenario completed.");

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

    private String applyEvents(List<ScenarioEvent> events, ScenarioContext context) {
        if (events.isEmpty()) {
            return "-";
        }
        StringJoiner joiner = new StringJoiner(" | ");
        for (ScenarioEvent event : events) {
            event.command().apply(context);
            joiner.add(event.description());
        }
        return joiner.toString();
    }

    private String formatPendingRequests(RequestManagingLiftController controller) {
        Set<LiftRequest> activeRequests = controller.getRequests().stream()
                .filter(request -> !request.isTerminal())
                .collect(Collectors.toSet());

        if (activeRequests.isEmpty()) {
            return "-";
        }

        Map<RequestState, Long> stateCounts = activeRequests.stream()
                .collect(Collectors.groupingBy(LiftRequest::getState, Collectors.counting()));

        StringBuilder sb = new StringBuilder();
        appendCount(sb, "Q", stateCounts.get(RequestState.QUEUED));
        appendCount(sb, "A", stateCounts.get(RequestState.ASSIGNED));
        appendCount(sb, "S", stateCounts.get(RequestState.SERVING));

        String floors = activeRequests.stream()
                .map(LiftRequest::getTargetFloor)
                .distinct()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        if (!floors.isEmpty()) {
            sb.append(" Floors:").append(floors);
        }

        return sb.toString().trim();
    }

    private void appendCount(StringBuilder sb, String label, Long count) {
        if (count != null && count > 0) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(label).append(":").append(count);
        }
    }

    private void recordActiveRequests(RequestManagingLiftController controller,
                                      long tick,
                                      Map<Long, RequestLifecycle> lifecycles) {
        for (LiftRequest request : controller.getRequests()) {
            recordRequestCreation(request, tick, lifecycles);
        }
    }

    private void recordRequestCreation(LiftRequest request, long tick, Map<Long, RequestLifecycle> lifecycles) {
        lifecycles.computeIfAbsent(request.getId(), id -> new RequestLifecycle(request, tick));
    }

    private void recordTerminalRequests(long tick, Map<Long, RequestLifecycle> lifecycles) {
        for (RequestLifecycle lifecycle : lifecycles.values()) {
            if (lifecycle.terminalTick() != null || !lifecycle.request().isTerminal()) {
                continue;
            }
            lifecycle.markTerminal(tick, lifecycle.request().getState());
        }
    }

    private String formatRequestDescription(LiftRequest request) {
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
}
