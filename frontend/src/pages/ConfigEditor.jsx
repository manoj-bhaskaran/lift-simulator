// @ts-check
import { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { liftSystemsApi } from '../api/liftSystemsApi';
import ConfirmModal from '../components/ConfirmModal';
import { handleApiError } from '../utils/errorHandlers';
import { getStatusBadgeClass } from '../utils/statusUtils';
import './ConfigEditor.css';

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

  useEffect(() => {
    loadData();
  }, [systemId, versionNumber]);

  useEffect(() => {
    if (version && config !== version.config) {
      setHasUnsavedChanges(true);
    } else {
      setHasUnsavedChanges(false);
    }
  }, [config, version]);

  const loadData = async () => {
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
  };

  const handleConfigChange = (e) => {
    setConfig(e.target.value);
    setValidationResult(null); // Clear validation when config changes
  };

  const handleSaveDraft = async () => {
    try {
      setSaving(true);
      setError(null);

      // Parse JSON to ensure it's valid
      const configObject = JSON.parse(config);

      // Auto-validate if not already validated
      if (!validationResult) {
        setValidating(true);
        try {
          const response = await liftSystemsApi.validateConfig(configObject);
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

  const handleValidate = async () => {
    try {
      setValidating(true);
      setValidationResult(null);
      setError(null);

      const configObject = JSON.parse(config);
      const response = await liftSystemsApi.validateConfig(configObject);
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

  const handlePublish = () => {
    setShowPublishConfirm(true);
  };

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
            placeholder='{"floors": 10, "lifts": 2, ...}'
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
