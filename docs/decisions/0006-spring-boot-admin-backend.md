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

We evaluated three main approaches for building the admin UI and backend:

### Approach 1: Desktop Admin App (Java GUI) + Embedded/Local Storage

#### Option 1A: JavaFX Desktop App + Embedded DB (SQLite/H2)
- **Description**: JavaFX-based desktop application with forms, tables, and wizards, using SQLite or H2 for local file-based storage
- **Pros**:
  - Best fit for a single installable tool
  - Works fully offline
  - Easy to ship "one configuration file + DB file"
  - Good developer ergonomics once screens and bindings are set up
  - Modern UI compared to Swing
- **Cons**:
  - JavaFX UI takes significant time to design well (form validation, UX polish)
  - Multi-user and remote access is awkward
  - Switching from any existing GUI framework to JavaFX is a learning curve
  - Limited to single-user scenarios
- **When to use**: When you want a local "Admin Console" and don't need multiple users or remote access

#### Option 1B: Swing Desktop App (NetBeans GUI Builder) + SQLite/H2
- **Description**: Swing-based desktop application using NetBeans GUI Builder for rapid form development
- **Pros**:
  - Familiar ecosystem if team already knows Swing
  - NetBeans GUI builder speeds up basic CRUD screens
  - Mature tooling and libraries
- **Cons**:
  - Swing UIs can feel dated unless significant time is spent on look-and-feel
  - More boilerplate for modern UX patterns
  - Limited web/remote capabilities
- **When to use**: When you want to move fast with familiar tools and don't need web access

**Why not chosen for this project**: Desktop apps limit multi-user scenarios, remote access, and future deployment flexibility. The lift simulator is likely to be used in environments where web access is preferred (labs, remote servers, cloud deployments).

---

### Approach 2: Web Admin App (Recommended Long-term) with REST Backend + DB

#### Option 2A: Spring Boot Backend + React/Vue Frontend (SELECTED)
- **Description**: Spring Boot REST API backend with a modern JavaScript SPA framework (React or Vue) as a separate frontend application
- **Pros**:
  - **Clean separation**: Simulator core vs config service remain decoupled
  - **Future extensibility**: Easy to add users, roles, audit trail, config history, approvals
  - **Better UX**: Modern web UI with wizards, grids, validation, search
  - **Flexible deployment**: Can run locally or deploy anywhere (cloud, on-premise)
  - **Multi-user ready**: Built for concurrent access from the start
  - **API-first design**: REST API can be consumed by other tools, scripts, CI/CD
  - **Cross-platform**: Works on any OS with a browser
- **Cons**:
  - More moving parts (frontend build, API, CORS configuration)
  - Learning curve if team is new to frontend frameworks
  - Requires managing two separate codebases (backend + frontend)
- **When to use**: When you want a proper admin portal with future expansion capabilities
- **Why chosen**: Best long-term solution for a professional admin UI that supports multiple users, remote access, and future features like authentication, audit trails, and integrations

#### Option 2B: Spring Boot + Server-Rendered UI (Thymeleaf)
- **Description**: Spring Boot with server-side rendering using Thymeleaf templates
- **Pros**:
  - One codebase, simpler deployment than SPA
  - Still web-based with decent forms
  - Less JavaScript complexity
  - Faster initial page loads
- **Cons**:
  - UX is less dynamic than a SPA unless significant JavaScript is added anyway
  - Still need to build form-heavy pages carefully
  - Less modern feel compared to SPA
- **When to use**: When you want web benefits but prefer staying mostly in Java
- **Why not chosen**: Modern admin UIs benefit greatly from dynamic interactions (live validation, drag-and-drop, real-time updates), which are easier with SPA frameworks

---

### Approach 3: Configuration-First (File-Based) + Optional UI Editor

#### Option 3A: JSON/YAML Configs Stored as Files (+ UI to Edit Them)
- **Description**: Store configurations as JSON/YAML files in a folder, with an optional UI tool to edit them
- **Pros**:
  - **Extremely portable**: Configs can be committed to Git
  - **Easy to load**: Simulator can use `--config path/to/config.json`
  - **Perfect for version control**: Reviewable, diff-able, shareable
  - **Simple deployment**: No database setup required
  - **Git-friendly workflow**: Changes tracked in version control
- **Cons**:
  - Searching, history, and multi-user editing is harder without a DB
  - Requires discipline about schema versioning and migrations
  - Concurrent edits can cause merge conflicts
  - No built-in audit trail or approval workflow
- **When to use**: When configs need to be shareable, reviewable, and version-controlled
- **Why not chosen**: While file-based configs are valuable, they don't support the full admin UI requirements (user management, audit trails, search, history). However, a **practical hybrid approach** is planned:
  - **DB stores metadata + versions**
  - **Actual config stored as JSON in DB OR as file + DB pointer**
  - This gives "best of both worlds": structured queries + portable configs

---

### Storage Choices and Trade-offs

We also evaluated different persistence options:

#### 1. SQLite (Single File DB)
- **Pros**: Simplest persistent DB, zero server, good enough for most admin tools
- **Cons**: Concurrency limits for many users; not ideal for central multi-user usage
- **Decision**: Good for Phase 1 (single-user scenarios), may upgrade later

#### 2. H2 (Embedded Java DB)
- **Pros**: Java-friendly, embedded, great for dev/test, in-memory mode available
- **Cons**: Less "standard" than SQLite, fewer external tools
- **Decision**: Good for development and testing

#### 3. PostgreSQL (Server DB)
- **Pros**: Robust, multi-user, great tooling, scalable, production-ready
- **Cons**: Requires running a DB service (more complex setup)
- **Decision**: Target for production deployments, defer to Phase 2

#### 4. Pure File Storage (JSON/YAML)
- **Pros**: Portable, Git-friendly, easy export/import
- **Cons**: Harder for querying, audit trails, concurrent edits
- **Decision**: Support as export/import format, not primary storage

---

### UI Framework Choices and Trade-offs

#### Desktop Options:
- **Swing**: Easiest given NetBeans GUI Builder; older UX
- **JavaFX**: Modern-ish; more learning/setup; nicer UX

#### Web Options:
- **React/Vue**: Best UX, more complexity (SELECTED for future frontend)
- **Thymeleaf**: Simpler, one Java stack, form-heavy apps are fine

---

### Other Backend Frameworks Considered

#### 1. Micronaut
- **Why not chosen**: Spring Boot has larger ecosystem and more mature tooling
- **When to reconsider**: If startup time becomes critical (Micronaut starts faster)

#### 2. Quarkus
- **Why not chosen**: Overkill for current needs, primarily targets cloud-native/GraalVM use cases
- **When to reconsider**: If we need native compilation or Kubernetes optimization

#### 3. Dropwizard
- **Why not chosen**: Less opinionated than Spring Boot, requires more manual configuration
- **When to reconsider**: If we need more control over component selection

#### 4. Plain Java with Embedded Web Server (Jetty/Undertow)
- **Why not chosen**: Requires significant boilerplate code for REST endpoints, JSON serialization, configuration management, logging, health checks
- **When to reconsider**: If we need absolute minimal dependencies and full control

#### 5. JAX-RS (Jersey) with Manual Setup
- **Why not chosen**: Still requires manual setup for many cross-cutting concerns (dependency injection, configuration, health monitoring)
- **When to reconsider**: If we prefer standard Java EE APIs over Spring

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
