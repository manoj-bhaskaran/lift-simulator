import process from 'node:process'
import { Buffer } from 'node:buffer'
import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

function buildE2EProxyHeaders() {
  const headers = {}
  const adminUsername = process.env.E2E_ADMIN_USERNAME?.trim()
  const adminPassword = process.env.E2E_ADMIN_PASSWORD?.trim()
  const apiKey = process.env.E2E_API_KEY?.trim()

  if (adminUsername && adminPassword) {
    headers.Authorization = `Basic ${Buffer.from(`${adminUsername}:${adminPassword}`).toString('base64')}`
  }

  if (apiKey) {
    headers['X-API-Key'] = apiKey
  }

  return headers
}

// Vite inlines every VITE_-prefixed variable into the compiled client bundle,
// so any `vite build` output must never embed real backend credentials. This
// applies regardless of --mode: `build` always produces a shippable bundle,
// and a custom mode (e.g. `--mode staging`) is not a safe way to opt out of
// the check. Fail the build rather than ship them; see frontend/README.md
// for the safe options.
function assertNoProductionCredentials(mode, command) {
  if (command !== 'build') {
    return
  }

  const env = loadEnv(mode, process.cwd(), 'VITE_')
  const offendingVars = [
    env.VITE_ADMIN_PASSWORD?.trim() ? 'VITE_ADMIN_PASSWORD' : null,
    env.VITE_API_KEY?.trim() ? 'VITE_API_KEY' : null,
  ].filter(Boolean)

  if (offendingVars.length > 0) {
    throw new Error(
      `Refusing to create a build with ${offendingVars.join(' and ')} set. ` +
        'VITE_* variables are embedded in the compiled client bundle and are readable by ' +
        'anyone who loads the app. Unset these before building and authenticate via ' +
        'a backend/session proxy instead. See frontend/README.md for details.'
    )
  }
}

const e2eProxyHeaders = buildE2EProxyHeaders()

// https://vite.dev/config/
export default defineConfig(({ mode, command }) => {
  assertNoProductionCredentials(mode, command)

  return {
    plugins: [react()],
    server: {
      port: 3000,
      proxy: {
        '/api/v1': {
          target: 'http://localhost:8080',
          changeOrigin: true,
          headers: e2eProxyHeaders,
        },
        '/actuator': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
  }
})
