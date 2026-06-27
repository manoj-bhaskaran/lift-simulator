import apiClient from './client';

export const scenariosApi = {
  // Scenarios
  getAllScenarios: () => apiClient.get('/scenarios'),
  getScenario: (id) => apiClient.get(`/scenarios/${id}`),
  createScenario: (data) => apiClient.post('/scenarios', data),
  updateScenario: (id, data) => apiClient.put(`/scenarios/${id}`, data),
  deleteScenario: (id) => apiClient.delete(`/scenarios/${id}`),
  getScenarioRunCount: (id) => apiClient.get(`/scenarios/${id}/run-count`),
  copyScenario: (id, data) => apiClient.post(`/scenarios/${id}/copy`, data),

  // Validation
  validateScenario: (data) => apiClient.post('/scenarios/validate', data),
};
