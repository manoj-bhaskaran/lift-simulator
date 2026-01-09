# ADR-0006: Spring Boot Admin Backend

**Date**: 2026-01-09

**Status**: Accepted

## Context

The lift simulator has been operating as a command-line application for simulating and testing lift controller algorithms. As the project matures, there is a need to provide a graphical admin UI for managing configurations, running simulations, and viewing results. To support this, we need a backend service that can:

1. Provide RESTful APIs for the admin UI to interact with
2. Manage lift simulator configurations
3. Run simulations on demand
4. Store and retrieve simulation results
5. Be easily deployable and maintainable
6. Support cross-platform usage (Windows, Linux, macOS)

We evaluated several options for building this backend:

### Option 1: Plain Java with embedded web server (Jetty/Undertow)
- **Pros**: Lightweight, minimal dependencies, full control
- **Cons**: Requires significant boilerplate code for REST endpoints, JSON serialization, configuration management, logging, health checks, etc.

### Option 2: JAX-RS (Jersey) with manual setup
- **Pros**: Standard Java REST API, good documentation
- **Cons**: Still requires manual setup for many cross-cutting concerns (dependency injection, configuration, health monitoring)

### Option 3: Spring Boot
- **Pros**:
  - Industry-standard framework with extensive ecosystem
  - Production-ready features out-of-the-box (health checks, metrics, logging)
  - Minimal boilerplate with auto-configuration
  - Strong dependency injection and configuration management
  - Large community and extensive documentation
  - Easy integration with future features (database, security, scheduling)
- **Cons**:
  - Adds framework dependency and larger JAR size
  - Learning curve for developers unfamiliar with Spring

## Decision

We will use **Spring Boot 3.2.1** as the foundation for the Lift Config Service backend.

### Implementation Details

1. **Framework**: Spring Boot 3.2.1 (requires Java 17+)
2. **Build System**: Continue using Maven, inherit from `spring-boot-starter-parent`
3. **Package Structure**: Create `admin` package hierarchy separate from simulation core:
   - `admin/controller` - REST controllers
   - `admin/service` - Business logic services
   - `admin/repository` - Data access layer
   - `admin/domain` - Backend-specific domain models
   - `admin/dto` - Data transfer objects
4. **Initial Features**:
   - Custom health endpoint at `/api/health`
   - Spring Boot Actuator endpoints (`/actuator/health`, `/actuator/info`)
   - Application configuration via `application.properties`
   - Logging configuration with DEBUG level for lift simulator package
5. **Port**: Default to 8080, configurable via properties

### Separation of Concerns

The existing lift simulator core (`domain/`, `engine/`, `scenario/`) remains completely independent. The Spring Boot backend is an optional layer that:
- Lives in a separate `admin` package hierarchy
- Does not modify existing simulation code
- Can be disabled if only CLI usage is needed
- Provides a REST API facade over the simulation engine

This separation ensures:
- CLI functionality continues to work independently
- Simulation tests remain unaffected
- No coupling between simulation logic and web framework
- Future ability to run simulations without Spring Boot if needed

## Consequences

### Positive

1. **Rapid Development**: Spring Boot's auto-configuration and starter dependencies enable rapid feature development
2. **Production-Ready**: Built-in health checks, metrics, and monitoring support production deployments
3. **Standard Patterns**: Spring's dependency injection and MVC patterns provide consistent architecture
4. **Future Extensibility**: Easy to add:
   - Database integration (Spring Data)
   - Authentication/Authorization (Spring Security)
   - Scheduled jobs (Spring Scheduling)
   - WebSocket support for real-time updates
   - API documentation (Swagger/OpenAPI)
5. **Testing Support**: Spring Boot Test provides excellent testing utilities for integration tests
6. **Cross-Platform**: Embedded Tomcat server works consistently across Windows, Linux, and macOS

### Negative

1. **JAR Size**: Spring Boot adds ~25-30 MB to the final JAR (acceptable for modern deployments)
2. **Startup Time**: Spring Boot adds ~2-3 seconds to startup time (negligible for backend services)
3. **Dependency Updates**: Need to keep Spring Boot version updated for security patches
4. **Framework Lock-in**: Replacing Spring Boot later would require significant refactoring

### Neutral

1. **Learning Curve**: Team needs Spring Boot knowledge, but framework is well-documented
2. **Two Entry Points**: System now has two main classes:
   - `Main.java` - CLI simulation runner (original)
   - `LiftConfigServiceApplication.java` - Spring Boot backend (new)

## Alternatives Considered

### 1. Micronaut
- **Why not chosen**: Spring Boot has larger ecosystem and more mature tooling
- **When to reconsider**: If startup time becomes critical (Micronaut starts faster)

### 2. Quarkus
- **Why not chosen**: Overkill for current needs, primarily targets cloud-native/GraalVM use cases
- **When to reconsider**: If we need native compilation or Kubernetes optimization

### 3. Dropwizard
- **Why not chosen**: Less opinionated than Spring Boot, requires more manual configuration
- **When to reconsider**: If we need more control over component selection

## Implementation Notes

1. **Backward Compatibility**: All existing CLI functionality remains unchanged
2. **Package Isolation**: Admin backend lives in `com.liftsimulator.admin` package
3. **Configuration**: `application.properties` for backend settings, simulation still uses builder pattern
4. **Health Checks**: Both custom (`/api/health`) and actuator (`/actuator/health`) endpoints provided
5. **Logging**: Separate logging configuration for web requests vs simulation logic

## References

- Spring Boot Documentation: https://docs.spring.io/spring-boot/docs/3.2.1/reference/html/
- Spring Boot Actuator: https://docs.spring.io/spring-boot/docs/3.2.1/reference/html/actuator.html
- ADR-0001: Tick-Based Simulation (simulation core design)
- ADR-0002: Single Source of Truth for Lift State (state management)
- ADR-0003: Request Lifecycle Management (request handling)
