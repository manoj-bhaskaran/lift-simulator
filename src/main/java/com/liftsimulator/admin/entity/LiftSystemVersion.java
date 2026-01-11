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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * JPA entity representing the lift_system_version table.
 * Versioned lift system configuration payloads.
 */
@Entity
@Table(
    name = "lift_system_version",
    schema = "lift_simulator",
    uniqueConstraints = @UniqueConstraint(columnNames = {"lift_system_id", "version_number"})
)
public class LiftSystemVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lift_system_id", nullable = false)
    private LiftSystem liftSystem;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VersionStatus status = VersionStatus.DRAFT;

    @Column(name = "is_published", nullable = false)
    private Boolean isPublished = false;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", nullable = false, columnDefinition = "jsonb")
    private String config;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public LiftSystemVersion() {
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "JPA entity constructor must accept and store mutable parent entity "
                    + "for bidirectional relationship management."
    )
    public LiftSystemVersion(LiftSystem liftSystem, Integer versionNumber, String config) {
        this.liftSystem = liftSystem;
        this.versionNumber = versionNumber;
        this.config = config;
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

    public void publish() {
        this.status = VersionStatus.PUBLISHED;
        this.isPublished = true;
        this.publishedAt = OffsetDateTime.now();
    }

    public void archive() {
        this.status = VersionStatus.ARCHIVED;
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
                    + "bidirectional relationship navigation and lazy loading."
    )
    public LiftSystem getLiftSystem() {
        return liftSystem;
    }

    @SuppressFBWarnings(
            value = "EI_EXPOSE_REP2",
            justification = "JPA entity setters require direct field assignment for Hibernate "
                    + "to manage bidirectional relationships and lazy loading correctly."
    )
    public void setLiftSystem(LiftSystem liftSystem) {
        this.liftSystem = liftSystem;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public VersionStatus getStatus() {
        return status;
    }

    public void setStatus(VersionStatus status) {
        this.status = status;
    }

    public Boolean getIsPublished() {
        return isPublished;
    }

    public void setIsPublished(Boolean isPublished) {
        this.isPublished = isPublished;
    }

    public OffsetDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(OffsetDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LiftSystemVersion that = (LiftSystemVersion) o;
        return Objects.equals(liftSystem, that.liftSystem)
                && Objects.equals(versionNumber, that.versionNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(liftSystem, versionNumber);
    }

    @Override
    public String toString() {
        return "LiftSystemVersion{"
                + "id=" + id
                + ", versionNumber=" + versionNumber
                + ", status=" + status
                + ", isPublished=" + isPublished
                + ", publishedAt=" + publishedAt
                + ", createdAt=" + createdAt
                + ", updatedAt=" + updatedAt
                + '}';
    }

    public enum VersionStatus {
        DRAFT,
        PUBLISHED,
        ARCHIVED
    }
}
