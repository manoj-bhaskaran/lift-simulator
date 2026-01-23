// @ts-check
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { liftSystemsApi } from '../api/liftSystemsApi';
import ConfirmModal from '../components/ConfirmModal';
import AlertModal from '../components/AlertModal';
import { handleApiError } from '../utils/errorHandlers';
import './ScenarioList.css';

/**
 * Main scenarios listing page component.
 * Displays a searchable grid of all passenger flow scenarios with creation and management capabilities.
 *
 * Features:
 * - Search scenarios by name
 * - Create new scenarios
 * - Edit existing scenarios
 * - Delete scenarios with confirmation
 * - Navigate to scenario details
 * - Display scenario metadata (ticks, floors, events)
 *
 * @returns {JSX.Element} The scenarios page component
 */
function ScenarioList() {
  const navigate = useNavigate();
  /** @type {[import('../types/models').Scenario[], Function]} */
  const [scenarios, setScenarios] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [alertMessage, setAlertMessage] = useState(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [deleteConfirm, setDeleteConfirm] = useState(null);

  useEffect(() => {
    loadScenarios();
  }, []);

  /**
   * Loads all scenarios from the API.
   * Sets loading state and handles errors appropriately.
   */
  const loadScenarios = async () => {
    try {
      setLoading(true);
      const response = await liftSystemsApi.getAllScenarios();
      setScenarios(response.data);
      setError(null);
    } catch (err) {
      handleApiError(err, setError, 'Failed to load scenarios');
    } finally {
      setLoading(false);
    }
  };

  /**
   * Navigates to the scenario creation page.
   */
  const handleCreateScenario = () => {
    navigate('/scenarios/new');
  };

  /**
   * Navigates to the scenario edit page.
   *
   * @param {number} scenarioId - Unique identifier of the scenario
   */
  const handleEditScenario = (scenarioId) => {
    navigate(`/scenarios/${scenarioId}/edit`);
  };

  /**
   * Initiates the delete confirmation flow.
   *
   * @param {import('../types/models').Scenario} scenario - The scenario to delete
   */
  const handleDeleteClick = (scenario) => {
    setDeleteConfirm(scenario);
  };

  /**
   * Deletes a scenario after confirmation.
   */
  const handleConfirmDelete = async () => {
    if (!deleteConfirm) return;

    try {
      await liftSystemsApi.deleteScenario(deleteConfirm.id);
      setDeleteConfirm(null);
      await loadScenarios();
    } catch (err) {
      handleApiError(err, setAlertMessage, 'Failed to delete scenario');
      setDeleteConfirm(null);
    }
  };

  /**
   * Memoized filtered list of scenarios based on search query.
   * Searches in scenario name field (case-insensitive).
   *
   * @type {Array<Object>}
   */
  const filteredScenarios = useMemo(() => {
    const normalizedQuery = searchQuery.trim().toLowerCase();
    if (!normalizedQuery) {
      return scenarios;
    }

    return scenarios.filter((scenario) => {
      const name = scenario.name?.toLowerCase() || '';
      const description = scenario.description?.toLowerCase() || '';
      return name.includes(normalizedQuery) || description.includes(normalizedQuery);
    });
  }, [searchQuery, scenarios]);

  return (
    <div className="scenario-list">
      <div className="page-header">
        <div className="page-title">
          <h2>Passenger Flow Scenarios</h2>
        </div>
        <div className="page-actions">
          <div className="search-input">
            <input
              type="search"
              placeholder="Search scenarios"
              value={searchQuery}
              onChange={(event) => setSearchQuery(event.target.value)}
              aria-label="Search scenarios"
            />
          </div>
          <button className="btn-primary" onClick={handleCreateScenario}>
            Create New Scenario
          </button>
        </div>
      </div>

      {loading ? (
        <p>Loading...</p>
      ) : error ? (
        <p className="error">{error}</p>
      ) : scenarios.length === 0 ? (
        <div className="empty-state">
          <p>No scenarios found. Create your first scenario to get started.</p>
        </div>
      ) : filteredScenarios.length === 0 ? (
        <div className="empty-state">
          <p>No scenarios match your search. Try a different name.</p>
        </div>
      ) : (
        <div className="scenarios-grid">
          {filteredScenarios.map((scenario) => (
            <div key={scenario.id} className="scenario-card">
              <h3>{scenario.name}</h3>
              {scenario.description && (
                <p className="description">{scenario.description}</p>
              )}
              <div className="scenario-details">
                <div className="detail-item">
                  <span className="detail-label">Duration:</span>
                  <span className="detail-value">{scenario.totalTicks} ticks</span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Floors:</span>
                  <span className="detail-value">
                    {scenario.minFloor} to {scenario.maxFloor}
                  </span>
                </div>
                <div className="detail-item">
                  <span className="detail-label">Controller:</span>
                  <span className="detail-value">{scenario.controllerStrategy}</span>
                </div>
                {scenario.events && scenario.events.length > 0 && (
                  <div className="detail-item">
                    <span className="detail-label">Events:</span>
                    <span className="detail-value">{scenario.events.length}</span>
                  </div>
                )}
              </div>
              <div className="scenario-meta">
                <span>Created: {new Date(scenario.createdAt).toLocaleDateString()}</span>
              </div>
              <div className="card-actions">
                <button
                  className="btn-secondary"
                  onClick={() => handleEditScenario(scenario.id)}
                >
                  Edit
                </button>
                <button
                  className="btn-danger"
                  onClick={() => handleDeleteClick(scenario)}
                >
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <ConfirmModal
        isOpen={!!deleteConfirm}
        onClose={() => setDeleteConfirm(null)}
        onConfirm={handleConfirmDelete}
        title="Delete Scenario"
        message={`Are you sure you want to delete scenario "${deleteConfirm?.name}"? This action cannot be undone.`}
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

export default ScenarioList;
