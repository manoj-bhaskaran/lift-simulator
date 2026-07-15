// @ts-check
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { liftSystemsApi } from '../api/liftSystemsApi';
import { scenariosApi } from '../api/scenariosApi';
import { authHeaders, normalizedApiBaseUrl } from '../api/client';
import { simulationRunsApi } from '../api/simulationRunsApi';
import ConfirmModal from '../components/ConfirmModal';
import { handleApiError } from '../utils/errorHandlers';
import { isTerminalRunStatus, useRunPolling } from '../hooks/useRunPolling';
import { formatRunDuration } from '../utils/statusUtils';
import SimulationRunStatusCard from '../components/simulation-runs/SimulationRunStatusCard';
import SimulationResultsPanel from '../components/simulation-runs/SimulationResultsPanel';
import SimulatorRunSetup from '../components/simulation-runs/SimulatorRunSetup';
import './Simulator.css';

function Simulator() {
  /** @type {import('../types/models').LiftSystem[]} */
  const [systems, setSystems] = useState([]);
  const [scenarios, setScenarios] = useState([]);
  const [loading, setLoading] = useState(true);
  const [formError, setFormError] = useState(null);
  const [runError, setRunError] = useState(null);
  const [resultsError, setResultsError] = useState(null);

  const [runInfo, setRunInfo] = useState(null);
  const [results, setResults] = useState(null);
  const [artefacts, setArtefacts] = useState([]);
  const [now, setNow] = useState(() => Date.now());
  const [isStarting, setIsStarting] = useState(false);
  const [isCancelling, setIsCancelling] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [runSelection, setRunSelection] = useState({});

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

  const runStatus = runInfo?.status;
  const isTerminal = isTerminalRunStatus(runStatus);

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

  const handleStartRun = async (payload, selection) => {
    try {
      setIsStarting(true);
      const response = await simulationRunsApi.createRun(payload);
      setRunSelection(selection || {});
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

      <SimulatorRunSetup
        loading={loading}
        formError={formError}
        setFormError={setFormError}
        systems={systems}
        scenarios={scenarios}
        isStarting={isStarting}
        isRunActive={Boolean(runInfo && !isTerminal)}
        onStartRun={handleStartRun}
      />

      {runInfo && (
        <SimulationRunStatusCard
          cardClassName="simulator-card"
          runInfo={runInfo}
          runStatus={runStatus}
          isTerminal={isTerminal}
          elapsedMs={elapsedMs}
          progressPercentage={progressPercentage}
          runError={runError}
          onCancel={() => setShowCancelModal(true)}
          isCancelling={isCancelling}
          fields={[
            { label: 'Run ID', value: runInfo.id },
            { label: 'Lift System', value: runSelection.selectedSystem?.displayName || runInfo.liftSystemId },
            { label: 'Version', value: runSelection.selectedVersion ? `Version ${runSelection.selectedVersion.versionNumber}` : runInfo.versionNumber },
            { label: 'Scenario', value: runSelection.selectedScenario?.name || runInfo.scenarioId || '—' },
            { label: 'Elapsed Time', value: formatRunDuration(elapsedMs) },
            { label: 'Seed', value: runInfo.seed ?? '—' },
          ]}
        />
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
        <SimulationResultsPanel
          cardClassName="simulator-card"
          runInfo={runInfo}
          runStatus={runStatus}
          results={results}
          artefacts={artefacts}
          resultsError={resultsError}
          artefactDownloadUrl={artefactDownloadUrl}
          onArtefactDownload={handleArtefactDownload}
          showFullDetailsLink
          showMissingInputMessage
          onlyShowReproduceOnSuccess={false}
        />
      )}
    </div>
  );
}

export default Simulator;
