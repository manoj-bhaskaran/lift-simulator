# Lift Simulator

A Java-based simulation of lift (elevator) controllers with a focus on correctness and design clarity.

Current version: **0.52.0**. This project follows [Semantic Versioning](https://semver.org/); see [CHANGELOG.md](CHANGELOG.md) for version history.

## What is this?

The Lift Simulator is an iterative project to model and test lift controller algorithms. It simulates:
- Lift state and movement (floor position, direction, door state)
- Controller decision logic (choosing which floor to service next)
- Request handling (hall calls and car calls)
- Time-based simulation using discrete ticks

The simulation is text-based and designed for clarity over visual appeal.

**Architecture assumption — single lift system per run:** The persistence model can store multiple lift systems and versions, but each simulation run executes exactly one selected lift system/version/scenario combination. This keeps scheduling, artefact capture, and KPI calculations deterministic. To compare buildings or controller strategies, create separate published versions and run them independently.

For a visual overview of the major components (React admin UI, Spring Boot backend, simulation engine, PostgreSQL, and artefact storage) and the main configuration and simulation-run flows, see [docs/architecture.md](docs/architecture.md).

## Quick Start

**Prerequisites**

- **Java 17+** — [Oracle](https://www.oracle.com/java/technologies/downloads/) or OpenJDK
- **Node.js 20.19+ or 22.12+** and npm — [nodejs.org](https://nodejs.org/)
- **PostgreSQL 12+** — [postgresql.org](https://www.postgresql.org/download/)
- **Maven 3.6+** — bundled with most Java IDEs, or [download separately](https://maven.apache.org/download.cgi)

**First-time setup (15–20 minutes)**

1. **Create the database, user, and schema** (Windows users: replace `sudo -u postgres psql` with `psql -U postgres`):
   ```bash
   sudo -u postgres psql
   ```
   ```sql
   CREATE DATABASE lift_simulator;
   CREATE USER lift_admin WITH PASSWORD 'YOUR_SECURE_PASSWORD';
   GRANT ALL PRIVILEGES ON DATABASE lift_simulator TO lift_admin;
   \c lift_simulator
   CREATE SCHEMA IF NOT EXISTS lift_simulator AUTHORIZATION lift_admin;
   GRANT ALL ON SCHEMA lift_simulator TO lift_admin;
   \q
   ```
2. **Configure application settings** — copy the template and set your database password and API credentials:
   ```bash
   cp src/main/resources/application-dev.yml.template src/main/resources/application-dev.yml
   ```
   Edit `application-dev.yml` and replace `CHANGE_ME` under `spring.datasource.password`, `security.admin.password`, and `api.auth.key`. This file is excluded from version control.
3. **Configure frontend API credentials** — create `frontend/.env.local` with values matching `application-dev.yml` so the browser client can send both backend auth schemes:
   ```bash
   VITE_ADMIN_USERNAME=admin
   VITE_ADMIN_PASSWORD=local-admin-password
   VITE_API_KEY=local-api-key
   ```
   `frontend/.env.local` is ignored by Git via the frontend `*.local` rule. Vite exposes `VITE_*` values to the browser bundle, so use only environment-appropriate credentials and never commit this file.
4. **Start the backend** with the `dev` profile so it loads `application-dev.yml` (the first run downloads dependencies and applies Flyway migrations):
   ```bash
   SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run
   ```
   The backend listens on **http://localhost:8080**.
5. **Start the frontend** in a new terminal:
   ```bash
   cd frontend
   npm install   # first time only
   npm run dev
   ```
   The frontend is served at **http://localhost:3000**.
6. **Open** http://localhost:3000 — you should see the Lift Simulator dashboard.

**Daily usage** (after first-time setup)

1. Start PostgreSQL (if it is not already running as a service).
2. Start the backend: `SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run` (from the project root).
3. Start the frontend: `cd frontend && npm run dev` (in a separate terminal).
4. Open http://localhost:3000 in your browser.

**Sample configurations** live in `src/main/resources/scenarios/`: `basic-office-building.json` (10-floor, 2-lift), `high-rise-residential.json` (30-floor, 4-lift), and `invalid-example.json` (validation errors, for testing).

**UAT testing** — follow the 14 detailed scenarios in [docs/UAT-TEST-SCENARIOS.md](docs/UAT-TEST-SCENARIOS.md) (estimated 2–3 hours, with expected results, pass/fail criteria, and a sign-off checklist).

**Quick troubleshooting** — most setup failures are: database not running (check credentials in `application-dev.yml`), a port conflict (8080 and 3000 must be free), the wrong Node version (`node --version`), or a migration failure (requires PostgreSQL 12+). See [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) for the full guide.

## Admin Interface

The project ships a Spring Boot backend (`Lift Config Service`) and a React single-page admin UI.

The **frontend admin UI** provides:
- **Dashboard** — overview of lift systems with quick statistics
- **Lift Systems Management** — full CRUD with list and detail views
- **Version Management** — create, publish, and archive versioned configurations with pagination, sorting, filtering, complete JSON examples, and schema guidance in the configuration editor
- **Scenario Builder** — template-based or custom passenger-flow scenarios with server-side validation, validated copy-to-version reuse, and an advanced JSON editor
- **Simulator Runs** — launch published versions with scenarios, poll status, bulk-cancel active runs, bulk-delete completed runs, and review KPI results with artefact downloads and CLI reproduction hints
- **Configuration Editor & Validator** — edit and validate configuration JSON before publishing
- **Health Check** — monitor backend service status

Run the UI in dev mode with `cd frontend && npm install && npm run dev`; it proxies API requests to the backend on port 8080. See [frontend/README.md](frontend/README.md) for setup, environment variables, and the type-definition (JSDoc) workflow.

The **backend REST API** is versioned under `/api/v1` (base URL `http://localhost:8080/api/v1`). Interactive documentation is available via Swagger UI at `/api/v1/swagger-ui.html` and OpenAPI JSON at `/api/v1/api-docs`. For the complete endpoint reference — lift systems, versions, scenarios, simulation runs, runtime configuration, health, and request/response examples — see [docs/API.md](docs/API.md). For CLI/UI run workflows, artefact reproduction, and simulation-run troubleshooting, see [docs/Workflows-and-Troubleshooting.md](docs/Workflows-and-Troubleshooting.md). See [ADR-0020](docs/decisions/0020-url-based-api-versioning.md) for the versioning strategy.

## Building the Project

Compile the project:
```bash
mvn clean compile
```

Build a backend-only JAR:
```bash
mvn clean package
```

Build a deployable Spring Boot JAR that also serves the React admin UI from `/` (activates the Maven `frontend` profile):
```bash
mvn -Pfrontend clean package
java -jar target/lift-simulator-0.52.0.jar
```

The `frontend` profile installs Node.js 20.19.0 (for Vite 7 compatibility), runs `npm ci`, builds the Vite bundle, and packages it under `BOOT-INF/classes/static/`. CI uses this profile so downloaded JAR artifacts include the frontend assets. Verify the packaged UI with:
```bash
jar tf target/lift-simulator-0.52.0.jar | grep '^BOOT-INF/classes/static/'
```

## Running the Application

Run the demo simulation:
```bash
mvn exec:java -Dexec.mainClass="com.liftsimulator.Main"
```

Or run the built JAR. The demo selects a controller strategy via command-line arguments:
```bash
java -cp target/lift-simulator-0.52.0.jar com.liftsimulator.Main --help
java -cp target/lift-simulator-0.52.0.jar com.liftsimulator.Main --strategy=directional-scan
```
`--strategy` accepts `nearest-request` (default) or `directional-scan`. The demo runs a pre-configured scenario and prints the simulation state at each tick.

Run a lightweight simulation from a published configuration JSON:
```bash
java -cp target/lift-simulator-0.52.0.jar com.liftsimulator.runtime.LocalSimulationMain --config=path/to/config.json
```
Optional flags: `--ticks=<count>` (default 25) and `-h, --help`.

Run scripted scenarios with the scenario runner — either the bundled demo or a custom file:
```bash
mvn exec:java -Dexec.mainClass="com.liftsimulator.scenario.ScenarioRunnerMain"
java -cp target/lift-simulator-0.52.0.jar com.liftsimulator.scenario.ScenarioRunnerMain path/to/scenario.scenario
```

Scenario files are plain text with metadata and tick-based event lines (parsing enforces limits of 1,000,000 ticks and 10,000 events per file); the controller strategy and idle-parking mode are taken from the scenario file. For the scenario file format and metadata keys, see the [CLI run workflows guide](docs/Workflows-and-Troubleshooting.md#cli-usage-unchanged). For simulation engine internals, controller strategy behaviour, out-of-service handling, request modelling, and the lift state machine, see [docs/DEVELOPER-GUIDE.md](docs/DEVELOPER-GUIDE.md).

## Testing

Run the full test suite:
```bash
mvn test
```

> **No local PostgreSQL needed.** Integration tests provision a throwaway PostgreSQL container via
> [Testcontainers](https://java.testcontainers.org/) and run the real Flyway migrations against it; the only
> requirement is a running **Docker** daemon. (CI uses its own PostgreSQL service container — see
> `.github/workflows/ci.yml`.)

The suite spans several levels:

- **Unit tests** — controllers, services, and domain logic: request lifecycle transitions (`LiftRequestTest`), artefact path/download/log/result handling (`ArtefactServiceTest`), simulation execution artefact and lifecycle orchestration (`SimulationRunExecutionServiceTest`), nearest-request and directional-scan routing, door handling and cancellation (`NaiveLiftControllerTest`, `DirectionalScanLiftControllerTest`), and the tick mechanism (`SimulationEngineTest`).
- **Integration tests** — full Spring context for REST APIs and repositories, including scenario CRUD, configuration-validation, runtime configuration, simulation-launch, and health controller APIs, multi-request scenarios, dynamic request addition during movement, cancellation, and out-of-service handling (`ScenarioControllerTest`, `ConfigValidationControllerTest`, `RuntimeConfigControllerTest`, `HealthControllerTest`, `DirectionalScanIntegrationTest`, `LiftRequestLifecycleTest`). Scenario fixtures live in `src/test/resources/scenarios`, controller API fixtures live under `src/test/java/com/liftsimulator/admin/controller/fixtures`, and batch-input golden files live under `src/test/resources/batch-input`.
- **Scenario tests** — `ControllerScenarioTest` exercises realistic multi-request routing for both controller strategies via a deterministic `ScenarioHarness`, guarding against behavioural regressions.
- **Playwright E2E tests** — browser smoke and feature tests under `frontend/e2e` run against the React dev server (port 3000) and a live backend (port 8080). See [ADR-0014](docs/decisions/0014-playwright-e2e-testing.md).

Run a single class or method:
```bash
mvn test -Dtest=ControllerScenarioTest
mvn test -Dtest=ControllerScenarioTest#testDirectionalScan_CanonicalScenario_FromReadme
```

The project enforces a minimum **80% line coverage** through JaCoCo; the build fails below this threshold. Generate the coverage report (available at `target/site/jacoco/index.html`):
```bash
mvn jacoco:report
```

For local frontend E2E runs, start the backend with credentials in one terminal, then run Playwright with matching variables in another:
```bash
# Terminal 1: repository root
export ADMIN_USERNAME=admin ADMIN_PASSWORD=local-admin-password API_KEY=local-api-key
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```
```bash
# Terminal 2: frontend/
E2E_ADMIN_USERNAME=admin E2E_ADMIN_PASSWORD=local-admin-password E2E_API_KEY=local-api-key npm test
```
In CI, the `e2e-playwright` job packages the backend with `mvn -Pfrontend package -DskipTests`, starts the packaged application, waits for `/api/v1/health` and the React root page, and runs `npm test` in `frontend/`.

## Quality Checks

```bash
mvn checkstyle:check        # code style
mvn spotbugs:check          # static analysis
mvn dependency-check:check  # dependency vulnerability scan
```

SpotBugs suppressions are limited to Spring-managed dependency injection in service constructors.

## Configuration

The backend is configured via YAML files under `src/main/resources/`:

- `application.yml` — base defaults (application name `lift-config-service`, server port `8080`, actuator `health`/`info`, root log level `INFO` and `DEBUG` for `com.liftsimulator`)
- `application-dev.yml` — development secrets and database settings (copy from `application-dev.yml.template`)
- `application-local.yml` — optional local-only overrides such as log paths or ports (copy from `application-local.yml.template`)

No profile is active in the checked-in base configuration, so launches must set `SPRING_PROFILES_ACTIVE` explicitly — for example `SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run` for development, `SPRING_PROFILES_ACTIVE=dev,local` to add local overrides (your `application-local.yml` is git-ignored, so `git pull` will not overwrite it), or `SPRING_PROFILES_ACTIVE=prod java -jar target/lift-simulator-0.52.0.jar` for production. This prevents a development profile from masking production configuration mistakes.

OpenAPI/Swagger access is controlled by `security.openapi.public-access` (`SECURITY_OPENAPI_PUBLIC_ACCESS`). It defaults to `true` to preserve local development behaviour; set it to `false` to require ADMIN-role authentication for the documentation endpoints.

## Authentication

The backend requires authentication for API access through two mechanisms:

- **Admin APIs** (`/api/v1/**` except `/api/v1/health`) use **HTTP Basic** authentication. Set credentials under `security.admin` in `application-dev.yml`, or via the `ADMIN_USERNAME` / `ADMIN_PASSWORD` environment variables.
- **Runtime and simulation APIs** (`/api/v1/runtime/**`, `/api/v1/simulation-runs/**`) use an **API key** in the `X-API-Key` header. Set `api.auth.key` (or the `API_KEY` environment variable); generate a key with `openssl rand -hex 32`.

The React admin UI sends both schemes from Vite environment variables when present. For local development, create `frontend/.env.local` with `VITE_ADMIN_USERNAME`, `VITE_ADMIN_PASSWORD`, and `VITE_API_KEY` values that match the backend credentials. The backend ignores the unused header for each endpoint, so one Axios client can safely send both headers on API requests.

Public endpoints requiring no authentication are `/api/v1/health`, `/actuator/health`, `/actuator/info`, and static/frontend routes. Unauthenticated requests return HTTP 401; authenticated requests lacking permission return HTTP 403.

Admin APIs support role-based access control with two roles: **ADMIN** (read and write) and **VIEWER** (read-only). Write methods (POST, PUT, PATCH, DELETE) require ADMIN; configure multiple users under `security.users` in `application-dev.yml`. Always keep credentials out of version control, prefer environment variables in production, and use HTTPS so HTTP Basic credentials are not exposed.

For runtime API-key setup and request/response examples, see [docs/API.md](docs/API.md). The security baseline, RBAC, and CORS/CSRF policies are documented in [ADR-0019](docs/decisions/0019-spring-security-baseline.md), [ADR-0021](docs/decisions/0021-role-based-access-control-rbac.md), and [ADR-0022](docs/decisions/0022-explicit-cors-csrf-policy.md).

## Rate Limiting

The API is protected by a token-bucket rate limiter (Bucket4j) applied per client IP address. Separate limits apply to the admin and runtime/simulation-run API groups.

Default thresholds (configurable in `application.yml` or overridden per environment):

| API group | Default capacity | Refill |
|-----------|-----------------|--------|
| Admin (`/api/v1/**`) | 100 requests | 100 per 60 s |
| Runtime / simulation-runs | 1 000 requests | 1 000 per 60 s |

When the limit is exceeded the server responds with **HTTP 429 Too Many Requests** and includes:
- `Retry-After: <seconds>` — how long to wait before retrying
- `X-RateLimit-Limit: <capacity>` — total bucket capacity
- `X-RateLimit-Remaining: 0` — tokens remaining (always 0 on a 429)

**Configuration reference** (`application.yml`):
```yaml
rate-limiting:
  enabled: true               # set to false to disable entirely (e.g. in integration tests)
  trust-forwarded-for: false  # set to true only behind a trusted reverse proxy
  admin:
    capacity: 100             # bucket capacity (max burst)
    refill-tokens: 100        # tokens added per period
    refill-period-seconds: 60
  runtime:
    capacity: 1000
    refill-tokens: 1000
    refill-period-seconds: 60
```

**`trust-forwarded-for`** — when `false` (the default), the rate-limiter uses `getRemoteAddr()` as the bucket key, which prevents callers from bypassing limits by spoofing `X-Forwarded-For`. Set to `true` only when the service runs exclusively behind a trusted reverse proxy that controls this header.

Override individual fields per Spring profile to tighten limits in production or loosen them for load testing.

## Database Setup

The backend uses PostgreSQL with Flyway for schema migrations (PostgreSQL 12+ required).

1. **Start PostgreSQL** if it is not already running:
   ```bash
   sudo service postgresql start   # Linux/Ubuntu
   brew services start postgresql  # macOS (Homebrew)
   ```
2. **Create the database, user, and schema** as shown in [Quick Start](#quick-start), or override credentials with the `DB_USERNAME` / `DB_PASSWORD` environment variables (useful for CI/CD pipelines and Docker deployments).
3. **Verify the connection:**
   ```bash
   psql -h localhost -U lift_admin -d lift_simulator
   ```

On startup the application runs Flyway automatically: it creates the `lift_simulator` schema and the `flyway_schema_history` table, then applies all pending migrations from `src/main/resources/db/migration/`.

The schema includes `lift_system` and `lift_system_version` (versioned JSONB configurations), `scenario` (reusable test scenarios), and `simulation_run`, which tracks run status (CREATED, RUNNING, SUCCEEDED, FAILED, CANCELLED) with referential integrity to lift systems and versions. See [ADR-0007](docs/decisions/0007-postgresql-flyway-integration.md) and [ADR-0008](docs/decisions/0008-jpa-entities-and-jsonb-mapping.md) for the persistence design.

For JPA entities, repositories, and verification-runner usage, see [docs/DEVELOPER-GUIDE.md#jpa-entities-and-repositories](docs/DEVELOPER-GUIDE.md#jpa-entities-and-repositories). For connection, permission, and migration errors, see [docs/TROUBLESHOOTING.md#database-troubleshooting](docs/TROUBLESHOOTING.md#database-troubleshooting). For backup and restore procedures, see [docs/DATABASE-BACKUP.md](docs/DATABASE-BACKUP.md).

## Logging

The backend uses Logback for logging with both console and file output. All logs are persisted to the `logs/` directory in the project root:

- **Main application log** (`logs/application.log`) — all levels; rotates at 10MB or daily at midnight; 30 days / 1GB retention; archived as `logs/application-YYYY-MM-DD.N.log`.
- **Error log** (`logs/application-error.log`) — ERROR level only; rotates at 10MB or daily; 90 days / 500MB retention.

Logging is configured via `src/main/resources/logback-spring.xml`, with profile-specific levels (DEBUG and verbose SQL for `dev`, INFO for `prod`) and full stack traces for all exceptions. Log files (`*.log`) are git-ignored; the `logs/` directory is preserved with a `.gitkeep` file.

View and search logs:
```bash
tail -f logs/application.log          # follow the main log
tail -f logs/application-error.log    # follow errors only
grep "ERROR" logs/application-*.log   # search across archives
```

To customise log locations without git conflicts, set them in `application-local.yml` (run with `SPRING_PROFILES_ACTIVE=dev,local`):
```yaml
logging:
  file:
    name: /custom/path/to/application.log
    path: /custom/path/to/logs
```
Alternatively, set `LOGGING_FILE_PATH` as an environment variable.

## Development Setup

This project ships an `.editorconfig` for consistent formatting across editors and IDEs (IntelliJ IDEA has built-in support; VS Code, Eclipse, and Vim/Neovim use plugins). It enforces UTF-8 encoding, LF line endings, 4-space indentation for Java and XML, trailing-whitespace removal, and final-newline insertion.

**Backend dependency baseline:** Spring Boot 3.4.13, Springdoc OpenAPI 2.7.0, PostgreSQL JDBC driver 42.7.11, and the Flyway PostgreSQL module (managed by Spring Boot dependency management).

**Commenting style:** use `//` for single-line comments and `/* */` for multi-line explanations, write non-obvious logic as complete sentences, and end comment sentences with periods.

## Project Structure

For a visual component-and-flow overview, see [docs/architecture.md](docs/architecture.md).

```
src/
├── main/java/com/liftsimulator/
│   ├── Main.java                          # Demo entry point (controller strategy via CLI)
│   ├── admin/                             # Spring Boot admin backend (Lift Config Service)
│   │   ├── LiftConfigServiceApplication.java  # Spring Boot main class
│   │   ├── config/                        # Security, CORS/CSRF, rate limiting, OpenAPI config
│   │   ├── security/                      # API-key authentication filter and config
│   │   ├── controller/                    # REST controllers (/api/v1)
│   │   ├── service/                       # Business logic services
│   │   │   └── metrics/                   # Run KPI/metrics models
│   │   ├── repository/                    # Spring Data JPA repositories
│   │   ├── entity/                        # JPA entities (lift system, version, scenario, run)
│   │   ├── dto/                           # Data transfer objects
│   │   └── runner/                        # Startup verification runner
│   ├── runtime/                           # Lightweight runtime/simulation API
│   │   ├── LocalSimulationMain.java       # Run a simulation from a config JSON (CLI)
│   │   ├── controller/                    # Runtime REST controllers
│   │   ├── service/                       # Runtime simulation services
│   │   └── dto/                           # Runtime data transfer objects
│   ├── scenario/                          # Scenario file parsing and scripted runner
│   │   ├── ScenarioRunnerMain.java        # Scripted scenario runner (CLI)
│   │   └── ...                            # Parser, definition, context, events
│   ├── domain/                            # Core domain models
│   │   ├── Action.java                    # Actions the lift can take
│   │   ├── Direction.java                 # UP, DOWN, IDLE
│   │   ├── DoorState.java                 # OPEN, CLOSED
│   │   ├── LiftRequest.java               # First-class request entity
│   │   ├── LiftState.java                 # Immutable lift state
│   │   ├── LiftStatus.java                # Lift state machine enum
│   │   ├── RequestState.java              # Request lifecycle enum
│   │   └── RequestType.java               # HALL_CALL or CAR_CALL
│   └── engine/                            # Simulation engine and controllers
│       ├── LiftController.java            # Controller interface
│       ├── ControllerFactory.java         # Builds a controller for the selected strategy
│       ├── NaiveLiftController.java       # Nearest-request routing controller
│       ├── DirectionalScanLiftController.java  # Directional-scan routing controller
│       ├── SimulationClock.java           # Deterministic simulation clock
│       ├── SimulationEngine.java          # Tick-based simulation engine
│       └── StateTransitionValidator.java  # State machine validator
├── main/resources/                        # application*.yml, scenarios/, db/migration/ (Flyway)
└── test/java/com/liftsimulator/           # Unit, integration, and scenario tests
```

## Architecture Decisions

See [docs/decisions](docs/decisions) for the full Architecture Decision Records (ADRs). For developer-facing simulation engine and persistence internals associated with these decisions, see [docs/DEVELOPER-GUIDE.md](docs/DEVELOPER-GUIDE.md).

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
- [ADR-0014: Playwright E2E Testing](docs/decisions/0014-playwright-e2e-testing.md)
- [ADR-0015: Test Management Platform Evaluation](docs/decisions/0015-test-management-platform-evaluation.md)
- [ADR-0016: Persistent Simulation Run Lifecycle](docs/decisions/0016-persistent-simulation-run-lifecycle.md)
- [ADR-0017: Batch Input Generator Backwards Compatibility](docs/decisions/0017-batch-input-generator-backwards-compatibility.md)
- [ADR-0018: API Key Authentication for Runtime Simulation](docs/decisions/0018-api-key-authentication-runtime-simulation.md)
- [ADR-0019: Spring Security Baseline](docs/decisions/0019-spring-security-baseline.md)
- [ADR-0020: URL-Based API Versioning](docs/decisions/0020-url-based-api-versioning.md)
- [ADR-0021: Role-Based Access Control (RBAC)](docs/decisions/0021-role-based-access-control-rbac.md)
- [ADR-0022: Explicit CORS and CSRF Policy](docs/decisions/0022-explicit-cors-csrf-policy.md)

## License

MIT License - see [LICENSE](LICENSE) file for details.
