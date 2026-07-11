function SimulatorRunSetup({
  loading,
  formError,
  systems,
  publishedVersions,
  filteredScenarios,
  selectedSystemId,
  selectedVersionNumber,
  selectedScenarioId,
  seed,
  isStarting,
  isRunActive,
  selectedSystem,
  selectedVersion,
  selectedScenario,
  onSystemChange,
  onVersionChange,
  onScenarioChange,
  onSeedChange,
  onStartRun,
}) {
  return (
    <section className="simulator-card">
      <h3>Run Setup</h3>
      {loading ? <p>Loading options...</p> : formError ? <p className="error">{formError}</p> : (
        <div className="run-setup-grid">
          <label className="field">
            <span>Lift System</span>
            <select value={selectedSystemId} onChange={(event) => onSystemChange(event.target.value)}>
              <option value="">Select a lift system</option>
              {systems.map((system) => <option key={system.id} value={system.id}>{system.displayName}</option>)}
            </select>
          </label>
          <label className="field">
            <span>Published Version</span>
            <select value={selectedVersionNumber} onChange={(event) => onVersionChange(event.target.value)} disabled={!selectedSystemId || publishedVersions.length === 0}>
              <option value="">Select a published version</option>
              {publishedVersions.map((version) => <option key={version.id} value={version.versionNumber}>Version {version.versionNumber}</option>)}
            </select>
            {!selectedSystemId && <small>Select a system to view published versions.</small>}
            {selectedSystemId && publishedVersions.length === 0 && <small className="warning">No published versions available for this system.</small>}
          </label>
          <label className="field">
            <span>Scenario</span>
            <select value={selectedScenarioId} onChange={(event) => onScenarioChange(event.target.value)} disabled={!selectedVersionNumber || filteredScenarios.length === 0}>
              <option value="">Select a scenario</option>
              {filteredScenarios.map((scenario) => <option key={scenario.id} value={scenario.id}>{scenario.name}</option>)}
            </select>
            {!selectedVersionNumber && <small>Select a version to view compatible scenarios.</small>}
            {selectedVersionNumber && filteredScenarios.length === 0 && <small className="warning">No scenarios available for this version. Create one from the Scenarios page.</small>}
          </label>
          <label className="field">
            <span>Optional Seed</span>
            <input type="number" value={seed} onChange={(event) => onSeedChange(event.target.value)} placeholder="Leave blank for random" />
          </label>
          <div className="run-setup-actions">
            <button className="btn-primary" onClick={onStartRun} disabled={isStarting || isRunActive || !selectedSystemId || !selectedVersionNumber || !selectedScenarioId}>
              {isStarting ? 'Starting...' : isRunActive ? 'Run in Progress' : 'Start Run'}
            </button>
            {selectedSystem && selectedVersion && selectedScenario && (
              <div className="selection-summary"><strong>Selected:</strong> {selectedSystem.displayName} · Version {selectedVersion.versionNumber} · {selectedScenario.name}</div>
            )}
            {isRunActive && <small className="run-in-progress-hint">A simulation run is in progress. Wait for it to complete or cancel it to start a new run.</small>}
          </div>
        </div>
      )}
    </section>
  );
}

export default SimulatorRunSetup;
