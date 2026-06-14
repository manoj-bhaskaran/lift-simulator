import apiClient from './client';

export const simulationRunsApi = {
  /**
   * Lists simulation runs with optional filtering and pagination.
   * @param {Object} params - Optional filter and pagination parameters
   * @param {number} [params.systemId] - Filter by lift system ID
   * @param {string} [params.status] - Filter by status (CREATED, RUNNING, SUCCEEDED, FAILED, CANCELLED)
   * @param {number} [params.page=0] - Zero-based page number
   * @param {number} [params.size=20] - Page size (max 100)
   * @param {string} [params.sort='createdAt,desc'] - Sort field and direction
   * @returns {Promise} API response with paginated simulation runs (content, totalElements, totalPages)
   */
  listRuns: (params = {}) => {
    const queryParams = new URLSearchParams();
    if (params.systemId) queryParams.append('systemId', params.systemId);
    if (params.status) queryParams.append('status', params.status);
    queryParams.append('page', params.page ?? 0);
    queryParams.append('size', params.size ?? 20);
    queryParams.append('sort', params.sort ?? 'createdAt,desc');
    return apiClient.get(`/simulation-runs?${queryParams.toString()}`);
  },
  createRun: (payload) => apiClient.post('/simulation-runs', payload),
  getRun: (id) => apiClient.get(`/simulation-runs/${id}`),
  getResults: (id) => apiClient.get(`/simulation-runs/${id}/results`),
  getArtefacts: (id) => apiClient.get(`/simulation-runs/${id}/artefacts`),
  cancelRun: (id) => apiClient.post(`/simulation-runs/${id}/cancel`),
  /**
   * Deletes a completed simulation run, removing its run history and stored artefacts.
   * Only runs in a terminal state (SUCCEEDED, FAILED, CANCELLED) can be deleted.
   * @param {number|string} id - Simulation run ID
   * @returns {Promise} API response resolving on 204 No Content
   */
  deleteRun: (id) => apiClient.delete(`/simulation-runs/${id}`),
};
