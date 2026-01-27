// @ts-check
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { simulationRunsApi } from '../api/simulationRunsApi';
import ConfirmModal from '../components/ConfirmModal';
import { handleApiError } from '../utils/errorHandlers';
import './SimulationRunDetail.css';

const terminalStatuses = new Set(['SUCCEEDED', 'FAILED', 'CANCELLED']);
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '/api').trim() || '/api';
const normalizedApiBaseUrl = apiBaseUrl.replace(/\/$/, '');

const kpiLabels = {
  requestsTotal: 'Requests',
  passengersServed: 'Passengers Served',
  passengersCancelled: 'Passengers Cancelled',
  avgWaitTicks: 'Avg Wait (ticks)',
  maxWaitTicks: 'Max Wait (ticks)',
  idleTicks: 'Idle Ticks',
  movingTicks: 'Moving Ticks',
  doorTicks: 'Door Ticks',
  utilisation: 'Utilisation',
};

/**
 * Simulation run detail page component.
 * Displays the full details and results of a specific simulation run.
 *
 * @returns {JSX.Element} The simulation run detail page component
 */
function SimulationRunDetail() {
  const { id } = useParams();
  const [runInfo, setRunInfo] = useState(null);
  const [results, setResults] = useState(null);
  const [artefacts, setArtefacts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [resultsError, setResultsError] = useState(null);
  const [now, setNow] = useState(() => Date.now());
  const [isCancelling, setIsCancelling] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);

  const runStatus = runInfo?.status;
  const isTerminal = runStatus ? terminalStatuses.has(runStatus) : false;

  const loadRunInfo = useCallback(async () => {
    if (!id) return;
    try {
      const response = await simulationRunsApi.getRun(id);
      setRunInfo(response.data);
      setError(null);
    } catch (err) {
      handleApiError(err, setError, 'Failed to load simulation run');
    }
  }, [id]);

  const handleCancelRun = useCallback(async () => {
    if (!runInfo?.id) {
      return;
    }
    try {
      setIsCancelling(true);
      const response = await simulationRunsApi.cancelRun(runInfo.id);
      setRunInfo(response.data);
      setError(null);
    } catch (err) {
      handleApiError(err, setError, 'Failed to cancel simulation run');
    } finally {
      setIsCancelling(false);
    }
  }, [runInfo?.id]);

  useEffect(() => {
    const loadInitial = async () => {
      setLoading(true);
      await loadRunInfo();
      setLoading(false);
    };
    loadInitial();
  }, [loadRunInfo]);

  // Poll for status updates while running
  useEffect(() => {
    if (!runInfo?.id || isTerminal) return undefined;

    const intervalId = setInterval(() => {
      setNow(Date.now());
      loadRunInfo();
    }, 3000);

    return () => clearInterval(intervalId);
  }, [runInfo?.id, isTerminal, loadRunInfo]);

  // Load results and artefacts when terminal
  useEffect(() => {
    if (!runInfo?.id || !isTerminal) return;

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

  const elapsedMs = useMemo(() => {
    if (!runInfo?.createdAt) return null;
    const baseTime = runInfo.startedAt || runInfo.createdAt;
    const start = new Date(baseTime).getTime();
    const endTime = runInfo.endedAt ? new Date(runInfo.endedAt).getTime() : now;
    return Math.max(0, endTime - start);
  }, [now, runInfo]);

  const progressPercentage = useMemo(() => {
    if (!runInfo?.totalTicks || runInfo.totalTicks === 0 || runInfo.currentTick == null) {
      return null;
    }
    return Math.min(100, (runInfo.currentTick / runInfo.totalTicks) * 100);
  }, [runInfo]);

  const artefactDownloadUrl = (path) => {
    if (!runInfo?.id || !path) return '#';
    return `${normalizedApiBaseUrl}/simulation-runs/${runInfo.id}/artefacts/${encodeURI(path)}`;
  };

  const handleArtefactDownload = useCallback(
    async (event, artefact) => {
      event.preventDefault();
      if (!runInfo?.id || !artefact?.path) {
        setResultsError('Artefact path is unavailable for download.');
        return;
      }

      try {
        setResultsError(null);
        const response = await fetch(artefactDownloadUrl(artefact.path));
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

  const formatDuration = (durationMs) => {
    if (durationMs == null) return '—';
    const totalSeconds = Math.floor(durationMs / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    if (minutes > 0) {
      return `${minutes}m ${seconds}s`;
    }
    return `${seconds}s`;
  };

  const formatNumber = (value) => {
    if (value == null || Number.isNaN(value)) return '—';
    if (typeof value === 'number') {
      return Number.isInteger(value) ? value.toString() : value.toFixed(2);
    }
    return String(value);
  };

  const formatKpiValue = (key, value) => {
    if (key === 'utilisation' && typeof value === 'number') {
      return `${(value * 100).toFixed(1)}%`;
    }
    return formatNumber(value);
  };

  const formatBytes = (bytes) => {
    if (!bytes && bytes !== 0) return '—';
    const units = ['B', 'KB', 'MB', 'GB'];
    let size = bytes;
    let index = 0;
    while (size >= 1024 && index < units.length - 1) {
      size /= 1024;
      index += 1;
    }
    return `${size.toFixed(1)} ${units[index]}`;
  };

  const formatDate = (dateString) => {
    if (!dateString) return '—';
    return new Date(dateString).toLocaleString();
  };

  const runSummary = results?.results?.runSummary;
  const kpis = results?.results?.kpis;
  const perLift = results?.results?.perLift || [];
  const perFloor = results?.results?.perFloor || [];
  const inputArtefact = artefacts.find((item) => item.name === 'input.scenario');

  if (loading) {
    return (
      <div className="simulation-run-detail">
        <p>Loading simulation run...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="simulation-run-detail">
        <div className="page-header">
          <Link to="/simulation-runs" className="back-link">
            &larr; Back to Runs
          </Link>
        </div>
        <p className="error">{error}</p>
      </div>
    );
  }

  if (!runInfo) {
    return (
      <div className="simulation-run-detail">
        <div className="page-header">
          <Link to="/simulation-runs" className="back-link">
            &larr; Back to Runs
          </Link>
        </div>
        <p>Simulation run not found.</p>
      </div>
    );
  }

  return (
    <div className="simulation-run-detail">
      <div className="page-header">
        <div>
          <Link to="/simulation-runs" className="back-link">
            &larr; Back to Runs
          </Link>
          <h2>Simulation Run #{runInfo.id}</h2>
        </div>
      </div>

      <section className="detail-card">
        <div className="status-header">
          <h3>Run Status</h3>
          <div className="status-actions">
            <span className={`status-pill ${runStatus?.toLowerCase()}`}>
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
            <p>{runInfo.liftSystemId}</p>
          </div>
          <div>
            <span className="label">Version</span>
            <p>{runInfo.versionId}</p>
          </div>
          <div>
            <span className="label">Scenario</span>
            <p>{runInfo.scenarioId || '—'}</p>
          </div>
          <div>
            <span className="label">Created</span>
            <p>{formatDate(runInfo.createdAt)}</p>
          </div>
          <div>
            <span className="label">Started</span>
            <p>{formatDate(runInfo.startedAt)}</p>
          </div>
          <div>
            <span className="label">Ended</span>
            <p>{formatDate(runInfo.endedAt)}</p>
          </div>
          <div>
            <span className="label">Duration</span>
            <p>{formatDuration(elapsedMs)}</p>
          </div>
          <div>
            <span className="label">Seed</span>
            <p>{runInfo.seed ?? '—'}</p>
          </div>
        </div>

        {progressPercentage != null && !isTerminal && (
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
      </section>

      {isTerminal && (
        <section className="detail-card">
          <h3>Results</h3>
          {resultsError && <p className="error">{resultsError}</p>}

          {runStatus === 'FAILED' && (
            <div className="result-banner error">
              <strong>Simulation failed.</strong>
              <p>{results?.errorMessage || runInfo.errorMessage || 'Unknown error.'}</p>
              {results?.logsUrl && (
                <a className="link" href={results.logsUrl}>
                  View logs
                </a>
              )}
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
                          <th>Utilisation</th>
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
                            <td>{formatKpiValue('utilisation', lift.utilisation)}</td>
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

          {runStatus === 'SUCCEEDED' && inputArtefact && (
            <div className="results-section reproduce-section">
              <h4>Reproduce via CLI</h4>
              <p>
                Use the generated batch input file to reproduce this run with the
                CLI simulator.
              </p>
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
            </div>
          )}
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
    </div>
  );
}

export default SimulationRunDetail;
