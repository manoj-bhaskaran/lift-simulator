// @ts-check
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { scenariosApi } from '../api/scenariosApi';
import AlertModal from '../components/AlertModal';
import ConfirmModal from '../components/ConfirmModal';
import { handleApiError } from '../utils/errorHandlers';
import './Scenarios.css';

/**
 * Main scenarios listing page component.
 * Displays a searchable grid of all passenger flow scenarios with creation and navigation capabilities.
 *
 * Features:
 * - Search scenarios by name
 * - Create new scenarios
 * - Edit existing scenarios
 * - Delete scenarios
 * - Display scenario metadata (duration, passenger flows, creation date)
 *
 * @returns {JSX.Element} The scenarios page component
 */
function Scenarios() {
  const navigate = useNavigate();
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
      const response = await scenariosApi.getAllScenarios();
      setScenarios(response.data);
      setError(null);
    } catch (err) {
      handleApiError(err, setError, 'Failed to load scenarios');
    } finally {
      setLoading(false);
    }
  };

  /**
   * Navigates to the scenario create form.
   */
  const handleCreateScenario = () => {
    navigate('/scenarios/new');
  };

  /**
   * Navigates to the scenario edit form.
   *
   * @param {string|number} scenarioId - Unique identifier of the scenario
   */
  const handleEditScenario = (scenarioId) => {
    navigate(`/scenarios/${scenarioId}/edit`);
  };

  /**
   * Handles scenario deletion with confirmation.
   *
   * @param {string|number} scenarioId - Unique identifier of the scenario
   */
  const handleDeleteScenario = async (scenarioId) => {
    try {
      await scenariosApi.deleteScenario(scenarioId);
      setDeleteConfirm(null);
      await loadScenarios();
    } catch (err) {
      handleApiError(err, setAlertMessage, 'Failed to delete scenario');
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
      return name.includes(normalizedQuery);
    });
  }, [searchQuery, scenarios]);

  /**
   * Formats scenario JSON for display.
   *
   * @param {Object} scenarioJson - The scenario JSON object
   * @returns {Object} Display-friendly scenario info
   */
  const getScenarioInfo = (scenarioJson) => {
    if (!scenarioJson) return { duration: 'N/A', flowCount: 0, hasSeed: false };

    return {
      duration: scenarioJson.durationTicks || 'N/A',
      flowCount: scenarioJson.passengerFlows?.length || 0,
      hasSeed: scenarioJson.seed !== undefined && scenarioJson.seed !== null
    };
  };

  return (
    <div className="scenarios">
      <div className="page-header">
        <div className="page-title">
          <h2>Passenger Flow Scenarios</h2>
        </div>
        <div className="page-actions">
          <div className="search-input">
            <input
              type="search"
              placeholder="Search by name"
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
          {filteredScenarios.map((scenario) => {
            const info = getScenarioInfo(scenario.scenarioJson);
            return (
              <div key={scenario.id} className="scenario-card">
                <h3>{scenario.name}</h3>
                <div className="scenario-meta">
                  <span>Duration: {info.duration} ticks</span>
                  <span>Flows: {info.flowCount}</span>
                  {info.hasSeed && <span className="seed-badge">Seeded</span>}
                </div>
                {scenario.versionInfo && (
                  <div className="scenario-meta">
                    <span>System: {scenario.versionInfo.displayName}</span>
                    <span>Version: {scenario.versionInfo.versionNumber}</span>
                    <span>Floors: {scenario.versionInfo.minFloor} to {scenario.versionInfo.maxFloor}</span>
                  </div>
                )}
                <div className="scenario-details">
                  <span className="created-date">
                    Created: {new Date(scenario.createdAt).toLocaleDateString()}
                  </span>
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
                    onClick={() => setDeleteConfirm(scenario)}
                  >
                    Delete
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Delete Confirmation Modal */}
      <ConfirmModal
        isOpen={!!deleteConfirm}
        onClose={() => setDeleteConfirm(null)}
        title="Delete Scenario"
        message={`Are you sure you want to delete "${deleteConfirm?.name}"? This action cannot be undone.`}
        confirmText="Delete"
        confirmStyle="danger"
        onConfirm={() => handleDeleteScenario(deleteConfirm.id)}
      />

      {/* Error Alert Modal */}
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

export default Scenarios;
