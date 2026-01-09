-- Initial Schema Setup for Lift Config Service
-- Version: 1
-- Description: Baseline schema initialization

-- This migration establishes the baseline for the database schema.
-- Flyway will automatically create the flyway_schema_history table
-- to track all migrations.

-- Create schema version metadata table
CREATE TABLE IF NOT EXISTS schema_metadata (
    id SERIAL PRIMARY KEY,
    version VARCHAR(50) NOT NULL,
    description VARCHAR(255),
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert initial version record
INSERT INTO schema_metadata (version, description)
VALUES ('0.22.0', 'Initial schema setup with PostgreSQL and Flyway');

-- Create index for version lookups
CREATE INDEX idx_schema_metadata_version ON schema_metadata(version);

COMMENT ON TABLE schema_metadata IS 'Tracks application schema versions and metadata';
COMMENT ON COLUMN schema_metadata.version IS 'Application version that applied this schema';
COMMENT ON COLUMN schema_metadata.description IS 'Description of schema changes';
COMMENT ON COLUMN schema_metadata.applied_at IS 'Timestamp when schema was applied';
