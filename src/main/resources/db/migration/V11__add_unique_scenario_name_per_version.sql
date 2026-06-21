-- Enforce unique scenario names per lift system version
-- Version: 11
-- Description: Prevents duplicate scenario names within the same lift system version,
--              mirroring the uniqueness already enforced for lift_system.system_key and
--              lift_system_version(lift_system_id, version_number).

SET search_path TO lift_simulator;

-- Existing databases may already contain duplicate scenario names within a version.
-- Keep the earliest scenario (lowest id) untouched and rename later duplicates with a
-- " (#<id>)" suffix, truncating the base name so the result fits the 200-char column.
WITH ranked_scenarios AS (
    SELECT
        id,
        name,
        ROW_NUMBER() OVER (
            PARTITION BY lift_system_version_id, name
            ORDER BY id
        ) AS name_rank
    FROM scenario
)
UPDATE scenario AS s
SET name = LEFT(s.name, 200 - LENGTH(' (#' || s.id || ')')) || ' (#' || s.id || ')'
FROM ranked_scenarios AS ranked
WHERE s.id = ranked.id
  AND ranked.name_rank > 1;

-- Replace the non-unique index from V6 with a unique index on (version, name).
DROP INDEX IF EXISTS idx_scenario_version;

CREATE UNIQUE INDEX IF NOT EXISTS uq_scenario_version_name
    ON scenario(lift_system_version_id, name);

COMMENT ON INDEX uq_scenario_version_name IS 'Ensures scenario names are unique within a lift system version';
