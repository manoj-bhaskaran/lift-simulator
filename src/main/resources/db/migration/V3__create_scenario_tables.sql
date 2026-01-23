-- Create Scenario Management Tables
-- Version: 3
-- Description: Add scenario and scenario_event tables for passenger flow management

SET search_path TO lift_simulator;

CREATE TABLE IF NOT EXISTS scenario (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    total_ticks INTEGER NOT NULL,
    min_floor INTEGER NOT NULL DEFAULT 0,
    max_floor INTEGER NOT NULL,
    initial_floor INTEGER,
    home_floor INTEGER,
    travel_ticks_per_floor INTEGER NOT NULL DEFAULT 10,
    door_transition_ticks INTEGER NOT NULL DEFAULT 5,
    door_dwell_ticks INTEGER NOT NULL DEFAULT 10,
    controller_strategy VARCHAR(50) NOT NULL DEFAULT 'DIRECTIONAL_SCAN',
    idle_parking_mode VARCHAR(50) NOT NULL DEFAULT 'STAY_PUT',
    seed BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_scenario_floor_range CHECK (max_floor > min_floor),
    CONSTRAINT chk_scenario_total_ticks CHECK (total_ticks > 0),
    CONSTRAINT chk_scenario_controller_strategy
        CHECK (controller_strategy IN ('NAIVE', 'SIMPLE', 'DIRECTIONAL_SCAN')),
    CONSTRAINT chk_scenario_idle_parking_mode
        CHECK (idle_parking_mode IN ('STAY_PUT', 'RETURN_HOME', 'RETURN_TO_LOBBY'))
);

CREATE INDEX idx_scenario_name ON scenario(name);
CREATE INDEX idx_scenario_created_at ON scenario(created_at DESC);

CREATE TABLE IF NOT EXISTS scenario_event (
    id BIGSERIAL PRIMARY KEY,
    scenario_id BIGINT NOT NULL REFERENCES scenario(id) ON DELETE CASCADE,
    tick BIGINT NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    description VARCHAR(200),
    origin_floor INTEGER,
    destination_floor INTEGER,
    direction VARCHAR(10),
    event_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_scenario_event_type
        CHECK (event_type IN ('HALL_CALL', 'CAR_CALL', 'CANCEL')),
    CONSTRAINT chk_scenario_event_direction
        CHECK (direction IS NULL OR direction IN ('UP', 'DOWN')),
    CONSTRAINT chk_scenario_event_tick CHECK (tick >= 0)
);

CREATE INDEX idx_scenario_event_scenario ON scenario_event(scenario_id);
CREATE INDEX idx_scenario_event_tick ON scenario_event(scenario_id, tick, event_order);

COMMENT ON TABLE scenario IS 'Passenger flow scenario definitions';
COMMENT ON TABLE scenario_event IS 'Individual events (hall calls, car calls) within scenarios';
COMMENT ON COLUMN scenario.total_ticks IS 'Total simulation duration in ticks';
COMMENT ON COLUMN scenario.seed IS 'Random seed for reproducibility (optional)';
COMMENT ON COLUMN scenario_event.tick IS 'When this event occurs in simulation timeline';
COMMENT ON COLUMN scenario_event.event_order IS 'Order of events within the same tick';
