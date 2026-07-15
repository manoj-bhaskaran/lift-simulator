import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { liftSystemsApi } from '../../api/liftSystemsApi';
import { handleApiError } from '../../utils/errorHandlers';

function SimulatorRunSetup({ loading, formError, setFormError, systems, scenarios, isStarting, isRunActive, onStartRun }) {
  const [searchParams] = useSearchParams();
  const [versions, setVersions] = useState([]);
  const [selectedSystemId, setSelectedSystemId] = useState(searchParams.get('systemId') || '');
  const [selectedVersionNumber, setSelectedVersionNumber] = useState(searchParams.get('versionNumber') || '');
  const [selectedScenarioId, setSelectedScenarioId] = useState('');
  const [seed, setSeed] = useState('');
  const hasSyncedFromParams = useRef(false);

  useEffect(() => {
    if (hasSyncedFromParams.current) return;
    const incomingSystemId = searchParams.get('systemId');
    const incomingVersionNumber = searchParams.get('versionNumber');
    if (incomingSystemId) setSelectedSystemId(incomingSystemId);
    if (incomingVersionNumber) setSelectedVersionNumber(incomingVersionNumber);
    hasSyncedFromParams.current = true;
  }, [searchParams]);

  const loadVersions = useCallback(async () => {
    if (!selectedSystemId) {
      setVersions([]);
      setSelectedVersionNumber('');
      return;
    }
    try {
      const response = await liftSystemsApi.getVersions(selectedSystemId);
      setVersions(response.data);
      setFormError(null);
    } catch (err) {
      handleApiError(err, setFormError, 'Failed to load versions');
    }
  }, [selectedSystemId, setFormError]);

  useEffect(() => { loadVersions(); }, [loadVersions]);

  const publishedVersions = useMemo(() => versions.filter((version) => version.status === 'PUBLISHED'), [versions]);

  useEffect(() => {
    if (!publishedVersions.length) {
      setSelectedVersionNumber('');
      return;
    }
    if (selectedVersionNumber) {
      const match = publishedVersions.find((version) => String(version.versionNumber) === String(selectedVersionNumber));
      if (!match) setSelectedVersionNumber('');
      return;
    }
    const querySystemId = searchParams.get('systemId');
    const preferredVersionNumber = searchParams.get('versionNumber');
    const queryMatchesSelectedSystem = querySystemId && String(querySystemId) === String(selectedSystemId);
    const resolvedVersion = queryMatchesSelectedSystem && preferredVersionNumber
      ? publishedVersions.find((version) => String(version.versionNumber) === String(preferredVersionNumber))
      : null;
    if (resolvedVersion) setSelectedVersionNumber(String(resolvedVersion.versionNumber));
  }, [publishedVersions, searchParams, selectedSystemId, selectedVersionNumber]);

  const selectedSystem = systems.find((system) => String(system.id) === String(selectedSystemId));
  const selectedVersion = publishedVersions.find((version) => String(version.versionNumber) === String(selectedVersionNumber));
  const filteredScenarios = useMemo(() => {
    if (!selectedVersionNumber) return [];
    return scenarios.filter((scenario) => selectedVersion && String(scenario.liftSystemVersionId) === String(selectedVersion.id));
  }, [scenarios, selectedVersion, selectedVersionNumber]);
  const selectedScenario = scenarios.find((scenario) => String(scenario.id) === String(selectedScenarioId));

  useEffect(() => {
    if (selectedScenarioId && selectedVersionNumber) {
      const isScenarioCompatible = filteredScenarios.some((scenario) => String(scenario.id) === String(selectedScenarioId));
      if (!isScenarioCompatible) setSelectedScenarioId('');
    }
  }, [selectedScenarioId, selectedVersionNumber, filteredScenarios]);

  const handleStart = () => {
    if (!selectedSystemId || !selectedVersionNumber || !selectedScenarioId) {
      setFormError('Select a lift system, published version, and scenario to continue.');
      return;
    }
    onStartRun({
      liftSystemId: Number(selectedSystemId),
      versionNumber: Number(selectedVersionNumber),
      scenarioId: Number(selectedScenarioId),
      seed: seed ? Number(seed) : null,
    }, { selectedSystem, selectedVersion, selectedScenario });
  };

  return (
    <section className="simulator-card">
      <h3>Run Setup</h3>
      {loading ? <p>Loading options...</p> : formError ? <p className="error">{formError}</p> : (
        <div className="run-setup-grid">
          <label className="field"><span>Lift System</span><select value={selectedSystemId} onChange={(event) => { setSelectedSystemId(event.target.value); setSelectedVersionNumber(''); }}><option value="">Select a lift system</option>{systems.map((system) => <option key={system.id} value={system.id}>{system.displayName}</option>)}</select></label>
          <label className="field"><span>Published Version</span><select value={selectedVersionNumber} onChange={(event) => setSelectedVersionNumber(event.target.value)} disabled={!selectedSystemId || publishedVersions.length === 0}><option value="">Select a published version</option>{publishedVersions.map((version) => <option key={version.id} value={version.versionNumber}>Version {version.versionNumber}</option>)}</select>{!selectedSystemId && <small>Select a system to view published versions.</small>}{selectedSystemId && publishedVersions.length === 0 && <small className="warning">No published versions available for this system.</small>}</label>
          <label className="field"><span>Scenario</span><select value={selectedScenarioId} onChange={(event) => setSelectedScenarioId(event.target.value)} disabled={!selectedVersionNumber || filteredScenarios.length === 0}><option value="">Select a scenario</option>{filteredScenarios.map((scenario) => <option key={scenario.id} value={scenario.id}>{scenario.name}</option>)}</select>{!selectedVersionNumber && <small>Select a version to view compatible scenarios.</small>}{selectedVersionNumber && filteredScenarios.length === 0 && <small className="warning">No scenarios available for this version. Create one from the Scenarios page.</small>}</label>
          <label className="field"><span>Optional Seed</span><input type="number" value={seed} onChange={(event) => setSeed(event.target.value)} placeholder="Leave blank for random" /></label>
          <div className="run-setup-actions"><button className="btn-primary" onClick={handleStart} disabled={isStarting || isRunActive || !selectedSystemId || !selectedVersionNumber || !selectedScenarioId}>{isStarting ? 'Starting...' : isRunActive ? 'Run in Progress' : 'Start Run'}</button>{selectedSystem && selectedVersion && selectedScenario && (<div className="selection-summary"><strong>Selected:</strong> {selectedSystem.displayName} · Version {selectedVersion.versionNumber} · {selectedScenario.name}</div>)}{isRunActive && <small className="run-in-progress-hint">A simulation run is in progress. Wait for it to complete or cancel it to start a new run.</small>}</div>
        </div>
      )}
    </section>
  );
}

export default SimulatorRunSetup;
