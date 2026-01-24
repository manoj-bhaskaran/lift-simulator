-- Add Simulation Run and Scenario tables
-- Version: 5
-- Description: Introduce persistent run lifecycle for simulation execution tracking

SET search_path TO lift_simulator;

-- Simulation Scenario Table
-- Stores reusable test scenarios that can be run against different lift systems
CREATE TABLE IF NOT EXISTS simulation_scenario (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    scenario_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_simulation_scenario_name ON simulation_scenario(name);
CREATE INDEX idx_simulation_scenario_json ON simulation_scenario USING GIN (scenario_json);

COMMENT ON TABLE simulation_scenario IS 'Reusable simulation scenarios for lift system testing';
COMMENT ON COLUMN simulation_scenario.name IS 'Human-readable name for the scenario';
COMMENT ON COLUMN simulation_scenario.scenario_json IS 'JSON payload containing scenario configuration (passengers, events, etc.)';

-- Simulation Run Table
-- Tracks individual simulation runs with their lifecycle status
CREATE TABLE IF NOT EXISTS simulation_run (
    id BIGSERIAL PRIMARY KEY,
    lift_system_id BIGINT NOT NULL REFERENCES lift_system(id) ON DELETE CASCADE,
    version_id BIGINT NOT NULL REFERENCES lift_system_version(id) ON DELETE CASCADE,
    scenario_id BIGINT REFERENCES simulation_scenario(id) ON DELETE SET NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    total_ticks BIGINT,
    current_tick BIGINT DEFAULT 0,
    seed BIGINT,
    error_message TEXT,
    artefact_base_path VARCHAR(500),
    CONSTRAINT chk_simulation_run_status
        CHECK (status IN ('CREATED', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_simulation_run_lift_system ON simulation_run(lift_system_id);
CREATE INDEX idx_simulation_run_version ON simulation_run(version_id);
CREATE INDEX idx_simulation_run_scenario ON simulation_run(scenario_id);
CREATE INDEX idx_simulation_run_status ON simulation_run(status);
CREATE INDEX idx_simulation_run_created_at ON simulation_run(created_at DESC);

COMMENT ON TABLE simulation_run IS 'Individual simulation run executions with lifecycle tracking';
COMMENT ON COLUMN simulation_run.lift_system_id IS 'Reference to the lift system being simulated';
COMMENT ON COLUMN simulation_run.version_id IS 'Reference to the specific version of the lift system configuration';
COMMENT ON COLUMN simulation_run.scenario_id IS 'Reference to the scenario being executed (nullable for ad-hoc runs)';
COMMENT ON COLUMN simulation_run.status IS 'Current status of the run: CREATED, RUNNING, SUCCEEDED, FAILED, CANCELLED';
COMMENT ON COLUMN simulation_run.started_at IS 'Timestamp when the simulation started execution';
COMMENT ON COLUMN simulation_run.ended_at IS 'Timestamp when the simulation completed or failed';
COMMENT ON COLUMN simulation_run.total_ticks IS 'Total number of simulation ticks to execute';
COMMENT ON COLUMN simulation_run.current_tick IS 'Current progress tick for running simulations';
COMMENT ON COLUMN simulation_run.seed IS 'Random seed for reproducible simulation runs';
COMMENT ON COLUMN simulation_run.error_message IS 'Error details if the run failed';
COMMENT ON COLUMN simulation_run.artefact_base_path IS 'File system path to simulation output artefacts';
