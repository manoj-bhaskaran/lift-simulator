import { describe, it, expect } from 'vitest';
import { getStatusBadgeClass } from '../statusUtils';

describe('getStatusBadgeClass', () => {
  it('returns published class for PUBLISHED status', () => {
    expect(getStatusBadgeClass('PUBLISHED')).toBe('status-badge status-published');
  });

  it('returns draft class for DRAFT status', () => {
    expect(getStatusBadgeClass('DRAFT')).toBe('status-badge status-draft');
  });

  it('returns archived class for ARCHIVED status', () => {
    expect(getStatusBadgeClass('ARCHIVED')).toBe('status-badge status-archived');
  });

  it('returns base class for unknown status', () => {
    expect(getStatusBadgeClass('UNKNOWN')).toBe('status-badge');
  });

  it('returns base class for undefined status', () => {
    expect(getStatusBadgeClass(undefined)).toBe('status-badge');
  });

  it('returns base class for empty string', () => {
    expect(getStatusBadgeClass('')).toBe('status-badge');
  });

  it('is case-sensitive (lowercase does not match)', () => {
    expect(getStatusBadgeClass('published')).toBe('status-badge');
    expect(getStatusBadgeClass('draft')).toBe('status-badge');
  });
});
