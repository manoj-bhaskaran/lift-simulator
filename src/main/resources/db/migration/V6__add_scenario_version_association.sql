-- Add lift system version association to scenarios
-- Version: 6
-- Description: Ties scenarios to specific lift system versions for floor range validation

SET search_path TO lift_simulator;

-- Add lift_system_version_id column to scenario table
-- Initially nullable to handle existing scenarios
ALTER TABLE scenario
    ADD COLUMN IF NOT EXISTS lift_system_version_id BIGINT;

-- Add foreign key constraint to lift_system_version
ALTER TABLE scenario
    ADD CONSTRAINT fk_scenario_lift_system_version
    FOREIGN KEY (lift_system_version_id)
    REFERENCES lift_system_version(id)
    ON DELETE RESTRICT;

-- Add index for performance
CREATE INDEX IF NOT EXISTS idx_scenario_version
    ON scenario(lift_system_version_id);

-- Add comments
COMMENT ON COLUMN scenario.lift_system_version_id IS 'Reference to the lift system version this scenario is designed for. Required for floor range validation.';

-- Note: Existing scenarios will have NULL lift_system_version_id
-- These scenarios must be assigned to a version before they can be executed
-- New scenarios will require lift_system_version_id to be set at creation time
