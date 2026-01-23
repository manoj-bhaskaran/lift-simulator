package com.liftsimulator.admin.service;

import com.liftsimulator.admin.dto.CreateScenarioRequest;
import com.liftsimulator.admin.dto.ScenarioEventRequest;
import com.liftsimulator.admin.dto.ScenarioResponse;
import com.liftsimulator.admin.dto.ScenarioValidationResponse;
import com.liftsimulator.admin.dto.UpdateScenarioRequest;
import com.liftsimulator.admin.dto.ValidationIssue;
import com.liftsimulator.admin.entity.EventType;
import com.liftsimulator.admin.entity.Scenario;
import com.liftsimulator.admin.entity.ScenarioEvent;
import com.liftsimulator.admin.repository.ScenarioEventRepository;
import com.liftsimulator.admin.repository.ScenarioRepository;
import com.liftsimulator.domain.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing scenarios.
 */
@Service
@Transactional(readOnly = true)
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioEventRepository scenarioEventRepository;

    public ScenarioService(
            ScenarioRepository scenarioRepository,
            ScenarioEventRepository scenarioEventRepository
    ) {
        this.scenarioRepository = scenarioRepository;
        this.scenarioEventRepository = scenarioEventRepository;
    }

    /**
     * Creates a new scenario with events.
     *
     * @param request the creation request
     * @return the created scenario
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional
    public ScenarioResponse createScenario(CreateScenarioRequest request) {
        // Validate the scenario
        ScenarioValidationResponse validation = validateScenario(request);
        if (!validation.valid()) {
            throw new IllegalArgumentException(
                "Scenario validation failed: " + formatValidationErrors(validation.errors())
            );
        }

        // Create the scenario entity
        Scenario scenario = new Scenario();
        populateScenarioFromRequest(scenario, request);

        // Add events
        if (request.events() != null) {
            int order = 0;
            for (ScenarioEventRequest eventReq : request.events()) {
                ScenarioEvent event = createEventFromRequest(eventReq);
                if (event.getEventOrder() == null || event.getEventOrder() == 0) {
                    event.setEventOrder(order++);
                }
                scenario.addEvent(event);
            }
        }

        Scenario savedScenario = scenarioRepository.save(scenario);
        return ScenarioResponse.fromEntity(savedScenario, true);
    }

    /**
     * Retrieves all scenarios.
     *
     * @return list of all scenarios
     */
    public List<ScenarioResponse> getAllScenarios() {
        List<Scenario> scenarios = scenarioRepository.findAllByOrderByCreatedAtDesc();
        return scenarios.stream()
            .map(scenario -> ScenarioResponse.fromEntity(scenario, false))
            .toList();
    }

    /**
     * Retrieves a scenario by ID with events.
     *
     * @param id the scenario ID
     * @return the scenario
     * @throws ResourceNotFoundException if not found
     */
    public ScenarioResponse getScenarioById(Long id) {
        Scenario scenario = scenarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Scenario not found with id: " + id
            ));
        return ScenarioResponse.fromEntity(scenario, true);
    }

    /**
     * Updates an existing scenario.
     *
     * @param id the scenario ID
     * @param request the update request
     * @return the updated scenario
     * @throws ResourceNotFoundException if not found
     * @throws IllegalArgumentException if validation fails
     */
    @Transactional
    public ScenarioResponse updateScenario(Long id, UpdateScenarioRequest request) {
        Scenario scenario = scenarioRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Scenario not found with id: " + id
            ));

        // Validate the scenario
        ScenarioValidationResponse validation = validateScenarioUpdate(request);
        if (!validation.valid()) {
            throw new IllegalArgumentException(
                "Scenario validation failed: " + formatValidationErrors(validation.errors())
            );
        }

        // Update scenario fields
        populateScenarioFromUpdateRequest(scenario, request);

        // Clear existing events and add new ones
        scenario.getEvents().clear();
        if (request.events() != null) {
            int order = 0;
            for (ScenarioEventRequest eventReq : request.events()) {
                ScenarioEvent event = createEventFromRequest(eventReq);
                if (event.getEventOrder() == null || event.getEventOrder() == 0) {
                    event.setEventOrder(order++);
                }
                scenario.addEvent(event);
            }
        }

        Scenario updatedScenario = scenarioRepository.save(scenario);
        return ScenarioResponse.fromEntity(updatedScenario, true);
    }

    /**
     * Deletes a scenario.
     *
     * @param id the scenario ID
     * @throws ResourceNotFoundException if not found
     */
    @Transactional
    public void deleteScenario(Long id) {
        if (!scenarioRepository.existsById(id)) {
            throw new ResourceNotFoundException("Scenario not found with id: " + id);
        }
        scenarioRepository.deleteById(id);
    }

    /**
     * Validates a scenario creation request.
     *
     * @param request the creation request
     * @return validation response
     */
    public ScenarioValidationResponse validateScenario(CreateScenarioRequest request) {
        List<ValidationIssue> errors = new ArrayList<>();
        List<ValidationIssue> warnings = new ArrayList<>();

        // Validate floor range
        if (request.maxFloor() <= request.minFloor()) {
            errors.add(new ValidationIssue(
                "maxFloor",
                "Max floor must be greater than min floor",
                "FLOOR_RANGE_INVALID"
            ));
        }

        // Validate initial floor
        if (request.initialFloor() != null) {
            if (request.initialFloor() < request.minFloor()
                    || request.initialFloor() > request.maxFloor()) {
                errors.add(new ValidationIssue(
                    "initialFloor",
                    "Initial floor must be within min and max floor range",
                    "INITIAL_FLOOR_OUT_OF_RANGE"
                ));
            }
        }

        // Validate home floor
        if (request.homeFloor() != null) {
            if (request.homeFloor() < request.minFloor()
                    || request.homeFloor() > request.maxFloor()) {
                errors.add(new ValidationIssue(
                    "homeFloor",
                    "Home floor must be within min and max floor range",
                    "HOME_FLOOR_OUT_OF_RANGE"
                ));
            }
        }

        // Validate events
        if (request.events() != null) {
            validateEvents(request.events(), request.totalTicks(),
                    request.minFloor(), request.maxFloor(), errors, warnings);
        }

        return ScenarioValidationResponse.withIssues(errors, warnings);
    }

    /**
     * Validates a scenario update request.
     *
     * @param request the update request
     * @return validation response
     */
    public ScenarioValidationResponse validateScenarioUpdate(UpdateScenarioRequest request) {
        List<ValidationIssue> errors = new ArrayList<>();
        List<ValidationIssue> warnings = new ArrayList<>();

        // Validate floor range
        if (request.maxFloor() <= request.minFloor()) {
            errors.add(new ValidationIssue(
                "maxFloor",
                "Max floor must be greater than min floor",
                "FLOOR_RANGE_INVALID"
            ));
        }

        // Validate initial floor
        if (request.initialFloor() != null) {
            if (request.initialFloor() < request.minFloor()
                    || request.initialFloor() > request.maxFloor()) {
                errors.add(new ValidationIssue(
                    "initialFloor",
                    "Initial floor must be within min and max floor range",
                    "INITIAL_FLOOR_OUT_OF_RANGE"
                ));
            }
        }

        // Validate home floor
        if (request.homeFloor() != null) {
            if (request.homeFloor() < request.minFloor()
                    || request.homeFloor() > request.maxFloor()) {
                errors.add(new ValidationIssue(
                    "homeFloor",
                    "Home floor must be within min and max floor range",
                    "HOME_FLOOR_OUT_OF_RANGE"
                ));
            }
        }

        // Validate events
        if (request.events() != null) {
            validateEvents(request.events(), request.totalTicks(),
                    request.minFloor(), request.maxFloor(), errors, warnings);
        }

        return ScenarioValidationResponse.withIssues(errors, warnings);
    }

    private void validateEvents(
            List<ScenarioEventRequest> events,
            Integer totalTicks,
            Integer minFloor,
            Integer maxFloor,
            List<ValidationIssue> errors,
            List<ValidationIssue> warnings
    ) {
        for (int i = 0; i < events.size(); i++) {
            ScenarioEventRequest event = events.get(i);
            String fieldPrefix = "events[" + i + "]";

            // Validate tick is within scenario duration
            if (event.tick() >= totalTicks) {
                errors.add(new ValidationIssue(
                    fieldPrefix + ".tick",
                    "Event tick " + event.tick() + " exceeds scenario duration " + totalTicks,
                    "EVENT_TICK_OUT_OF_RANGE"
                ));
            }

            // Validate event-specific fields
            if (event.eventType() == EventType.HALL_CALL) {
                if (event.originFloor() == null) {
                    errors.add(new ValidationIssue(
                        fieldPrefix + ".originFloor",
                        "Hall call must have an origin floor",
                        "MISSING_ORIGIN_FLOOR"
                    ));
                } else if (event.originFloor() < minFloor || event.originFloor() > maxFloor) {
                    errors.add(new ValidationIssue(
                        fieldPrefix + ".originFloor",
                        "Origin floor must be within min and max floor range",
                        "ORIGIN_FLOOR_OUT_OF_RANGE"
                    ));
                }

                if (event.direction() == null || event.direction() == Direction.IDLE) {
                    errors.add(new ValidationIssue(
                        fieldPrefix + ".direction",
                        "Hall call must have a direction (UP or DOWN)",
                        "MISSING_DIRECTION"
                    ));
                }
            } else if (event.eventType() == EventType.CAR_CALL) {
                if (event.destinationFloor() == null) {
                    errors.add(new ValidationIssue(
                        fieldPrefix + ".destinationFloor",
                        "Car call must have a destination floor",
                        "MISSING_DESTINATION_FLOOR"
                    ));
                } else if (event.destinationFloor() < minFloor
                        || event.destinationFloor() > maxFloor) {
                    errors.add(new ValidationIssue(
                        fieldPrefix + ".destinationFloor",
                        "Destination floor must be within min and max floor range",
                        "DESTINATION_FLOOR_OUT_OF_RANGE"
                    ));
                }
            }
        }
    }

    private void populateScenarioFromRequest(Scenario scenario, CreateScenarioRequest request) {
        scenario.setName(request.name());
        scenario.setDescription(request.description());
        scenario.setTotalTicks(request.totalTicks());
        scenario.setMinFloor(request.minFloor());
        scenario.setMaxFloor(request.maxFloor());
        scenario.setInitialFloor(request.initialFloor());
        scenario.setHomeFloor(request.homeFloor());
        scenario.setTravelTicksPerFloor(
            request.travelTicksPerFloor() != null ? request.travelTicksPerFloor() : 10
        );
        scenario.setDoorTransitionTicks(
            request.doorTransitionTicks() != null ? request.doorTransitionTicks() : 5
        );
        scenario.setDoorDwellTicks(
            request.doorDwellTicks() != null ? request.doorDwellTicks() : 10
        );
        scenario.setControllerStrategy(
            request.controllerStrategy() != null
                ? request.controllerStrategy()
                : scenario.getControllerStrategy()
        );
        scenario.setIdleParkingMode(
            request.idleParkingMode() != null
                ? request.idleParkingMode()
                : scenario.getIdleParkingMode()
        );
        scenario.setSeed(request.seed());
    }

    private void populateScenarioFromUpdateRequest(
            Scenario scenario,
            UpdateScenarioRequest request
    ) {
        scenario.setName(request.name());
        scenario.setDescription(request.description());
        scenario.setTotalTicks(request.totalTicks());
        scenario.setMinFloor(request.minFloor());
        scenario.setMaxFloor(request.maxFloor());
        scenario.setInitialFloor(request.initialFloor());
        scenario.setHomeFloor(request.homeFloor());
        scenario.setTravelTicksPerFloor(
            request.travelTicksPerFloor() != null ? request.travelTicksPerFloor() : 10
        );
        scenario.setDoorTransitionTicks(
            request.doorTransitionTicks() != null ? request.doorTransitionTicks() : 5
        );
        scenario.setDoorDwellTicks(
            request.doorDwellTicks() != null ? request.doorDwellTicks() : 10
        );
        scenario.setControllerStrategy(
            request.controllerStrategy() != null
                ? request.controllerStrategy()
                : scenario.getControllerStrategy()
        );
        scenario.setIdleParkingMode(
            request.idleParkingMode() != null
                ? request.idleParkingMode()
                : scenario.getIdleParkingMode()
        );
        scenario.setSeed(request.seed());
    }

    private ScenarioEvent createEventFromRequest(ScenarioEventRequest request) {
        ScenarioEvent event = new ScenarioEvent();
        event.setTick(request.tick());
        event.setEventType(request.eventType());
        event.setDescription(request.description());
        event.setOriginFloor(request.originFloor());
        event.setDestinationFloor(request.destinationFloor());
        event.setDirection(request.direction());
        event.setEventOrder(request.eventOrder() != null ? request.eventOrder() : 0);
        return event;
    }

    private String formatValidationErrors(List<ValidationIssue> errors) {
        return errors.stream()
            .map(e -> e.field() + ": " + e.message())
            .reduce((a, b) -> a + ", " + b)
            .orElse("Unknown validation error");
    }
}
