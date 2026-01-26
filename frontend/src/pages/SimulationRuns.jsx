// @ts-check
import { useCallback, useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { liftSystemsApi } from '../api/liftSystemsApi';
import { simulationRunsApi } from '../api/simulationRunsApi';
import { handleApiError } from '../utils/errorHandlers';
import './SimulationRuns.css';

const statusOptions = ['ALL', 'SUCCEEDED', 'FAILED', 'RUNNING', 'CREATED', 'CANCELLED'];

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
  const [runs, setRuns] = useState([]);
  const [systems, setSystems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

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

  const loadRuns = useCallback(async () => {
    try {
      setLoading(true);
      const params = {};
      if (selectedSystemId) {
        params.systemId = selectedSystemId;
      }
      if (selectedStatus && selectedStatus !== 'ALL') {
        params.status = selectedStatus;
      }
      const response = await simulationRunsApi.listRuns(params);
      setRuns(response.data);
      setError(null);
    } catch (err) {
      handleApiError(err, setError, 'Failed to load simulation runs');
    } finally {
      setLoading(false);
    }
  }, [selectedSystemId, selectedStatus]);

  useEffect(() => {
    loadRuns();
  }, [loadRuns]);

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
    setSelectedSystemId(event.target.value);
  };

  const handleStatusChange = (event) => {
    setSelectedStatus(event.target.value);
  };

  const handleClearFilters = () => {
    setSelectedSystemId('');
    setSelectedStatus('ALL');
  };

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
          <table className="runs-table">
            <thead>
              <tr>
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
                    <Link
                      to={`/simulation-runs/${run.id}`}
                      className="btn-small btn-secondary"
                    >
                      View
                    </Link>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}

export default SimulationRuns;
