import { describe, it, expect, vi } from 'vitest';
import { getApiErrorMessage, handleApiError } from '../errorHandlers';

describe('getApiErrorMessage', () => {
  it('returns fallback message when error has no detail', () => {
    expect(getApiErrorMessage({}, 'Something went wrong')).toBe('Something went wrong');
  });

  it('returns default fallback when no args provided', () => {
    expect(getApiErrorMessage({})).toBe('Something went wrong');
  });

  it('returns fallback + responseData.message when present', () => {
    const error = { response: { data: { message: 'Not found' } } };
    expect(getApiErrorMessage(error, 'Failed to load')).toBe('Failed to load: Not found');
  });

  it('maps network errors to a friendly backend-unreachable message', () => {
    const error = { message: 'Network Error' };
    expect(getApiErrorMessage(error, 'Request failed')).toBe(
      'Request failed: Cannot reach the server. Please check it is running and try again.'
    );
  });

  it('maps proxy and gateway errors to a friendly backend-unreachable message', () => {
    const error = {
      response: { status: 502, data: { message: 'Request failed with status code 502' } },
    };
    expect(getApiErrorMessage(error, 'Failed to load lift systems')).toBe(
      'Failed to load lift systems: Cannot reach the server. Please check it is running and try again.'
    );
  });

  it('maps timeout errors to a friendly backend-unreachable message', () => {
    const error = { code: 'ECONNABORTED', message: 'timeout of 10000ms exceeded' };
    expect(getApiErrorMessage(error, '')).toBe(
      'Cannot reach the server. Please check it is running and try again.'
    );
  });

  it('returns detail alone when fallbackMessage is empty string', () => {
    const error = { response: { data: { message: 'Bad request' } } };
    expect(getApiErrorMessage(error, '')).toBe('Bad request');
  });

  it('formats validation issues from errors array', () => {
    const error = {
      response: {
        data: {
          errors: [
            { field: 'name', message: 'is required' },
            { field: 'value', message: 'must be positive' },
          ],
        },
      },
    };
    const result = getApiErrorMessage(error, 'Validation failed');
    expect(result).toBe('Validation failed: name: is required; value: must be positive');
  });

  it('formats validation issues without field prefix when field is absent', () => {
    const error = {
      response: { data: { errors: [{ message: 'general error' }] } },
    };
    const result = getApiErrorMessage(error, 'Oops');
    expect(result).toBe('Oops: general error');
  });

  it('returns null detail from empty errors array', () => {
    const error = { response: { data: { errors: [] } } };
    expect(getApiErrorMessage(error, 'Fallback')).toBe('Fallback');
  });

  it('formats fieldErrors object', () => {
    const error = {
      response: { data: { fieldErrors: { email: 'invalid format', age: 'must be >= 0' } } },
    };
    const result = getApiErrorMessage(error, 'Submit failed');
    expect(result).toContain('email: invalid format');
    expect(result).toContain('age: must be >= 0');
  });

  it('prefers validation errors over responseData.message', () => {
    const error = {
      response: {
        data: {
          errors: [{ field: 'x', message: 'is bad' }],
          message: 'Generic error',
        },
      },
    };
    const result = getApiErrorMessage(error, 'Failed');
    expect(result).toBe('Failed: x: is bad');
  });

  it('handles null error gracefully', () => {
    expect(getApiErrorMessage(null, 'Fallback')).toBe('Fallback');
  });
});

describe('handleApiError', () => {
  it('calls setError with formatted message and logs to console', () => {
    const setError = vi.fn();
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {});
    const error = { response: { data: { message: 'Unauthorized' } } };

    handleApiError(error, setError, 'Access denied');

    expect(setError).toHaveBeenCalledWith('Access denied: Unauthorized');
    expect(consoleSpy).toHaveBeenCalledWith(error);

    consoleSpy.mockRestore();
  });

  it('calls setError with fallback when error has no detail', () => {
    const setError = vi.fn();
    vi.spyOn(console, 'error').mockImplementation(() => {});

    handleApiError({}, setError, 'Default message');

    expect(setError).toHaveBeenCalledWith('Default message');
  });
});
