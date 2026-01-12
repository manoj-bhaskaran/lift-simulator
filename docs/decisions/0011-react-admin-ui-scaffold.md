# ADR-0011: React Admin UI Scaffold

**Date**: 2026-01-12

**Status**: Accepted

## Context

The lift simulator backend (ADR-0006) provides a comprehensive REST API for managing lift system configurations. To make this functionality accessible to administrators and operators, we need a web-based admin interface. The frontend must:

1. Provide an intuitive interface for managing lift systems and configurations
2. Support CRUD operations on lift systems and versions
3. Integrate with the backend validation framework for configuration validation
4. Monitor backend service health
5. Be maintainable and extensible for future features
6. Work seamlessly in local development and production environments

We evaluated several options for the frontend technology stack:

### Option 1: Server-Side Rendered (Thymeleaf/JSP)
- **Pros**: Simple deployment (packaged with backend), no separate frontend build process
- **Cons**: Limited interactivity, less modern user experience, harder to maintain complex UI state

### Option 2: Vue.js
- **Pros**: Progressive framework, easier learning curve, good documentation
- **Cons**: Smaller ecosystem compared to React, less industry adoption for enterprise applications

### Option 3: React with Create React App
- **Pros**: Most popular React setup tool, comprehensive documentation
- **Cons**: Considered legacy (no longer actively maintained), slower build times, outdated tooling

### Option 4: React with Vite
- **Pros**:
  - Modern build tool with fast HMR (Hot Module Replacement)
  - Industry standard for new React projects
  - Optimized production builds
  - Built-in dev server with proxy support
  - Active development and strong community
  - Native ES modules support
- **Cons**: Requires Node.js environment for development

## Decision

We will build the admin UI as a **React 19.2.0 application using Vite 7.2.4** as the build tool.

### Implementation Details

1. **Core Framework**: React 19.2.0 with functional components and hooks
2. **Build Tool**: Vite 7.2.4 for development server and production builds
3. **Routing**: React Router 7.12.0 for client-side navigation
4. **HTTP Client**: Axios 1.13.2 for API integration
5. **Project Structure**:
   ```
   frontend/
   ├── src/
   │   ├── api/          - API client and service methods
   │   ├── components/   - Reusable UI components
   │   ├── pages/        - Page components for routes
   │   ├── App.jsx       - Root component with routing
   │   └── main.jsx      - Entry point
   ├── public/           - Static assets
   └── package.json      - Dependencies and scripts
   ```

### Key Architecture Decisions

#### 1. Separation of Concerns
- **API Layer** (`src/api/`): Centralized API client configuration and service methods
  - `client.js`: Axios instance with global configuration and error handling
  - `liftSystemsApi.js`: Type-safe API methods for all backend endpoints
- **Components** (`src/components/`): Reusable UI components like Layout
- **Pages** (`src/pages/`): Route-level components (Dashboard, LiftSystems, ConfigValidator, HealthCheck)

#### 2. Development Workflow
- **Frontend Port**: 3000 (Vite dev server)
- **Backend Port**: 8080 (Spring Boot)
- **Proxy Configuration**: Vite proxies `/api/*` and `/actuator/*` to backend
  - Eliminates CORS issues during development
  - Simplifies API calls (no need for full URLs)
  - Mirrors production reverse proxy setup

#### 3. Component Architecture
- **Functional Components**: All components use React hooks (no class components)
- **Layout Component**: Provides consistent navigation and structure across pages
- **Page Components**: Self-contained with own state management and data fetching
- **CSS Modules**: Component-specific CSS files for styling isolation

#### 4. API Integration Patterns
```javascript
// Centralized API client with error handling
import apiClient from './client';

export const liftSystemsApi = {
  getAllSystems: () => apiClient.get('/lift-systems'),
  createSystem: (data) => apiClient.post('/lift-systems', data),
  // ... other methods
};
```

Pages use async/await with try/catch for API calls:
```javascript
const loadSystems = async () => {
  try {
    const response = await liftSystemsApi.getAllSystems();
    setSystems(response.data);
  } catch (err) {
    setError('Failed to load lift systems');
  }
};
```

### Initial Feature Set

The scaffold includes four main pages:

1. **Dashboard** (`/`):
   - Overview of lift systems
   - Statistics (total systems, total versions)
   - Quick action buttons

2. **Lift Systems** (`/systems`):
   - Grid view of all lift systems
   - Display system metadata (key, name, description, version count)
   - Placeholders for create/edit operations

3. **Config Validator** (`/config-validator`):
   - Interactive JSON editor with sample configuration
   - Real-time validation using backend API
   - Split-pane layout (editor | results)

4. **Health Check** (`/health`):
   - Backend service status monitoring
   - Manual refresh capability
   - Detailed health information display

### Development Setup

```bash
# Install dependencies
cd frontend
npm install

# Start dev server (runs on port 3000)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

### Production Considerations

1. **Build Output**: Static files in `dist/` directory
2. **Deployment Options**:
   - Serve `dist/` from CDN (AWS S3 + CloudFront, Netlify, Vercel)
   - Serve from Spring Boot as static resources
   - Use reverse proxy (nginx, Apache) to route `/api` to backend
3. **Environment Configuration**: API base URL configurable via environment variables

## Consequences

### Positive

1. **Modern Development Experience**: Fast HMR, modern ES modules, optimized builds
2. **Maintainability**: Clear separation of concerns, reusable components, centralized API layer
3. **Extensibility**: Easy to add new pages, components, and API integrations
4. **Developer Familiarity**: React is widely known, making onboarding easier
5. **Performance**: Vite provides fast dev server and optimized production bundles
6. **Type Safety Potential**: Can migrate to TypeScript incrementally if needed

### Negative

1. **Additional Build Step**: Requires Node.js environment and separate build process
2. **Deployment Complexity**: Two separate deployment artifacts (frontend + backend)
3. **CORS Configuration**: Must configure CORS in production if deployed separately
4. **Learning Curve**: Developers unfamiliar with React need to learn the ecosystem

### Trade-offs

- **Development Speed vs. Runtime Performance**: Vite prioritizes fast development with minimal overhead, accepting slightly larger initial bundle size compared to hand-optimized builds
- **Flexibility vs. Convention**: React provides flexibility in architecture but requires more decisions (state management, routing, styling) compared to opinionated frameworks like Angular
- **Client-Side Routing**: Using React Router improves UX but requires proper server configuration for handling routes in production

## Alternatives Considered

### Alternative 1: Single-Page Backend (Thymeleaf)
Would simplify deployment but limit interactivity and modern UX patterns. Not suitable for the interactive configuration editing and real-time validation features needed.

### Alternative 2: Vue.js + Vite
Viable alternative with similar benefits. Chose React due to:
- Larger talent pool and community
- More mature ecosystem for enterprise applications
- Better integration with testing tools (Jest, React Testing Library)

### Alternative 3: Next.js (React Framework)
Provides SSR and static site generation but adds unnecessary complexity for a pure admin interface without SEO requirements. The admin UI does not need server-side rendering.

## Notes

- This ADR documents the initial scaffold. Future ADRs may cover state management (Redux, Zustand), advanced form handling, authentication/authorization, and testing strategies.
- The scaffold uses placeholder UI for CRUD operations. Future work will implement full create, edit, and delete functionality with form validation.
- No state management library (Redux, Zustand) is included initially. Will add when complexity warrants it (likely when implementing multi-step forms or complex data flows).

## References

- ADR-0006: Spring Boot Admin Backend (the API this UI consumes)
- [Vite Documentation](https://vitejs.dev/)
- [React Router Documentation](https://reactrouter.com/)
- [Axios Documentation](https://axios-http.com/)
