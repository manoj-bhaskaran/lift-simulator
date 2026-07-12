// @ts-check

function ScenarioJsonEditor({ jsonText, setJsonText, formErrors, setFormErrors }) {
  return (
    <div className="form-section">
      <h3>Scenario JSON</h3>
      <div className="form-group">
        <textarea
          value={jsonText}
          onChange={(e) => {
            setJsonText(e.target.value);
            if (formErrors.durationTicks) setFormErrors(prev => ({ ...prev, durationTicks: '' }));
          }}
          rows={20}
          className="json-editor"
          placeholder="Enter scenario JSON..."
        />
        <p className="help-text">Edit the scenario JSON directly. Valid fields: durationTicks, passengerFlows, seed (optional)</p>
        {formErrors.durationTicks && <span className="error-message">{formErrors.durationTicks}</span>}
      </div>
    </div>
  );
}

export default ScenarioJsonEditor;
