import { beforeEach, describe, expect, it, vi } from 'vitest';

vi.mock('../../utils/errorHandlers', () => ({
  logApiError: vi.fn(),
}));

import { logApiError } from '../../utils/errorHandlers';

const axiosMock = vi.hoisted(() => ({
  create: vi.fn(),
  client: {
    interceptors: {
      response: {
        use: vi.fn(),
      },
    },
    request: vi.fn(),
  },
}));

vi.mock('axios', () => ({
  default: {
    create: axiosMock.create,
  },
}));

const importClient = async () => {
  await import('../client');
  return axiosMock.create.mock.calls.at(-1)?.[0];
};

describe('api client', () => {
  beforeEach(() => {
    vi.resetModules();
    vi.unstubAllEnvs();
    axiosMock.create.mockReset();
    axiosMock.client.interceptors.response.use.mockReset();
    axiosMock.client.request.mockReset();
    axiosMock.create.mockReturnValue(axiosMock.client);
    logApiError.mockReset();
    vi.useRealTimers();
  });

  it('creates axios client with JSON defaults when auth env vars are absent', async () => {
    const config = await importClient();

    expect(config).toEqual({
      baseURL: '/api/v1',
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
    });
    expect(config.headers).not.toHaveProperty('Authorization');
    expect(config.headers).not.toHaveProperty('X-API-Key');
  });

  it('exports apiBaseUrl and a trailing-slash-normalised variant for reuse across pages', async () => {
    vi.stubEnv('VITE_API_BASE_URL', 'https://api.example.com/api/v1/');

    const clientModule = await import('../client');

    expect(clientModule.apiBaseUrl).toBe('https://api.example.com/api/v1/');
    expect(clientModule.normalizedApiBaseUrl).toBe('https://api.example.com/api/v1');
  });

  it('defaults apiBaseUrl to /api/v1 when unset', async () => {
    const clientModule = await import('../client');

    expect(clientModule.apiBaseUrl).toBe('/api/v1');
    expect(clientModule.normalizedApiBaseUrl).toBe('/api/v1');
  });

  it('adds Basic auth and API key headers from Vite env vars', async () => {
    vi.stubEnv('VITE_ADMIN_USERNAME', 'admin');
    vi.stubEnv('VITE_ADMIN_PASSWORD', 'local-admin-password');
    vi.stubEnv('VITE_API_KEY', 'local-api-key');

    const config = await importClient();

    expect(config.headers).toMatchObject({
      'Content-Type': 'application/json',
      Authorization: `Basic ${btoa('admin:local-admin-password')}`,
      'X-API-Key': 'local-api-key',
    });
  });

  it('omits Basic auth unless both username and password are present', async () => {
    vi.stubEnv('VITE_ADMIN_USERNAME', 'admin');
    vi.stubEnv('VITE_API_KEY', 'local-api-key');

    const config = await importClient();

    expect(config.headers).not.toHaveProperty('Authorization');
    expect(config.headers).toHaveProperty('X-API-Key', 'local-api-key');
  });

  it('retries a safe timed-out request once so cold starts can keep showing loading UI', async () => {
    vi.useFakeTimers();
    await importClient();
    const [, onRejected] = axiosMock.client.interceptors.response.use.mock.calls.at(-1);
    const retryResponse = { data: { status: 'ok' } };
    axiosMock.client.request.mockResolvedValue(retryResponse);

    const retryPromise = onRejected({
      code: 'ECONNABORTED',
      message: 'timeout of 10000ms exceeded',
      config: { url: '/health', method: 'get' },
    });

    await vi.advanceTimersByTimeAsync(500);

    await expect(retryPromise).resolves.toBe(retryResponse);
    expect(axiosMock.client.request).toHaveBeenCalledWith(
      expect.objectContaining({
        url: '/health',
        method: 'get',
        __timeoutRetryCount: 1,
      })
    );
  });



  it('does not retry a timed-out mutating request because the first attempt may have committed', async () => {
    await importClient();
    const [, onRejected] = axiosMock.client.interceptors.response.use.mock.calls.at(-1);
    const timeoutError = {
      code: 'ECONNABORTED',
      message: 'timeout of 10000ms exceeded',
      config: { url: '/lift-systems', method: 'post' },
    };

    await expect(onRejected(timeoutError)).rejects.toBe(timeoutError);
    expect(axiosMock.client.request).not.toHaveBeenCalled();
  });

  it('does not retry a request after the timeout retry has already been used', async () => {
    await importClient();
    const [, onRejected] = axiosMock.client.interceptors.response.use.mock.calls.at(-1);
    const timeoutError = {
      code: 'ECONNABORTED',
      message: 'timeout of 10000ms exceeded',
      config: { url: '/health', method: 'get', __timeoutRetryCount: 1 },
    };

    await expect(onRejected(timeoutError)).rejects.toBe(timeoutError);
    expect(axiosMock.client.request).not.toHaveBeenCalled();
  });

  it('rejects and logs a non-timeout error response without retrying', async () => {
    await importClient();
    const [, onRejected] = axiosMock.client.interceptors.response.use.mock.calls.at(-1);
    const serverError = {
      message: 'Request failed with status code 500',
      config: { url: '/simulation-runs', method: 'post' },
      response: { status: 500, data: { message: 'Internal error' } },
    };

    await expect(onRejected(serverError)).rejects.toBe(serverError);
    expect(axiosMock.client.request).not.toHaveBeenCalled();
    expect(logApiError).toHaveBeenCalledWith(serverError);
  });

  it('rejects and logs a network error (no response) without retrying', async () => {
    await importClient();
    const [, onRejected] = axiosMock.client.interceptors.response.use.mock.calls.at(-1);
    const networkError = {
      message: 'Network Error',
      code: 'ERR_NETWORK',
      config: { url: '/simulation-runs/55', method: 'get' },
    };

    await expect(onRejected(networkError)).rejects.toBe(networkError);
    expect(axiosMock.client.request).not.toHaveBeenCalled();
    expect(logApiError).toHaveBeenCalledWith(networkError);
  });
});
