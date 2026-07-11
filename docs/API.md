# REST API Conventions

This document covers the cross-cutting conventions of the Lift Simulator backend REST API: authentication, role-based access control, versioning, rate limiting, request-size limits, and the shared error-response shape. **The full, always-current endpoint reference — every path, request/response schema, and field — is generated from the code by SpringDoc, so it is not duplicated here.**

## Generated OpenAPI Specification

The complete endpoint reference is served by the running backend and is the authoritative source of truth for request/response schemas:

- **Interactive Swagger UI:** `http://localhost:8080/api/v1/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/api/v1/api-docs`

Access is controlled by `security.openapi.public-access` (`SECURITY_OPENAPI_PUBLIC_ACCESS`). The checked-in base configuration and the code fallback both default to `false`, so both URLs require ADMIN-role HTTP Basic authentication unless an environment or profile override explicitly enables public access. The development template keeps `${SECURITY_OPENAPI_PUBLIC_ACCESS:true}` for local convenience; set `SECURITY_OPENAPI_PUBLIC_ACCESS=false` in any shared or production environment.

For operational guidance that the generated spec does not capture — CLI/UI run workflows, artefact reproduction, the configuration and scenario schema reference, and simulation-run troubleshooting — see [Workflows and Troubleshooting](Workflows-and-Troubleshooting.md).

## Base URL and Versioning

All application endpoints use the `/api/v1` base path (base URL `http://localhost:8080/api/v1`) unless otherwise noted. The version is carried in the URL path; a future breaking revision would be published under a new prefix (`/api/v2`) so existing clients keep working. See [ADR-0020: URL-Based API Versioning](decisions/0020-url-based-api-versioning.md) for the rationale.

## Authentication

The backend requires authentication for all API access except the public endpoints listed below. Two mechanisms are used, selected by endpoint group:

| Endpoint group | Paths | Scheme |
|----------------|-------|--------|
| Admin / management | `/api/v1/**` (except `/api/v1/health`) | HTTP Basic |
| Runtime & simulation execution | `/api/v1/runtime/**`, `/api/v1/simulation-runs/**` | API key header |
| Public | `/api/v1/health`, static/frontend routes | none |
| Actuator | `/actuator/**` | ADMIN-role HTTP Basic |

Unauthenticated requests return **HTTP 401**; authenticated requests lacking permission return **HTTP 403**. Actuator endpoints (`/actuator/health`, `/actuator/info`, and any future `/actuator/**` exposure) return redacted health details unless authorised. Always use HTTPS in shared environments so HTTP Basic credentials and API keys are not exposed in transit.

### HTTP Basic (Admin APIs)

Set credentials under `security.admin` in `application-dev.yml`, or via the `ADMIN_USERNAME` / `ADMIN_PASSWORD` environment variables. Configure additional users under `security.users`.

### API Key (Runtime & Simulation Execution)

Runtime and simulation execution endpoints require an API key so they can be invoked from CLI tooling and automation.

- **Startup validation:** the application requires `api.auth.key` to be configured and non-empty. If it is missing or blank, the application fails to start with a clear error message.
- **Secure comparison:** API keys are compared using SHA-256 hashing with constant-time comparison to prevent timing attacks and credential leakage.

**Configuration:**

- `api.auth.key` (required): API key value. Must be non-empty and provided via environment variable or property file.
- `api.auth.header` (optional, default `X-API-Key`): header name to read the key from.

Generate a secure random key (any method with sufficient entropy is acceptable) and configure it via environment variable:

```bash
export API_KEY="$(openssl rand -base64 32)"
```

Or in `application-dev.yml`:

```yaml
api:
  auth:
    key: <your-generated-api-key>
    header: X-API-Key
```

**Example (simulation run):**

```bash
curl -H "X-API-Key: <your-api-key>" \
  -H "Content-Type: application/json" \
  -d '{"liftSystemId":1,"versionNumber":2,"scenarioId":3,"seed":12345}' \
  http://localhost:8080/api/v1/simulation-runs
```

**Example (runtime config):**

```bash
curl -H "X-API-Key: <your-api-key>" \
  http://localhost:8080/api/v1/runtime/systems/{systemKey}/config
```

The React admin UI sends both schemes from Vite environment variables when present; the backend ignores whichever header does not apply to a given endpoint, so a single client can send both. Because Vite inlines all `VITE_*` values into the browser bundle, frontend credentials are **not** a production security boundary — do not ship real admin passwords or API keys in a hosted SPA. Use a backend/session proxy or another server-side auth layer for hosted deployments. See [ADR-0018: API Key Authentication](decisions/0018-api-key-authentication-runtime-simulation.md).

## Role-Based Access Control

Admin APIs support role-based access control with two roles:

| Role | Access |
|------|--------|
| `ADMIN` | Read and write |
| `VIEWER` | Read-only |

Write methods (`POST`, `PUT`, `PATCH`, `DELETE`) require the `ADMIN` role; read methods (`GET`) are available to both roles. Configure multiple users under `security.users` in `application-dev.yml`. Keep credentials out of version control and prefer environment variables in production. See [ADR-0019: Spring Security Baseline](decisions/0019-spring-security-baseline.md), [ADR-0021: Role-Based Access Control](decisions/0021-role-based-access-control-rbac.md), and [ADR-0022: Explicit CORS and CSRF Policy](decisions/0022-explicit-cors-csrf-policy.md).

## Rate Limiting

The API is protected by a token-bucket rate limiter (Bucket4j) applied per client IP address. Separate limits apply to the admin and runtime/simulation-run API groups.

Default thresholds (configurable in `application.yml` or overridden per environment):

| API group | Default capacity | Refill |
|-----------|-----------------|--------|
| Admin (`/api/v1/**`) | 100 requests | 100 per 60 s |
| Runtime / simulation-runs | 1 000 requests | 1 000 per 60 s |

When the limit is exceeded the server responds with **HTTP 429 Too Many Requests** and includes:

- `Retry-After: <seconds>` — how long to wait before retrying
- `X-RateLimit-Limit: <capacity>` — total bucket capacity
- `X-RateLimit-Remaining: 0` — tokens remaining (always 0 on a 429)

**Configuration reference** (`application.yml`):

```yaml
rate-limiting:
  enabled: true               # set to false to disable entirely (e.g. in integration tests)
  trust-forwarded-for: false  # set to true only behind a trusted reverse proxy
  max-buckets-per-group: 10000 # caps retained client-IP buckets per API group
  admin:
    capacity: 100             # bucket capacity (max burst)
    refill-tokens: 100        # tokens added per period
    refill-period-seconds: 60
  runtime:
    capacity: 1000
    refill-tokens: 1000
    refill-period-seconds: 60
```

**`trust-forwarded-for`** — when `false` (the default), the rate-limiter uses `getRemoteAddr()` as the bucket key, which prevents callers from bypassing limits by spoofing `X-Forwarded-For`. Set it to `true` only when the service runs exclusively behind a trusted reverse proxy that controls this header. Override individual fields per Spring profile to tighten limits in production or loosen them for load testing.

## Request and Artefact Size Limits

The backend rejects oversized API request bodies before JSON deserialization and constrains Jackson JSON nesting/string sizes to reduce denial-of-service risk from very large or deeply nested payloads. By default, `/api/v1/**` and `/actuator/**` request bodies are capped at **1 MiB** and oversized requests receive **HTTP 413 Content Too Large**.

```yaml
request-size-limits:
  enabled: true
  max-body-bytes: 1048576
```

Simulation artefact reads are also bounded: full log reads and `results.json` parsing are capped at **1 MiB** each. Prefer the `tail` query parameter for large logs; `tail` accepts values from `1` to `10000`, rejects out-of-range values with HTTP 400, and tailed log reads remain capped by line count without materializing the entire file in memory.

## Response Conventions

Controllers return typed DTOs for JSON payloads rather than ad-hoc maps.

- **Create endpoints** return `201 Created` with a `Location` header pointing at the created resource (for example `Location: /api/v1/lift-systems/{id}`). The default CORS exposed-headers list includes `Location` so allowed browser clients can read it. Creating a simulation run also returns `201 Created` and starts execution asynchronously.
- **Deletes** return `204 No Content`.

### Pagination and Sorting

List endpoints that support paging (for example `GET /api/v1/simulation-runs`) return a Spring Data `Page` envelope and accept:

- `page` — 0-based page index
- `size` — page size (default `20`, maximum `100`)
- `sort` — one or more parameters in Spring Data format, for example `sort=createdAt,desc` or `sort=status,asc&sort=id,desc`

Sortable properties are allowlisted per endpoint. Unsupported properties or `ignorecase` sort modifiers return **HTTP 400 Bad Request** with the list of allowed properties.

## Error Response Structure

Error payloads use the shared `ErrorResponse` shape:

```json
{
  "status": 404,
  "message": "Lift system not found with id: 999",
  "timestamp": "2026-01-11T10:00:00Z"
}
```

Bean-validation failures use `ValidationErrorResponse`, where each `fieldErrors` entry is an array of messages so duplicate constraint failures are preserved:

```json
{
  "status": 400,
  "message": "Validation failed",
  "fieldErrors": {
    "size": ["must be less than or equal to 100"]
  },
  "timestamp": "2026-01-11T10:00:00Z"
}
```

Configuration- and scenario-validation endpoints return a typed validation response with `valid`, `errors`, and `warnings`. `errors` block the operation; `warnings` are non-blocking suggestions:

```json
{
  "valid": false,
  "errors": [
    {
      "field": "maxFloor",
      "message": "Maximum floor (0) must be greater than minimum floor (0)",
      "severity": "ERROR"
    }
  ],
  "warnings": []
}
```

The same `valid` / `errors` / `warnings` shape is returned when validation runs implicitly while creating, updating, or publishing a version (a `400 Bad Request` with the error detail).

### Common Status Codes

| Status | Meaning |
|--------|---------|
| `200 OK` | Successful read or update |
| `201 Created` | Resource created (includes `Location` header) |
| `204 No Content` | Successful delete |
| `400 Bad Request` | Validation error or malformed request |
| `401 Unauthorized` | Missing or invalid credentials |
| `403 Forbidden` | Authenticated but insufficient role |
| `404 Not Found` | Resource does not exist |
| `409 Conflict` | State conflict (e.g. already published, non-terminal run, duplicate name) |
| `413 Content Too Large` | Request body exceeds the configured size limit |
| `429 Too Many Requests` | Rate limit exceeded (includes `Retry-After`) |

## Schema Reference and Operational Details

The lift-configuration and scenario schemas (field constraints, validation rules, the floor-range migration note, and the batch-input-generator format), the run-artefact directory layout, and the `results.json` schema are documented in [Workflows and Troubleshooting](Workflows-and-Troubleshooting.md#configuration-and-scenario-schema-reference). Step-by-step CLI usage, UI-driven run workflows, artefact reproduction, and troubleshooting live in the same guide.
