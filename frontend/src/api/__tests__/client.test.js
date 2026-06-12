import { beforeEach, describe, expect, it, vi } from 'vitest';

const axiosMock = vi.hoisted(() => ({
  create: vi.fn(),
  client: {
    interceptors: {
      response: {
        use: vi.fn(),
      },
    },
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
    axiosMock.create.mockReturnValue(axiosMock.client);
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
});
