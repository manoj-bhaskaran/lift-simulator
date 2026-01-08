package com.liftsimulator.scenario;

import com.liftsimulator.domain.LiftRequest;
import com.liftsimulator.domain.LiftState;
import com.liftsimulator.domain.RequestState;
import com.liftsimulator.engine.NaiveLiftController;
import com.liftsimulator.engine.SimulationEngine;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class ScenarioRunner {
    private final SimulationEngine engine;
    private final NaiveLiftController controller;

    public ScenarioRunner(SimulationEngine engine, NaiveLiftController controller) {
        this.engine = engine;
        this.controller = controller;
    }

    public void run(ScenarioDefinition scenario) {
        ScenarioContext context = new ScenarioContext(engine, controller);
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
            List<ScenarioEvent> events = eventsByTick.getOrDefault(engine.getCurrentTick(), List.of());
            String eventNotes = applyEvents(events, context);

            LiftState state = engine.getCurrentState();
            String pendingRequests = formatPendingRequests(controller);

            System.out.println(String.format("%-6d %-6d %-15s %-30s %-30s",
                    engine.getCurrentTick(),
                    state.getFloor(),
                    state.getStatus(),
                    pendingRequests,
                    eventNotes));

            engine.tick();
        }

        System.out.println();
        System.out.println("Scenario completed.");
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

    private String formatPendingRequests(NaiveLiftController controller) {
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
}
