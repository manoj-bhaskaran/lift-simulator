import apiClient from './client';

export const simulationRunsApi = {
  /**
   * Lists all simulation runs with optional filtering.
   * @param {Object} params - Optional filter parameters
   * @param {number} [params.systemId] - Filter by lift system ID
   * @param {string} [params.status] - Filter by status (CREATED, RUNNING, SUCCEEDED, FAILED, CANCELLED)
   * @returns {Promise} API response with list of simulation runs
   */
  listRuns: (params = {}) => {
    const queryParams = new URLSearchParams();
    if (params.systemId) queryParams.append('systemId', params.systemId);
    if (params.status) queryParams.append('status', params.status);
    const queryString = queryParams.toString();
    return apiClient.get(`/simulation-runs${queryString ? `?${queryString}` : ''}`);
  },
  createRun: (payload) => apiClient.post('/simulation-runs', payload),
  getRun: (id) => apiClient.get(`/simulation-runs/${id}`),
  getResults: (id) => apiClient.get(`/simulation-runs/${id}/results`),
  getArtefacts: (id) => apiClient.get(`/simulation-runs/${id}/artefacts`),
  cancelRun: (id) => apiClient.post(`/simulation-runs/${id}/cancel`),
};
