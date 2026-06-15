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
      "config": "{\"minFloor\": 0, \"maxFloor\": 9, \"lifts\": 2, \"travelTicksPerFloor\": 1, \"doorTransitionTicks\": 2, \"doorDwellTicks\": 3, \"doorReopenWindowTicks\": 2, \"homeFloor\": 0, \"idleTimeoutTicks\": 5, \"controllerStrategy\": \"NEAREST_REQUEST_ROUTING\", \"idleParkingMode\": \"PARK_TO_HOME_FLOOR\"}"
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

#### Batch Input Generator

The batch input generator converts stored scenario definitions and lift configurations into the legacy `.scenario` file format used by the CLI simulator. It exists as a backwards-compatibility bridge so scenarios authored through the UI/API can be replayed by the existing CLI batch infrastructure without manual conversion. It runs internally as part of starting a simulation run; there is no standalone public endpoint.

| Direction | Format |
|-----------|--------|
| Input | Lift system version configuration plus scenario JSON (passenger flows) — see [Scenario Management](#scenario-management) for the scenario schema and field constraints |
| Output | A `.scenario` file in the format consumed by the CLI runner — see the Scenario Runner section in [Workflows and Troubleshooting](Workflows-and-Troubleshooting.md) |

##### Configuration Structure

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

If validation fails with errors, the operation will be rejected with a 400 Bad Request response containing detailed error information. The response body uses the same `valid` / `errors` / `warnings` shape shown in the [Configuration Validation](#configuration-validation) examples above.

#### API Key Authentication (Runtime & Simulation Execution)

Runtime and simulation execution endpoints require an API key so they can be invoked from CLI tooling and automation.

**Security Requirements:**
- **Startup Validation**: The application requires `api.auth.key` to be configured and non-empty. If missing or blank, the application will fail to start with a clear error message.
- **Secure Comparison**: API keys are compared using SHA-256 hashing with constant-time comparison to prevent timing attacks and credential leakage.

**Configuration:**
- `api.auth.key` (required): API key value used for authentication. Must be non-empty and provided via environment variable or property file.
- `api.auth.header` (optional, default: `X-API-Key`): Header name to read the key from

**Setting Up API Key:**

Generate a secure random API key (any method producing sufficient entropy is acceptable):

```bash
# Using OpenSSL (32 bytes of entropy)
openssl rand -base64 32
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

### Standard Response DTOs

Controllers return typed DTOs for JSON payloads rather than ad-hoc maps. Error payloads use the shared `ErrorResponse` shape (`status`, `message`, `timestamp`), bean-validation failures use `ValidationErrorResponse` (`status`, `message`, `fieldErrors`, `timestamp`), and validation endpoints continue to return typed validation responses containing `valid`, `errors`, and `warnings`.


For step-by-step CLI usage, UI-driven run workflows, artefact reproduction, the Morning Rush walkthrough, and troubleshooting scenarios, see [Workflows and Troubleshooting](Workflows-and-Troubleshooting.md).

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

| Status | Description |
|--------|-------------|
| `CREATED` | Run created but not yet started |
| `RUNNING` | Simulation is currently executing |
| `SUCCEEDED` | Simulation completed successfully |
| `FAILED` | Simulation failed with error |
| `CANCELLED` | Simulation was cancelled |

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
  "runId": 1,
  "logs": "Starting simulation...\nTick 0: Initializing lifts\nTick 1: Processing requests\n...",
  "tail": 100,
  "error": null
}
```

**Common Log Files (searched in order):**
- `simulation.log`
- `output.log`
- `run.log`

**Error Response (500 Internal Server Error):**
```json
{
  "runId": 1,
  "logs": null,
  "tail": null,
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

For the layout of each run folder and the files it contains, see [Run Artefacts and Results Schema](#run-artefacts-and-results-schema) above.

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
      "config": "{\"minFloor\": 0, \"maxFloor\": 9, \"lifts\": 2, \"travelTicksPerFloor\": 1, \"doorTransitionTicks\": 2, \"doorDwellTicks\": 3, \"doorReopenWindowTicks\": 2, \"homeFloor\": 0, \"idleTimeoutTicks\": 5, \"controllerStrategy\": \"NEAREST_REQUEST_ROUTING\", \"idleParkingMode\": \"PARK_TO_HOME_FLOOR\"}",
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
