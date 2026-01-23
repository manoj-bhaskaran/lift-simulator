package com.liftsimulator.admin.entity;

import com.liftsimulator.domain.ControllerStrategy;
import com.liftsimulator.domain.IdleParkingMode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JPA entity representing the scenario table.
 * Passenger flow scenario definitions for lift simulations.
 */
@Entity
@Table(name = "scenario", schema = "lift_simulator")
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "total_ticks", nullable = false)
    private Integer totalTicks;

    @Column(name = "min_floor", nullable = false)
    private Integer minFloor = 0;

    @Column(name = "max_floor", nullable = false)
    private Integer maxFloor;

    @Column(name = "initial_floor")
    private Integer initialFloor;

    @Column(name = "home_floor")
    private Integer homeFloor;

    @Column(name = "travel_ticks_per_floor", nullable = false)
    private Integer travelTicksPerFloor = 10;

    @Column(name = "door_transition_ticks", nullable = false)
    private Integer doorTransitionTicks = 5;

    @Column(name = "door_dwell_ticks", nullable = false)
    private Integer doorDwellTicks = 10;

    @Enumerated(EnumType.STRING)
    @Column(name = "controller_strategy", nullable = false, length = 50)
    private ControllerStrategy controllerStrategy = ControllerStrategy.DIRECTIONAL_SCAN;

    @Enumerated(EnumType.STRING)
    @Column(name = "idle_parking_mode", nullable = false, length = 50)
    private IdleParkingMode idleParkingMode = IdleParkingMode.STAY_PUT;

    @Column(name = "seed")
    private Long seed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "scenario", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("tick ASC, eventOrder ASC")
    private List<ScenarioEvent> events = new ArrayList<>();

    public Scenario() {
    }

    public Scenario(String name, Integer totalTicks, Integer maxFloor) {
        this.name = name;
        this.totalTicks = totalTicks;
        this.maxFloor = maxFloor;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public void addEvent(ScenarioEvent event) {
        events.add(event);
        event.setScenario(this);
    }

    public void removeEvent(ScenarioEvent event) {
        events.remove(event);
        event.setScenario(null);
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getTotalTicks() {
        return totalTicks;
    }

    public void setTotalTicks(Integer totalTicks) {
        this.totalTicks = totalTicks;
    }

    public Integer getMinFloor() {
        return minFloor;
    }

    public void setMinFloor(Integer minFloor) {
        this.minFloor = minFloor;
    }

    public Integer getMaxFloor() {
        return maxFloor;
    }

    public void setMaxFloor(Integer maxFloor) {
        this.maxFloor = maxFloor;
    }

    public Integer getInitialFloor() {
        return initialFloor;
    }

    public void setInitialFloor(Integer initialFloor) {
        this.initialFloor = initialFloor;
    }

    public Integer getHomeFloor() {
        return homeFloor;
    }

    public void setHomeFloor(Integer homeFloor) {
        this.homeFloor = homeFloor;
    }

    public Integer getTravelTicksPerFloor() {
        return travelTicksPerFloor;
    }

    public void setTravelTicksPerFloor(Integer travelTicksPerFloor) {
        this.travelTicksPerFloor = travelTicksPerFloor;
    }

    public Integer getDoorTransitionTicks() {
        return doorTransitionTicks;
    }

    public void setDoorTransitionTicks(Integer doorTransitionTicks) {
        this.doorTransitionTicks = doorTransitionTicks;
    }

    public Integer getDoorDwellTicks() {
        return doorDwellTicks;
    }

    public void setDoorDwellTicks(Integer doorDwellTicks) {
        this.doorDwellTicks = doorDwellTicks;
    }

    public ControllerStrategy getControllerStrategy() {
        return controllerStrategy;
    }

    public void setControllerStrategy(ControllerStrategy controllerStrategy) {
        this.controllerStrategy = controllerStrategy;
    }

    public IdleParkingMode getIdleParkingMode() {
        return idleParkingMode;
    }

    public void setIdleParkingMode(IdleParkingMode idleParkingMode) {
        this.idleParkingMode = idleParkingMode;
    }

    public Long getSeed() {
        return seed;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP",
            justification = "JPA entity getters must return mutable collections for change tracking. "
                    + "Use addEvent/removeEvent methods for safe modification."
    )
    public List<ScenarioEvent> getEvents() {
        return events;
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "JPA entity setters require direct field assignment for Hibernate proxies "
                    + "and lazy loading to work correctly."
    )
    public void setEvents(List<ScenarioEvent> events) {
        this.events = events;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Scenario scenario = (Scenario) o;
        return Objects.equals(id, scenario.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Scenario{"
                + "id=" + id
                + ", name='" + name + '\''
                + ", totalTicks=" + totalTicks
                + ", minFloor=" + minFloor
                + ", maxFloor=" + maxFloor
                + ", controllerStrategy=" + controllerStrategy
                + ", createdAt=" + createdAt
                + ", updatedAt=" + updatedAt
                + '}';
    }
}
