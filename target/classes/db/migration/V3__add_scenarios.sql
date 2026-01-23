-- Add scenario storage for UI-driven passenger flow scenarios
-- Version: 3
-- Description: Stores scenario JSON payloads used by the UI

SET search_path TO lift_simulator;

CREATE TABLE IF NOT EXISTS scenario (
    id BIGSERIAL PRIMARY KEY,
    scenario_json JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_scenario_json
    ON scenario USING GIN (scenario_json);

COMMENT ON TABLE scenario IS 'Passenger flow scenarios stored for UI-driven simulations';
COMMENT ON COLUMN scenario.scenario_json IS 'Scenario JSON payload for passenger flow definitions';
