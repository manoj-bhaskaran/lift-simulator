// @ts-check
import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { liftSystemsApi } from '../api/liftSystemsApi';
import ScenarioEvents from '../components/ScenarioEvents';
import AlertModal from '../components/AlertModal';
import { handleApiError } from '../utils/errorHandlers';
import './ScenarioForm.css';

/**
 * Scenario creation and editing form component.
 * Handles creating new scenarios and editing existing ones with validation.
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
  const [alertMessage, setAlertMessage] = useState(null);
  const [validationErrors, setValidationErrors] = useState([]);
  const [validationWarnings, setValidationWarnings] = useState([]);

  // Form state
  const [formData, setFormData] = useState({
    name: '',
    description: '',
    totalTicks: 1000,
    minFloor: 0,
    maxFloor: 10,
    initialFloor: 0,
    homeFloor: 0,
    travelTicksPerFloor: 10,
    doorTransitionTicks: 5,
    doorDwellTicks: 10,
    controllerStrategy: 'DIRECTIONAL_SCAN',
    idleParkingMode: 'STAY_PUT',
    seed: '',
    events: [],
  });

  useEffect(() => {
    if (isEditMode) {
      loadScenario();
    }
  }, [id]);

  /**
   * Loads the scenario for editing.
   */
  const loadScenario = async () => {
    try {
      setLoading(true);
      const response = await liftSystemsApi.getScenario(id);
      const scenario = response.data;
      setFormData({
        name: scenario.name,
        description: scenario.description || '',
        totalTicks: scenario.totalTicks,
        minFloor: scenario.minFloor,
        maxFloor: scenario.maxFloor,
        initialFloor: scenario.initialFloor ?? scenario.minFloor,
        homeFloor: scenario.homeFloor ?? scenario.minFloor,
        travelTicksPerFloor: scenario.travelTicksPerFloor,
        doorTransitionTicks: scenario.doorTransitionTicks,
        doorDwellTicks: scenario.doorDwellTicks,
        controllerStrategy: scenario.controllerStrategy,
        idleParkingMode: scenario.idleParkingMode,
        seed: scenario.seed ? String(scenario.seed) : '',
        events: scenario.events || [],
      });
    } catch (err) {
      handleApiError(err, setAlertMessage, 'Failed to load scenario');
    } finally {
      setLoading(false);
    }
  };

  /**
   * Handles form field changes.
   */
  const handleChange = (field, value) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    setValidationErrors([]);
    setValidationWarnings([]);
  };

  /**
   * Handles events update from the ScenarioEvents component.
   */
  const handleEventsChange = (events) => {
    setFormData((prev) => ({ ...prev, events }));
    setValidationErrors([]);
    setValidationWarnings([]);
  };

  /**
   * Validates the scenario before saving.
   */
  const handleValidate = async () => {
    try {
      setValidating(true);
      setValidationErrors([]);
      setValidationWarnings([]);

      const payload = buildPayload();
      const response = await liftSystemsApi.validateScenario(payload);
      const validation = response.data;

      if (validation.valid) {
        setAlertMessage('Scenario is valid!');
      } else {
        setValidationErrors(validation.errors || []);
        setValidationWarnings(validation.warnings || []);
        setAlertMessage('Scenario has validation errors. Please fix them before saving.');
      }
    } catch (err) {
      handleApiError(err, setAlertMessage, 'Validation failed');
    } finally {
      setValidating(false);
    }
  };

  /**
   * Saves the scenario (create or update).
   */
  const handleSave = async () => {
    try {
      setSaving(true);
      const payload = buildPayload();

      if (isEditMode) {
        await liftSystemsApi.updateScenario(id, payload);
      } else {
        await liftSystemsApi.createScenario(payload);
      }

      navigate('/scenarios');
    } catch (err) {
      if (err.response?.data?.message) {
        setAlertMessage(err.response.data.message);
      } else {
        handleApiError(err, setAlertMessage, `Failed to ${isEditMode ? 'update' : 'create'} scenario`);
      }
    } finally {
      setSaving(false);
    }
  };

  /**
   * Builds the API payload from form data.
   */
  const buildPayload = () => {
    return {
      name: formData.name.trim(),
      description: formData.description.trim() || null,
      totalTicks: parseInt(formData.totalTicks, 10),
      minFloor: parseInt(formData.minFloor, 10),
      maxFloor: parseInt(formData.maxFloor, 10),
      initialFloor: formData.initialFloor !== '' ? parseInt(formData.initialFloor, 10) : null,
      homeFloor: formData.homeFloor !== '' ? parseInt(formData.homeFloor, 10) : null,
      travelTicksPerFloor: parseInt(formData.travelTicksPerFloor, 10),
      doorTransitionTicks: parseInt(formData.doorTransitionTicks, 10),
      doorDwellTicks: parseInt(formData.doorDwellTicks, 10),
      controllerStrategy: formData.controllerStrategy,
      idleParkingMode: formData.idleParkingMode,
      seed: formData.seed ? parseInt(formData.seed, 10) : null,
      events: formData.events,
    };
  };

  if (loading) {
    return <div className="scenario-form"><p>Loading...</p></div>;
  }

  return (
    <div className="scenario-form">
      <div className="page-header">
        <h2>{isEditMode ? 'Edit Scenario' : 'Create New Scenario'}</h2>
        <div className="page-actions">
          <button
            className="btn-secondary"
            onClick={() => navigate('/scenarios')}
            disabled={saving}
          >
            Cancel
          </button>
          <button
            className="btn-secondary"
            onClick={handleValidate}
            disabled={saving || validating}
          >
            {validating ? 'Validating...' : 'Validate'}
          </button>
          <button
            className="btn-primary"
            onClick={handleSave}
            disabled={saving || validating}
          >
            {saving ? 'Saving...' : 'Save'}
          </button>
        </div>
      </div>

      {(validationErrors.length > 0 || validationWarnings.length > 0) && (
        <div className="validation-summary">
          {validationErrors.length > 0 && (
            <div className="validation-errors">
              <h4>Validation Errors</h4>
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
              <h4>Warnings</h4>
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

      <div className="form-content">
        <section className="form-section">
          <h3>Basic Information</h3>
          <div className="form-row">
            <div className="form-field">
              <label htmlFor="name">Scenario Name *</label>
              <input
                type="text"
                id="name"
                value={formData.name}
                onChange={(e) => handleChange('name', e.target.value)}
                placeholder="e.g., Morning Rush Hour"
                required
              />
            </div>
          </div>
          <div className="form-row">
            <div className="form-field">
              <label htmlFor="description">Description</label>
              <textarea
                id="description"
                value={formData.description}
                onChange={(e) => handleChange('description', e.target.value)}
                placeholder="Describe this scenario..."
                rows="3"
              />
            </div>
          </div>
          <div className="form-row">
            <div className="form-field">
              <label htmlFor="totalTicks">Total Ticks (Duration) *</label>
              <input
                type="number"
                id="totalTicks"
                value={formData.totalTicks}
                onChange={(e) => handleChange('totalTicks', e.target.value)}
                min="1"
                required
              />
              <small>Total simulation duration in ticks</small>
            </div>
            <div className="form-field">
              <label htmlFor="seed">Random Seed (Optional)</label>
              <input
                type="number"
                id="seed"
                value={formData.seed}
                onChange={(e) => handleChange('seed', e.target.value)}
                placeholder="Leave empty for random"
              />
              <small>For reproducible simulations</small>
            </div>
          </div>
        </section>

        <section className="form-section">
          <h3>Floor Configuration</h3>
          <div className="form-row">
            <div className="form-field">
              <label htmlFor="minFloor">Min Floor *</label>
              <input
                type="number"
                id="minFloor"
                value={formData.minFloor}
                onChange={(e) => handleChange('minFloor', e.target.value)}
                required
              />
            </div>
            <div className="form-field">
              <label htmlFor="maxFloor">Max Floor *</label>
              <input
                type="number"
                id="maxFloor"
                value={formData.maxFloor}
                onChange={(e) => handleChange('maxFloor', e.target.value)}
                required
              />
            </div>
          </div>
          <div className="form-row">
            <div className="form-field">
              <label htmlFor="initialFloor">Initial Floor</label>
              <input
                type="number"
                id="initialFloor"
                value={formData.initialFloor}
                onChange={(e) => handleChange('initialFloor', e.target.value)}
              />
              <small>Starting floor for the lift</small>
            </div>
            <div className="form-field">
              <label htmlFor="homeFloor">Home Floor</label>
              <input
                type="number"
                id="homeFloor"
                value={formData.homeFloor}
                onChange={(e) => handleChange('homeFloor', e.target.value)}
              />
              <small>Floor to return to when idle</small>
            </div>
          </div>
        </section>

        <section className="form-section">
          <h3>Timing Configuration</h3>
          <div className="form-row">
            <div className="form-field">
              <label htmlFor="travelTicksPerFloor">Travel Ticks Per Floor *</label>
              <input
                type="number"
                id="travelTicksPerFloor"
                value={formData.travelTicksPerFloor}
                onChange={(e) => handleChange('travelTicksPerFloor', e.target.value)}
                min="1"
                required
              />
            </div>
            <div className="form-field">
              <label htmlFor="doorTransitionTicks">Door Transition Ticks *</label>
              <input
                type="number"
                id="doorTransitionTicks"
                value={formData.doorTransitionTicks}
                onChange={(e) => handleChange('doorTransitionTicks', e.target.value)}
                min="1"
                required
              />
            </div>
            <div className="form-field">
              <label htmlFor="doorDwellTicks">Door Dwell Ticks *</label>
              <input
                type="number"
                id="doorDwellTicks"
                value={formData.doorDwellTicks}
                onChange={(e) => handleChange('doorDwellTicks', e.target.value)}
                min="1"
                required
              />
            </div>
          </div>
        </section>

        <section className="form-section">
          <h3>Controller Configuration</h3>
          <div className="form-row">
            <div className="form-field">
              <label htmlFor="controllerStrategy">Controller Strategy *</label>
              <select
                id="controllerStrategy"
                value={formData.controllerStrategy}
                onChange={(e) => handleChange('controllerStrategy', e.target.value)}
                required
              >
                <option value="NAIVE">Naive (FCFS)</option>
                <option value="SIMPLE">Simple</option>
                <option value="DIRECTIONAL_SCAN">Directional Scan</option>
              </select>
              <small>Algorithm for handling passenger requests</small>
            </div>
            <div className="form-field">
              <label htmlFor="idleParkingMode">Idle Parking Mode *</label>
              <select
                id="idleParkingMode"
                value={formData.idleParkingMode}
                onChange={(e) => handleChange('idleParkingMode', e.target.value)}
                required
              >
                <option value="STAY_PUT">Stay Put</option>
                <option value="RETURN_HOME">Return Home</option>
                <option value="RETURN_TO_LOBBY">Return to Lobby</option>
              </select>
              <small>Behavior when no requests are pending</small>
            </div>
          </div>
        </section>

        <section className="form-section">
          <h3>Passenger Flow Events</h3>
          <ScenarioEvents
            events={formData.events}
            totalTicks={parseInt(formData.totalTicks, 10)}
            minFloor={parseInt(formData.minFloor, 10)}
            maxFloor={parseInt(formData.maxFloor, 10)}
            onChange={handleEventsChange}
          />
        </section>
      </div>

      <AlertModal
        isOpen={!!alertMessage}
        onClose={() => setAlertMessage(null)}
        title={validationErrors.length > 0 ? 'Validation Error' : 'Information'}
        message={alertMessage}
        type={validationErrors.length > 0 ? 'error' : 'info'}
      />
    </div>
  );
}

export default ScenarioForm;
