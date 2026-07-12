// @ts-check

function ScenarioValidationResults({ validationErrors, validationWarnings }) {
  if (validationErrors.length === 0 && validationWarnings.length === 0) return null;

  return (
    <div className="form-section">
      <h3>Validation Results</h3>
      {validationErrors.length > 0 && (
        <div className="validation-errors"><h4>Errors:</h4><ul>{validationErrors.map((error, index) => <li key={index}><strong>{error.field}:</strong> {error.message}</li>)}</ul></div>
      )}
      {validationWarnings.length > 0 && (
        <div className="validation-warnings"><h4>Warnings:</h4><ul>{validationWarnings.map((warning, index) => <li key={index}><strong>{warning.field}:</strong> {warning.message}</li>)}</ul></div>
      )}
    </div>
  );
}

export default ScenarioValidationResults;
