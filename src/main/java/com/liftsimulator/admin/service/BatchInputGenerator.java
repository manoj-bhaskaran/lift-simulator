package com.liftsimulator.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liftsimulator.admin.dto.LiftConfigDTO;
import com.liftsimulator.admin.dto.PassengerFlowDTO;
import com.liftsimulator.admin.dto.ScenarioDefinitionDTO;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Generates batch simulator input files in the legacy .scenario format.
 * This generator produces backwards-compatible input files that can be consumed
 * by the existing CLI simulator (ScenarioRunnerMain).
 *
 * <p>The generator takes lift system configuration from LiftSystemVersion and
 * passenger flow definitions from ScenarioDefinitionDTO, producing a .scenario file
 * that adheres to the exact format expected by ScenarioParser.
 */
@Service
public class BatchInputGenerator {

    private final ObjectMapper objectMapper;

    public BatchInputGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Generates a batch input file from lift configuration and scenario definition.
     *
     * @param liftConfigJson JSON string containing LiftConfigDTO configuration
     * @param scenarioDefinition Scenario definition with passenger flows
     * @param scenarioName Name to use in the generated scenario file
     * @param outputPath Path where the .scenario file should be written
     * @throws IOException if file writing fails
     * @throws IllegalArgumentException if configuration or scenario is invalid
     */
    public void generateBatchInputFile(
            String liftConfigJson,
            ScenarioDefinitionDTO scenarioDefinition,
            String scenarioName,
            Path outputPath
    ) throws IOException {
        LiftConfigDTO liftConfig = parseLiftConfig(liftConfigJson);
        validateScenarioAgainstConfig(scenarioDefinition, liftConfig);

        String content = generateScenarioContent(liftConfig, scenarioDefinition, scenarioName);
        writeScenarioFile(outputPath, content);
    }

    /**
     * Generates scenario content as a string without writing to file.
     * Useful for testing and validation.
     */
    public String generateScenarioContent(
            LiftConfigDTO liftConfig,
            ScenarioDefinitionDTO scenarioDefinition,
            String scenarioName
    ) {
        StringBuilder sb = new StringBuilder();

        appendHeader(sb, scenarioName);
        appendConfiguration(sb, liftConfig, scenarioDefinition.durationTicks());
        appendEvents(sb, scenarioDefinition.passengerFlows());

        return sb.toString();
    }

    private LiftConfigDTO parseLiftConfig(String liftConfigJson) throws IOException {
        try {
            return objectMapper.readValue(liftConfigJson, LiftConfigDTO.class);
        } catch (IOException e) {
            throw new IOException("Failed to parse lift configuration JSON: " + e.getMessage(), e);
        }
    }

    private void validateScenarioAgainstConfig(
            ScenarioDefinitionDTO scenario,
            LiftConfigDTO config
    ) {
        for (PassengerFlowDTO flow : scenario.passengerFlows()) {
            if (flow.originFloor() < config.minFloor() || flow.originFloor() > config.maxFloor()) {
                throw new IllegalArgumentException(
                        String.format(
                                "Origin floor %d is outside configured floor range [%d, %d]",
                                flow.originFloor(),
                                config.minFloor(),
                                config.maxFloor()
                        )
                );
            }
            if (flow.destinationFloor() < config.minFloor()
                    || flow.destinationFloor() > config.maxFloor()) {
                throw new IllegalArgumentException(
                        String.format(
                                "Destination floor %d is outside configured floor range [%d, %d]",
                                flow.destinationFloor(),
                                config.minFloor(),
                                config.maxFloor()
                        )
                );
            }
            if (flow.startTick() >= scenario.durationTicks()) {
                throw new IllegalArgumentException(
                        String.format(
                                "Passenger flow start tick %d must be less than duration %d",
                                flow.startTick(),
                                scenario.durationTicks()
                        )
                );
            }
        }
    }

    private void appendHeader(StringBuilder sb, String scenarioName) {
        sb.append("name: ").append(scenarioName).append("\n");
    }

    private void appendConfiguration(
            StringBuilder sb,
            LiftConfigDTO config,
            int durationTicks
    ) {
        sb.append("ticks: ").append(durationTicks).append("\n");
        sb.append("min_floor: ").append(config.minFloor()).append("\n");
        sb.append("max_floor: ").append(config.maxFloor()).append("\n");
        sb.append("initial_floor: ").append(config.homeFloor()).append("\n");
        sb.append("travel_ticks_per_floor: ").append(config.travelTicksPerFloor()).append("\n");
        sb.append("door_transition_ticks: ").append(config.doorTransitionTicks()).append("\n");
        sb.append("door_dwell_ticks: ").append(config.doorDwellTicks()).append("\n");
        sb.append("door_reopen_window_ticks: ").append(config.doorReopenWindowTicks()).append("\n");
        sb.append("home_floor: ").append(config.homeFloor()).append("\n");
        sb.append("idle_timeout_ticks: ").append(config.idleTimeoutTicks()).append("\n");
        sb.append("controller_strategy: ").append(config.controllerStrategy().name()).append("\n");
        sb.append("idle_parking_mode: ").append(config.idleParkingMode().name()).append("\n");
        sb.append("\n");
    }

    private void appendEvents(StringBuilder sb, List<PassengerFlowDTO> passengerFlows) {
        List<HallCallEvent> events = generateHallCallEvents(passengerFlows);

        events.sort(Comparator.comparingInt(HallCallEvent::tick)
                .thenComparing(HallCallEvent::alias));

        for (HallCallEvent event : events) {
            sb.append(event.tick())
              .append(", hall_call, ")
              .append(event.alias())
              .append(", ")
              .append(event.originFloor())
              .append(", ")
              .append(event.direction())
              .append("\n");
        }
    }

    private List<HallCallEvent> generateHallCallEvents(List<PassengerFlowDTO> passengerFlows) {
        List<HallCallEvent> events = new ArrayList<>();
        int passengerCounter = 1;

        for (PassengerFlowDTO flow : passengerFlows) {
            String direction = determineDirection(flow.originFloor(), flow.destinationFloor());

            for (int i = 0; i < flow.passengers(); i++) {
                String alias = String.format(Locale.ROOT, "p%d", passengerCounter++);
                events.add(new HallCallEvent(
                        flow.startTick(),
                        alias,
                        flow.originFloor(),
                        direction
                ));
            }
        }

        return events;
    }

    private String determineDirection(int originFloor, int destinationFloor) {
        return destinationFloor > originFloor ? "UP" : "DOWN";
    }

    private void writeScenarioFile(Path outputPath, String content) throws IOException {
        Path parentDir = outputPath.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }

        Files.writeString(
                outputPath,
                content,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }

    /**
     * Internal record representing a hall call event in the batch input format.
     */
    private record HallCallEvent(int tick, String alias, int originFloor, String direction) {
    }
}
