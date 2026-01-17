import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link, useLocation } from 'react-router-dom';
import { liftSystemsApi } from '../api/liftSystemsApi';
import VersionActions from '../components/VersionActions';
import ConfirmModal from '../components/ConfirmModal';
import AlertModal from '../components/AlertModal';
import { getApiErrorMessage, handleApiError } from '../utils/errorHandlers';
import { getStatusBadgeClass } from '../utils/statusUtils';
import './LiftSystemDetail.css';

function LiftSystemDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const [system, setSystem] = useState(null);
  const [versions, setVersions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showCreateVersion, setShowCreateVersion] = useState(false);
  const [newVersionConfig, setNewVersionConfig] = useState('');
  const [creating, setCreating] = useState(false);
  const [runningVersion, setRunningVersion] = useState(null);
  const [simulationStatus, setSimulationStatus] = useState(null);

  const [showPublishConfirm, setShowPublishConfirm] = useState(false);
  const [versionToPublish, setVersionToPublish] = useState(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [alertMessage, setAlertMessage] = useState(null);

  // Pagination, sorting, and filtering states
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(10);
  const [sortBy, setSortBy] = useState('versionNumber');
  const [sortOrder, setSortOrder] = useState('desc');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [versionSearch, setVersionSearch] = useState('');

  useEffect(() => {
    loadSystemData();
  }, [id]);

  useEffect(() => {
    if (location.hash !== '#versions' || loading) {
      return;
    }

    const target = document.getElementById('versions');
    if (target) {
      target.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [location.hash, loading, versions.length]);

  const loadSystemData = async () => {
    try {
      setLoading(true);
      const [systemRes, versionsRes] = await Promise.all([
        liftSystemsApi.getSystem(id),
        liftSystemsApi.getVersions(id)
      ]);
      setSystem(systemRes.data);
      setVersions(versionsRes.data);
      setError(null);
    } catch (err) {
      handleApiError(err, setError, 'Failed to load system details');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateVersion = async (e) => {
    e.preventDefault();
    try {
      setCreating(true);
      await liftSystemsApi.createVersion(id, { config: newVersionConfig });
      setNewVersionConfig('');
      setShowCreateVersion(false);
      await loadSystemData();
    } catch (err) {
      handleApiError(err, setAlertMessage, 'Failed to create version');
    } finally {
      setCreating(false);
    }
  };

  const handlePublishVersion = (versionNumber) => {
    setVersionToPublish(versionNumber);
    setShowPublishConfirm(true);
  };

  const confirmPublish = async () => {
    try {
      await liftSystemsApi.publishVersion(id, versionToPublish);
      await loadSystemData();
    } catch (err) {
      handleApiError(err, setAlertMessage, 'Failed to publish version');
    }
  };

  const handleRunSimulation = async (versionNumber) => {
    setRunningVersion(versionNumber);
    setSimulationStatus(null);
    try {
      const response = await liftSystemsApi.runSimulation(system.systemKey);
      setSimulationStatus({ type: 'success', message: response.data.message });
    } catch (err) {
      setSimulationStatus({
        type: 'error',
        message: getApiErrorMessage(err, 'Failed to start simulator'),
      });
    } finally {
      setRunningVersion(null);
    }
  };

  const handleDeleteSystem = () => {
    setShowDeleteConfirm(true);
  };

  const confirmDelete = async () => {
    try {
      await liftSystemsApi.deleteSystem(id);
      navigate('/systems');
    } catch (err) {
      handleApiError(err, setAlertMessage, 'Failed to delete system');
    }
  };

  // Filter, sort, and paginate versions
  const getFilteredAndSortedVersions = () => {
    let filtered = [...versions];

    // Apply status filter
    if (statusFilter !== 'ALL') {
      filtered = filtered.filter((v) => v.status === statusFilter);
    }

    // Apply version number search
    if (versionSearch.trim()) {
      filtered = filtered.filter((v) =>
        v.versionNumber.toString().includes(versionSearch.trim())
      );
    }

    // Apply sorting
    filtered.sort((a, b) => {
      let comparison = 0;

      if (sortBy === 'versionNumber') {
        comparison = a.versionNumber - b.versionNumber;
      } else if (sortBy === 'createdAt') {
        comparison = new Date(a.createdAt) - new Date(b.createdAt);
      } else if (sortBy === 'status') {
        const statusOrder = { PUBLISHED: 1, DRAFT: 2, ARCHIVED: 3 };
        comparison = statusOrder[a.status] - statusOrder[b.status];
      }

      return sortOrder === 'asc' ? comparison : -comparison;
    });

    return filtered;
  };

  const filteredVersions = getFilteredAndSortedVersions();
  const totalPages = Math.ceil(filteredVersions.length / itemsPerPage);
  const startIndex = (currentPage - 1) * itemsPerPage;
  const endIndex = startIndex + itemsPerPage;
  const paginatedVersions = filteredVersions.slice(startIndex, endIndex);

  // Reset to page 1 when filters change
  useEffect(() => {
    setCurrentPage(1);
  }, [statusFilter, versionSearch, sortBy, sortOrder, itemsPerPage]);

  const handlePageChange = (newPage) => {
    if (newPage >= 1 && newPage <= totalPages) {
      setCurrentPage(newPage);
    }
  };

  const renderPageNumbers = () => {
    const pages = [];
    const maxPagesToShow = 5;
    let startPage = Math.max(1, currentPage - Math.floor(maxPagesToShow / 2));
    let endPage = Math.min(totalPages, startPage + maxPagesToShow - 1);

    if (endPage - startPage < maxPagesToShow - 1) {
      startPage = Math.max(1, endPage - maxPagesToShow + 1);
    }

    for (let i = startPage; i <= endPage; i++) {
      pages.push(
        <button
          key={i}
          onClick={() => handlePageChange(i)}
          className={`page-number ${i === currentPage ? 'active' : ''}`}
        >
          {i}
        </button>
      );
    }

    return pages;
  };

  if (loading) {
    return <div className="lift-system-detail"><p>Loading...</p></div>;
  }

  if (error || !system) {
    return (
      <div className="lift-system-detail">
        <p className="error">{error || 'System not found'}</p>
        <Link to="/systems" className="btn-secondary">Back to Systems</Link>
      </div>
    );
  }

  return (
    <div className="lift-system-detail">
      <div className="detail-header">
        <div>
          <Link to="/systems" className="breadcrumb">← Back to Systems</Link>
          <h2>{system.displayName}</h2>
          <p className="system-key">{system.systemKey}</p>
        </div>
        <button onClick={handleDeleteSystem} className="btn-danger">Delete System</button>
      </div>

      <div className="detail-section">
        <h3>System Information</h3>
        <div className="info-grid">
          <div className="info-item">
            <label>Display Name</label>
            <p>{system.displayName}</p>
          </div>
          <div className="info-item">
            <label>System Key</label>
            <p className="monospace">{system.systemKey}</p>
          </div>
          <div className="info-item">
            <label>Description</label>
            <p>{system.description || 'No description provided'}</p>
          </div>
          <div className="info-item">
            <label>Created</label>
            <p>{new Date(system.createdAt).toLocaleString()}</p>
          </div>
          <div className="info-item">
            <label>Last Updated</label>
            <p>{new Date(system.updatedAt).toLocaleString()}</p>
          </div>
        </div>
      </div>

      <div className="detail-section" id="versions">
        <div className="section-header">
          <h3>Versions ({filteredVersions.length} of {versions.length})</h3>
          <button
            onClick={() => setShowCreateVersion(!showCreateVersion)}
            className="btn-primary"
          >
            {showCreateVersion ? 'Cancel' : 'Create New Version'}
          </button>
        </div>

        {simulationStatus && (
          <div className={`simulation-status ${simulationStatus.type}`}>
            {simulationStatus.message}
          </div>
        )}

        {showCreateVersion && (
          <form onSubmit={handleCreateVersion} className="create-version-form">
            <label htmlFor="config">Configuration JSON</label>
            <textarea
              id="config"
              value={newVersionConfig}
              onChange={(e) => setNewVersionConfig(e.target.value)}
              placeholder='{"floors": 10, "lifts": 2, "travelTicksPerFloor": 10, ...}'
              rows="10"
              required
            />
            <div className="form-actions">
              <button type="submit" className="btn-primary" disabled={creating}>
                {creating ? 'Creating...' : 'Create Version'}
              </button>
              <button
                type="button"
                onClick={() => setShowCreateVersion(false)}
                className="btn-secondary"
              >
                Cancel
              </button>
            </div>
          </form>
        )}

        {versions.length === 0 ? (
          <div className="empty-state">
            <p>No versions yet. Create the first version to get started.</p>
          </div>
        ) : (
          <>
            <div className="versions-controls">
              <div className="controls-row">
                <div className="control-group">
                  <label htmlFor="versionSearch">Search Version:</label>
                  <input
                    id="versionSearch"
                    type="text"
                    placeholder="Search by version number..."
                    value={versionSearch}
                    onChange={(e) => setVersionSearch(e.target.value)}
                    className="search-input"
                  />
                </div>

                <div className="control-group">
                  <label htmlFor="statusFilter">Filter by Status:</label>
                  <select
                    id="statusFilter"
                    value={statusFilter}
                    onChange={(e) => setStatusFilter(e.target.value)}
                    className="filter-select"
                  >
                    <option value="ALL">All Statuses</option>
                    <option value="PUBLISHED">Published</option>
                    <option value="DRAFT">Draft</option>
                    <option value="ARCHIVED">Archived</option>
                  </select>
                </div>

                <div className="control-group">
                  <label htmlFor="sortBy">Sort by:</label>
                  <select
                    id="sortBy"
                    value={sortBy}
                    onChange={(e) => setSortBy(e.target.value)}
                    className="sort-select"
                  >
                    <option value="versionNumber">Version Number</option>
                    <option value="createdAt">Creation Date</option>
                    <option value="status">Status</option>
                  </select>
                </div>

                <div className="control-group">
                  <label htmlFor="sortOrder">Order:</label>
                  <select
                    id="sortOrder"
                    value={sortOrder}
                    onChange={(e) => setSortOrder(e.target.value)}
                    className="sort-select"
                  >
                    <option value="desc">
                      {sortBy === 'versionNumber' ? 'Descending' :
                       sortBy === 'createdAt' ? 'Newest First' :
                       'Published First'}
                    </option>
                    <option value="asc">
                      {sortBy === 'versionNumber' ? 'Ascending' :
                       sortBy === 'createdAt' ? 'Oldest First' :
                       'Archived First'}
                    </option>
                  </select>
                </div>

                <div className="control-group">
                  <label htmlFor="itemsPerPage">Items per page:</label>
                  <select
                    id="itemsPerPage"
                    value={itemsPerPage}
                    onChange={(e) => setItemsPerPage(Number(e.target.value))}
                    className="items-select"
                  >
                    <option value="10">10</option>
                    <option value="20">20</option>
                    <option value="50">50</option>
                    <option value="100">100</option>
                  </select>
                </div>
              </div>
            </div>

            {filteredVersions.length === 0 ? (
              <div className="empty-state">
                <p>No versions match your filters.</p>
              </div>
            ) : (
              <>
                <div className="versions-list">
                  {paginatedVersions.map((version) => (
              <div key={version.id} className="version-card">
                <div className="version-header">
                  <div>
                    <h4>Version {version.versionNumber}</h4>
                    <span className={getStatusBadgeClass(version.status)}>
                      {version.status}
                    </span>
                  </div>
                  <VersionActions
                    systemId={id}
                    versionNumber={version.versionNumber}
                    status={version.status}
                    onPublish={handlePublishVersion}
                    onRunSimulation={handleRunSimulation}
                    runningVersion={runningVersion}
                  />
                </div>
                <div className="version-info">
                  <div className="info-row">
                    <span className="label">Created:</span>
                    <span>{new Date(version.createdAt).toLocaleString()}</span>
                  </div>
                  {version.publishedAt && (
                    <div className="info-row">
                      <span className="label">Published:</span>
                      <span>{new Date(version.publishedAt).toLocaleString()}</span>
                    </div>
                  )}
                  <details className="config-details">
                    <summary>View Configuration</summary>
                    <pre className="config-preview">{JSON.stringify(JSON.parse(version.config), null, 2)}</pre>
                  </details>
                </div>
              </div>
            ))}
          </div>

          {totalPages > 1 && (
            <div className="pagination-controls">
              <div className="pagination-info">
                Showing {startIndex + 1}-{Math.min(endIndex, filteredVersions.length)} of {filteredVersions.length} versions
              </div>
              <div className="pagination-buttons">
                <button
                  onClick={() => handlePageChange(1)}
                  disabled={currentPage === 1}
                  className="page-btn"
                  title="First page"
                >
                  &laquo;
                </button>
                <button
                  onClick={() => handlePageChange(currentPage - 1)}
                  disabled={currentPage === 1}
                  className="page-btn"
                  title="Previous page"
                >
                  &lsaquo;
                </button>
                {renderPageNumbers()}
                <button
                  onClick={() => handlePageChange(currentPage + 1)}
                  disabled={currentPage === totalPages}
                  className="page-btn"
                  title="Next page"
                >
                  &rsaquo;
                </button>
                <button
                  onClick={() => handlePageChange(totalPages)}
                  disabled={currentPage === totalPages}
                  className="page-btn"
                  title="Last page"
                >
                  &raquo;
                </button>
              </div>
            </div>
          )}
        </>
            )}
          </>
        )}
      </div>

      <ConfirmModal
        isOpen={showPublishConfirm}
        onClose={() => setShowPublishConfirm(false)}
        onConfirm={confirmPublish}
        title="Publish Version"
        message={`Are you sure you want to publish version ${versionToPublish}?`}
        confirmText="Publish"
        confirmStyle="primary"
      />

      <ConfirmModal
        isOpen={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        onConfirm={confirmDelete}
        title="Delete System"
        message={`Are you sure you want to delete "${system?.displayName}"? This will delete all versions as well.`}
        confirmText="Delete"
        confirmStyle="danger"
      />

      <AlertModal
        isOpen={!!alertMessage}
        onClose={() => setAlertMessage(null)}
        title="Error"
        message={alertMessage}
        type="error"
      />
    </div>
  );
}

export default LiftSystemDetail;
