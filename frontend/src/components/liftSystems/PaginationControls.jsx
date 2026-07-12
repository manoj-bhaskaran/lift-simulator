// @ts-check

function PaginationControls({ totalPages, currentPage, startIndex, endIndex, totalItems, onPageChange, renderPageNumbers }) {
  if (totalPages <= 1) return null;

  return (
    <div className="pagination-controls">
      <div className="pagination-info">Showing {startIndex + 1}-{Math.min(endIndex, totalItems)} of {totalItems} versions</div>
      <div className="pagination-buttons">
        <button onClick={() => onPageChange(1)} disabled={currentPage === 1} className="page-btn" title="First page">&laquo;</button>
        <button onClick={() => onPageChange(currentPage - 1)} disabled={currentPage === 1} className="page-btn" title="Previous page">&lsaquo;</button>
        {renderPageNumbers()}
        <button onClick={() => onPageChange(currentPage + 1)} disabled={currentPage === totalPages} className="page-btn" title="Next page">&rsaquo;</button>
        <button onClick={() => onPageChange(totalPages)} disabled={currentPage === totalPages} className="page-btn" title="Last page">&raquo;</button>
      </div>
    </div>
  );
}

export default PaginationControls;
