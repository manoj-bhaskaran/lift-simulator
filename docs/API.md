# REST API Reference

This document contains the full REST API reference for the Lift Simulator backend. The interactive Swagger UI is available at `http://localhost:8080/api/v1/swagger-ui.html`, and the OpenAPI JSON is available at `http://localhost:8080/api/v1/api-docs` when the backend is running.

All application endpoints use the `/api/v1` base path unless otherwise noted. Admin endpoints use HTTP Basic authentication, while runtime and simulation execution endpoints use the configured API key header.

### Available Endpoints

#### Lift System Management

- **Create Lift System**: `POST /api/v1/lift-systems`
  - Creates a new lift system configuration
  - Request body:
    ```json
    {
      "systemKey": "building-a-lifts",
      "displayName": "Building A Lift System",
      "description": "Main lift system for Building A"
    }
    ```
  - Response (201 Created):
    ```json
    {
      "id": 1,
      "systemKey": "building-a-lifts",
      "displayName": "Building A Lift System",
      "description": "Main lift system for Building A",
      "createdAt": "2026-01-11T10:00:00Z",
      "updatedAt": "2026-01-11T10:00:00Z",
      "versionCount": 0
    }
    ```

- **List All Lift Systems**: `GET /api/v1/lift-systems`
  - Returns all lift systems
  - Response (200 OK):
    ```json
    [
      {
        "id": 1,
        "systemKey": "building-a-lifts",
        "displayName": "Building A Lift System",
        "description": "Main lift system for Building A",
        "createdAt": "2026-01-11T10:00:00Z",
        "updatedAt": "2026-01-11T10:00:00Z",
        "versionCount": 3
      }
    ]
    ```

- **Get Lift System by ID**: `GET /api/v1/lift-systems/{id}`
  - Returns a specific lift system by ID
  - Response (200 OK): Same as create response (includes `versionCount`)
  - Error (404 Not Found):
    ```json
    {
      "status": 404,
      "message": "Lift system not found with id: 999",
      "timestamp": "2026-01-11T10:00:00Z"
    }
    ```

- **Update Lift System**: `PUT /api/v1/lift-systems/{id}`
  - Updates lift system metadata (display name and description)
  - Request body:
    ```json
    {
      "displayName": "Updated Building A Lift System",
      "description": "Updated description"
    }
    ```
  - Response (200 OK): Updated lift system details
  - Note: System key cannot be changed after creation

- **Delete Lift System**: `DELETE /api/v1/lift-systems/{id}`
  - Deletes a lift system and all its versions (cascade delete)
  - Response (204 No Content): Success with no body
  - Error (404 Not Found): If lift system doesn't exist

#### Version Management

- **Create Version**: `POST /api/v1/lift-systems/{systemId}/versions`
  - Creates a new version for a lift system
  - Optionally clones configuration from an existing version
  - Request body (with new config):
    ```json
    {
      "config": "{\"minFloor\": 0, \"maxFloor\": 9, \"lifts\": 2}",
      "cloneFromVersionNumber": null
    }
    ```
  - Request body (cloning from version):
    ```json
    {
      "config": "{}",
      "cloneFromVersionNumber": 1
    }
    ```
  - Response (201 Created):
    ```json
    {
      "id": 1,
      "liftSystemId": 1,
      "versionNumber": 1,
      "status": "DRAFT",
      "isPublished": false,
      "publishedAt": null,
      "config": "{\"minFloor\": 0, \"maxFloor\": 9, \"lifts\": 2}",
      "createdAt": "2026-01-11T10:00:00Z",
      "updatedAt": "2026-01-11T10:00:00Z"
    }
    ```
  - Version numbers auto-increment per lift system

- **Update Version Config**: `PUT /api/v1/lift-systems/{systemId}/versions/{versionNumber}`
  - Updates the configuration JSON for a specific version
  - Request body:
    ```json
    {
      "config": "{\"minFloor\": 0, \"maxFloor\": 14, \"lifts\": 3}"
    }
    ```
  - Response (200 OK): Updated version details

- **List Versions**: `GET /api/v1/lift-systems/{systemId}/versions`
  - Returns all versions for a lift system, ordered by version number descending
  - Response (200 OK):
    ```json
    [
      {
        "id": 2,
        "liftSystemId": 1,
        "versionNumber": 2,
        "status": "DRAFT",
        "isPublished": false,
        "publishedAt": null,
        "config": "{\"minFloor\": 0, \"maxFloor\": 14}",
        "createdAt": "2026-01-11T11:00:00Z",
        "updatedAt": "2026-01-11T11:00:00Z"
      },
      {
        "id": 1,
        "liftSystemId": 1,
        "versionNumber": 1,
        "status": "DRAFT",
        "isPublished": false,
        "publishedAt": null,
        "config": "{\"minFloor\": 0, \"maxFloor\": 9}",
        "createdAt": "2026-01-11T10:00:00Z",
        "updatedAt": "2026-01-11T10:00:00Z"
      }
    ]
    ```

- **Get Version**: `GET /api/v1/lift-systems/{systemId}/versions/{versionNumber}`
  - Returns a specific version by version number
  - Response (200 OK): Version details
  - Error (404 Not Found): If version doesn't exist

- **Publish Version**: `POST /api/v1/lift-systems/{systemId}/versions/{versionNumber}/publish`
  - Publishes a version after validating its configuration
  - Automatically archives any previously published version for the same lift system
  - Ensures exactly one published version per lift system at any given time
  - Only versions with valid configurations can be published
  - Response (200 OK): Published version details with `status: "PUBLISHED"` and `isPublished: true`
  - Error (400 Bad Request with validation errors): If configuration is invalid
  - Error (409 Conflict): If version is already published
  - Note: Publishing is blocked if the configuration has validation errors
  - Note: Previously published versions are automatically set to `status: "ARCHIVED"`

#### Configuration Validation

The backend includes a comprehensive validation framework for lift system configurations. All configurations are validated before being saved or published.

- **Validate Configuration**: `POST /api/v1/config/validate`
  - Validates a configuration JSON without persisting it
  - Request body:
    ```json
    {
      "config": "{\"minFloor\": 0, \"maxFloor\": 9, \"lifts\": 2, \"travelTicksPerFloor\": 1, ...}"
    }
    ```
  - Response (200 OK) - Valid configuration:
    ```json
    {
      "valid": true,
      "errors": [],
      "warnings": [
        {
          "field": "doorDwellTicks",
          "message": "Door dwell ticks (1) is very low. Consider increasing to allow sufficient time for passengers.",
          "severity": "WARNING"
        }
      ]
    }
    ```
  - Response (200 OK) - Invalid configuration:
    ```json
    {
      "valid": false,
      "errors": [
        {
          "field": "maxFloor",
          "message": "Maximum floor (0) must be greater than minimum floor (0)",
          "severity": "ERROR"
        },
        {
          "field": "doorReopenWindowTicks",
          "message": "Door reopen window ticks (5) must not exceed door transition ticks (2)",
          "severity": "ERROR"
        }
      ],
      "warnings": []
    }
    ```

#### Scenario Management

Scenario payloads define passenger flow for UI-driven simulation runs. The scenario schema is separate from the batch `.scenario` files used by the CLI.

- **Create Scenario**: `POST /api/v1/scenarios`
  - Saves a scenario JSON payload after validation
  - Request body:
    ```json
    {
      "scenarioJson": {
        "durationTicks": 60,
        "passengerFlows": [
          {
            "startTick": 0,
            "originFloor": 0,
            "destinationFloor": 5,
            "passengers": 3
          }
        ],
        "seed": 42
      }
    }
    ```
  - Response (201 Created):
    ```json
    {
      "id": 1,
      "scenarioJson": {
        "durationTicks": 60,
        "passengerFlows": [
          {
            "startTick": 0,
            "originFloor": 0,
            "destinationFloor": 5,
            "passengers": 3
          }
        ],
        "seed": 42
      },
      "createdAt": "2026-01-11T10:00:00Z",
      "updatedAt": "2026-01-11T10:00:00Z"
    }
    ```

- **Update Scenario**: `PUT /api/v1/scenarios/{id}`
  - Updates an existing scenario payload after validation
  - Request body: same as create
  - Response (200 OK): Updated scenario details

- **Get Scenario**: `GET /api/v1/scenarios/{id}`
  - Returns a stored scenario by ID
  - Response (200 OK): Scenario details

#### Scenario Validation

- **Validate Scenario**: `POST /api/v1/scenarios/validate`
  - Validates a scenario JSON payload without saving it
  - Request body:
    ```json
    {
      "scenarioJson": {
        "durationTicks": 60,
        "passengerFlows": [
          {
            "startTick": 0,
            "originFloor": 0,
            "destinationFloor": 5,
            "passengers": 3
          }
        ],
        "seed": 42
      }
    }
    ```
  - Response (200 OK) - Valid scenario:
    ```json
    {
      "valid": true,
      "errors": [],
      "warnings": []
    }
    ```
  - Response (200 OK) - Invalid scenario:
    ```json
    {
      "valid": false,
      "errors": [
        {
          "field": "passengerFlows[0].startTick",
          "message": "startTick must be less than durationTicks (60)",
          "severity": "ERROR"
        }
      ],
      "warnings": []
    }
    ```

#### Batch Input Generator

The batch input generator converts stored scenario definitions and lift configurations into the legacy `.scenario` file format used by the CLI simulator. This enables backwards compatibility between the modern UI-driven workflow and the existing batch simulation infrastructure.

**Purpose:**
- Generates `.scenario` files from database-stored configurations
- Ensures exact format compliance with `ScenarioParser`
- Maintains backwards compatibility with CLI simulator
- Stores generated files in run-specific artifact directories

**How it works:**

1. **Input**: Lift system version configuration (from database) + Scenario JSON (passenger flows)
2. **Processing**: Converts passenger flows to `hall_call` events with proper direction calculation
3. **Output**: `.scenario` file in the exact format expected by `ScenarioRunnerMain`

**Example conversion:**

Input scenario:
```json
{
  "durationTicks": 30,
  "passengerFlows": [
    {
      "startTick": 0,
      "originFloor": 0,
      "destinationFloor": 5,
      "passengers": 2
    },
    {
      "startTick": 10,
      "originFloor": 8,
      "destinationFloor": 2,
      "passengers": 1
    }
  ]
}
```

Generated `.scenario` file:
```
name: Simulation Run 123 - Morning Rush
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
controller_strategy: NEAREST_REQUEST_ROUTING
idle_parking_mode: PARK_TO_HOME_FLOOR

0, hall_call, p1, 0, UP
0, hall_call, p2, 0, UP
10, hall_call, p3, 8, DOWN
```

**Programmatic usage:**

```java
@Autowired
private SimulationRunService runService;

// Generate batch input file for a simulation run
Path inputFile = runService.generateBatchInputFile(runId);
// Returns: /path/to/artifacts/input.scenario
```

**Key features:**
- **Automatic direction calculation**: Determines UP/DOWN based on origin and destination floors
- **Unique passenger aliases**: Each passenger gets a unique alias (p1, p2, p3...)
- **Event ordering**: Events are sorted by tick, then by alias
- **Validation**: Ensures floor values are within configured range and start ticks are valid when generating content or files
- **Artifact management**: Files are stored in run-specific directories under `artefactBasePath`

**Scenario JSON Structure:**

```json
{
  "durationTicks": 60,
  "passengerFlows": [
    {
      "startTick": 0,
      "originFloor": 0,
      "destinationFloor": 5,
      "passengers": 3
    }
  ],
  "seed": 42
}
```

**Scenario Validation Rules:**

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `durationTicks` | Integer | Required, ≥ 1 | Total number of ticks to simulate |
| `passengerFlows` | Array | Required, ≥ 1 entry | Passenger flow entries |
| `passengerFlows[].startTick` | Integer | ≥ 0, < durationTicks | Tick when passengers arrive |
| `passengerFlows[].originFloor` | Integer | Required | Origin floor for the flow |
| `passengerFlows[].destinationFloor` | Integer | Required, ≠ origin | Destination floor for the flow |
| `passengerFlows[].passengers` | Integer | Required, ≥ 1 | Number of passengers in the flow |
| `seed` | Integer | Optional, ≥ 0 | Random seed for deterministic runs |

**Configuration Structure:**

All lift system configurations must conform to the following structure:

```json
{
  "minFloor": 0,
  "maxFloor": 9,
  "lifts": 2,
  "travelTicksPerFloor": 1,
  "doorTransitionTicks": 2,
  "doorDwellTicks": 3,
  "doorReopenWindowTicks": 2,
  "homeFloor": 0,
  "idleTimeoutTicks": 5,
  "controllerStrategy": "NEAREST_REQUEST_ROUTING",
  "idleParkingMode": "PARK_TO_HOME_FLOOR"
}
```

**Migration Guide (0.46.0 floor range update):**

- Replace `floors` with explicit `minFloor` and `maxFloor`.
  - For existing configs, set `minFloor` to `0` and `maxFloor` to `floors - 1`.
- Ensure `homeFloor` is within the new range (`minFloor` to `maxFloor`).
- Apply the Flyway migration `V2__migrate_floor_range_config.sql` to update stored configuration payloads.

**Validation Rules:**

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `minFloor` | Integer | Required | Minimum floor in the building (can be negative for basements) |
| `maxFloor` | Integer | > minFloor | Maximum floor in the building (must be greater than minFloor) |
| `lifts` | Integer | ≥ 1 | Number of lift cars |
| `travelTicksPerFloor` | Integer | ≥ 1 | Ticks required to travel one floor |
| `doorTransitionTicks` | Integer | ≥ 1 | Ticks required for doors to open or close |
| `doorDwellTicks` | Integer | ≥ 1 | Ticks doors stay open before closing |
| `doorReopenWindowTicks` | Integer | ≥ 0, ≤ doorTransitionTicks | Window during door closing when doors can reopen |
| `homeFloor` | Integer | minFloor ≤ homeFloor ≤ maxFloor | Idle parking floor (must be within floor range) |
| `idleTimeoutTicks` | Integer | ≥ 0 | Ticks before idle parking behavior activates |
| `controllerStrategy` | Enum | NEAREST_REQUEST_ROUTING, DIRECTIONAL_SCAN | Controller algorithm |
| `idleParkingMode` | Enum | STAY_AT_CURRENT_FLOOR, PARK_TO_HOME_FLOOR | Idle parking behavior |

**Validation Features:**

- **Structural Validation**: Ensures JSON is well-formed and all required fields are present
- **Type Validation**: Validates field types and enum values
  - **Domain Validation**: Enforces business rules and cross-field constraints
  - doorReopenWindowTicks must not exceed doorTransitionTicks
  - maxFloor must be greater than minFloor
  - homeFloor must be within valid floor range (minFloor to maxFloor)
- **Warnings**: Non-blocking suggestions for suboptimal configurations
  - Low doorDwellTicks values
  - More lifts than floors available in the range
  - Low idleTimeoutTicks with PARK_TO_HOME_FLOOR mode
  - Zero doorReopenWindowTicks (disables door reopening)

**Automatic Validation:**

Validation is automatically performed when:
- Creating a new version (`POST /api/v1/lift-systems/{systemId}/versions`)
- Updating a version's configuration (`PUT /api/v1/lift-systems/{systemId}/versions/{versionNumber}`)
- Publishing a version (`POST /api/v1/lift-systems/{systemId}/versions/{versionNumber}/publish`)

If validation fails with errors, the operation will be rejected with a 400 Bad Request response containing detailed error information.

**Error Response Format:**

When configuration validation fails:
```json
{
  "valid": false,
  "errors": [
    {
      "field": "homeFloor",
      "message": "Home floor (15) must be within valid floor range (0 to 9)",
      "severity": "ERROR"
    }
  ],
  "warnings": []
}
```

#### API Key Authentication (Runtime & Simulation Execution)

Runtime and simulation execution endpoints require an API key so they can be invoked from CLI tooling and automation.

**Security Requirements:**
- **Startup Validation**: The application requires `api.auth.key` to be configured and non-empty. If missing or blank, the application will fail to start with a clear error message.
- **Secure Comparison**: API keys are compared using SHA-256 hashing with constant-time comparison to prevent timing attacks and credential leakage.

**Configuration:**
- `api.auth.key` (required): API key value used for authentication. Must be non-empty and provided via environment variable or property file.
- `api.auth.header` (optional, default: `X-API-Key`): Header name to read the key from

**Setting Up API Key:**

Generate a secure random API key:

```bash
# Using OpenSSL (recommended for 32 bytes of entropy)
openssl rand -base64 32

# Using /dev/urandom on Unix-like systems
head -c 32 /dev/urandom | base64

# Using Python
python3 -c "import secrets; print(secrets.token_urlsafe(32))"
```

Configure via environment variable:

```bash
export API_KEY="<your-generated-api-key>"
```

Or in `application-dev.yml`:

```yaml
api:
  auth:
    key: <your-generated-api-key>
    header: X-API-Key
```

**Example (Simulation Run):**
```bash
curl -H "X-API-Key: <your-api-key>" \\
  -H "Content-Type: application/json" \\
  -d '{"liftSystemId":1,"versionId":2,"scenarioId":3,"seed":12345}' \\
  http://localhost:8080/api/v1/simulation-runs
```

**Example (Runtime Config):**
```bash
curl -H "X-API-Key: <your-api-key>" \\
  http://localhost:8080/api/v1/runtime/systems/{systemKey}/config
```


#### Simulation Run APIs

The Simulation Run APIs enable UI to start simulations, poll their status, and access results/logs. These endpoints provide the complete lifecycle management for simulation execution.

**Key Features:**
- Create and start simulation runs atomically
- Poll run status with progress tracking
- Cancel in-progress simulation runs
- Retrieve structured results when completed
- Access logs with optional tail functionality
- List and manage simulation artefacts
- Security controls to prevent path traversal attacks

##### Run Artefacts and Results Schema

Run artefacts are stored on disk using the configured `simulation.artefacts.base-path` property
(defaults to `./simulation-runs`). Each run folder is created at `{base-path}/run-{id}/` and contains:

- `config.json` - exact configuration payload used
- `scenario.json` - scenario payload for passenger flows
- `run.log` - log output from the runner
- `results.json` - structured summary of the simulation run for UI rendering

`results.json` provides a minimal, additive results schema suitable for UI consumption:

```json
{
  "runSummary": {
    "runId": 42,
    "status": "SUCCEEDED",
    "generatedAt": "2026-02-01T10:05:00Z",
    "ticks": 120,
    "durationTicks": 120,
    "seed": 123,
    "liftSystemId": 1,
    "versionId": 3,
    "scenarioId": 5
  },
  "kpis": {
    "requestsTotal": 12,
    "passengersServed": 12,
    "passengersCancelled": 0,
    "avgWaitTicks": 4.5,
    "maxWaitTicks": 11,
    "idleTicks": 30,
    "movingTicks": 50,
    "doorTicks": 40,
    "utilisation": 0.75
  },
  "perLift": [
    {
      "liftId": "lift-1",
      "minFloor": 0,
      "maxFloor": 10,
      "homeFloor": 0,
      "travelTicksPerFloor": 1,
      "doorTransitionTicks": 2,
      "doorDwellTicks": 3,
      "doorReopenWindowTicks": 2,
      "controllerStrategy": "NEAREST_REQUEST_ROUTING",
      "idleParkingMode": "PARK_TO_HOME_FLOOR",
      "statusCounts": {
        "IDLE": 30,
        "MOVING_UP": 25,
        "MOVING_DOWN": 25,
        "DOORS_OPENING": 8,
        "DOORS_OPEN": 20,
        "DOORS_CLOSING": 12
      },
      "totalTicks": 120,
      "idleTicks": 30,
      "movingTicks": 50,
      "doorTicks": 40,
      "utilisation": 0.75
    }
  ],
  "perFloor": [
    {
      "floor": 0,
      "originPassengers": 5,
      "destinationPassengers": 2,
      "liftVisits": 12
    }
  ]
}
```

- `runSummary` includes tick counts, duration, seed, and lift system/version references.
- `kpis` includes available wait-time metrics and utilisation from the simulation run.
- `perLift` contains single-lift state counts and configuration metadata (one entry for now).
- `perFloor` aggregates passenger origins/destinations and lift visit counts per floor (counted on floor changes).

---

##### Create and Start Simulation Run

**Endpoint:** `POST /api/v1/simulation-runs`

Creates a new simulation run, sets up the artefact directory, and immediately starts execution.
All simulation run endpoints require the configured API key header.

**Request Body:**
```json
{
  "liftSystemId": 1,
  "versionId": 2,
  "scenarioId": 3,
  "seed": 12345
}
```

**Request Fields:**
- `liftSystemId` (required): ID of the lift system to simulate
- `versionId` (required): ID of the version to use
- `scenarioId` (optional): ID of the scenario to run (null for ad-hoc runs)
- `seed` (optional): Random seed for reproducibility (auto-generated if not provided)

**Response (201 Created):**
```json
{
  "id": 1,
  "liftSystemId": 1,
  "versionId": 2,
  "scenarioId": 3,
  "status": "RUNNING",
  "createdAt": "2026-01-23T10:00:00Z",
  "startedAt": "2026-01-23T10:00:01Z",
  "endedAt": null,
  "totalTicks": 10000,
  "currentTick": 0,
  "seed": 12345,
  "errorMessage": null
}
```

**Status Values:**
- `CREATED`: Run created but not yet started
- `RUNNING`: Simulation is currently executing
- `SUCCEEDED`: Simulation completed successfully
- `FAILED`: Simulation failed with error
- `CANCELLED`: Simulation was cancelled

---

##### Get Simulation Run Status

**Endpoint:** `GET /api/v1/simulation-runs/{id}`

Retrieves the current status and details of a simulation run, including progress information.

**Response (200 OK):**
```json
{
  "id": 1,
  "liftSystemId": 1,
  "versionId": 2,
  "scenarioId": 3,
  "status": "RUNNING",
  "createdAt": "2026-01-23T10:00:00Z",
  "startedAt": "2026-01-23T10:00:01Z",
  "endedAt": null,
  "totalTicks": 10000,
  "currentTick": 5432,
  "seed": 12345,
  "errorMessage": null
}
```

**Progress Calculation:**
- Progress percentage: `(currentTick / totalTicks) × 100`
- Example: `(5432 / 10000) × 100 = 54.32%`

**Error Response (404 Not Found):**
```json
{
  "status": 404,
  "message": "Simulation run not found with id: 999",
  "timestamp": "2026-01-23T10:00:00Z"
}
```

---

##### Cancel Simulation Run

**Endpoint:** `POST /api/v1/simulation-runs/{id}/cancel`

Cancels a running simulation run and transitions it to a terminal `CANCELLED` state.

**Response (200 OK):**
```json
{
  "id": 1,
  "liftSystemId": 1,
  "versionId": 2,
  "scenarioId": 3,
  "status": "CANCELLED",
  "createdAt": "2026-01-23T10:00:00Z",
  "startedAt": "2026-01-23T10:00:01Z",
  "endedAt": "2026-01-23T10:05:00Z",
  "totalTicks": 10000,
  "currentTick": 4321,
  "seed": 12345,
  "errorMessage": null
}
```

**Error Response (409 Conflict):**
```json
{
  "status": 409,
  "message": "Can only cancel a run in CREATED or RUNNING state. Current state: SUCCEEDED",
  "timestamp": "2026-01-23T10:05:30Z"
}
```

---

##### Get Simulation Results

**Endpoint:** `GET /api/v1/simulation-runs/{id}/results`

Retrieves the results of a simulation run. Response varies based on run status.

**Response for SUCCEEDED (200 OK):**
```json
{
  "runId": 1,
  "status": "SUCCEEDED",
  "results": {
    "totalPassengersServed": 150,
    "averageWaitTime": 12.5,
    "maxWaitTime": 45.2,
    "liftsUtilization": {
      "lift1": 0.85,
      "lift2": 0.78
    }
  },
  "errorMessage": null,
  "logsUrl": "/api/simulation-runs/1/logs"
}
```

**Response for SUCCEEDED without results file (200 OK):**
```json
{
  "runId": 1,
  "status": "SUCCEEDED",
  "results": null,
  "errorMessage": "Results file not available: No results file found for simulation run 1",
  "logsUrl": "/api/simulation-runs/1/logs"
}
```

*Note: If a simulation completed successfully but the results file cannot be read (e.g., file not found, permission denied), the response preserves the SUCCEEDED status while indicating results are unavailable via errorMessage.*

**Response for FAILED (200 OK):**
```json
{
  "runId": 1,
  "status": "FAILED",
  "results": null,
  "errorMessage": "Simulation engine crashed at tick 1234",
  "logsUrl": "/api/simulation-runs/1/logs"
}
```

**Response for RUNNING (409 Conflict):**
```json
{
  "runId": 1,
  "status": "RUNNING",
  "results": null,
  "errorMessage": "Simulation is still running",
  "logsUrl": null
}
```

**Response for CREATED/CANCELLED (400 Bad Request):**
```json
{
  "runId": 1,
  "status": "CREATED",
  "results": null,
  "errorMessage": "Results not available for CREATED runs",
  "logsUrl": null
}
```

---

##### Get Simulation Logs

**Endpoint:** `GET /api/v1/simulation-runs/{id}/logs?tail=N`

Retrieves simulation logs with optional tail functionality.

**Query Parameters:**
- `tail` (optional): Number of lines to retrieve from end of log
  - Default: All lines
  - Maximum: 10,000 lines
  - Large tails are buffered with fixed-size deque storage, so reading the last lines of large logs uses bounded memory and linear time.

**Response (200 OK):**
```json
{
  "runId": "1",
  "logs": "Starting simulation...\nTick 0: Initializing lifts\nTick 1: Processing requests\n...",
  "tail": "100"
}
```

**Common Log Files (searched in order):**
- `simulation.log`
- `output.log`
- `run.log`

**Error Response (500 Internal Server Error):**
```json
{
  "runId": "1",
  "error": "Failed to read logs: Artefact base path is not set for run 1"
}
```

**Example Usage:**
```bash
# Get all logs
curl http://localhost:8080/api/simulation-runs/1/logs

# Get last 100 lines
curl http://localhost:8080/api/simulation-runs/1/logs?tail=100
```

---

##### List Simulation Artefacts

**Endpoint:** `GET /api/v1/simulation-runs/{id}/artefacts`

Lists all artefacts (downloadable files) associated with a simulation run.

**Response (200 OK):**
```json
[
  {
    "name": "results.json",
    "path": "results.json",
    "size": 1024,
    "mimeType": "application/json"
  },
  {
    "name": "simulation.log",
    "path": "simulation.log",
    "size": 5120,
    "mimeType": "text/plain"
  },
  {
    "name": "input.scenario",
    "path": "input.scenario",
    "size": 2048,
    "mimeType": "text/plain"
  }
]
```

**Artefact Structure:**
- `name`: File name
- `path`: Relative path within artefact directory
- `size`: File size in bytes
- `mimeType`: MIME type based on file extension

**Supported MIME Types:**
- `.json` → `application/json`
- `.txt`, `.log` → `text/plain`
- `.csv` → `text/csv`
- `.scenario` → `text/plain`
- Others → `application/octet-stream`

**Empty Directory Response (200 OK):**
```json
[]
```

---

##### Download Simulation Artefact

**Endpoint:** `GET /api/v1/simulation-runs/{id}/artefacts/{path}`

Downloads a specific artefact file for a simulation run. The `{path}` value should match the
`path` field returned by the artefact list endpoint.

**Response (200 OK):**
- Binary file content with `Content-Disposition: attachment` and the appropriate MIME type.

**Error Responses:**
- **404 Not Found** when the artefact does not exist.
- **400 Bad Request** when the artefact path is invalid or attempts path traversal.
- **409 Conflict** when the run artefact base path is not configured.

**Example Usage:**
```bash
curl -O http://localhost:8080/api/simulation-runs/1/artefacts/results.json
```

---

**Security Features:**
- **Path Traversal Prevention**: All file access paths are normalized and validated
- **Directory Isolation**: Artefacts are restricted to run-specific directories
- **Secure Resolution**: Attempts to access files outside the artefact directory are blocked

**Configuration:**
```yaml
# Base directory for simulation artefacts (application.yml)
simulation:
  artefacts:
    base-path: ./simulation-runs
```

**Directory Structure:**
```
simulation-runs/
├── run-1/
│   ├── input.scenario
│   ├── results.json
│   └── simulation.log
├── run-2/
│   ├── results.json
│   └── simulation.log
└── run-3/
    └── simulation.log
```

---

#### CLI and UI-Run Workflows

This section documents how to run simulations using both the command-line interface (CLI) and the web UI. The CLI interface remains fully backward compatible with previous versions, while the new UI provides a streamlined workflow for running simulations with managed configurations and scenarios.

##### Overview

The Lift Simulator supports two primary workflows:

1. **CLI Workflow (Backward Compatible)**: Run simulations directly from the command line using scenario files
2. **UI-Driven Workflow (New in v0.46.0)**: Run simulations through the web interface with Version + Scenario selection

Both workflows produce the same simulation results and use the same underlying simulation engine.

---

##### CLI Usage (Unchanged)

The command-line interface remains **fully backward compatible** with previous versions. All existing scripts and automation will continue to work without modification.

**Available CLI Entry Points:**

1. **Demo Simulation** - Quick test with sample configuration
2. **Scenario Runner** - Run scripted scenarios from `.scenario` files
3. **Local Simulation** - Run with JSON configuration files

###### Scenario Runner (Primary CLI)

Run a simulation using a `.scenario` file:

```bash
java -cp target/lift-simulator-0.49.11.jar \
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
java -cp target/lift-simulator-0.49.11.jar \
  com.liftsimulator.scenario.ScenarioRunnerMain \
  my-test-scenario.scenario

# Run with default demo scenario
java -cp target/lift-simulator-0.49.11.jar \
  com.liftsimulator.scenario.ScenarioRunnerMain
```

**Scenario File Format:**

Scenario files use a simple text format with metadata and events:

```
# Metadata (required)
min_floor: 0
max_floor: 10
home_floor: 0
controller_strategy: NEAREST_REQUEST_ROUTING
idle_parking_mode: PARK_TO_HOME_FLOOR
travel_ticks_per_floor: 1
door_transition_ticks: 2
door_dwell_ticks: 3
door_reopen_window_ticks: -1
idle_timeout_ticks: 5

# Events (tick number, event type, parameters)
0 car_call 0 5           # At tick 0: Car call from floor 0 to floor 5
5 hall_call 3 UP         # At tick 5: Hall call at floor 3, going UP
10 hall_call 8 DOWN      # At tick 10: Hall call at floor 8, going DOWN
```

**Event Types:**
- `car_call <origin_floor> <destination_floor>` - Passenger inside the lift
- `hall_call <floor> <direction>` - Passenger waiting (UP/DOWN/NONE)
- `cancel <floor> <direction>` - Cancel a hall call
- `out_of_service <lift_id>` - Take a lift out of service
- `return_to_service <lift_id>` - Return a lift to service

###### Local Simulation with JSON Config

Run a simulation using a JSON configuration file:

```bash
java -cp target/lift-simulator-0.49.11.jar \
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
java -cp target/lift-simulator-0.49.11.jar \
  com.liftsimulator.runtime.LocalSimulationMain \
  --config=building-a.json \
  --ticks=1000
```

###### Demo Simulation

Run a quick demo simulation with built-in configuration:

```bash
java -cp target/lift-simulator-0.49.11.jar \
  com.liftsimulator.Main \
  --strategy=directional-scan
```

**Options:**
- `--strategy=<strategy>` - Controller strategy to use
  - Valid values: `nearest-request`, `directional-scan`
  - Default: `nearest-request`
- `-h, --help` - Show help message

---

##### UI-Driven Run Workflow (New in v0.46.0)

The web UI provides a streamlined workflow for running simulations with managed configurations and scenarios.

###### Workflow Steps

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
   - KPI cards (wait times, utilization, passengers served)
   - Per-lift metrics table
   - Per-floor metrics table
   - Download artefacts (logs, results, input files)

###### Run States

| State | Description |
|-------|-------------|
| `CREATED` | Run record created, waiting to start execution |
| `RUNNING` | Simulation actively executing |
| `SUCCEEDED` | Simulation completed successfully with results |
| `FAILED` | Simulation encountered an error and stopped |
| `CANCELLED` | User cancelled the run before completion |

###### API Workflow

Behind the scenes, the UI uses these APIs:

```bash
# 1. Create and start run
POST /api/simulation-runs
{
  "liftSystemId": 1,
  "versionId": 3,
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

##### Artefact Storage

Each simulation run produces a set of artefacts stored in a run-specific directory.

###### Directory Structure

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

###### Artefact Files

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
    "versionId": 3,
    "scenarioId": 5
  },
  "kpis": {
    "requestsTotal": 150,
    "passengersServed": 145,
    "passengersCancelled": 5,
    "avgWaitTicks": 25.5,
    "maxWaitTicks": 85,
    "idleTicks": 200,
    "movingTicks": 500,
    "doorTicks": 300,
    "utilisation": 0.80
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
      "utilisation": 0.80,
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

##### Reproducing UI Runs via CLI

You can reproduce any UI-driven run using the CLI by downloading the generated input files.

###### Step-by-Step Reproduction

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
   java -cp target/lift-simulator-0.49.11.jar \
     com.liftsimulator.scenario.ScenarioRunnerMain \
     run-42-reproduction.scenario
   ```

###### Example: Reproducing Run #42

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
java -cp target/lift-simulator-0.49.11.jar \
  com.liftsimulator.scenario.ScenarioRunnerMain \
  run-42-reproduction.scenario
```

**Expected Output:**

The CLI will produce the same simulation results as the UI run (given the same seed).

###### Verifying Reproduction

To verify the reproduction matches the original run:

1. Compare tick counts: Should match `durationTicks` in scenario
2. Compare KPIs: Should match `kpis` in `results.json` (wait times, utilization, etc.)
3. Compare passenger counts: Should match `requestsTotal`, `passengersServed`, etc.

**Note:** Results are deterministic when using the same seed value.

---

##### Example Scenario Walkthrough

This example demonstrates a complete workflow from UI run to CLI reproduction.

###### Scenario: Morning Rush Hour

**Setup:**
- Lift System: "Office Building Lifts"
- Published Version: v2 (10 floors, 2 lifts, Nearest Request strategy)
- Scenario: "Morning Rush - Ground to Upper Floors"
- Seed: 42

**Passenger Flows:**
- Ticks 0-100: Heavy traffic from ground floor to floors 5-10 (30 passengers)
- Ticks 200-300: Light return traffic from upper floors to ground (5 passengers)
- Duration: 500 ticks

###### UI Workflow

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
   - Passengers Served: 35 / 35
   - Avg Wait Time: 18 ticks
   - Max Wait Time: 45 ticks
   - Lift Utilization: 85%

###### Download Artefacts

From the Simulator page, download:
- `config.json` - Contains v2 configuration
- `scenario.json` - Contains passenger flows with seed 42
- `results.json` - Contains KPIs and metrics
- `run.log` - Contains execution trace

###### Reproduce via CLI

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
java -cp target/lift-simulator-0.49.11.jar \
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

##### Troubleshooting

###### Run Failed (Status: FAILED)

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

###### Where to Find Logs

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

###### Common Validation Errors

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

###### Run Stuck in CREATED State

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

###### Run Stuck in RUNNING State

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

###### Results Not Available After SUCCEEDED

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

###### CLI Reproduction Produces Different Results

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

#### Runtime Configuration API

The backend provides dedicated runtime APIs for retrieving published configurations. These APIs are read-only and return only configurations with `PUBLISHED` status.
Runtime endpoints require the configured API key header (default `X-API-Key`).

Example:
```bash
curl -H "X-API-Key: replace-with-secure-key" \\
  http://localhost:8080/api/v1/runtime/systems/building-a-lifts/config
```

- **Get Published Configuration**: `GET /api/v1/runtime/systems/{systemKey}/config`
  - Retrieves the currently published configuration for a lift system by system key
  - Returns the latest published version
  - Response (200 OK):
    ```json
    {
      "systemKey": "building-a-lifts",
      "displayName": "Building A Lift System",
      "versionNumber": 3,
      "config": "{\"minFloor\": 0, \"maxFloor\": 9, \"lifts\": 2, ...}",
      "publishedAt": "2026-01-11T14:30:00Z"
    }
    ```
  - Error (404 Not Found):
    - If lift system with the given key doesn't exist
    - If no published version exists for the system
    ```json
    {
      "status": 404,
      "message": "No published version found for lift system: building-a-lifts",
      "timestamp": "2026-01-11T15:00:00Z"
    }
    ```

- **Get Specific Published Version**: `GET /api/v1/runtime/systems/{systemKey}/versions/{versionNumber}`
  - Retrieves a specific version by system key and version number
  - Only returns the version if it is currently published
  - Response (200 OK): Same format as above
  - Error (404 Not Found):
    - If lift system doesn't exist
    - If version doesn't exist
    - If version exists but is not published (status is DRAFT or ARCHIVED)
    ```json
    {
      "status": 404,
      "message": "Version 2 is not published for lift system: building-a-lifts",
      "timestamp": "2026-01-11T15:00:00Z"
    }
    ```

- **Launch Local Simulator**: `POST /api/v1/runtime/systems/{systemKey}/simulate`
  - Writes the published configuration to a temporary JSON file
  - Spawns a local simulator process using the configuration
  - Response (200 OK):
    ```json
    {
      "success": true,
      "message": "Simulator started for system building-a-lifts using config lift-simulator-building-a-lifts-1234.json",
      "processId": 4242
    }
    ```
  - Error (404 Not Found):
    - If lift system with the given key doesn't exist
    - If no published version exists for the system

**Runtime Simulation Launch Assumptions:**
- The launcher uses the Java binary from `JAVA_HOME` (via `java.home`) to start child processes.
- **Local/dev mode**: launches the simulator with the current application classpath (`java -cp <classpath> com.liftsimulator.runtime.LocalSimulationMain`).
- **Packaged Spring Boot JARs**: launches the simulator using the Spring Boot `PropertiesLauncher` inside the packaged JAR (`java -cp <jar> org.springframework.boot.loader.launch.PropertiesLauncher --loader.main=...`).
- Simulator process output is captured and logged by the backend, and running processes are tracked for shutdown.

**Design Notes:**
- Runtime APIs use system key (not internal ID) for lookups
- Runtime APIs are read-only - no create, update, or delete operations
- Only published configurations are returned - draft and archived versions are hidden
- Lightweight response format optimized for runtime consumption
- Clear separation between admin APIs (management) and runtime APIs (consumption)

**Use Cases:**
- Lift simulator runtime fetching current configuration on startup
- Admin UI launching a local simulator instance for a published configuration
- Configuration service providing settings to running lift systems
- Monitoring systems checking which configuration version is active
- API clients that should only see production-ready configurations


#### Health & Monitoring

- **Custom Health Check**: `GET /api/v1/health`
  - Returns custom health status with service name and timestamp
- **Actuator Health**: `GET /actuator/health`
  - Returns detailed Spring Boot actuator health information
- **Actuator Info**: `GET /actuator/info`
  - Returns application information

