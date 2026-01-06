# ADR-0003: Request Lifecycle Management

## Status
Accepted

## Context
Prior to version 0.5.0, lift requests (`CarCall` and `HallCall`) were simple data records without explicit state tracking. The controller managed requests as sets of records and removed them when serviced, but there was no visibility into:
- Where a request is in its lifecycle (queued, being served, completed)
- Whether a request completed successfully or was cancelled
- Which requests are actively being processed vs. waiting

This lack of request state tracking created several issues:
- **No observability**: Cannot monitor request progress or identify stuck requests
- **No cancellation support**: No way to cancel a request once added
- **Implicit lifecycle**: Request states existed only in controller logic, not as explicit data
- **Limited testing**: Cannot verify requests progress through correct states
- **Poor debugging**: Cannot inspect request state when diagnosing issues

### Requirements
- Track each request as a distinct entity with a unique identifier
- Model request lifecycle with explicit states from creation to completion
- Ensure all requests reach a terminal state (completed or cancelled)
- Prevent invalid state transitions
- Maintain backward compatibility with existing `CarCall` and `HallCall` interfaces

### Options Considered

**Option 1: Status flag on existing records (Minimal Change)**
- Add a `RequestStatus` field to `CarCall` and `HallCall`
- Pros: Minimal code changes, backward compatible
- Cons: No unique ID, limited state modeling, duplicates logic across two record types

**Option 2: Wrapper objects with state**
- Keep `CarCall`/`HallCall` records, wrap them in a stateful `RequestTracker` class
- Pros: Preserves existing records, adds state tracking
- Cons: Two-tier model complexity, indirect access to request data

**Option 3: First-class request entity (Chosen)**
- Create unified `LiftRequest` class with ID, type, and lifecycle states
- Factory methods to create from `CarCall`/`HallCall` for compatibility
- Pros: Clean domain model, unique IDs, explicit lifecycle, testable
- Cons: Requires controller refactoring, new domain concepts

## Decision
We will adopt **Option 3** and create `LiftRequest` as a first-class entity with explicit lifecycle management:

### New Domain Models

**`RequestState` enum** - Six lifecycle states:
- `CREATED`: Request instantiated but not yet queued
- `QUEUED`: Waiting in controller queue for assignment
- `ASSIGNED`: Controller has targeted this request for service
- `SERVING`: Lift is actively traveling to or at the target floor
- `COMPLETED`: Request successfully fulfilled (terminal state)
- `CANCELLED`: Request cancelled before completion (terminal state)

**`RequestType` enum** - Distinguishes request origin:
- `HALL_CALL`: Request from a floor (hall button press)
- `CAR_CALL`: Request from inside lift (destination button press)

**`LiftRequest` class** - Unified request entity:
- Unique auto-generated ID (long)
- Request type (HALL_CALL or CAR_CALL)
- Origin floor (for hall calls and car calls with origin tracking)
- Destination floor (for car calls)
- Direction (for hall calls)
- Current state (RequestState)
- State transition validation
- Terminal state detection

### State Transition Rules

Valid transitions (enforced by `transitionTo()` method):

| From State | Valid Next States |
|------------|------------------|
| CREATED | QUEUED, CANCELLED |
| QUEUED | ASSIGNED, CANCELLED (or back to QUEUED) |
| ASSIGNED | SERVING, QUEUED, CANCELLED |
| SERVING | COMPLETED, CANCELLED |
| COMPLETED | *(none - terminal)* |
| CANCELLED | *(none - terminal)* |

**Transition constraints:**
- Self-transitions are forbidden (cannot go from QUEUED to QUEUED)
- Terminal states (COMPLETED, CANCELLED) cannot transition
- Invalid transitions throw `IllegalStateException`

### Controller Integration

`NaiveLiftController` manages the request lifecycle:
- **CREATED → QUEUED**: When `addCarCall()`, `addHallCall()`, or `addRequest()` is called
- **QUEUED → ASSIGNED**: When controller selects nearest floor target
- **ASSIGNED → SERVING**: When lift reaches target floor or starts serving
- **SERVING → COMPLETED**: When doors open at target floor
- **Cleanup**: Completed/cancelled requests are automatically removed

### Backward Compatibility

Existing controller methods maintained:
```java
controller.addCarCall(new CarCall(5));      // Still works
controller.addHallCall(new HallCall(3, UP)); // Still works
```

New direct request API:
```java
LiftRequest request = LiftRequest.carCall(5);
controller.addRequest(request);
```

## Consequences

### Positive
- **Explicit lifecycle**: Request states are now first-class domain concepts, not hidden in controller logic
- **Unique identification**: Each request has a unique ID for tracking and debugging
- **State validation**: Invalid transitions are impossible, preventing logic errors
- **Terminal state guarantee**: Every request must end in COMPLETED or CANCELLED
- **Better testing**: Can verify request state progression in unit tests
- **Observability**: Can inspect request state at any time for monitoring/debugging
- **Cancellation support**: Framework in place for cancelling requests (CANCELLED state)
- **Unified model**: Single `LiftRequest` handles both hall and car calls
- **Extensibility**: Easy to add new states or request metadata in the future

### Negative
- **Breaking change internally**: `NaiveLiftController` internals completely rewritten
- **Slight complexity**: More domain objects to understand (RequestState, RequestType, LiftRequest)
- **Memory overhead**: Each request now carries state, ID, and type information
- **Migration effort**: Future controllers must adopt the request lifecycle model

### Neutral
- Backward compatibility maintained through factory methods and compatibility methods
- `CarCall` and `HallCall` records still exist and are used externally
- Test suite expanded significantly (37 new test cases)
- Request lifecycle is managed automatically by controller, not manually

## Implementation Details

### Key Design Patterns
- **Factory pattern**: `LiftRequest.hallCall()` and `LiftRequest.carCall()` create properly typed requests
- **State pattern**: Encapsulated state transitions with validation
- **Value object**: Requests are identified by unique ID, state is mutable but controlled

### Safety Features
- Atomic ID generation with `AtomicLong`
- Immutable request metadata (type, origin, destination, direction)
- State transition validation on every `transitionTo()` call
- Terminal state detection with `isTerminal()` method

### Test Coverage
- **LiftRequestTest** (22 tests): Request creation, state transitions, validation
- **LiftRequestLifecycleTest** (15 tests): Controller integration, lifecycle progression

## Migration Impact
- **Updated**: `NaiveLiftController` (complete internal rewrite)
- **Added**: `LiftRequest`, `RequestState`, `RequestType` domain models
- **Added**: Comprehensive unit tests for request lifecycle
- **Updated**: CHANGELOG.md with version 0.5.0 entry
- **No changes needed**: External consumers using `CarCall`/`HallCall` (backward compatible)

## Future Considerations
- Request priority levels (express vs. normal)
- Request timeout handling (auto-cancel after N ticks)
- Request grouping (batch multiple floor requests)
- Request analytics (average time in each state)
- Multi-lift request assignment
- User/tenant association with requests

## References
- Related to ADR-0001 (Tick-Based Simulation) - states still advance per tick
- Related to ADR-0002 (Single Source of Truth) - RequestState is the single source of truth for request status
- Implements state pattern from "Design Patterns" (Gamma et al.)
- Follows domain-driven design principles (Evans, "Domain-Driven Design")
