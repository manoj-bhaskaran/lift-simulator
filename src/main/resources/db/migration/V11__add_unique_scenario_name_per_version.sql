-- Enforce unique scenario names per lift system version
-- Version: 11
-- Description: Prevents duplicate scenario names within the same lift system version,
--              mirroring the uniqueness already enforced for lift_system.system_key and
--              lift_system_version(lift_system_id, version_number).

SET search_path TO lift_simulator;

-- Existing databases may already contain duplicate scenario names within a version.
-- Keep the earliest scenario (lowest id) untouched and rename later duplicates with a
-- " (#<n>)" suffix, truncating the base name so the result fits the 200-char column.
-- The generated name is checked against ALL existing names in the same version (not just
-- the duplicate group), incrementing the suffix until it is unique, so a pre-existing
-- row such as "Morning (#2)" cannot collide with a renamed duplicate before the unique
-- index is created.
DO $$
DECLARE
    dup RECORD;
    candidate TEXT;
    suffix BIGINT;
BEGIN
    LOOP
        -- Pick one row that shares its (version, name) with an earlier row (lower id).
        SELECT s.id, s.name, s.lift_system_version_id
        INTO dup
        FROM scenario s
        WHERE EXISTS (
            SELECT 1
            FROM scenario o
            WHERE o.lift_system_version_id = s.lift_system_version_id
              AND o.name = s.name
              AND o.id < s.id
        )
        ORDER BY s.lift_system_version_id, s.name, s.id
        LIMIT 1;

        EXIT WHEN NOT FOUND;

        -- Find a suffix that yields a name unique within the version.
        suffix := dup.id;
        LOOP
            candidate := LEFT(dup.name, 200 - LENGTH(' (#' || suffix || ')')) || ' (#' || suffix || ')';
            EXIT WHEN NOT EXISTS (
                SELECT 1
                FROM scenario o
                WHERE o.lift_system_version_id = dup.lift_system_version_id
                  AND o.name = candidate
            );
            suffix := suffix + 1;
        END LOOP;

        UPDATE scenario SET name = candidate WHERE id = dup.id;
    END LOOP;
END $$;

-- Replace the non-unique index from V6 with a unique index on (version, name).
DROP INDEX IF EXISTS idx_scenario_version;

CREATE UNIQUE INDEX IF NOT EXISTS uq_scenario_version_name
    ON scenario(lift_system_version_id, name);

COMMENT ON INDEX uq_scenario_version_name IS 'Ensures scenario names are unique within a lift system version';
