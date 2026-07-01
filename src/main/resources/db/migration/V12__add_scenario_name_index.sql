-- Add index on scenario.name for efficient filtering
-- Version: 12
-- Description: Prevents sequential scans when filtering scenarios by name

SET search_path TO lift_simulator;

CREATE INDEX IF NOT EXISTS idx_scenario_name
    ON scenario(name);

COMMENT ON INDEX idx_scenario_name IS 'Index on scenario name for efficient single-column filtering';
