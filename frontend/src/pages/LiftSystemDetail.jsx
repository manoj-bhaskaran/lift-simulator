// @ts-check
import { useEffect, useState, useCallback } from 'react';
import { Link, useParams, useNavigate, useLocation } from 'react-router-dom';

import { liftSystemsApi } from '../api/liftSystemsApi';
import ConfirmModal from '../components/ConfirmModal';
import AlertModal from '../components/AlertModal';
import EditSystemModal from '../components/EditSystemModal';
import CreateVersionForm from '../components/liftSystems/CreateVersionForm';
import LiftSystemHeader from '../components/liftSystems/LiftSystemHeader';
import LiftSystemInfoSection from '../components/liftSystems/LiftSystemInfoSection';
import PaginationControls from '../components/PaginationControls';
import VersionCard from '../components/liftSystems/VersionCard';
import VersionsControls from '../components/liftSystems/VersionsControls';
import { handleApiError } from '../utils/errorHandlers';
import {
  getDefaultVersionFormData,
  configToFormData,
  formDataToJson,
  validateVersionFormData
} from '../utils/versionConfigSchema';
import './LiftSystemDetail.css';

function formatVersionConfig(config) {
  if (typeof config !== 'string') {
    return JSON.stringify(config, null, 2);
  }

  try {
    return JSON.stringify(JSON.parse(config), null, 2);
  } catch (err) {
    console.warn('Version configuration is not valid JSON; showing raw value instead', err);
    return config;
  }
}

/**
 * Detailed view page for a specific lift system.
 * Displays system information, version management, and provides version filtering, sorting, and pagination.
 *
 * Features:
 * - System metadata display (name, key, description, timestamps)
 * - Version list with pagination, sorting, and filtering
 * - Version creation, publishing, and deletion
 * - Simulator execution for published versions
 * - System deletion with confirmation
 * - Auto-scroll to versions section when navigating from list view
 *
 * URL Parameters:
 * - id: System identifier
 * - #versions: Optional hash to scroll to versions section
 *
 * @returns {JSX.Element} The lift system detail page component
 */
function LiftSystemDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  /** @type {import('../types/models').LiftSystem | null} */
  const [system, setSystem] = useState(null);
  /** @type {import('../types/models').Version[]} */
  const [versions, setVersions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showCreateVersion, setShowCreateVersion] = useState(false);
  // 'guided' renders the structured form (default); 'json' renders the advanced JSON editor.
  const [editorMode, setEditorMode] = useState('guided');
  const [versionFormData, setVersionFormData] = useState(() => getDefaultVersionFormData());
  const [versionFormErrors, setVersionFormErrors] = useState({});
  const [newVersionConfig, setNewVersionConfig] = useState(() => formDataToJson(getDefaultVersionFormData()));
  const [creating, setCreating] = useState(false);
  const [validatingCreate, setValidatingCreate] = useState(false);
  /** @type {import('../types/models').ValidationResult | null} */
  const [createValidationResult, setCreateValidationResult] = useState(null);
  const [createValidationError, setCreateValidationError] = useState(null);

  const [showPublishConfirm, setShowPublishConfirm] = useState(false);
  /** @type {number | null} */
  const [versionToPublish, setVersionToPublish] = useState(null);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [showEditModal, setShowEditModal] = useState(false);
  const [alertMessage, setAlertMessage] = useState(null);

  // Pagination, sorting, and filtering states
  const [currentPage, setCurrentPage] = useState(1);
  const [itemsPerPage, setItemsPerPage] = useState(10);
  const [sortBy, setSortBy] = useState('versionNumber');
  const [sortOrder, setSortOrder] = useState('desc');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [versionSearch, setVersionSearch] = useState('');

  /**
   * Loads system metadata and all versions from the API.
   * Fetches data in parallel for improved performance.
   */
  const loadSystemData = useCallback(async () => {
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
  }, [id]);

  const handleNewVersionConfigChange = (e) => {
    setNewVersionConfig(e.target.value);
    setCreateValidationResult(null);
    setCreateValidationError(null);
  };

  /**
   * Handles edits to a single guided form field. Keeps the JSON representation
   * and client-side validation state in sync, and clears any stale server
   * validation result so the user re-validates after changes.
   *
   * @param {string} name - Configuration field name.
   * @param {string} value - New field value.
   */
  const handleVersionFormChange = (name, value) => {
    const updated = { ...versionFormData, [name]: value };
    setVersionFormData(updated);
    setNewVersionConfig(formDataToJson(updated));
    setVersionFormErrors(validateVersionFormData(updated));
    setCreateValidationResult(null);
    setCreateValidationError(null);
  };

  /**
   * Resets the create-version form back to its default guided state.
   */
  const resetCreateVersionForm = () => {
    const defaults = getDefaultVersionFormData();
    setVersionFormData(defaults);
    setVersionFormErrors({});
    setNewVersionConfig(formDataToJson(defaults));
    setEditorMode('guided');
    setCreateValidationResult(null);
    setCreateValidationError(null);
  };

  /**
   * Switches to the advanced JSON editor, serializing the current guided form
   * values so no data is lost when toggling modes.
   */
  const switchToJsonMode = () => {
    setNewVersionConfig(formDataToJson(versionFormData));
    setEditorMode('json');
    setCreateValidationResult(null);
    setCreateValidationError(null);
  };

  /**
   * Switches back to the guided form, parsing the current JSON into form fields
   * where feasible. If the JSON cannot be parsed, the previous guided values are
   * retained and the user is informed.
   */
  const switchToGuidedMode = () => {
    setCreateValidationResult(null);
    setCreateValidationError(null);
    try {
      const parsed = JSON.parse(newVersionConfig);
      const formData = configToFormData(parsed);
      setVersionFormData(formData);
      setVersionFormErrors(validateVersionFormData(formData));
    } catch {
      setCreateValidationError(
        'The JSON could not be parsed into the guided form, so your last form values are shown. Fix the JSON or continue editing in the form.'
      );
    }
    setEditorMode('guided');
  };

  /**
   * Cancels version creation, hiding and resetting the form.
   */
  const handleCancelCreateVersion = () => {
    resetCreateVersionForm();
    setShowCreateVersion(false);
  };

  /**
   * Toggles the create-version form visibility, resetting state when closing.
   */
  const toggleCreateVersion = () => {
    if (showCreateVersion) {
      handleCancelCreateVersion();
    } else {
      setShowCreateVersion(true);
    }
  };

  useEffect(() => {
    loadSystemData();
  }, [loadSystemData]);

  useEffect(() => {
    if (location.hash !== '#versions' || loading) {
      return;
    }

    const target = document.getElementById('versions');
    if (target) {
      target.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [location.hash, loading, versions.length]);

  /**
   * Handles new version creation from the inline form.
   * Creates version and refreshes the system data.
   *
   * @param {React.FormEvent} e - Form submission event
   */
  const handleCreateVersion = async (e) => {
    if (e?.preventDefault) {
      e.preventDefault();
    }
    try {
      if (!createValidationResult?.valid) {
        setCreateValidationError('Validate the configuration before creating the version.');
        return;
      }
      setCreating(true);
      await liftSystemsApi.createVersion(id, { config: newVersionConfig });
      resetCreateVersionForm();
      setShowCreateVersion(false);
      await loadSystemData();
    } catch (err) {
      handleApiError(err, setAlertMessage, 'Failed to create version');
    } finally {
      setCreating(false);
    }
  };

  /**
   * Validates the new version configuration before creation.
   */
  const handleValidateCreateVersion = async () => {
    try {
      setValidatingCreate(true);
      setCreateValidationError(null);
      setCreateValidationResult(null);
      JSON.parse(newVersionConfig);
      const response = await liftSystemsApi.validateConfig({ config: newVersionConfig });
      setCreateValidationResult(response.data);
    } catch (err) {
      if (err.name === 'SyntaxError') {
        setCreateValidationError('Invalid JSON format. Please fix syntax errors first.');
      } else {
        handleApiError(err, setAlertMessage, 'Validation failed');
      }
    } finally {
      setValidatingCreate(false);
    }
  };

  /**
   * Initiates version publishing workflow by showing confirmation modal.
   *
   * @param {number} versionNumber - Version number to publish
   */
  const handlePublishVersion = (versionNumber) => {
    setVersionToPublish(versionNumber);
    setShowPublishConfirm(true);
  };

  /**
   * Confirms and executes version publishing.
   * Publishes the version and refreshes the system data.
   */
  const confirmPublish = async () => {
    try {
      await liftSystemsApi.publishVersion(id, versionToPublish);
      await loadSystemData();
    } catch (err) {
      handleApiError(err, setAlertMessage, 'Failed to publish version');
    }
  };

  /**
   * Navigates to the simulator flow for a specific version.
   *
   * @param {number} versionNumber - Version number to run simulation for
   */
  const handleRunSimulation = (versionNumber) => {
    navigate(`/simulator/run?systemId=${id}&versionNumber=${versionNumber}`);
  };

  /**
   * Initiates system deletion workflow by showing confirmation modal.
   */
  const handleDeleteSystem = () => {
    setShowDeleteConfirm(true);
  };

  /**
   * Confirms and executes system deletion.
   * Deletes the system and navigates back to systems list.
   */
  const confirmDelete = async () => {
    try {
      await liftSystemsApi.deleteSystem(id);
      navigate('/systems');
    } catch (err) {
      handleApiError(err, setAlertMessage, 'Failed to delete system');
    }
  };

  /**
   * Handles system edit submission.
   * Updates the system and refreshes the system data.
   *
   * @param {Object} formData - Form data containing displayName and description
   */
  const handleEditSystem = async (formData) => {
    try {
      await liftSystemsApi.updateSystem(id, formData);
      await loadSystemData();
      setShowEditModal(false);
    } catch (err) {
      handleApiError(err, setAlertMessage, 'Failed to update system');
      throw err; // Re-throw to let modal handle loading state
    }
  };

  /**
   * Filters and sorts versions based on current filter/sort state.
   * Applies status filter, version number search, and sorting criteria.
   *
   * @returns {Array<Object>} Filtered and sorted array of versions
   */
  const getFilteredAndSortedVersions = () => {
    let filtered = [...versions];

    // Apply status filter
    if (statusFilter !== 'ALL') {
      filtered = filtered.filter((v) => v.status === statusFilter);
    }

    // Apply version number search
    if (versionSearch.trim()) {
      const searchTerm = versionSearch.trim();
      filtered = filtered.filter((v) =>
        v.versionNumber.toString() === searchTerm
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
        const statusOrder = { ARCHIVED: 1, DRAFT: 2, PUBLISHED: 3 };
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

  /**
   * Handles pagination page changes.
   * Only updates if the new page is within valid range.
   *
   * @param {number} newPage - Target page number (1-indexed)
   */
  const handlePageChange = (newPage) => {
    if (newPage >= 1 && newPage <= totalPages) {
      setCurrentPage(newPage);
    }
  };

  /**
   * Renders pagination number buttons with smart windowing.
   * Shows up to 5 page numbers centered around current page.
   *
   * @returns {Array<JSX.Element>} Array of page number button elements
   */
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
      <LiftSystemHeader system={system} onEdit={() => setShowEditModal(true)} onDelete={handleDeleteSystem} />
      <LiftSystemInfoSection system={system} />

      <div className="detail-section" id="versions">
        <div className="section-header">
          <h3>Versions ({filteredVersions.length} of {versions.length})</h3>
          <button onClick={toggleCreateVersion} className="btn-primary">
            {showCreateVersion ? 'Cancel' : 'Create New Version'}
          </button>
        </div>

        {showCreateVersion && (
          <CreateVersionForm
            versions={versions}
            editorMode={editorMode}
            switchToGuidedMode={switchToGuidedMode}
            switchToJsonMode={switchToJsonMode}
            versionFormData={versionFormData}
            versionFormErrors={versionFormErrors}
            handleVersionFormChange={handleVersionFormChange}
            newVersionConfig={newVersionConfig}
            handleNewVersionConfigChange={handleNewVersionConfigChange}
            creating={creating}
            createValidationResult={createValidationResult}
            handleCreateVersion={handleCreateVersion}
            validatingCreate={validatingCreate}
            handleValidateCreateVersion={handleValidateCreateVersion}
            handleCancelCreateVersion={handleCancelCreateVersion}
            createValidationError={createValidationError}
          />
        )}

        {versions.length === 0 ? (
          <div className="empty-state"><p>No versions yet. Create the first version to get started.</p></div>
        ) : (
          <>
            <VersionsControls
              versionSearch={versionSearch}
              setVersionSearch={setVersionSearch}
              statusFilter={statusFilter}
              setStatusFilter={setStatusFilter}
              sortBy={sortBy}
              setSortBy={setSortBy}
              sortOrder={sortOrder}
              setSortOrder={setSortOrder}
              itemsPerPage={itemsPerPage}
              setItemsPerPage={setItemsPerPage}
            />

            {filteredVersions.length === 0 ? (
              <div className="empty-state"><p>No versions match your filters.</p></div>
            ) : (
              <>
                <div className="versions-list">
                  {paginatedVersions.map((version) => (
                    <VersionCard
                      key={version.id}
                      version={version}
                      systemId={id}
                      onPublish={handlePublishVersion}
                      onRunSimulation={handleRunSimulation}
                      formatVersionConfig={formatVersionConfig}
                    />
                  ))}
                </div>
                <PaginationControls
                  totalPages={totalPages}
                  currentPage={currentPage}
                  startIndex={startIndex}
                  endIndex={endIndex}
                  totalItems={filteredVersions.length}
                  onPageChange={handlePageChange}
                  renderPageNumbers={renderPageNumbers}
                />
              </>
            )}
          </>
        )}
      </div>

      <ConfirmModal isOpen={showPublishConfirm} onClose={() => setShowPublishConfirm(false)} onConfirm={confirmPublish} title="Publish Version" message={`Are you sure you want to publish version ${versionToPublish}?`} confirmText="Publish" confirmStyle="primary" />
      <ConfirmModal isOpen={showDeleteConfirm} onClose={() => setShowDeleteConfirm(false)} onConfirm={confirmDelete} title="Delete System" message={`Are you sure you want to delete "${system?.displayName}"? This will delete all versions as well.`} confirmText="Delete" confirmStyle="danger" />
      <AlertModal isOpen={!!alertMessage} onClose={() => setAlertMessage(null)} title="Error" message={alertMessage} type="error" />
      <EditSystemModal isOpen={showEditModal} onClose={() => setShowEditModal(false)} onSubmit={handleEditSystem} system={system} />
    </div>
  );
}

export default LiftSystemDetail;
