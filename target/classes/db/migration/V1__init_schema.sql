-- Initial Schema Setup for Lift Config Service
-- Version: 1
-- Description: Baseline schema initialization for lift configuration storage

CREATE SCHEMA IF NOT EXISTS lift_simulator;

SET search_path TO lift_simulator;

CREATE TABLE IF NOT EXISTS lift_system (
    id BIGSERIAL PRIMARY KEY,
    system_key VARCHAR(120) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uq_lift_system_system_key ON lift_system(system_key);

CREATE TABLE IF NOT EXISTS lift_system_version (
    id BIGSERIAL PRIMARY KEY,
    lift_system_id BIGINT NOT NULL REFERENCES lift_system(id) ON DELETE CASCADE,
    version_number INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMPTZ,
    config JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_lift_system_version_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

CREATE UNIQUE INDEX uq_lift_system_version_number
    ON lift_system_version(lift_system_id, version_number);

CREATE INDEX idx_lift_system_version_status
    ON lift_system_version(status);

CREATE INDEX idx_lift_system_version_published
    ON lift_system_version(lift_system_id, is_published)
    WHERE is_published = TRUE;

CREATE INDEX idx_lift_system_version_config
    ON lift_system_version USING GIN (config);

COMMENT ON SCHEMA lift_simulator IS 'Schema for lift configuration and versioning';
COMMENT ON TABLE lift_system IS 'Lift system configuration root records';
COMMENT ON TABLE lift_system_version IS 'Versioned lift system configuration payloads';
COMMENT ON COLUMN lift_system.system_key IS 'Stable identifier for a lift system configuration';
COMMENT ON COLUMN lift_system_version.status IS 'Workflow status for the version';
COMMENT ON COLUMN lift_system_version.config IS 'JSON configuration payload for the lift system';
