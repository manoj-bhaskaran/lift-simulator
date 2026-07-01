-- Add update triggers to automatically maintain updated_at timestamps
-- Version: 13
-- Description: Creates triggers to update the updated_at column on INSERT/UPDATE operations

SET search_path TO lift_simulator;

-- Trigger function to update updated_at column
CREATE OR REPLACE FUNCTION update_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger for lift_system
CREATE TRIGGER lift_system_update_timestamp
    BEFORE UPDATE ON lift_system
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- Trigger for lift_system_version
CREATE TRIGGER lift_system_version_update_timestamp
    BEFORE UPDATE ON lift_system_version
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

-- Trigger for scenario
CREATE TRIGGER scenario_update_timestamp
    BEFORE UPDATE ON scenario
    FOR EACH ROW
    EXECUTE FUNCTION update_timestamp();

COMMENT ON FUNCTION update_timestamp() IS 'Automatically updates updated_at to current time on record modification';
COMMENT ON TRIGGER lift_system_update_timestamp ON lift_system IS 'Maintains updated_at timestamp for lift_system table';
COMMENT ON TRIGGER lift_system_version_update_timestamp ON lift_system_version IS 'Maintains updated_at timestamp for lift_system_version table';
COMMENT ON TRIGGER scenario_update_timestamp ON scenario IS 'Maintains updated_at timestamp for scenario table';
