import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { liftSystemsApi } from '../api/liftSystemsApi';
import CreateSystemModal from '../components/CreateSystemModal';
import AlertModal from '../components/AlertModal';
import { handleApiError } from '../utils/errorHandlers';
import './LiftSystems.css';

/**
 * Main lift systems listing page component.
 * Displays a searchable grid of all lift systems with creation and navigation capabilities.
 *
 * Features:
 * - Search systems by display name or system key
 * - Create new lift systems via modal
 * - Navigate to system details or version management
 * - Display system metadata (version count, creation date)
 *
 * @returns {JSX.Element} The lift systems page component
 */
function LiftSystems() {
  const navigate = useNavigate();
  const [systems, setSystems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [alertMessage, setAlertMessage] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    loadSystems();
  }, []);

  /**
   * Loads all lift systems from the API.
   * Sets loading state and handles errors appropriately.
   */
  const loadSystems = async () => {
    try {
      setLoading(true);
      const response = await liftSystemsApi.getAllSystems();
      setSystems(response.data);
      setError(null);
    } catch (err) {
      handleApiError(err, setError, 'Failed to load lift systems');
    } finally {
      setLoading(false);
    }
  };

  /**
   * Handles lift system creation from the modal form.
   * On success, closes modal, refreshes list, and navigates to new system detail page.
   *
   * @param {Object} formData - Form data containing systemKey, displayName, and description
   */
  const handleCreateSystem = async (formData) => {
    try {
      const response = await liftSystemsApi.createSystem(formData);
      setShowCreateModal(false);
      await loadSystems();
      // Navigate to the newly created system
      navigate(`/systems/${response.data.id}`);
    } catch (err) {
      handleApiError(err, setAlertMessage, 'Failed to create system');
    }
  };

  /**
   * Navigates to the detailed view of a specific lift system.
   *
   * @param {string|number} systemId - Unique identifier of the system
   */
  const handleViewDetails = (systemId) => {
    navigate(`/systems/${systemId}`);
  };

  /**
   * Navigates to the version management section of a specific lift system.
   *
   * @param {string|number} systemId - Unique identifier of the system
   */
  const handleManageVersions = (systemId) => {
    navigate(`/systems/${systemId}#versions`);
  };

  /**
   * Memoized filtered list of systems based on search query.
   * Searches in both display name and system key fields (case-insensitive).
   *
   * @type {Array<Object>}
   */
  const filteredSystems = useMemo(() => {
    const normalizedQuery = searchQuery.trim().toLowerCase();
    if (!normalizedQuery) {
      return systems;
    }

    return systems.filter((system) => {
      const displayName = system.displayName?.toLowerCase() || '';
      const systemKey = system.systemKey?.toLowerCase() || '';
      return displayName.includes(normalizedQuery) || systemKey.includes(normalizedQuery);
    });
  }, [searchQuery, systems]);

  return (
    <div className="lift-systems">
      <div className="page-header">
        <div className="page-title">
          <h2>Lift Systems</h2>
        </div>
        <div className="page-actions">
          <div className="search-input">
            <input
              type="search"
              placeholder="Search by name or key"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              aria-label="Search lift systems"
            />
          </div>
          <button className="btn-primary" onClick={() => setShowCreateModal(true)}>
            Create New System
          </button>
        </div>
      </div>

      {loading ? (
        <p>Loading...</p>
      ) : error ? (
        <p className="error">{error}</p>
      ) : systems.length === 0 ? (
        <div className="empty-state">
          <p>No lift systems found. Create your first system to get started.</p>
        </div>
      ) : filteredSystems.length === 0 ? (
        <div className="empty-state">
          <p>No lift systems match your search. Try a different name or key.</p>
        </div>
      ) : (
        <div className="systems-grid">
          {filteredSystems.map((system) => (
            <div key={system.id} className="system-card">
              <h3>{system.displayName}</h3>
              <p className="system-key">System Key: {system.systemKey}</p>
              {system.description && <p className="description">{system.description}</p>}
              <div className="system-meta">
                <span>Versions: {system.versions?.length || 0}</span>
                <span>Created: {new Date(system.createdAt).toLocaleDateString()}</span>
              </div>
              <div className="card-actions">
                <button
                  className="btn-secondary"
                  onClick={() => handleViewDetails(system.id)}
                >
                  View Details
                </button>
                <button
                  className="btn-secondary"
                  onClick={() => handleManageVersions(system.id)}
                >
                  Manage Versions
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <CreateSystemModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onSubmit={handleCreateSystem}
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

export default LiftSystems;
