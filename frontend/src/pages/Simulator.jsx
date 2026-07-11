// @ts-check
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { liftSystemsApi } from '../api/liftSystemsApi';
import { scenariosApi } from '../api/scenariosApi';
import { authHeaders, normalizedApiBaseUrl } from '../api/client';
import { simulationRunsApi } from '../api/simulationRunsApi';
import ConfirmModal from '../components/ConfirmModal';
import { handleApiError } from '../utils/errorHandlers';
import { isTerminalRunStatus, useRunPolling } from '../hooks/useRunPolling';
import { formatRunDuration, getRunStatusPillClass } from '../utils/statusUtils';
import './Simulator.css';

const kpiLabels = {
  requestsTotal: 'Requests',
  pickupRequestsServed: 'Pickup Requests Served',
  pickupRequestsCancelled: 'Pickup Requests Cancelled',
  passengersServed: 'Passengers Served',
  passengersCancelled: 'Passengers Cancelled',
  avgPickupWaitTicks: 'Avg Wait to Pickup (ticks)',
  maxPickupWaitTicks: 'Max Wait to Pickup (ticks)',
  idleTicks: 'Idle Ticks',
  movingTicks: 'Moving Ticks',
  doorTicks: 'Door Ticks',
  pickupLegUtilisation: 'Pickup-leg Utilisation',
  utilisation: 'Pickup-leg Utilisation',
};

function Simulator() {
  const [searchParams] = useSearchParams();
  /** @type {import('../types/models').LiftSystem[]} */
  const [systems, setSystems] = useState([]);
  const [versions, setVersions] = useState([]);
  const [scenarios, setScenarios] = useState([]);
  const [loading, setLoading] = useState(true);
  const [formError, setFormError] = useState(null);
  const [runError, setRunError] = useState(null);
  const [resultsError, setResultsError] = useState(null);

  const [selectedSystemId, setSelectedSystemId] = useState(
    searchParams.get('systemId') || ''
  );
  const [selectedVersionNumber, setSelectedVersionNumber] = useState(
    searchParams.get('versionNumber') || ''
  );
  const [selectedScenarioId, setSelectedScenarioId] = useState('');
  const [seed, setSeed] = useState('');
  const [runInfo, setRunInfo] = useState(null);
  const [results, setResults] = useState(null);
  const [artefacts, setArtefacts] = useState([]);
  const [now, setNow] = useState(() => Date.now());
  const [isStarting, setIsStarting] = useState(false);
  const [isCancelling, setIsCancelling] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);
  const hasSyncedFromParams = useRef(false);

  useEffect(() => {
    const loadOptions = async () => {
      try {
        setLoading(true);
        const [systemsResponse, scenariosResponse] = await Promise.all([
          liftSystemsApi.getAllSystems(),
          scenariosApi.getAllScenarios(),
        ]);
        setSystems(systemsResponse.data);
        setScenarios(scenariosResponse.data);
        setFormError(null);
      } catch (err) {
        handleApiError(err, setFormError, 'Failed to load simulator options');
      } finally {
        setLoading(false);
      }
    };

    loadOptions();
  }, []);

  useEffect(() => {
    if (hasSyncedFromParams.current) {
      return;
    }

    const incomingSystemId = searchParams.get('systemId');
    const incomingVersionNumber = searchParams.get('versionNumber');

    if (incomingSystemId) {
      setSelectedSystemId(incomingSystemId);
    }

    if (incomingVersionNumber) {
      setSelectedVersionNumber(incomingVersionNumber);
    }

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
  }, [selectedSystemId]);

  useEffect(() => {
    loadVersions();
  }, [loadVersions]);

  const publishedVersions = useMemo(
    () => versions.filter((version) => version.status === 'PUBLISHED'),
    [versions]
  );

  useEffect(() => {
    if (!publishedVersions.length) {
      setSelectedVersionNumber('');
      return;
    }

    if (selectedVersionNumber) {
      const match = publishedVersions.find(
        (version) => String(version.versionNumber) === String(selectedVersionNumber)
      );
      if (!match) {
        setSelectedVersionNumber('');
      }
      return;
    }

    const querySystemId = searchParams.get('systemId');
    const preferredVersionNumber = searchParams.get('versionNumber');
    const queryMatchesSelectedSystem =
      querySystemId && String(querySystemId) === String(selectedSystemId);
    const resolvedVersion = queryMatchesSelectedSystem && preferredVersionNumber
      ? publishedVersions.find(
          (version) => String(version.versionNumber) === String(preferredVersionNumber)
        )
      : null;

    if (resolvedVersion) {
      setSelectedVersionNumber(String(resolvedVersion.versionNumber));
    }
  }, [publishedVersions, searchParams, selectedSystemId, selectedVersionNumber]);

  const selectedSystem = systems.find(
    (system) => String(system.id) === String(selectedSystemId)
  );
  const selectedVersion = publishedVersions.find(
    (version) => String(version.versionNumber) === String(selectedVersionNumber)
  );

  // Filter scenarios to only show those belonging to the selected version
  const filteredScenarios = useMemo(() => {
    if (!selectedVersionNumber) {
      return [];
    }
    return scenarios.filter(
      (scenario) => selectedVersion && String(scenario.liftSystemVersionId) === String(selectedVersion.id)
    );
  }, [scenarios, selectedVersion, selectedVersionNumber]);

  const selectedScenario = scenarios.find(
    (scenario) => String(scenario.id) === String(selectedScenarioId)
  );

  const runStatus = runInfo?.status;
  const isTerminal = isTerminalRunStatus(runStatus);

  // Clear selected scenario when version changes if it's not compatible
  useEffect(() => {
    if (selectedScenarioId && selectedVersionNumber) {
      const isScenarioCompatible = filteredScenarios.some(
        (scenario) => String(scenario.id) === String(selectedScenarioId)
      );
      if (!isScenarioCompatible) {
        setSelectedScenarioId('');
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedVersionNumber, filteredScenarios]);

  useRunPolling(() => setNow(Date.now()), {
    intervalMs: 1000,
    enabled: Boolean(runInfo) && !isTerminal,
  });

  const pollRunStatus = useCallback(async () => {
    if (!runInfo?.id) {
      return;
    }

    try {
      const response = await simulationRunsApi.getRun(runInfo.id);
      setRunInfo(response.data);
      setRunError(null);
    } catch (err) {
      handleApiError(err, setRunError, 'Failed to refresh run status');
    }
  }, [runInfo]);

  useRunPolling(pollRunStatus, {
    intervalMs: 3000,
    enabled: Boolean(runInfo?.id) && !isTerminal,
    immediate: true,
  });

  useEffect(() => {
    if (!runInfo?.id || !isTerminal) {
      return;
    }

    const loadResults = async () => {
      try {
        if (runStatus === 'SUCCEEDED' || runStatus === 'FAILED') {
          const response = await simulationRunsApi.getResults(runInfo.id);
          setResults(response.data);
          setResultsError(null);
        }
      } catch (err) {
        handleApiError(err, setResultsError, 'Failed to load simulation results');
      }
    };

    const loadArtefacts = async () => {
      try {
        const response = await simulationRunsApi.getArtefacts(runInfo.id);
        setArtefacts(response.data);
      } catch (err) {
        handleApiError(err, setResultsError, 'Failed to load artefacts');
      }
    };

    loadResults();
    loadArtefacts();
  }, [isTerminal, runInfo?.id, runStatus]);

  const handleStartRun = async () => {
    if (!selectedSystemId || !selectedVersionNumber || !selectedScenarioId) {
      setFormError('Select a lift system, published version, and scenario to continue.');
      return;
    }

    try {
      setIsStarting(true);
      const payload = {
        liftSystemId: Number(selectedSystemId),
        versionNumber: Number(selectedVersionNumber),
        scenarioId: Number(selectedScenarioId),
        seed: seed ? Number(seed) : null,
      };
      const response = await simulationRunsApi.createRun(payload);
      setRunInfo(response.data);
      setResults(null);
      setArtefacts([]);
      setRunError(null);
      setResultsError(null);
      setNow(Date.now());
    } catch (err) {
      handleApiError(err, setRunError, 'Failed to start simulation run');
    } finally {
      setIsStarting(false);
    }
  };

  const handleCancelRun = async () => {
    if (!runInfo?.id) {
      return;
    }
    try {
      setIsCancelling(true);
      const response = await simulationRunsApi.cancelRun(runInfo.id);
      setRunInfo(response.data);
      setRunError(null);
    } catch (err) {
      handleApiError(err, setRunError, 'Failed to cancel simulation run');
    } finally {
      setIsCancelling(false);
    }
  };

  const elapsedMs = useMemo(() => {
    if (!runInfo?.createdAt) {
      return null;
    }
    const baseTime = runInfo.startedAt || runInfo.createdAt;
    const start = new Date(baseTime).getTime();
    return Math.max(0, now - start);
  }, [now, runInfo?.createdAt, runInfo?.startedAt]);

  const progressPercentage = useMemo(() => {
    if (!runInfo?.totalTicks || runInfo.totalTicks === 0 || runInfo.currentTick == null) {
      return null;
    }
    return Math.min(100, (runInfo.currentTick / runInfo.totalTicks) * 100);
  }, [runInfo]);

  const artefactDownloadUrl = useCallback((path) => {
    if (!runInfo?.id || !path) {
      return '#';
    }
    const normalizedPath = path.replace(/\\/g, '/');
    const encodedPath = normalizedPath
      .split('/')
      .map((segment) => encodeURIComponent(segment))
      .join('/');
    return `${normalizedApiBaseUrl}/simulation-runs/${runInfo.id}/artefacts/${encodedPath}`;
  }, [runInfo]);

  const handleArtefactDownload = useCallback(
    async (event, artefact) => {
      event.preventDefault();
      if (!runInfo?.id || !artefact?.path) {
        setResultsError('Artefact path is unavailable for download.');
        return;
      }

      try {
        setResultsError(null);
        const response = await fetch(artefactDownloadUrl(artefact.path), { headers: authHeaders });
        if (!response.ok) {
          let errorMessage = `Unable to download ${artefact.name || 'artefact'}.`;
          const contentType = response.headers.get('content-type') || '';
          if (contentType.includes('application/json')) {
            const data = await response.json();
            if (data?.message) {
              errorMessage = data.message;
            }
          } else {
            const text = await response.text();
            if (text) {
              errorMessage = text;
            }
          }
          throw new Error(errorMessage);
        }

        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = artefact.name || artefact.path.split('/').pop() || 'artefact';
        document.body.appendChild(link);
        link.click();
        link.remove();
        window.URL.revokeObjectURL(url);
      } catch (err) {
        handleApiError(err, setResultsError, `Failed to download ${artefact.name || 'artefact'}`);
      }
    },
    [runInfo?.id, artefactDownloadUrl]
  );

  const formatNumber = (value) => {
    if (value == null || Number.isNaN(value)) {
      return '—';
    }
    if (typeof value === 'number') {
      return Number.isInteger(value) ? value.toString() : value.toFixed(2);
    }
    return String(value);
  };

  const formatKpiValue = (key, value) => {
    if ((key === 'pickupLegUtilisation' || key === 'utilisation') && typeof value === 'number') {
      return `${(value * 100).toFixed(1)}%`;
    }
    return formatNumber(value);
  };

  const getPickupLegUtilisation = (lift) => lift?.pickupLegUtilisation ?? lift?.utilisation;

  const formatBytes = (bytes) => {
    if (!bytes && bytes !== 0) {
      return '—';
    }
    const units = ['B', 'KB', 'MB', 'GB'];
    let size = bytes;
    let index = 0;
    while (size >= 1024 && index < units.length - 1) {
      size /= 1024;
      index += 1;
    }
    return `${size.toFixed(1)} ${units[index]}`;
  };

  const runSummary = results?.results?.runSummary;
  const kpis = results?.results?.kpis;
  const perLift = results?.results?.perLift || [];
  const perFloor = results?.results?.perFloor || [];
  const inputArtefact = artefacts.find((item) => item.name === 'input.scenario');

  return (
    <div className="simulator">
      <div className="page-header">
        <div>
          <h2>Run Simulator</h2>
          <p className="page-subtitle">
            Launch a batch simulation run, monitor progress, and review results.
          </p>
        </div>
        <div className="page-actions">
          <Link to="/simulation-runs" className="btn-secondary">
            View All Runs
          </Link>
          <Link to="/simulator" className="btn-secondary">
            Choose System
          </Link>
        </div>
      </div>

      <section className="simulator-card">
        <h3>Run Setup</h3>
        {loading ? (
          <p>Loading options...</p>
        ) : formError ? (
          <p className="error">{formError}</p>
        ) : (
          <div className="run-setup-grid">
            <label className="field">
              <span>Lift System</span>
              <select
                value={selectedSystemId}
                onChange={(event) => {
                  setSelectedSystemId(event.target.value);
                  setSelectedVersionNumber('');
                }}
              >
                <option value="">Select a lift system</option>
                {systems.map((system) => (
                  <option key={system.id} value={system.id}>
                    {system.displayName}
                  </option>
                ))}
              </select>
            </label>

            <label className="field">
              <span>Published Version</span>
              <select
                value={selectedVersionNumber}
                onChange={(event) => setSelectedVersionNumber(event.target.value)}
                disabled={!selectedSystemId || publishedVersions.length === 0}
              >
                <option value="">Select a published version</option>
                {publishedVersions.map((version) => (
                  <option key={version.id} value={version.versionNumber}>
                    Version {version.versionNumber}
                  </option>
                ))}
              </select>
              {!selectedSystemId && (
                <small>Select a system to view published versions.</small>
              )}
              {selectedSystemId && publishedVersions.length === 0 && (
                <small className="warning">
                  No published versions available for this system.
                </small>
              )}
            </label>

            <label className="field">
              <span>Scenario</span>
              <select
                value={selectedScenarioId}
                onChange={(event) => setSelectedScenarioId(event.target.value)}
                disabled={!selectedVersionNumber || filteredScenarios.length === 0}
              >
                <option value="">Select a scenario</option>
                {filteredScenarios.map((scenario) => (
                  <option key={scenario.id} value={scenario.id}>
                    {scenario.name}
                  </option>
                ))}
              </select>
              {!selectedVersionNumber && (
                <small>Select a version to view compatible scenarios.</small>
              )}
              {selectedVersionNumber && filteredScenarios.length === 0 && (
                <small className="warning">
                  No scenarios available for this version. Create one from the Scenarios page.
                </small>
              )}
            </label>

            <label className="field">
              <span>Optional Seed</span>
              <input
                type="number"
                value={seed}
                onChange={(event) => setSeed(event.target.value)}
                placeholder="Leave blank for random"
              />
            </label>

            <div className="run-setup-actions">
              <button
                className="btn-primary"
                onClick={handleStartRun}
                disabled={
                  isStarting ||
                  (runInfo && !isTerminal) ||
                  !selectedSystemId ||
                  !selectedVersionNumber ||
                  !selectedScenarioId
                }
              >
                {isStarting ? 'Starting...' : (runInfo && !isTerminal) ? 'Run in Progress' : 'Start Run'}
              </button>
              {selectedSystem && selectedVersion && selectedScenario && (
                <div className="selection-summary">
                  <strong>Selected:</strong> {selectedSystem.displayName} · Version{' '}
                  {selectedVersion.versionNumber} · {selectedScenario.name}
                </div>
              )}
              {runInfo && !isTerminal && (
                <small className="run-in-progress-hint">
                  A simulation run is in progress. Wait for it to complete or cancel it to start a new run.
                </small>
              )}
            </div>
          </div>
        )}
      </section>

      {runInfo && (
        <section className="simulator-card">
          <div className="status-header">
            <h3>Run Status</h3>
            <div className="status-actions">
              <span className={getRunStatusPillClass(runStatus)}>
                {runStatus}
              </span>
              {(runStatus === 'RUNNING' || runStatus === 'CREATED') && (
                <button
                  className="btn-danger"
                  type="button"
                  onClick={() => setShowCancelModal(true)}
                  disabled={isCancelling}
                >
                  {isCancelling ? 'Cancelling...' : 'Cancel Run'}
                </button>
              )}
            </div>
          </div>

          <div className="status-grid">
            <div>
              <span className="label">Run ID</span>
              <p>{runInfo.id}</p>
            </div>
            <div>
              <span className="label">Lift System</span>
              <p>{selectedSystem?.displayName || runInfo.liftSystemId}</p>
            </div>
            <div>
              <span className="label">Version</span>
              <p>{selectedVersion ? `Version ${selectedVersion.versionNumber}` : runInfo.versionNumber}</p>
            </div>
            <div>
              <span className="label">Scenario</span>
              <p>{selectedScenario?.name || runInfo.scenarioId || '—'}</p>
            </div>
            <div>
              <span className="label">Elapsed Time</span>
              <p>{formatRunDuration(elapsedMs)}</p>
            </div>
            <div>
              <span className="label">Seed</span>
              <p>{runInfo.seed ?? '—'}</p>
            </div>
          </div>

          {progressPercentage != null && (
            <div className="progress-section">
              <div className="progress-meta">
                <span>
                  {runInfo.currentTick ?? 0} / {runInfo.totalTicks} ticks
                </span>
                <span>{progressPercentage.toFixed(1)}%</span>
              </div>
              <div className="progress-bar">
                <div
                  className="progress-fill"
                  style={{ width: `${progressPercentage}%` }}
                />
              </div>
            </div>
          )}

          {runError && <p className="error">{runError}</p>}
        </section>
      )}

      <ConfirmModal
        isOpen={showCancelModal}
        onClose={() => setShowCancelModal(false)}
        onConfirm={handleCancelRun}
        title="Cancel simulation run?"
        message="This will stop the current simulation run. Partial results will be preserved, but the run will be marked as cancelled."
        confirmText="Cancel run"
        cancelText="Keep running"
        confirmStyle="danger"
      />

      {isTerminal && runInfo && (
        <section className="simulator-card">
          <div className="results-header">
            <h3>Results</h3>
            <Link to={`/simulation-runs/${runInfo.id}`} className="btn-secondary btn-small">
              View Full Details
            </Link>
          </div>
          {resultsError && <p className="error">{resultsError}</p>}

          {runStatus === 'FAILED' && (
            <div className="result-banner error">
              <strong>Simulation failed.</strong>
              <p>{results?.errorMessage || runInfo.errorMessage || 'Unknown error.'}</p>
            </div>
          )}

          {runStatus === 'CANCELLED' && (
            <div className="result-banner warning">
              <strong>Simulation was cancelled.</strong>
            </div>
          )}

          {runStatus === 'SUCCEEDED' && results?.results && (
            <>
              <div className="result-banner success">
                <strong>Simulation completed successfully.</strong>
                {results.errorMessage && <p>{results.errorMessage}</p>}
              </div>

              <div className="kpi-grid">
                {kpis &&
                  Object.entries(kpis).map(([key, value]) => (
                    <div key={key} className="kpi-card">
                      <span>{kpiLabels[key] || key}</span>
                      <strong>{formatKpiValue(key, value)}</strong>
                    </div>
                  ))}
              </div>

              <div className="results-section">
                <h4>Run Summary</h4>
                <div className="summary-grid">
                  <div>
                    <span className="label">Generated</span>
                    <p>{runSummary?.generatedAt ? new Date(runSummary.generatedAt).toLocaleString() : '—'}</p>
                  </div>
                  <div>
                    <span className="label">Ticks</span>
                    <p>{runSummary?.ticks ?? '—'}</p>
                  </div>
                  <div>
                    <span className="label">Duration</span>
                    <p>{runSummary?.durationTicks ?? '—'} ticks</p>
                  </div>
                  <div>
                    <span className="label">Seed</span>
                    <p>{runSummary?.seed ?? runInfo.seed ?? '—'}</p>
                  </div>
                </div>
              </div>

              <div className="results-section">
                <h4>Per Lift</h4>
                {perLift.length === 0 ? (
                  <p>No lift metrics available.</p>
                ) : (
                  <div className="table-wrapper">
                    <table>
                      <thead>
                        <tr>
                          <th>Lift</th>
                          <th>Controller</th>
                          <th>Parking</th>
                          <th>Pickup-leg Utilisation</th>
                          <th>Idle</th>
                          <th>Moving</th>
                          <th>Door</th>
                          <th>Status Counts</th>
                        </tr>
                      </thead>
                      <tbody>
                        {perLift.map((lift) => (
                          <tr key={lift.liftId}>
                            <td>{lift.liftId}</td>
                            <td>{lift.controllerStrategy || '—'}</td>
                            <td>{lift.idleParkingMode || '—'}</td>
                            <td>{formatKpiValue('pickupLegUtilisation', getPickupLegUtilisation(lift))}</td>
                            <td>{formatNumber(lift.idleTicks)}</td>
                            <td>{formatNumber(lift.movingTicks)}</td>
                            <td>{formatNumber(lift.doorTicks)}</td>
                            <td>
                              {lift.statusCounts
                                ? Object.entries(lift.statusCounts)
                                    .map(([status, count]) => `${status}: ${count}`)
                                    .join(', ')
                                : '—'}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>

              <div className="results-section">
                <h4>Per Floor</h4>
                {perFloor.length === 0 ? (
                  <p>No floor metrics available.</p>
                ) : (
                  <div className="table-wrapper">
                    <table>
                      <thead>
                        <tr>
                          <th>Floor</th>
                          <th>Origin Passengers</th>
                          <th>Destination Passengers</th>
                          <th>Lift Visits</th>
                        </tr>
                      </thead>
                      <tbody>
                        {perFloor.map((floor) => (
                          <tr key={floor.floor}>
                            <td>{floor.floor}</td>
                            <td>{formatNumber(floor.originPassengers)}</td>
                            <td>{formatNumber(floor.destinationPassengers)}</td>
                            <td>{formatNumber(floor.liftVisits)}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                )}
              </div>
            </>
          )}

          {runStatus === 'SUCCEEDED' && !results?.results && (
            <div className="result-banner warning">
              <strong>Simulation completed, but results were unavailable.</strong>
              <p>{results?.errorMessage || 'Results file could not be read.'}</p>
            </div>
          )}

          <div className="results-section">
            <h4>Artefacts</h4>
            {artefacts.length === 0 ? (
              <p>No artefacts available.</p>
            ) : (
              <div className="table-wrapper">
                <table>
                  <thead>
                    <tr>
                      <th>Name</th>
                      <th>Size</th>
                      <th>Type</th>
                      <th>Download</th>
                    </tr>
                  </thead>
                  <tbody>
                    {artefacts.map((artefact) => (
                      <tr key={`${artefact.name}-${artefact.path}`}>
                        <td>{artefact.name}</td>
                        <td>{formatBytes(artefact.size)}</td>
                        <td>{artefact.mimeType}</td>
                        <td>
                          <a
                            className="link"
                            href={artefactDownloadUrl(artefact.path)}
                            onClick={(event) => handleArtefactDownload(event, artefact)}
                          >
                            Download
                          </a>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          <div className="results-section reproduce-section">
            <h4>Reproduce via CLI</h4>
            <p>
              Use the generated batch input file to reproduce this run with the
              CLI simulator.
            </p>
            {inputArtefact ? (
              <div className="reproduce-actions">
                <a
                  className="btn-secondary"
                  href={artefactDownloadUrl(inputArtefact.path)}
                  onClick={(event) => handleArtefactDownload(event, inputArtefact)}
                >
                  Download input.scenario
                </a>
                <code>lift-simulator --input {inputArtefact.path}</code>
              </div>
            ) : (
              <p className="warning">Input file not available for this run.</p>
            )}
          </div>
        </section>
      )}
    </div>
  );
}

export default Simulator;
