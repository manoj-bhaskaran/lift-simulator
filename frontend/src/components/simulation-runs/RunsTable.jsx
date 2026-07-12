// @ts-check
import { Link } from 'react-router-dom';

import { isTerminalRunStatus } from '../../hooks/useRunPolling';
import { formatDate, formatRunDuration, getRunStatusBadgeClass } from '../../utils/statusUtils';

function formatDuration(startDate, endDate, nowMs) {
  if (!startDate) return '—';
  const start = new Date(startDate).getTime();
  const end = endDate ? new Date(endDate).getTime() : nowMs;
  return formatRunDuration(end - start);
}

function formatProgress(currentTick, totalTicks) {
  if (!totalTicks || totalTicks === 0 || currentTick == null) return '—';
  const percentage = (currentTick / totalTicks) * 100;
  return `${percentage.toFixed(1)}%`;
}

function RunsTable({
  runs,
  selectedRunIds,
  allVisibleSelected,
  selectedCount,
  canBulkCancel,
  canBulkDelete,
  isDeleting,
  isBulkProcessing,
  now,
  onToggleRunSelection,
  onToggleAllVisible,
  onRequestBulkAction,
  onRequestDelete,
}) {
  return (
    <>
      <div className="bulk-actions" aria-live="polite">
        <span>{selectedCount} selected</span>
        <button
          type="button"
          className="btn-small btn-secondary"
          onClick={() => onRequestBulkAction('cancel')}
          disabled={!canBulkCancel || isBulkProcessing}
          title={selectedCount > 0 && !canBulkCancel ? 'Select only CREATED or RUNNING runs to cancel.' : undefined}
        >
          Cancel Selected
        </button>
        <button
          type="button"
          className="btn-small btn-danger"
          onClick={() => onRequestBulkAction('delete')}
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
                onChange={onToggleAllVisible}
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
                  onChange={() => onToggleRunSelection(run.id)}
                />
              </td>
              <td className="run-id">#{run.id}</td>
              <td>{run.liftSystemName || `System ${run.liftSystemId}`}</td>
              <td>v{run.versionNumber}</td>
              <td>{run.scenarioName || '—'}</td>
              <td><span className={getRunStatusBadgeClass(run.status)}>{run.status}</span></td>
              <td>
                {run.status === 'RUNNING' ? (
                  <div className="progress-cell">
                    <span>{formatProgress(run.currentTick, run.totalTicks)}</span>
                    <div className="mini-progress">
                      <div
                        className="mini-progress-fill"
                        style={{ width: `${run.totalTicks ? (run.currentTick / run.totalTicks) * 100 : 0}%` }}
                      />
                    </div>
                  </div>
                ) : run.status === 'SUCCEEDED' || run.status === 'FAILED' ? '100%' : '—'}
              </td>
              <td>{formatDuration(run.startedAt, run.endedAt, now)}</td>
              <td>{formatDate(run.createdAt)}</td>
              <td>
                <div className="run-actions">
                  <Link to={`/simulation-runs/${run.id}`} className="btn-small btn-secondary">View</Link>
                  {isTerminalRunStatus(run.status) && (
                    <button
                      type="button"
                      className="btn-small btn-danger"
                      onClick={() => onRequestDelete(run)}
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
    </>
  );
}

export default RunsTable;
