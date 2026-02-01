# ADR-0022: Explicit CORS and CSRF Policy for Frontend Integration

**Date**: 2026-02-15

**Status**: Accepted

## Context

The Lift Simulator backend now serves a React admin UI and exposes REST APIs under `/api/v1`.
While Spring Security is in place, the project previously relied on defaults for CORS behavior
and implicitly disabled CSRF without a clearly documented policy. This created ambiguity for:

1. **Frontend development**: predictable cross-origin access from the local React dev server.
2. **Production deployment**: explicit allowed origins for the deployed UI.
3. **Security posture**: an auditable statement of CSRF behavior for stateless APIs.

## Decision

We will define explicit CORS and CSRF policies that are configurable via application settings.

### 1. CORS Policy

The backend exposes explicit CORS configuration for API endpoints:

- **Allowed origins** are configured via `security.cors.allowed-origins` (or `CORS_ALLOWED_ORIGINS` env var).
- **Allowed methods** include GET/POST/PUT/DELETE/PATCH/OPTIONS.
- **Allowed headers** include `Authorization` and `X-API-Key` to support HTTP Basic and API key authentication.
- **Exposed headers** include `WWW-Authenticate` for HTTP Basic challenges.
- **Credentials** are allowed to support HTTP auth headers in browser requests.

### 2. CSRF Policy

CSRF is **explicitly disabled by default** because the backend APIs are stateless and authenticated
via HTTP Basic or API key headers. This policy is configurable via `security.csrf.enabled`.
If enabled, CSRF protection uses cookie-backed tokens and can ignore API and actuator routes
via `security.csrf.ignored-paths`.

## Implementation Details

### Security Configuration

`SecurityConfig` now applies CORS and CSRF settings to all security filter chains. CORS settings
are applied via a shared `CorsConfigurationSource`, and CSRF configuration is explicitly toggled
based on `security.csrf.enabled`.

### Configuration Properties

```properties
security.cors.allowed-origins=http://localhost:3000,http://127.0.0.1:3000
security.cors.allowed-methods=GET,POST,PUT,DELETE,PATCH,OPTIONS
security.cors.allowed-headers=Authorization,Content-Type,X-API-Key,X-Requested-With,Accept,Origin
security.cors.exposed-headers=WWW-Authenticate
security.cors.allow-credentials=true
security.cors.max-age=3600

security.csrf.enabled=false
security.csrf.ignored-paths=/api/**,/actuator/**
```

## Consequences

### Positive

1. **Predictable cross-origin behavior** across dev and production deployments.
2. **Explicit security posture** for CSRF, with a clear opt-in path if session-based flows are introduced.
3. **Frontend compatibility** with HTTP Basic and API key headers in browsers.
4. **Testable security behavior** with integration tests for CORS preflight handling.

### Negative

1. **Additional configuration surface area** that must be maintained in deployments.
2. **CSRF disabled by default** remains a conscious trade-off for stateless APIs.

## Alternatives Considered

### 1. Rely on Spring Boot defaults

**Pros**: Minimal configuration, fewer properties.

**Cons**: Unclear production behavior and no explicit documentation of policy.

### 2. Enable CSRF universally

**Pros**: Stronger protection for browser-based sessions.

**Cons**: Breaks stateless API clients unless tokens are managed end-to-end; increases frontend complexity.

## References

- ADR-0019: Spring Security Baseline
- ADR-0021: Role-Based Access Control (RBAC)
