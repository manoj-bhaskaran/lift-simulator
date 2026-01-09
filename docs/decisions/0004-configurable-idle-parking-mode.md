# ADR-0004: Configurable Idle Parking Mode

## Status
Accepted

## Context
Prior to version 0.13.0, the lift's idle parking behavior was hardcoded to always move to a designated home floor after an idle timeout. When no active requests existed and the idle timeout elapsed, the lift would automatically begin moving one floor per tick toward the `homeFloor`.

This single parking strategy doesn't accommodate all use cases:
- **Low-traffic buildings**: Moving to a home floor (e.g., lobby) saves energy and provides predictable positioning
- **High-traffic buildings**: Staying at the last serviced floor reduces response time for new requests
- **Multi-tenant buildings**: Different floors may serve as optimal idle positions depending on time of day
- **Testing scenarios**: Need to verify controller behavior without parking movement

### Requirements
- Make idle parking behavior configurable at controller initialization
- Support two distinct modes:
  1. Stay at current floor when idle (no parking movement)
  2. Park to home floor after timeout (existing behavior)
- Maintain backward compatibility (existing behavior must remain default)
- Preserve existing idle timeout tracking logic (only track when idle with doors closed)
- Support configuration via scenario files and programmatic API
- No changes to idle timeout calculation or tracking behavior

### Options Considered

**Option 1: Boolean flag `disableParking` (Simple Toggle)**
- Add `boolean disableParking` parameter to constructor
- Pros: Minimal API surface, easy to understand
- Cons: Not extensible (what if we add more modes?), negative boolean naming

**Option 2: Enum-based mode selection (Chosen)**
- Create `IdleParkingMode` enum with `STAY_AT_CURRENT_FLOOR` and `PARK_TO_HOME_FLOOR`
- Pros: Explicit mode names, extensible to future modes, self-documenting
- Cons: Slightly more complex than boolean

**Option 3: Strategy pattern with pluggable parking strategies**
- Create `IdleParkingStrategy` interface with implementations
- Pros: Maximum flexibility, supports custom strategies
- Cons: Over-engineered for current needs, more complex API

## Decision
We will adopt **Option 2** and create `IdleParkingMode` enum with explicit mode selection:

### New Domain Model

**`IdleParkingMode` enum** - Two parking behaviors:
- `STAY_AT_CURRENT_FLOOR`: Lift remains at its current floor when idle, no parking movement occurs
- `PARK_TO_HOME_FLOOR`: Lift moves to the designated home floor when idle after timeout (existing behavior)

### Controller Integration

**Updated `NaiveLiftController` constructors:**
```java
// Default constructor - uses PARK_TO_HOME_FLOOR for backward compatibility
public NaiveLiftController()

// Two-parameter constructor - uses PARK_TO_HOME_FLOOR for backward compatibility
public NaiveLiftController(int homeFloor, int idleTimeoutTicks)

// Three-parameter constructor - explicit mode selection
public NaiveLiftController(int homeFloor, int idleTimeoutTicks, IdleParkingMode idleParkingMode)
```

**Idle parking logic modification:**
- Idle timeout tracking remains unchanged (still increments only when idle with doors closed)
- After timeout is reached, parking movement only initiates if mode is `PARK_TO_HOME_FLOOR`
- `STAY_AT_CURRENT_FLOOR` mode: lift continues idling indefinitely at current floor
- Both modes respond to new requests identically

### Configuration Support

**Scenario file configuration:**
```
idle_parking_mode: STAY_AT_CURRENT_FLOOR
idle_parking_mode: PARK_TO_HOME_FLOOR
```

**Programmatic configuration:**
```java
NaiveLiftController controller = new NaiveLiftController(
    0,                                        // homeFloor
    5,                                        // idleTimeoutTicks
    IdleParkingMode.STAY_AT_CURRENT_FLOOR    // idleParkingMode
);
```

### Backward Compatibility Strategy

**Default mode:** `PARK_TO_HOME_FLOOR`
- All existing code continues to work without changes
- Existing constructors delegate to new three-parameter constructor with default mode
- Scenario files without `idle_parking_mode:` use default mode

**Validation:**
- Constructor throws `IllegalArgumentException` if `idleParkingMode` is null
- Scenario parser throws `IllegalArgumentException` for invalid mode values

## Consequences

### Positive
- **Use case flexibility**: Supports both stay-in-place and home-floor parking strategies
- **Backward compatible**: Existing behavior preserved as default
- **Explicit configuration**: Mode is clearly stated in code and scenario files
- **Extensible design**: Easy to add new parking modes (e.g., `PARK_TO_NEAREST_LOBBY`, `PARK_BY_TIME_OF_DAY`)
- **Self-documenting**: Enum names make intent clear (`STAY_AT_CURRENT_FLOOR` vs `PARK_TO_HOME_FLOOR`)
- **Testable**: Both modes have comprehensive unit test coverage
- **No behavior changes**: Idle timeout tracking logic completely unchanged

### Negative
- **API expansion**: New parameter in constructor and scenario definition
- **Migration consideration**: Future controllers must handle idle parking mode
- **Documentation requirement**: Need to document mode selection in README and scenarios

### Neutral
- Idle timeout calculation remains identical across both modes
- Both modes reset idle tracking when requests arrive
- Door state requirements for idle tracking unchanged

## Implementation Details

### Key Changes
1. **New enum**: `IdleParkingMode` in `com.liftsimulator.domain` package
2. **Controller field**: `idleParkingMode` in `NaiveLiftController`
3. **Constructor chain**: Three constructors with progressive defaults
4. **Logic change**: Added mode check in idle parking condition at `NaiveLiftController.java:356-357`
5. **Scenario support**: Parser handles `idle_parking_mode:` configuration line

### Parking Logic Modification
**Before:**
```java
if (idleTicks >= idleTimeoutTicks && currentFloor != homeFloor) {
    parkingInProgress = true;
    return moveTowardHome(currentFloor);
}
```

**After:**
```java
if (idleTicks >= idleTimeoutTicks && currentFloor != homeFloor
        && idleParkingMode == IdleParkingMode.PARK_TO_HOME_FLOOR) {
    parkingInProgress = true;
    return moveTowardHome(currentFloor);
}
```

### Test Coverage
- **testStaysAtCurrentFloorWithStayMode**: Verifies STAY mode at floor 0
- **testStaysAtCurrentFloorWithStayModeAtNonZeroFloor**: Verifies STAY mode at non-zero floor
- **testParksToHomeFloorWithParkMode**: Verifies PARK mode behavior unchanged
- **testIdleTimeIncrementsSameForBothModes**: Verifies idle tracking consistency
- **testStayModeWithRequestInterruption**: Verifies request handling in STAY mode
- **testIdleTimeOnlyTrackedWhenDoorsClosedForBothModes**: Verifies door state requirements

## Migration Impact
- **Updated**: `NaiveLiftController` (new field, constructors, parking logic)
- **Added**: `IdleParkingMode` enum in domain package
- **Updated**: `ScenarioDefinition` (new field and getter)
- **Updated**: `ScenarioParser` (parse `idle_parking_mode:` configuration)
- **Updated**: `ScenarioRunnerMain` (pass mode to controller)
- **Added**: 6 new unit tests in `NaiveLiftControllerTest`
- **Updated**: CHANGELOG.md with version 0.13.0 entry
- **Updated**: README.md with idle parking mode documentation
- **No changes needed**: Existing code using default constructors (backward compatible)

## Future Considerations
- **Time-based parking**: Different modes during business hours vs. off-hours
- **Load-based parking**: Adjust parking strategy based on recent traffic patterns
- **Multi-floor parking zones**: Park at different floors based on building zones
- **Energy optimization**: Parking mode selection based on energy consumption metrics
- **Predictive parking**: Use historical data to pre-position lift at likely request floors

## References
- Related to ADR-0001 (Tick-Based Simulation) - idle timeout still tracked per tick
- Related to ADR-0002 (Single Source of Truth) - `idleParkingMode` is the single source for parking behavior
- Implements strategy pattern concept from "Design Patterns" (Gamma et al.)
- Addresses requirement from GitHub issue: "Idle parking mode is configurable (STAY vs HOME)"
