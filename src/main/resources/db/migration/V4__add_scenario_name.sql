-- Add name column to scenario table
-- Version: 4
-- Description: Adds a name field to scenarios for better identification in the UI

SET search_path TO lift_simulator;

ALTER TABLE scenario
    ADD COLUMN IF NOT EXISTS name VARCHAR(200) NOT NULL DEFAULT 'Unnamed Scenario';

-- Remove default after adding column (for future inserts to require explicit name)
ALTER TABLE scenario
    ALTER COLUMN name DROP DEFAULT;

COMMENT ON COLUMN scenario.name IS 'Human-readable name for the scenario';
