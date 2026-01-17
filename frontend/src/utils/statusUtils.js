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
