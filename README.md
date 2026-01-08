# Lift Simulator

A Java-based simulation of lift (elevator) controllers with a focus on correctness and design clarity.

## Version

Current version: **0.12.9**

This project follows [Semantic Versioning](https://semver.org/). See [CHANGELOG.md](CHANGELOG.md) for version history.

## What is this?

The Lift Simulator is an iterative project to model and test lift controller algorithms. It simulates:
- Lift state and movement (floor position, direction, door state)
- Controller decision logic (choosing which floor to service next)
- Request handling (hall calls and car calls)
- Time-based simulation using discrete ticks

The simulation is text-based and designed for clarity over visual appeal.

## Features

The current version (v0.12.9) implements:
- **Out-of-service functionality**: Take lifts out of service safely for maintenance or emergencies, automatically cancelling all pending requests
- **Request lifecycle management**: Requests are first-class entities with explicit lifecycle states (CREATED → QUEUED → ASSIGNED → SERVING → COMPLETED/CANCELLED)
- **Request cancellation**: Cancel hall and car calls by request ID at any point before completion
- **Request state tracking**: Every request has a unique ID and progresses through validated state transitions
- **Formal lift state machine** with 7 explicit states (IDLE, MOVING_UP, MOVING_DOWN, DOORS_OPENING, DOORS_OPEN, DOORS_CLOSING, OUT_OF_SERVICE)
- **Single source of truth**: LiftStatus is the only stored state for the lift, all other properties are derived
- **State transition validation** ensuring only valid state changes occur (for both lift and requests)
- **Invalid action reporting** with explicit action results and warning logs that include tick and floor context
- **Symmetric door behavior**: Both opening and closing are modeled as transitional states
- **Door reopening window**: Configurable time window during which closing doors can be reopened for new requests at the current floor
- **Idle parking**: Configurable home floor and idle timeout to park the lift when no requests are pending
- **Single lift simulation** operating between configurable floor ranges
- **Tick-based simulation engine** that advances time in discrete steps
- **Simulation clock** powering deterministic tick progression
- **Configurable travel, door transition, and door dwell durations** to model time per floor and door cycles
- **Timed door dwell** with an automatic DOORS_OPEN → DOORS_CLOSING cycle
- **NaiveLiftController** - A simple controller that services the nearest pending request
- **Console output** displaying tick-by-tick lift state (floor, direction, door state, status, request lifecycle)
- **Request lifecycle visibility** in demo output with compact status display (Q:n, A:n, S:n)
- **Scenario runner** for scripted simulations with tick-based events and pending request logging
- **Request types**: Car calls (from inside the lift) and hall calls (from a floor)
- **Safety enforcement**: Lift cannot move with doors open, doors cannot open while moving
- **Backward compatibility**: Existing CarCall/HallCall interfaces still work

Future iterations will add multi-lift systems, smarter algorithms, request priorities, and more realistic constraints.

## Prerequisites

- Java 17 or later
- Maven 3.6 or later

## Development Setup

This project includes an `.editorconfig` file to maintain consistent code formatting across different editors and IDEs. Most modern editors support EditorConfig either natively or through plugins:

- **IntelliJ IDEA**: Built-in support (no plugin needed)
- **VS Code**: Install the "EditorConfig for VS Code" extension
- **Eclipse**: Install the EditorConfig Eclipse plugin
- **Vim/Neovim**: Install the editorconfig-vim plugin

The configuration enforces:
- UTF-8 encoding
- LF line endings (ensures compatibility across Windows, Linux, and macOS)
- 4-space indentation for Java and XML files
- Trailing whitespace removal
- Final newline insertion

## Building the Project

Compile the project using Maven:

```bash
mvn clean compile
```

To build a JAR package:

```bash
mvn clean package
```

The packaged JAR will be in `target/lift-simulator-0.12.9.jar`.

## Running Tests

Run the test suite with Maven:

```bash
mvn test
```

The test suite includes integration coverage for the scenario system using fixtures in
`src/test/resources/scenarios`.

## Quality Checks

Run code style checks:

```bash
mvn checkstyle:check
```

Run static analysis:

```bash
mvn spotbugs:check
```

Generate coverage reports while running tests:

```bash
mvn test jacoco:report
```

## Running the Simulation

Run the demo simulation:

```bash
mvn exec:java -Dexec.mainClass="com.liftsimulator.Main"
```

Or run directly after building:

```bash
java -cp target/lift-simulator-0.12.9.jar com.liftsimulator.Main
```

The demo runs a pre-configured scenario with several lift requests and displays the simulation state at each tick.

## Running Scripted Scenarios

Run the scenario runner with the bundled demo scenario:

```bash
mvn exec:java -Dexec.mainClass="com.liftsimulator.scenario.ScenarioRunnerMain"
```

Or run a custom scenario file:

```bash
java -cp target/lift-simulator-0.12.9.jar com.liftsimulator.scenario.ScenarioRunnerMain path/to/scenario.scenario
```

Scenario files are plain text with metadata and event lines:

```text
name: Demo scenario - multiple events
ticks: 30
min_floor: 0
max_floor: 10
initial_floor: 0
travel_ticks_per_floor: 1
door_transition_ticks: 2
door_dwell_ticks: 3
door_reopen_window_ticks: 2
home_floor: 0
idle_timeout_ticks: 5

0, car_call, req1, 3
2, hall_call, req2, 7, UP
4, car_call, req3, 5
10, cancel, req3
15, out_of_service
20, return_to_service
22, car_call, req4, 4
```

Each event executes at the specified tick, and the output logs the tick, floor, lift state, and pending requests to help validate complex behavior.
The scenario runner automatically expands the default floor range (0–10) to include any requested floors, so negative floors in scripted scenarios are supported without extra configuration.
If you set any of the scenario parameters (e.g., `door_dwell_ticks`), the scenario runner uses them to configure the controller and simulation engine.

Scenario metadata keys:

- **min_floor** / **max_floor**: floor bounds used for the simulation (still expanded to include requested floors)
- **initial_floor**: starting floor for the lift (clamped to the final min/max range)
- **travel_ticks_per_floor**: ticks required to travel one floor
- **door_transition_ticks**: ticks required to open or close doors
- **door_dwell_ticks**: ticks doors stay open before closing
- **door_reopen_window_ticks**: ticks during door closing when doors can reopen (0 disables)
- **home_floor**: idle parking floor for the naive controller
- **idle_timeout_ticks**: idle ticks before the lift parks at the home floor

Note: If a `return_to_service` event is scheduled while the lift is still completing the out-of-service shutdown sequence, the return is deferred until the lift reaches the `OUT_OF_SERVICE` state.

## Configuring Tick Timing

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

## Configuring Idle Parking

You can configure the home floor and idle timeout for the naive controller:

```java
NaiveLiftController controller = new NaiveLiftController(
    0, // homeFloor
    5  // idleTimeoutTicks
);
```

- **homeFloor**: The floor to park on when idle
- **idleTimeoutTicks**: How many idle ticks before the lift starts parking (0 means park immediately)

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

## Running Tests

Execute all tests:

```bash
mvn test
```

Run code style checks:

```bash
mvn checkstyle:check
```

Run static analysis:

```bash
mvn spotbugs:check
```

Run tests with coverage:

```bash
mvn test jacoco:report jacoco:check
```

The JaCoCo check enforces a minimum 80% line coverage threshold for the project.

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

## Project Structure

```
src/
├── main/java/com/liftsimulator/
│   ├── Main.java                          # Entry point and demo
│   ├── domain/                            # Core domain models
│   │   ├── Action.java                    # Actions the lift can take
│   │   ├── CarCall.java                   # Request from inside lift (legacy)
│   │   ├── Direction.java                 # UP, DOWN, IDLE
│   │   ├── DoorState.java                 # OPEN, CLOSED
│   │   ├── HallCall.java                  # Request from a floor (legacy)
│   │   ├── LiftRequest.java               # First-class request entity (NEW)
│   │   ├── LiftState.java                 # Immutable lift state
│   │   ├── LiftStatus.java                # Lift state machine enum
│   │   ├── RequestState.java              # Request lifecycle enum (NEW)
│   │   └── RequestType.java               # HALL_CALL or CAR_CALL (NEW)
│   └── engine/                            # Simulation engine and controllers
│       ├── LiftController.java            # Controller interface
│       ├── NaiveLiftController.java       # Simple nearest-floor controller
│       ├── SimpleLiftController.java      # Alternative basic controller
│       ├── SimulationClock.java           # Deterministic simulation clock
│       ├── SimulationEngine.java          # Tick-based simulation engine
│       └── StateTransitionValidator.java  # State machine validator
└── test/java/com/liftsimulator/
    ├── domain/
    │   └── LiftRequestTest.java           # Request lifecycle tests
    ├── engine/
    │   ├── LiftRequestLifecycleTest.java  # Controller integration tests
    │   ├── NaiveLiftControllerTest.java   # Controller unit tests
    │   ├── OutOfServiceTest.java          # Out-of-service tests
    │   └── SimulationEngineTest.java      # Engine unit tests
    └── ...                                # Additional tests
```

## Architecture Decisions

See [docs/decisions](docs/decisions) for Architecture Decision Records (ADRs):
- [ADR-0001: Tick-Based Simulation](docs/decisions/0001-tick-based-simulation.md)
- [ADR-0002: Single Source of Truth for Lift State](docs/decisions/0002-single-source-of-truth-state.md)
- [ADR-0003: Request Lifecycle Management](docs/decisions/0003-request-lifecycle-management.md)

## License

MIT License - see [LICENSE](LICENSE) file for details.
