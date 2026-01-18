/**
 * Extracts a user-friendly error message from an API error object.
 * Attempts to retrieve the error message from various locations in the error response.
 *
 * @param {Error|Object} error - Error object from API call (axios error or standard Error)
 * @param {string} [fallbackMessage='Something went wrong'] - Default message if no specific error detail found
 * @returns {string} Formatted error message combining fallback and detail, or just the detail/fallback
 *
 * @example
 * // Returns: "Failed to load data: Network timeout"
 * getApiErrorMessage(axiosError, 'Failed to load data');
 *
 * @example
 * // Returns: "Something went wrong"
 * getApiErrorMessage({}, 'Something went wrong');
 */
function formatValidationIssues(issues) {
  if (!Array.isArray(issues) || issues.length === 0) {
    return null;
  }

  const messages = issues
    .map((issue) => {
      if (!issue || !issue.message) {
        return null;
      }
      const fieldPrefix = issue.field ? `${issue.field}: ` : '';
      return `${fieldPrefix}${issue.message}`;
    })
    .filter(Boolean);

  return messages.length > 0 ? messages.join('; ') : null;
}

function formatFieldErrors(fieldErrors) {
  if (!fieldErrors || typeof fieldErrors !== 'object') {
    return null;
  }

  const messages = Object.entries(fieldErrors)
    .map(([field, message]) => (message ? `${field}: ${message}` : null))
    .filter(Boolean);

  return messages.length > 0 ? messages.join('; ') : null;
}

export function getApiErrorMessage(error, fallbackMessage = 'Something went wrong') {
  const responseData = error?.response?.data;
  const validationDetail =
    formatValidationIssues(responseData?.errors) ||
    formatFieldErrors(responseData?.fieldErrors);
  const detail = validationDetail || responseData?.message || error?.message;

  if (detail && fallbackMessage) {
    return `${fallbackMessage}: ${detail}`;
  }
  return detail || fallbackMessage;
}

/**
 * Handles API errors by extracting the error message, updating state, and logging to console.
 * Convenience function that combines error message extraction with state updates.
 *
 * @param {Error|Object} error - Error object from API call
 * @param {Function} setError - React state setter function for error message
 * @param {string} fallbackMessage - Context-specific error message prefix
 *
 * @example
 * try {
 *   await api.getData();
 * } catch (err) {
 *   handleApiError(err, setError, 'Failed to load data');
 * }
 */
export function handleApiError(error, setError, fallbackMessage) {
  setError(getApiErrorMessage(error, fallbackMessage));
  console.error(error);
}
