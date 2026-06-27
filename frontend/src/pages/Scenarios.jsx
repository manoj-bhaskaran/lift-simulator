// @ts-check
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { scenariosApi } from '../api/scenariosApi';
import { liftSystemsApi } from '../api/liftSystemsApi';
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
  const [loadingDeleteImpact, setLoadingDeleteImpact] = useState(null);
  const [copyScenario, setCopyScenario] = useState(null);
  const [systems, setSystems] = useState([]);
  const [targetSystemId, setTargetSystemId] = useState('');
  const [targetVersions, setTargetVersions] = useState([]);
  const [targetVersionId, setTargetVersionId] = useState('');
  const [copying, setCopying] = useState(false);

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

  useEffect(() => {
    loadScenarios();
  }, []);

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
   * Opens the copy dialog and preselects the source scenario's lift system when available.
   *
   * @param {Object} scenario - Scenario selected for copying
   */
  const handleOpenCopy = async (scenario) => {
    setTargetVersionId('');
    setTargetVersions([]);
    const preferredSystemId = scenario.versionInfo?.liftSystemId ? String(scenario.versionInfo.liftSystemId) : '';
    setTargetSystemId(preferredSystemId);
    setCopyScenario(scenario);

    try {
      const systemsResponse = await liftSystemsApi.getAllSystems();
      setSystems(systemsResponse.data);
      if (preferredSystemId) {
        const versionsResponse = await liftSystemsApi.getVersions(preferredSystemId);
        setTargetVersions(versionsResponse.data);
      } else {
        setTargetVersions([]);
      }
    } catch (err) {
      handleApiError(err, setAlertMessage, 'Failed to load lift system versions');
    }
  };

  /**
   * Loads versions when the selected target system changes.
   *
   * @param {string} systemId - Selected lift system ID
   */
  const handleTargetSystemChange = async (systemId) => {
    setTargetSystemId(systemId);
    setTargetVersionId('');
    setTargetVersions([]);
    if (!systemId) {
      return;
    }

    try {
      const response = await liftSystemsApi.getVersions(systemId);
      setTargetVersions(response.data);
    } catch (err) {
      handleApiError(err, setAlertMessage, 'Failed to load versions');
    }
  };

  /**
   * Copies the selected scenario after backend validation against the target version.
   */
  const handleCopyScenario = async () => {
    if (!copyScenario || !targetVersionId) {
      setAlertMessage('Select a target lift system version before copying.');
      return;
    }

    try {
      setCopying(true);
      const response = await scenariosApi.copyScenario(copyScenario.id, {
        targetLiftSystemVersionId: Number(targetVersionId)
      });
      setCopyScenario(null);
      setAlertMessage(`Copied scenario as "${response.data.name}".`);
      await loadScenarios();
    } catch (err) {
      handleApiError(err, setAlertMessage, 'Failed to copy scenario');
    } finally {
      setCopying(false);
    }
  };

  /**
   * Opens scenario deletion confirmation with run-history impact details.
   *
   * @param {Object} scenario - Scenario selected for deletion
   */
  const handleOpenDeleteConfirm = async (scenario) => {
    setLoadingDeleteImpact(scenario.id);
    try {
      const response = await scenariosApi.getScenarioRunCount(scenario.id);
      setDeleteConfirm({ ...scenario, simulationRunCount: Number(response.data) || 0 });
    } catch (err) {
      handleApiError(err, setAlertMessage, 'Failed to load scenario deletion impact');
    } finally {
      setLoadingDeleteImpact(null);
    }
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
                    className="btn-secondary"
                    onClick={() => handleOpenCopy(scenario)}
                  >
                    Copy to Version
                  </button>
                  <button
                    className="btn-danger"
                    onClick={() => handleOpenDeleteConfirm(scenario)}
                    disabled={loadingDeleteImpact === scenario.id}
                  >
                    {loadingDeleteImpact === scenario.id ? 'Checking...' : 'Delete'}
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {copyScenario && (
        <div className="copy-panel" role="dialog" aria-modal="true" aria-labelledby="copy-scenario-title">
          <div className="copy-panel-content">
            <h3 id="copy-scenario-title">Copy "{copyScenario.name}" to another version</h3>
            <p>Select a target lift system version. The server validates the scenario against the target floor range before creating a copy.</p>
            <label htmlFor="targetSystem">Lift System</label>
            <select
              id="targetSystem"
              value={targetSystemId}
              onChange={(event) => handleTargetSystemChange(event.target.value)}
            >
              <option value="">Select a lift system</option>
              {systems.map((system) => (
                <option key={system.id} value={system.id}>{system.displayName}</option>
              ))}
            </select>
            <label htmlFor="targetVersion">Target Version</label>
            <select
              id="targetVersion"
              value={targetVersionId}
              onChange={(event) => setTargetVersionId(event.target.value)}
              disabled={!targetSystemId}
            >
              <option value="">Select a version</option>
              {targetVersions.map((version) => (
                <option key={version.id} value={version.id}>
                  Version {version.versionNumber} ({version.status})
                </option>
              ))}
            </select>
            <div className="copy-panel-actions">
              <button className="btn-secondary" onClick={() => setCopyScenario(null)}>Cancel</button>
              <button className="btn-primary" onClick={handleCopyScenario} disabled={copying || !targetVersionId}>
                {copying ? 'Copying...' : 'Copy Scenario'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      <ConfirmModal
        isOpen={!!deleteConfirm}
        onClose={() => setDeleteConfirm(null)}
        title="Delete Scenario"
        message={deleteConfirm?.simulationRunCount > 0
          ? `Deleting "${deleteConfirm?.name}" will permanently delete ${deleteConfirm.simulationRunCount} simulation run(s), their history, and their artefacts. This action cannot be undone.`
          : `Are you sure you want to delete "${deleteConfirm?.name}"? This action cannot be undone.`}
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
