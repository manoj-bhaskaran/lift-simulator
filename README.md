# Lift Simulator

A Java-based simulation of lift (elevator) controllers with a focus on correctness and design clarity.

Current version: **0.57.17**. This project follows [Semantic Versioning](https://semver.org/); see [CHANGELOG.md](CHANGELOG.md) for version history.

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

- **Java 17+** (Java 21 LTS recommended; CI builds and tests on 21) — [Oracle](https://www.oracle.com/java/technologies/downloads/) or OpenJDK
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
   Edit `application-dev.yml` and replace `CHANGE_ME` under `spring.datasource.password`, `security.admin.password`, and `api.auth.key`. Backend startup rejects blank values and the `CHANGE_ME` placeholder for admin and API-key secrets, so generate unique values before starting the app. This file is excluded from version control.
3. **Configure frontend API credentials** — create `frontend/.env.local` with values matching `application-dev.yml` so the browser client can send both backend auth schemes:
   ```bash
   VITE_ADMIN_USERNAME=admin
   VITE_ADMIN_PASSWORD=local-admin-password
   VITE_API_KEY=local-api-key
   ```
   `frontend/.env.local` is ignored by Git via the frontend `*.local` rule. Vite exposes `VITE_*` values to the browser bundle, so treat `VITE_ADMIN_PASSWORD` and `VITE_API_KEY` as local-development conveniences only: anyone who loads a built SPA can inspect them. Do not deploy hosted frontend bundles with real backend credentials; for hosted environments, put authentication behind a backend/session proxy and keep secrets server-side. Never commit this file.
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
- **Version Management** — create, publish, and archive versioned configurations with optimistic locking, single-published-version enforcement, pagination, sorting, filtering, complete JSON examples, and schema guidance in the configuration editor
- **Scenario Builder** — template-based or custom passenger-flow scenarios with server-side validation, validated copy-to-version reuse, run-history deletion impact warnings, and an advanced JSON editor
- **Simulator Runs** — launch published versions with scenarios, poll status, recover orphaned active runs after restarts, list runs with capped pagination and documented sorting (`createdAt`, `startedAt`, `endedAt`, `status`, `id`; ignore-case modifiers rejected), bulk-cancel active runs, bulk-delete completed runs with post-commit best-effort artefact cleanup, clean scenario- and lift-system-cascade artefacts after confirmed deletion on the same best-effort basis, and review pickup-latency KPI results with artefact downloads and CLI reproduction hints. Generated `input.scenario` artefacts skip same-floor passenger flows so CLI reproduction matches the in-process executor. Run creation uses the same lift-system-scoped `versionNumber` identifier as the version-management API, avoiding surrogate version row IDs in client requests.
- **Configuration Editor & Validator** — edit and validate configuration JSON before publishing
- **Health Check** — monitor backend service status


> **KPI scope:** The current simulator models the passenger pickup leg for each scenario flow. Result KPIs therefore report pickup-request completions, wait-to-pickup latency, and pickup-leg lift utilisation; destination floors are retained for per-floor demand reporting but destination travel is not simulated yet. Stored historical artefacts that use the previous `utilisation` name are still rendered as pickup-leg utilisation in the UI.

Run the UI in dev mode with `cd frontend && npm install && npm run dev`; it proxies API requests to the backend on port 8080. See [frontend/README.md](frontend/README.md) for setup, environment variables, and the type-definition (JSDoc) workflow.

The **backend REST API** is versioned under `/api/v1` (base URL `http://localhost:8080/api/v1`). The complete, always-current endpoint reference is generated from the code by SpringDoc and served as interactive Swagger UI at `/api/v1/swagger-ui.html` and OpenAPI JSON at `/api/v1/api-docs`. API conventions — authentication, role-based access control, versioning, rate limiting, request-size limits, and the shared error-response shape — are documented in [docs/API.md](docs/API.md). For CLI/UI run workflows and artefact reproduction, see [docs/Workflows-and-Troubleshooting.md](docs/Workflows-and-Troubleshooting.md); for configuration and scenario field constraints, see [docs/CONFIG-SCHEMA.md](docs/CONFIG-SCHEMA.md); for simulation-run troubleshooting, see [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md). See [ADR-0020](docs/decisions/0020-url-based-api-versioning.md) for the versioning strategy.

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
java -jar target/lift-simulator-0.57.17.jar
```

The `frontend` profile installs Node.js 20.19.0 (for Vite 7 compatibility), runs `npm ci`, builds the Vite bundle, and packages it under `BOOT-INF/classes/static/`. CI uses this profile so downloaded JAR artifacts include the frontend assets. Verify the packaged UI with:
```bash
jar tf target/lift-simulator-0.57.17.jar | grep '^BOOT-INF/classes/static/'
```

## Running the Application

Run the demo simulation:
```bash
mvn exec:java -Dexec.mainClass="com.liftsimulator.Main"
```

Or run the built JAR. The demo selects a controller strategy via command-line arguments:
```bash
java -cp target/lift-simulator-0.57.17.jar com.liftsimulator.Main --help
java -cp target/lift-simulator-0.57.17.jar com.liftsimulator.Main --strategy=directional-scan
```
`--strategy` accepts `nearest-request` (default) or `directional-scan`. The demo runs a pre-configured scenario and prints the simulation state at each tick.


Run scripted scenarios with the scenario runner — either the bundled demo or a custom file:
```bash
mvn exec:java -Dexec.mainClass="com.liftsimulator.scenario.ScenarioRunnerMain"
java -cp target/lift-simulator-0.57.17.jar com.liftsimulator.scenario.ScenarioRunnerMain path/to/scenario.scenario
```

Scenario files are plain text with metadata and tick-based event lines (parsing enforces limits of 1,000,000 ticks and 10,000 events per file); the controller strategy and idle-parking mode are taken from the scenario file. For the scenario file format and metadata keys, see the [CLI run workflows guide](docs/Workflows-and-Troubleshooting.md#cli-usage-unchanged). For simulation engine internals, controller strategy behaviour, out-of-service handling, request modelling, and the lift state machine, see [docs/DEVELOPER-GUIDE.md](docs/DEVELOPER-GUIDE.md).

## Testing

Run the full verification suite (tests and JaCoCo coverage gate) or a faster test-only cycle:
```bash
mvn verify   # tests + JaCoCo coverage gate
mvn test     # tests only
```

> **No local PostgreSQL needed.** Integration tests provision a throwaway PostgreSQL container via
> [Testcontainers](https://java.testcontainers.org/) and run the real Flyway migrations against it; the only
> requirement is a running **Docker** daemon. (CI uses its own PostgreSQL service container.)

The suite spans backend unit tests, Spring-context integration tests for the REST APIs and repositories, deterministic controller scenario tests, and Playwright browser E2E tests under `frontend/e2e` (see [ADR-0014](docs/decisions/0014-playwright-e2e-testing.md)). `mvn verify` enforces a minimum **50% line coverage baseline** through JaCoCo for production business logic (report at `target/site/jacoco/index.html`); CI runs the same phase so pull requests fail if the scoped baseline drops. For the full setup — test database, categories, environment variables, and IDE integration — see [docs/TESTING-SETUP.md](docs/TESTING-SETUP.md) and [docs/TESTING-ARCHITECTURE-GUIDE.md](docs/TESTING-ARCHITECTURE-GUIDE.md). For local frontend E2E runs and the Playwright configuration, see [frontend/README.md](frontend/README.md#testing).

## Quality Checks

```bash
mvn spotbugs:check          # static analysis
mvn verify                  # tests and JaCoCo coverage gate
```

Static analysis is enforced with SpotBugs; the previous no-op Checkstyle gate was removed because it did not enforce repository style rules. SpotBugs suppressions are limited to Spring-managed dependency injection in service constructors.

Dependabot is configured in `.github/dependabot.yml` to check Maven dependencies, frontend npm dependencies, and GitHub Actions weekly. CodeQL static security analysis runs in `.github/workflows/codeql.yml` for the `java-kotlin` and `javascript-typescript` languages, triggered on pull requests to `main` and weekly.

## Configuration

The backend is configured via YAML files under `src/main/resources/`:

- `application.yml` — base defaults (application name `lift-config-service`, server port `8080`, actuator `health`/`info`, root log level `INFO` and `DEBUG` for `com.liftsimulator`, and Logback file base `logs/application`, which produces `logs/application.log` and `logs/application-error.log`)
- `application-dev.yml` — development secrets and database settings (copy from `application-dev.yml.template`)
- `application-local.yml` — optional local-only overrides such as log paths or ports (copy from `application-local.yml.template`)

No profile is active in the checked-in base configuration, so launches must set `SPRING_PROFILES_ACTIVE` explicitly — for example `SPRING_PROFILES_ACTIVE=dev mvn spring-boot:run` for development, `SPRING_PROFILES_ACTIVE=dev,local` to add local overrides (your `application-local.yml` is git-ignored, so `git pull` will not overwrite it), or `SPRING_PROFILES_ACTIVE=prod java -jar target/lift-simulator-0.57.17.jar` for production. This prevents a development profile from masking production configuration mistakes.

OpenAPI/Swagger access is controlled by `security.openapi.public-access` (`SECURITY_OPENAPI_PUBLIC_ACCESS`). The checked-in base configuration and the code fallback both default to `false`, so `/api/v1/api-docs` and `/api/v1/swagger-ui.html` require ADMIN-role authentication unless an environment or profile override explicitly enables public access. The development template keeps `${SECURITY_OPENAPI_PUBLIC_ACCESS:true}` for local convenience; set `SECURITY_OPENAPI_PUBLIC_ACCESS=false` in any shared or production environment.

## Authentication

The backend requires authentication for API access through two mechanisms:

- **Admin APIs** (`/api/v1/**` except `/api/v1/health`) use **HTTP Basic** authentication. Set credentials under `security.admin` in `application-dev.yml`, or via the `ADMIN_USERNAME` / `ADMIN_PASSWORD` environment variables.
- **Runtime and simulation APIs** (`/api/v1/runtime/**`, `/api/v1/simulation-runs/**`) use an **API key** in the `X-API-Key` header. Set `api.auth.key` (or the `API_KEY` environment variable); generate a key with `openssl rand -hex 32`.

The React admin UI sends both schemes from Vite environment variables when present. For local development, create `frontend/.env.local` with `VITE_ADMIN_USERNAME`, `VITE_ADMIN_PASSWORD`, and `VITE_API_KEY` values that match the backend credentials. The backend ignores the unused header for each endpoint, so one Axios client can send both headers on API requests. Because Vite inlines all `VITE_*` values into the browser bundle, these frontend credentials are not a production security boundary; do not ship real admin passwords or API keys in a hosted SPA. Use a backend/session proxy or another server-side auth layer for hosted deployments.

Public endpoints requiring no authentication are `/api/v1/health` and static/frontend routes. Actuator endpoints (`/actuator/health`, `/actuator/info`, and any future `/actuator/**` exposure) require ADMIN-role HTTP Basic authentication and use redacted health details unless authorised. Unauthenticated requests return HTTP 401; authenticated requests lacking permission return HTTP 403.

Admin APIs support role-based access control with two roles: **ADMIN** (read and write) and **VIEWER** (read-only). Write methods (POST, PUT, PATCH, DELETE) require ADMIN; configure multiple users under `security.users` in `application-dev.yml`. Always keep credentials out of version control, prefer environment variables in production, and use HTTPS so HTTP Basic credentials are not exposed.

For runtime API-key setup, RBAC details, and the shared error-response shape, see [docs/API.md](docs/API.md). The security baseline, RBAC, and CORS/CSRF policies are documented in [ADR-0019](docs/decisions/0019-spring-security-baseline.md), [ADR-0021](docs/decisions/0021-role-based-access-control-rbac.md), and [ADR-0022](docs/decisions/0022-explicit-cors-csrf-policy.md).

## Frontend timeout retry behavior

The admin UI keeps the default Axios request timeout at 10 seconds, but automatically retries one timed-out safe read request after a short delay. This helps first-use backend cold starts complete while the existing page or action loading indicator remains visible; a user-facing error is shown only if the retry also fails.

## Rate Limiting and Size Limits

The API is protected by a per-client-IP token-bucket rate limiter (Bucket4j) with separate limits for the admin (100 req/60 s) and runtime/simulation-run (1 000 req/60 s) groups; exceeding a limit returns **HTTP 429** with `Retry-After` and `X-RateLimit-*` headers. Request bodies for `/api/v1/**` and `/actuator/**` are capped at **1 MiB** (**HTTP 413** when exceeded), and simulation artefact log/`results.json` reads are bounded at 1 MiB each. All thresholds are configurable in `application.yml` and overridable per Spring profile. See [docs/API.md](docs/API.md#rate-limiting) for the full configuration reference, response headers, and the `trust-forwarded-for` proxy note.

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

The schema includes `lift_system` and `lift_system_version` (versioned JSONB configurations), `scenario` (reusable test scenarios), and `simulation_run`, which tracks run status (CREATED, RUNNING, SUCCEEDED, FAILED, CANCELLED) with optimistic locking and referential integrity to lift systems and versions. See [ADR-0007](docs/decisions/0007-postgresql-flyway-integration.md) and [ADR-0008](docs/decisions/0008-jpa-entities-and-jsonb-mapping.md) for the persistence design.

For JPA entities, repositories, and verification-runner usage, see [docs/DEVELOPER-GUIDE.md#jpa-entities-and-repositories](docs/DEVELOPER-GUIDE.md#jpa-entities-and-repositories). For connection, permission, and migration errors, see [docs/TROUBLESHOOTING.md#database-troubleshooting](docs/TROUBLESHOOTING.md#database-troubleshooting). For backup and restore procedures, see [docs/DATABASE-BACKUP.md](docs/DATABASE-BACKUP.md).

## Logging

The backend uses Logback (configured in `src/main/resources/logback-spring.xml`) with console and file output persisted to `logs/`: `logs/application.log` (all levels, 30-day retention) and `logs/application-error.log` (ERROR only, 90-day retention), both rotating at 10 MB or daily. Levels are profile-specific (DEBUG and verbose SQL for `dev`, INFO for `prod`). Log files are git-ignored; the directory is kept with `.gitkeep`. Override log locations via `application-local.yml` (run with `SPRING_PROFILES_ACTIVE=dev,local`) or the `LOGGING_FILE_PATH` environment variable. For finding and tailing simulation-run logs, see [docs/Workflows-and-Troubleshooting.md#where-to-find-logs](docs/Workflows-and-Troubleshooting.md#where-to-find-logs).

## Development Setup

This project ships an `.editorconfig` for consistent formatting across editors and IDEs (IntelliJ IDEA has built-in support; VS Code, Eclipse, and Vim/Neovim use plugins). It enforces UTF-8 encoding, LF line endings, 4-space indentation for Java and XML, trailing-whitespace removal, and final-newline insertion.

**Backend dependency baseline** — versions are supplied by the Spring Boot 4.1.0 parent's dependency management unless marked explicit:

| Component | Version |
|-----------|---------|
| Spring Boot | 4.1.0 |
| Spring Framework | 7.0 |
| Spring Security | 7.1 |
| Hibernate ORM | 7.4 |
| Jackson | 3.1 (`tools.jackson` packages) |
| Flyway | 12.4 (with PostgreSQL module) |
| JUnit | 6.0 |
| Testcontainers | 2.0 |
| Springdoc OpenAPI | 3.0.3 (explicit) |
| PostgreSQL JDBC driver | 42.7.11 (explicit) |

The backend test suite uses the modular MVC and Data JPA test starters for controller and repository slice tests. Test sources use Jackson 3 `tools.jackson` core/databind APIs and Spring-configured mappers for JSON contract assertions.

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
│   ├── runtime/                           # Lightweight runtime configuration API
│   │   ├── controller/                    # Runtime REST controllers
│   │   ├── service/                       # Runtime configuration services
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

## Acknowledgements
