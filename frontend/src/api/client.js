import axios from 'axios';

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '/api/v1').trim() || '/api/v1';
const timeoutFromEnv = Number(import.meta.env.VITE_API_TIMEOUT_MS);
const requestTimeoutMs =
  Number.isFinite(timeoutFromEnv) && timeoutFromEnv > 0 ? timeoutFromEnv : 10000;
const adminUsername = (import.meta.env.VITE_ADMIN_USERNAME || '').trim();
const adminPassword = import.meta.env.VITE_ADMIN_PASSWORD || '';
const apiKey = (import.meta.env.VITE_API_KEY || '').trim();
export const authHeaders = {};

if (adminUsername && adminPassword) {
  authHeaders.Authorization = `Basic ${btoa(`${adminUsername}:${adminPassword}`)}`;
}

if (apiKey) {
  authHeaders['X-API-Key'] = apiKey;
}

const defaultHeaders = {
  'Content-Type': 'application/json',
  ...authHeaders,
};

const apiClient = axios.create({
  baseURL: apiBaseUrl,
  timeout: requestTimeoutMs,
  headers: defaultHeaders,
});

const TIMEOUT_RETRY_DELAY_MS = 500;
const TIMEOUT_RETRY_LIMIT = 1;
const RETRY_COUNT_KEY = '__timeoutRetryCount';

const delay = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

function isTimeoutError(error) {
  return error?.code === 'ECONNABORTED' || /timeout/i.test(error?.message || '');
}

async function retryTimedOutRequest(error) {
  const config = error?.config;

  if (!config || !isTimeoutError(error)) {
    return Promise.reject(error);
  }

  const retryCount = config[RETRY_COUNT_KEY] || 0;
  if (retryCount >= TIMEOUT_RETRY_LIMIT) {
    return Promise.reject(error);
  }

  const retryConfig = {
    ...config,
    [RETRY_COUNT_KEY]: retryCount + 1,
  };

  await delay(TIMEOUT_RETRY_DELAY_MS);
  return apiClient.request(retryConfig);
}

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    try {
      return await retryTimedOutRequest(error);
    } catch (retryError) {
      console.error('API Error:', retryError.response?.data || retryError.message);
      return Promise.reject(retryError);
    }
  }
);

export default apiClient;
