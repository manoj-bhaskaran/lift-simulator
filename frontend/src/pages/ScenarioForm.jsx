// @ts-check
import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { scenariosApi } from '../api/scenariosApi';
import { liftSystemsApi } from '../api/liftSystemsApi';
import AlertModal from '../components/AlertModal';
import ScenarioBasicsSection from '../components/scenarios/ScenarioBasicsSection';
import ScenarioJsonEditor from '../components/scenarios/ScenarioJsonEditor';
import ScenarioSettingsSection from '../components/scenarios/ScenarioSettingsSection';
import ScenarioTemplateSection from '../components/scenarios/ScenarioTemplateSection';
import ScenarioValidationResults from '../components/scenarios/ScenarioValidationResults';
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

  // Lift system selection
  const [liftSystems, setLiftSystems] = useState([]);
  const [selectedSystemId, setSelectedSystemId] = useState('');
  const [versions, setVersions] = useState([]);
  const [selectedVersionId, setSelectedVersionId] = useState('');
  const [floorRange, setFloorRange] = useState(null);

  const [validationErrors, setValidationErrors] = useState([]);
  const [validationWarnings, setValidationWarnings] = useState([]);
  const [formErrors, setFormErrors] = useState({});
  const [alertMessage, setAlertMessage] = useState(null);
  const [showAdvancedJson, setShowAdvancedJson] = useState(false);
  const [jsonText, setJsonText] = useState('');

  /**
   * Parses version config to extract floor range.
   *
   * @param {string} configJson - The config JSON string
   * @returns {Object|null} Floor range {minFloor, maxFloor} or null if parsing fails
   */
  const parseVersionConfig = (configJson) => {
    if (!configJson) return null;
    try {
      const config = JSON.parse(configJson);
      return {
        minFloor: config.minFloor,
        maxFloor: config.maxFloor
      };
    } catch {
      return null;
    }
  };

  /**
   * Loads all lift systems.
   */
  const loadLiftSystems = async () => {
    try {
      const response = await liftSystemsApi.getAllSystems();
      setLiftSystems(response.data || []);
    } catch (err) {
      handleApiError(err, setAlertMessage, 'Failed to load lift systems');
    }
  };

  /**
   * Loads versions for a specific lift system.
   *
   * @param {number|string} systemId - The lift system ID
   */
  const loadVersions = async (systemId) => {
    try {
      const response = await liftSystemsApi.getVersions(systemId);
      setVersions(response.data || []);
    } catch (err) {
      handleApiError(err, setAlertMessage, 'Failed to load versions');
      setVersions([]);
    }
  };

  useEffect(() => {
    loadLiftSystems();
  }, []);

  const loadScenario = useCallback(async () => {
    if (!isEditMode) {
      return;
    }

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

      // Load version information if available
      if (scenario.liftSystemVersionId) {
        setSelectedVersionId(String(scenario.liftSystemVersionId));
        // Find the system ID from version info if available
        if (scenario.versionInfo?.liftSystemId) {
          setSelectedSystemId(String(scenario.versionInfo.liftSystemId));
        }
      }
    } catch (err) {
      handleApiError(err, setAlertMessage, 'Failed to load scenario');
    } finally {
      setLoading(false);
    }
  }, [id, isEditMode]);

  useEffect(() => {
    loadScenario();
  }, [loadScenario]);

  useEffect(() => {
    if (selectedSystemId) {
      loadVersions(selectedSystemId);
    } else {
      setVersions([]);
      setSelectedVersionId('');
      setFloorRange(null);
    }
  }, [selectedSystemId]);

  useEffect(() => {
    if (selectedVersionId && versions.length > 0) {
      const selectedVersion = versions.find(v => v.id === parseInt(selectedVersionId, 10));
      if (selectedVersion) {
        const floorInfo = parseVersionConfig(selectedVersion.config);
        setFloorRange(floorInfo);
      } else {
        setFloorRange(null);
      }
    } else {
      setFloorRange(null);
    }
  }, [selectedVersionId, versions]);

  /**
   * Adapts passenger flows to fit within the valid floor range.
   * Floors are scaled proportionally to fit the available range.
   *
   * @param {Array} flows - Original passenger flows
   * @param {Object} range - Floor range {minFloor, maxFloor}
   * @returns {Array} Adapted passenger flows
   */
  const adaptFlowsToFloorRange = (flows, range) => {
    if (!flows || flows.length === 0 || !range) return flows;

    const { minFloor, maxFloor } = range;
    const availableFloors = maxFloor - minFloor;

    // Find the range used by the template
    let templateMin = Infinity;
    let templateMax = -Infinity;
    flows.forEach(flow => {
      templateMin = Math.min(templateMin, flow.originFloor, flow.destinationFloor);
      templateMax = Math.max(templateMax, flow.originFloor, flow.destinationFloor);
    });

    const templateRange = templateMax - templateMin;

    return flows.map(flow => {
      // Scale floors proportionally to fit the available range
      let originFloor, destinationFloor;

      if (templateRange === 0) {
        // All template floors are the same, use minFloor
        originFloor = minFloor;
        destinationFloor = maxFloor;
      } else {
        // Scale proportionally
        const originRatio = (flow.originFloor - templateMin) / templateRange;
        const destRatio = (flow.destinationFloor - templateMin) / templateRange;

        originFloor = Math.round(minFloor + originRatio * availableFloors);
        destinationFloor = Math.round(minFloor + destRatio * availableFloors);

        // Ensure floors are different (no same-floor trips)
        if (originFloor === destinationFloor) {
          if (destinationFloor < maxFloor) {
            destinationFloor += 1;
          } else {
            originFloor -= 1;
          }
        }
      }

      return {
        ...flow,
        originFloor,
        destinationFloor
      };
    });
  };

  /**
   * Applies a scenario template.
   * When a floor range is selected, adapts the template to fit the valid range.
   *
   * @param {string} templateKey - The template key to apply
   */
  const applyTemplate = (templateKey) => {
    const template = SCENARIO_TEMPLATES[templateKey];
    if (!template) return;

    const json = template.scenarioJson;
    setDurationTicks(json.durationTicks);

    // Adapt flows to floor range if a version is selected
    const flows = json.passengerFlows || [];
    if (floorRange && flows.length > 0) {
      setPassengerFlows(adaptFlowsToFloorRange(flows, floorRange));
    } else {
      setPassengerFlows(flows);
    }

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
   * Builds the scenario JSON from form state or JSON text.
   * When in Advanced JSON Mode, parses and returns the JSON text.
   * When in Form Mode, builds JSON from form state.
   *
   * @returns {Object} The scenario JSON
   * @throws {Error} If JSON text is invalid when in Advanced JSON Mode
   */

  const validateScenarioJsonClientSide = (scenarioJson) => {
    const duration = Number(scenarioJson?.durationTicks);

    if (!Number.isInteger(duration) || duration < 1) {
      return { durationTicks: 'Duration ticks must be at least 1' };
    }

    return {};
  };

  const buildScenarioJson = () => {
    // If in Advanced JSON Mode, parse and return the JSON text
    if (showAdvancedJson) {
      return JSON.parse(jsonText);
    }

    // Otherwise, build from form state
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
   * Syncs parsed scenario JSON back to form state.
   * This ensures consistency between JSON mode and form mode.
   *
   * @param {Object} scenarioJson - The parsed scenario JSON
   */
  const syncJsonToFormState = (scenarioJson) => {
    setDurationTicks(scenarioJson.durationTicks || 100);
    setPassengerFlows(scenarioJson.passengerFlows || []);
    if (scenarioJson.seed !== undefined && scenarioJson.seed !== null) {
      setSeed(String(scenarioJson.seed));
      setUseSeed(true);
    } else {
      setSeed('');
      setUseSeed(false);
    }
  };

  /**
   * Validates the scenario using server-side validation.
   */
  const handleValidate = async () => {
    const errors = {};
    if (!scenarioName.trim()) {
      errors.scenarioName = 'Scenario name is required';
    }
    if (!selectedSystemId) {
      errors.liftSystem = 'Lift system is required';
    } else if (!selectedVersionId) {
      errors.liftSystemVersion = 'Lift system version is required';
    }

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    try {
      setValidating(true);
      setValidationErrors([]);
      setValidationWarnings([]);

      let scenarioJson;
      try {
        scenarioJson = buildScenarioJson();
        
        // If in Advanced JSON Mode, sync the parsed JSON back to form state
        // so that switching back to form mode shows the validated values
        if (showAdvancedJson) {
          syncJsonToFormState(scenarioJson);
        }
      } catch (error) {
        console.error('JSON parsing error during validation:', error);
        setAlertMessage('Invalid JSON format. Please fix the JSON before validating.');
        setValidating(false);
        return;
      }

      const clientErrors = validateScenarioJsonClientSide(scenarioJson);
      if (Object.keys(clientErrors).length > 0) {
        setFormErrors(clientErrors);
        setValidating(false);
        return;
      }

      const response = await scenariosApi.validateScenario({
        scenarioJson: scenarioJson,
        liftSystemVersionId: parseInt(selectedVersionId, 10)
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
    if (!selectedSystemId) {
      errors.liftSystem = 'Lift system is required';
    } else if (!selectedVersionId) {
      errors.liftSystemVersion = 'Lift system version is required';
    }

    if (Object.keys(errors).length > 0) {
      setFormErrors(errors);
      return;
    }

    try {
      setSaving(true);
      setValidationErrors([]);
      setValidationWarnings([]);

      let scenarioJson;
      try {
        scenarioJson = buildScenarioJson();
        
        // If in Advanced JSON Mode, sync the parsed JSON back to form state
        // This ensures data consistency if there's any error and user needs to edit
        if (showAdvancedJson) {
          syncJsonToFormState(scenarioJson);
        }
      } catch (error) {
        console.error('JSON parsing error during save:', error);
        setAlertMessage('Invalid JSON format. Please fix the JSON before saving.');
        setSaving(false);
        return;
      }

      const clientErrors = validateScenarioJsonClientSide(scenarioJson);
      if (Object.keys(clientErrors).length > 0) {
        setFormErrors(clientErrors);
        setSaving(false);
        return;
      }

      const payload = {
        name: scenarioName,
        scenarioJson: scenarioJson,
        liftSystemVersionId: parseInt(selectedVersionId, 10)
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
        <ScenarioBasicsSection
          scenarioName={scenarioName}
          setScenarioName={setScenarioName}
          formErrors={formErrors}
          setFormErrors={setFormErrors}
          liftSystems={liftSystems}
          selectedSystemId={selectedSystemId}
          setSelectedSystemId={setSelectedSystemId}
          versions={versions}
          selectedVersionId={selectedVersionId}
          setSelectedVersionId={setSelectedVersionId}
          floorRange={floorRange}
          parseVersionConfig={parseVersionConfig}
        />

        {!isEditMode && (
          <ScenarioTemplateSection
            floorRange={floorRange}
            selectedVersionId={selectedVersionId}
            templates={SCENARIO_TEMPLATES}
            onApplyTemplate={applyTemplate}
          />
        )}

        <div className="form-section">
          <button type="button" className="btn-secondary" onClick={handleToggleAdvancedJson}>
            {showAdvancedJson ? 'Switch to Form Mode' : 'Switch to Advanced JSON Mode'}
          </button>
        </div>

        {showAdvancedJson ? (
          <ScenarioJsonEditor
            jsonText={jsonText}
            setJsonText={setJsonText}
            formErrors={formErrors}
            setFormErrors={setFormErrors}
          />
        ) : (
          <ScenarioSettingsSection
            durationTicks={durationTicks}
            setDurationTicks={setDurationTicks}
            useSeed={useSeed}
            setUseSeed={setUseSeed}
            seed={seed}
            setSeed={setSeed}
            passengerFlows={passengerFlows}
            setPassengerFlows={setPassengerFlows}
            floorRange={floorRange}
            formErrors={formErrors}
            setFormErrors={setFormErrors}
          />
        )}

        <ScenarioValidationResults validationErrors={validationErrors} validationWarnings={validationWarnings} />

        <div className="form-actions">
          <button type="button" className="btn-secondary" onClick={() => navigate('/scenarios')}>Cancel</button>
          <button type="button" className="btn-secondary" onClick={handleValidate} disabled={validating}>
            {validating ? 'Validating...' : 'Validate'}
          </button>
          <button type="submit" className="btn-primary" disabled={saving || validating}>
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
