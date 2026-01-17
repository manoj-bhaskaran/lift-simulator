import { Link } from 'react-router-dom';

function VersionActions({
  systemId,
  versionNumber,
  status,
  onPublish,
  onRunSimulation,
  runningVersion,
}) {
  const isDraft = status === 'DRAFT';
  const isPublished = status === 'PUBLISHED';
  const isRunning = runningVersion === versionNumber;

  return (
    <div className="version-actions">
      <Link
        to={`/systems/${systemId}/versions/${versionNumber}/edit`}
        className="btn-secondary btn-sm"
      >
        {isDraft ? 'Edit Config' : 'View Config'}
      </Link>
      {isDraft ? (
        <button
          onClick={() => onPublish(versionNumber)}
          className="btn-primary btn-sm"
        >
          Publish
        </button>
      ) : (
        isPublished && (
          <button
            onClick={() => onRunSimulation(versionNumber)}
            className="btn-primary btn-sm"
            disabled={isRunning}
          >
            {isRunning ? 'Starting...' : 'Run Simulator'}
          </button>
        )
      )}
    </div>
  );
}

export default VersionActions;
