# ADR-0020: URL-Based API Versioning with /api/v1 Prefix

**Date**: 2026-02-01

**Status**: Accepted

## Context

The Lift Simulator exposes REST APIs for lift system management, simulation execution, and configuration access. Until now, all API endpoints have been exposed under the `/api` prefix without explicit versioning (e.g., `/api/lift-systems`, `/api/scenarios`, `/api/runtime/systems/{key}/config`).

As the system matures and approaches production deployment, the need for explicit API versioning becomes critical to:

1. **Establish a stable contract**: Clients can rely on consistent API behavior within a major version
2. **Enable breaking changes**: Future API improvements can be introduced in v2, v3, etc. without disrupting existing clients
3. **Support multiple versions concurrently**: Legacy clients can continue using v1 while new clients adopt v2
4. **Communicate compatibility**: Version number signals expected behavior and breaking changes
5. **Facilitate deprecation**: Old API versions can be marked deprecated and eventually retired with clear migration paths

Without explicit versioning, any API change risks breaking existing integrations, complicating deployment and client compatibility.

## Decision

We will implement **URL-based API versioning** by introducing the `/api/v1` prefix for all REST endpoints.

### Versioning Strategy

All API endpoints will be prefixed with `/api/v{major}` where `{major}` is an integer representing the major version number. The initial version is `v1`.

**Before:**
```
GET  /api/lift-systems
POST /api/scenarios
GET  /api/runtime/systems/{key}/config
GET  /api/health
```

**After:**
```
GET  /api/v1/lift-systems
POST /api/v1/scenarios
GET  /api/v1/runtime/systems/{key}/config
GET  /api/v1/health
```

### Versioning Policy

1. **Major version changes** introduce breaking changes:
   - Removed endpoints
   - Renamed fields
   - Changed response structures
   - Modified authentication requirements
   - Incompatible behavior changes

2. **Within a major version**, changes must be backward compatible:
   - New optional fields
   - New endpoints
   - Additional query parameters (optional)
   - Extended enums (with fallback handling)

3. **Version lifecycle:**
   - Each major version is supported for a minimum transition period
   - Deprecated versions are clearly marked in documentation
   - Sunset timeline is communicated to clients

4. **Version selection:**
   - Version is specified in the URL path
   - No version negotiation via headers or query parameters
   - Clients explicitly choose the version they use

### Implementation

#### Backend Changes

All Spring Boot `@RestController` classes updated with versioned base paths:

```java
@RestController
@RequestMapping("/api/v1/lift-systems")
public class LiftSystemController { ... }

@RestController
@RequestMapping("/api/v1/scenarios")
public class ScenarioController { ... }

@RestController
@RequestMapping("/api/v1/runtime/systems")
public class RuntimeConfigController { ... }

@RestController
@RequestMapping("/api/v1")
public class HealthController {
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() { ... }
}
```

#### Security Configuration

Spring Security filter chains updated to protect versioned endpoints:

```java
// API key authentication for runtime and simulation APIs
@Bean
@Order(1)
public SecurityFilterChain apiKeySecurityFilterChain(HttpSecurity http) {
    http.securityMatcher(new OrRequestMatcher(
            new AntPathRequestMatcher("/api/v1/runtime/**"),
            new AntPathRequestMatcher("/api/v1/simulation-runs/**")
        ));
    // ...
}

// HTTP Basic authentication for admin APIs
@Bean
@Order(2)
public SecurityFilterChain adminApiSecurityFilterChain(HttpSecurity http) {
    http.securityMatcher("/api/v1/**")
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/health").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/v1/**").hasRole("ADMIN")
            // ...
        );
}
```

#### Frontend Changes

**API Client (`frontend/src/api/client.js`):**
```javascript
const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL || '/api/v1').trim() || '/api/v1';
```

**Vite Proxy (`frontend/vite.config.js`):**
```javascript
export default defineConfig({
  server: {
    proxy: {
      '/api/v1': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

#### Test Updates

All test files updated to use versioned endpoints:
- Backend controller tests (MockMvc)
- Integration tests (TestRestTemplate)
- Security configuration tests
- Frontend E2E tests (Playwright)

Example:
```java
mockMvc.perform(get("/api/v1/lift-systems")
    .with(httpBasic("admin", "password")))
    .andExpect(status().isOk());
```

## Consequences

### Positive

1. **Clear API contract**: Clients know exactly which API version they're using
2. **Safe evolution**: Breaking changes can be introduced in v2 without affecting v1 clients
3. **Gradual migration**: Clients can migrate to new versions at their own pace
4. **Explicit compatibility**: Version number communicates expected behavior
5. **Industry standard**: URL-based versioning is widely adopted and well-understood
6. **Simple implementation**: Version prefix in URL path is straightforward to implement and test
7. **Tooling compatibility**: Works seamlessly with API gateways, proxies, and monitoring tools

### Negative

1. **URL length**: Endpoints are slightly longer (adds 3 characters: `/v1`)
2. **Code duplication**: Future v2 may require duplicating controller classes (can be mitigated with shared service layer)
3. **Migration effort**: Existing clients must update their base URL (one-time change)
4. **Version proliferation**: Must manage multiple API versions concurrently during transition periods

### Neutral

1. **Version maintenance**: Requires discipline to maintain backward compatibility within a version
2. **Documentation overhead**: Each version needs clear documentation and migration guides
3. **Deprecation process**: Requires planning and communication when retiring old versions

## Alternatives Considered

### 1. Header-Based Versioning

**Example:** `Accept: application/vnd.lift-simulator.v1+json`

**Pros:**
- Cleaner URLs
- RESTful content negotiation

**Cons:**
- Hidden from URL (harder to debug, test, and document)
- Not supported by all HTTP clients
- More complex to implement in proxies and caches
- Less discoverable for API consumers

**Decision:** Rejected. URL-based versioning is more transparent and easier to use.

### 2. Query Parameter Versioning

**Example:** `/api/lift-systems?version=1`

**Pros:**
- Flexible (can specify version per request)
- Doesn't change URL structure

**Cons:**
- Inconsistent REST semantics
- Easy to forget or omit
- Complicates caching and routing
- Non-standard approach

**Decision:** Rejected. Not a common or recommended pattern for API versioning.

### 3. No Versioning

**Example:** Keep `/api` prefix without version

**Pros:**
- No changes required
- Simpler URLs

**Cons:**
- Breaking changes disrupt all clients
- No graceful migration path
- Difficult to maintain backward compatibility
- Not suitable for production APIs

**Decision:** Rejected. Versioning is essential for API stability and evolution.

### 4. Subdomain Versioning

**Example:** `v1.api.liftsimulator.com/lift-systems`

**Pros:**
- Complete isolation of versions
- Can deploy different versions independently

**Cons:**
- Requires DNS and SSL certificate management
- Overkill for self-hosted deployments
- Complicates local development
- More infrastructure overhead

**Decision:** Rejected. Too complex for the current deployment model.

## Future Considerations

1. **API v2 Planning**: 
   - Consider GraphQL for complex query requirements
   - Evaluate batching and field selection capabilities
   - Plan breaking changes in advance

2. **Version Sunset Policy**:
   - Define minimum support period (e.g., 12 months after v2 release)
   - Establish deprecation warnings in API responses
   - Create migration guides and tooling

3. **Automated Testing**:
   - Contract testing to ensure backward compatibility
   - Version compatibility matrix testing
   - API changelog generation

4. **Documentation**:
   - Maintain separate OpenAPI/Swagger specs per version
   - Version-specific documentation sites
   - Migration guides for each major version

5. **Monitoring**:
   - Track version usage in production
   - Alert on deprecated version usage
   - Measure migration progress

## References

- [Semantic Versioning](https://semver.org/)
- [Microsoft REST API Guidelines - Versioning](https://github.com/microsoft/api-guidelines/blob/vNext/Guidelines.md#12-versioning)
- [Stripe API Versioning](https://stripe.com/docs/api/versioning)
- [GitHub API Versioning](https://docs.github.com/en/rest/overview/api-versions)
- [REST API Versioning Best Practices](https://www.freecodecamp.org/news/rest-api-versioning-best-practices/)
- [Spring Boot REST API Versioning](https://www.baeldung.com/rest-versioning)
