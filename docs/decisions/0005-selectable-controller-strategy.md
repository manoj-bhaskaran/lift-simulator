# ADR-0005: Selectable Controller Strategy

## Status
Accepted

## Context
Prior to version 0.14.0, the lift system was hardcoded to use the `NaiveLiftController` implementation, which services the nearest request first. This approach works well for many scenarios but doesn't accommodate different scheduling algorithms that may be more efficient for specific use cases or traffic patterns.

Different controller algorithms have different performance characteristics:
- **Nearest-request routing**: Simple and predictable, services closest floor first
- **Directional scan (SCAN/elevator algorithm)**: Continues in current direction while requests exist, more efficient for high-traffic scenarios
- **LOOK algorithm**: Similar to SCAN but reverses at the furthest request rather than the building extreme
- **Destination dispatch**: Groups passengers by destination for optimal efficiency

### Requirements
- Make controller strategy selectable at system setup/configuration
- Define a `ControllerStrategy` enum with at least two strategies
- Provide a factory pattern for instantiating the chosen controller
- Persist the selected strategy in system configuration
- Default selection should preserve current behavior (NEAREST_REQUEST_ROUTING)
- Support configuration via scenario files and programmatic API

### Options Considered

**Option 1: Direct controller class specification**
- Allow specifying controller class name in configuration
- Pros: Maximum flexibility, can use any controller implementation
- Cons: Type-unsafe, requires reflection, error-prone

**Option 2: Enum-based strategy selection with factory (Chosen)**
- Create `ControllerStrategy` enum with defined strategies
- Use factory pattern to instantiate controllers based on strategy
- Pros: Type-safe, extensible, self-documenting, compile-time validation
- Cons: Need to update enum for each new strategy

**Option 3: Service provider interface (SPI)**
- Use Java SPI to load controller implementations
- Pros: Plugin architecture, third-party extensions possible
- Cons: Over-engineered for current needs, complex configuration

## Decision
We will adopt **Option 2** and create `ControllerStrategy` enum with factory-based instantiation:

### New Domain Model

**`ControllerStrategy` enum** - Defines available controller algorithms:
- `NEAREST_REQUEST_ROUTING`: Services nearest request first (current NaiveLiftController behavior)
- `DIRECTIONAL_SCAN`: Directional scan algorithm (placeholder for future implementation)

### Factory Pattern

**`ControllerFactory` class** - Creates controller instances:
```java
public class ControllerFactory {
    public static LiftController createController(ControllerStrategy strategy)

    public static LiftController createController(
        ControllerStrategy strategy,
        int homeFloor,
        int idleTimeoutTicks,
        IdleParkingMode idleParkingMode)
}
```

### Configuration Support

**Scenario file configuration:**
```
controller_strategy: NEAREST_REQUEST_ROUTING
controller_strategy: DIRECTIONAL_SCAN
```

**Programmatic configuration:**
```java
LiftController controller = ControllerFactory.createController(
    ControllerStrategy.NEAREST_REQUEST_ROUTING,
    0,    // homeFloor
    5,    // idleTimeoutTicks
    IdleParkingMode.PARK_TO_HOME_FLOOR
);
```

### Backward Compatibility Strategy

**Default strategy:** `NEAREST_REQUEST_ROUTING`
- Preserves existing behavior when no strategy is specified
- Scenario files without `controller_strategy:` use default in ScenarioRunnerMain
- Main demo explicitly uses `NEAREST_REQUEST_ROUTING` to demonstrate factory usage

**Validation:**
- Factory throws `IllegalArgumentException` if strategy is null
- Factory throws `UnsupportedOperationException` for unimplemented strategies (e.g., DIRECTIONAL_SCAN)
- Scenario parser throws `IllegalArgumentException` for invalid strategy values

## Consequences

### Positive
- **Algorithm flexibility**: Supports multiple controller algorithms with easy switching
- **Backward compatible**: Existing behavior preserved with NEAREST_REQUEST_ROUTING default
- **Type-safe configuration**: Enum prevents invalid strategy specifications
- **Extensible design**: Easy to add new strategies by extending enum and factory
- **Self-documenting**: Enum names clearly indicate scheduling algorithm
- **Testable**: Factory and configuration parsing have comprehensive test coverage
- **Factory pattern**: Centralizes controller instantiation logic

### Negative
- **API expansion**: New enum, factory class, and configuration parameter
- **Incomplete implementation**: DIRECTIONAL_SCAN is defined but not yet implemented
- **Factory maintenance**: Each new controller strategy requires factory updates
- **Documentation requirement**: Need to document strategy options and characteristics

### Neutral
- Controller interface remains unchanged
- Existing controller implementations unmodified
- Strategy selection happens at initialization only (not runtime switchable)

## Implementation Details

### Key Changes
1. **New enum**: `ControllerStrategy` in `com.liftsimulator.domain` package
2. **New factory**: `ControllerFactory` in `com.liftsimulator.engine` package
3. **Configuration field**: `controllerStrategy` in `ScenarioDefinition`
4. **Parser support**: `ScenarioParser` handles `controller_strategy:` configuration
5. **Main entry points**: Updated `Main.java` and `ScenarioRunnerMain` to use factory
6. **Demo scenario**: Updated `demo.scenario` with explicit strategy

### Factory Implementation Pattern
```java
return switch (strategy) {
    case NEAREST_REQUEST_ROUTING -> new NaiveLiftController(
        homeFloor, idleTimeoutTicks, idleParkingMode);
    case DIRECTIONAL_SCAN -> throw new UnsupportedOperationException(
        "DIRECTIONAL_SCAN controller strategy is not yet implemented");
};
```

### Test Coverage
- **ControllerFactoryTest**: 6 tests covering factory instantiation, null handling, and unsupported strategies
- **ScenarioIntegrationTest**: 4 new tests verifying:
  - Controller strategy parsing from scenario files
  - Null strategy for scenarios without configuration
  - Correct controller instance creation
  - Invalid strategy error handling

## Migration Impact
- **Added**: `ControllerStrategy` enum in domain package
- **Added**: `ControllerFactory` class in engine package
- **Updated**: `ScenarioDefinition` (new field, getter, and constructor parameter)
- **Updated**: `ScenarioParser` (parse `controller_strategy:` configuration)
- **Updated**: `ScenarioRunnerMain` (use factory with strategy from scenario)
- **Updated**: `Main.java` (use factory with explicit NEAREST_REQUEST_ROUTING)
- **Updated**: `demo.scenario` (added controller_strategy configuration)
- **Added**: `ControllerFactoryTest` with 6 unit tests
- **Updated**: `ScenarioIntegrationTest` with 4 integration tests
- **Added**: `invalid_controller_strategy.scenario` test resource
- **Updated**: CHANGELOG.md with version 0.14.0 entry
- **Updated**: README.md with controller strategy documentation
- **No changes needed**: Existing controllers (NaiveLiftController, SimpleLiftController)

## Future Considerations
- **DIRECTIONAL_SCAN implementation**: Complete the directional scan controller
- **Additional strategies**:
  - LOOK algorithm (optimized scan)
  - SSTF (Shortest Seek Time First)
  - Destination dispatch
  - Zone-based scheduling
- **Performance metrics**: Add benchmarking to compare strategy efficiency
- **Dynamic strategy switching**: Support runtime controller strategy changes
- **Hybrid strategies**: Combine multiple algorithms based on traffic patterns
- **Machine learning optimization**: Learn optimal strategy based on usage patterns

## References
- Related to ADR-0001 (Tick-Based Simulation) - strategies must work with tick-based model
- Related to ADR-0002 (Single Source of Truth) - factory is single source for controller creation
- Related to ADR-0004 (Configurable Idle Parking) - strategies must support idle parking modes
- Implements factory pattern from "Design Patterns" (Gamma et al.)
- SCAN/elevator algorithm: Denning, P. J. (1967). "Effects of scheduling on file memory operations"
- Addresses requirement: "Controller strategy is selectable at system setup"
