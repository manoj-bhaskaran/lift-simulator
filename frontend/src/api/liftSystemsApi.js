import apiClient from './client';

export const liftSystemsApi = {
  // Lift Systems
  getAllSystems: () => apiClient.get('/lift-systems'),
  getSystem: (id) => apiClient.get(`/lift-systems/${id}`),
  createSystem: (data) => apiClient.post('/lift-systems', data),
  updateSystem: (id, data) => apiClient.put(`/lift-systems/${id}`, data),
  deleteSystem: (id) => apiClient.delete(`/lift-systems/${id}`),

  // Versions
  getVersions: (systemId) => apiClient.get(`/lift-systems/${systemId}/versions`),
  getVersion: (systemId, versionNumber) =>
    apiClient.get(`/lift-systems/${systemId}/versions/${versionNumber}`),
  createVersion: (systemId, data) =>
    apiClient.post(`/lift-systems/${systemId}/versions`, data),
  updateVersion: (systemId, versionNumber, data) =>
    apiClient.put(`/lift-systems/${systemId}/versions/${versionNumber}`, data),
  publishVersion: (systemId, versionNumber) =>
    apiClient.post(`/lift-systems/${systemId}/versions/${versionNumber}/publish`),
  runSimulation: (systemKey) =>
    apiClient.post(`/runtime/systems/${systemKey}/simulate`),

  // Validation
  validateConfig: (config) => apiClient.post('/config/validate', config),

  // Health Check
  healthCheck: () => apiClient.get('/health'),

  // Scenarios
  getAllScenarios: () => apiClient.get('/scenarios'),
  getScenario: (id) => apiClient.get(`/scenarios/${id}`),
  createScenario: (data) => apiClient.post('/scenarios', data),
  updateScenario: (id, data) => apiClient.put(`/scenarios/${id}`, data),
  deleteScenario: (id) => apiClient.delete(`/scenarios/${id}`),
  validateScenario: (data) => apiClient.post('/scenarios/validate', data),
};
