# Lift Simulator

A Java-based simulation of lift (elevator) controllers with a focus on correctness and design clarity.

## Version

Current version: **0.41.3**

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
java -jar target/lift-simulator-0.41.3.jar
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
java -jar target/lift-simulator-0.41.3.jar
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
      "updatedAt": "2026-01-11T10:00:00Z"
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
        "updatedAt": "2026-01-11T10:00:00Z"
      }
    ]
    ```

- **Get Lift System by ID**: `GET /api/lift-systems/{id}`
  - Returns a specific lift system by ID
  - Response (200 OK): Same as create response
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
      "config": "{\"floors\": 10, \"lifts\": 2}",
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
      "config": "{\"floors\": 10, \"lifts\": 2}",
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
      "config": "{\"floors\": 15, \"lifts\": 3}"
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
        "config": "{\"floors\": 15}",
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
        "config": "{\"floors\": 10}",
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
      "config": "{\"floors\": 10, \"lifts\": 2, \"travelTicksPerFloor\": 1, ...}"
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
          "field": "floors",
          "message": "Number of floors must be at least 2",
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

**Configuration Structure:**

All lift system configurations must conform to the following structure:

```json
{
  "floors": 10,
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

**Validation Rules:**

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `floors` | Integer | ≥ 2 | Number of floors in the building |
| `lifts` | Integer | ≥ 1 | Number of lift cars |
| `travelTicksPerFloor` | Integer | ≥ 1 | Ticks required to travel one floor |
| `doorTransitionTicks` | Integer | ≥ 1 | Ticks required for doors to open or close |
| `doorDwellTicks` | Integer | ≥ 1 | Ticks doors stay open before closing |
| `doorReopenWindowTicks` | Integer | ≥ 0, ≤ doorTransitionTicks | Window during door closing when doors can reopen |
| `homeFloor` | Integer | ≥ 0, < floors | Idle parking floor (must be within floor range) |
| `idleTimeoutTicks` | Integer | ≥ 0 | Ticks before idle parking behavior activates |
| `controllerStrategy` | Enum | NEAREST_REQUEST_ROUTING, DIRECTIONAL_SCAN | Controller algorithm |
| `idleParkingMode` | Enum | STAY_AT_CURRENT_FLOOR, PARK_TO_HOME_FLOOR | Idle parking behavior |

**Validation Features:**

- **Structural Validation**: Ensures JSON is well-formed and all required fields are present
- **Type Validation**: Validates field types and enum values
- **Domain Validation**: Enforces business rules and cross-field constraints
  - doorReopenWindowTicks must not exceed doorTransitionTicks
  - homeFloor must be within valid floor range (0 to floors-1)
- **Warnings**: Non-blocking suggestions for suboptimal configurations
  - Low doorDwellTicks values
  - More lifts than floors
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
      "config": "{\"floors\": 10, \"lifts\": 2, ...}",
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

The initial schema (V1) includes:
- `lift_simulator` - Application schema for lift configuration data (Flyway default)
- `lift_simulator.flyway_schema_history` - Flyway migration tracking (auto-created)
- `lift_system` - Lift system configuration roots
- `lift_system_version` - Versioned lift configuration payloads (JSONB)

Future migrations will extend lift configuration metadata, simulation runs, and other entities.

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

#### Verifying JPA Operations

To verify the JPA entities and repositories are working correctly, run the Spring Boot application with the JPA verification runner:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.jpa.verify=true"
```

Or with the JAR:

```bash
java -jar target/lift-simulator-0.41.3.jar --spring.jpa.verify=true
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

The current version (v0.41.3) includes comprehensive lift simulation and configuration management capabilities:

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
  - Delete system functionality with confirmation
  - Navigation between list and detail views
- **Version Management**: Comprehensive version control interface
  - List all versions for a lift system (ordered by version number)
  - Status badges (DRAFT, PUBLISHED, ARCHIVED) with color coding
  - Create new versions with JSON configuration input
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
- **Configuration Validator**: Interactive JSON editor for validating configurations
  - Live editing with syntax highlighting
  - Real-time validation using backend API
  - Sample configuration template provided
  - Split-pane layout showing editor and validation results
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

The packaged JAR will be in `target/lift-simulator-0.41.3.jar`.

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
java -cp target/lift-simulator-0.41.3.jar com.liftsimulator.Main
```

### Configuring the Demo

The demo supports selecting the controller strategy via command-line arguments:

```bash
# Show help
java -cp target/lift-simulator-0.41.3.jar com.liftsimulator.Main --help

# Run with the default demo configuration (nearest-request routing)
java -cp target/lift-simulator-0.41.3.jar com.liftsimulator.Main

# Run with directional scan controller
java -cp target/lift-simulator-0.41.3.jar com.liftsimulator.Main --strategy=directional-scan

# Run with nearest-request routing controller (explicit)
java -cp target/lift-simulator-0.41.3.jar com.liftsimulator.Main --strategy=nearest-request
```

**Available Options:**
- `-h, --help`: Show help message
- `--strategy=<strategy>`: Controller strategy to use (nearest-request or directional-scan)

The demo runs a pre-configured scenario with several lift requests and displays the simulation state at each tick.

### Running a Configured Simulation

Use a published configuration JSON file to run a lightweight simulation:

```bash
java -cp target/lift-simulator-0.41.3.jar com.liftsimulator.runtime.LocalSimulationMain --config=path/to/config.json
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
java -cp target/lift-simulator-0.41.3.jar com.liftsimulator.scenario.ScenarioRunnerMain path/to/scenario.scenario
```

### Configuring Scenario Runner

The scenario runner relies on scenario file settings for controller strategy and idle parking mode. The only command-line option is the help flag:

```bash
# Show help
java -cp target/lift-simulator-0.41.3.jar com.liftsimulator.scenario.ScenarioRunnerMain --help

# Run with default demo scenario
java -cp target/lift-simulator-0.41.3.jar com.liftsimulator.scenario.ScenarioRunnerMain

# Run a custom scenario
java -cp target/lift-simulator-0.41.3.jar com.liftsimulator.scenario.ScenarioRunnerMain custom.scenario
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
