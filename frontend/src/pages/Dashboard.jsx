import { useEffect, useState } from 'react';
import { liftSystemsApi } from '../api/liftSystemsApi';
import './Dashboard.css';

function Dashboard() {
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
    <div className="dashboard">
      <h2>Dashboard</h2>
      <div className="dashboard-content">
        <div className="card">
          <h3>Overview</h3>
          {loading ? (
            <p>Loading...</p>
          ) : error ? (
            <p className="error">{error}</p>
          ) : (
            <div className="stats">
              <div className="stat-item">
                <div className="stat-value">{systems.length}</div>
                <div className="stat-label">Lift Systems</div>
              </div>
              <div className="stat-item">
                <div className="stat-value">
                  {systems.reduce((sum, sys) => sum + (sys.versions?.length || 0), 0)}
                </div>
                <div className="stat-label">Total Versions</div>
              </div>
            </div>
          )}
        </div>

        <div className="card">
          <h3>Quick Actions</h3>
          <div className="actions">
            <a href="/systems" className="action-button">
              Manage Lift Systems
            </a>
            <a href="/config-validator" className="action-button">
              Validate Configuration
            </a>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Dashboard;
