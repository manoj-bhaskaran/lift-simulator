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
