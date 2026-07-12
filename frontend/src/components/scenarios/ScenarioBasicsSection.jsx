// @ts-check

function ScenarioBasicsSection({
  scenarioName, setScenarioName, formErrors, setFormErrors,
  liftSystems, selectedSystemId, setSelectedSystemId,
  versions, selectedVersionId, setSelectedVersionId,
  floorRange, parseVersionConfig
}) {
  return (
    <div className="form-section">
      <div className="form-group">
        <label htmlFor="scenarioName">Scenario Name <span className="required">*</span></label>
        <input
          type="text"
          id="scenarioName"
          value={scenarioName}
          onChange={(e) => {
            setScenarioName(e.target.value);
            if (formErrors.scenarioName) setFormErrors(prev => ({ ...prev, scenarioName: '' }));
          }}
          placeholder="e.g., Morning Rush Hour"
          className={formErrors.scenarioName ? 'error' : ''}
        />
        {formErrors.scenarioName && <span className="error-message">{formErrors.scenarioName}</span>}
      </div>

      <div className="form-group">
        <label htmlFor="liftSystem">Lift System <span className="required">*</span></label>
        <select
          id="liftSystem"
          value={selectedSystemId}
          onChange={(e) => {
            setSelectedSystemId(e.target.value);
            setSelectedVersionId('');
            if (formErrors.liftSystem || formErrors.liftSystemVersion) {
              setFormErrors(prev => ({ ...prev, liftSystem: '', liftSystemVersion: '' }));
            }
          }}
          className={formErrors.liftSystem ? 'error' : ''}
        >
          <option value="">-- Select Lift System --</option>
          {liftSystems.map((system) => <option key={system.id} value={system.id}>{system.displayName}</option>)}
        </select>
        {formErrors.liftSystem && <span className="error-message">{formErrors.liftSystem}</span>}
        <p className="help-text">Select the lift system this scenario will be designed for</p>
      </div>

      {selectedSystemId && (
        <div className="form-group">
          <label htmlFor="liftSystemVersion">Version <span className="required">*</span></label>
          <select
            id="liftSystemVersion"
            value={selectedVersionId}
            onChange={(e) => {
              setSelectedVersionId(e.target.value);
              if (formErrors.liftSystemVersion) setFormErrors(prev => ({ ...prev, liftSystemVersion: '' }));
            }}
            className={formErrors.liftSystemVersion ? 'error' : ''}
          >
            <option value="">-- Select Version --</option>
            {versions.map((version) => {
              const floorInfo = parseVersionConfig(version.config);
              return (
                <option key={version.id} value={version.id}>
                  Version {version.versionNumber}{floorInfo ? ` (Floors ${floorInfo.minFloor} to ${floorInfo.maxFloor})` : ''}
                </option>
              );
            })}
          </select>
          <p className="help-text">Select the version to ensure floor ranges are validated correctly</p>
          {floorRange && <p className="help-text"><strong>Valid floor range: {floorRange.minFloor} to {floorRange.maxFloor}</strong></p>}
          {formErrors.liftSystemVersion && <span className="error-message">{formErrors.liftSystemVersion}</span>}
        </div>
      )}
    </div>
  );
}

export default ScenarioBasicsSection;
