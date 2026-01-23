package com.liftsimulator.admin.entity;

import com.liftsimulator.domain.Direction;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * JPA entity representing the scenario_event table.
 * Individual events (hall calls, car calls, cancellations) within scenarios.
 */
@Entity
@Table(name = "scenario_event", schema = "lift_simulator")
public class ScenarioEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", nullable = false)
    private Scenario scenario;

    @Column(name = "tick", nullable = false)
    private Long tick;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private EventType eventType;

    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "origin_floor")
    private Integer originFloor;

    @Column(name = "destination_floor")
    private Integer destinationFloor;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", length = 10)
    private Direction direction;

    @Column(name = "event_order", nullable = false)
    private Integer eventOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public ScenarioEvent() {
    }

    public ScenarioEvent(Long tick, EventType eventType, String description) {
        this.tick = tick;
        this.eventType = eventType;
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    public Long getTick() {
        return tick;
    }

    public void setTick(Long tick) {
        this.tick = tick;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getOriginFloor() {
        return originFloor;
    }

    public void setOriginFloor(Integer originFloor) {
        this.originFloor = originFloor;
    }

    public Integer getDestinationFloor() {
        return destinationFloor;
    }

    public void setDestinationFloor(Integer destinationFloor) {
        this.destinationFloor = destinationFloor;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public Integer getEventOrder() {
        return eventOrder;
    }

    public void setEventOrder(Integer eventOrder) {
        this.eventOrder = eventOrder;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ScenarioEvent that = (ScenarioEvent) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ScenarioEvent{"
                + "id=" + id
                + ", tick=" + tick
                + ", eventType=" + eventType
                + ", description='" + description + '\''
                + ", originFloor=" + originFloor
                + ", destinationFloor=" + destinationFloor
                + ", direction=" + direction
                + ", eventOrder=" + eventOrder
                + ", createdAt=" + createdAt
                + '}';
    }
}
