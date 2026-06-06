import process from 'node:process'
import { Buffer } from 'node:buffer'
import { defineConfig } from 'vite'
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

const e2eProxyHeaders = buildE2EProxyHeaders()

// https://vite.dev/config/
export default defineConfig({
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
})
