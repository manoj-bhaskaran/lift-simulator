// @ts-check

function LiftSystemInfoSection({ system }) {
  return (
    <div className="detail-section">
      <h3>System Information</h3>
      <div className="info-grid">
        <div className="info-item"><label>Display Name</label><p>{system.displayName}</p></div>
        <div className="info-item"><label>System Key</label><p className="monospace">{system.systemKey}</p></div>
        <div className="info-item"><label>Description</label><p>{system.description || 'No description provided'}</p></div>
        <div className="info-item"><label>Created</label><p>{new Date(system.createdAt).toLocaleString()}</p></div>
        <div className="info-item"><label>Last Updated</label><p>{new Date(system.updatedAt).toLocaleString()}</p></div>
      </div>
    </div>
  );
}

export default LiftSystemInfoSection;
