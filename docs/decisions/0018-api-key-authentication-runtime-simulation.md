# ADR-0018: API Key Authentication for Runtime & Simulation Execution

**Date**: 2026-02-15

**Status**: Accepted

## Context

Runtime configuration endpoints and simulation execution APIs are designed for non-interactive use
cases (CLI tools, automation, and backend-to-backend integrations). These endpoints previously
accepted unauthenticated requests, which is incompatible with the security posture expected for
production runtime consumption and automated execution.

We need a lightweight authentication mechanism that:
- Works for CLI and automation use cases.
- Does not require full user/session management.
- Can be configured via application config or environment variables.
- Avoids logging or leaking secrets.

## Decision

Introduce API key authentication for:
- Runtime configuration APIs (`/api/runtime/**`)
- Simulation execution APIs (`/api/simulation-runs/**`)

Key characteristics:
- API key is configured via `api.auth.key` (with environment variable support).
- Header name is configurable via `api.auth.header` (default: `X-API-Key`).
- Missing or invalid keys return HTTP 401.
- The key value is never logged.

## Consequences

- Runtime and simulation execution clients must include the configured API key header.
- Automated tooling can authenticate without interactive login flows.
- Admin endpoints remain unaffected, preserving existing admin workflows.
