# ADR-0019: Spring Security Baseline for API Authentication

**Date**: 2026-01-31

**Status**: Accepted

## Context

The Lift Simulator backend exposes REST APIs for managing lift system configurations, running simulations, and retrieving results. Prior to this decision, all API endpoints were publicly accessible without authentication, relying solely on network isolation for security.

As the system approaches production readiness, authentication becomes a critical requirement to:

1. **Protect administrative operations**: Creating, modifying, and deleting lift systems, versions, and scenarios should be restricted to authorized users
2. **Secure runtime configuration access**: External systems (e.g., lift controllers) retrieving published configurations should authenticate to prevent unauthorized access
3. **Enable audit trails**: Knowing who performed actions is essential for debugging and compliance
4. **Prepare for multi-tenancy**: Future expansion may require per-user or per-organization access control

The system has two distinct API categories with different security requirements:

- **Admin APIs** (`/api/**`): Used by human operators via the web UI for configuration management
- **Runtime APIs** (`/api/runtime/**`): Used by machine clients (lift controllers, external systems) for read-only configuration access

## Decision

We will implement **Spring Security baseline authentication** with two authentication mechanisms:

### 1. HTTP Basic Authentication for Admin APIs

Admin APIs use HTTP Basic authentication for simplicity and compatibility with the browser-based frontend.

**Rationale:**
- Simple to implement and widely supported
- Browser can handle authentication prompts natively
- Easy to test with curl and API clients
- Stateless (no session management complexity)
- Credentials configurable via environment variables for deployment flexibility

**Configuration:**
```java
http
    .securityMatcher("/api/**")
    .httpBasic(Customizer.withDefaults())
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/health").permitAll()
        .requestMatchers("/api/**").hasRole("ADMIN"));
```

### 2. API Key Authentication for Runtime APIs

Runtime APIs use API key authentication via the `X-API-Key` header.

**Rationale:**
- Better suited for machine-to-machine communication
- No credential encoding overhead (unlike Basic auth's base64)
- Separate authentication from admin users
- Easy to rotate without affecting admin credentials
- Common pattern for API integrations

**Configuration:**
```java
http
    .securityMatcher("/api/runtime/**")
    .addFilterBefore(
        new ApiKeyAuthenticationFilter(apiKey, authenticationEntryPoint),
        UsernamePasswordAuthenticationFilter.class)
    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
```

### 3. Public Endpoints

The following endpoints remain accessible without authentication:
- `/api/health` - Health check for monitoring
- `/actuator/**` - Spring Boot Actuator endpoints
- Static assets and frontend routes

### 4. Error Handling

Authentication failures return HTTP 401 with consistent JSON error responses:

```json
{
  "status": 401,
  "message": "Authentication required",
  "timestamp": "2026-01-31T12:00:00Z"
}
```

This matches the application's existing `ErrorResponse` format for consistency.

## Implementation Details

### Security Filter Chains

Three ordered security filter chains handle different request patterns:

| Order | Matcher | Authentication | Role |
|-------|---------|----------------|------|
| 1 | `/api/runtime/**` | API Key | RUNTIME |
| 2 | `/api/**` | HTTP Basic | ADMIN |
| 3 | Default | None | N/A |

### Configuration Properties

```properties
# Admin credentials (HTTP Basic)
security.admin.username=${ADMIN_USERNAME:admin}
security.admin.password=${ADMIN_PASSWORD:}

# Runtime API key
security.api-key=${API_KEY:}
```

### Key Classes

1. **SecurityConfig** (`com.liftsimulator.admin.config.SecurityConfig`)
   - Configures three `SecurityFilterChain` beans with ordering
   - Defines `UserDetailsService` with in-memory admin user
   - Uses `BCryptPasswordEncoder` for password hashing

2. **ApiKeyAuthenticationFilter** (`com.liftsimulator.admin.config.ApiKeyAuthenticationFilter`)
   - Extracts and validates `X-API-Key` header
   - Creates `UsernamePasswordAuthenticationToken` with RUNTIME role
   - Delegates to entry point on failure

3. **CustomAuthenticationEntryPoint** (`com.liftsimulator.admin.config.CustomAuthenticationEntryPoint`)
   - Returns JSON error response on authentication failure
   - Logs authentication failures for monitoring

## Consequences

### Positive

1. **Security posture improved**: All APIs now require authentication by default
2. **Flexible configuration**: Credentials configurable via environment variables
3. **Clear separation**: Different auth mechanisms for admin vs. runtime access
4. **Consistent errors**: Authentication failures return standard JSON format
5. **Stateless design**: No server-side sessions to manage
6. **Test compatibility**: Easy to test with `@WithMockUser` and `httpBasic()` helpers

### Negative

1. **Breaking change**: Existing clients must update to provide credentials
2. **Deployment complexity**: Credentials must be provisioned during deployment
3. **Password management**: Admin password in memory (not suitable for multi-admin production)
4. **No token refresh**: Basic auth sends credentials on every request
5. **HTTPS requirement**: Basic auth credentials are only base64-encoded, requiring HTTPS in production

### Neutral

1. **CSRF disabled**: Appropriate for stateless REST APIs, may need review for browser forms
2. **In-memory user store**: Sufficient for single-admin scenarios, not for multi-user production
3. **No rate limiting**: Should be added separately for production

## Alternatives Considered

### 1. JWT (JSON Web Tokens)

**Pros:**
- Token-based, reduces credential transmission
- Self-contained (can include user claims)
- Better for distributed systems

**Cons:**
- More complex to implement
- Requires token refresh mechanism
- Overkill for single-admin local deployment

**Decision**: Deferred. JWT may be added later for multi-user scenarios.

### 2. OAuth2/OpenID Connect

**Pros:**
- Industry standard for authorization
- Supports SSO and external identity providers
- Rich ecosystem of libraries

**Cons:**
- Significant implementation complexity
- Requires external authorization server
- Overkill for local/self-hosted deployment

**Decision**: Not suitable for current use case.

### 3. No Authentication (Network Isolation Only)

**Pros:**
- Simplest implementation
- No credential management

**Cons:**
- Assumes network is trusted
- No audit trail
- Not suitable for any external exposure

**Decision**: Rejected. Authentication is a prerequisite for production readiness.

### 4. Session-Based Authentication

**Pros:**
- Traditional web application pattern
- Session invalidation possible

**Cons:**
- Requires session storage
- More complex for stateless APIs
- CSRF protection complexity

**Decision**: Rejected. Stateless design preferred for REST APIs.

## Future Considerations

1. **Multi-user support**: Replace in-memory store with database-backed users
2. **JWT tokens**: Add JWT support for better frontend integration
3. **Role hierarchy**: More granular roles (VIEWER, EDITOR, ADMIN)
4. **API key management**: UI for generating/revoking runtime API keys
5. **Audit logging**: Track authentication events and API access
6. **Rate limiting**: Protect against brute force and abuse
7. **HTTPS enforcement**: Redirect HTTP to HTTPS in production

## References

- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [Spring Security Filter Chain Ordering](https://docs.spring.io/spring-security/reference/servlet/architecture.html)
- [HTTP Basic Authentication RFC 7617](https://tools.ietf.org/html/rfc7617)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [API Key Best Practices](https://cloud.google.com/docs/authentication/api-keys)
