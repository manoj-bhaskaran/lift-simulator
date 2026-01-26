-- Enforce NOT NULL constraint on scenario lift_system_version_id
-- Version: 7
-- Description: Makes lift_system_version_id required for all scenarios.
--              Deletes any existing scenarios without a version association
--              as they are invalid and cannot be executed.

SET search_path TO lift_simulator;

-- Delete any scenarios that don't have a lift system version association
-- These scenarios are invalid and cannot be executed anyway
DELETE FROM scenario WHERE lift_system_version_id IS NULL;

-- Add NOT NULL constraint to ensure all future scenarios have a version
ALTER TABLE scenario
    ALTER COLUMN lift_system_version_id SET NOT NULL;

-- Update column comment
COMMENT ON COLUMN scenario.lift_system_version_id IS 'Reference to the lift system version this scenario is designed for. Required for floor range validation.';
