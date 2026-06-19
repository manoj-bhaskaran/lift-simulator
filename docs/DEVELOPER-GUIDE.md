# Developer Guide

This guide contains developer-facing details for contributors who extend the lift simulator internals. It preserves the simulation engine, request model, state machine, and persistence reference material that used to live in the README.

## Simulation Engine — Tick Timing

You can model travel and door timing by using the `SimulationEngine` builder:

```java
SimulationEngine engine = SimulationEngine.builder(controller, 0, 10)
    .travelTicksPerFloor(3)
    .doorTransitionTicks(2)
    .doorDwellTicks(3)
    .doorReopenWindowTicks(2)
    .build();
```

- **travelTicksPerFloor**: How many ticks it takes to move one floor
- **doorTransitionTicks**: Ticks required for doors to fully open or close
- **doorDwellTicks**: How long doors remain open before automatically closing
- **doorReopenWindowTicks**: Time window (in ticks) during door closing when doors can be reopened for new requests at the current floor
  - Must be non-negative and cannot exceed `doorTransitionTicks`
  - Default: `min(2, doorTransitionTicks)` for backward compatibility
  - Setting to 0 disables door reopening (doors cannot be interrupted once closing starts)
  - Realistic behavior: if a request arrives for the current floor while doors are closing and within this window, doors will reopen
  - If the window has passed, the request is queued normally and will be served in the next cycle

## Simulation Engine — Idle Parking

You can configure the home floor, idle timeout, and parking behavior for the naive controller:

```java
NaiveLiftController controller = new NaiveLiftController(
    0,                                        // homeFloor
    5,                                        // idleTimeoutTicks
    IdleParkingMode.PARK_TO_HOME_FLOOR       // idleParkingMode
);
```

- **homeFloor**: The floor to park on when idle (used only with `PARK_TO_HOME_FLOOR` mode)
- **idleTimeoutTicks**: How many idle ticks before the parking behavior activates (0 means activate immediately)
- **idleParkingMode**: The parking behavior when idle timeout is reached (optional, defaults to `PARK_TO_HOME_FLOOR`)
  - `IdleParkingMode.STAY_AT_CURRENT_FLOOR`: Lift stays at current floor indefinitely when idle
  - `IdleParkingMode.PARK_TO_HOME_FLOOR`: Lift moves to home floor after idle timeout (existing behavior)

**Backward compatibility**: The two-parameter constructor defaults to `PARK_TO_HOME_FLOOR` mode:

```java
// This uses PARK_TO_HOME_FLOOR mode by default
NaiveLiftController controller = new NaiveLiftController(0, 5);
```

**Scenario file configuration:**

```
home_floor: 0
idle_timeout_ticks: 5
idle_parking_mode: STAY_AT_CURRENT_FLOOR
```

The `idle_parking_mode` parameter is optional and defaults to `PARK_TO_HOME_FLOOR` if not specified.

## Simulation Engine — Controller Strategy Selection

You can configure which controller algorithm the lift uses via the `ControllerStrategy` enum:

```java
// Create controller using factory with desired strategy
LiftController controller = ControllerFactory.createController(
    ControllerStrategy.NEAREST_REQUEST_ROUTING,
    0,                                        // homeFloor
    5,                                        // idleTimeoutTicks
    IdleParkingMode.PARK_TO_HOME_FLOOR       // idleParkingMode
);
```

**Available strategies:**
- `ControllerStrategy.NEAREST_REQUEST_ROUTING`: Services the nearest request first (default, uses `NaiveLiftController`)
- `ControllerStrategy.DIRECTIONAL_SCAN`: Directional scan/elevator algorithm (uses `DirectionalScanLiftController`)

**Factory methods:**

```java
// With default parameters (home floor 0, idle timeout 5 ticks, park to home floor)
LiftController controller = ControllerFactory.createController(
    ControllerStrategy.NEAREST_REQUEST_ROUTING
);

// With custom parameters
LiftController controller = ControllerFactory.createController(
    ControllerStrategy.NEAREST_REQUEST_ROUTING,
    homeFloor,
    idleTimeoutTicks,
    idleParkingMode
);
```

**Scenario file configuration:**

```
controller_strategy: NEAREST_REQUEST_ROUTING
```

The `controller_strategy` parameter is optional and defaults to `NEAREST_REQUEST_ROUTING` if not specified.

**Notes:**
- The controller strategy must be selected at system initialization (not runtime switchable)
- Invalid strategy names in scenario files will throw an `IllegalArgumentException`

## Simulation Engine — Controller Strategies

### Nearest Request Routing (NaiveLiftController)

The nearest request routing strategy services the closest requested floor first, regardless of direction. This is a simple but inefficient algorithm that can result in excessive back-and-forth movement.

**Behavior:**
- Selects the nearest floor with a pending request
- Services requests immediately upon arrival
- No batching or direction preference
- Best suited for low-traffic scenarios

**Use case:** Simple lifts with minimal traffic where efficiency isn't critical.

### Directional Scan (DirectionalScanLiftController)

The directional scan strategy implements a SCAN-style algorithm that continues in the current direction until all requests in that direction are serviced, then reverses.

**Behavior:**
- **Direction commitment:** Once moving in a direction, continues until no more requests exist ahead
- **Hall call filtering:** Only services hall calls that match the current travel direction
  - Example: While going UP, only services hall calls with direction=UP
  - Exception: Car calls are always eligible (passengers already onboard)
- **Reversal logic:** Reverses at the furthest pending stop in the current direction
- **Efficient batching:** Reduces back-and-forth movement by servicing multiple requests in one sweep
- **Direction selection:** When idle, selects initial direction based on nearest request

**Example scenario:**
```
Lift at floor 0, requests:
- Hall call: floor 2, UP
- Car call: floor 5
- Hall call: floor 3, DOWN

Execution:
1. Select UP direction (floor 2 is nearest)
2. Service floor 2 (hall call UP) ✓
3. Continue to floor 5 (car call) ✓
4. No more requests going UP, reverse to DOWN
5. Service floor 3 (hall call DOWN) ✓
```

**Advantages:**
- Reduces average wait time in moderate to high traffic
- Minimizes unnecessary direction changes
- More predictable behavior for passengers
- Better energy efficiency

**Use case:** Most real-world elevator scenarios, especially multi-floor buildings with moderate traffic.

**Key invariants maintained:**
- No duplicate servicing of requests
- No lost requests during movement
- Compatible with door open/close timing semantics
- Requests can be added during movement and will be scheduled according to rules

## Taking Lifts Out of Service

You can take a lift out of service for maintenance or emergency situations:

```java
// Take lift out of service
controller.takeOutOfService();  // Cancels all pending requests
engine.setOutOfService();       // Transitions to OUT_OF_SERVICE state

// ... lift is now offline and cannot move or accept requests ...

// Return to service
controller.returnToService();   // Prepares controller for normal operation
engine.returnToService();       // Transitions to IDLE state
```

**Behavior when taking out of service (graceful shutdown):**
- All pending requests (QUEUED, ASSIGNED, SERVING) are immediately cancelled
- If the lift is moving, it completes movement to the next floor in its current direction
- Doors open to allow passengers to exit safely
- Doors close after dwell time
- Lift transitions to OUT_OF_SERVICE state
- While OUT_OF_SERVICE: cannot move, open doors, or accept new requests (new assignments are ignored)

**Behavior when returning to service:**
- Lift transitions to IDLE state at its current floor
- Can immediately accept and service new requests
- Operates normally as if freshly initialized

**Use cases:**
- Emergency stop situations
- Scheduled maintenance windows
- Simulating equipment failures
- Testing failover scenarios in multi-lift systems

## Request Types: Hall Calls vs. Car Calls

The simulator distinguishes between two types of lift requests, modeling real-world elevator behavior:

### Hall Call (Request from outside the lift)

Made when someone presses an up/down button **outside** the lift:

```java
controller.addHallCall(new HallCall(3, Direction.UP));
```

**Known information:**
- **Origin floor**: Where the person is waiting (floor 3)
- **Direction**: Where they want to go (UP or DOWN)
- **Unknown**: Exact destination floor (person hasn't boarded yet)

**Physical analog:** The up/down buttons on each floor

**Completion:** Request completes when lift arrives at the origin floor and opens doors (person can now board)

### Car Call (Request from inside the lift)

Made when someone presses a floor button **inside** the lift:

```java
controller.addCarCall(new CarCall(7));
```

**Known information:**
- **Destination floor**: Where the person wants to go (floor 7)
- **Unknown/Irrelevant**: Origin floor, direction (inferred from current position)

**Physical analog:** The numbered floor buttons inside the lift car

**Completion:** Request completes when lift arrives at the destination floor and opens doors (person can now exit)

### Why the Distinction Matters

#### 1. Current Naive Algorithm
The current `NaiveLiftController` treats both types similarly (goes to nearest floor), but the infrastructure supports future smart algorithms.

#### 2. Future Direction-Aware Scheduling
Smart algorithms can optimize based on hall call direction:

**Example scenario:**
- Lift at floor 0
- Hall call: floor 3 going DOWN
- Hall call: floor 5 going UP
- Car call: floor 7

**Naive:** 0 → 3 → 5 → 7 (inefficient backtracking)

**Smart:** 0 → 5 (pick up UP) → 7 (drop off) → 3 (now going down, pick up DOWN)

#### 3. Real-World Modeling
Elevator panels have different buttons:
- **Hall panels:** Only up/down buttons (direction matters)
- **Car panels:** Only floor buttons (destination matters)

#### 4. Multiple Requests at Same Floor
If two people at floor 4 press different buttons:
- Person A presses UP
- Person B presses DOWN

These should be **separate requests** because they'll board at different times (when lift is going their direction).

### Unified Request Model

The `LiftRequest` class unifies both types while preserving the distinction:

```java
// Hall call
LiftRequest hallRequest = LiftRequest.hallCall(5, Direction.UP);
// Has: type=HALL_CALL, originFloor=5, direction=UP, destinationFloor=null

// Car call
LiftRequest carRequest = LiftRequest.carCall(10);
// Has: type=CAR_CALL, originFloor=null, destinationFloor=10, direction=null
```

This architecture enables future algorithms like SCAN, LOOK, and destination dispatch while maintaining backward compatibility.

## Lift State Machine

The lift uses a **single source of truth** pattern where `LiftStatus` is the only stored state - all other properties (direction, door state) are derived from it.

### States

| State | Description | Direction | Doors |
|-------|-------------|-----------|-------|
| **IDLE** | Stationary, ready to accept requests | IDLE | CLOSED |
| **MOVING_UP** | Traveling upward between floors | UP | CLOSED (locked) |
| **MOVING_DOWN** | Traveling downward between floors | DOWN | CLOSED (locked) |
| **DOORS_OPENING** | Doors in process of opening (transitional) | IDLE | CLOSED |
| **DOORS_OPEN** | Doors fully open, passengers can enter/exit | IDLE | OPEN |
| **DOORS_CLOSING** | Doors in process of closing (transitional) | IDLE | CLOSED |
| **OUT_OF_SERVICE** | Offline for maintenance or emergency | IDLE | CLOSED |

### State Transition Table

| From ↓ / To → | IDLE | MOVING_UP | MOVING_DOWN | DOORS_OPENING | DOORS_OPEN | DOORS_CLOSING | OUT_OF_SERVICE |
|---------------|------|-----------|-------------|---------------|------------|---------------|----------------|
| **IDLE** | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ✅ |
| **MOVING_UP** | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |
| **MOVING_DOWN** | ✅ | ❌ | ✅ | ❌ | ❌ | ❌ | ✅ |
| **DOORS_OPENING** | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ |
| **DOORS_OPEN** | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ | ✅ |
| **DOORS_CLOSING** | ✅ | ❌ | ❌ | ✅ | ❌ | ✅ | ✅ |
| **OUT_OF_SERVICE** | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |

### Safety Constraints

The state machine enforces critical safety rules:
- **Cannot move with doors open**: DOORS_OPEN/DOORS_OPENING/DOORS_CLOSING cannot transition to MOVING_UP/MOVING_DOWN
- **Cannot reverse direction**: Must stop (IDLE) before changing direction
- **Cannot open doors while moving**: MOVING_UP/MOVING_DOWN must first transition to IDLE (stop), then to DOORS_OPENING
- **Doors must complete transitions**: DOORS_OPENING cannot transition directly to IDLE; must go through DOORS_OPEN → DOORS_CLOSING → IDLE
- **Symmetric door transitions**: Both opening and closing are modeled as separate states
- **All transitions validated**: Invalid transitions are prevented and logged

Valid transitions are managed by the `StateTransitionValidator` class, which ensures the lift operates safely and predictably.


## JPA Entities and Repositories

The backend includes JPA entities and Spring Data repositories for database access:

#### Entities

- **LiftSystem** (`com.liftsimulator.admin.entity.LiftSystem`)
  - Maps to `lift_system` table
  - Root configuration records for lift systems
  - Manages one-to-many relationship with versions
  - Automatic timestamp management via `@PrePersist` and `@PreUpdate`

- **LiftSystemVersion** (`com.liftsimulator.admin.entity.LiftSystemVersion`)
  - Maps to `lift_system_version` table
  - Versioned lift configuration payloads
  - **JSONB field mapping**: Uses `@JdbcTypeCode(SqlTypes.JSON)` for PostgreSQL JSONB support
  - Version status enum: DRAFT, PUBLISHED, ARCHIVED
  - Helper methods: `publish()`, `archive()`

- **Scenario** (`com.liftsimulator.admin.entity.Scenario`)
  - Maps to `scenario` table
  - Reusable test scenarios for lift system testing
  - **JSONB field mapping**: Stores scenario configuration as JSON
  - Automatic timestamp management via `@PrePersist` and `@PreUpdate`

- **SimulationRun** (`com.liftsimulator.admin.entity.SimulationRun`)
  - Maps to `simulation_run` table
  - Individual simulation run executions with lifecycle tracking
  - Run status enum: CREATED, RUNNING, SUCCEEDED, FAILED, CANCELLED
  - Relationships: Many-to-one with LiftSystem, LiftSystemVersion, and Scenario
  - Status transition methods: `start()`, `succeed()`, `fail()`, `cancel()`
  - Progress tracking via repository-backed current tick updates

#### Repositories

- **LiftSystemRepository** (`com.liftsimulator.admin.repository.LiftSystemRepository`)
  - Find by system key: `findBySystemKey(String systemKey)`
  - Check existence: `existsBySystemKey(String systemKey)`
  - Standard CRUD operations via `JpaRepository`

- **LiftSystemVersionRepository** (`com.liftsimulator.admin.repository.LiftSystemVersionRepository`)
  - Find versions by lift system: `findByLiftSystemIdOrderByVersionNumberDesc(Long liftSystemId)`
  - Find specific version: `findByLiftSystemIdAndVersionNumber(Long liftSystemId, Integer versionNumber)`
  - Find published versions: `findByLiftSystemIdAndIsPublishedTrue(Long liftSystemId)`
  - Find by status: `findByStatus(VersionStatus status)`
  - Get max version number: `findMaxVersionNumberByLiftSystemId(Long liftSystemId)`

- **ScenarioRepository** (`com.liftsimulator.admin.repository.ScenarioRepository`)
  - Find by name: `findByName(String name)`
  - Find by name pattern: `findByNameContainingIgnoreCase(String name)`
  - Check existence: `existsByName(String name)`
  - Standard CRUD operations via `JpaRepository`

- **SimulationRunRepository** (`com.liftsimulator.admin.repository.SimulationRunRepository`)
  - Persists simulation run lifecycle changes for both API orchestration and asynchronous execution, avoiding service-to-service circular dependencies
  - Update run progress directly: `updateCurrentTick(Long id, Long currentTick)`
  - Find runs by lift system: `findByLiftSystemIdOrderByCreatedAtDesc(Long liftSystemId)`
  - Find runs by version: `findByVersionIdOrderByCreatedAtDesc(Long versionId)`
  - Find runs by scenario: `findByScenarioIdOrderByCreatedAtDesc(Long scenarioId)`
  - Find runs by status: `findByStatusOrderByCreatedAtDesc(RunStatus status)`
  - Find active runs: `findActiveRunsByLiftSystemId(Long liftSystemId)`
  - Count operations: `countByLiftSystemId(Long liftSystemId)`, `countByStatus(RunStatus status)`

#### Verifying JPA Operations

To verify the JPA entities and repositories are working correctly, run the Spring Boot application with the JPA verification runner:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.jpa.verify=true"
```

Or with the JAR:

```bash
java -jar target/lift-simulator-0.52.2.jar --spring.jpa.verify=true
```

The verification runner will:
1. Create and retrieve `LiftSystem` entities
2. Create and retrieve `LiftSystemVersion` entities with JSONB configs
3. Test complex JSON configurations
4. Verify entity relationships and cascading
5. Test all custom query methods

Look for log output like:
```
=== Starting JPA Entity and Repository Verification ===
--- Verifying LiftSystem CRUD Operations ---
✓ Created LiftSystem: id=1, key=demo-system
✓ Found LiftSystem by ID: demo-system
--- Verifying JSONB Field Mapping ---
✓ Saved complex JSONB config: id=2
✓ Retrieved JSONB config matches original
=== JPA Verification Completed Successfully ===
```

The verification runner is located at `com.liftsimulator.admin.runner.JpaVerificationRunner` and is only enabled when `spring.jpa.verify=true` is set.

#### Integration Tests

Integration tests for the repositories are available:
- `LiftSystemRepositoryTest`: Tests CRUD operations, queries, and updates
- `LiftSystemVersionRepositoryTest`: Tests version operations, JSONB mapping, and relationships
- `FlywayMigrationIntegrationTest`: Verifies that Flyway migrations are executed at startup

**Test Database Configuration:**
- Integration tests run against a **real PostgreSQL** instance provisioned on demand by
  [Testcontainers](https://java.testcontainers.org/) — **no pre-existing database is required**.
- The only prerequisite is a running **Docker** daemon (Testcontainers starts and tears down a
  `postgres:15-alpine` container automatically). A single container is shared across the suite to
  keep startup fast.
- **Flyway migrations run during test startup**, so migration bugs are caught by the test suite.
- Hibernate runs in **`validate`** mode (not `ddl-auto: update`), so the entity mappings are
  checked against the migrated schema and any drift fails the build.
- In CI, where the workflow provides a PostgreSQL service container via `SPRING_DATASOURCE_URL`,
  Testcontainers stands aside and that database is used instead.

Run the tests:
```bash
mvn test -Dtest=LiftSystemRepositoryTest,LiftSystemVersionRepositoryTest
```

## Frontend API Authentication

The React admin UI uses `frontend/src/api/client.js` for backend calls. During local development, create `frontend/.env.local` with credentials that match `src/main/resources/application-dev.yml` or the backend environment variables:

```bash
VITE_ADMIN_USERNAME=admin
VITE_ADMIN_PASSWORD=local-admin-password
VITE_API_KEY=local-api-key
```

When both admin values are present, the Axios client sends an HTTP Basic `Authorization` header. When `VITE_API_KEY` is present, it sends the runtime `X-API-Key` header. The backend ignores the header that is irrelevant to a given endpoint, so the shared client can send both defaults. `frontend/.env.local` is covered by the frontend `*.local` ignore rule; never commit real credentials, and remember that Vite exposes `VITE_*` variables to browser bundles.

## Git Hooks

A pre-commit hook lives in `.githooks/pre-commit`. It fires automatically whenever `pom.xml` is staged and keeps version references in `README.md`, `docs/*.md`, and `frontend/package.json` in sync — so you never need to update them by hand.

Activate it once after cloning:

```bash
git config core.hooksPath .githooks
```

**What the hook does:** when it detects a version change in the staged `pom.xml`, it runs `mvn generate-resources` to update `README.md` and `docs/*.md`, updates `frontend/package.json` (via `npm version` if available, otherwise Python), and re-stages all affected files before the commit is recorded.

**CI safety net:** the CI pipeline includes a version-consistency check that fails the build if any of these files are out of sync with `pom.xml`, catching the rare case where the hook was bypassed with `--no-verify`.
