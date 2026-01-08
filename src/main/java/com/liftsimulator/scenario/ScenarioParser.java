package com.liftsimulator.scenario;

import com.liftsimulator.domain.Direction;
import com.liftsimulator.domain.LiftRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ScenarioParser {
    public ScenarioDefinition parse(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path)) {
            return parse(inputStream, path.toString());
        }
    }

    public ScenarioDefinition parseResource(String resourcePath) throws IOException {
        InputStream inputStream = ScenarioParser.class.getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new IOException("Scenario resource not found: " + resourcePath);
        }
        try (InputStream stream = inputStream) {
            return parse(stream, resourcePath);
        }
    }

    private ScenarioDefinition parse(InputStream inputStream, String sourceName) throws IOException {
        String scenarioName = "Unnamed Scenario";
        Integer totalTicks = null;
        List<ScenarioEvent> events = new ArrayList<>();
        Integer minFloor = null;
        Integer maxFloor = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                if (trimmed.startsWith("name:")) {
                    scenarioName = trimmed.substring("name:".length()).trim();
                    continue;
                }

                if (trimmed.startsWith("ticks:")) {
                    String value = trimmed.substring("ticks:".length()).trim();
                    totalTicks = Integer.parseInt(value);
                    continue;
                }

                String[] tokens = trimmed.split("\\s*,\\s*");
                if (tokens.length < 2) {
                    throw new IllegalArgumentException(errorMessage(sourceName, lineNumber, "Invalid event line: " + line));
                }

                long tick = Long.parseLong(tokens[0]);
                String action = tokens[1].toLowerCase();
                Integer floorValue = extractFloorValue(action, tokens);
                if (floorValue != null) {
                    minFloor = minFloor == null ? floorValue : Math.min(minFloor, floorValue);
                    maxFloor = maxFloor == null ? floorValue : Math.max(maxFloor, floorValue);
                }
                ScenarioEvent event = parseEvent(tick, action, tokens, sourceName, lineNumber);
                events.add(event);
            }
        }

        if (totalTicks == null) {
            throw new IllegalArgumentException("Scenario must define ticks in " + sourceName);
        }

        return new ScenarioDefinition(scenarioName, totalTicks, events, minFloor, maxFloor);
    }

    private Integer extractFloorValue(String action, String[] tokens) {
        if ("car_call".equals(action) && tokens.length >= 4) {
            return Integer.parseInt(tokens[3]);
        }
        if ("hall_call".equals(action) && tokens.length >= 4) {
            return Integer.parseInt(tokens[3]);
        }
        return null;
    }

    private ScenarioEvent parseEvent(long tick, String action, String[] tokens, String sourceName, int lineNumber) {
        return switch (action) {
            case "car_call" -> parseCarCall(tick, tokens, sourceName, lineNumber);
            case "hall_call" -> parseHallCall(tick, tokens, sourceName, lineNumber);
            case "cancel" -> parseCancel(tick, tokens, sourceName, lineNumber);
            case "out_of_service" -> new ScenarioEvent(tick, "Out of service", context -> {
                context.getController().takeOutOfService();
                context.getEngine().setOutOfService();
            });
            case "return_to_service" -> new ScenarioEvent(tick, "Return to service", context -> {
                context.getController().returnToService();
                context.getEngine().returnToService();
            });
            default -> throw new IllegalArgumentException(errorMessage(sourceName, lineNumber, "Unknown action: " + action));
        };
    }

    private ScenarioEvent parseCarCall(long tick, String[] tokens, String sourceName, int lineNumber) {
        if (tokens.length != 4) {
            throw new IllegalArgumentException(errorMessage(sourceName, lineNumber, "car_call requires tick, alias, destination"));
        }
        String alias = tokens[2];
        int destination = Integer.parseInt(tokens[3]);
        String description = String.format("Car call %s to %d", alias, destination);
        return new ScenarioEvent(tick, description, context -> {
            LiftRequest request = LiftRequest.carCall(destination);
            context.getController().addRequest(request);
            context.registerAlias(alias, request.getId());
        });
    }

    private ScenarioEvent parseHallCall(long tick, String[] tokens, String sourceName, int lineNumber) {
        if (tokens.length != 5) {
            throw new IllegalArgumentException(errorMessage(sourceName, lineNumber, "hall_call requires tick, alias, floor, direction"));
        }
        String alias = tokens[2];
        int floor = Integer.parseInt(tokens[3]);
        Direction direction = Direction.valueOf(tokens[4].toUpperCase());
        String description = String.format("Hall call %s at %d %s", alias, floor, direction);
        return new ScenarioEvent(tick, description, context -> {
            LiftRequest request = LiftRequest.hallCall(floor, direction);
            context.getController().addRequest(request);
            context.registerAlias(alias, request.getId());
        });
    }

    private ScenarioEvent parseCancel(long tick, String[] tokens, String sourceName, int lineNumber) {
        if (tokens.length != 3) {
            throw new IllegalArgumentException(errorMessage(sourceName, lineNumber, "cancel requires tick and alias"));
        }
        String alias = tokens[2];
        String description = String.format("Cancel %s", alias);
        return new ScenarioEvent(tick, description, context -> {
            Long requestId = context.resolveAlias(alias);
            if (requestId == null) {
                throw new IllegalArgumentException("Unknown request alias: " + alias);
            }
            context.getController().cancelRequest(requestId);
        });
    }

    private String errorMessage(String sourceName, int lineNumber, String message) {
        return String.format("%s (line %d): %s", sourceName, lineNumber, message);
    }
}
