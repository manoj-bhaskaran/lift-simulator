# Changelog Archive (0.1.0 – 0.29.0)

Historical detail for the early pre-MVP releases, moved out of the main
[CHANGELOG.md](../CHANGELOG.md) to keep it focused on recent and major changes.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [0.29.0] - 2026-01-12

React admin UI scaffold release introducing the first web interface for lift-system management and backend monitoring.

- Added a React 19/Vite frontend with client-side routing, shared layout, Axios API client, and local dev-server proxying.
- Added Dashboard, Lift Systems, Config Validator, and Health Check pages for common administrative workflows.
- Added interactive JSON validation and backend health-monitoring screens with live API feedback.
- Added frontend README coverage for setup, development workflow, API integration, troubleshooting, and production builds.
- Updated the main README and admin-interface docs to include frontend setup and overview material.

## [0.28.0] - 2026-01-11

### Added
- **Publish/Archive Workflow**: Automatic archiving of previously published versions when publishing a new version
  - Ensures exactly one published configuration per lift system at any given time
  - Enhanced `LiftSystemVersionService.publishVersion()` to automatically archive previously published versions
  - Transactional workflow guarantees atomic state transitions
  - No manual archiving required - reduces human error and improves workflow efficiency
- **Runtime Configuration API**: New dedicated API for retrieving published configurations
  - `RuntimeConfigService` providing read-only access to published configurations
  - `GET /api/runtime/systems/{systemKey}/config` - Get currently published configuration by system key
  - `GET /api/runtime/systems/{systemKey}/versions/{versionNumber}` - Get specific published version
  - Returns 404 if no published version exists
  - Clear separation between admin APIs (management) and runtime APIs (consumption)
  - `RuntimeConfigDTO` with streamlined response format
- **Comprehensive Test Coverage**: Added publish/archive workflow coverage and `RuntimeConfigServiceTest` with 6 unit tests for runtime API success paths plus system-not-found, no-published-version, and unpublished-version errors.
- **Documentation**: ADR-0010 for publish/archive workflow design decisions

### Changed
- `publishVersion()` method now archives previously published versions before publishing new version
- Publish workflow is transactional - if archiving or publishing fails, entire operation rolls back
- Updated service layer tests to verify archiving behavior

## [0.27.0] - 2026-01-11

Configuration-validation release enforcing structural, domain, and publish-time checks for lift-system version JSON.

- Added `LiftConfigDTO`, `ConfigValidationService`, and validation DTOs for typed structural errors, domain rules, and warnings.
- Added `POST /api/config/validate` for non-persistent configuration validation with structured errors and warnings.
- Added version publishing validation through `POST /api/lift-systems/{systemId}/versions/{versionNumber}/publish`, including 409 responses for already-published versions.
- Enforced validation during version create, update, and publish flows so invalid configurations return 400 responses with detailed validation results.
- Expanded exception handling and tests for malformed JSON, domain rules, warnings, publish conflicts, and create/update validation failures.

## [0.26.0] - 2026-01-11

Lift-system versioning API release adding lifecycle management for configuration versions.

- Added REST endpoints to create, update, list, and retrieve lift-system versions, including optional cloning from an existing version.
- Added `LiftSystemVersionService` with per-system auto-incrementing version numbers, existence checks, and transactional writes.
- Added typed request/response DTOs for version creation, configuration updates, and version responses.
- Added unit and integration coverage for version creation, cloning, updating, listing, retrieval, auto-incrementing, error cases, and validation failures.
- Updated the REST API behavior so lift-system configurations have full version lifecycle management starting at version number 1 per system.

## [0.25.0] - 2026-01-11

Lift-system CRUD API release adding backend lifecycle management for lift-system records.

- Added create, list, detail, update, and delete endpoints for lift systems, with cascade deletion of versions.
- Added `LiftSystemService` business logic for duplicate-key validation, audit timestamps, transactions, and error handling.
- Added typed request/response DTOs with Jakarta validation for lift-system create and update flows.
- Added global exception handling for 404, validation, and illegal-argument responses with structured error payloads.
- Added service/controller tests and fixed SpotBugs representation-exposure warnings with defensive copies and targeted suppression for Spring DI.

## [0.24.0] - 2026-01-11

JPA persistence release mapping lift systems, versions, JSONB configuration, and repository access.

- Added `LiftSystem` and `LiftSystemVersion` entities with timestamps, relationships, cascade operations, and version lifecycle helpers.
- Added Hibernate 6 JSONB mapping for lift configuration storage in PostgreSQL.
- Added Spring Data repositories with lookup, status, published-version, ordering, and max-version-number queries.
- Added repository integration tests using an H2 PostgreSQL-compatible test profile for CRUD, JSONB, relationship, and cascade behavior.
- Added an optional JPA verification runner for exercising entity CRUD, JSONB persistence, relationships, and custom repository queries.

## [0.23.1–0.23.6] - 2026-01-09

### Fixed
- Fixed Flyway migration filename conflicts, PostgreSQL `lift_simulator` schema targeting, and stale build artifact cleanup across six consecutive patch releases.

## [0.23.0] - 2026-01-09

### Added
- Create the initial lift configuration schema with Flyway migration `V1__init.sql`.
- Add the `lift_simulator` schema with `lift_system` and `lift_system_version` tables for versioned JSONB configurations.
- Include publish status fields, foreign keys, and indexes for lift system versions.

## [0.22.0] - 2026-01-09

PostgreSQL/Flyway integration release establishing persistent storage for the Lift Config Service backend.

- Added PostgreSQL connectivity with Spring Data JPA, the PostgreSQL JDBC driver, and default HikariCP pooling.
- Added Flyway migration management with a baseline schema migration and automatic migration tracking at startup.
- Added profile-based development configuration for local PostgreSQL credentials, connection pooling, SQL logging, and Hibernate validation.
- Added initial `schema_metadata` storage and ADR-0007 documenting the PostgreSQL/Flyway decision.
- Updated README database setup guidance and fixed Flyway module resolution plus development credential alignment across patch releases.

## [0.21.0] - 2026-01-09

### Added
- **Spring Boot backend for Lift Config Service**: Bootstrapped a runnable Spring Boot backend skeleton for managing lift simulator configurations
  - Spring Boot 3.2.1 with Maven configuration
  - Standard package structure: `admin/controller`, `admin/service`, `admin/repository`, `admin/domain`, `admin/dto`
  - Spring Boot application class: `LiftConfigServiceApplication`
  - Custom health endpoint at `/api/health` returning service name, status, and timestamp
  - Spring Boot Actuator for production-ready health monitoring at `/actuator/health` and `/actuator/info`
  - Application configuration via `application.properties`:
    - Application name: `lift-config-service`
    - Server port: 8080
    - Logging: INFO level (root), DEBUG level (com.liftsimulator package)
    - Custom console and file logging patterns
  - Cross-platform support: Runs on Windows, Linux, and macOS with Java 17+
- **Documentation in README**:
  - "Admin UI Backend" section with setup and running instructions
  - Available endpoints documentation
  - Configuration details
  - Updated project structure showing new admin package hierarchy

### Changed
- Maven POM now inherits from `spring-boot-starter-parent` for Spring Boot dependency management
- Project description updated to reflect Spring Boot backend integration

## [0.20.0] - 2026-01-09

Scenario-test release adding deterministic coverage for controller routing behavior across realistic multi-request flows.

- Added a reusable `ScenarioHarness` for tick simulation, timed request injection, service-event logging, and queue/service assertions.
- Added DirectionalScan scenario tests for documented examples, calls above/below the lift, reversals, batching, alternating directions, and service order.
- Added NaiveLift scenario tests for nearest-first routing, back-and-forth behavior, mixed call types, dynamic additions, and baseline requests.
- Added comparison coverage running identical scenarios against both strategies.
- Verified scenario tests complete all requests, clear queues, and prevent lost or duplicated servicing.

## [0.19.0] - 2026-01-09

Directional/SCAN controller release integrating direction-aware scheduling into the simulator lifecycle.

- Integrated `DirectionalScanLiftController` so new requests, floor arrivals, and door cycles update target stops without losing or duplicating requests.
- Added `--strategy=<strategy>` to the Main demo with `nearest-request` as the default and `directional-scan` as an alternative.
- Added end-to-end DirectionalScan integration tests for movement-time requests, hall-call scheduling, cancellation, service transitions, and out-of-service recovery.
- Documented controller strategy tradeoffs, examples, hall-call filtering, and direction commitment in the README.
- Updated Main help text and demo configuration docs for controller selection.

## [0.18.1] - 2026-01-12

### Fixed
- Scenario runner now prints request lifecycle summaries after execution

## [0.18.0] - 2026-01-12

### Added
- Demo output now includes a request lifecycle summary table showing created and completed/cancelled ticks for each request

## [0.17.1–0.17.2] - 2026-01-12

### Changed
- Directional scan controller keeps traveling to the furthest pending stop in the current direction before reversing, even when only opposite-direction hall calls remain

## [0.17.0] - 2026-01-11

### Added
- Directional scan controller tests covering hall-call direction filtering and car-call eligibility

### Changed
- Directional scan controller now defers opposite-direction hall calls until after reversal and filters stop eligibility by hall-call direction

## [0.16.0] - 2026-01-10

### Added
- Directional scan controller implementation that continues in the current direction until requests are serviced
- Scenario runner support for DIRECTIONAL_SCAN controller strategy
- Directional scan controller unit tests covering direction commitment and reversal

### Changed
- Controller factory now instantiates the directional scan controller
- Scenario and main demo now use a shared request-managing controller interface

## [0.15.0] - 2026-01-09

### Added
- Explicit `idle_parking_mode` configuration in `demo.scenario` with inline documentation
- Comments in `demo.scenario` documenting available controller configuration options
- Comprehensive README documentation for command-line usage of both demo and scenario runner

### Changed
- Demo and scenario runner now display selected controller strategy and idle parking mode at startup
- Demo.scenario now explicitly shows both controller_strategy and idle_parking_mode with comments
- Demo and scenario runner only expose `--help` on the command line; controller strategy and idle parking mode are configured via scenario files or defaults

## [0.14.0] - 2026-01-09

### Added
- Selectable controller strategy with `ControllerStrategy` enum
  - `NEAREST_REQUEST_ROUTING`: Services nearest request first (current NaiveLiftController behavior, default)
  - `DIRECTIONAL_SCAN`: Directional scan algorithm (placeholder for future implementation)
- `ControllerFactory` class for creating controller instances based on strategy
  - Factory method with default parameters: `createController(ControllerStrategy strategy)`
  - Factory method with custom parameters: `createController(ControllerStrategy strategy, int homeFloor, int idleTimeoutTicks, IdleParkingMode idleParkingMode)`
- Scenario file support for `controller_strategy:` configuration parameter
- Architecture Decision Record (ADR-0005) documenting controller strategy design
- Comprehensive unit tests for `ControllerFactory` (6 test cases)
- Integration tests verifying controller selection in scenarios (4 test cases)

### Changed
- `ScenarioDefinition`, `ScenarioParser`, and `ScenarioRunnerMain` updated to support controller strategy configuration
- `Main.java` updated to use `ControllerFactory` with explicit `NEAREST_REQUEST_ROUTING` strategy
- `demo.scenario` updated to include explicit controller strategy configuration

## [0.13.0] - 2026-01-09

### Added
- Configurable idle parking mode with `IdleParkingMode` enum
  - `STAY_AT_CURRENT_FLOOR`: Lift remains at current floor when idle (no parking movement)
  - `PARK_TO_HOME_FLOOR`: Lift moves to home floor after idle timeout (existing behavior, now default)
- Three-parameter `NaiveLiftController` constructor accepting `homeFloor`, `idleTimeoutTicks`, and `idleParkingMode`
- Scenario file support for `idle_parking_mode:` configuration parameter
- Architecture Decision Record (ADR-0004) documenting idle parking mode design
- Comprehensive unit tests for both parking modes (6 new test cases)

### Changed
- Idle parking logic now respects configured mode (only initiates parking movement when mode is `PARK_TO_HOME_FLOOR`)
- `ScenarioDefinition`, `ScenarioParser`, and `ScenarioRunnerMain` updated to support idle parking mode configuration

## [0.12.7–0.12.17] - 2026-01-25

### Added
- Added CI and quality tooling with build, coverage, style, static-analysis, packaging, Checkstyle, SpotBugs, and OWASP Dependency-Check coverage.

### Changed
- Improved CI and quality implementation by centralizing request lifecycle transitions, indexing naive-controller active requests, retaining dedicated request collections, and standardizing comment formatting.

### Fixed
- Fixed SpotBugs offline dependency resolution, guarded idle-timeout calculations, enforced scenario tick/event input limits, optimized naive-controller request handling, and improved scenario validation safety.

## [0.12.1–0.12.6] - 2026-01-14

### Added
- Added simulation engine package documentation, JaCoCo coverage enforcement, scenario parsing/execution integration tests, and a lift request ID generator reset hook for test isolation.

### Changed
- Refactored the simulation engine around builder defaults, named timing/sentinel constants, state-specific tick handlers, JavaDoc coverage, and lower-boilerplate sequence-based engine tests.

### Fixed
- Fixed scenario event tick validation and invalid action reporting so malformed scenario files and invalid engine actions fail with explicit messages.

## [0.12.0] - 2026-01-10

### Changed
- Replace telescoping `SimulationEngine` constructors with a builder for configuration

## [0.11.0] - 2026-01-10

### Added
- Allow scenario files to configure controller and simulation timing parameters (including door dwell time) alongside scripted events

### Fixed
- Expand scenario runner floor bounds to include requested negative floors while keeping the default range and starting floor

## [0.10.2] - 2026-01-10

### Fixed
- Prevent direction reversals while still in a moving state by stopping before changing direction

## [0.10.1] - 2026-01-10

### Fixed
- Defer return-to-service events until the out-of-service shutdown sequence completes

## [0.10.0] - 2026-01-09

### Added
- Scenario runner for scripted simulations with tick-based event triggers
- Plain-text scenario definitions with support for car calls, hall calls, cancellations, and service mode changes
- Scenario output logging that includes tick, floor, lift state, and pending requests

## [0.9.0] - 2026-01-08

### Added
- **Out-of-service functionality**: Full support for taking lifts out of service safely
- **`takeOutOfService()`**: Cancels all active requests in `NaiveLiftController` when taking the lift out of service
- **`returnToService()` (controller)**: Prepares `NaiveLiftController` for returning to normal operation
- **`setOutOfService()`**: Transitions the lift to OUT_OF_SERVICE state from any state in `SimulationEngine`
- **`returnToService()` (engine)**: Transitions `SimulationEngine` from OUT_OF_SERVICE back to IDLE state
- **Automatic request cancellation**: Cancels all pending requests (QUEUED, ASSIGNED, SERVING) when taking the lift out of service
- **Test coverage**: Comprehensive `OutOfServiceTest` suite covering entering/exiting service from all states
- **Demo update**: Showcases the out-of-service scenario at tick 25 and return to service at tick 30
- **Documentation**: Out-of-service behavior documented in README with usage examples

### Fixed
- Corrected arrival and door timing by normalizing movement status and preserving `MOVING_*` state until controller decisions.
- Aligned out-of-service test flow with the required stop tick and ignored new request assignments while a lift is out of service.
- Avoided reopening doors during out-of-service shutdown when doors are already open or opening.

### Changed
- OUT_OF_SERVICE state (already existed since v0.2.0) now has full operational support
- Lifts in OUT_OF_SERVICE state cannot move, open doors, or accept new requests
- Demo simulation extended to 50 ticks to show complete out-of-service workflow

## [0.8.0] - 2026-01-06

### Added
- Configurable idle parking behavior for the naive controller with home floor and idle timeout
- Parking logic that resumes normal request handling when new requests arrive
- Tests covering idle parking and interruption scenarios

## [0.7.1] - 2026-01-06

### Fixed
- Align cancellation-while-moving integration test expectations with tick-per-floor movement behavior

## [0.7.0] - 2026-01-06

### Added
- **Request cancellation API**: New `cancelRequest(long requestId)` method in `NaiveLiftController` to safely cancel pending requests
- **Non-terminal cancellation**: Ability to cancel requests in any non-terminal state (QUEUED, ASSIGNED, SERVING)
- **Queue cleanup**: Automatic removal of cancelled requests from the controller's queue
- **Success signaling**: Return value indicates cancellation success (false if request not found or already terminal)
- **Unit tests**: Comprehensive coverage for cancelling requests in each lifecycle state
- **Integration tests**: Cancellation scenarios (while moving, multiple requests on the same floor)
- **Lifecycle tests**: Verify CANCELLED terminal state behavior

## [0.6.0] - 2026-01-06

### Added
- **Door reopening window**: Configurable time window during door closing when doors can be reopened for new requests
- **`doorReopenWindowTicks` parameter**: Added to the `SimulationEngine` constructor (default: `min(2, doorTransitionTicks)`)
- **Realistic door behavior**: Doors reopen if a request arrives for the current floor within the reopen window
- **Request queuing**: Automatic queuing when the door closing window has passed
- **Validation**: Ensures `doorReopenWindowTicks` is non-negative and does not exceed `doorTransitionTicks`
- **Unit tests**: Comprehensive coverage for door reopening scenarios (within window, outside window, boundary cases, zero window)
- **Integration tests**: `NaiveLiftController` coverage demonstrating real-world door reopening behavior
- **Backward compatibility**: Default window automatically adjusts to `doorTransitionTicks` when smaller than 2

### Changed
- `NaiveLiftController` now attempts to reopen doors when a request arrives for the current floor during door closing
- `SimulationEngine` tracks elapsed ticks during door closing to enforce reopen window
- Door reopening logic in `startAction()` checks elapsed time against configured window

## [0.5.0] - 2026-01-06

### Added
- **Lift request lifecycle management**: Requests are now first-class entities with explicit lifecycle states
- **`LiftRequest` domain model**: Unique ID, type, origin, destination, and direction
- **`RequestState` enum**: Six lifecycle states: CREATED, QUEUED, ASSIGNED, SERVING, COMPLETED, CANCELLED
- **`RequestType` enum**: Distinguishes between HALL_CALL and CAR_CALL requests
- **State transition validation**: Ensures only valid request state changes occur
- **Factory methods**: `LiftRequest.hallCall()` and `LiftRequest.carCall()` for creating requests
- **Terminal state detection**: `isTerminal()` method
- **Unit tests**: Comprehensive request lifecycle coverage (`LiftRequestTest`, `LiftRequestLifecycleTest`)
- **ADR-0003**: Documents the request lifecycle architectural decision

### Changed
- `NaiveLiftController` now manages requests as `LiftRequest` objects internally
- Controller automatically advances requests through lifecycle states (CREATED → QUEUED → ASSIGNED → SERVING → COMPLETED)
- Completed and cancelled requests are automatically removed from the controller
- Backward compatibility maintained: `addCarCall()` and `addHallCall()` methods still work
- Demo output now displays request lifecycle status with compact "Requests" column showing counts by state (Q:n, A:n, S:n)

## [0.4.0] - 2026-01-13

### Added
- Added configurable door dwell timing with automatic door close cycles
- Extended simulation engine timing tests to cover door dwell and closing transitions

### Changed
- Door cycles now automatically progress from open to closing after the dwell duration

### Fixed
- Align door closing timing from dwell with door transition tick consumption

## [0.3.0] - 2026-01-12

### Added
- Introduced a deterministic simulation clock to manage tick progression
- Added configurable travel and door transition durations to model time per floor
- Expanded unit tests to validate tick-based movement and door timing
- Added a deterministic simulation regression test for fixed inputs

### Changed
- Movement and door transitions now consume configured ticks before completing

## [0.2.1–0.2.9] - 2026-01-11

### Added
- Added version display to demo output so runs show the active software version.

### Fixed
- Fixed door-state, direction, and state-machine guard bugs in the naive controller, including same-floor request cleanup, transitional door states, stop-before-open behavior, and door-opening/downward movement sequencing.
- Fixed build reliability by adding the missing `LiftStatus` test import, configuring the Maven Java release target, and setting filtered resource encoding.

## [0.2.0] - 2026-01-06

### Added
- Introduced formal lift state machine with `LiftStatus` enum (7 states)
- New lift states: IDLE, MOVING_UP, MOVING_DOWN, DOORS_OPENING, DOORS_OPEN, DOORS_CLOSING, OUT_OF_SERVICE
- `DOORS_OPENING` state for symmetric door transitions
- `StateTransitionValidator` class to enforce valid state transitions
- Comprehensive unit tests for state machine transitions and validation
- State transition table in README documentation
- ADR-0002 documenting single source of truth architectural decision
- State machine prevents invalid operations (e.g., movement with doors open)
- Logging for invalid state transitions

### Changed
- **Breaking**: `LiftState` refactored to single source of truth pattern
- **Reduced state**: `LiftState` now stores only `floor` and `status` (direction and doorState are derived)
- **Constructor change**: Signature changed from `LiftState(floor, direction, doorState, status)` to `LiftState(floor, status)`
- **Computed properties**: `Direction` and `DoorState` are now derived, not stored fields
- **State-driven engine**: `SimulationEngine` refactored to be state-driven with enforced transitions
- **Validated transitions**: State transitions are validated before being applied
- **Symmetric door behavior**: Both opening and closing are now transitional states

### Fixed
- Eliminated possibility of invalid state combinations (e.g., moving with doors open)
- Lift can no longer move when doors are open (enforced by state machine)
- Invalid state transitions are prevented and logged
- Door state changes are now controlled by the state machine
- Removed redundant state storage reducing memory usage

## [0.1.3] - 2026-01-09

### Fixed
- Use enum comparison for door state checks in the demo output

## [0.1.2] - 2026-01-08

### Fixed
- Make nearest-floor selection deterministic with a tie-breaker for equidistant requests
- Added tests covering deterministic behavior for equidistant requests

## [0.1.1] - 2026-01-07

### Fixed
- Initialize the simulation state at the configured minimum floor
- Normalize direction to idle when movement is requested at top or bottom floors
- Added tests for non-zero minimum floor initialization and boundary direction behavior

## [0.1.0] - 2026-01-06

### Added
- Initial simulator foundation release with the tick-based core engine and immutable `LiftState` model.
- Core simulation contracts and strategies: `LiftController`, `NaiveLiftController`, and `SimpleLiftController`.
- Initial request/action primitives: `Action`, `Direction`, `DoorState`, plus support for `CarCall` and `HallCall`.
- Developer baseline: console demo (`Main.java`), unit tests, Maven/Java 17 + JUnit 5 build setup, and starter docs (README, CHANGELOG, ADR-0001).

### Changed
- Chose tick-based simulation for predictable, testable time advancement.
- Used immutable state objects to reduce shared-mutable-state defects.
- Separated controller logic from the simulation engine for extensibility.
