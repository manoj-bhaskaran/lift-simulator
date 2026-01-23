import apiClient from './client';

export const scenariosApi = {
  // Scenarios
  getAllScenarios: () => apiClient.get('/scenarios'),
  getScenario: (id) => apiClient.get(`/scenarios/${id}`),
  createScenario: (data) => apiClient.post('/scenarios', data),
  updateScenario: (id, data) => apiClient.put(`/scenarios/${id}`, data),
  deleteScenario: (id) => apiClient.delete(`/scenarios/${id}`),

  // Validation
  validateScenario: (scenarioJson) => apiClient.post('/scenarios/validate', scenarioJson),
};
