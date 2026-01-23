// @ts-check
import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { liftSystemsApi } from '../api/liftSystemsApi';
import { handleApiError } from '../utils/errorHandlers';
import './SimulatorLanding.css';

function SimulatorLanding() {
  const navigate = useNavigate();
  /** @type {import('../types/models').LiftSystem[]} */
  const [systems, setSystems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [versionsError, setVersionsError] = useState(null);
  const [selectedSystemId, setSelectedSystemId] = useState('');
  const [selectedVersionId, setSelectedVersionId] = useState('');
  const [versions, setVersions] = useState([]);
  const [isLoadingVersions, setIsLoadingVersions] = useState(false);

  useEffect(() => {
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

    loadSystems();
  }, []);

  useEffect(() => {
    if (!selectedSystemId) {
      setVersions([]);
      setSelectedVersionId('');
      return;
    }

    const loadVersions = async () => {
      try {
        setIsLoadingVersions(true);
        const response = await liftSystemsApi.getVersions(selectedSystemId);
        setVersions(response.data);
        setVersionsError(null);
      } catch (err) {
        handleApiError(err, setVersionsError, 'Failed to load versions');
      } finally {
        setIsLoadingVersions(false);
      }
    };

    loadVersions();
  }, [selectedSystemId]);

  const publishedVersions = useMemo(
    () => versions.filter((version) => version.status === 'PUBLISHED'),
    [versions]
  );

  const selectedSystem = systems.find(
    (system) => String(system.id) === String(selectedSystemId)
  );

  const handleProceed = () => {
    if (!selectedSystemId || !selectedVersionId) {
      return;
    }

    navigate(`/simulator/run?systemId=${selectedSystemId}&versionId=${selectedVersionId}`);
  };

  return (
    <div className="simulator-landing">
      <div className="page-header">
        <div>
          <h2>Simulator</h2>
          <p className="page-subtitle">
            Choose a lift system and a published version to start a simulation run.
          </p>
        </div>
        <div className="page-actions">
          <button
            className="btn-primary"
            onClick={handleProceed}
            disabled={!selectedSystemId || !selectedVersionId}
          >
            Continue to Simulation Setup
          </button>
        </div>
      </div>

      {loading ? (
        <p>Loading lift systems...</p>
      ) : error ? (
        <p className="error">{error}</p>
      ) : systems.length === 0 ? (
        <div className="empty-state">
          <p>No lift systems available. Create one to start a simulation run.</p>
        </div>
      ) : (
        <div className="simulator-landing-grid">
          <section className="system-list">
            <h3>Lift Systems</h3>
            <div className="system-cards">
              {systems.map((system) => {
                const isSelected = String(system.id) === String(selectedSystemId);
                return (
                  <div
                    key={system.id}
                    className={`system-card ${isSelected ? 'selected' : ''}`}
                  >
                    <div className="system-card-header">
                      <h4>{system.displayName}</h4>
                      <span className="system-key">{system.systemKey}</span>
                    </div>
                    {system.description && (
                      <p className="description">{system.description}</p>
                    )}
                    <button
                      className={isSelected ? 'btn-secondary' : 'btn-primary'}
                      onClick={() => {
                        setSelectedSystemId(String(system.id));
                        setSelectedVersionId('');
                      }}
                    >
                      {isSelected ? 'Selected' : 'Select System'}
                    </button>
                  </div>
                );
              })}
            </div>
          </section>

          <aside className="selection-panel">
            <h3>Selected System</h3>
            {selectedSystem ? (
              <div className="selection-summary">
                <div>
                  <span className="label">Name</span>
                  <p>{selectedSystem.displayName}</p>
                </div>
                <div>
                  <span className="label">Key</span>
                  <p>{selectedSystem.systemKey}</p>
                </div>
                <div>
                  <span className="label">Description</span>
                  <p>{selectedSystem.description || '—'}</p>
                </div>
              </div>
            ) : (
              <p>Select a lift system to review its details.</p>
            )}

            <label className="field">
              <span>Published Version</span>
              <select
                value={selectedVersionId}
                onChange={(event) => setSelectedVersionId(event.target.value)}
                disabled={!selectedSystemId || isLoadingVersions}
              >
                <option value="">Select a published version</option>
                {publishedVersions.map((version) => (
                  <option key={version.id} value={version.id}>
                    Version {version.versionNumber}
                  </option>
                ))}
              </select>
            </label>

            {isLoadingVersions && <p>Loading versions...</p>}
            {versionsError && <p className="error">{versionsError}</p>}
            {!isLoadingVersions && selectedSystemId && publishedVersions.length === 0 && (
              <p className="warning">No published versions available for this system.</p>
            )}

            <button
              className="btn-primary"
              onClick={handleProceed}
              disabled={!selectedSystemId || !selectedVersionId}
            >
              Continue to Simulation Setup
            </button>
          </aside>
        </div>
      )}
    </div>
  );
}

export default SimulatorLanding;
