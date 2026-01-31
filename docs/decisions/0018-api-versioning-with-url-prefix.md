# ADR-0018: API Versioning with URL Prefix

## Status

Accepted

## Context

As the Lift Simulator project evolves, the need for a stable, versioned API becomes critical to:

1. **Maintain backwards compatibility**: Allow existing clients to continue functioning while new features are added
2. **Enable breaking changes**: Provide a path for necessary API changes without disrupting existing integrations
3. **Support multiple API versions**: Allow gradual migration of clients from old to new API versions
4. **Improve API contract clarity**: Make it explicit which API version clients are using

Without API versioning, any breaking change to endpoints, request/response formats, or behavior would immediately break all existing clients. This becomes increasingly risky as the application matures and gains more users.

### Current State

Prior to this ADR, all API endpoints were available under the `/api` prefix:
- `/api/lift-systems`
- `/api/scenarios`
- `/api/simulation-runs`
- `/api/health`
- etc.

This approach provides no versioning mechanism, making it impossible to introduce breaking changes safely.

### Versioning Strategies Considered

Several API versioning strategies were evaluated:

1. **URL Path Versioning** (e.g., `/api/v1/resource`)
   - Pros: Explicit, simple, works with all HTTP clients, cacheable, easy to route
   - Cons: URLs change between versions, requires more routing configuration

2. **Query Parameter Versioning** (e.g., `/api/resource?version=1`)
   - Pros: URL stays consistent, easy to add
   - Cons: Easy to forget, doesn't work well with caching, less visible

3. **Header Versioning** (e.g., `Accept: application/vnd.liftsimulator.v1+json`)
   - Pros: Clean URLs, follows REST principles
   - Cons: Invisible to casual users, harder to test with browsers, requires custom headers

4. **Content Negotiation** (e.g., `Accept: application/vnd.liftsimulator+json; version=1`)
   - Pros: RESTful, flexible
   - Cons: Complex to implement, harder to understand and debug

5. **No Versioning** (rely on backwards compatibility)
   - Pros: Simple, no version management
   - Cons: Forces permanent backwards compatibility or breaking changes for all clients

## Decision

We will implement **URL path versioning** using the `/api/v1` prefix for all API endpoints.

### Implementation

All REST controllers will use versioned URL mappings:

```java
@RestController
@RequestMapping("/api/v1/scenarios")
public class ScenarioController { ... }
```

The current API version (v1) includes:
- `/api/v1/health` - Health check endpoint
- `/api/v1/lift-systems` - Lift system management
- `/api/v1/lift-systems/{id}/versions` - Version management
- `/api/v1/scenarios` - Scenario management
- `/api/v1/simulation-runs` - Simulation run lifecycle
- `/api/v1/config/validate` - Configuration validation
- `/api/v1/runtime/systems` - Runtime configuration access

The frontend API client will be updated to use the versioned base URL:

```javascript
const apiBaseUrl = '/api/v1';
```

### Version Lifecycle Policy

1. **Version Support**: Each major version will be supported for a minimum of 6 months after the next version is released
2. **Deprecation**: Deprecated APIs will include warnings in response headers and documentation
3. **Breaking Changes**: Only allowed in new major versions (v1 → v2)
4. **Non-Breaking Changes**: Can be added to existing versions (new optional fields, new endpoints)
5. **Bug Fixes**: Applied to all supported versions

### Migration Path

For this initial versioning implementation:
1. All existing `/api` endpoints are migrated to `/api/v1`
2. No `/api` legacy support is maintained (as this is the first versioning)
3. All tests, documentation, and client code updated to use `/api/v1`

For future version upgrades:
1. New version endpoints created (e.g., `/api/v2`)
2. Old version marked as deprecated in documentation
3. Minimum support period maintained before removal
4. Migration guide provided for breaking changes

## Consequences

### Positive

1. **Clear API Versioning**: Version is immediately visible in the URL
2. **Flexible Evolution**: Can introduce breaking changes in v2, v3, etc. without affecting v1 clients
3. **Easy Testing**: Can test different versions using simple URL changes
4. **Browser-Friendly**: Works with all HTTP tools and browsers without special headers
5. **Explicit Contracts**: Each version has a clear, explicit API contract
6. **Gradual Migration**: Clients can migrate at their own pace
7. **Better Caching**: Different versions can have different cache strategies

### Negative

1. **URL Changes**: All endpoints now have a version prefix (mitigated by using environment variable for base URL)
2. **Code Duplication**: Future versions may duplicate controller code (can be mitigated with shared services)
3. **Routing Complexity**: More routes to manage as versions increase
4. **Documentation Overhead**: Each version needs separate documentation

### Neutral

1. **Test Updates**: All integration and E2E tests require updates to use `/api/v1`
2. **Documentation Updates**: README, API docs, and ADRs need version prefix
3. **Breaking Change**: This is a breaking change for any existing external clients (acceptable for pre-1.0 software)

## Implementation Notes

### Frontend Configuration

The frontend API client supports environment variable override:
```bash
VITE_API_BASE_URL=/api/v1
```

This allows flexibility for:
- Testing different API versions
- Pointing to different backends
- Proxy configurations

### Testing

All tests updated to use versioned endpoints:
- Java integration tests: Updated MockMvc paths
- Playwright E2E tests: Updated API call URLs
- Test helpers: Updated health check endpoints

### Documentation

The following documentation requires updates:
- [x] Controller Java class `@RequestMapping` annotations
- [x] Controller method documentation comments
- [x] Integration test endpoints
- [x] E2E test endpoints
- [ ] README API endpoint examples
- [ ] CHANGELOG entry for v0.46.0
- [x] This ADR

## Related Decisions

- This decision builds upon ADR-0006 (Spring Boot Admin Backend) by adding versioning to the REST API
- Future ADR may address specific v1 → v2 migration scenarios if breaking changes are needed

## References

- [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
- [Semantic Versioning](https://semver.org/spec/v2.0.0.html)
- [REST API Versioning Strategies](https://restfulapi.net/versioning/)
- [Microsoft REST API Guidelines - Versioning](https://github.com/microsoft/api-guidelines/blob/vNext/Guidelines.md#12-versioning)
