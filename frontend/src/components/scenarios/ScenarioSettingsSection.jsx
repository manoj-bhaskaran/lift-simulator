// @ts-check
import PassengerFlowBuilder from '../PassengerFlowBuilder';

function ScenarioSettingsSection({ durationTicks, setDurationTicks, useSeed, setUseSeed, seed, setSeed, passengerFlows, setPassengerFlows, floorRange, formErrors, setFormErrors }) {
  return (
    <>
      <div className="form-section">
        <h3>Simulation Settings</h3>
        <div className="form-group">
          <label htmlFor="durationTicks">Duration (ticks) <span className="required">*</span></label>
          <input
            type="number"
            id="durationTicks"
            value={durationTicks}
            onChange={(e) => {
              setDurationTicks(e.target.value);
              if (formErrors.durationTicks) setFormErrors(prev => ({ ...prev, durationTicks: '' }));
            }}
            className={formErrors.durationTicks ? 'error' : ''}
            min="1"
            placeholder="e.g., 100"
          />
          {formErrors.durationTicks && <span className="error-message">{formErrors.durationTicks}</span>}
          <p className="help-text">How long the simulation will run (each tick represents one time unit)</p>
        </div>
        <div className="form-group">
          <label htmlFor="useSeed" className="checkbox-label">
            <input type="checkbox" id="useSeed" checked={useSeed} onChange={(e) => setUseSeed(e.target.checked)} />
            Use Random Seed (for reproducibility)
          </label>
        </div>
        {useSeed && (
          <div className="form-group">
            <label htmlFor="seed">Random Seed</label>
            <input type="number" id="seed" value={seed} onChange={(e) => setSeed(e.target.value)} min="0" placeholder="e.g., 42" />
            <p className="help-text">Optional seed for reproducible random behavior. Same seed = same simulation results.</p>
          </div>
        )}
      </div>
      <div className="form-section">
        <h3>Passenger Flows</h3>
        <PassengerFlowBuilder flows={passengerFlows} onChange={setPassengerFlows} maxTick={parseInt(durationTicks, 10) || 100} floorRange={floorRange} />
      </div>
    </>
  );
}

export default ScenarioSettingsSection;
