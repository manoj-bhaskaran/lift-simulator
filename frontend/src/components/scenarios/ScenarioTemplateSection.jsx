// @ts-check

function ScenarioTemplateSection({ floorRange, selectedVersionId, templates, selectedTemplateKey, onApplyTemplate }) {
  return (
    <div className="form-section">
      <h3>Quick Start Templates</h3>
      {floorRange && <p className="help-text" style={{ marginBottom: '1rem' }}>Templates will be adapted to the selected floor range ({floorRange.minFloor} to {floorRange.maxFloor}).</p>}
      {!selectedVersionId && <p className="help-text" style={{ marginBottom: '1rem', color: '#e67e22' }}>Select a lift system and version above to ensure templates use valid floor ranges.</p>}
      <div className="template-grid">
        {Object.entries(templates).map(([key, template]) => (
          <button key={key} type="button" className={`template-card${selectedTemplateKey === key ? ' is-selected' : ''}`} onClick={() => onApplyTemplate(key)} aria-pressed={selectedTemplateKey === key}>
            <div className="template-name">{template.name}</div>
            <div className="template-description">{template.description}</div>
          </button>
        ))}
      </div>
    </div>
  );
}

export default ScenarioTemplateSection;
