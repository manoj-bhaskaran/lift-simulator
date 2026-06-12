import axios from 'axios';

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '/api/v1').trim() || '/api/v1';
const timeoutFromEnv = Number(import.meta.env.VITE_API_TIMEOUT_MS);
const requestTimeoutMs =
  Number.isFinite(timeoutFromEnv) && timeoutFromEnv > 0 ? timeoutFromEnv : 10000;
const adminUsername = (import.meta.env.VITE_ADMIN_USERNAME || '').trim();
const adminPassword = import.meta.env.VITE_ADMIN_PASSWORD || '';
const apiKey = (import.meta.env.VITE_API_KEY || '').trim();
const defaultHeaders = {
  'Content-Type': 'application/json',
};

if (adminUsername && adminPassword) {
  defaultHeaders.Authorization = `Basic ${btoa(`${adminUsername}:${adminPassword}`)}`;
}

if (apiKey) {
  defaultHeaders['X-API-Key'] = apiKey;
}

const apiClient = axios.create({
  baseURL: apiBaseUrl,
  timeout: requestTimeoutMs,
  headers: defaultHeaders,
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error.response?.data || error.message);
    return Promise.reject(error);
  }
);

export default apiClient;
