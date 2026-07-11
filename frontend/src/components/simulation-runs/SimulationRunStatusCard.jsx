import { formatDate, formatRunDuration, getRunStatusPillClass } from '../../utils/statusUtils';

function SimulationRunStatusCard({
  cardClassName = 'detail-card',
  title = 'Run Status',
  runInfo,
  runStatus,
  isTerminal = false,
  elapsedMs,
  progressPercentage,
  runError = null,
  onCancel,
  isCancelling = false,
  onDelete,
  isDeleting = false,
  showDeleteButton = false,
  hideProgressWhenTerminal = false,
  fields,
}) {
  const defaultFields = [
    { label: 'Run ID', value: runInfo.id },
    { label: 'Lift System', value: runInfo.liftSystemId },
    { label: 'Version', value: runInfo.versionNumber },
    { label: 'Scenario', value: runInfo.scenarioId || '—' },
    { label: 'Created', value: formatDate(runInfo.createdAt) },
    { label: 'Started', value: formatDate(runInfo.startedAt) },
    { label: 'Ended', value: formatDate(runInfo.endedAt) },
    { label: 'Duration', value: formatRunDuration(elapsedMs) },
    { label: 'Seed', value: runInfo.seed ?? '—' },
  ];
  const visibleFields = fields || defaultFields;
  const canCancel = runStatus === 'RUNNING' || runStatus === 'CREATED';
  const shouldShowProgress = progressPercentage != null && (!hideProgressWhenTerminal || !isTerminal);

  return (
    <section className={cardClassName}>
      <div className="status-header">
        <h3>{title}</h3>
        <div className="status-actions">
          <span className={getRunStatusPillClass(runStatus)}>{runStatus}</span>
          {canCancel && onCancel && (
            <button className="btn-danger" type="button" onClick={onCancel} disabled={isCancelling}>
              {isCancelling ? 'Cancelling...' : 'Cancel Run'}
            </button>
          )}
          {showDeleteButton && isTerminal && onDelete && (
            <button className="btn-danger" type="button" onClick={onDelete} disabled={isDeleting}>
              {isDeleting ? 'Deleting...' : 'Delete Run'}
            </button>
          )}
        </div>
      </div>

      <div className="status-grid">
        {visibleFields.map((field) => (
          <div key={field.label}>
            <span className="label">{field.label}</span>
            <p>{field.value}</p>
          </div>
        ))}
      </div>

      {shouldShowProgress && (
        <div className="progress-section">
          <div className="progress-meta">
            <span>{runInfo.currentTick ?? 0} / {runInfo.totalTicks} ticks</span>
            <span>{progressPercentage.toFixed(1)}%</span>
          </div>
          <div className="progress-bar">
            <div className="progress-fill" style={{ width: `${progressPercentage}%` }} />
          </div>
        </div>
      )}

      {runError && <p className="error">{runError}</p>}
    </section>
  );
}

export default SimulationRunStatusCard;
