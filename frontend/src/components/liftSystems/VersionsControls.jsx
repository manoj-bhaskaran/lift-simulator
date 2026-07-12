// @ts-check

function VersionsControls({ versionSearch, setVersionSearch, statusFilter, setStatusFilter, sortBy, setSortBy, sortOrder, setSortOrder, itemsPerPage, setItemsPerPage }) {
  return (
    <div className="versions-controls"><div className="controls-row">
      <div className="control-group"><label htmlFor="versionSearch">Search Version:</label><input id="versionSearch" type="text" placeholder="Search by version number..." value={versionSearch} onChange={(e) => setVersionSearch(e.target.value)} className="search-input" /></div>
      <div className="control-group"><label htmlFor="statusFilter">Filter by Status:</label><select id="statusFilter" value={statusFilter} onChange={(e) => setStatusFilter(e.target.value)} className="filter-select"><option value="ALL">All Statuses</option><option value="PUBLISHED">Published</option><option value="DRAFT">Draft</option><option value="ARCHIVED">Archived</option></select></div>
      <div className="control-group"><label htmlFor="sortBy">Sort by:</label><select id="sortBy" value={sortBy} onChange={(e) => setSortBy(e.target.value)} className="sort-select"><option value="versionNumber">Version Number</option><option value="createdAt">Creation Date</option><option value="status">Status</option></select></div>
      <div className="control-group"><label htmlFor="sortOrder">Order:</label><select id="sortOrder" value={sortOrder} onChange={(e) => setSortOrder(e.target.value)} className="sort-select"><option value="desc">{sortBy === 'versionNumber' ? 'Descending' : sortBy === 'createdAt' ? 'Newest First' : 'Published First'}</option><option value="asc">{sortBy === 'versionNumber' ? 'Ascending' : sortBy === 'createdAt' ? 'Oldest First' : 'Archived First'}</option></select></div>
      <div className="control-group"><label htmlFor="itemsPerPage">Items per page:</label><select id="itemsPerPage" value={itemsPerPage} onChange={(e) => setItemsPerPage(Number(e.target.value))} className="items-select"><option value="10">10</option><option value="20">20</option><option value="50">50</option><option value="100">100</option></select></div>
    </div></div>
  );
}

export default VersionsControls;
