# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
