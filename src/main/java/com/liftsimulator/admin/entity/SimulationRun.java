package com.liftsimulator.admin.entity;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * JPA entity representing the simulation_run table.
 * Individual simulation run executions with lifecycle tracking.
 */
@Entity
@Table(name = "simulation_run", schema = "lift_simulator")
public class SimulationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lift_system_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private LiftSystem liftSystem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private LiftSystemVersion version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Scenario scenario;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RunStatus status = RunStatus.CREATED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "total_ticks")
    private Long totalTicks;

    @Column(name = "current_tick")
    private Long currentTick = 0L;

    @Column(name = "seed")
    private Long seed;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "artefact_base_path", length = 500)
    private String artefactBasePath;

    public SimulationRun() {
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "JPA entity constructor must accept and store mutable parent entities "
                    + "for relationship management."
    )
    public SimulationRun(LiftSystem liftSystem, LiftSystemVersion version) {
        this.liftSystem = liftSystem;
        this.version = version;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    /**
     * Starts the simulation run.
     * Transitions from CREATED to RUNNING.
     */
    public void start() {
        if (status != RunStatus.CREATED) {
            throw new IllegalStateException("Can only start a run in CREATED state. Current state: " + status);
        }
        this.status = RunStatus.RUNNING;
        this.startedAt = OffsetDateTime.now();
    }

    /**
     * Marks the simulation run as succeeded.
     * Transitions from RUNNING to SUCCEEDED.
     */
    public void succeed() {
        if (status != RunStatus.RUNNING) {
            throw new IllegalStateException("Can only succeed a run in RUNNING state. Current state: " + status);
        }
        this.status = RunStatus.SUCCEEDED;
        this.endedAt = OffsetDateTime.now();
    }

    /**
     * Marks the simulation run as failed.
     * Transitions from RUNNING to FAILED.
     *
     * @param errorMessage The error message describing the failure
     */
    public void fail(String errorMessage) {
        if (status != RunStatus.RUNNING) {
            throw new IllegalStateException("Can only fail a run in RUNNING state. Current state: " + status);
        }
        this.status = RunStatus.FAILED;
        this.errorMessage = errorMessage;
        this.endedAt = OffsetDateTime.now();
    }

    /**
     * Cancels the simulation run.
     * Can transition from CREATED or RUNNING to CANCELLED.
     */
    public void cancel() {
        if (status != RunStatus.CREATED && status != RunStatus.RUNNING) {
            throw new IllegalStateException(
                    "Can only cancel a run in CREATED or RUNNING state. Current state: " + status);
        }
        this.status = RunStatus.CANCELLED;
        this.endedAt = OffsetDateTime.now();
    }

    /**
     * Updates the current progress tick.
     *
     * @param tick The current tick number
     */
    public void updateProgress(Long tick) {
        this.currentTick = tick;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP",
            justification = "JPA entity getters must return mutable parent entity for "
                    + "relationship navigation and lazy loading."
    )
    public LiftSystem getLiftSystem() {
        return liftSystem;
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "JPA entity setters require direct field assignment for Hibernate "
                    + "to manage relationships and lazy loading correctly."
    )
    public void setLiftSystem(LiftSystem liftSystem) {
        this.liftSystem = liftSystem;
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP",
            justification = "JPA entity getters must return mutable parent entity for "
                    + "relationship navigation and lazy loading."
    )
    public LiftSystemVersion getVersion() {
        return version;
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "JPA entity setters require direct field assignment for Hibernate "
                    + "to manage relationships and lazy loading correctly."
    )
    public void setVersion(LiftSystemVersion version) {
        this.version = version;
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP",
            justification = "JPA entity getters must return mutable parent entity for "
                    + "relationship navigation and lazy loading."
    )
    public Scenario getScenario() {
        return scenario;
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "JPA entity setters require direct field assignment for Hibernate "
                    + "to manage relationships and lazy loading correctly."
    )
    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    public RunStatus getStatus() {
        return status;
    }

    public void setStatus(RunStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(OffsetDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public OffsetDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(OffsetDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public Long getTotalTicks() {
        return totalTicks;
    }

    public void setTotalTicks(Long totalTicks) {
        this.totalTicks = totalTicks;
    }

    public Long getCurrentTick() {
        return currentTick;
    }

    public void setCurrentTick(Long currentTick) {
        this.currentTick = currentTick;
    }

    public Long getSeed() {
        return seed;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getArtefactBasePath() {
        return artefactBasePath;
    }

    public void setArtefactBasePath(String artefactBasePath) {
        this.artefactBasePath = artefactBasePath;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SimulationRun that = (SimulationRun) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SimulationRun{"
                + "id=" + id
                + ", status=" + status
                + ", createdAt=" + createdAt
                + ", startedAt=" + startedAt
                + ", endedAt=" + endedAt
                + ", totalTicks=" + totalTicks
                + ", currentTick=" + currentTick
                + ", seed=" + seed
                + ", artefactBasePath='" + artefactBasePath + '\''
                + '}';
    }

    /**
     * Enumeration of possible run statuses.
     */
    public enum RunStatus {
        CREATED,
        RUNNING,
        SUCCEEDED,
        FAILED,
        CANCELLED
    }
}
