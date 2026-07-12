# Lift Simulator Admin UI

![Node.js Version](https://img.shields.io/badge/node-%3E%3D20.19.0%20%7C%7C%20%3E%3D22.12.0-brightgreen)
![License](https://img.shields.io/badge/license-MIT-blue)
[![CI](https://github.com/manoj-bhaskaran/lift-simulator/actions/workflows/ci.yml/badge.svg)](https://github.com/manoj-bhaskaran/lift-simulator/actions/workflows/ci.yml)
![React](https://img.shields.io/badge/React-19.x-61DAFB?logo=react)
![Vite](https://img.shields.io/badge/Vite-7.x-646CFF?logo=vite)

React-based admin interface for managing lift system configurations.

## Overview

This is the frontend admin application for the Lift Simulator system. It provides a web-based interface for:
- Managing lift systems and configurations
- Creating and publishing configuration versions
- Validating configuration JSON and displaying detailed validation errors
- Managing simulation runs, including multi-select bulk cancellation for active runs and bulk deletion for completed runs
- Monitoring system health
- Running simulations through decomposed setup, run-status, and results panels shared with run detail views
- Maintaining scenarios and lift-system versions through decomposed page sections for easier UI maintenance

For the overall project setup (backend, database, and Quick Start), see the [root README](../README.md). Backend REST API conventions (auth, RBAC, rate limits, error shape) live in [docs/API.md](../docs/API.md), and the always-current endpoint reference is the generated Swagger UI at `/api/v1/swagger-ui.html`.

## Versioning

The footer displays the current UI version by reading the `version` field from `frontend/package.json` at build time. Update that field as part of each release to keep the footer in sync with the deployed build.

## Tech Stack

This project requires Node.js `^20.19.0 || >=22.12.0` for Vite 7 compatibility and uses the following core dependencies (version ranges as specified in `package.json`):

- **React ^19.2.0** - UI library
- **Vite ^7.3.5** - Build tool and dev server
- **React Router ^7.17.0** - Client-side routing
- **Axios ^1.17.0** - HTTP client for API calls

> **Note:** The caret (^) prefix allows npm to install compatible minor and patch updates. For example, `^19.2.0` allows versions `>=19.2.0` and `<20.0.0`. Run `npm list` to see the exact installed versions.

## Type Checking (JSDoc + Type Declarations)

The frontend remains JavaScript-first, but it includes TypeScript declaration files for core data models to enable editor type checking and autocomplete:

1. Ensure `frontend/jsconfig.json` is present (it enables `checkJs` and `allowJs`).
2. Add `// @ts-check` to the top of any JS file where you want type checking.
3. Use JSDoc imports to reference shared models from `src/types/models.d.ts`.

Example:

```js
// @ts-check

/**
 * @param {import('../types/models').LiftSystem} system
 */
function renderSystem(system) {
  return system.displayName;
}
```

Shared interfaces include `LiftSystem`, `Version`, and `ValidationResult`.

## Prerequisites

- Node.js 20.19+ (or 22.12+) and npm
- Backend service running on `http://localhost:8080`

## Local Development Setup

### 1. Install Dependencies

```bash
npm install
```

### 2. Configure API Credentials

Create `frontend/.env.local` with credentials that match the backend values in `application-dev.yml` or the backend environment. The file is ignored by Git through the frontend `*.local` rule.

```bash
VITE_ADMIN_USERNAME=admin
VITE_ADMIN_PASSWORD=local-admin-password
VITE_API_KEY=local-api-key
```

The Axios client sends `Authorization: Basic ...` when both admin values are set and sends `X-API-Key` when `VITE_API_KEY` is set.

> **⚠️ `VITE_*` variables ship in the bundle.** Vite inlines every `VITE_`-prefixed variable into the compiled JavaScript at build time — there is no server-side secret store. Anyone who loads the app can read `VITE_ADMIN_PASSWORD` and `VITE_API_KEY` from the shipped bundle. `npm run build` **fails** if `VITE_ADMIN_PASSWORD` or `VITE_API_KEY` is set for any `vite build` invocation, including a custom `--mode` (e.g. `--mode staging`), so a real credential can't accidentally end up in a deployed build. Use `VITE_*` credentials for local development or disposable environments only; for hosted deployments, authenticate through a backend/session proxy instead.

### 3. Start Development Server

```bash
npm run dev
```

The application will start on **http://localhost:3000**.

### 4. Start Backend Service

Make sure the Spring Boot backend is running on port 8080. The authenticated admin and simulation APIs require credentials, so export backend credentials before starting Spring Boot:

```bash
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD=local-admin-password
export API_KEY=local-api-key
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Or use your IDE to run the main application class with the same backend environment variables.

## Available Scripts

- `npm run dev` - Start development server (port 3000)
- `npm run build` - Build for production
- `npm run lint` - Run ESLint
- `npm run lint:fix` - Fix lint issues automatically
- `npm run preview` - Preview production build
- `npm test` - Run Playwright tests (headless)
- `npm run test:headed` - Run Playwright tests with browser UI visible
- `npm run test:ui` - Run Playwright tests in interactive UI mode
- `npm run test:debug` - Run Playwright tests in debug mode
- `npm run test:report` - Show HTML test report

## Testing

The frontend uses **Playwright** for end-to-end (E2E) UI automation testing.

### Testing Strategy

- **E2E tests (Playwright)**: Full user journeys against the running application, located in `e2e/*.spec.ts`
- **Unit tests** (future): Components, hooks, and utilities as `*.test.jsx` alongside component files
- **Integration tests** (future): Page-level flows with mocked API calls as `*.spec.jsx` in `src/__tests__/`

### First-Time Setup

Install Playwright browser binaries (once per machine):

```bash
npx playwright install          # Chromium, Firefox, WebKit
npx playwright install chromium # Chromium only, to save disk space
```

### Running Tests

Start the backend first (see step 4 above), then run Playwright with matching E2E-only credentials. Do **not** use `VITE_*` variables for E2E secrets — Vite exposes those to the browser bundle by design; the `E2E_*` variables are injected at browser-context level and are never bundled.

```bash
export E2E_ADMIN_USERNAME=admin
export E2E_ADMIN_PASSWORD=local-admin-password
export E2E_API_KEY=local-api-key
npm test              # headless
npm run test:headed   # with browser UI
npm run test:ui       # interactive UI (recommended for debugging)
npm run test:debug    # step-by-step debug
npm run test:report   # view the HTML report
```

The Playwright web server automatically starts the frontend dev server before tests and shuts it down afterward. Start the backend separately first so feature tests run instead of being skipped.

### Configuration

Playwright is configured in `playwright.config.ts` with:

- **Base URL**: http://localhost:3000
- **API URL**: `/api/v1` by default, proxied by Vite to `http://localhost:8080/api/v1`
- **Backend Health URL**: feature tests check `http://localhost:8080/api/v1/health` before executing
- **E2E Auth Headers**: Playwright injects optional `E2E_ADMIN_USERNAME`/`E2E_ADMIN_PASSWORD` Basic auth and `E2E_API_KEY` headers at browser-context level, mirrored by the Vite dev proxy for `/api/v1` requests; these values are never read by app code or bundled by Vite
- **Retries on CI**: 2 retries to handle flaky tests
- **Artifacts**: screenshots and videos on failure, traces on retry
- **Browsers**: Chromium (default), with Firefox and WebKit commented out

### Current Coverage and Structure

Smoke tests (`e2e/smoke.spec.ts`) cover: application loads and displays the dashboard, navigation to Lift Systems works, and the Health Check and Configuration Validator pages are accessible with the footer version shown. Add new tests as `e2e/*.spec.ts`:

```typescript
import { test, expect } from '@playwright/test';

test.describe('Feature Name', () => {
  test('should perform action', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('text=Expected Text')).toBeVisible();
  });
});
```

See the [Playwright documentation](https://playwright.dev/docs/writing-tests) for more details.

### Future Testing Additions

- **Unit tests**: Vitest + React Testing Library for component testing
- **Integration tests**: Mock Service Worker (MSW) for API mocking
- **Coverage**: target ≥80% statement/branch coverage for new code

## CI/CD

The frontend is built and tested as part of the repository-wide GitHub Actions pipeline (`.github/workflows/ci.yml`), which runs `npm ci`, `npm run lint`, `npm run build`, and the Playwright E2E job against a live backend, plus CodeQL security scanning (`.github/workflows/codeql.yml`). The Playwright HTML report and failure artifacts (screenshots, videos, traces) are uploaded as GitHub Actions artifacts, available under the **Artifacts** section of each workflow run for 30 days. For the full job breakdown and how to run the same checks locally, see the [root README Testing and Quality Checks sections](../README.md#testing).

To reproduce the frontend CI steps locally:

```bash
npm ci
npm run lint
npm run build
E2E_ADMIN_USERNAME=admin E2E_ADMIN_PASSWORD=local-admin-password E2E_API_KEY=local-api-key npm test
```

## Deployment

There is currently **no automated deployment pipeline**. Two deployment models are supported:

- **Spring Boot integration (recommended for this monorepo):** the backend serves the built frontend from the same origin, producing a single JAR with no CORS configuration and consistent versioning. Build and run it from the repository root with the Maven `frontend` profile — see [Building the Project](../README.md#building-the-project) in the root README for the exact commands and asset verification.
- **Standalone static hosting (CDN / Netlify / Vercel / S3+CloudFront):** run `npm run build` and deploy the generated `dist/` directory. When the frontend and backend are hosted separately you must set `VITE_API_BASE_URL` to the backend API URL at build time and configure CORS on the backend to allow the frontend origin. Because `VITE_*` values are inlined into the bundle, never bake real admin passwords or API keys into a hosted build; authenticate through a backend/session proxy instead.

**Deployment checklist:**

- [ ] Confirm `VITE_API_BASE_URL` targets the correct environment.
- [ ] Ensure CORS is configured if frontend and backend are hosted separately.
- [ ] Verify `npm run build` completes without warnings.
- [ ] Smoke test core flows (list systems, create version, publish).
- [ ] Validate backend health (`/actuator/health`) after deployment.

## API Configuration

### Environment Variables

The API client can be configured at build time via Vite environment variables:

| Variable | Description | Default |
| --- | --- | --- |
| `VITE_API_BASE_URL` | Base URL for API requests (e.g., `https://api.example.com/api/v1`) | `/api/v1` |
| `VITE_API_TIMEOUT_MS` | Axios request timeout in milliseconds. Timed-out safe read requests are retried once after a short delay so cold-started backends can keep showing the current loading indicator before an error is displayed; if the backend stays unreachable, the UI shows a plain-language server-reachability message rather than raw transport details. | `10000` |
| `VITE_ADMIN_USERNAME` | Optional admin username used with `VITE_ADMIN_PASSWORD` to build an HTTP Basic `Authorization` header | unset |
| `VITE_ADMIN_PASSWORD` | Optional admin password used with `VITE_ADMIN_USERNAME` to build an HTTP Basic `Authorization` header | unset |
| `VITE_API_KEY` | Optional runtime/simulation API key sent as `X-API-Key` | unset |

If `VITE_API_BASE_URL` is left unset, the app uses `/api/v1`, which works with the Vite proxy in local development. The backend ignores whichever auth header does not apply to a specific endpoint, so the shared Axios client can safely send both. As noted above, `vite.config.js` fails any `vite build` (including `npm run build` and custom `--mode`) if `VITE_ADMIN_PASSWORD` or `VITE_API_KEY` is set; `VITE_ADMIN_USERNAME` alone does not trigger the guard, and `npm run dev`/`npm run preview` are unaffected.

### Playwright E2E Environment Variables

The Playwright configuration and Vite dev proxy inject backend authentication headers for local and CI E2E runs without exposing credentials through Vite's client-side environment variables:

| Variable | Description | Default |
| --- | --- | --- |
| `E2E_ADMIN_USERNAME` | Optional admin username used to build a Basic auth header for Playwright browser requests | unset |
| `E2E_ADMIN_PASSWORD` | Optional admin password used to build a Basic auth header for Playwright browser requests | unset |
| `E2E_API_KEY` | Optional runtime/simulation API key sent as `X-API-Key` by Playwright browser requests | unset |

### Proxy and Ports

The Vite dev server proxies `/api/*` and `/actuator/*` to `http://localhost:8080`, eliminating CORS issues during local development. The frontend runs on **http://localhost:3000** and the backend on **http://localhost:8080**.

## Project Structure

```
frontend/
├── public/              # Static assets
├── src/
│   ├── api/             # API client and service methods
│   │   ├── client.js    # Axios configuration
│   │   └── liftSystemsApi.js  # API methods
│   ├── components/      # Reusable components
│   │   ├── Layout.jsx   # Main layout with navigation
│   │   └── Layout.css
│   ├── hooks/           # Shared React hooks (e.g. useRunPolling)
│   ├── pages/           # Page components
│   │   ├── Dashboard.jsx
│   │   ├── LiftSystems.jsx
│   │   ├── ConfigValidator.jsx
│   │   └── HealthCheck.jsx
│   ├── App.jsx          # Root component with routes
│   ├── main.jsx         # Entry point
│   └── index.css        # Global styles
├── index.html           # HTML template
├── vite.config.js       # Vite configuration
└── package.json         # Dependencies and scripts
```

## Features

- **Dashboard** — overview of lift systems with quick statistics and action links
- **Lift Systems Management** — view, create, and inspect systems and their versions; jump to the versions section; launch a local simulator for published configurations
- **Configuration Validator** — validate configuration JSON with real-time feedback and a sample configuration
- **Health Check** — monitor backend service health

The frontend integrates with the backend `/api/v1` endpoints (lift systems, versions, validation, simulation runs, runtime configuration, and health). The authoritative endpoint reference is the generated Swagger UI (`/api/v1/swagger-ui.html`); cross-cutting conventions are documented in [docs/API.md](../docs/API.md).

## Troubleshooting

- **Backend connection issues** (e.g. `Cannot reach the server. Please check it is running and try again.`): verify the backend is running (`curl http://localhost:8080/api/v1/health`), check backend logs, and confirm PostgreSQL is running and accessible.
- **Port already in use**: if port 3000 is busy, Vite tries the next available port — check the console output for the actual port.
- **CORS errors**: the proxy configuration should prevent these locally; verify `vite.config.js` proxy settings, restart `npm run dev`, and clear the browser cache.

For broader setup and database troubleshooting, see [docs/TROUBLESHOOTING.md](../docs/TROUBLESHOOTING.md).

## Maintenance

Keep dependencies current and in sync with this README:

```bash
npm outdated                        # list outdated dependencies
npx npm-check-updates --interactive # review and choose updates
npm install                         # apply
npm audit                           # check for vulnerabilities
```

Review changelogs before major-version updates, test after updating (`npm test`, `npm run build`, `npm run dev`), and when versions change update `package.json`, the **Tech Stack** section above, and the project CHANGELOG.

## Contributing

Follow the project's coding standards and submit pull requests for review.
