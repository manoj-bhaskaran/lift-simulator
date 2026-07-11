/**
 * Gets the appropriate CSS class names for a version status badge.
 * Returns status-specific classes that control badge styling and color.
 *
 * @param {'PUBLISHED'|'DRAFT'|'ARCHIVED'|string} status - Version status value
 * @returns {string} Space-separated CSS class names for the status badge
 *
 * @example
 * // Returns: "status-badge status-published"
 * getStatusBadgeClass('PUBLISHED');
 *
 * @example
 * // Returns: "status-badge status-draft"
 * getStatusBadgeClass('DRAFT');
 *
 * @example
 * // Returns: "status-badge" (fallback for unknown status)
 * getStatusBadgeClass('UNKNOWN');
 */
export function getStatusBadgeClass(status) {
  switch (status) {
    case 'PUBLISHED':
      return 'status-badge status-published';
    case 'DRAFT':
      return 'status-badge status-draft';
    case 'ARCHIVED':
      return 'status-badge status-archived';
    default:
      return 'status-badge';
  }
}

/**
 * Gets the appropriate CSS class names for a simulation-run status badge.
 *
 * @param {'SUCCEEDED'|'FAILED'|'RUNNING'|'CANCELLED'|'CREATED'|string} status - Simulation run status value
 * @returns {string} Space-separated CSS class names for the status badge
 *
 * @example
 * // Returns: "status-badge status-succeeded"
 * getRunStatusBadgeClass('SUCCEEDED');
 */
export function getRunStatusBadgeClass(status) {
  switch (status) {
    case 'SUCCEEDED':
      return 'status-badge status-succeeded';
    case 'FAILED':
      return 'status-badge status-failed';
    case 'RUNNING':
      return 'status-badge status-running';
    case 'CANCELLED':
      return 'status-badge status-cancelled';
    default:
      return 'status-badge status-created';
  }
}

/**
 * Gets the CSS class for a simulation-run status pill, as used on run status headers.
 *
 * @param {string|null|undefined} status - Simulation run status value
 * @returns {string} CSS class name, e.g. "status-pill running"
 */
export function getRunStatusPillClass(status) {
  return `status-pill ${status ? status.toLowerCase() : ''}`;
}

/**
 * Formats an ISO date string for display.
 *
 * @param {string|null|undefined} dateString - ISO date string
 * @returns {string} Locale-formatted date/time, or '—' when absent
 */
export function formatDate(dateString) {
  if (!dateString) {
    return '—';
  }
  return new Date(dateString).toLocaleString();
}

/**
 * Formats a duration in milliseconds as "Xm Ys" (or "Ys" when under a minute).
 *
 * @param {number|null|undefined} durationMs - Duration in milliseconds
 * @returns {string} Formatted duration, or '—' when absent
 */
export function formatRunDuration(durationMs) {
  if (durationMs == null) {
    return '—';
  }
  const totalSeconds = Math.floor(durationMs / 1000);
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  if (minutes > 0) {
    return `${minutes}m ${seconds}s`;
  }
  return `${seconds}s`;
}
