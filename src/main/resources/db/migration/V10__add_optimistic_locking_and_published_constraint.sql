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

-- Databases that already experienced concurrent publish races may have more than
-- one published row per lift system. Keep the most recently published/highest
-- numbered version as published and archive the rest before adding the unique
-- partial index below.
WITH ranked_published_versions AS (
    SELECT
        id,
        ROW_NUMBER() OVER (
            PARTITION BY lift_system_id
            ORDER BY published_at DESC NULLS LAST, version_number DESC, id DESC
        ) AS publish_rank
    FROM lift_system_version
    WHERE is_published = TRUE
)
UPDATE lift_system_version AS version
SET
    status = CASE
        WHEN ranked.publish_rank = 1 THEN 'PUBLISHED'
        ELSE 'ARCHIVED'
    END,
    is_published = (ranked.publish_rank = 1)
FROM ranked_published_versions AS ranked
WHERE version.id = ranked.id
  AND (
      ranked.publish_rank > 1
      OR version.status <> 'PUBLISHED'
      OR version.is_published IS DISTINCT FROM TRUE
  );

DROP INDEX IF EXISTS idx_lift_system_version_published;

CREATE UNIQUE INDEX IF NOT EXISTS uq_lift_system_version_single_published
    ON lift_system_version(lift_system_id)
    WHERE is_published = TRUE;

COMMENT ON COLUMN simulation_run.lock_version IS 'JPA optimistic-lock version for run lifecycle transitions';
COMMENT ON COLUMN lift_system_version.lock_version IS 'JPA optimistic-lock version for version publication and configuration updates';
