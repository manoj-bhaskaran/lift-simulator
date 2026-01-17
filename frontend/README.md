# Lift Simulator Admin UI

![Node.js Version](https://img.shields.io/badge/node-%3E%3D18.0.0-brightgreen)
![License](https://img.shields.io/badge/license-MIT-blue)
![React](https://img.shields.io/badge/React-19.x-61DAFB?logo=react)
![Vite](https://img.shields.io/badge/Vite-7.x-646CFF?logo=vite)

React-based admin interface for managing lift system configurations.

## Overview

This is the frontend admin application for the Lift Simulator system. It provides a web-based interface for:
- Managing lift systems and configurations
- Creating and publishing configuration versions
- Validating configuration JSON
- Monitoring system health

## Tech Stack

This project uses the following core dependencies (version ranges as specified in `package.json`):

- **React ^19.2.0** - UI library
- **Vite ^7.2.4** - Build tool and dev server
- **React Router ^7.12.0** - Client-side routing
- **Axios ^1.13.2** - HTTP client for API calls

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

- Node.js 18+ and npm
- Backend service running on `http://localhost:8080`

## Local Development Setup

### 1. Install Dependencies

```bash
npm install
```

### 2. Start Development Server

```bash
npm run dev
```

The application will start on **http://localhost:3000**

### 3. Start Backend Service

Make sure the Spring Boot backend is running on port 8080. From the project root:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Or use your IDE to run the main application class.

## Available Scripts

- `npm run dev` - Start development server (port 3000)
- `npm run build` - Build for production
- `npm run preview` - Preview production build
- `npm run lint` - Run ESLint

## Deployment

This section covers production deployment options for the admin UI.

### Primary Deployment: Spring Boot Integration (Recommended)

**Use this approach for the monorepo setup where the frontend is served by the Spring Boot backend.**

This is the recommended deployment model for most use cases. It provides:
- Single JAR deployment with both frontend and backend
- Simplified deployment and operations
- No CORS configuration needed
- Consistent versioning across frontend and backend

**Build and deploy:**

From the repository root:

```bash
mvn -Pfrontend clean package
java -jar target/lift-simulator-0.39.0.jar
```

This command:
1. Builds the React app using Vite
2. Copies the build output to `src/main/resources/static`
3. Packages everything into a single Spring Boot JAR
4. Serves the frontend from `http://localhost:8080/`
5. Serves the API from `http://localhost:8080/api`

**Access the application:**
- Frontend: http://localhost:8080
- API: http://localhost:8080/api
- Actuator: http://localhost:8080/actuator

### Alternative Deployment: Standalone Static Hosting

**Use this approach if you need to deploy the frontend separately (CDN, static hosting, etc.).**

This deployment model is suitable when:
- You want to serve the frontend from a CDN
- You need separate scaling for frontend and backend
- You're deploying to a static hosting service (Netlify, Vercel, etc.)

**Build the frontend:**

```bash
cd frontend
npm run build
```

The build output will be in the `dist/` directory.

**Preview the production build locally:**

```bash
npm run preview
```

**Deploy the `dist/` directory to:**
- Static web server (nginx, Apache)
- CDN (CloudFront, Cloudflare)
- Static hosting platforms (Netlify, Vercel, GitHub Pages)

**Important:** When deploying separately, you must:
1. Configure CORS on the backend to allow requests from your frontend domain
2. Set `VITE_API_BASE_URL` environment variable to point to your backend API URL
3. Ensure the backend is accessible from the frontend domain

## API Configuration

### Environment Variables

The API client can be configured at build time via Vite environment variables:

| Variable | Description | Default |
| --- | --- | --- |
| `VITE_API_BASE_URL` | Base URL for API requests (e.g., `https://api.example.com/api`) | `/api` |
| `VITE_API_TIMEOUT_MS` | Axios request timeout in milliseconds | `10000` |

Example `.env` file:

```bash
VITE_API_BASE_URL=http://localhost:8080/api
VITE_API_TIMEOUT_MS=15000
```

If `VITE_API_BASE_URL` is left unset, the app will continue to use `/api`, which works with the Vite proxy in local development.

### Proxy Setup

The Vite dev server is configured to proxy API requests to the backend:

- `/api/*` → `http://localhost:8080/api/*`
- `/actuator/*` → `http://localhost:8080/actuator/*`

This eliminates CORS issues during local development.

### Ports

- **Frontend**: http://localhost:3000
- **Backend**: http://localhost:8080

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

### Dashboard
- Overview of lift systems
- Quick statistics
- Quick action links

### Lift Systems Management
- View all lift systems
- Create new systems
- View system details and versions
- Jump directly to the versions section from the Lift Systems list
- Launch a local simulator for published configurations

### Configuration Validator
- Validate lift system configuration JSON
- Real-time validation feedback
- Sample configuration provided

### Health Check
- Monitor backend service health
- Real-time status updates
- Detailed health information

## Sanity Checks

- From the **Lift Systems** list, select **Manage Versions** and confirm the detail view scrolls to the Versions section.

## API Integration

The frontend integrates with these backend endpoints:

### Admin APIs
- `GET /api/lift-systems` - List all systems
- `POST /api/lift-systems` - Create system
- `GET /api/lift-systems/{id}` - Get system details
- `PUT /api/lift-systems/{id}` - Update system
- `DELETE /api/lift-systems/{id}` - Delete system
- `GET /api/lift-systems/{systemId}/versions` - List versions
- `POST /api/lift-systems/{systemId}/versions` - Create version
- `PUT /api/lift-systems/{systemId}/versions/{versionNumber}` - Update version
- `POST /api/lift-systems/{systemId}/versions/{versionNumber}/publish` - Publish version

### Runtime APIs
- `POST /api/runtime/systems/{systemKey}/simulate` - Launch simulator using published configuration

### Validation API
- `POST /api/config/validate` - Validate configuration

### Health API
- `GET /api/health` - Health check

## Troubleshooting

### Backend Connection Issues

If you see errors about failed API calls:

1. Verify backend is running: `curl http://localhost:8080/api/health`
2. Check backend logs for errors
3. Ensure PostgreSQL database is running and accessible

### Port Already in Use

If port 3000 is in use, Vite will automatically try the next available port. Check the console output for the actual port.

### CORS Errors

The proxy configuration should prevent CORS issues. If you encounter CORS errors:
1. Verify `vite.config.js` proxy settings are correct
2. Restart the dev server: `npm run dev`
3. Clear browser cache and reload

## Maintenance

### Dependency Updates

To keep dependencies up to date and secure, periodically check for and apply updates:

**Check for outdated dependencies:**

```bash
npm outdated
```

**Update dependencies interactively:**

```bash
npx npm-check-updates --interactive
```

This allows you to:
- Review available updates for each dependency
- Choose which updates to apply (major, minor, patch)
- See breaking change warnings

**Apply selected updates:**

```bash
npx npm-check-updates -u
npm install
```

**Best practices:**
- Review changelogs before applying major version updates
- Test thoroughly after updating dependencies
- Update dependencies regularly (monthly recommended)
- Keep the `package.json` ranges in sync with this README
- Run `npm audit` to check for security vulnerabilities

**After updating:**

```bash
npm test          # Run tests (if available)
npm run build     # Verify build still works
npm run dev       # Test in development mode
```

### Version Synchronization

When updating dependency versions:
1. Update `package.json` with new version ranges
2. Update the "Tech Stack" section in this README to match
3. Test the application thoroughly
4. Document breaking changes in the project CHANGELOG

## Next Steps

This is a basic scaffold. Future enhancements could include:

- Full CRUD operations for lift systems and versions
- Rich configuration editor with validation
- User authentication and authorization
- Real-time monitoring and metrics
- Deployment automation
- E2E testing with Cypress or Playwright

## Contributing

Follow the project's coding standards and submit pull requests for review.
