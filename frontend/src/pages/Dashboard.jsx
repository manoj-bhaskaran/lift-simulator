import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { liftSystemsApi } from '../api/liftSystemsApi';
import { logApiError } from '../utils/errorHandlers';
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
      logApiError(err, 'Failed to load lift systems');
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
                  {systems.reduce(
                    (sum, sys) =>
                      sum + (sys.versionCount ?? sys.versions?.length ?? 0),
                    0
                  )}
                </div>
                <div className="stat-label">Configuration Versions</div>
              </div>
            </div>
          )}
        </div>

        <div className="card">
          <h3>Quick Actions</h3>
          <div className="actions">
            <Link to="/systems" className="action-button">
              Manage Lift Systems
            </Link>
            <Link to="/config-validator" className="action-button">
              Validate Configuration
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Dashboard;
