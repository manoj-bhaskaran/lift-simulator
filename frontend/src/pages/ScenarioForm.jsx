// @ts-check
import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { scenariosApi } from '../api/scenariosApi';
import AlertModal from '../components/AlertModal';
import PassengerFlowBuilder from '../components/PassengerFlowBuilder';
import { handleApiError } from '../utils/errorHandlers';
import './ScenarioForm.css';

/**
 * Scenario templates for quick start
 */
const SCENARIO_TEMPLATES = {
  blank: {
    name: 'Blank Scenario',
    description: 'Start from scratch',
    scenarioJson: {
      durationTicks: 100,
      passengerFlows: [],
      seed: null
    }
  },
  morningRush: {
    name: 'Morning Rush',
    description: 'Typical morning rush hour with ground floor pickups',
    scenarioJson: {
      durationTicks: 200,
      passengerFlows: [
        { startTick: 0, originFloor: 0, destinationFloor: 5, passengers: 3 },
        { startTick: 5, originFloor: 0, destinationFloor: 8, passengers: 2 },
        { startTick: 10, originFloor: 0, destinationFloor: 3, passengers: 4 },
        { startTick: 15, originFloor: 0, destinationFloor: 10, passengers: 2 }
      ],
      seed: 42
    }
  },
  eveningRush: {
    name: 'Evening Rush',
    description: 'Evening rush hour with descending traffic',
    scenarioJson: {
      durationTicks: 200,
      passengerFlows: [
        { startTick: 0, originFloor: 5, destinationFloor: 0, passengers: 3 },
        { startTick: 5, originFloor: 8, destinationFloor: 0, passengers: 2 },
        { startTick: 10, originFloor: 3, destinationFloor: 0, passengers: 4 },
        { startTick: 15, originFloor: 10, destinationFloor: 0, passengers: 2 }
      ],
      seed: 42
    }
  },
  interFloor: {
    name: 'Inter-Floor Traffic',
    description: 'Mixed traffic between different floors',
    scenarioJson: {
      durationTicks: 150,
      passengerFlows: [
        { startTick: 0, originFloor: 3, destinationFloor: 7, passengers: 2 },
        { startTick: 10, originFloor: 5, destinationFloor: 2, passengers: 1 },
        { startTick: 20, originFloor: 8, destinationFloor: 4, passengers: 3 }
      ],
      seed: 123
    }
  }
};

/**
 * Scenario form component for creating and editing passenger flow scenarios.
 * Supports template selection, manual configuration, and server-side validation.
 *
 * @returns {JSX.Element} The scenario form component
 */
function ScenarioForm() {
  const navigate = useNavigate();
  const { id } = useParams();
  const isEditMode = !!id;

  const [loading, setLoading] = useState(isEditMode);
  const [saving, setSaving] = useState(false);
  const [validating, setValidating] = useState(false);

  const [scenarioName, setScenarioName] = useState('');
  const [durationTicks, setDurationTicks] = useState(100);
  const [passengerFlows, setPassengerFlows] = useState([]);
  const [seed, setSeed] = useState('');
  const [useSeed, setUseSeed] = useState(false);

  const [validationErrors, setValidationErrors] = useState([]);
  const [validationWarnings, setValidationWarnings] = useState([]);
  const [formErrors, setFormErrors] = useState({});
  const [alertMessage, setAlertMessage] = useState(null);
  const [showAdvancedJson, setShowAdvancedJson] = useState(false);
  const [jsonText, setJsonText] = useState('');

  /**
   * Loads scenario data for editing.
   */
  const loadScenario = useCallback(async () => {
    try {
      setLoading(true);
      const response = await scenariosApi.getScenario(id);
      const scenario = response.data;

      setScenarioName(scenario.name || '');
      if (scenario.scenarioJson) {
        setDurationTicks(scenario.scenarioJson.durationTicks || 100);
        setPassengerFlows(scenario.scenarioJson.passengerFlows || []);
        if (scenario.scenarioJson.seed !== undefined && scenario.scenarioJson.seed !== null) {
          setSeed(String(scenario.scenarioJson.seed));
          setUseSeed(true);
        }
      }
    } catch (err) {
      handleApiError(err, setAlertMessage, 'Failed to load scenario');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    if (isEditMode) {
      loadScenario();
    }
  }, [isEditMode, loadScenario]);

  /**
   * Applies a scenario template.
   *
   * @param {string} templateKey - The template key to apply
   */
  const applyTemplate = (templateKey) => {
    const template = SCENARIO_TEMPLATES[templateKey];
    if (!template) return;

    const json = template.scenarioJson;
    setDurationTicks(json.durationTicks);
    setPassengerFlows(json.passengerFlows || []);
    if (json.seed !== null && json.seed !== undefined) {
      setSeed(String(json.seed));
      setUseSeed(true);
    } else {
      setSeed('');
      setUseSeed(false);
    }
    setValidationErrors([]);
    setValidationWarnings([]);
  };

  /**
   * Builds the scenario JSON from form state.
   *
   * @returns {Object} The scenario JSON
   */
  const buildScenarioJson = () => {
    const scenarioJson = {
      durationTicks: parseInt(durationTicks, 10),
      passengerFlows: passengerFlows
    };

    if (useSeed && seed.trim() !== '') {
      scenarioJson.seed = parseInt(seed, 10);
    }

    return scenarioJson;
  };

  /**
   * Validates the scenario using server-side validation.
   */
  const handleValidate = async () => {
    const errors = {};
    if (!scenarioName.trim()) {
      errors.scenarioName = 'Scenario name is required';
    }

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    try {
      setValidating(true);
      setValidationErrors([]);
      setValidationWarnings([]);

      const scenarioJson = buildScenarioJson();
      const response = await scenariosApi.validateScenario({
        name: scenarioName,
        scenarioJson: scenarioJson
      });

      if (response.data.valid) {
        setAlertMessage('Scenario is valid!');
      } else {
        const errors = response.data.errors || [];
        const warnings = response.data.warnings || [];
        setValidationErrors(errors);
        setValidationWarnings(warnings);
      }
    } catch (err) {
      handleApiError(err, setAlertMessage, 'Validation failed');
    } finally {
      setValidating(false);
    }
  };

  /**
   * Handles form submission.
   */
  const handleSubmit = async (e) => {
    e.preventDefault();

    const errors = {};
    if (!scenarioName.trim()) {
      errors.scenarioName = 'Scenario name is required';
    }

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    try {
      setSaving(true);
      setValidationErrors([]);
      setValidationWarnings([]);

      const scenarioJson = buildScenarioJson();
      const payload = {
        name: scenarioName,
        scenarioJson: scenarioJson
      };

      if (isEditMode) {
        await scenariosApi.updateScenario(id, payload);
      } else {
        await scenariosApi.createScenario(payload);
      }

      navigate('/scenarios');
    } catch (err) {
      // Check if it's a validation error
      if (err.response?.data?.errors) {
        setValidationErrors(err.response.data.errors);
        setValidationWarnings(err.response.data.warnings || []);
      } else {
        handleApiError(err, setAlertMessage, `Failed to ${isEditMode ? 'update' : 'create'} scenario`);
      }
    } finally {
      setSaving(false);
    }
  };

  /**
   * Handles advanced JSON mode toggle.
   */
  const handleToggleAdvancedJson = () => {
    if (!showAdvancedJson) {
      // Switching to JSON mode
      const scenarioJson = buildScenarioJson();
      setJsonText(JSON.stringify(scenarioJson, null, 2));
    } else {
      // Switching back to form mode
      try {
        const parsed = JSON.parse(jsonText);
        setDurationTicks(parsed.durationTicks || 100);
        setPassengerFlows(parsed.passengerFlows || []);
        if (parsed.seed !== undefined && parsed.seed !== null) {
          setSeed(String(parsed.seed));
          setUseSeed(true);
        } else {
          setSeed('');
          setUseSeed(false);
        }
      } catch {
        setAlertMessage('Invalid JSON format. Please fix the JSON before switching back to form mode.');
        return;
      }
    }
    setShowAdvancedJson(!showAdvancedJson);
  };

  if (loading) {
    return <div className="scenario-form"><p>Loading...</p></div>;
  }

  return (
    <div className="scenario-form">
      <div className="page-header">
        <h2>{isEditMode ? 'Edit Scenario' : 'Create New Scenario'}</h2>
      </div>

      <form onSubmit={handleSubmit}>
        {/* Scenario Name */}
        <div className="form-section">
          <div className="form-group">
            <label htmlFor="scenarioName">
              Scenario Name <span className="required">*</span>
            </label>
            <input
              type="text"
              id="scenarioName"
              value={scenarioName}
              onChange={(e) => {
                setScenarioName(e.target.value);
                if (formErrors.scenarioName) {
                  setFormErrors(prev => ({ ...prev, scenarioName: '' }));
                }
              }}
              placeholder="e.g., Morning Rush Hour"
              className={formErrors.scenarioName ? 'error' : ''}
            />
            {formErrors.scenarioName && (
              <span className="error-message">{formErrors.scenarioName}</span>
            )}
          </div>
        </div>

        {/* Template Selection */}
        {!isEditMode && (
          <div className="form-section">
            <h3>Quick Start Templates</h3>
            <div className="template-grid">
              {Object.entries(SCENARIO_TEMPLATES).map(([key, template]) => (
                <button
                  key={key}
                  type="button"
                  className="template-card"
                  onClick={() => applyTemplate(key)}
                >
                  <div className="template-name">{template.name}</div>
                  <div className="template-description">{template.description}</div>
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Advanced JSON Toggle */}
        <div className="form-section">
          <button
            type="button"
            className="btn-secondary"
            onClick={handleToggleAdvancedJson}
          >
            {showAdvancedJson ? 'Switch to Form Mode' : 'Switch to Advanced JSON Mode'}
          </button>
        </div>

        {showAdvancedJson ? (
          /* Advanced JSON Editor */
          <div className="form-section">
            <h3>Scenario JSON</h3>
            <div className="form-group">
              <textarea
                value={jsonText}
                onChange={(e) => setJsonText(e.target.value)}
                rows={20}
                className="json-editor"
                placeholder="Enter scenario JSON..."
              />
              <p className="help-text">
                Edit the scenario JSON directly. Valid fields: durationTicks, passengerFlows, seed (optional)
              </p>
            </div>
          </div>
        ) : (
          /* Form Mode */
          <>
            {/* Duration */}
            <div className="form-section">
              <h3>Simulation Settings</h3>
              <div className="form-group">
                <label htmlFor="durationTicks">
                  Duration (ticks) <span className="required">*</span>
                </label>
                <input
                  type="number"
                  id="durationTicks"
                  value={durationTicks}
                  onChange={(e) => setDurationTicks(e.target.value)}
                  min="1"
                  placeholder="e.g., 100"
                />
                <p className="help-text">
                  How long the simulation will run (each tick represents one time unit)
                </p>
              </div>

              {/* Seed */}
              <div className="form-group">
                <label htmlFor="useSeed">
                  <input
                    type="checkbox"
                    id="useSeed"
                    checked={useSeed}
                    onChange={(e) => setUseSeed(e.target.checked)}
                  />
                  {' '}Use Random Seed (for reproducibility)
                </label>
              </div>

              {useSeed && (
                <div className="form-group">
                  <label htmlFor="seed">Random Seed</label>
                  <input
                    type="number"
                    id="seed"
                    value={seed}
                    onChange={(e) => setSeed(e.target.value)}
                    min="0"
                    placeholder="e.g., 42"
                  />
                  <p className="help-text">
                    Optional seed for reproducible random behavior. Same seed = same simulation results.
                  </p>
                </div>
              )}
            </div>

            {/* Passenger Flows */}
            <div className="form-section">
              <h3>Passenger Flows</h3>
              <PassengerFlowBuilder
                flows={passengerFlows}
                onChange={setPassengerFlows}
                maxTick={parseInt(durationTicks, 10) || 100}
              />
            </div>
          </>
        )}

        {/* Validation Results */}
        {(validationErrors.length > 0 || validationWarnings.length > 0) && (
          <div className="form-section">
            <h3>Validation Results</h3>
            {validationErrors.length > 0 && (
              <div className="validation-errors">
                <h4>Errors:</h4>
                <ul>
                  {validationErrors.map((error, index) => (
                    <li key={index}>
                      <strong>{error.field}:</strong> {error.message}
                    </li>
                  ))}
                </ul>
              </div>
            )}
            {validationWarnings.length > 0 && (
              <div className="validation-warnings">
                <h4>Warnings:</h4>
                <ul>
                  {validationWarnings.map((warning, index) => (
                    <li key={index}>
                      <strong>{warning.field}:</strong> {warning.message}
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        )}

        {/* Actions */}
        <div className="form-actions">
          <button
            type="button"
            className="btn-secondary"
            onClick={() => navigate('/scenarios')}
          >
            Cancel
          </button>
          <button
            type="button"
            className="btn-secondary"
            onClick={handleValidate}
            disabled={validating}
          >
            {validating ? 'Validating...' : 'Validate'}
          </button>
          <button
            type="submit"
            className="btn-primary"
            disabled={saving || validating}
          >
            {saving ? 'Saving...' : (isEditMode ? 'Update Scenario' : 'Create Scenario')}
          </button>
        </div>
      </form>

      <AlertModal
        isOpen={!!alertMessage}
        onClose={() => setAlertMessage(null)}
        title={validationErrors.length === 0 && alertMessage === 'Scenario is valid!' ? 'Success' : 'Error'}
        message={alertMessage}
        type={validationErrors.length === 0 && alertMessage === 'Scenario is valid!' ? 'success' : 'error'}
      />
    </div>
  );
}

export default ScenarioForm;
