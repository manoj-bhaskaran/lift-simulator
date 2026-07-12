// @ts-check
import VersionActions from '../VersionActions';
import { getStatusBadgeClass } from '../../utils/statusUtils';

function VersionCard({ version, systemId, onPublish, onRunSimulation, formatVersionConfig }) {
  return (
    <div className="version-card">
      <div className="version-header">
        <div>
          <h4>Version {version.versionNumber}</h4>
          <span className={getStatusBadgeClass(version.status)}>{version.status}</span>
        </div>
        <VersionActions systemId={systemId} versionNumber={version.versionNumber} status={version.status} onPublish={onPublish} onRunSimulation={onRunSimulation} />
      </div>
      <div className="version-info">
        <div className="info-row"><span className="label">Created:</span><span>{new Date(version.createdAt).toLocaleString()}</span></div>
        {version.publishedAt && <div className="info-row"><span className="label">Published:</span><span>{new Date(version.publishedAt).toLocaleString()}</span></div>}
        <details className="config-details"><summary>View Configuration</summary><pre className="config-preview">{formatVersionConfig(version.config)}</pre></details>
      </div>
    </div>
  );
}

export default VersionCard;
