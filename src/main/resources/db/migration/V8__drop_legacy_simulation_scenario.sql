-- Drop legacy simulation_scenario table
-- Version: 8
-- Description: Removes the legacy simulation_scenario table and related columns.
--              Scenarios are now managed through the 'scenario' table which is
--              tied to lift system versions for proper validation.

SET search_path TO lift_simulator;

-- First drop the scenario_id column from simulation_run
-- This also drops the foreign key constraint automatically
ALTER TABLE simulation_run DROP COLUMN IF EXISTS scenario_id;

-- Drop indexes on simulation_scenario table
DROP INDEX IF EXISTS idx_simulation_scenario_name;
DROP INDEX IF EXISTS idx_simulation_scenario_json;

-- Drop the simulation_scenario table
DROP TABLE IF EXISTS simulation_scenario;
