// @ts-check
import VersionConfigForm from '../VersionConfigForm';
import { CONFIG_EXAMPLE_JSON, CONFIG_REQUIRED_FIELDS, CONFIG_SCHEMA_DOCS_URL, CONFIG_SCHEMA_HELP_TEXT } from '../../utils/configSchemaHelp';

function CreateVersionForm({ versions, editorMode, switchToGuidedMode, switchToJsonMode, versionFormData, versionFormErrors, handleVersionFormChange, newVersionConfig, handleNewVersionConfigChange, creating, createValidationResult, handleCreateVersion, validatingCreate, handleValidateCreateVersion, handleCancelCreateVersion, createValidationError }) {
  return (
    <div className="create-version-form">
      <div className="version-number-display"><h4>Version {versions.length > 0 ? Math.max(...versions.map(v => v.versionNumber)) + 1 : 1}</h4></div>
      <div className="editor-mode-toggle" role="group" aria-label="Configuration editor mode">
        <button type="button" className={editorMode === 'guided' ? 'mode-btn active' : 'mode-btn'} onClick={switchToGuidedMode} aria-pressed={editorMode === 'guided'}>Guided Form</button>
        <button type="button" className={editorMode === 'json' ? 'mode-btn active' : 'mode-btn'} onClick={switchToJsonMode} aria-pressed={editorMode === 'json'}>Advanced (JSON)</button>
      </div>
      {editorMode === 'guided' ? (
        <>
          <div className="config-label-row"><label>Version Configuration</label><a href={CONFIG_SCHEMA_DOCS_URL} target="_blank" rel="noreferrer">View schema docs</a></div>
          <p className="config-help-text">Fill in the parameters below. Switch to Advanced (JSON) to edit the raw configuration directly.</p>
          <VersionConfigForm value={versionFormData} errors={versionFormErrors} onChange={handleVersionFormChange} />
        </>
      ) : (
        <>
          <div className="config-label-row"><label htmlFor="config">Configuration JSON</label><a href={CONFIG_SCHEMA_DOCS_URL} target="_blank" rel="noreferrer">View schema docs</a></div>
          <p id="config-help" className="config-help-text">{CONFIG_SCHEMA_HELP_TEXT}</p>
          <details className="config-example-help"><summary>Show complete valid example</summary><pre>{CONFIG_EXAMPLE_JSON}</pre></details>
          <textarea id="config" value={newVersionConfig} onChange={handleNewVersionConfigChange} placeholder={CONFIG_EXAMPLE_JSON} rows="14" required aria-describedby="config-help config-required-fields" />
          <p id="config-required-fields" className="config-required-fields">Required fields: {CONFIG_REQUIRED_FIELDS.map((field) => `\`${field}\``).join(', ')}.</p>
        </>
      )}
      <div className="form-actions">
        <button type="button" className="btn-primary" disabled={creating || !createValidationResult?.valid} onClick={handleCreateVersion} title={createValidationResult?.valid ? 'Create a new version with this configuration' : 'Validate the configuration before creating'}>{creating ? 'Creating...' : 'Create Version'}</button>
        <button type="button" className="btn-secondary" onClick={handleValidateCreateVersion} disabled={validatingCreate}>{validatingCreate ? 'Validating...' : 'Validate'}</button>
        <button type="button" onClick={handleCancelCreateVersion} className="btn-secondary">Cancel</button>
      </div>
      {createValidationError && <div className="validation-error-banner">{createValidationError}</div>}
      {createValidationResult && (
        <div className={createValidationResult.valid ? 'validation-success-banner' : 'validation-error-banner'}>
          <strong>{createValidationResult.valid ? '✓ Configuration is valid' : '✗ Configuration has errors'}</strong>
          {createValidationResult.errors?.length > 0 && <ul>{createValidationResult.errors.map((err, idx) => <li key={idx}><strong>{err.field}:</strong> {err.message}</li>)}</ul>}
          {createValidationResult.warnings?.length > 0 && <ul>{createValidationResult.warnings.map((warn, idx) => <li key={idx}><strong>{warn.field}:</strong> {warn.message}</li>)}</ul>}
        </div>
      )}
    </div>
  );
}

export default CreateVersionForm;
