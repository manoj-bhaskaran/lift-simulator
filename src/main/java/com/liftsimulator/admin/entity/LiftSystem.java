package com.liftsimulator.admin.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JPA entity representing the lift_system table.
 * Root configuration records for lift systems.
 */
@Entity
@Table(name = "lift_system", schema = "lift_simulator")
public class LiftSystem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "system_key", nullable = false, unique = true, length = 120)
    private String systemKey;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "liftSystem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LiftSystemVersion> versions = new ArrayList<>();

    public LiftSystem() {
    }

    public LiftSystem(String systemKey, String displayName, String description) {
        this.systemKey = systemKey;
        this.displayName = displayName;
        this.description = description;
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

    public void addVersion(LiftSystemVersion version) {
        versions.add(version);
        version.setLiftSystem(this);
    }

    public void removeVersion(LiftSystemVersion version) {
        versions.remove(version);
        version.setLiftSystem(null);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSystemKey() {
        return systemKey;
    }

    public void setSystemKey(String systemKey) {
        this.systemKey = systemKey;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public List<LiftSystemVersion> getVersions() {
        return versions;
    }

    public void setVersions(List<LiftSystemVersion> versions) {
        this.versions = versions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LiftSystem that = (LiftSystem) o;
        return Objects.equals(systemKey, that.systemKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(systemKey);
    }

    @Override
    public String toString() {
        return "LiftSystem{"
                + "id=" + id
                + ", systemKey='" + systemKey + '\''
                + ", displayName='" + displayName + '\''
                + ", description='" + description + '\''
                + ", createdAt=" + createdAt
                + ", updatedAt=" + updatedAt
                + '}';
    }
}
