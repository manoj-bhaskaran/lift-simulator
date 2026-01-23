import apiClient from './client';

export const simulationRunsApi = {
  createRun: (payload) => apiClient.post('/simulation-runs', payload),
  getRun: (id) => apiClient.get(`/simulation-runs/${id}`),
  getResults: (id) => apiClient.get(`/simulation-runs/${id}/results`),
  getArtefacts: (id) => apiClient.get(`/simulation-runs/${id}/artefacts`),
};
