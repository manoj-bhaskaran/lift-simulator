// @ts-check
import PaginationControls from '../PaginationControls';
import RunsTable from './RunsTable';

function RunListContent({
  loading,
  error,
  runs,
  hasFilters,
  currentPage,
  totalPages,
  totalElements,
  selectedRunIds,
  allVisibleSelected,
  selectedCount,
  canBulkCancel,
  canBulkDelete,
  isDeleting,
  isBulkProcessing,
  now,
  onClearFilters,
  onPageChange,
  onToggleRunSelection,
  onToggleAllVisible,
  onRequestBulkAction,
  onRequestDelete,
}) {
  if (loading) return <p>Loading runs...</p>;
  if (error) return <p className="error">{error}</p>;
  if (runs.length === 0) {
    return (
      <div className="empty-state">
        <p>{hasFilters ? 'No simulation runs match your filters.' : 'No simulation runs found. Run a simulation to see results here.'}</p>
        {hasFilters && <button className="btn-secondary" onClick={onClearFilters}>Clear Filters</button>}
      </div>
    );
  }

  return (
    <div className="runs-table-container">
      {totalElements > 0 && (
        <p className="pagination-summary">
          Showing {currentPage * 20 + 1}–{Math.min((currentPage + 1) * 20, totalElements)} of {totalElements} runs
        </p>
      )}
      <RunsTable
        runs={runs}
        selectedRunIds={selectedRunIds}
        allVisibleSelected={allVisibleSelected}
        selectedCount={selectedCount}
        canBulkCancel={canBulkCancel}
        canBulkDelete={canBulkDelete}
        isDeleting={isDeleting}
        isBulkProcessing={isBulkProcessing}
        now={now}
        onToggleRunSelection={onToggleRunSelection}
        onToggleAllVisible={onToggleAllVisible}
        onRequestBulkAction={onRequestBulkAction}
        onRequestDelete={onRequestDelete}
      />
      <PaginationControls
        totalPages={totalPages}
        currentPage={currentPage}
        startIndex={currentPage * 20}
        endIndex={(currentPage + 1) * 20}
        totalItems={totalElements}
        onPageChange={onPageChange}
        itemLabel="runs"
        pageBase={0}
        simpleLabels
      />
    </div>
  );
}

export default RunListContent;
