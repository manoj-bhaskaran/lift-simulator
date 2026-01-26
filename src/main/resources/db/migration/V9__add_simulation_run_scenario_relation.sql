-- Add scenario relationship to simulation_run table
-- Version: 9
-- Description: Links simulation runs to the scenario table for tracking
--              which scenario was executed in each run.

SET search_path TO lift_simulator;

-- Add scenario_id column to simulation_run (nullable for existing runs)
ALTER TABLE simulation_run
    ADD COLUMN IF NOT EXISTS scenario_id BIGINT;

-- Add foreign key constraint to scenario table with CASCADE delete
ALTER TABLE simulation_run
    ADD CONSTRAINT fk_simulation_run_scenario
    FOREIGN KEY (scenario_id)
    REFERENCES scenario(id)
    ON DELETE CASCADE;

-- Add index for performance
CREATE INDEX IF NOT EXISTS idx_simulation_run_scenario
    ON simulation_run(scenario_id);

-- Add comment
COMMENT ON COLUMN simulation_run.scenario_id IS 'Reference to the scenario executed in this run. Nullable for legacy or ad-hoc runs.';
