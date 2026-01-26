# Lift Simulator

A Java-based simulation of lift (elevator) controllers with a focus on correctness and design clarity.

## Version

Current version: **0.45.0**

This project follows [Semantic Versioning](https://semver.org/). See [CHANGELOG.md](CHANGELOG.md) for version history.

## What is this?

The Lift Simulator is an iterative project to model and test lift controller algorithms. It simulates:
- Lift state and movement (floor position, direction, door state)
- Controller decision logic (choosing which floor to service next)
- Request handling (hall calls and car calls)
- Time-based simulation using discrete ticks

The simulation is text-based and designed for clarity over visual appeal.

## Quick Start for UAT

**New to the Lift Simulator? Follow these steps to get up and running quickly.**

### Prerequisites

- **Java 17+** - [Download from Oracle](https://www.oracle.com/java/technologies/downloads/) or use OpenJDK
- **Node.js 18+** and npm - [Download from nodejs.org](https://nodejs.org/)
- **PostgreSQL 12+** - [Download from postgresql.org](https://www.postgresql.org/download/)
- **Maven 3.6+** - Usually bundled with Java IDEs, or [download separately](https://maven.apache.org/download.cgi)

### First-Time Setup (15-20 minutes)

#### 1. Set Up PostgreSQL Database

Start PostgreSQL, then create the database and user:

```bash
# Connect to PostgreSQL as superuser
sudo -u postgres psql

# Execute these commands in the psql prompt:
CREATE DATABASE lift_simulator;
CREATE USER lift_admin WITH PASSWORD 'YOUR_SECURE_PASSWORD';
GRANT ALL PRIVILEGES ON DATABASE lift_simulator TO lift_admin;
\c lift_simulator
CREATE SCHEMA IF NOT EXISTS lift_simulator AUTHORIZATION lift_admin;
GRANT ALL ON SCHEMA lift_simulator TO lift_admin;
\q
```

**Windows users:** Replace `sudo -u postgres psql` with `psql -U postgres`

**Verification:**
```bash
psql -h localhost -U lift_admin -d lift_simulator
# Enter the password you set above when prompted
# You should see the PostgreSQL prompt
\q
```

See [Database Setup](#database-setup) section for detailed instructions.

#### 2. Configure Application Settings

Create your local configuration file:

```bash
cp src/main/resources/application-dev.yml.template src/main/resources/application-dev.yml
```

Edit `src/main/resources/application-dev.yml` and replace `CHANGE_ME` with your database password:

```yaml
spring:
  datasource:
    password: YOUR_SECURE_PASSWORD  # Replace CHANGE_ME with the password you set in step 1
```

**Note:** This file is excluded from version control and contains your local credentials.

#### 3. Start the Backend

From the project root directory:

```bash
mvn spring-boot:run
```

**Expected output:**
```
Started LiftConfigServiceApplication in X.XXX seconds
```

The backend will be available at **http://localhost:8080**

**Note:** First run will download Maven dependencies (may take a few minutes) and automatically run Flyway database migrations.

#### 4. Start the Frontend

Open a new terminal window, then:

```bash
cd frontend
npm install    # First time only - installs dependencies
npm run dev
```

**Expected output:**
```
VITE v7.x.x  ready in XXX ms

➜  Local:   http://localhost:3000/
```

The frontend will be available at **http://localhost:3000**

#### 5. Access the Application

Open your browser and navigate to **http://localhost:3000**

You should see the Lift Simulator dashboard.

### Daily Usage (After First-Time Setup)

Once set up, starting the application is quick:

1. **Start PostgreSQL** (if not already running as a service)
2. **Start backend:** `mvn spring-boot:run` (from project root)
3. **Start frontend:** `cd frontend && npm run dev` (in a separate terminal)
4. **Access:** Open http://localhost:3000 in your browser

### UAT Testing

Follow the comprehensive test scenarios in **[docs/UAT-TEST-SCENARIOS.md](docs/UAT-TEST-SCENARIOS.md)**

The UAT guide includes:
- 14 detailed test scenarios covering all major workflows
- Expected results for each test
- Pass/fail criteria
- Issue reporting template
- UAT sign-off checklist

**Estimated testing time:** 2-3 hours

### Sample Configurations

Sample lift configurations are available in `src/main/resources/scenarios/`:
- **basic-office-building.json** - Simple 10-floor, 2-lift system
- **high-rise-residential.json** - Complex 30-floor, 4-lift system
- **invalid-example.json** - Configuration with validation errors (for testing)

Use these as starting points or reference examples.

### Quick Troubleshooting

**Backend won't start:**
- Verify PostgreSQL is running: `psql -h localhost -U lift_admin -d lift_simulator`
- Check database credentials in `src/main/resources/application-dev.yml`
- Check port 8080 isn't already in use: `lsof -i :8080` (macOS/Linux) or `netstat -ano | findstr :8080` (Windows)

**StackOverflowError in backend logs when loading HTML routes:**
- Ensure the SPA forwarder does not match `/index.html` (the app now excludes it to prevent recursive forwards)
- Restart the backend after pulling the latest changes

**Backend returns 404 for index.html:**
- Build the frontend assets with `mvn -Pfrontend clean package` or run the frontend dev server at http://localhost:3000
- Verify `target/classes/static/index.html` exists after building the frontend bundle

**Frontend won't start:**
- Verify Node.js version: `node --version` (should be 18+)
- Delete `node_modules` and reinstall: `rm -rf node_modules && npm install`
- Check port 3000 isn't already in use

**Can't connect to backend from frontend:**
- Verify backend is running at http://localhost:8080/api/health
- Check browser console for CORS errors
- Vite dev proxy should handle this automatically

**Database migrations fail:**
- Drop and recreate database (see [Database Setup](#database-setup))
- Verify PostgreSQL version is 12+
- Check Flyway migration files in `src/main/resources/db/migration/`

**Scenario validation fails with "Unable to read scenario payload":**
- Confirm the scenario JSON is valid and not empty
- Retry the request after verifying the payload format

For detailed troubleshooting, see the relevant sections below.

---

## Admin Interface

The project includes both a backend API and a React-based frontend for managing lift simulator configurations.

### Frontend Admin UI

A modern React web application provides a user-friendly interface for managing lift systems:

#### Features
- **Dashboard**: Overview of lift systems with quick statistics
- **Lift Systems Management**: Full CRUD interface for lift systems with list/detail views and quick access to versions
- **Version Management**: Create, publish, and manage versioned configurations with pagination, sorting, and filtering
  - Paginate version lists (10/20/50/100 per page)
  - Sort by version number, creation date, or status
  - Filter by status (All/Published/Draft/Archived)
  - Search by version number
- **Scenario Builder**: Create and manage passenger flow scenarios for simulations
  - Build scenarios using template-based quick start or custom flows
  - Highlight the selected quick start template for clear visual feedback
  - Align the random seed checkbox with its label text for consistent form layout
  - Define passenger flows with origin, destination, timing, and passenger count
  - Server-side validation with detailed error and warning feedback
  - Advanced JSON editor mode for direct scenario editing
  - Optional random seed for reproducible simulations
- **Simulator Runs**: Launch published versions with scenarios, poll status, and review results
  - **Simulator landing page**: Choose a lift system and published version before configuring scenarios
  - **Run Simulator button**: Quick access button next to each published version for immediate simulation launch
  - Run setup with lift system, published version, and passenger flow scenario selection
  - Live status updates with elapsed time and progress details
  - Results view with KPI cards, per-lift/per-floor tables, artefact downloads, and CLI reproduction hints
- **Validation Feedback**: Display detailed configuration validation errors when version creation fails
- **Configuration Editor**: Edit configuration JSON with validation, save draft, and publish workflows
- **Configuration Validator**: Validate configuration JSON before publishing
- **Health Check**: Monitor backend service status

#### Running the Frontend (Dev Mode)

```bash
cd frontend
npm install
npm run dev
```

The frontend will start on **http://localhost:3000** and automatically proxy API requests to the backend on port 8080.

The frontend API base URL and request timeout can be configured via Vite environment variables. See the [frontend README](frontend/README.md#environment-variables) for details.

**See [frontend/README.md](frontend/README.md) for detailed setup instructions and documentation.**

#### Frontend Type Definitions (JSDoc)

The admin UI ships TypeScript declaration files for core data models to provide type-aware IntelliSense in JavaScript files without a full TypeScript migration. To opt in, add `// @ts-check` to the top of your file and reference the types in JSDoc:

```js
// @ts-check

/**
 * @param {import('../types/models').LiftSystem} system
 */
function renderSystem(system) {
  // IDE now knows system shape
}
```

The shared models live in `frontend/src/types/models.d.ts` and include LiftSystem, Version, and ValidationResult interfaces. See the frontend README for more details.

#### Production Build (Single App)

To package the React UI with the Spring Boot backend and serve everything from **http://localhost:8080**:

```bash
mvn -Pfrontend clean package
java -jar target/lift-simulator-0.45.0.jar
```

This builds the React app and bundles it into the Spring Boot JAR so the frontend is served from `/` and all API calls remain under `/api`.

### Backend API

The Spring Boot backend service (`Lift Config Service`) provides a RESTful API for managing lift simulator configurations.

#### Running the Backend

Start the Spring Boot application:

```bash
mvn spring-boot:run
```

Or build and run the JAR:

```bash
mvn clean package
java -jar target/lift-simulator-0.45.0.jar
```

The backend will start on `http://localhost:8080`.

### Available Endpoints

#### Lift System Management

- **Create Lift System**: `POST /api/lift-systems`
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

- **List All Lift Systems**: `GET /api/lift-systems`
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

- **Get Lift System by ID**: `GET /api/lift-systems/{id}`
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

- **Update Lift System**: `PUT /api/lift-systems/{id}`
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

- **Delete Lift System**: `DELETE /api/lift-systems/{id}`
  - Deletes a lift system and all its versions (cascade delete)
  - Response (204 No Content): Success with no body
  - Error (404 Not Found): If lift system doesn't exist

#### Version Management

- **Create Version**: `POST /api/lift-systems/{systemId}/versions`
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

- **Update Version Config**: `PUT /api/lift-systems/{systemId}/versions/{versionNumber}`
  - Updates the configuration JSON for a specific version
  - Request body:
    ```json
    {
      "config": "{\"minFloor\": 0, \"maxFloor\": 14, \"lifts\": 3}"
    }
    ```
  - Response (200 OK): Updated version details

- **List Versions**: `GET /api/lift-systems/{systemId}/versions`
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

- **Get Version**: `GET /api/lift-systems/{systemId}/versions/{versionNumber}`
  - Returns a specific version by version number
  - Response (200 OK): Version details
  - Error (404 Not Found): If version doesn't exist

- **Publish Version**: `POST /api/lift-systems/{systemId}/versions/{versionNumber}/publish`
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

- **Validate Configuration**: `POST /api/config/validate`
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

- **Create Scenario**: `POST /api/scenarios`
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

- **Update Scenario**: `PUT /api/scenarios/{id}`
  - Updates an existing scenario payload after validation
  - Request body: same as create
  - Response (200 OK): Updated scenario details

- **Get Scenario**: `GET /api/scenarios/{id}`
  - Returns a stored scenario by ID
  - Response (200 OK): Scenario details

#### Scenario Validation

- **Validate Scenario**: `POST /api/scenarios/validate`
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

#### Simulation Runs

Simulation runs execute asynchronously using stored lift system configurations and UI scenarios.

- **Start Simulation Run**: `POST /api/simulation-runs`
  - Creates a run record, writes input artefacts, and launches the simulation asynchronously.
  - Request body:
    ```json
    {
      "liftSystemId": 1,
      "versionId": 3,
      "scenarioId": 5
    }
    ```
  - Response (202 Accepted):
    ```json
    {
      "id": 42,
      "liftSystemId": 1,
      "versionId": 3,
      "scenarioId": 5,
      "status": "CREATED",
      "createdAt": "2026-02-01T10:00:00Z",
      "startedAt": null,
      "endedAt": null,
      "totalTicks": null,
      "currentTick": 0,
      "seed": null,
      "errorMessage": null,
      "artefactBasePath": "run-artefacts/run-42"
    }
    ```

- **Get Simulation Run**: `GET /api/simulation-runs/{id}`
  - Returns the current status, progress, and artefact location.

Run artefacts are stored on disk using the configured `simulation.runs.artefacts-root` directory
(defaults to `run-artefacts/`). Each run folder contains:

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

**Migration Guide (0.45.0 floor range update):**

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
- Creating a new version (`POST /api/lift-systems/{systemId}/versions`)
- Updating a version's configuration (`PUT /api/lift-systems/{systemId}/versions/{versionNumber}`)
- Publishing a version (`POST /api/lift-systems/{systemId}/versions/{versionNumber}/publish`)

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


#### Simulation Run APIs

The Simulation Run APIs enable UI to start simulations, poll their status, and access results/logs. These endpoints provide the complete lifecycle management for simulation execution.

**Key Features:**
- Create and start simulation runs atomically
- Poll run status with progress tracking
- Retrieve structured results when completed
- Access logs with optional tail functionality
- List and manage simulation artefacts
- Security controls to prevent path traversal attacks

---

##### Create and Start Simulation Run

**Endpoint:** `POST /api/simulation-runs`

Creates a new simulation run, sets up the artefact directory, and immediately starts execution.

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

**Endpoint:** `GET /api/simulation-runs/{id}`

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

##### Get Simulation Results

**Endpoint:** `GET /api/simulation-runs/{id}/results`

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

**Endpoint:** `GET /api/simulation-runs/{id}/logs?tail=N`

Retrieves simulation logs with optional tail functionality.

**Query Parameters:**
- `tail` (optional): Number of lines to retrieve from end of log
  - Default: All lines
  - Maximum: 10,000 lines

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

**Endpoint:** `GET /api/simulation-runs/{id}/artefacts`

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

**Security Features:**
- **Path Traversal Prevention**: All file access paths are normalized and validated
- **Directory Isolation**: Artefacts are restricted to run-specific directories
- **Secure Resolution**: Attempts to access files outside the artefact directory are blocked

**Configuration:**
```properties
# Base directory for simulation artefacts (application.properties)
simulation.artefacts.base-path=./simulation-runs
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
2. **UI-Driven Workflow (New in v0.45.0)**: Run simulations through the web interface with Version + Scenario selection

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
java -cp target/lift-simulator-0.45.0.jar \
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
java -cp target/lift-simulator-0.45.0.jar \
  com.liftsimulator.scenario.ScenarioRunnerMain \
  my-test-scenario.scenario

# Run with default demo scenario
java -cp target/lift-simulator-0.45.0.jar \
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
java -cp target/lift-simulator-0.45.0.jar \
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
java -cp target/lift-simulator-0.45.0.jar \
  com.liftsimulator.runtime.LocalSimulationMain \
  --config=building-a.json \
  --ticks=1000
```

###### Demo Simulation

Run a quick demo simulation with built-in configuration:

```bash
java -cp target/lift-simulator-0.45.0.jar \
  com.liftsimulator.Main \
  --strategy=directional-scan
```

**Options:**
- `--strategy=<strategy>` - Controller strategy to use
  - Valid values: `nearest-request`, `directional-scan`
  - Default: `nearest-request`
- `-h, --help` - Show help message

---

##### UI-Driven Run Workflow (New in v0.45.0)

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
```properties
# Base directory for simulation artefacts (application.properties)
simulation.artefacts.base-path=./simulation-runs
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
   java -cp target/lift-simulator-0.45.0.jar \
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
java -cp target/lift-simulator-0.45.0.jar \
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
java -cp target/lift-simulator-0.45.0.jar \
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
- Increase thread pool size in `application.properties`:
  ```properties
  simulation.execution.thread-pool-size=5
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

- **Get Published Configuration**: `GET /api/runtime/systems/{systemKey}/config`
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

- **Get Specific Published Version**: `GET /api/runtime/systems/{systemKey}/versions/{versionNumber}`
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

- **Launch Local Simulator**: `POST /api/runtime/systems/{systemKey}/simulate`
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

- **Custom Health Check**: `GET /api/health`
  - Returns custom health status with service name and timestamp
- **Actuator Health**: `GET /actuator/health`
  - Returns detailed Spring Boot actuator health information
- **Actuator Info**: `GET /actuator/info`
  - Returns application information

### Configuration

The backend is configured via `src/main/resources/application.properties`:
- Application name: `lift-config-service`
- Server port: `8080`
- Active profile: `dev` (default)
- Logging level: `INFO` (root), `DEBUG` (com.liftsimulator package)
- Actuator endpoints: health, info

### Logging

The backend uses Logback for comprehensive logging with both console and file output.

#### Log File Locations

All backend logs are persisted to the `logs/` directory in the project root:

- **Main application log**: `logs/application.log`
  - Contains all log messages (INFO, DEBUG, ERROR, etc.)
  - Automatically rotates when file reaches 10MB or daily at midnight
  - Keeps 30 days of history (max 1GB total)
  - Archived logs: `logs/application-YYYY-MM-DD.N.log`

- **Error log**: `logs/application-error.log`
  - Contains ERROR level messages only
  - Automatically rotates when file reaches 10MB or daily at midnight
  - Keeps 90 days of history (max 500MB total)
  - Archived logs: `logs/application-error-YYYY-MM-DD.N.log`

#### Log Configuration

Logging is configured via `src/main/resources/logback-spring.xml`:
- **Console Output**: Logs to stdout for development monitoring
- **File Output**: Logs to rotating files for debugging and audit trails
- **Full Stack Traces**: All exceptions include complete stack traces
- **Profile-Specific Levels**:
  - **Dev profile** (default): DEBUG level for application code, verbose SQL logging
  - **Prod profile**: INFO level for application code, reduced noise

#### Accessing Logs

**View recent logs:**
```bash
# Main application log (all levels)
tail -f logs/application.log

# Error log only
tail -f logs/application-error.log

# Last 100 lines of main log
tail -n 100 logs/application.log
```

**Search for specific errors:**
```bash
# Find all ERROR level messages
grep "ERROR" logs/application.log

# Find stack traces for a specific exception
grep -A 20 "NullPointerException" logs/application.log

# Search across all archived logs
grep "ERROR" logs/application-*.log
```

**View logs by date:**
```bash
# View logs from a specific date
cat logs/application-2026-01-17.0.log

# List all archived logs
ls -lht logs/
```

#### Log Retention

Log files are automatically managed:
- Files rotate when they reach 10MB in size
- Files also rotate daily at midnight
- Old files are automatically deleted after retention period
- Main log: 30 days retention, 1GB max total size
- Error log: 90 days retention, 500MB max total size

**Note**: Log files (`*.log`) are excluded from version control via `.gitignore`. The `logs/` directory structure is preserved with a `.gitkeep` file.

#### Customizing Log Locations

If you need to customize log file locations (e.g., different directory, external drive, or to avoid git pull conflicts), use **local configuration overrides**:

1. Create a local override file:
   ```bash
   cp src/main/resources/application-local.properties.template src/main/resources/application-local.properties
   ```

2. Edit `application-local.properties` and uncomment/set your custom paths:
   ```properties
   # Custom log location
   logging.file.name=/custom/path/to/application.log
   logging.file.path=/custom/path/to/logs
   ```

3. Run with the local profile:
   ```bash
   SPRING_PROFILES_ACTIVE=dev,local mvn spring-boot:run
   ```

**Alternative: Environment Variables**
```bash
export LOGGING_FILE_PATH=/custom/logs
mvn spring-boot:run
```

This approach prevents git conflicts - your `application-local.properties` file is excluded from version control, so `git pull` won't overwrite your local settings.

### Database Setup

The backend uses PostgreSQL with Flyway for schema migrations. Follow these steps to set up the database:

#### Prerequisites

- PostgreSQL 12 or later installed and running

#### Setup Steps

1. **Start PostgreSQL Service** (if not already running):
   ```bash
   # Linux/Ubuntu
   sudo service postgresql start

   # macOS with Homebrew
   brew services start postgresql
   ```

2. **Create Database and User**:
   ```bash
   # Connect to PostgreSQL as superuser
   sudo -u postgres psql

   # Execute these commands in the psql prompt:
   CREATE DATABASE lift_simulator;
   CREATE USER lift_admin WITH PASSWORD 'YOUR_SECURE_PASSWORD';
   GRANT ALL PRIVILEGES ON DATABASE lift_simulator TO lift_admin;
   \c lift_simulator
   CREATE SCHEMA IF NOT EXISTS lift_simulator AUTHORIZATION lift_admin;
   GRANT ALL ON SCHEMA lift_simulator TO lift_admin;
   \q
   ```

3. **Configure Application Settings**:

   Create your local configuration file from the template:
   ```bash
   cp src/main/resources/application-dev.yml.template src/main/resources/application-dev.yml
   ```

   **Edit** `src/main/resources/application-dev.yml` and replace `CHANGE_ME` with your database password:
   ```yaml
   spring:
     datasource:
       password: YOUR_SECURE_PASSWORD  # Replace CHANGE_ME with the password you set above
   ```

   **Important:**
   - The file `application-dev.yml` is excluded from version control (listed in `.gitignore`)
   - Never commit this file - it contains your local credentials
   - The template file (`application-dev.yml.template`) is version controlled for reference

   **Alternative: Environment Variables**

   You can override database credentials using environment variables instead of editing the file:
   ```bash
   export DB_USERNAME=lift_admin
   export DB_PASSWORD=your_secure_password
   mvn spring-boot:run
   ```

   This is especially useful for CI/CD pipelines or Docker deployments.

4. **Verify Database Connection**:
   ```bash
   psql -h localhost -U lift_admin -d lift_simulator
   # Enter the password you set when prompted
   ```

5. **Run the Application**:
When you start the Spring Boot application, Flyway will automatically:
   - Create the `lift_simulator` schema (if it does not exist yet)
   - Create the `flyway_schema_history` table inside the `lift_simulator` schema
   - Execute all pending migrations from `src/main/resources/db/migration/`
   - Initialize the schema with the baseline version

#### Configuration Profiles

The application supports different profiles for different environments:

- **dev** (default): Uses local PostgreSQL with connection pooling
  - **Template**: `src/main/resources/application-dev.yml.template` (version controlled)
  - **Local Config**: `src/main/resources/application-dev.yml` (create from template, **not** version controlled)
  - Database: `localhost:5432/lift_simulator`
  - Default user: `lift_admin` (customizable in your local config)

- **local** (optional): For local-only overrides without git conflicts
  - **Template**: `src/main/resources/application-local.properties.template` (version controlled)
  - **Local Config**: `src/main/resources/application-local.properties` (create from template, **not** version controlled)
  - **Use case**: Override log paths, server ports, or other settings locally
  - **Activation**: `SPRING_PROFILES_ACTIVE=dev,local` (combine with dev profile)
  - **Benefits**:
    - Your custom settings won't be overwritten by `git pull`
    - No git merge conflicts for environment-specific config
    - Each developer can have different local settings

**Using Multiple Profiles:**
```bash
# Activate both dev and local profiles
SPRING_PROFILES_ACTIVE=dev,local mvn spring-boot:run
```

**Using a Different Profile:**
```bash
# Use production profile
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

#### Database Schema

The schema includes the following tables:
- `lift_simulator` - Application schema for lift configuration data (Flyway default)
- `lift_simulator.flyway_schema_history` - Flyway migration tracking (auto-created)
- `lift_system` - Lift system configuration roots
- `lift_system_version` - Versioned lift configuration payloads (JSONB)
- `scenario` - Reusable test scenarios with JSON configuration (V3)
- `simulation_run` - Individual simulation run executions with lifecycle tracking (V3)

The `simulation_run` table tracks run status (CREATED, RUNNING, SUCCEEDED, FAILED, CANCELLED) and maintains referential integrity with lift systems and versions for persistent run lifecycle management.

### JPA Entities and Repositories

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
  - Progress tracking via `updateProgress(Long tick)`

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
java -jar target/lift-simulator-0.45.0.jar --spring.jpa.verify=true
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

**Test Database Configuration:**
- Tests use **H2 in-memory database** with PostgreSQL compatibility mode
- The H2 test database initializes the `lift_simulator` schema automatically for integration tests
- No external database required for running tests
- Schema is automatically created via JPA's `ddl-auto: create-drop`
- Flyway is disabled for tests (schema created from JPA entities)

Run the tests:
```bash
mvn test -Dtest=LiftSystemRepositoryTest,LiftSystemVersionRepositoryTest
```

Or run all tests:
```bash
mvn test
```

#### Troubleshooting

**Connection refused errors:**
- Ensure PostgreSQL is running: `sudo service postgresql status`
- Check the connection settings in `application-dev.yml`

**Permission denied errors:**
- Verify the database user has proper permissions: `GRANT ALL PRIVILEGES ON DATABASE lift_simulator TO lift_admin;`
- Ensure schema-level permissions: `GRANT ALL ON SCHEMA lift_simulator TO lift_admin;`

**Migration errors:**
- Check Flyway history: `SELECT * FROM flyway_schema_history;`
- For development, you can reset the database: `DROP DATABASE lift_simulator; CREATE DATABASE lift_simulator;`
- If `public.flyway_schema_history` exists from earlier runs, drop it and restart the app so Flyway recreates history in `lift_simulator`: `DROP TABLE public.flyway_schema_history;`
- If a legacy `public.schema_metadata` table exists from older releases, it can be dropped; current migrations do not use it: `DROP TABLE public.schema_metadata;`
- If you upgraded from 0.23.0 and see "Found more than one migration with version 1", run `mvn clean` once to clear stale build artifacts; the build now removes old migration resources automatically.
- If Flyway reports "No migrations found", rebuild with `mvn clean package` to refresh the packaged `db/migration` resources.

### Database Backup and Restore

The lift simulator's configuration database can be backed up and restored using PostgreSQL's native `pg_dump` and `pg_restore` utilities. Backups protect against data loss from hardware failure, operator error, or corruption.

#### Manual Ad-Hoc Backup

For immediate, on-demand backups, execute:

**Linux/macOS:**
```bash
pg_dump -h localhost -U lift_admin -d lift_simulator -F p -f lift_simulator_backup_$(date +%Y%m%d_%H%M%S).sql
```

**Windows (Command Prompt):**
```cmd
pg_dump -h localhost -U lift_admin -d lift_simulator -F p -f lift_simulator_backup_%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%.sql
```

**Windows (PowerShell):**
```powershell
pg_dump -h localhost -U lift_admin -d lift_simulator -F p -f "lift_simulator_backup_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql"
```

This creates a plain SQL backup file with a timestamp in the filename (e.g., `lift_simulator_backup_20260113_140530.sql`).

**When to use manual backups:**
- Before major schema migrations or application upgrades
- Before bulk data updates or deletions
- Before testing risky operations
- Before deploying to a new environment

#### Automated Scheduled Backup

Automated backups are managed via an external PowerShell script in the **My-Scripts** repository.

**Schedule**: Every Tuesday at 8:00 a.m. (Windows Task Scheduler)

**Script Location**: `My-Scripts/src/powershell/backup/Backup-LiftSimulatorDatabase.ps1`

**Command** (example local path, may vary):
```powershell
pwsh -File "C:\Users\manoj\Documents\Scripts\src\powershell\backup\Backup-LiftSimulatorDatabase.ps1"
```

**Backup Storage**:
- Backups: `D:\pgbackup\lift_simulator`
- Logs: `D:\pgbackup\lift_simulator\logs`

**Note**: Paths shown are local examples; your implementation may vary. Refer to the My-Scripts repository at `src/powershell/backup/README-LiftSimulator.md` for setup instructions, prerequisites, and configuration details.

#### Restore Procedure

**Standard Restore** (to existing database):

1. Stop the Spring Boot application to prevent writes during restore

2. Drop and recreate the database:

   **Linux/macOS:**
   ```bash
   sudo -u postgres psql -c "DROP DATABASE lift_simulator;"
   sudo -u postgres psql -c "CREATE DATABASE lift_simulator;"
   sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE lift_simulator TO lift_admin;"
   ```

   **Windows:**
   ```cmd
   psql -U postgres -c "DROP DATABASE lift_simulator;"
   psql -U postgres -c "CREATE DATABASE lift_simulator;"
   psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE lift_simulator TO lift_admin;"
   ```

3. Restore from backup file:

   **Linux/macOS:**
   ```bash
   psql -h localhost -U lift_admin -d lift_simulator -f lift_simulator_backup_YYYYMMDD_HHMMSS.sql
   ```

   **Windows:**
   ```cmd
   psql -h localhost -U lift_admin -d lift_simulator -f lift_simulator_backup_YYYYMMDD_HHMMSS.sql
   ```

4. Verify the restore:

   **Linux/macOS:**
   ```bash
   psql -h localhost -U lift_admin -d lift_simulator -c "\dt"
   psql -h localhost -U lift_admin -d lift_simulator -c "SELECT COUNT(*) FROM lift_system;"
   ```

   **Windows:**
   ```cmd
   psql -h localhost -U lift_admin -d lift_simulator -c "\dt"
   psql -h localhost -U lift_admin -d lift_simulator -c "SELECT COUNT(*) FROM lift_system;"
   ```

5. Restart the application

**Clean Restore** (to new machine or fresh install):

1. Install PostgreSQL 12 or later
2. Create the database and user as documented in the Database Setup section above
3. Restore from backup (step 3 from Standard Restore)
4. Verify the restore (step 4 from Standard Restore)
5. Start the application

#### Backup Verification

To verify a backup file is valid:

**Linux/macOS:**
```bash
# Check file size and format
ls -lh lift_simulator_backup_*.sql

# View first 20 lines (should show valid SQL)
head -n 20 lift_simulator_backup_*.sql
```

**Windows (Command Prompt):**
```cmd
REM Check file size
dir lift_simulator_backup_*.sql

REM View first 20 lines (should show valid SQL)
more /E +1 lift_simulator_backup_*.sql | findstr /N ".*" | findstr "^[1-9]: ^[12][0-9]:"
```

**Windows (PowerShell):**
```powershell
# Check file size
Get-ChildItem lift_simulator_backup_*.sql | Format-Table Name, Length, LastWriteTime

# View first 20 lines (should show valid SQL)
Get-Content lift_simulator_backup_*.sql -Head 20
```

**Periodic restore testing** (recommended quarterly):

**Linux/macOS:**
```bash
# Create test database
createdb lift_simulator_test

# Restore to test database
psql -U lift_admin -d lift_simulator_test -f lift_simulator_backup_YYYYMMDD_HHMMSS.sql

# Verify tables exist
psql -U lift_admin -d lift_simulator_test -c "\dt"

# Clean up
dropdb lift_simulator_test
```

**Windows:**
```cmd
REM Create test database
createdb lift_simulator_test

REM Restore to test database
psql -U lift_admin -d lift_simulator_test -f lift_simulator_backup_YYYYMMDD_HHMMSS.sql

REM Verify tables exist
psql -U lift_admin -d lift_simulator_test -c "\dt"

REM Clean up
dropdb lift_simulator_test
```

#### Important Notes

- Backups can be taken while the database is online (no application downtime required)
- Configuration data is **not** committed to version control; backups are the only recovery mechanism
- For detailed backup/restore architecture and automation setup, see ADR-0012 and the My-Scripts repository documentation
- Backup retention policy and log management are handled by the external backup script

## Features

The current version (v0.45.0) includes comprehensive lift simulation and configuration management capabilities:

### Admin Backend & REST API

- **Spring Boot Admin Backend**: RESTful API service for managing lift system configurations
- **PostgreSQL Database**: Persistent storage with Flyway migrations for schema management
- **JPA Entities**: Object-relational mapping with `LiftSystem` and `LiftSystemVersion` entities
- **JSONB Support**: PostgreSQL JSONB field mapping for flexible configuration storage
- **Lift System CRUD APIs**: Create, read, update, and delete lift systems
- **Version Management APIs**: Create, update, list, and retrieve versioned configurations
- **Configuration Validation Framework**: Comprehensive validation for configuration JSON
  - Structural validation with Jakarta Bean Validation
  - Domain validation for business rules and cross-field constraints
  - Detailed error messages with field-level granularity
  - Warning system for suboptimal configurations
  - Validation blocking on create, update, and publish operations
- **Publish/Archive Workflow**: Automatic state management for configuration versions
  - Publish mechanism with validation enforcement
  - Automatically archives previously published version when publishing a new one
  - Guarantees exactly one published configuration per lift system
  - Transactional workflow ensures atomic state transitions
- **Runtime Configuration API**: Dedicated read-only API for published configurations
  - Retrieves currently published configuration by system key
  - Filters for published status only (hides drafts and archived versions)
  - Streamlined response format optimized for runtime consumption
  - Clear separation between admin and runtime concerns
- **Global Exception Handling**: Consistent error responses with appropriate HTTP status codes
- **Health Endpoints**: Custom health checks and Spring Boot Actuator integration
- **Persistent File-Based Logging**: Comprehensive logging system for debugging and audit trails
  - Dual output: console (development) and rotating files (debugging/audit)
  - Automatic log rotation (daily and size-based) to prevent disk exhaustion
  - Separate error log for quick issue identification
  - Full stack traces preserved for all exceptions
  - Profile-specific log levels (verbose dev, production-ready prod)
  - Configurable via Logback with retention policies (30/90 days)

### React Admin UI

- **Modern Web Interface**: React 19.2.0 single-page application for managing lift systems
- **Vite Build Tool**: Fast development server with HMR and optimized production builds
- **Client-Side Routing**: React Router 7.12.0 for seamless navigation without page reloads
- **Dashboard**: Overview page with system statistics and quick actions
- **Lift Systems Management**: Complete CRUD interface for lift system configurations
  - List view with responsive card grid showing all lift systems
  - System key, display name, and description display
  - Version count and creation timestamps
  - Create new system modal with form validation
  - Detail view for individual lift systems with full metadata
  - Edit system metadata (display name and description)
  - Delete system functionality with confirmation
  - Navigation between list and detail views
- **Version Management**: Comprehensive version control interface
  - List all versions for a lift system (ordered by version number)
  - Status badges (DRAFT, PUBLISHED, ARCHIVED) with color coding
  - Create new versions with explicit validation workflow
    - JSON configuration input with dedicated validation button
    - Real-time validation with detailed error and warning messages
    - Create Version button disabled until configuration is validated
    - Split-pane layout with editor and validation results side-by-side
    - Prevents creation of invalid configurations
  - Edit existing version configurations with dedicated editor
  - Publish versions with validation and automatic archiving
  - View version configuration with expandable JSON display
  - Published/created timestamps for version tracking
- **Configuration Editor**: Full-featured JSON editor for version configurations
  - Edit configuration JSON with syntax highlighting in monospace textarea
  - Save draft functionality to persist changes without publishing
  - Real-time validation with detailed error and warning messages
  - Publish action with validation enforcement (blocks invalid configs)
  - Visual indicators for unsaved changes and last saved time
  - Read-only view for published and archived versions
  - Split-pane layout with editor and validation results side-by-side
- **Simulator Runs**: End-to-end UI flow for executing and monitoring simulation runs
  - **Run Simulator button**: Discoverable button next to each published version for quick access to simulation workflow
  - Launch runs from published versions (via button) or the dedicated Simulator landing page
  - Automatic preselection of lift system and version when launching from published version list
  - Poll run status with elapsed time, progress, and status indicators
  - Results rendering with KPI cards, per-lift/per-floor tables, artefact downloads, and CLI reproduction details
- **Configuration Validator**: Interactive JSON editor for validating configurations
  - Live editing with syntax highlighting
  - Real-time validation using backend API
  - Sample configuration template provided
  - Split-pane layout showing editor and validation results
  - Distinct display of errors and warnings with field-level detail
  - Clear indication of validation success with optional warnings
- **Health Check Monitor**: Real-time backend service health monitoring
  - Status display with color-coded indicators
  - Manual refresh capability
  - Detailed health information and error handling
- **API Integration**: Axios-based HTTP client with centralized service methods
  - Connects to all backend endpoints
  - Global error handling with interceptors
  - Structured API layer for maintainability
- **Development Proxy**: Vite proxy configuration for seamless local development
  - Frontend on port 3000, backend on port 8080
  - Automatic proxying of `/api/*` and `/actuator/*` requests
  - Eliminates CORS issues during development
- **JSDoc Documentation**: Comprehensive inline documentation for all components
  - Detailed JSDoc comments on all React components and utility functions
  - Props documentation with types and descriptions
  - Function parameter and return type annotations
  - Usage examples and feature descriptions
  - Improved IDE autocomplete and type hints
  - Enhanced developer onboarding and code maintainability

### Lift Simulation Engine

- **Selectable Controller Strategy**: Choose between NEAREST_REQUEST_ROUTING or DIRECTIONAL_SCAN algorithms
- **NaiveLiftController**: Simple controller that services the nearest pending request
- **DirectionalScanLiftController**: SCAN-style algorithm with direction commitment and batching
- **Hall-Call Direction Filtering**: Direction-aware request servicing for efficient routing
- **Request Lifecycle Management**: First-class request entities with explicit lifecycle states (CREATED → QUEUED → ASSIGNED → SERVING → COMPLETED/CANCELLED)
- **Request Cancellation**: Cancel any request by ID before completion
- **Out-of-Service Functionality**: Safe maintenance mode with automatic request cancellation
- **Formal Lift State Machine**: 7 explicit states (IDLE, MOVING_UP, MOVING_DOWN, DOORS_OPENING, DOORS_OPEN, DOORS_CLOSING, OUT_OF_SERVICE)
- **State Transition Validation**: Enforces valid state changes for both lift and requests
- **Single Source of Truth**: LiftStatus is the only stored state; all other properties are derived
- **Tick-Based Simulation**: Discrete time advancement with configurable durations
- **Simulation Clock**: Deterministic tick progression for reproducible simulations
- **Configurable Door Behavior**:
  - Symmetric door opening/closing as transitional states
  - Configurable door transition, dwell, and reopen window timing
- **Configurable Idle Parking**: STAY_AT_CURRENT_FLOOR or PARK_TO_HOME_FLOOR modes
- **Request Types**: Car calls (from inside) and hall calls (from floor with direction)
- **Safety Enforcement**: Prevents moving with doors open or opening doors while moving

### Testing & Quality

- **Comprehensive Test Coverage**: 80%+ line coverage requirement with JaCoCo
- **Unit Tests**: Extensive unit tests for controllers, services, and domain logic
- **Integration Tests**: Full Spring context testing for REST APIs and repositories
- **Scenario Tests**: Realistic multi-request routing scenarios for both controller strategies
- **Code Quality Tools**: Checkstyle, SpotBugs, OWASP Dependency Check

### Developer Tools

- **Scenario Runner**: Scripted simulations with tick-based events and lifecycle summaries
- **Console Output**: Tick-by-tick lift state visualization with request lifecycle tracking
- **Request Lifecycle Visibility**: Compact status display (Q:n, A:n, S:n) and summary tables
- **Command-Line Configuration**: Configurable controller strategy and simulation parameters
- **EditorConfig**: Consistent code formatting across editors

### Documentation

- **Comprehensive README**: API documentation, setup guides, and usage examples
- **Architecture Decision Records (ADRs)**: 9 ADRs documenting key design decisions
- **Changelog**: Detailed version history following Keep a Changelog format
- **Inline Documentation**: Extensive Javadoc comments throughout the codebase

Future iterations will add multi-lift systems, coordination algorithms, request priorities, and more realistic constraints.

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

## Commenting Style

Use the following commenting conventions throughout the codebase:

- Use `//` for single-line comments.
- Use `/* */` for multi-line explanations.
- End comment sentences with periods.
- Use complete sentences for non-obvious logic.

## Building the Project

Compile the project using Maven:

```bash
mvn clean compile
```

To build a JAR package:

```bash
mvn clean package
```

The packaged JAR will be in `target/lift-simulator-0.45.0.jar`.

## Running Tests

Run the test suite with Maven:

```bash
mvn test
```

The test suite includes integration coverage for the scenario system using fixtures in
`src/test/resources/scenarios`.
It also exercises simulation run lifecycle polling and batch input generator contracts
with golden files under `src/test/resources/batch-input`.

## Quality Checks

Run code style checks:

```bash
mvn checkstyle:check
```

Run static analysis:

```bash
mvn spotbugs:check
```

SpotBugs suppressions are limited to Spring-managed dependency injection in service constructors.

Run dependency vulnerability checks:

```bash
mvn dependency-check:check
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
java -cp target/lift-simulator-0.45.0.jar com.liftsimulator.Main
```

### Configuring the Demo

The demo supports selecting the controller strategy via command-line arguments:

```bash
# Show help
java -cp target/lift-simulator-0.45.0.jar com.liftsimulator.Main --help

# Run with the default demo configuration (nearest-request routing)
java -cp target/lift-simulator-0.45.0.jar com.liftsimulator.Main

# Run with directional scan controller
java -cp target/lift-simulator-0.45.0.jar com.liftsimulator.Main --strategy=directional-scan

# Run with nearest-request routing controller (explicit)
java -cp target/lift-simulator-0.45.0.jar com.liftsimulator.Main --strategy=nearest-request
```

**Available Options:**
- `-h, --help`: Show help message
- `--strategy=<strategy>`: Controller strategy to use (nearest-request or directional-scan)

The demo runs a pre-configured scenario with several lift requests and displays the simulation state at each tick.

### Running a Configured Simulation

Use a published configuration JSON file to run a lightweight simulation:

```bash
java -cp target/lift-simulator-0.45.0.jar com.liftsimulator.runtime.LocalSimulationMain --config=path/to/config.json
```

Optional flags:
- `--ticks=<count>`: Number of ticks to simulate (default: 25)
- `-h, --help`: Show help message

## Running Scripted Scenarios

Run the scenario runner with the bundled demo scenario:

```bash
mvn exec:java -Dexec.mainClass="com.liftsimulator.scenario.ScenarioRunnerMain"
```

Or run a custom scenario file:

```bash
java -cp target/lift-simulator-0.45.0.jar com.liftsimulator.scenario.ScenarioRunnerMain path/to/scenario.scenario
```

### Configuring Scenario Runner

The scenario runner relies on scenario file settings for controller strategy and idle parking mode. The only command-line option is the help flag:

```bash
# Show help
java -cp target/lift-simulator-0.45.0.jar com.liftsimulator.scenario.ScenarioRunnerMain --help

# Run with default demo scenario
java -cp target/lift-simulator-0.45.0.jar com.liftsimulator.scenario.ScenarioRunnerMain

# Run a custom scenario
java -cp target/lift-simulator-0.45.0.jar com.liftsimulator.scenario.ScenarioRunnerMain custom.scenario
```

**Available Options:**
- `-h, --help`: Show help message

Scenario file settings take precedence over defaults.

Scenario files are plain text with metadata and event lines. Scenario parsing enforces limits of 1,000,000 ticks and 10,000 events per file:

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

Each event executes at the specified tick, and the output logs the tick, floor, lift state, and pending requests to help validate complex behavior. After the run, a request lifecycle summary table lists when each request was created and completed or cancelled.
The scenario runner automatically expands the default floor range (0–10) to include any requested floors, so negative floors in scripted scenarios are supported without extra configuration.
If you set any of the scenario parameters (e.g., `door_dwell_ticks`), the scenario runner uses them to configure the controller and simulation engine.

Scenario metadata keys:

- **min_floor** / **max_floor**: floor bounds used for the simulation (still expanded to include requested floors)
- **initial_floor**: starting floor for the lift (clamped to the final min/max range)
- **travel_ticks_per_floor**: ticks required to travel one floor
- **door_transition_ticks**: ticks required to open or close doors
- **door_dwell_ticks**: ticks doors stay open before closing
- **door_reopen_window_ticks**: ticks during door closing when doors can reopen (0 disables)
- **home_floor**: idle parking floor for the naive controller (used with `PARK_TO_HOME_FLOOR` mode)
- **idle_timeout_ticks**: idle ticks before the parking behavior activates
- **idle_parking_mode**: parking behavior when idle (`STAY_AT_CURRENT_FLOOR` or `PARK_TO_HOME_FLOOR`, optional, defaults to `PARK_TO_HOME_FLOOR`)
- **controller_strategy**: controller algorithm to use (`NEAREST_REQUEST_ROUTING` or `DIRECTIONAL_SCAN`, optional, defaults to `NEAREST_REQUEST_ROUTING`)

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

## Selecting Controller Strategy

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

## Controller Strategies

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
│   ├── admin/                             # Spring Boot admin backend
│   │   ├── LiftConfigServiceApplication.java  # Spring Boot main class
│   │   ├── controller/                    # REST controllers
│   │   │   └── HealthController.java      # Health check endpoint
│   │   ├── service/                       # Business logic services
│   │   ├── repository/                    # Data access layer
│   │   ├── domain/                        # Backend domain models
│   │   └── dto/                           # Data transfer objects
│   ├── domain/                            # Core domain models
│   │   ├── Action.java                    # Actions the lift can take
│   │   ├── CarCall.java                   # Request from inside lift (legacy)
│   │   ├── Direction.java                 # UP, DOWN, IDLE
│   │   ├── DoorState.java                 # OPEN, CLOSED
│   │   ├── HallCall.java                  # Request from a floor (legacy)
│   │   ├── LiftRequest.java               # First-class request entity
│   │   ├── LiftState.java                 # Immutable lift state
│   │   ├── LiftStatus.java                # Lift state machine enum
│   │   ├── RequestState.java              # Request lifecycle enum
│   │   └── RequestType.java               # HALL_CALL or CAR_CALL
│   └── engine/                            # Simulation engine and controllers
│       ├── LiftController.java            # Controller interface
│       ├── NaiveLiftController.java       # Simple nearest-floor controller
│       ├── SimpleLiftController.java      # Alternative basic controller
│       ├── SimulationClock.java           # Deterministic simulation clock
│       ├── SimulationEngine.java          # Tick-based simulation engine
│       └── StateTransitionValidator.java  # State machine validator
└── test/java/com/liftsimulator/
    ├── domain/
    │   └── LiftRequestTest.java                 # Request lifecycle tests
    ├── engine/
    │   ├── ControllerScenarioTest.java          # Scenario-based routing tests
    │   ├── DirectionalScanIntegrationTest.java  # Directional controller integration tests
    │   ├── LiftRequestLifecycleTest.java        # Controller integration tests
    │   ├── NaiveLiftControllerTest.java         # Controller unit tests
    │   ├── OutOfServiceTest.java                # Out-of-service tests
    │   └── SimulationEngineTest.java            # Engine unit tests
    └── ...                                      # Additional tests
```

## Testing

The project includes comprehensive test coverage across multiple levels:

### Unit Tests
- **LiftRequestTest**: Tests request lifecycle state transitions (CREATED → QUEUED → ASSIGNED → SERVING → COMPLETED)
- **NaiveLiftControllerTest**: 50+ tests covering nearest-request logic, door handling, cancellation, idle parking
- **DirectionalScanLiftControllerTest**: Tests direction selection, commitment, reversal, hall call filtering
- **SimulationEngineTest**: Tests tick mechanism, state transitions, door cycles

### Integration Tests
- **DirectionalScanIntegrationTest**: End-to-end tests with SimulationEngine
  - Multi-request scenarios
  - Dynamic request addition during movement
  - Cancellation handling
  - Out-of-service scenarios
  - Direction-aware scheduling validation
- **LiftRequestLifecycleTest**: Tests request state tracking through full simulation

### Scenario Tests (NEW)
- **ControllerScenarioTest**: Comprehensive scenario-based test suite for both controller strategies
  - Provides `ScenarioHarness` utility for deterministic scenario testing
  - Tests realistic multi-request routing scenarios
  - Validates service order, direction transitions, and queue management
  - Protects both NaiveLift and DirectionalScan strategies from behavioral regressions

**Example scenario tests:**
- **Canonical DirectionalScan scenario**: Validates deferred hall call servicing (from README documentation)
- **Mixed calls while moving**: Tests direction commitment with requests above and below current position
- **Idle → commit → clear → reverse**: Tests complete direction selection and reversal cycle
- **Comparison tests**: Runs identical scenarios with both strategies to highlight behavioral differences

### Running Tests

Run all tests:
```bash
mvn test
```

Run specific test class:
```bash
mvn test -Dtest=ControllerScenarioTest
```

Run specific test method:
```bash
mvn test -Dtest=ControllerScenarioTest#testDirectionalScan_CanonicalScenario_FromReadme
```

Generate coverage report (requires 80% line coverage):
```bash
mvn jacoco:report
# Report available at target/site/jacoco/index.html
```

### Test Coverage Requirements

The project enforces a minimum of **80% line coverage** through JaCoCo. The build will fail if coverage falls below this threshold.

## Architecture Decisions

See [docs/decisions](docs/decisions) for Architecture Decision Records (ADRs):
- [ADR-0001: Tick-Based Simulation](docs/decisions/0001-tick-based-simulation.md)
- [ADR-0002: Single Source of Truth for Lift State](docs/decisions/0002-single-source-of-truth-state.md)
- [ADR-0003: Request Lifecycle Management](docs/decisions/0003-request-lifecycle-management.md)
- [ADR-0004: Configurable Idle Parking Mode](docs/decisions/0004-configurable-idle-parking-mode.md)
- [ADR-0005: Selectable Controller Strategy](docs/decisions/0005-selectable-controller-strategy.md)
- [ADR-0006: Spring Boot Admin Backend](docs/decisions/0006-spring-boot-admin-backend.md)
- [ADR-0007: PostgreSQL and Flyway Integration](docs/decisions/0007-postgresql-flyway-integration.md)
- [ADR-0008: JPA Entities and JSONB Mapping](docs/decisions/0008-jpa-entities-and-jsonb-mapping.md)
- [ADR-0009: Configuration Validation Framework](docs/decisions/0009-configuration-validation-framework.md)
- [ADR-0010: Publish/Archive Workflow](docs/decisions/0010-publish-archive-workflow.md)
- [ADR-0011: React Admin UI Scaffold](docs/decisions/0011-react-admin-ui-scaffold.md)
- [ADR-0012: Database Backup and Restore Strategy](docs/decisions/0012-database-backup-restore-strategy.md)
- [ADR-0013: Strict Schema Validation for Unknown Fields](docs/decisions/0013-strict-schema-validation-unknown-fields.md)

## License

MIT License - see [LICENSE](LICENSE) file for details.
