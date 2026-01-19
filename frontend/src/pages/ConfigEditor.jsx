// @ts-check
import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { liftSystemsApi } from '../api/liftSystemsApi';
import ConfirmModal from '../components/ConfirmModal';
import { handleApiError } from '../utils/errorHandlers';
import { getStatusBadgeClass } from '../utils/statusUtils';
import './ConfigEditor.css';

/**
 * Lift system configuration editor page component.
 * Provides a JSON editor for modifying lift system configurations with validation and publishing capabilities.
 *
 * Features:
 * - JSON configuration editing with syntax highlighting
 * - Real-time validation with error/warning display
 * - Draft saving with unsaved changes tracking
 * - Version publishing workflow
 * - Read-only mode for non-DRAFT versions
 *
 * Workflow:
 * 1. Edit configuration JSON
 * 2. Validate configuration
 * 3. Save draft (auto-validates if not validated)
 * 4. Publish version (requires valid, saved configuration)
 *
 * @returns {JSX.Element} The configuration editor page component
 */
function ConfigEditor() {
  const { systemId, versionNumber } = useParams();
  const navigate = useNavigate();

  /** @type {import('../types/models').LiftSystem | null} */
  const [system, setSystem] = useState(null);
  /** @type {import('../types/models').Version | null} */
  const [version, setVersion] = useState(null);
  const [config, setConfig] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [saving, setSaving] = useState(false);
  const [validating, setValidating] = useState(false);
  const [publishing, setPublishing] = useState(false);

  /** @type {import('../types/models').ValidationResult | null} */
  const [validationResult, setValidationResult] = useState(null);
  const [lastSaved, setLastSaved] = useState(null);
  const [hasUnsavedChanges, setHasUnsavedChanges] = useState(false);

  const [showPublishConfirm, setShowPublishConfirm] = useState(false);

  /**
   * Loads system and version data from the API.
   * Fetches system metadata and version configuration in parallel.
   */
  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const [systemRes, versionRes] = await Promise.all([
        liftSystemsApi.getSystem(systemId),
        liftSystemsApi.getVersion(systemId, versionNumber)
      ]);
      setSystem(systemRes.data);
      setVersion(versionRes.data);
      setConfig(versionRes.data.config);
      setError(null);
    } catch (err) {
      handleApiError(err, setError, 'Failed to load version data');
    } finally {
      setLoading(false);
    }
  }, [systemId, versionNumber]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  useEffect(() => {
    if (version && config !== version.config) {
      setHasUnsavedChanges(true);
    } else {
      setHasUnsavedChanges(false);
    }
  }, [config, version]);

  /**
   * Handles configuration text changes in the editor.
   * Clears validation results when config is modified.
   *
   * @param {React.ChangeEvent<HTMLTextAreaElement>} e - Textarea change event
   */
  const handleConfigChange = (e) => {
    setConfig(e.target.value);
    setValidationResult(null); // Clear validation when config changes
  };

  /**
   * Saves the current configuration as a draft.
   * Validates JSON syntax, runs configuration validation if needed, and persists to API.
   * Only allows saving if configuration is valid.
   */
  const handleSaveDraft = async () => {
    try {
      setSaving(true);
      setError(null);

      // Parse JSON to ensure it's valid
      JSON.parse(config);

      // Auto-validate if not already validated
      if (!validationResult) {
        setValidating(true);
        try {
          const response = await liftSystemsApi.validateConfig({ config });
          setValidationResult(response.data);

          // If validation fails, prevent save and show errors
          if (!response.data.valid) {
            setError('Configuration has validation errors. Please fix them before saving.');
            setSaving(false);
            setValidating(false);
            return;
          }
        } catch (validationErr) {
          handleApiError(validationErr, setError, 'Failed to validate configuration');
          setSaving(false);
          setValidating(false);
          return;
        } finally {
          setValidating(false);
        }
      } else if (!validationResult.valid) {
        // If already validated and invalid, prevent save
        setError('Configuration has validation errors. Please fix them before saving.');
        setSaving(false);
        return;
      }

      await liftSystemsApi.updateVersion(systemId, versionNumber, { config });
      setLastSaved(new Date());
      setHasUnsavedChanges(false);

      // Reload to get updated version data
      await loadData();

    } catch (err) {
      if (err.name === 'SyntaxError') {
        setError('Invalid JSON format. Please check your configuration.');
      } else {
        handleApiError(err, setError, 'Failed to save draft');
      }
    } finally {
      setSaving(false);
    }
  };

  /**
   * Validates the current configuration against business rules.
   * Parses JSON and sends to validation API endpoint.
   * Displays errors and warnings in the validation panel.
   */
  const handleValidate = async () => {
    try {
      setValidating(true);
      setValidationResult(null);
      setError(null);

      // Parse JSON to ensure it's valid before sending to API
      JSON.parse(config);
      const response = await liftSystemsApi.validateConfig({ config });
      setValidationResult(response.data);

    } catch (err) {
      if (err.name === 'SyntaxError') {
        setError('Invalid JSON format. Please fix syntax errors first.');
      } else {
        handleApiError(err, setError, 'Validation failed');
      }
    } finally {
      setValidating(false);
    }
  };

  /**
   * Initiates the publish workflow by showing confirmation modal.
   */
  const handlePublish = () => {
    setShowPublishConfirm(true);
  };

  /**
   * Confirms and executes version publishing.
   * Publishes the version and navigates back to system detail page.
   */
  const confirmPublish = async () => {
    try {
      setPublishing(true);
      setError(null);

      await liftSystemsApi.publishVersion(systemId, versionNumber);
      navigate(`/systems/${systemId}`);

    } catch (err) {
      handleApiError(err, setError, 'Failed to publish version');
    } finally {
      setPublishing(false);
    }
  };

  /**
   * Determines whether the current version can be published.
   * Requires DRAFT status, valid configuration, and no unsaved changes.
   *
   * @returns {boolean} True if version can be published, false otherwise
   */
  const canPublish = () => {
    return version?.status === 'DRAFT' &&
           validationResult &&
           validationResult.valid &&
           !hasUnsavedChanges;
  };

  if (loading) {
    return <div className="config-editor"><p>Loading...</p></div>;
  }

  if (error && !version) {
    return (
      <div className="config-editor">
        <p className="error">{error}</p>
        <Link to={`/systems/${systemId}`} className="btn-secondary">Back to System</Link>
      </div>
    );
  }

  return (
    <div className="config-editor">
      <div className="editor-header">
        <div>
          <Link to={`/systems/${systemId}`} className="breadcrumb">
            ← Back to {system?.displayName || 'System'}
          </Link>
          <h2>Edit Configuration</h2>
          <div className="version-info-header">
            <span className="version-number">Version {version?.versionNumber}</span>
            <span className={getStatusBadgeClass(version?.status)}>{version?.status}</span>
            {hasUnsavedChanges && <span className="unsaved-indicator">● Unsaved changes</span>}
          </div>
        </div>
      </div>

      {error && <div className="error-banner">{error}</div>}

      <div className="editor-layout">
        <div className="editor-section">
          <div className="section-header">
            <h3>Configuration JSON</h3>
            {lastSaved && (
              <span className="last-saved">
                Last saved: {lastSaved.toLocaleTimeString()}
              </span>
            )}
          </div>

          <textarea
            className="config-textarea"
            value={config}
            onChange={handleConfigChange}
            spellCheck="false"
            placeholder='{"minFloor": 0, "maxFloor": 9, "lifts": 2, ...}'
          />

          <div className="editor-actions">
            <button
              className="btn-primary"
              onClick={handleSaveDraft}
              disabled={saving || !hasUnsavedChanges || version?.status !== 'DRAFT'}
            >
              {saving ? 'Saving...' : 'Save Draft'}
            </button>

            <button
              className="btn-secondary"
              onClick={handleValidate}
              disabled={validating}
            >
              {validating ? 'Validating...' : 'Validate'}
            </button>

            <button
              className="btn-success"
              onClick={handlePublish}
              disabled={publishing || !canPublish()}
              title={
                !canPublish()
                  ? hasUnsavedChanges
                    ? 'Save changes before publishing'
                    : 'Validate configuration before publishing'
                  : 'Publish this version'
              }
            >
              {publishing ? 'Publishing...' : 'Publish'}
            </button>
          </div>

          {version?.status !== 'DRAFT' && (
            <div className="info-banner">
              This version is {version?.status}. Only DRAFT versions can be edited.
            </div>
          )}
        </div>

        <div className="validation-section">
          <h3>Validation Results</h3>

          {validationResult ? (
            <div className={validationResult.valid ? 'validation-success' : 'validation-error'}>
              <h4>
                {validationResult.valid ? '✓ Configuration is valid' : '✗ Configuration has errors'}
              </h4>

              {validationResult.errors && validationResult.errors.length > 0 && (
                <div className="validation-messages">
                  <h5>Errors</h5>
                  <ul>
                    {validationResult.errors.map((err, idx) => (
                      <li key={idx} className="validation-error-item">
                        <strong>{err.field}:</strong> {err.message}
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {validationResult.warnings && validationResult.warnings.length > 0 && (
                <div className="validation-messages">
                  <h5>Warnings</h5>
                  <ul>
                    {validationResult.warnings.map((warn, idx) => (
                      <li key={idx} className="validation-warning-item">
                        <strong>{warn.field}:</strong> {warn.message}
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {validationResult.valid && (!validationResult.warnings || validationResult.warnings.length === 0) && (
                <p className="validation-detail">All validation checks passed successfully.</p>
              )}
            </div>
          ) : (
            <div className="validation-placeholder">
              <p>Click "Validate" to check your configuration for errors.</p>
              <p className="help-text">
                Validation will check for required fields, valid ranges, and cross-field constraints.
              </p>
            </div>
          )}
        </div>
      </div>

      <ConfirmModal
        isOpen={showPublishConfirm}
        onClose={() => setShowPublishConfirm(false)}
        onConfirm={confirmPublish}
        title="Publish Version"
        message={`Are you sure you want to publish version ${versionNumber}? This will archive any currently published version.`}
        confirmText="Publish"
        confirmStyle="success"
      />
    </div>
  );
}

export default ConfigEditor;
