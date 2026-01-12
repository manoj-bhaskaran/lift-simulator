import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { liftSystemsApi } from '../api/liftSystemsApi';
import './LiftSystemDetail.css';

function LiftSystemDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [system, setSystem] = useState(null);
  const [versions, setVersions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showCreateVersion, setShowCreateVersion] = useState(false);
  const [newVersionConfig, setNewVersionConfig] = useState('');
  const [creating, setCreating] = useState(false);
  const [runningVersion, setRunningVersion] = useState(null);
  const [simulationStatus, setSimulationStatus] = useState(null);

  useEffect(() => {
    loadSystemData();
  }, [id]);

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
      setError('Failed to load system details');
      console.error(err);
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
      alert('Failed to create version: ' + (err.response?.data?.message || err.message));
      console.error(err);
    } finally {
      setCreating(false);
    }
  };

  const handlePublishVersion = async (versionNumber) => {
    if (!confirm(`Are you sure you want to publish version ${versionNumber}?`)) {
      return;
    }
    try {
      await liftSystemsApi.publishVersion(id, versionNumber);
      await loadSystemData();
    } catch (err) {
      alert('Failed to publish version: ' + (err.response?.data?.message || err.message));
      console.error(err);
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
        message: 'Failed to start simulator: ' + (err.response?.data?.message || err.message),
      });
    } finally {
      setRunningVersion(null);
    }
  };

  const handleDeleteSystem = async () => {
    if (!confirm(`Are you sure you want to delete "${system?.displayName}"? This will delete all versions as well.`)) {
      return;
    }
    try {
      await liftSystemsApi.deleteSystem(id);
      navigate('/systems');
    } catch (err) {
      alert('Failed to delete system: ' + (err.response?.data?.message || err.message));
      console.error(err);
    }
  };

  const getStatusBadgeClass = (status) => {
    switch (status) {
      case 'PUBLISHED': return 'status-badge status-published';
      case 'DRAFT': return 'status-badge status-draft';
      case 'ARCHIVED': return 'status-badge status-archived';
      default: return 'status-badge';
    }
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

      <div className="detail-section">
        <div className="section-header">
          <h3>Versions ({versions.length})</h3>
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
          <div className="versions-list">
            {versions.map((version) => (
              <div key={version.id} className="version-card">
                <div className="version-header">
                  <div>
                    <h4>Version {version.versionNumber}</h4>
                    <span className={getStatusBadgeClass(version.status)}>
                      {version.status}
                    </span>
                  </div>
                  <div className="version-actions">
                    {version.status === 'DRAFT' && (
                      <>
                        <Link
                          to={`/systems/${id}/versions/${version.versionNumber}/edit`}
                          className="btn-secondary btn-sm"
                        >
                          Edit Config
                        </Link>
                        <button
                          onClick={() => handlePublishVersion(version.versionNumber)}
                          className="btn-primary btn-sm"
                        >
                          Publish
                        </button>
                      </>
                    )}
                    {version.status !== 'DRAFT' && (
                      <>
                        <Link
                          to={`/systems/${id}/versions/${version.versionNumber}/edit`}
                          className="btn-secondary btn-sm"
                        >
                          View Config
                        </Link>
                        {version.status === 'PUBLISHED' && (
                          <button
                            onClick={() => handleRunSimulation(version.versionNumber)}
                            className="btn-primary btn-sm"
                            disabled={runningVersion === version.versionNumber}
                          >
                            {runningVersion === version.versionNumber ? 'Starting...' : 'Run Simulator'}
                          </button>
                        )}
                      </>
                    )}
                  </div>
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
        )}
      </div>
    </div>
  );
}

export default LiftSystemDetail;
