-- Add optimistic locking columns and enforce a single published version per lift system
-- Version: 10
-- Description: Prevent concurrent run lifecycle and version publication/allocation races.

SET search_path TO lift_simulator;

ALTER TABLE simulation_run
    ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE lift_system_version
    ADD COLUMN IF NOT EXISTS lock_version BIGINT NOT NULL DEFAULT 0;

-- Historical archive rows may still have is_published=true from earlier archive behavior.
UPDATE lift_system_version
SET is_published = FALSE
WHERE status = 'ARCHIVED' AND is_published = TRUE;

DROP INDEX IF EXISTS idx_lift_system_version_published;

CREATE UNIQUE INDEX IF NOT EXISTS uq_lift_system_version_single_published
    ON lift_system_version(lift_system_id)
    WHERE is_published = TRUE;

COMMENT ON COLUMN simulation_run.lock_version IS 'JPA optimistic-lock version for run lifecycle transitions';
COMMENT ON COLUMN lift_system_version.lock_version IS 'JPA optimistic-lock version for version publication and configuration updates';
