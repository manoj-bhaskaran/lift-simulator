# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

### Design Decisions
- Enum-based strategy selection provides type safety and compile-time validation
- Factory pattern centralizes controller instantiation logic
- Default strategy preserves backward compatibility (NEAREST_REQUEST_ROUTING)
- Unimplemented strategies (DIRECTIONAL_SCAN) throw `UnsupportedOperationException` with clear error messages
- Controller strategy is configured at initialization time (not runtime switchable)

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

## [0.12.17] - 2026-01-25

### Changed
- Centralize request completion transitions in `LiftRequest` to avoid duplicated lifecycle chains

## [0.12.16] - 2026-01-24

### Fixed
- Guard idle timeout calculations in the naive controller against null idle tracking state

## [0.12.15] - 2026-01-23

### Changed
- Standardize comment formatting across the codebase and tests

## [0.12.14] - 2026-01-22

### Fixed
- Enforce scenario tick and event limits to prevent resource exhaustion when parsing scenario files

## [0.12.13] - 2026-01-21

### Changed
- Keep dedicated active/completed request collections in the naive controller to avoid rebuilding active sets during scheduling

## [0.12.12] - 2026-01-20

### Changed
- Index active lift requests by ID in the naive controller to avoid linear lookups during cancellation

## [0.12.11] - 2026-01-19

### Added
- Add OWASP Dependency-Check Maven plugin to scan for vulnerable dependencies during builds

## [0.12.10] - 2026-01-18

### Fixed
- Replace SpotBugs annotation dependency with a local exclude filter to keep builds resolving offline

## [0.12.7] - 2026-01-15

### Added
- Expand CI to include build, coverage, style checks, static analysis, and packaging steps
- Add Checkstyle and SpotBugs tooling with a shared Checkstyle configuration

## [0.12.6] - 2026-01-14

### Added
- Add package-level documentation for the core lift simulator packages

## [0.12.5] - 2026-01-13

### Changed
- Add JavaDoc coverage for public APIs in the lift engine and domain enums

## [0.12.4] - 2026-01-12

### Added
- Add JaCoCo coverage reporting and enforce an 80% line coverage minimum in Maven builds
- Add integration tests covering scenario parsing, execution, and invalid scenario handling

### Changed
- Refactor simulation engine tests to use a sequence-based controller helper to reduce boilerplate
- Extend the move-down test sequence to cover all downward ticks

### Fixed
- Validate scenario event ticks to reject negative or malformed values with clear error messages
- Return explicit action results and log invalid action attempts in the simulation engine to avoid silent failures

## [0.12.3] - 2026-01-11

### Added
- Add a test reset hook for the lift request ID generator to improve test isolation

## [0.12.2] - 2026-01-10

### Changed
- Refactor tick processing into state-specific handlers for clearer maintenance

## [0.12.1] - 2026-01-10

### Changed
- Extract default timing values and sentinel configuration into named constants

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

## [0.9.4] - 2026-01-08

### Fixed
- Ignore new request assignments while a lift is out of service

## [0.9.3] - 2026-01-08

### Fixed
- Align out-of-service integration test flow with the required stop tick before door opening

## [0.9.2] - 2026-01-08

### Fixed
- Preserve MOVING_* status on arrival until controller decisions so door dwell timing and parking interruptions occur after a stop tick

## [0.9.1] - 2026-01-08

### Fixed
- Normalize movement status before controller decisions so doors can open immediately after arriving at a requested floor
- Avoid reopening doors during out-of-service shutdown when doors are already open or opening

## [0.9.0] - 2026-01-08

### Added
- **Out-of-service functionality**: Full support for taking lifts out of service safely
- `takeOutOfService()` method in `NaiveLiftController` to cancel all active requests when taking lift out of service
- `returnToService()` method in `NaiveLiftController` to prepare lift for returning to normal operation
- `setOutOfService()` method in `SimulationEngine` to transition lift to OUT_OF_SERVICE state from any state
- `returnToService()` method in `SimulationEngine` to transition from OUT_OF_SERVICE back to IDLE state
- Automatic cancellation of all pending requests (QUEUED, ASSIGNED, SERVING) when taking lift out of service
- Comprehensive test suite (`OutOfServiceTest`) covering entering/exiting service from all states
- Demo updated to showcase out-of-service scenario at tick 25 and return to service at tick 30
- Documentation of out-of-service behavior in README with usage examples

### Changed
- OUT_OF_SERVICE state (already existed since v0.2.0) now has full operational support
- Lifts in OUT_OF_SERVICE state cannot move, open doors, or accept new requests
- Demo simulation extended to 50 ticks to show complete out-of-service workflow

### Design Decisions
- Taking lift out of service immediately cancels all unserviced requests for safety
- **Graceful shutdown sequence**: If moving, lift completes movement to next floor; doors then open to allow passenger exit, then close, before transitioning to OUT_OF_SERVICE
- Ensures passenger safety by allowing exit before going offline
- If lift is stationary with doors closed, doors still open/close to ensure nobody is trapped
- Door state is CLOSED when out of service (derived from status)
- Direction is IDLE when out of service (derived from status)
- Returning to service transitions to IDLE state, ready to accept new requests
- Two-step process: controller cancels requests, engine manages graceful shutdown (separation of concerns)

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
- Ability to cancel requests in any non-terminal state (QUEUED, ASSIGNED, SERVING)
- Automatic removal of cancelled requests from the controller's queue
- Return value indicates cancellation success (false if request not found or already terminal)
- Comprehensive unit tests for cancelling requests in each lifecycle state
- Integration tests for cancellation scenarios (while moving, multiple requests same floor)
- Lifecycle tests verifying CANCELLED terminal state behavior

### Design Decisions
- Cancelled requests transition to CANCELLED state before removal, maintaining lifecycle integrity
- Cancellation is idempotent - calling multiple times on same request ID is safe
- Cancelled requests are immediately removed from queue to prevent processing
- Existing `isTerminal()` checks automatically filter cancelled requests from active processing
- No ADR created as this extends the existing request lifecycle architecture (ADR-0003)

## [0.6.0] - 2026-01-06

### Added
- **Door reopening window**: Configurable time window during door closing when doors can be reopened for new requests
- `doorReopenWindowTicks` parameter to `SimulationEngine` constructor (default: `min(2, doorTransitionTicks)`)
- Realistic door behavior: doors reopen if request arrives for current floor within the reopen window
- Automatic request queuing when door closing window has passed
- Validation ensuring `doorReopenWindowTicks` is non-negative and does not exceed `doorTransitionTicks`
- Comprehensive unit tests for door reopening scenarios (within window, outside window, boundary cases, zero window)
- Integration tests with `NaiveLiftController` demonstrating real-world door reopening behavior
- Backward compatibility: default window automatically adjusts to `doorTransitionTicks` when smaller than 2

### Changed
- `NaiveLiftController` now attempts to reopen doors when a request arrives for the current floor during door closing
- `SimulationEngine` tracks elapsed ticks during door closing to enforce reopen window
- Door reopening logic in `startAction()` checks elapsed time against configured window

### Design Decisions
- Reopen window measured in simulation ticks for consistency with other timing parameters
- Window applies only during DOORS_CLOSING state, not after doors fully close
- Zero-tick window disables reopening entirely (doors cannot be interrupted once closing starts)
- Request remains queued if door closing window has passed, lift will serve it after current cycle

## [0.5.0] - 2026-01-06

### Added
- **Lift request lifecycle management**: Requests are now first-class entities with explicit lifecycle states
- `LiftRequest` domain model with unique ID, type, origin, destination, and direction
- `RequestState` enum with six lifecycle states: CREATED, QUEUED, ASSIGNED, SERVING, COMPLETED, CANCELLED
- `RequestType` enum to distinguish between HALL_CALL and CAR_CALL requests
- State transition validation ensuring only valid request state changes occur
- Factory methods `LiftRequest.hallCall()` and `LiftRequest.carCall()` for creating requests
- Terminal state detection with `isTerminal()` method
- Comprehensive unit tests for request lifecycle (LiftRequestTest, LiftRequestLifecycleTest)
- ADR-0003 documenting request lifecycle architectural decision

### Changed
- `NaiveLiftController` now manages requests as `LiftRequest` objects internally
- Controller automatically advances requests through lifecycle states (CREATED → QUEUED → ASSIGNED → SERVING → COMPLETED)
- Completed and cancelled requests are automatically removed from the controller
- Backward compatibility maintained: `addCarCall()` and `addHallCall()` methods still work
- Demo output now displays request lifecycle status with compact "Requests" column showing counts by state (Q:n, A:n, S:n)

### Design Decisions
- Every request must end in either COMPLETED or CANCELLED state (terminal states)
- Invalid state transitions throw `IllegalStateException`
- Self-transitions are prevented
- Terminal states cannot transition to any other state
- Request state progression is automatically managed by the controller

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

## [0.2.9] - 2026-01-06

### Fixed
- Configure Maven to use Java release targeting to avoid module path warnings
- Explicitly set the resources plugin encoding for filtered properties files

## [0.2.8] - 2026-01-11

### Fixed
- Keep the naive controller idle while doors are closing to avoid invalid move attempts

## [0.2.7] - 2026-01-11

### Added
- Print the running software version in the demo output

## [0.2.6] - 2026-01-11

### Fixed
- Keep the NaiveLiftController idle during door opening to prevent invalid move attempts
- Clear same-floor requests while doors are opening to avoid redundant open commands

## [0.2.5] - 2026-01-11

### Fixed
- Clear same-floor requests while doors are opening or open to avoid extra open/close cycles

## [0.2.4] - 2026-01-11

### Fixed
- Ensure the NaiveLiftController stops before opening doors when arriving at a requested floor while moving

## [0.2.3] - 2026-01-10

### Fixed
- Include lift status in the NaiveLiftController demo output so transitional states are visible

## [0.2.2] - 2026-01-10

### Fixed
- Align door-opening test flow with state machine requirements before attempting to move down

## [0.2.1] - 2026-01-10

### Fixed
- Add missing `LiftStatus` import in `SimulationEngineTest` to restore test compilation

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
- `LiftState` now stores only `floor` and `status` (direction and doorState are derived)
- Constructor signature changed from `LiftState(floor, direction, doorState, status)` to `LiftState(floor, status)`
- `Direction` and `DoorState` are now computed properties, not stored fields
- `SimulationEngine` refactored to be state-driven with enforced transitions
- State transitions are validated before being applied
- Door behavior is now symmetric (both opening and closing are transitional states)

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
- Initial tick-based simulation engine
- `SimulationEngine` class for advancing time in discrete ticks
- `LiftState` immutable domain model (floor, direction, door state)
- `Action` enum for lift actions (MOVE_UP, MOVE_DOWN, OPEN_DOOR, CLOSE_DOOR, IDLE)
- `LiftController` interface for pluggable controller strategies
- `NaiveLiftController` implementation (services nearest request first)
- `SimpleLiftController` alternative basic implementation
- Support for `CarCall` (requests from inside the lift)
- Support for `HallCall` (requests from a floor with direction)
- `Direction` enum (UP, DOWN, IDLE)
- `DoorState` enum (OPEN, CLOSED)
- Console-based demo in `Main.java`
- Unit tests for `SimulationEngine` and `NaiveLiftController`
- Maven build configuration with Java 17 and JUnit 5
- Basic project documentation (README, CHANGELOG, ADR-0001)
- EditorConfig file for consistent code formatting across IDEs and platforms

### Design Decisions
- Chose tick-based simulation for predictable, testable time advancement
- Immutable state objects to avoid bugs from shared mutable state
- Separated controller logic from simulation engine for flexibility

[0.9.1]: https://github.com/manoj-bhaskaran/lift-simulator/compare/v0.9.0...v0.9.1
[0.6.0]: https://github.com/manoj-bhaskaran/lift-simulator/compare/v0.5.0...v0.6.0
[0.5.0]: https://github.com/manoj-bhaskaran/lift-simulator/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.4.0
[0.3.0]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.3.0
[0.2.9]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.2.9
[0.2.8]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.2.8
[0.2.7]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.2.7
[0.2.6]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.2.6
[0.2.5]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.2.5
[0.2.4]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.2.4
[0.2.3]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.2.3
[0.2.2]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.2.2
[0.2.1]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.2.1
[0.2.0]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.2.0
[0.1.3]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.1.3
[0.1.2]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.1.2
[0.1.1]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.1.1
[0.1.0]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.1.0
