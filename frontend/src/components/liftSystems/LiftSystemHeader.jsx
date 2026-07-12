// @ts-check
import { Link } from 'react-router-dom';

function LiftSystemHeader({ system, onEdit, onDelete }) {
  return (
    <div className="detail-header">
      <div>
        <Link to="/systems" className="breadcrumb">← Back to Systems</Link>
        <h2>{system.displayName}</h2>
        <p className="system-key">{system.systemKey}</p>
      </div>
      <div className="header-actions">
        <button onClick={onEdit} className="btn-secondary">Edit System</button>
        <button onClick={onDelete} className="btn-danger">Delete System</button>
      </div>
    </div>
  );
}

export default LiftSystemHeader;
