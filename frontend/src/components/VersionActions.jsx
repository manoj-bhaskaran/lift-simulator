// @ts-check
import { Link } from 'react-router-dom';

/**
 * Action buttons component for lift system version management.
 * Provides context-aware actions based on version status (DRAFT, PUBLISHED, ARCHIVED).
 *
 * Actions available:
 * - DRAFT versions: Edit Config, Publish buttons
 * - PUBLISHED versions: View Config, Run Simulator buttons
 * - ARCHIVED versions: View Config button only
 *
 * @param {Object} props - Component props
 * @param {string|number} props.systemId - Unique identifier of the lift system
 * @param {string|number} props.versionId - Unique identifier of the version
 * @param {number} props.versionNumber - Version number being acted upon
 * @param {'DRAFT'|'PUBLISHED'|'ARCHIVED'} props.status - Current status of the version
 * @param {Function} props.onPublish - Callback function invoked when Publish button is clicked, receives versionNumber
 * @param {Function} props.onRunSimulation - Callback function invoked when Run Simulator button is clicked, receives versionId
 * @returns {JSX.Element} The version actions component
 */
function VersionActions({
  systemId,
  versionId,
  versionNumber,
  status,
  onPublish,
  onRunSimulation,
}) {
  const isDraft = status === 'DRAFT';
  const isPublished = status === 'PUBLISHED';

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
            onClick={() => onRunSimulation(versionId)}
            className="btn-primary btn-sm"
          >
            Run Simulator
          </button>
        )
      )}
    </div>
  );
}

export default VersionActions;
