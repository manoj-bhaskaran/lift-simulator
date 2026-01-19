import { useState } from 'react';
import { liftSystemsApi } from '../api/liftSystemsApi';
import './ConfigValidator.css';

function ConfigValidator() {
  const [config, setConfig] = useState('{\n  "minFloor": 0,\n  "maxFloor": 9,\n  "lifts": 2,\n  "travelTicksPerFloor": 1,\n  "doorTransitionTicks": 2,\n  "doorDwellTicks": 3,\n  "doorReopenWindowTicks": 2,\n  "homeFloor": 0,\n  "idleTimeoutTicks": 5,\n  "controllerStrategy": "NEAREST_REQUEST_ROUTING",\n  "idleParkingMode": "PARK_TO_HOME_FLOOR"\n}');
  const [validating, setValidating] = useState(false);
  const [result, setResult] = useState(null);
  const [error, setError] = useState(null);

  const handleValidate = async () => {
    try {
      setValidating(true);
      setResult(null);
      setError(null);

      // Validate JSON syntax first
      JSON.parse(config);
      // Send config as string in request object
      const response = await liftSystemsApi.validateConfig({ config });
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
          {result && result.valid && (
            <div className="result-success">
              <h4>Configuration is valid!</h4>
              {result.warnings && result.warnings.length > 0 && (
                <div className="warnings">
                  <h5>Warnings:</h5>
                  <ul>
                    {result.warnings.map((warning, index) => (
                      <li key={index}>
                        <strong>{warning.field}:</strong> {warning.message}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}
          {result && !result.valid && (
            <div className="result-error">
              <h4>Validation Errors</h4>
              <ul>
                {result.errors.map((error, index) => (
                  <li key={index}>
                    <strong>{error.field}:</strong> {error.message}
                  </li>
                ))}
              </ul>
              {result.warnings && result.warnings.length > 0 && (
                <div className="warnings">
                  <h5>Warnings:</h5>
                  <ul>
                    {result.warnings.map((warning, index) => (
                      <li key={index}>
                        <strong>{warning.field}:</strong> {warning.message}
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}
          {error && (
            <div className="result-error">
              <h4>Error</h4>
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
