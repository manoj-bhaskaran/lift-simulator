// @ts-check
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { authHeaders, normalizedApiBaseUrl } from '../api/client';
import { simulationRunsApi } from '../api/simulationRunsApi';
import AlertModal from '../components/AlertModal';
import ConfirmModal from '../components/ConfirmModal';
import { getApiErrorMessage, handleApiError, logApiError } from '../utils/errorHandlers';
import { isTerminalRunStatus, useRunPolling } from '../hooks/useRunPolling';
import SimulationRunStatusCard from '../components/simulation-runs/SimulationRunStatusCard';
import SimulationResultsPanel from '../components/simulation-runs/SimulationResultsPanel';
import './SimulationRunDetail.css';

/**
 * Simulation run detail page component.
 * Displays the full details and results of a specific simulation run.
 *
 * @returns {JSX.Element} The simulation run detail page component
 */
function SimulationRunDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [runInfo, setRunInfo] = useState(null);
  const [results, setResults] = useState(null);
  const [artefacts, setArtefacts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [resultsError, setResultsError] = useState(null);
  const [now, setNow] = useState(() => Date.now());
  const [isCancelling, setIsCancelling] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [deleteError, setDeleteError] = useState(null);

  const runStatus = runInfo?.status;
  const isTerminal = isTerminalRunStatus(runStatus);

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
  }, [runInfo]);

  const handleDeleteRun = useCallback(async () => {
    if (!runInfo?.id) {
      return;
    }
    const runId = runInfo.id;
    try {
      setIsDeleting(true);
      setDeleteError(null);
      await simulationRunsApi.deleteRun(runId);
      navigate('/simulation-runs', {
        replace: true,
        state: { notice: `Run #${runId} and its artefacts were deleted.` },
      });
    } catch (err) {
      setDeleteError(getApiErrorMessage(err, `Failed to delete run #${runId}`));
      logApiError(err, `Failed to delete run #${runId}`);
    } finally {
      setIsDeleting(false);
    }
  }, [runInfo, navigate]);

  useEffect(() => {
    const loadInitial = async () => {
      setLoading(true);
      await loadRunInfo();
      setLoading(false);
    };
    loadInitial();
  }, [loadRunInfo]);

  // Poll for status updates while running
  useRunPolling(
    useCallback(() => {
      setNow(Date.now());
      loadRunInfo();
    }, [loadRunInfo]),
    { intervalMs: 3000, enabled: Boolean(runInfo?.id) && !isTerminal }
  );

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

  const artefactDownloadUrl = useCallback((path) => {
    if (!runInfo?.id || !path) return '#';
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

      <SimulationRunStatusCard
        runInfo={runInfo}
        runStatus={runStatus}
        isTerminal={isTerminal}
        elapsedMs={elapsedMs}
        progressPercentage={progressPercentage}
        onCancel={() => setShowCancelModal(true)}
        isCancelling={isCancelling}
        onDelete={() => {
          setDeleteError(null);
          setShowDeleteModal(true);
        }}
        isDeleting={isDeleting}
        showDeleteButton
        hideProgressWhenTerminal
      />

      {isTerminal && (
        <SimulationResultsPanel
          runInfo={runInfo}
          runStatus={runStatus}
          results={results}
          artefacts={artefacts}
          resultsError={resultsError}
          artefactDownloadUrl={artefactDownloadUrl}
          onArtefactDownload={handleArtefactDownload}
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

      <ConfirmModal
        isOpen={showDeleteModal}
        onClose={() => setShowDeleteModal(false)}
        onConfirm={handleDeleteRun}
        title="Delete simulation run?"
        message={
          `This will permanently delete run #${runInfo.id}, including its run history `
          + 'and all stored artefacts (generated input, logs, and result files). '
          + 'This action cannot be undone.'
        }
        confirmText="Delete run"
        cancelText="Keep run"
        confirmStyle="danger"
      />

      <AlertModal
        isOpen={deleteError !== null}
        onClose={() => setDeleteError(null)}
        title="Failed to delete run"
        message={deleteError || ''}
        type="error"
      />
    </div>
  );
}

export default SimulationRunDetail;
