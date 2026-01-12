import { useState } from 'react';
import { liftSystemsApi } from '../api/liftSystemsApi';
import './ConfigValidator.css';

function ConfigValidator() {
  const [config, setConfig] = useState('{\n  "floors": 10,\n  "lifts": 2,\n  "travelTicksPerFloor": 1,\n  "doorTransitionTicks": 2,\n  "doorDwellTicks": 3,\n  "doorReopenWindowTicks": 2,\n  "homeFloor": 0,\n  "idleTimeoutTicks": 5,\n  "controllerStrategy": "NEAREST_REQUEST_ROUTING",\n  "idleParkingMode": "PARK_TO_HOME_FLOOR"\n}');
  const [validating, setValidating] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const handleValidate = async () => {
    try {
      setValidating(true);
      setResult(null);
      setError(null);

      const configObject = JSON.parse(config);
      const response = await liftSystemsApi.validateConfig(configObject);
      setResult(response.data);
    } catch (err) {
      if (err.name === 'SyntaxError') {
        setError('Invalid JSON format');
      } else {
        setError(err.response?.data?.message || 'Validation failed');
      }
    } finally {
      setValidating(false);
    }
  };

  return (
    <div className="config-validator">
      <h2>Configuration Validator</h2>
      <p className="description">
        Validate your lift system configuration before creating or updating a version.
      </p>

      <div className="validator-layout">
        <div className="editor-section">
          <h3>Configuration JSON</h3>
          <textarea
            className="config-editor"
            value={config}
            onChange={(e) => setConfig(e.target.value)}
            spellCheck="false"
          />
          <button
            className="btn-primary"
            onClick={handleValidate}
            disabled={validating}
          >
            {validating ? 'Validating...' : 'Validate Configuration'}
          </button>
        </div>

        <div className="result-section">
          <h3>Validation Result</h3>
          {result && (
            <div className="result-success">
              <h4>Configuration is valid!</h4>
              <pre>{JSON.stringify(result, null, 2)}</pre>
            </div>
          )}
          {error && (
            <div className="result-error">
              <h4>Validation Error</h4>
              <p>{error}</p>
            </div>
          )}
          {!result && !error && (
            <p className="placeholder">Validation results will appear here</p>
          )}
        </div>
      </div>
    </div>
  );
}

export default ConfigValidator;
