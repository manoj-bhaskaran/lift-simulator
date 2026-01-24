package com.liftsimulator.admin.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * JPA entity representing the scenario table.
 * Stores passenger flow scenarios for UI-driven simulations.
 * Each scenario is tied to a specific lift system version to ensure floor range validation.
 */
@Entity
@Table(name = "scenario", schema = "lift_simulator")
public class Scenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "scenario_json", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String scenarioJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lift_system_version_id")
    private LiftSystemVersion liftSystemVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Scenario() {
    }

    public Scenario(String name, String scenarioJson) {
        this.name = name;
        this.scenarioJson = scenarioJson;
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "JPA entity with managed relationship. "
                    + "Direct reference required for ORM functionality and lazy loading."
    )
    public Scenario(String name, String scenarioJson, LiftSystemVersion liftSystemVersion) {
        this.name = name;
        this.scenarioJson = scenarioJson;
        this.liftSystemVersion = liftSystemVersion;
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

    public String getScenarioJson() {
        return scenarioJson;
    }

    public void setScenarioJson(String scenarioJson) {
        this.scenarioJson = scenarioJson;
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
            justification = "JPA entity with managed relationship. "
                    + "Direct reference required for ORM functionality and lazy loading."
    )
    public LiftSystemVersion getLiftSystemVersion() {
        return liftSystemVersion;
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "JPA entity with managed relationship. "
                    + "Direct reference required for ORM functionality and lazy loading."
    )
    public void setLiftSystemVersion(LiftSystemVersion liftSystemVersion) {
        this.liftSystemVersion = liftSystemVersion;
    }
}
