// @ts-check

function RunFilters({
  systems,
  statusOptions,
  selectedSystemId,
  selectedStatus,
  hasFilters,
  onSystemChange,
  onStatusChange,
  onClearFilters,
}) {
  return (
    <div className="filters-section">
      <div className="filter-group">
        <label htmlFor="system-filter">Lift System</label>
        <select id="system-filter" value={selectedSystemId} onChange={onSystemChange}>
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
        <select id="status-filter" value={selectedStatus} onChange={onStatusChange}>
          {statusOptions.map((status) => (
            <option key={status} value={status}>
              {status === 'ALL' ? 'All Statuses' : status}
            </option>
          ))}
        </select>
      </div>

      {hasFilters && (
        <button className="btn-link" onClick={onClearFilters}>
          Clear Filters
        </button>
      )}
    </div>
  );
}

export default RunFilters;
