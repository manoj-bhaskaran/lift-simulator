import { useEffect, useState } from 'react';
import { liftSystemsApi } from '../api/liftSystemsApi';
import './HealthCheck.css';

function HealthCheck() {
  const [health, setHealth] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [lastChecked, setLastChecked] = useState(null);

  useEffect(() => {
    checkHealth();
  }, []);

  const checkHealth = async () => {
    try {
      setLoading(true);
      setError(null);
      const response = await liftSystemsApi.healthCheck();
      setHealth(response.data);
      setLastChecked(new Date());
    } catch (err) {
      setError('Failed to check health status');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="health-check">
      <div className="page-header">
        <h2>Health Check</h2>
        <button className="btn-primary" onClick={checkHealth} disabled={loading}>
          {loading ? 'Checking...' : 'Refresh'}
        </button>
      </div>

      {error ? (
        <div className="error-card">
          <h3>Service Unavailable</h3>
          <p>{error}</p>
          <p className="hint">Make sure the backend service is running on port 8080.</p>
        </div>
      ) : (
        <>
          {lastChecked && (
            <p className="last-checked">
              Last checked: {lastChecked.toLocaleTimeString()}
            </p>
          )}

          {loading ? (
            <p>Loading...</p>
          ) : health ? (
            <div className="health-status">
              <div className={`status-badge ${health.status?.toLowerCase()}`}>
                {health.status || 'UNKNOWN'}
              </div>

              {health.message && (
                <div className="card">
                  <h3>Message</h3>
                  <p>{health.message}</p>
                </div>
              )}

              {health.details && (
                <div className="card">
                  <h3>Details</h3>
                  <pre>{JSON.stringify(health.details, null, 2)}</pre>
                </div>
              )}
            </div>
          ) : (
            <p>No health data available</p>
          )}
        </>
      )}
    </div>
  );
}

export default HealthCheck;
