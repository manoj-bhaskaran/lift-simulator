# Lift Simulator Admin UI

React-based admin interface for managing lift system configurations.

## Overview

This is the frontend admin application for the Lift Simulator system. It provides a web-based interface for:
- Managing lift systems and configurations
- Creating and publishing configuration versions
- Validating configuration JSON
- Monitoring system health

## Tech Stack

- **React 19.2.0** - UI library
- **Vite 7.2.4** - Build tool and dev server
- **React Router 7.12.0** - Client-side routing
- **Axios 1.13.2** - HTTP client for API calls

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

## Production Build with Spring Boot

For a single-app setup (frontend served by Spring Boot on port 8080), build from the repository root:

```bash
mvn -Pfrontend clean package
java -jar target/lift-simulator-0.32.1.jar
```

This packages the React build output into the Spring Boot JAR and serves it from `/`.

## Available Scripts

- `npm run dev` - Start development server (port 3000)
- `npm run build` - Build for production
- `npm run preview` - Preview production build
- `npm run lint` - Run ESLint

## API Configuration

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
- Placeholder UI for CRUD operations

### Configuration Validator
- Validate lift system configuration JSON
- Real-time validation feedback
- Sample configuration provided

### Health Check
- Monitor backend service health
- Real-time status updates
- Detailed health information

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

## Production Build

To build for production:

```bash
npm run build
```

The build output will be in the `dist/` directory. The production build can be served by:

```bash
npm run preview
```

Or deploy the `dist/` directory to your web server or CDN.

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
