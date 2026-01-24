-- Migrate lift configuration JSON payloads from floors to minFloor/maxFloor
-- Version: 2
-- Description: Introduce explicit floor ranges for runtime configurations

SET search_path TO lift_simulator;

UPDATE lift_system_version
SET config = jsonb_set(
    jsonb_set(config - 'floors', '{minFloor}', '0'::jsonb, true),
    '{maxFloor}', to_jsonb((config->>'floors')::int - 1), true
)
WHERE config ? 'floors';
