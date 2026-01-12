import { useEffect, useState } from 'react';
import { liftSystemsApi } from '../api/liftSystemsApi';
import './LiftSystems.css';

function LiftSystems() {
  const [systems, setSystems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    loadSystems();
  }, []);

  const loadSystems = async () => {
    try {
      setLoading(true);
      const response = await liftSystemsApi.getAllSystems();
      setSystems(response.data);
      setError(null);
    } catch (err) {
      setError('Failed to load lift systems');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="lift-systems">
      <div className="page-header">
        <h2>Lift Systems</h2>
        <button className="btn-primary">Create New System</button>
      </div>

      {loading ? (
        <p>Loading...</p>
      ) : error ? (
        <p className="error">{error}</p>
      ) : systems.length === 0 ? (
        <div className="empty-state">
          <p>No lift systems found. Create your first system to get started.</p>
        </div>
      ) : (
        <div className="systems-grid">
          {systems.map((system) => (
            <div key={system.id} className="system-card">
              <h3>{system.displayName}</h3>
              <p className="system-key">System Key: {system.systemKey}</p>
              {system.description && <p className="description">{system.description}</p>}
              <div className="system-meta">
                <span>Versions: {system.versions?.length || 0}</span>
                <span>Created: {new Date(system.createdAt).toLocaleDateString()}</span>
              </div>
              <div className="card-actions">
                <button className="btn-secondary">View Details</button>
                <button className="btn-secondary">Manage Versions</button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default LiftSystems;
