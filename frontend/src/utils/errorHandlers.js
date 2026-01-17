export function getApiErrorMessage(error, fallbackMessage = 'Something went wrong') {
  const detail = error?.response?.data?.message || error?.message;
  if (detail && fallbackMessage) {
    return `${fallbackMessage}: ${detail}`;
  }
  return detail || fallbackMessage;
}

export function handleApiError(error, setError, fallbackMessage) {
  setError(getApiErrorMessage(error, fallbackMessage));
  console.error(error);
}
