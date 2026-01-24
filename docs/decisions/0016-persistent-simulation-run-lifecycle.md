# ADR-0016: Persistent Simulation Run Lifecycle with Status-Based Polling

**Date**: 2026-01-20

**Status**: Accepted

## Context

The Lift Simulator application needs a mechanism for the UI to:
1. Initiate simulation runs against specific lift system configurations
2. Track simulation execution progress and status
3. Retrieve simulation results when complete
4. Handle simulation failures gracefully
5. Support reusable test scenarios across different configurations

Previously, simulations could only be run from the command-line with immediate synchronous execution. As the application evolves toward a web-based UI, we need a persistent run lifecycle that allows:
- **Asynchronous execution**: UI can start a run and poll for completion
- **State recovery**: Simulations survive server restarts
- **Historical tracking**: View past simulation runs and their outcomes
- **Scenario reusability**: Define test scenarios once, run against multiple systems

The key design questions are:
- How should simulation state be tracked and stored?
- What status model best represents the simulation lifecycle?
- Should scenarios be stored separately or embedded in runs?
- How will the UI poll for progress and completion?
- What happens to runs when referenced systems are deleted?

## Decision

We will implement a **persistent simulation run lifecycle** using two new database tables with a **status-based state machine** for run tracking. The UI will use **polling-based status checks** to monitor progress.

### Implementation Details

#### 1. Database Schema (V3 Migration)

**`simulation_scenario` table**:
- Stores reusable test scenarios independent of lift systems
- JSONB `scenario_json` field for flexible scenario configuration
- Indexed on `name` for fast lookups
- Separate table allows scenarios to be reused across multiple systems

**`simulation_run` table**:
- Tracks individual simulation executions
- Foreign keys to `lift_system`, `lift_system_version`, and `simulation_scenario`
- Status-based state machine with controlled transitions
- Progress tracking via `current_tick` and `total_ticks`
- Nullable `scenario_id` supports both ad-hoc and scenario-based runs
- `ON DELETE CASCADE` for `lift_system_id` and `version_id` (runs deleted with parent)
- `ON DELETE SET NULL` for `scenario_id` (runs preserved when scenario deleted)

#### 2. Run Status State Machine

**Status enum (`RunStatus`)**:
```
CREATED → RUNNING → SUCCEEDED
                  → FAILED
                  → CANCELLED (from CREATED or RUNNING)
```

**Status definitions**:
- **CREATED**: Run initialized but not yet started (allows configuration before execution)
- **RUNNING**: Simulation actively executing (started_at timestamp set)
- **SUCCEEDED**: Simulation completed successfully (ended_at timestamp set)
- **FAILED**: Simulation encountered an error (ended_at and error_message set)
- **CANCELLED**: Run was cancelled by user or system (ended_at timestamp set)

**Transition rules** (enforced in `SimulationRun` entity):
- `start()`: CREATED → RUNNING (sets `started_at`)
- `succeed()`: RUNNING → SUCCEEDED (sets `ended_at`)
- `fail(errorMessage)`: RUNNING → FAILED (sets `ended_at`, `error_message`)
- `cancel()`: CREATED|RUNNING → CANCELLED (sets `ended_at`)
- Invalid transitions throw `IllegalStateException`

#### 3. JPA Entities

**`SimulationScenario` entity**:
- Maps to `simulation_scenario` table
- JSONB field stored as `String` (same pattern as `LiftSystemVersion.config`)
- No relationships to runs (scenarios are independent)
- Automatic timestamp management

**`SimulationRun` entity**:
- Maps to `simulation_run` table
- Many-to-one relationships (lazy-loaded) to `LiftSystem`, `LiftSystemVersion`, `SimulationScenario`
- Business methods for status transitions (encapsulate state machine logic)
- `updateProgress(Long tick)` method for progress tracking
- Nullable `scenario` field supports ad-hoc runs

#### 4. Service Layer

**`SimulationScenarioService`**:
- CRUD operations for scenarios
- Standard validation and error handling

**`SimulationRunService`**:
- Create runs (with or without scenarios)
- Status transition methods: `startRun()`, `succeedRun()`, `failRun()`, `cancelRun()`
- Progress tracking: `updateProgress()`
- Query methods: by lift system, status, active runs
- Configuration method: `configureRun()` (set total_ticks, seed, artefact_base_path)

#### 5. Polling Model

The UI will poll for status updates using these query patterns:
1. **Active runs**: `GET /api/runs?status=CREATED,RUNNING` (check for completion)
2. **Specific run**: `GET /api/runs/{id}` (get current status and progress)
3. **Run history**: `GET /api/runs?liftSystemId={id}` (view past runs)

**Polling frequency recommendations**:
- Short simulations (< 1 min): Poll every 1-2 seconds
- Long simulations (> 1 min): Poll every 5 seconds
- Use exponential backoff if desired

## Alternatives Considered

### Alternative 1: Event-Driven Architecture with WebSockets

**Description**: Push simulation status updates to UI via WebSockets or Server-Sent Events (SSE)

**Pros**:
- Real-time updates without polling
- Lower latency for status changes
- More efficient (no unnecessary poll requests)
- Better user experience

**Cons**:
- Adds WebSocket infrastructure complexity
- Requires connection management and reconnection logic
- Doesn't survive server restarts without additional persistence
- Harder to debug and test
- Increases backend complexity

**Why rejected**:
- Polling is sufficient for simulation use case (updates every few seconds acceptable)
- Simpler implementation and testing
- Can add WebSockets later if needed without schema changes
- Database persistence provides state recovery that WebSockets alone cannot

### Alternative 2: In-Memory Execution Tracking

**Description**: Track running simulations in-memory (e.g., `Map<Long, SimulationExecution>`)

**Pros**:
- Very fast status lookups
- No database overhead during execution
- Simpler transaction handling

**Cons**:
- Lost on server restart
- Can't track historical runs
- No audit trail
- Doesn't support distributed deployments
- UI can't recover run status after browser refresh

**Why rejected**: Persistence is critical for production use cases. Users need to see historical results, and runs must survive server restarts.

### Alternative 3: Single Status with Substatus

**Description**: Use one `status` field (e.g., "RUNNING") with a separate `substatus` field for details (e.g., "INITIALIZING", "EXECUTING", "FINALIZING")

**Pros**:
- More granular state information
- Could show detailed progress stages

**Cons**:
- Overcomplicates the state model
- Adds confusion about which field to check
- Most simulations don't need this granularity
- Can add later if needed

**Why rejected**: Five states (CREATED, RUNNING, SUCCEEDED, FAILED, CANCELLED) are sufficient for initial implementation. Can add substatus later if needed.

### Alternative 4: Embedded Scenario JSON in Run

**Description**: Store scenario JSON directly in `simulation_run` table (no separate `simulation_scenario` table)

**Pros**:
- Simpler schema (one less table)
- No foreign key to manage
- Captures exact scenario used for each run

**Cons**:
- Duplicates scenario data across runs
- Can't update scenarios centrally
- Can't reuse scenarios across systems
- Harder to query "all runs of scenario X"

**Why rejected**: Separate `simulation_scenario` table enables scenario reusability and avoids duplication. The nullable foreign key supports both ad-hoc and scenario-based runs.

### Alternative 5: Message Queue for Long-Running Tasks

**Description**: Use message queue (RabbitMQ, AWS SQS) for simulation execution with workers

**Pros**:
- Better for long-running tasks
- Horizontal scalability
- Retry and dead-letter queue support
- Decouples execution from API

**Cons**:
- Adds infrastructure dependency
- More complex deployment
- Overkill for current scale
- Still needs database persistence for status

**Why rejected**: Current simulation execution times don't justify message queue complexity. Can migrate to this model later if simulations become very long-running or require scaling.

## Consequences

### Positive

1. **Persistent State**: Simulations survive server restarts and database backup/restore
2. **Historical Tracking**: Full audit trail of all simulation runs with timestamps
3. **Scenario Reusability**: Define scenarios once, run against multiple systems/versions
4. **Clear State Machine**: Five states with controlled transitions prevent invalid states
5. **Progress Tracking**: `current_tick` and `total_ticks` enable progress bars in UI
6. **Error Handling**: `error_message` field captures failure details for debugging
7. **Referential Integrity**: Cascade deletes prevent orphaned runs
8. **Testing Support**: Integration tests verify state transitions and persistence
9. **Simple Polling Model**: UI implementation is straightforward (no WebSocket complexity)
10. **Future-Proof**: Schema supports future additions (result storage, artefacts, etc.)

### Negative

1. **Polling Overhead**: UI must periodically query database for status updates
2. **Not Real-Time**: Latency between status change and UI update (acceptable trade-off)
3. **Database Load**: Frequent status polls could increase database load at scale
4. **No Push Notifications**: Users won't get immediate notifications of completion
5. **Manual Cleanup**: Old runs accumulate unless periodically pruned
6. **Transaction Complexity**: Long-running simulations hold database connections

### Neutral

1. **Polling Frequency**: UI must choose appropriate polling interval based on simulation duration
2. **Artefact Storage**: `artefact_base_path` is a string field; actual file storage strategy TBD
3. **Result Storage**: Schema doesn't yet define how simulation results are stored (future work)
4. **Concurrency**: No optimistic locking yet (consider `@Version` if needed)
5. **Cancellation**: CANCELLED status reserved for future use (not yet implemented in execution logic)

## Implementation Notes

1. **Status Transitions**: Enforced in `SimulationRun` entity methods, not at database level
2. **Timestamp Management**: `created_at` uses `@PrePersist` (no `@PreUpdate` as run records don't update in place)
3. **Cascade Behavior**:
   - `lift_system_id`: ON DELETE CASCADE (runs deleted with system)
   - `version_id`: ON DELETE CASCADE (runs deleted with version)
   - `scenario_id`: ON DELETE SET NULL (runs preserved, scenario reference nullified)
4. **Indexes**: Created on `status`, `lift_system_id`, `version_id`, `scenario_id`, `created_at` for query performance
5. **JSONB Storage**: Scenario JSON uses same pattern as `LiftSystemVersion.config` (String with `@JdbcTypeCode`)
6. **Nullable Scenario**: Supports both ad-hoc runs (no scenario) and scenario-based runs
7. **Seed Field**: `seed` field enables reproducible simulation runs

## Testing Strategy

### Repository Integration Tests

**`SimulationScenarioRepositoryTest`**:
- CRUD operations (save, find, update, delete)
- Name-based queries (`findByName`, `existsByName`)
- Pattern matching (`findByNameContainingIgnoreCase`)

**`SimulationRunRepositoryTest`**:
- Status transitions (CREATED → RUNNING → SUCCEEDED/FAILED)
- Invalid transition detection
- Relationship queries (by lift system, version, scenario)
- Status-based queries (by status, active runs)
- Progress tracking updates
- Cascade delete verification

### Service Unit Tests

**`SimulationScenarioServiceTest`**:
- Scenario CRUD with validation
- ResourceNotFoundException handling

**`SimulationRunServiceTest`**:
- Run creation (with and without scenario)
- Status transition methods (start, succeed, fail, cancel)
- Invalid transition error handling
- Progress updates
- Run configuration (total_ticks, seed, artefact_base_path)
- Query methods (by system, status, active runs)

### Test Coverage
- **23 repository integration tests** (H2 in-memory database)
- **29 service unit tests** (Mockito for repository mocking)
- Tests cover all state transitions and error cases

## Future Considerations

1. **Result Storage**: Add tables/fields for storing simulation results (request statistics, lift metrics, etc.)
2. **Artefact Management**: Define strategy for storing simulation output files (logs, traces, visualizations)
3. **WebSocket Support**: Add real-time push notifications for status changes if needed
4. **Run Pruning**: Implement automatic cleanup of old runs (retention policy)
5. **Optimistic Locking**: Add `@Version` field if concurrent updates become an issue
6. **Execution Integration**: Connect run lifecycle to actual simulation execution engine
7. **Progress Events**: Stream progress updates more frequently than status changes
8. **Cancellation Logic**: Implement actual cancellation of running simulations (currently CANCELLED is a terminal state but not actionable)
9. **Result Pagination**: Add pagination for run history queries
10. **Scenario Versioning**: Consider versioning scenarios to track changes over time
11. **Run Comparison**: Add features to compare results across multiple runs
12. **Scheduled Runs**: Add ability to schedule simulations for future execution

## API Design Considerations

### Suggested REST Endpoints (Future Work)

**Scenarios**:
- `POST /api/scenarios` - Create scenario
- `GET /api/scenarios` - List all scenarios
- `GET /api/scenarios/{id}` - Get scenario by ID
- `PUT /api/scenarios/{id}` - Update scenario
- `DELETE /api/scenarios/{id}` - Delete scenario

**Simulation Runs**:
- `POST /api/runs` - Create new run (with or without scenario)
- `GET /api/runs` - List runs (filterable by status, lift system, scenario)
- `GET /api/runs/{id}` - Get run details and current status
- `POST /api/runs/{id}/start` - Start a created run
- `POST /api/runs/{id}/cancel` - Cancel a running run
- `GET /api/runs/{id}/progress` - Get detailed progress (current_tick, total_ticks)
- `DELETE /api/runs/{id}` - Delete run (admin only)

**Polling Strategy**:
```javascript
// Example UI polling logic
async function pollRunStatus(runId) {
  const response = await fetch(`/api/runs/${runId}`);
  const run = await response.json();

  if (run.status === 'SUCCEEDED' || run.status === 'FAILED') {
    // Stop polling, show results
    return run;
  }

  // Continue polling after delay
  setTimeout(() => pollRunStatus(runId), 2000);
}
```

## References

- ADR-0006: Spring Boot Admin Backend
- ADR-0007: PostgreSQL and Flyway Integration
- ADR-0008: JPA Entities and JSONB Mapping
- Spring Data JPA: https://docs.spring.io/spring-data/jpa/docs/current/reference/html/
- PostgreSQL CASCADE DELETE: https://www.postgresql.org/docs/current/ddl-constraints.html#DDL-CONSTRAINTS-FK
- State Machine Pattern: https://refactoring.guru/design-patterns/state
