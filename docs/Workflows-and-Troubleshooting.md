# Workflows and Troubleshooting

This guide explains how to run Lift Simulator simulations from the command line and the admin UI, where run artefacts are stored, how to reproduce UI-driven runs locally, and how to diagnose common simulation-run failures. It complements the API reference by focusing on operational workflows rather than endpoint schemas.

## Table of Contents

- [CLI and UI-Run Workflows](#cli-and-ui-run-workflows)
  - [Overview](#overview)
  - [CLI Usage (Unchanged)](#cli-usage-unchanged)
  - [UI-Driven Run Workflow](#ui-driven-run-workflow)
  - [Artefact Storage](#artefact-storage)
  - [Reproducing UI Runs via CLI](#reproducing-ui-runs-via-cli)
  - [Example Scenario Walkthrough: Morning Rush Hour](#example-scenario-walkthrough-morning-rush-hour)
  - [Troubleshooting](#troubleshooting)

## CLI and UI-Run Workflows

This guide documents how to run simulations using both the command-line interface (CLI) and the web UI. The CLI interface remains fully backward compatible with previous versions, while the new UI provides a streamlined workflow for running simulations with managed configurations and scenarios.

### Overview

The Lift Simulator supports two primary workflows:

1. **CLI Workflow (Backward Compatible)**: Run simulations directly from the command line using scenario files
2. **UI-Driven Workflow (New in v0.46.0)**: Run simulations through the web interface with Version + Scenario selection

Both workflows produce the same simulation results and use the same underlying simulation engine.

---

### CLI Usage (Unchanged)

The command-line interface remains **fully backward compatible** with previous versions. All existing scripts and automation will continue to work without modification.

**Available CLI Entry Points:**

1. **Demo Simulation** - Quick test with sample configuration
2. **Scenario Runner** - Run scripted scenarios from `.scenario` files
3. **Local Simulation** - Run with JSON configuration files

#### Scenario Runner (Primary CLI)

Run a simulation using a `.scenario` file:

```bash
java -cp target/lift-simulator-0.53.3.jar \
  com.liftsimulator.scenario.ScenarioRunnerMain \
  path/to/scenario.scenario
```

**Options:**
- `-h, --help` - Show help message

**Default Behavior:**
If no scenario file is provided, uses `demo.scenario` from the classpath.

**Example:**
```bash
# Run with a specific scenario file
java -cp target/lift-simulator-0.53.3.jar \
  com.liftsimulator.scenario.ScenarioRunnerMain \
  my-test-scenario.scenario

# Run with default demo scenario
java -cp target/lift-simulator-0.53.3.jar \
  com.liftsimulator.scenario.ScenarioRunnerMain
```

**Scenario File Format:**

Scenario files use a simple text format with metadata and events:

```
# Metadata (required)
ticks: 100
min_floor: 0
max_floor: 10
home_floor: 0
controller_strategy: NEAREST_REQUEST_ROUTING
idle_parking_mode: PARK_TO_HOME_FLOOR
travel_ticks_per_floor: 1
door_transition_ticks: 2
door_dwell_ticks: 3
door_reopen_window_ticks: 1
idle_timeout_ticks: 5

# Events: comma-separated fields - "<tick>, <event_type>, <parameters...>"
# (full-line "#" comments are skipped; inline trailing comments are not supported)
0, car_call, p1, 5
5, hall_call, p2, 3, UP
10, hall_call, p3, 8, DOWN
20, cancel, p2
```

Event rows are comma-delimited, and each request carries a unique alias (`p1`, `p2`, …) that later events (such as `cancel`) refer to.

**Event Types:**
- `<tick>, car_call, <alias>, <destination_floor>` - Passenger already inside a lift requesting a destination floor
- `<tick>, hall_call, <alias>, <floor>, <direction>[, <passengers>]` - Passenger(s) waiting at a floor (direction `UP` or `DOWN`; optional `passengers` count, default 1)
- `<tick>, cancel, <alias>` - Cancel a previously registered request by its alias
- `<tick>, out_of_service` - Take the lift out of service
- `<tick>, return_to_service` - Return the lift to service

#### Local Simulation with JSON Config

Run a simulation using a JSON configuration file:

```bash
java -cp target/lift-simulator-0.53.3.jar \
  com.liftsimulator.runtime.LocalSimulationMain \
  --config=path/to/config.json \
  --ticks=100
```

**Options:**
- `--config=PATH` - Path to configuration JSON file (required)
- `--ticks=N` - Number of ticks to simulate (default: 25)
- `-h, --help` - Show help message

**Example:**
```bash
java -cp target/lift-simulator-0.53.3.jar \
  com.liftsimulator.runtime.LocalSimulationMain \
  --config=building-a.json \
  --ticks=1000
```

#### Demo Simulation

Run a quick demo simulation with built-in configuration:

```bash
java -cp target/lift-simulator-0.53.3.jar \
  com.liftsimulator.Main \
  --strategy=directional-scan
```

**Options:**
- `--strategy=<strategy>` - Controller strategy to use
  - Valid values: `nearest-request`, `directional-scan`
  - Default: `nearest-request`
- `-h, --help` - Show help message

---

### UI-Driven Run Workflow (New in v0.46.0)

The web UI provides a streamlined workflow for running simulations with managed configurations and scenarios.

#### Workflow Steps

1. **Navigate to Simulator Page**
   - Access the simulator at `http://localhost:3000/simulator`

2. **Select Lift System**
   - Choose from configured lift systems (e.g., "Building A Lifts", "Building B Lifts")

3. **Select Published Version**
   - Choose a published version of the lift configuration
   - Dropdown shows only published versions for the selected system
   - Each version has configuration details (floors, lifts, strategy, etc.)

4. **Select Scenario**
   - Choose a scenario to run
   - Scenarios define passenger flows and events over time
   - View scenario details (duration, flow count)

5. **Configure Run (Optional)**
   - Set random seed for reproducible results (optional)
   - Default seed is generated if not specified

6. **Start Run**
   - Click "Start Run" button
   - Run begins executing asynchronously
   - Progress updates every 3 seconds

7. **Monitor Progress**
   - Real-time status: CREATED → RUNNING → SUCCEEDED/FAILED
   - Progress bar shows tick progress (e.g., "5000 / 10000 ticks")
   - Elapsed time displayed

8. **View Results**
   - KPI cards (pickup-request completions, pickup wait times, pickup-leg utilisation)
   - Per-lift metrics table
   - Per-floor metrics table
   - Download artefacts (logs, results, input files)

#### Run States

| State | Description |
|-------|-------------|
| `CREATED` | Run record created, waiting to start execution |
| `RUNNING` | Simulation actively executing |
| `SUCCEEDED` | Simulation completed successfully with results |
| `FAILED` | Simulation encountered an error and stopped |
| `CANCELLED` | User cancelled the run before completion |

#### API Workflow

Behind the scenes, the UI uses these APIs:

```bash
# 1. Create and start run
POST /api/simulation-runs
{
  "liftSystemId": 1,
  "versionNumber": 3,
  "scenarioId": 5,
  "seed": 12345
}

# 2. Poll for status (every 3 seconds)
GET /api/simulation-runs/{id}

# 3. Get results (when SUCCEEDED)
GET /api/simulation-runs/{id}/results

# 4. Get logs (if needed)
GET /api/simulation-runs/{id}/logs?tail=100

# 5. List artefacts
GET /api/simulation-runs/{id}/artefacts

# 6. Download an artefact
GET /api/simulation-runs/{id}/artefacts/{path}
```

---

### Artefact Storage

Each simulation run produces a set of artefacts stored in a run-specific directory.

#### Directory Structure

```
simulation-runs/
└── run-{runId}/
    ├── config.json       # Input: Lift configuration used for the run
    ├── scenario.json     # Input: Scenario with passenger flows
    ├── run.log           # Output: Execution log with timestamps
    └── results.json      # Output: Structured results with KPIs
```

**Configuration:**
```yaml
# Base directory for simulation artefacts (application.yml)
simulation:
  artefacts:
    base-path: ./simulation-runs
```

#### Artefact Files

**config.json** - Lift Configuration

The exact configuration used for the simulation run:

```json
{
  "minFloor": 0,
  "maxFloor": 10,
  "homeFloor": 0,
  "controllerStrategy": "NEAREST_REQUEST_ROUTING",
  "idleParkingMode": "PARK_TO_HOME_FLOOR",
  "travelTicksPerFloor": 1,
  "doorTransitionTicks": 2,
  "doorDwellTicks": 3,
  "doorReopenWindowTicks": -1,
  "idleTimeoutTicks": 5
}
```

**scenario.json** - Scenario Definition

The scenario with passenger flows used for the simulation:

```json
{
  "durationTicks": 1000,
  "seed": 12345,
  "passengerFlows": [
    {
      "startTick": 0,
      "originFloor": 0,
      "destinationFloor": 5,
      "passengers": 2
    },
    {
      "startTick": 50,
      "originFloor": 8,
      "destinationFloor": 2,
      "passengers": 1
    }
  ]
}
```

**run.log** - Execution Log

Timestamped log of simulation execution:

```
[2025-01-23T12:34:56.123Z] Run directory initialized at /path/to/run-42
[2025-01-23T12:34:56.234Z] Wrote config input to config.json
[2025-01-23T12:34:56.345Z] Wrote scenario input to scenario.json
[2025-01-23T12:34:56.456Z] Simulation started for run 42
[2025-01-23T12:34:56.567Z] Starting simulation for 1000 ticks
[2025-01-23T12:35:12.890Z] Simulation completed at tick 1000
[2025-01-23T12:35:12.901Z] Simulation succeeded for run 42
```

**results.json** - Structured Results

Comprehensive results with KPIs and metrics:

```json
{
  "runSummary": {
    "runId": 42,
    "status": "SUCCEEDED",
    "generatedAt": "2025-01-23T12:35:13Z",
    "durationTicks": 1000,
    "seed": 12345,
    "ticks": 1000,
    "liftSystemId": 1,
    "versionNumber": 3,
    "scenarioId": 5
  },
  "kpis": {
    "requestsTotal": 150,
    "pickupRequestsServed": 145,
    "pickupRequestsCancelled": 5,
    "passengersServed": 145,
    "passengersCancelled": 5,
    "avgPickupWaitTicks": 25.5,
    "maxPickupWaitTicks": 85,
    "idleTicks": 200,
    "movingTicks": 500,
    "doorTicks": 300,
    "pickupLegUtilisation": 0.80
  },
  "perLift": [
    {
      "liftId": "lift-1",
      "minFloor": 0,
      "maxFloor": 10,
      "homeFloor": 0,
      "controllerStrategy": "NEAREST_REQUEST_ROUTING",
      "idleParkingMode": "PARK_TO_HOME_FLOOR",
      "totalTicks": 1000,
      "idleTicks": 200,
      "movingTicks": 500,
      "doorTicks": 300,
      "pickupLegUtilisation": 0.80,
      "statusCounts": {
        "IDLE": 200,
        "MOVING_UP": 250,
        "MOVING_DOWN": 250,
        "DOOR_OPENING": 100,
        "DOOR_OPEN": 100,
        "DOOR_CLOSING": 100
      }
    }
  ],
  "perFloor": [
    {
      "floor": 0,
      "originPassengers": 50,
      "destinationPassengers": 20,
      "liftVisits": 45
    },
    {
      "floor": 5,
      "originPassengers": 20,
      "destinationPassengers": 50,
      "liftVisits": 40
    }
  ]
}
```

---

### Reproducing UI Runs via CLI

You can reproduce any UI-driven run using the CLI by downloading the generated input files.

#### Step-by-Step Reproduction

1. **Download Artefacts from UI**
   - Navigate to completed run in Simulator page
   - Scroll to "Artefacts" section
   - Download `config.json` and `scenario.json`

2. **Convert to CLI Format**
   - Use the Batch Input Generator to convert UI format to CLI format:
   ```bash
   POST /api/batch/generate-input
   {
     "config": { ... },      # Contents of config.json
     "scenario": { ... }     # Contents of scenario.json
   }
   ```

3. **Save Generated Scenario File**
   - Save the response to a `.scenario` file (e.g., `run-42-reproduction.scenario`)

4. **Run via CLI**
   ```bash
   java -cp target/lift-simulator-0.53.3.jar \
     com.liftsimulator.scenario.ScenarioRunnerMain \
     run-42-reproduction.scenario
   ```

#### Example: Reproducing Run #42

**1. Download config.json and scenario.json from run 42**

**2. Generate CLI scenario file:**

```bash
curl -X POST http://localhost:8080/api/batch/generate-input \
  -H "Content-Type: application/json" \
  -d '{
    "config": {
      "minFloor": 0,
      "maxFloor": 10,
      "homeFloor": 0,
      "controllerStrategy": "NEAREST_REQUEST_ROUTING",
      "idleParkingMode": "PARK_TO_HOME_FLOOR",
      "travelTicksPerFloor": 1,
      "doorTransitionTicks": 2,
      "doorDwellTicks": 3,
      "doorReopenWindowTicks": -1,
      "idleTimeoutTicks": 5
    },
    "scenario": {
      "durationTicks": 1000,
      "seed": 12345,
      "passengerFlows": [
        {
          "startTick": 0,
          "originFloor": 0,
          "destinationFloor": 5,
          "passengers": 2
        }
      ]
    }
  }' > run-42-reproduction.scenario
```

**3. Run the scenario:**

```bash
java -cp target/lift-simulator-0.53.3.jar \
  com.liftsimulator.scenario.ScenarioRunnerMain \
  run-42-reproduction.scenario
```

**Expected Output:**

The CLI will produce the same simulation results as the UI run (given the same seed).

#### Verifying Reproduction

To verify the reproduction matches the original run:

1. Compare tick counts: Should match `durationTicks` in scenario
2. Compare KPIs: Should match `kpis` in `results.json` (pickup wait times, pickup-leg utilisation, etc.)
3. Compare passenger counts: Should match `requestsTotal`, `pickupRequestsServed`, etc.

**Note:** Results are deterministic when using the same seed value.

---

### Example Scenario Walkthrough: Morning Rush Hour

This example demonstrates a complete workflow from UI run to CLI reproduction.

#### Scenario: Morning Rush Hour

**Setup:**
- Lift System: "Office Building Lifts"
- Published Version: v2 (10 floors, 2 lifts, Nearest Request strategy)
- Scenario: "Morning Rush - Ground to Upper Floors"
- Seed: 42

**Passenger Flows:**
- Ticks 0-100: Heavy traffic from ground floor to floors 5-10 (30 passengers)
- Ticks 200-300: Light return traffic from upper floors to ground (5 passengers)
- Duration: 500 ticks

#### UI Workflow

1. **Select Configuration:**
   - System: "Office Building Lifts"
   - Version: v2
   - Scenario: "Morning Rush - Ground to Upper Floors"
   - Seed: 42

2. **Start Run:**
   - Click "Start Run"
   - Run ID: 123

3. **Monitor Progress:**
   - Status: RUNNING
   - Progress: 250 / 500 ticks (50%)
   - Elapsed: 15 seconds

4. **View Results (after completion):**
   - Status: SUCCEEDED
   - Pickup Requests Served: 35 / 35
   - Avg Wait to Pickup: 18 ticks
   - Max Wait to Pickup: 45 ticks
   - Pickup-leg Lift Utilisation: 85%

#### Download Artefacts

From the Simulator page, download:
- `config.json` - Contains v2 configuration
- `scenario.json` - Contains passenger flows with seed 42
- `results.json` - Contains KPIs and metrics
- `run.log` - Contains execution trace

#### Reproduce via CLI

**1. Generate CLI scenario file:**

```bash
curl -X POST http://localhost:8080/api/batch/generate-input \
  -H "Content-Type: application/json" \
  -d @run-123-inputs.json \
  > morning-rush-reproduction.scenario
```

Where `run-123-inputs.json` contains:
```json
{
  "config": { /* contents of config.json */ },
  "scenario": { /* contents of scenario.json */ }
}
```

**2. Run via CLI:**

```bash
java -cp target/lift-simulator-0.53.3.jar \
  com.liftsimulator.scenario.ScenarioRunnerMain \
  morning-rush-reproduction.scenario
```

**3. Verify Results:**

CLI output should show:
- Same number of requests (35)
- Same avg wait time (18 ticks)
- Same max wait time (45 ticks)
- Same utilization (85%)

The results are deterministic because we used seed 42 in both runs.

---

### Troubleshooting

#### Run Failed (Status: FAILED)

**Symptom:** Run status shows FAILED in the UI

**Steps to Diagnose:**

1. **Check Run Logs:**
   ```bash
   # Via API
   curl http://localhost:8080/api/simulation-runs/{id}/logs?tail=100

   # Or download from UI
   Navigate to run → Artefacts → Download run.log
   ```

2. **Look for Error Messages:**
   - Configuration validation errors
   - Scenario validation errors
   - Runtime exceptions during simulation

3. **Common Causes:**
   - Invalid floor range (minFloor > maxFloor)
   - Invalid passenger flows (origin/destination out of range)
   - Invalid tick values (negative or zero duration)
   - Controller strategy not recognized

**Example Error in Logs:**

```
[2025-01-23T12:34:56Z] ERROR: Configuration validation failed
[2025-01-23T12:34:56Z] minFloor (5) cannot be greater than maxFloor (3)
```

**Resolution:** Fix the configuration and create a new run.

#### Where to Find Logs

**Option 1: Via UI**
1. Navigate to Simulator page
2. Find the run in the results table
3. Click to view run details
4. Scroll to "Artefacts" section
5. Download `run.log`

**Option 2: Via API**
```bash
# Get full logs
curl http://localhost:8080/api/simulation-runs/123/logs

# Get last 100 lines
curl http://localhost:8080/api/simulation-runs/123/logs?tail=100

# Get last 50 lines
curl http://localhost:8080/api/simulation-runs/123/logs?tail=50
```

**Option 3: Direct File Access**
```bash
# Navigate to run directory
cd simulation-runs/run-123

# View logs
cat run.log

# Or tail logs in real-time
tail -f run.log
```

#### Common Validation Errors

**1. Invalid Floor Range**

**Error Message:**
```
minFloor (10) cannot be greater than maxFloor (5)
```

**Cause:** Configuration has inverted floor range.

**Resolution:**
- Ensure minFloor ≤ maxFloor
- Common for underground parking: minFloor can be negative (e.g., -2 to 20)

**2. Home Floor Out of Range**

**Error Message:**
```
homeFloor (15) must be between minFloor (0) and maxFloor (10)
```

**Cause:** Home floor is outside the valid floor range.

**Resolution:** Set homeFloor to a value between minFloor and maxFloor (inclusive).

**3. Invalid Passenger Flow Floor**

**Error Message:**
```
Passenger flow at tick 0: originFloor (15) is out of range [0, 10]
```

**Cause:** Scenario has passenger flows with floors outside the configuration's floor range.

**Resolution:**
- Update scenario to use valid floors
- Or update configuration to support the required floor range

**4. Invalid Controller Strategy**

**Error Message:**
```
Unknown controller strategy: INVALID_STRATEGY
```

**Cause:** Configuration specifies a controller strategy that doesn't exist.

**Valid Values:**
- `NEAREST_REQUEST_ROUTING`
- `DIRECTIONAL_SCAN`

**Resolution:** Use one of the valid strategy names (case-sensitive).

**5. Invalid Idle Parking Mode**

**Error Message:**
```
Unknown idle parking mode: INVALID_MODE
```

**Cause:** Configuration specifies an idle parking mode that doesn't exist.

**Valid Values:**
- `STAY_WHERE_STOPPED`
- `PARK_TO_HOME_FLOOR`

**Resolution:** Use one of the valid parking modes (case-sensitive).

**6. Negative Tick Values**

**Error Message:**
```
durationTicks (-100) must be positive
```

**Cause:** Scenario has negative or zero duration.

**Resolution:** Set durationTicks to a positive value (typically 100-10000).

**7. Invalid Seed**

**Error Message:**
```
seed must be a valid integer
```

**Cause:** Seed is not a valid integer or is outside valid range.

**Resolution:**
- Use a positive integer for the seed
- Or omit seed to use auto-generated value

#### Run Stuck in CREATED State

**Symptom:** Run shows CREATED status for extended period

**Possible Causes:**
1. Backend execution service is not running
2. Thread pool exhausted (too many concurrent runs)
3. Database connection issue

**Steps to Diagnose:**

1. **Check Backend Logs:**
   ```bash
   # Application logs
   tail -f logs/lift-simulator.log

   # Look for execution service startup
   grep "SimulationRunExecutionService" logs/lift-simulator.log
   ```

2. **Check Thread Pool:**
   ```bash
   # Look for thread pool warnings
   grep "Thread pool" logs/lift-simulator.log
   ```

3. **Restart Backend:**
   ```bash
   # Stop backend
   pkill -f "lift-simulator"

   # Start backend
   mvn spring-boot:run
   ```

**Resolution:**
- Wait for concurrent runs to complete
- Increase thread pool size in `application.yml` or an active profile override:
  ```yaml
  simulation:
    execution:
      thread-pool-size: 5
  ```
- Restart backend if needed

#### Run Stuck in RUNNING State

**Symptom:** Run shows RUNNING status indefinitely, progress not updating

**Possible Causes:**
1. Infinite loop in simulation logic (bug)
2. Process crashed without updating status
3. Very long-running simulation (large tick count)

**Steps to Diagnose:**

1. **Check Expected Duration:**
   - View scenario to see total ticks
   - Estimate time: ~1000 ticks per second (approximate)
   - Example: 100,000 ticks ≈ 100 seconds

2. **Check Run Logs:**
   ```bash
   curl http://localhost:8080/api/simulation-runs/{id}/logs?tail=50
   ```
   - Look for recent tick progress
   - Look for errors or exceptions

3. **Check Backend Process:**
   ```bash
   # Check if backend is running
   ps aux | grep "lift-simulator"

   # Check CPU usage (high CPU = still running)
   top -p <backend-pid>
   ```

**Resolution:**
- If truly stuck: Restart backend (will mark in-progress runs as FAILED)
- If just slow: Wait for completion or cancel the run

#### Results Not Available After SUCCEEDED

**Symptom:** Run shows SUCCEEDED but results.json is missing

**Steps to Diagnose:**

1. **Check Artefacts:**
   ```bash
   curl http://localhost:8080/api/simulation-runs/{id}/artefacts
   ```

2. **Check File System:**
   ```bash
   ls -lh simulation-runs/run-{id}/
   ```

3. **Check Run Logs:**
   ```bash
   cat simulation-runs/run-{id}/run.log | grep "results.json"
   ```

**Possible Causes:**
- File write permission issue
- Disk full
- Results generation failed after simulation

**Resolution:**
- Check file permissions on `simulation-runs/` directory
- Check disk space: `df -h`
- Re-run the simulation

#### CLI Reproduction Produces Different Results

**Symptom:** CLI run produces different KPIs than UI run

**Possible Causes:**
1. **Different seed values** - Most common cause
2. **Different configurations** - Verify config.json matches
3. **Different scenarios** - Verify scenario.json matches
4. **Code version mismatch** - Different versions of simulator

**Steps to Diagnose:**

1. **Verify Seed:**
   - Check UI run seed in `scenario.json`
   - Ensure CLI scenario file uses same seed
   - Seeds should be identical for deterministic results

2. **Compare Configurations:**
   ```bash
   # Download UI config
   curl http://localhost:8080/api/simulation-runs/{id}/artefacts/config.json > ui-config.json

   # Extract config from CLI scenario file
   grep -A 20 "^min_floor:" run-reproduction.scenario > cli-config.txt

   # Compare manually
   ```

3. **Verify Code Version:**
   ```bash
   # Check version in pom.xml
   grep "<version>" pom.xml | head -1

   # Should match UI backend version
   ```

**Resolution:**
- Use exact same seed from UI run
- Verify all configuration parameters match
- Rebuild CLI JAR if versions don't match: `mvn clean package`

---
