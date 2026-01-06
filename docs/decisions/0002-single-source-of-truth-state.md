# ADR-0002: Single Source of Truth for Lift State

## Status
Accepted

## Context
In version 0.2.0, we introduced a formal state machine with `LiftStatus` enum to control lift behavior. However, the initial implementation created redundancy by storing three overlapping state representations in `LiftState`:
- `direction` (Direction enum: UP, DOWN, IDLE)
- `doorState` (DoorState enum: OPEN, CLOSED)
- `status` (LiftStatus enum: IDLE, MOVING_UP, MOVING_DOWN, DOORS_OPENING, DOORS_OPEN, DOORS_CLOSING, OUT_OF_SERVICE)

This redundancy created several problems:
- **Inconsistency risk**: Possible to have contradictory states (e.g., `status=MOVING_UP` with `doorState=OPEN`)
- **Maintenance burden**: Need to keep three fields synchronized when updating state
- **Validation complexity**: Must validate all three fields are consistent with each other
- **Memory waste**: Storing derived information that could be computed

### Options Considered

**Option 1: Keep all three fields (Status Quo)**
- Pros: Backward compatible, familiar API
- Cons: Redundant, error-prone, requires complex validation

**Option 2: Single source of truth with LiftStatus only**
- Pros: Eliminates redundancy, impossible to have invalid states, cleaner design
- Cons: Breaking change, requires updating all code that creates LiftState

**Option 3: Add validation to constructor**
- Pros: Catches inconsistencies early
- Cons: Still stores redundant data, adds runtime overhead

## Decision
We will adopt **Option 2** and make `LiftStatus` the single source of truth for lift state:
- `LiftState` stores only `floor` (int) and `status` (LiftStatus)
- `Direction` and `DoorState` are derived (computed) from `status` via getters
- Constructor signature changes from `LiftState(floor, direction, doorState, status)` to `LiftState(floor, status)`

Additionally, we are adding the `DOORS_OPENING` state to make door transitions symmetric:
- **DOORS_OPENING**: Doors in process of opening (transitional state)
- **DOORS_OPEN**: Doors fully open
- **DOORS_CLOSING**: Doors in process of closing (transitional state)
- **IDLE**: Doors closed, ready to move

### State-to-Property Mapping

| LiftStatus | Direction | DoorState |
|-----------|-----------|-----------|
| IDLE | IDLE | CLOSED |
| MOVING_UP | UP | CLOSED |
| MOVING_DOWN | DOWN | CLOSED |
| DOORS_OPENING | IDLE | CLOSED |
| DOORS_OPEN | IDLE | OPEN |
| DOORS_CLOSING | IDLE | CLOSED |
| OUT_OF_SERVICE | IDLE | CLOSED |

## Consequences

### Positive
- **No invalid states**: Impossible to create `LiftState` with contradictory properties
- **Reduced memory**: Store 1 enum instead of 3 (floor + status vs floor + direction + doorState + status)
- **Simpler validation**: Only need to validate `LiftStatus` transitions, not multiple field consistency
- **Clearer semantics**: Single status field is unambiguous
- **Easier testing**: Test state transitions with single field, not combinations
- **Symmetric door behavior**: DOORS_OPENING and DOORS_CLOSING states provide realistic door timing

### Negative
- **Breaking change**: All code creating `LiftState` must be updated
- **Slight performance cost**: Direction and door state are computed on each getter call (negligible for this application)
- **Less flexible**: Cannot represent theoretical states outside the state machine (acceptable trade-off)

### Neutral
- Tests must be updated to use new constructor signature
- Controllers still use `getDirection()` and `getDoorState()` methods, so their logic is unchanged
- `toString()` still shows all properties for debugging, but they're computed

## Migration Impact
- Updated: `LiftState` class (constructor and internal storage)
- Updated: `SimulationEngine` (removed direction/doorState references)
- Updated: `StateTransitionValidator` (added DOORS_OPENING transitions)
- Updated: All test files (new constructor signature)
- No changes needed: Controllers (still use getter methods)

## References
- Related to ADR-0001 (Tick-Based Simulation) - states still advance per tick
- Implements state machine pattern from "Design Patterns" (Gamma et al.)
- Follows "single source of truth" principle from Redux/Elm architecture
