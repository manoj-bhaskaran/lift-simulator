// @ts-check

function PaginationControls({
  totalPages,
  currentPage,
  startIndex,
  endIndex,
  totalItems,
  onPageChange,
  renderPageNumbers,
  itemLabel = 'items',
  pageBase = 1,
  simpleLabels = false,
}) {
  if (totalPages <= 1) return null;

  const firstPage = pageBase;
  const lastPage = pageBase + totalPages - 1;
  const displayPage = currentPage - pageBase + 1;

  return (
    <div className="pagination-controls">
      {Number.isFinite(startIndex) && Number.isFinite(endIndex) && Number.isFinite(totalItems) && (
        <div className="pagination-info">
          Showing {startIndex + 1}-{Math.min(endIndex, totalItems)} of {totalItems} {itemLabel}
        </div>
      )}
      <div className="pagination-buttons">
        <button onClick={() => onPageChange(firstPage)} disabled={currentPage === firstPage} className="page-btn" title="First page">&laquo;</button>
        <button onClick={() => onPageChange(currentPage - 1)} disabled={currentPage === firstPage} className="page-btn" title="Previous page">{simpleLabels ? '← Previous' : '‹'}</button>
        {renderPageNumbers ? renderPageNumbers() : <span className="pagination-info">Page {displayPage} of {totalPages}</span>}
        <button onClick={() => onPageChange(currentPage + 1)} disabled={currentPage === lastPage} className="page-btn" title="Next page">{simpleLabels ? 'Next →' : '›'}</button>
        <button onClick={() => onPageChange(lastPage)} disabled={currentPage === lastPage} className="page-btn" title="Last page">&raquo;</button>
      </div>
    </div>
  );
}

export default PaginationControls;
