# ADR-0021: Role-Based Access Control (RBAC) for Admin Operations

**Date**: 2026-02-01

**Status**: Accepted

## Context

Following the implementation of Spring Security baseline authentication (ADR-0019), all admin API endpoints required the ADMIN role for any operation. This "all-or-nothing" approach worked for single-admin deployments but became limiting as the system matured:

1. **Read-only access needs**: Some users need to view configurations and simulation results without modification privileges
2. **Separation of duties**: Production environments benefit from separating who can view vs. who can modify
3. **Audit requirements**: Different roles enable better tracking of who performed which actions
4. **Local development**: Developers testing with different permission levels need configurable users

ADR-0019 identified "Role hierarchy (VIEWER, EDITOR, ADMIN)" as a future consideration. This decision implements that enhancement.

## Decision

We will implement **role-based access control (RBAC)** with two roles for admin operations:

### 1. Role Definitions

| Role | Description | Permissions |
|------|-------------|-------------|
| **ADMIN** | Full administrative access | Read and write all resources |
| **VIEWER** | Read-only access | View configurations, scenarios, and simulation results |

### 2. Authorization Rules

Authorization is based on HTTP method:

| HTTP Method | Required Role | Description |
|-------------|---------------|-------------|
| GET | ADMIN or VIEWER | Read operations allowed for both roles |
| POST | ADMIN only | Create operations restricted to admins |
| PUT | ADMIN only | Update operations restricted to admins |
| DELETE | ADMIN only | Delete operations restricted to admins |
| PATCH | ADMIN only | Partial update operations restricted to admins |

### 3. Error Handling

Unauthorized access (authenticated user lacking permission) returns HTTP 403 Forbidden:

```json
{
  "status": 403,
  "message": "Access denied",
  "timestamp": "2026-02-01T12:00:00Z"
}
```

This is distinct from HTTP 401 Unauthorized (authentication failure), following RFC 7231 semantics.

### 4. Multi-User Configuration

Users can now be configured with different roles via `security.users`:

```yaml
security:
  users:
    - username: admin
      password: adminpassword
      role: ADMIN
    - username: viewer
      password: viewerpassword
      role: VIEWER
```

The legacy single-user configuration (`security.admin.*`) remains supported for backward compatibility.

## Implementation Details

### Security Configuration

The `SecurityConfig` class is updated to apply role-based authorization:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/v1/health").permitAll()
    // Read operations allowed for both ADMIN and VIEWER
    .requestMatchers(HttpMethod.GET, "/api/v1/**").hasAnyRole("ADMIN", "VIEWER")
    // Write operations restricted to ADMIN only
    .requestMatchers(HttpMethod.POST, "/api/v1/**").hasRole("ADMIN")
    .requestMatchers(HttpMethod.PUT, "/api/v1/**").hasRole("ADMIN")
    .requestMatchers(HttpMethod.DELETE, "/api/v1/**").hasRole("ADMIN")
    .requestMatchers(HttpMethod.PATCH, "/api/v1/**").hasRole("ADMIN")
    .anyRequest().authenticated())
```

### Key Classes

1. **SecurityConfig** (`com.liftsimulator.admin.config.SecurityConfig`)
   - Updated to support VIEWER and ADMIN roles
   - Multi-user support via `SecurityUsersProperties`
   - Backward compatible with legacy single-admin configuration

2. **SecurityUsersProperties** (`com.liftsimulator.admin.config.SecurityUsersProperties`)
   - New configuration properties class for multi-user setup
   - Supports configuring username, password, and role per user

3. **CustomAccessDeniedHandler** (`com.liftsimulator.admin.config.CustomAccessDeniedHandler`)
   - New handler for HTTP 403 responses
   - Returns consistent JSON error format matching other error responses

### Configuration Properties

```properties
# Multi-user configuration (new)
security.users[0].username=admin
security.users[0].password=adminpassword
security.users[0].role=ADMIN
security.users[1].username=viewer
security.users[1].password=viewerpassword
security.users[1].role=VIEWER

# Legacy single-admin configuration (still supported)
security.admin.username=${ADMIN_USERNAME:admin}
security.admin.password=${ADMIN_PASSWORD:}
```

## Consequences

### Positive

1. **Granular access control**: Different users can have different permission levels
2. **Read-only access**: Viewers can safely browse without accidental modifications
3. **Backward compatible**: Existing single-admin configurations continue to work
4. **Configurable**: Easy to set up different users for development and testing
5. **Clear 403 responses**: Authenticated users get informative feedback when lacking permission
6. **Audit-ready**: Role differentiation enables meaningful access logging

### Negative

1. **Increased complexity**: More configuration options to understand
2. **No fine-grained permissions**: Cannot restrict specific resources (e.g., read-only for certain systems)
3. **In-memory only**: Users still configured in memory, not database-backed

### Neutral

1. **Method-based authorization**: Simple and clear, but less flexible than annotation-based
2. **Two roles only**: Sufficient for current needs; EDITOR role can be added later if needed

## Alternatives Considered

### 1. Method-Level Security with @PreAuthorize

**Pros:**
- Fine-grained control per endpoint
- Self-documenting in controller code

**Cons:**
- Scattered authorization logic
- Harder to audit/review
- Method-level annotations verbose

**Decision**: Centralized URL-based authorization preferred for consistency and easier auditing.

### 2. Resource-Based Authorization

**Pros:**
- Control access per lift system or scenario
- Multi-tenant ready

**Cons:**
- Significant implementation complexity
- Requires ownership tracking in database
- Overkill for current single-tenant use

**Decision**: Deferred. Current coarse-grained roles sufficient; can add resource-level control later.

### 3. Three-Role Hierarchy (ADMIN, EDITOR, VIEWER)

**Pros:**
- EDITOR could create/update but not delete
- More nuanced permission levels

**Cons:**
- Adds complexity without clear current need
- Delete vs. update distinction rarely needed

**Decision**: Start with two roles; add EDITOR if use case emerges.

## Future Considerations

1. **Database-backed users**: Replace in-memory store with persistent user management
2. **EDITOR role**: Add intermediate role if needed (create/update but not delete)
3. **Resource-level permissions**: Control access per lift system or scenario
4. **Permission groups**: Group permissions for easier management
5. **Audit logging**: Log role-based access decisions with username and role

## Related Decisions

- [ADR-0019: Spring Security Baseline](./0019-spring-security-baseline.md) - Foundation for authentication
- [ADR-0018: API Key Authentication](./0018-api-key-authentication-runtime-simulation.md) - Runtime API authentication

## References

- [Spring Security Authorization](https://docs.spring.io/spring-security/reference/servlet/authorization/index.html)
- [HTTP 403 vs 401](https://stackoverflow.com/questions/3297048/403-forbidden-vs-401-unauthorized-http-responses)
- [OWASP Access Control Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Access_Control_Cheat_Sheet.html)
- [RBAC Overview (NIST)](https://csrc.nist.gov/projects/role-based-access-control)
