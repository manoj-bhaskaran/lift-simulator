// @ts-check
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import { liftSystemsApi } from '../api/liftSystemsApi';
import { simulationRunsApi } from '../api/simulationRunsApi';
import AlertModal from '../components/AlertModal';
import ConfirmModal from '../components/ConfirmModal';
import { getApiErrorMessage, handleApiError } from '../utils/errorHandlers';
import './SimulationRuns.css';

const statusOptions = ['ALL', 'SUCCEEDED', 'FAILED', 'RUNNING', 'CREATED', 'CANCELLED'];
const activeStatuses = new Set(['RUNNING', 'CREATED']);
const terminalStatuses = new Set(['SUCCEEDED', 'FAILED', 'CANCELLED']);

/**
 * Simulation runs history page component.
 * Displays a filterable list of all simulation runs with their status and key metrics.
 *
 * Features:
 * - Filter by lift system
 * - Filter by status
 * - View run details and results
 * - Navigate to view full results
 *
 * @returns {JSX.Element} The simulation runs page component
 */
function SimulationRuns() {
  const [searchParams, setSearchParams] = useSearchParams();
  const location = useLocation();
  const navigate = useNavigate();
  const [runs, setRuns] = useState([]);
  const [systems, setSystems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [runToDelete, setRunToDelete] = useState(null);
  const [bulkAction, setBulkAction] = useState(null);
  const [selectedRunIds, setSelectedRunIds] = useState([]);
  const [isDeleting, setIsDeleting] = useState(false);
  const [isBulkProcessing, setIsBulkProcessing] = useState(false);
  const [actionError, setActionError] = useState(null);
  const [actionErrorTitle, setActionErrorTitle] = useState('Action failed');
  const [actionNotice, setActionNotice] = useState(null);

  const [selectedSystemId, setSelectedSystemId] = useState(
    searchParams.get('systemId') || ''
  );
  const [selectedStatus, setSelectedStatus] = useState(
    searchParams.get('status') || 'ALL'
  );

  useEffect(() => {
    const loadSystems = async () => {
      try {
        const response = await liftSystemsApi.getAllSystems();
        setSystems(response.data);
      } catch (err) {
        handleApiError(err, setError, 'Failed to load lift systems');
      }
    };
    loadSystems();
  }, []);

  const loadRuns = useCallback(
    async (isPolling = false, page = currentPage) => {
      try {
        if (!isPolling) {
          setLoading(true);
        }
        const params = { page };
        if (selectedSystemId) {
          params.systemId = selectedSystemId;
        }
        if (selectedStatus && selectedStatus !== 'ALL') {
          params.status = selectedStatus;
        }
        const response = await simulationRunsApi.listRuns(params);
        const pageData = response.data;
        setRuns(pageData.content);
        setTotalPages(pageData.totalPages);
        setTotalElements(pageData.totalElements);
        setCurrentPage(pageData.number);
        setError(null);
      } catch (err) {
        handleApiError(err, setError, 'Failed to load simulation runs');
      } finally {
        if (!isPolling) {
          setLoading(false);
        }
      }
    },
    [selectedSystemId, selectedStatus, currentPage]
  );

  useEffect(() => {
    loadRuns(false);
  }, [loadRuns]);

  // Surface a one-time notice passed via navigation state (e.g. after deleting a
  // run from the detail page), then clear it so it does not reappear on refresh.
  useEffect(() => {
    if (location.state?.notice) {
      setActionNotice(location.state.notice);
      navigate(location.pathname + location.search, { replace: true, state: null });
    }
  }, [location, navigate]);

  // Check if any runs are active (RUNNING or CREATED) and need polling
  const hasActiveRuns = useMemo(
    () => runs.some((run) => activeStatuses.has(run.status)),
    [runs]
  );

  // Poll for updates when there are active runs
  useEffect(() => {
    if (!hasActiveRuns) return undefined;

    const intervalId = setInterval(() => {
      loadRuns(true);
    }, 3000);

    return () => clearInterval(intervalId);
  }, [hasActiveRuns, loadRuns]);

  useEffect(() => {
    setSelectedRunIds((currentIds) =>
      currentIds.filter((id) => runs.some((run) => run.id === id))
    );
  }, [runs]);

  const selectedRuns = useMemo(
    () => selectedRunIds
      .map((id) => runs.find((run) => run.id === id))
      .filter(Boolean),
    [selectedRunIds, runs]
  );
  const allVisibleSelected = runs.length > 0 && selectedRunIds.length === runs.length;
  const selectedCount = selectedRuns.length;
  const canBulkCancel = selectedCount > 0 && selectedRuns.every((run) => activeStatuses.has(run.status));
  const canBulkDelete = selectedCount > 0 && selectedRuns.every((run) => terminalStatuses.has(run.status));

  useEffect(() => {
    const params = new URLSearchParams();
    if (selectedSystemId) params.set('systemId', selectedSystemId);
    if (selectedStatus && selectedStatus !== 'ALL') params.set('status', selectedStatus);
    setSearchParams(params, { replace: true });
  }, [selectedSystemId, selectedStatus, setSearchParams]);

  /**
   * Formats a date for display.
   *
   * @param {string|null} dateString - ISO date string
   * @returns {string} Formatted date string
   */
  const formatDate = (dateString) => {
    if (!dateString) return '—';
    return new Date(dateString).toLocaleString();
  };

  /**
   * Calculates duration between two dates.
   *
   * @param {string|null} startDate - ISO start date string
   * @param {string|null} endDate - ISO end date string
   * @returns {string} Formatted duration string
   */
  const formatDuration = (startDate, endDate) => {
    if (!startDate) return '—';
    const start = new Date(startDate).getTime();
    const end = endDate ? new Date(endDate).getTime() : Date.now();
    const durationMs = end - start;
    const totalSeconds = Math.floor(durationMs / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    if (minutes > 0) {
      return `${minutes}m ${seconds}s`;
    }
    return `${seconds}s`;
  };

  /**
   * Calculates progress percentage.
   *
   * @param {number|null} currentTick - Current tick number
   * @param {number|null} totalTicks - Total tick count
   * @returns {string} Progress percentage string
   */
  const formatProgress = (currentTick, totalTicks) => {
    if (!totalTicks || totalTicks === 0 || currentTick == null) return '—';
    const percentage = (currentTick / totalTicks) * 100;
    return `${percentage.toFixed(1)}%`;
  };

  /**
   * Get the appropriate status CSS class.
   *
   * @param {string} status - Run status
   * @returns {string} CSS class name
   */
  const getStatusClass = (status) => {
    switch (status) {
      case 'SUCCEEDED':
        return 'status-succeeded';
      case 'FAILED':
        return 'status-failed';
      case 'RUNNING':
        return 'status-running';
      case 'CANCELLED':
        return 'status-cancelled';
      default:
        return 'status-created';
    }
  };

  const handleSystemChange = (event) => {
    setCurrentPage(0);
    setSelectedSystemId(event.target.value);
  };

  const handleStatusChange = (event) => {
    setCurrentPage(0);
    setSelectedStatus(event.target.value);
  };

  const handleClearFilters = () => {
    setCurrentPage(0);
    setSelectedSystemId('');
    setSelectedStatus('ALL');
  };

  const handlePageChange = (newPage) => {
    loadRuns(false, newPage);
  };

  const handleToggleRunSelection = (runId) => {
    setSelectedRunIds((currentIds) =>
      currentIds.includes(runId)
        ? currentIds.filter((id) => id !== runId)
        : [...currentIds, runId]
    );
  };

  const handleToggleAllVisible = () => {
    setSelectedRunIds(allVisibleSelected ? [] : runs.map((run) => run.id));
  };

  const handleRequestBulkAction = (type) => {
    setActionError(null);
    setActionNotice(null);
    setBulkAction(type);
  };

  const handleRequestDelete = (run) => {
    setActionError(null);
    setActionNotice(null);
    setRunToDelete(run);
  };

  const handleConfirmDelete = useCallback(async () => {
    if (!runToDelete) {
      return;
    }
    const runId = runToDelete.id;
    try {
      setIsDeleting(true);
      await simulationRunsApi.deleteRun(runId);
      setActionNotice(`Run #${runId} and its artefacts were deleted.`);
      setActionError(null);
      // When the deleted run was the only row on a non-first page, that page no
      // longer exists after removal, so step back a page to avoid landing on an
      // out-of-range page that renders as an empty state.
      const emptiesCurrentPage = runs.length === 1 && currentPage > 0;
      const targetPage = emptiesCurrentPage ? currentPage - 1 : currentPage;
      await loadRuns(false, targetPage);
    } catch (err) {
      setActionErrorTitle('Failed to delete run');
      setActionError(getApiErrorMessage(err, `Failed to delete run #${runId}`));
      console.error(err);
    } finally {
      setIsDeleting(false);
      setRunToDelete(null);
    }
  }, [runToDelete, loadRuns, runs.length, currentPage]);

  const handleConfirmBulkAction = useCallback(async () => {
    if (!bulkAction || selectedRuns.length === 0) {
      return;
    }
    const runIds = selectedRuns.map((run) => run.id);
    const actionName = bulkAction === 'cancel' ? 'cancel' : 'delete';
    const actionPastTense = bulkAction === 'cancel' ? 'cancelled' : 'deleted';
    const apiAction = bulkAction === 'cancel'
      ? simulationRunsApi.cancelRun
      : simulationRunsApi.deleteRun;
    try {
      setIsBulkProcessing(true);
      const outcomes = await Promise.allSettled(runIds.map((id) => apiAction(id)));
      const successCount = outcomes.filter((outcome) => outcome.status === 'fulfilled').length;
      const failureCount = outcomes.length - successCount;
      setActionNotice(
        `${successCount} run${successCount === 1 ? '' : 's'} ${actionPastTense} successfully`
        + (failureCount > 0 ? `; ${failureCount} failed.` : '.')
      );
      if (failureCount > 0) {
        setActionErrorTitle(`Failed to ${actionName} selected runs`);
        setActionError(
          `Failed to ${actionName} ${failureCount} selected run${failureCount === 1 ? '' : 's'}. `
          + 'Refresh the list and try again if needed.'
        );
      } else {
        setActionError(null);
      }
      setSelectedRunIds([]);
      const emptiesCurrentPage = bulkAction === 'delete' && successCount >= runs.length && currentPage > 0;
      await loadRuns(false, emptiesCurrentPage ? currentPage - 1 : currentPage);
    } catch (err) {
      setActionErrorTitle(`Failed to ${actionName} selected runs`);
      setActionError(getApiErrorMessage(err, `Failed to ${actionName} selected runs`));
      console.error(err);
    } finally {
      setIsBulkProcessing(false);
      setBulkAction(null);
    }
  }, [bulkAction, selectedRuns, loadRuns, runs.length, currentPage]);

  const hasFilters = selectedSystemId || (selectedStatus && selectedStatus !== 'ALL');

  return (
    <div className="simulation-runs">
      <div className="page-header">
        <div className="page-title">
          <h2>Simulation Runs</h2>
          <p className="page-subtitle">
            View history of all simulation runs and access their results.
          </p>
        </div>
        <div className="page-actions">
          <Link to="/simulator" className="btn-primary">
            New Simulation
          </Link>
        </div>
      </div>

      <div className="filters-section">
        <div className="filter-group">
          <label htmlFor="system-filter">Lift System</label>
          <select
            id="system-filter"
            value={selectedSystemId}
            onChange={handleSystemChange}
          >
            <option value="">All Systems</option>
            {systems.map((system) => (
              <option key={system.id} value={system.id}>
                {system.displayName}
              </option>
            ))}
          </select>
        </div>

        <div className="filter-group">
          <label htmlFor="status-filter">Status</label>
          <select
            id="status-filter"
            value={selectedStatus}
            onChange={handleStatusChange}
          >
            {statusOptions.map((status) => (
              <option key={status} value={status}>
                {status === 'ALL' ? 'All Statuses' : status}
              </option>
            ))}
          </select>
        </div>

        {hasFilters && (
          <button className="btn-link" onClick={handleClearFilters}>
            Clear Filters
          </button>
        )}
      </div>

      {actionNotice && (
        <p className="action-notice" role="status">
          {actionNotice}
        </p>
      )}

      {loading ? (
        <p>Loading runs...</p>
      ) : error ? (
        <p className="error">{error}</p>
      ) : runs.length === 0 ? (
        <div className="empty-state">
          <p>
            {hasFilters
              ? 'No simulation runs match your filters.'
              : 'No simulation runs found. Run a simulation to see results here.'}
          </p>
          {hasFilters && (
            <button className="btn-secondary" onClick={handleClearFilters}>
              Clear Filters
            </button>
          )}
        </div>
      ) : (
        <div className="runs-table-container">
          {totalElements > 0 && (
            <p className="pagination-summary">
              Showing {currentPage * 20 + 1}–{Math.min((currentPage + 1) * 20, totalElements)} of {totalElements} runs
            </p>
          )}
          <div className="bulk-actions" aria-live="polite">
            <span>{selectedCount} selected</span>
            <button
              type="button"
              className="btn-small btn-secondary"
              onClick={() => handleRequestBulkAction('cancel')}
              disabled={!canBulkCancel || isBulkProcessing}
              title={selectedCount > 0 && !canBulkCancel ? 'Select only CREATED or RUNNING runs to cancel.' : undefined}
            >
              Cancel Selected
            </button>
            <button
              type="button"
              className="btn-small btn-danger"
              onClick={() => handleRequestBulkAction('delete')}
              disabled={!canBulkDelete || isBulkProcessing}
              title={selectedCount > 0 && !canBulkDelete ? 'Select only completed runs to delete.' : undefined}
            >
              Delete Selected
            </button>
            {selectedCount > 0 && !(canBulkCancel || canBulkDelete) && (
              <span className="bulk-actions-hint">Mixed states selected; choose only active or completed runs.</span>
            )}
          </div>
          <table className="runs-table">
            <thead>
              <tr>
                <th className="select-column">
                  <input
                    type="checkbox"
                    aria-label="Select all visible runs"
                    checked={allVisibleSelected}
                    onChange={handleToggleAllVisible}
                  />
                </th>
                <th>ID</th>
                <th>System</th>
                <th>Version</th>
                <th>Scenario</th>
                <th>Status</th>
                <th>Progress</th>
                <th>Duration</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {runs.map((run) => (
                <tr key={run.id}>
                  <td className="select-column">
                    <input
                      type="checkbox"
                      aria-label={`Select run #${run.id}`}
                      checked={selectedRunIds.includes(run.id)}
                      onChange={() => handleToggleRunSelection(run.id)}
                    />
                  </td>
                  <td className="run-id">#{run.id}</td>
                  <td>{run.liftSystemName || `System ${run.liftSystemId}`}</td>
                  <td>v{run.versionNumber || run.versionId}</td>
                  <td>{run.scenarioName || '—'}</td>
                  <td>
                    <span className={`status-badge ${getStatusClass(run.status)}`}>
                      {run.status}
                    </span>
                  </td>
                  <td>
                    {run.status === 'RUNNING' ? (
                      <div className="progress-cell">
                        <span>{formatProgress(run.currentTick, run.totalTicks)}</span>
                        <div className="mini-progress">
                          <div
                            className="mini-progress-fill"
                            style={{
                              width: `${run.totalTicks ? (run.currentTick / run.totalTicks) * 100 : 0}%`,
                            }}
                          />
                        </div>
                      </div>
                    ) : run.status === 'SUCCEEDED' || run.status === 'FAILED' ? (
                      '100%'
                    ) : (
                      '—'
                    )}
                  </td>
                  <td>{formatDuration(run.startedAt, run.endedAt)}</td>
                  <td>{formatDate(run.createdAt)}</td>
                  <td>
                    <div className="run-actions">
                      <Link
                        to={`/simulation-runs/${run.id}`}
                        className="btn-small btn-secondary"
                      >
                        View
                      </Link>
                      {terminalStatuses.has(run.status) && (
                        <button
                          type="button"
                          className="btn-small btn-danger"
                          onClick={() => handleRequestDelete(run)}
                          disabled={isDeleting || isBulkProcessing}
                        >
                          Delete
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {totalPages > 1 && (
            <div className="pagination-controls">
              <button
                className="btn-secondary"
                onClick={() => handlePageChange(currentPage - 1)}
                disabled={currentPage === 0}
              >
                ← Previous
              </button>
              <span className="pagination-info">
                Page {currentPage + 1} of {totalPages}
              </span>
              <button
                className="btn-secondary"
                onClick={() => handlePageChange(currentPage + 1)}
                disabled={currentPage >= totalPages - 1}
              >
                Next →
              </button>
            </div>
          )}
        </div>
      )}

      <ConfirmModal
        isOpen={runToDelete !== null}
        onClose={() => setRunToDelete(null)}
        onConfirm={handleConfirmDelete}
        title="Delete simulation run?"
        message={
          runToDelete
            ? `This will permanently delete run #${runToDelete.id}, including its run history `
              + 'and all stored artefacts (generated input, logs, and result files). '
              + 'This action cannot be undone.'
            : ''
        }
        confirmText="Delete run"
        cancelText="Keep run"
        confirmStyle="danger"
      />

      <ConfirmModal
        isOpen={bulkAction !== null}
        onClose={() => setBulkAction(null)}
        onConfirm={handleConfirmBulkAction}
        title={bulkAction === 'cancel' ? 'Cancel selected runs?' : 'Delete selected runs?'}
        message={bulkAction === 'cancel'
          ? `This will request cancellation for ${selectedCount} selected active run${selectedCount === 1 ? '' : 's'}.`
          : `This will permanently delete ${selectedCount} selected completed run${selectedCount === 1 ? '' : 's'}, including history and stored artefacts. This action cannot be undone.`}
        confirmText={bulkAction === 'cancel' ? 'Cancel runs' : 'Delete runs'}
        cancelText="Keep runs"
        confirmStyle={bulkAction === 'delete' ? 'danger' : 'primary'}
      />

      <AlertModal
        isOpen={actionError !== null}
        onClose={() => setActionError(null)}
        title={actionErrorTitle}
        message={actionError || ''}
        type="error"
      />
    </div>
  );
}

export default SimulationRuns;
