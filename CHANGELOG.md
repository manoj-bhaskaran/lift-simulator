# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.46.0] - 2026-01-31

### Added
- **Spring Security Baseline**: Introduced authentication for all admin and runtime APIs
  - **HTTP Basic Authentication**: Admin APIs (`/api/**` except `/api/health`) require HTTP Basic authentication with environment-configured username and password
  - **API Key Authentication**: Runtime APIs (`/api/runtime/**`) require API key authentication via `X-API-Key` header for machine-to-machine communication
  - **Role-Based Access Control**: Admin users have `ADMIN` role; runtime API clients have `RUNTIME` role
  - **Consistent Error Responses**: Unauthenticated requests return HTTP 401 with standard JSON error payload (`status`, `message`, `timestamp`)
  - **Stateless Sessions**: No session cookies; each request must include credentials
  - **CSRF Disabled**: Appropriate for stateless REST APIs
- **Security Configuration**: Added `SecurityConfig` class with three security filter chains:
  - Runtime API filter chain (Order 1) for API key authentication
  - Admin API filter chain (Order 2) for HTTP Basic authentication
  - Public filter chain (Order 3) for static assets and SPA routes
- **API Key Filter**: Implemented `ApiKeyAuthenticationFilter` for validating `X-API-Key` header on runtime endpoints
- **Custom Authentication Entry Point**: `CustomAuthenticationEntryPoint` returns JSON error responses matching application's standard error format
- **Security Properties**: Added configurable security settings in `application.properties`:
  - `security.admin.username` - Admin username (default: `admin`, override: `ADMIN_USERNAME` env var)
  - `security.admin.password` - Admin password (required, override: `ADMIN_PASSWORD` env var)
  - `security.api-key` - Runtime API key (required, override: `API_KEY` env var)
- **Authentication Tests**: Added comprehensive test suite for security configuration:
  - Health endpoint accessibility without authentication
  - Admin API authentication requirements (valid/invalid credentials)
  - Runtime API authentication requirements (valid/invalid API keys)
  - Actuator endpoint accessibility
  - Error response format verification
- **Documentation**: Added Authentication section in README with:
  - Configuration instructions for admin credentials and API keys
  - Usage examples with curl
  - Environment variable alternatives
  - Security best practices

### Changed
- **Breaking: All admin APIs now require authentication**: Requests to `/api/**` endpoints (except `/api/health`) must include HTTP Basic credentials
- **Breaking: All runtime APIs now require API key**: Requests to `/api/runtime/**` endpoints must include `X-API-Key` header
- Updated all controller integration tests to use HTTP Basic authentication
- Updated `GlobalExceptionHandlerValidationTest` to work with Spring Security

### Security
- All administrative API endpoints are now protected with HTTP Basic authentication
- Runtime configuration endpoints are protected with API key authentication
- Public endpoints (health, actuator, static assets) remain accessible without authentication

## [0.45.0] - 2026-02-01

### Added
- **Scenario Builder UI**: Complete UI for creating and managing passenger flow scenarios
  - **Scenarios List Page**: Browse, search, and manage all saved scenarios
  - **Scenario Form**: Create and edit scenarios with intuitive form-based or JSON editor modes
  - **Template-Based Quick Start**: Pre-configured templates (Morning Rush, Evening Rush, Inter-Floor Traffic)
  - **Passenger Flow Builder**: Visual component for building passenger flows with drag-and-drop reordering
  - **Server-Side Validation**: Real-time validation with detailed error and warning feedback
  - **Advanced JSON Mode**: Toggle between form mode and direct JSON editing
  - **Random Seed Support**: Optional seed field for reproducible simulations
  - Added navigation link in main menu for easy access
  - Full CRUD operations (create, read, update, delete) with confirmation modals
- **Simulator Run UI**: End-to-end UI flow for executing simulation runs
  - Added Simulator landing page for lift system + published version selection before run setup
  - Run setup supports optional seed entry for reproducibility
  - Polling-based run status with elapsed time, progress, and terminal state handling
  - Results rendering with KPI cards, per-lift and per-floor tables, artefact downloads, and CLI reproduction guidance
  - **Run Simulator button**: Added discoverable "Run Simulator" button next to each published version that launches the run workflow with preselected system and version
- **Simulation Runs History**: Persistent access to past simulation run results
  - **Runs List Page**: View history of all simulation runs with status, system, version, and scenario information
  - **Filtering**: Filter runs by lift system and status (SUCCEEDED, FAILED, RUNNING, CREATED, CANCELLED)
  - **Run Detail Page**: View full results, KPIs, per-lift and per-floor metrics, and downloadable artefacts for any past run
  - **Navigation Integration**: Added "Runs" link in main navigation and "View All Runs" button in Simulator
  - **Backend API**: New `GET /api/simulation-runs` endpoint with optional `systemId` and `status` query parameters
  - Results persist after navigation away from the simulator, addressing UAT feedback
- **Backend Scenario Enhancements**:
  - Added `name` field to Scenario entity for better identification
  - Implemented `GET /api/scenarios` endpoint to list all scenarios
  - Implemented `DELETE /api/scenarios/{id}` endpoint to delete scenarios
  - Updated `ScenarioRequest` and `ScenarioResponse` DTOs to include name field
  - Database migration (V4) to add name column to scenario table
- **Scenario management API**:
  -  Validation endpoints for UI-driven passenger-flow scenarios
  -  JSON schema validation and storage support.
- **Simulation Run APIs**: Comprehensive API endpoints for simulation execution and monitoring
  - **POST /api/simulation-runs**: Create and start simulation runs with liftSystemId, versionId, optional scenarioId and seed
  - **GET /api/simulation-runs/{id}**: Retrieve run status, timestamps (created/started/ended), progress (currentTick/totalTicks), and error messages
  - **POST /api/simulation-runs/{id}/cancel**: Cancel in-progress simulation runs and transition to CANCELLED
  - **GET /api/simulation-runs/{id}/results**: Access structured results JSON (200 for SUCCEEDED, 409 for RUNNING, 400 for CREATED/CANCELLED)
  - **GET /api/simulation-runs/{id}/logs?tail=N**: Stream simulation logs with optional tail parameter (default: all lines, max: 10,000)
  - **GET /api/simulation-runs/{id}/artefacts**: List downloadable artefacts with name, path, size, and MIME type
  - Implemented `SimulationRunController` with comprehensive error handling and status-based responses
  - Created `ArtefactService` with path traversal prevention and secure file access controls
  - Enhanced `SimulationRunService` with `createAndStartRun()` method for atomic run creation and execution
  - Added DTOs: `CreateSimulationRunRequest`, `SimulationRunResponse`, `SimulationResultResponse`, `ArtefactInfo`
  - Configurable artefacts storage via `simulation.artefacts.base-path` property (default: ./simulation-runs)
  - Automatic artefact directory creation with structure: `{base-path}/run-{id}/`
  - Added comprehensive integration tests covering all endpoints and edge cases
  - Security: Prevents path traversal attacks in artefact access with normalized path validation
- **Simulation Run Domain Model**: Introduced persistent run lifecycle for simulation execution tracking
  - Created `simulation_scenario` table to store reusable test scenarios with JSON configuration
  - Created `simulation_run` table to track individual simulation executions with lifecycle status
  - Added `RunStatus` enum with states: CREATED, RUNNING, SUCCEEDED, FAILED, CANCELLED
  - Established referential integrity with existing `lift_system` and `lift_system_version` tables
  - Implemented JPA entities: `SimulationScenario` and `SimulationRun` with proper relationships
  - Created repositories: `SimulationScenarioRepository` and `SimulationRunRepository` with custom queries
  - Implemented service layer: `SimulationScenarioService` and `SimulationRunService` with status transition methods
  - Added comprehensive unit and integration tests for all new components
  - Database migration (V3) maintains backward compatibility with existing schema
  - Documented architectural decision in ADR-0016
- **Asynchronous Simulation Runner**:
  - Added backend service and API to launch simulation runs asynchronously using stored configs and scenarios
  - Persisted run artefacts (inputs, logs, results placeholder) under a configurable artefact root
- **Structured simulation results output**:
  - Generate `results.json` with run summary, KPI metrics, per-lift status counts, and per-floor aggregates
  - Results are additive and do not change existing CLI outputs
  - Per-floor lift visit counts reflect floor changes (arrivals)
- **Batch Input Generator**: Backwards-compatible wrapper for scenario-to-CLI conversion
  - Implemented `BatchInputGenerator` service to generate `.scenario` files from stored configurations
  - Converts lift system version configuration and scenario JSON to legacy batch input format
  - Generates `hall_call` events from passenger flows with automatic direction calculation
  - Ensures exact format compliance with `ScenarioParser` for CLI simulator compatibility
  - Stores generated files in run-specific artifact directories under `artefactBasePath`
  - Added `generateBatchInputFile()` method to `SimulationRunService` for programmatic access
  - Validates floor ranges and tick constraints during generation
  - Provides unique passenger aliases (p1, p2, p3...) and proper event ordering
  - Added comprehensive test suite including golden-file tests and format validation
  - Documented in README with usage examples and API reference
- **Testing**: Added run lifecycle integration coverage, golden-file contract checks for batch input generation,
  and a CLI compatibility test for the demo scenario.
- **E2E Testing - Scenario Management**: Added comprehensive Playwright E2E test suite for scenario creation and editing
  - TC_SCENARIO_001: Create scenario using form mode
  - TC_SCENARIO_002: Create scenario using Advanced JSON Mode with validation and save
  - TC_SCENARIO_003: Edit existing scenario using Advanced JSON Mode
  - TC_SCENARIO_004: Validate invalid JSON shows proper error messages

### Fixed
- **Run Simulator Artefacts**: Added a dedicated artefact download endpoint with secure path validation and improved UI error feedback when downloads fail.
- Ensure simulation run progress ticks persist during execution so the UI progress bar and tick counter advance while runs are active.
- **Run Simulator**: Fixed scenario dropdown to only show scenarios belonging to the selected Lift System Version. Previously, all scenarios were shown regardless of version compatibility, leading to backend validation errors when incompatible scenarios were selected. The dropdown now filters scenarios by `liftSystemVersionId` and automatically clears the selection when switching to a version that doesn't have the currently selected scenario. Added helpful messages when no version is selected or no scenarios are available for the selected version.
- **Scenario Builder - Advanced JSON Mode**: Fixed critical bug where updates made in Advanced JSON Mode were not persisted after clicking "Validate" then "Update Scenario". The form state was not synchronized with the JSON text editor, causing old values to be sent to the backend. Now `buildScenarioJson()` correctly parses the JSON text when in Advanced JSON Mode, and both validate and save operations sync the parsed JSON back to form state for consistency.
- **Scenario Builder**: Add clear selection styling for quick start templates on the Create Scenario screen.
- **Scenario Builder**: Align the random seed checkbox with its label text on the Create Scenario screen.
- **Scenario Builder**: Prevent blank page regression when navigating to the Create New Scenario screen.
- Return a conflict response with guidance when deleting lift systems that still have scenarios tied to their versions.
- Update the frontend footer to report the current application version (0.45.0) from the admin UI package metadata.
- Avoid SpotBugs EI_EXPOSE_REP2 warnings in admin services by using defensive ObjectMapper copies and lazy execution service injection.
- Handle unexpected IO failures when validating scenario payloads in the admin service.
- Validate scenario floor ranges and start ticks when generating batch scenario content in memory.
- Initialize the H2 test schema for Spring Boot integration tests to prevent application context startup failures.
- Fix H2 JSON compatibility in @SpringBootTest integration tests by implementing custom `JsonStringConverter` AttributeConverter with validation to replace Hibernate 6's `@JdbcTypeCode(SqlTypes.JSON)` annotation, which is only supported for Oracle and PostgreSQL databases. The converter validates JSON format at write time to prevent malformed JSON from being persisted (particularly for SimulationScenario entities where service-layer validation may be bypassed), ensuring errors are caught early rather than failing at simulation execution time.
- **Simulator Run UI**: Only apply preselected system/version from query params once so user selections are not overridden.
- **Simulator Landing**: Ignore stale version fetch responses when switching between systems quickly.
- **Simulator Landing**: Clear version options when loading versions fails to prevent mismatched selections.
- Add database-level ON DELETE CASCADE constraint to SimulationRun foreign keys (lift_system_id and version_id) via Hibernate's @OnDelete annotation. This ensures SimulationRun records are automatically deleted when their parent LiftSystem or LiftSystemVersion is deleted, matching the existing database schema and preventing constraint violations.
- Align the "Use Random Seed (for reproducibility)" checkbox with its label on the Create Scenario screen.
- Align README schema/entity references to the `scenario` table after legacy `simulation_scenario` removal.
- **Run Simulator**: Fixed UI hang when clicking Start Run multiple times before a previous run completes. The Start Run button is now disabled while a run is in progress (RUNNING or CREATED status), preventing duplicate run requests and state conflicts. Button text changes to "Run in Progress" and a hint message guides users to wait for completion or cancel the current run before starting a new one.

## [0.44.0] - 2026-01-20

### Changed
- **Breaking: Unified floor range configuration**: Replaced `floors` with `minFloor`/`maxFloor` across runtime config, validation, API examples, and UI placeholders to support basement levels.
- **Validation updates**: Enforced `maxFloor > minFloor`, ensured `homeFloor` stays within the configured floor range, and adjusted lift count warnings to use the derived floor count.
- **Database migration**: Added a data migration to convert stored JSON configs from `floors` to `minFloor`/`maxFloor`.
- **Test suite updates**: Refreshed backend and Playwright fixtures to align with the new floor range schema, including basement-capable configurations.

## [0.43.0] - 2026-01-19

### Added
- **Playwright UI Automation Framework**: Introduced Playwright as the E2E testing framework for the frontend
  - Installed @playwright/test with TypeScript support
  - Created `playwright.config.ts` with optimal CI/local development settings
  - Configured base URL (http://localhost:3000) and automatic web server startup
  - Set up reasonable timeouts (10s actions, 30s navigation) and retries (2 on CI)
  - Enabled test artifacts: screenshots on failure, videos on failure, traces on retry
  - Created `e2e/` directory for test organization
  - Added comprehensive smoke test suite (`e2e/smoke.spec.ts`) covering:
    - Application loads and displays dashboard
    - Navigation to Lift Systems page
    - Health check page accessibility
    - Configuration validator page accessibility
    - Footer version information display
  - Added Playwright scripts to package.json:
    - `npm test` - Run tests in headless mode
    - `npm run test:headed` - Run tests with browser UI visible
    - `npm run test:ui` - Interactive UI mode for debugging
    - `npm run test:debug` - Debug mode with step-by-step execution
    - `npm run test:report` - View HTML test report
  - Updated `.gitignore` to exclude Playwright artifacts (test-results/, playwright-report/, playwright/.cache/)
  - Documented setup and usage in frontend/README.md with complete instructions
  - Created ADR-0014 documenting the decision to adopt Playwright
  - Integrated Playwright tests into CI pipeline:
    - Automatic Chromium browser installation with dependencies
    - Playwright webServer auto-starts frontend on port 3000 during test execution
    - HTML test report uploaded as GitHub Actions artifact (available for 30 days)
    - Test artifacts (screenshots, videos, traces) uploaded on test failures
    - Test failures properly fail the CI workflow to block PR merges
    - Test reports accessible via GitHub Actions "Artifacts" section in workflow runs
- **Automated UI Test Suite**: Converted 10 manual test cases to automated Playwright tests
  - **Test Helpers and Fixtures** (`e2e/helpers/test-helpers.ts`):
    - Comprehensive test data generators and helper functions
    - Configuration fixtures for valid and invalid scenarios
    - Reusable functions for common operations (create system, create version, publish version)
    - Backend availability checks and cleanup utilities
    - Dashboard metrics aggregation helpers
  - **Lift Systems CRUD Tests** (`e2e/lift-systems.spec.ts`) - 5 automated tests:
    - TC_0003: Create New Lift System with validation
    - TC_0004: View Lift System Details with metadata verification
    - TC_0013: Delete Lift System and cascade delete versions
    - Create system with invalid key validation
    - Create system with duplicate key error handling
  - **Configuration Version Lifecycle Tests** (`e2e/configuration-versions.spec.ts`) - 6 automated tests:
    - TC_0005: Create Valid Configuration Version (Draft)
    - TC_0006: Reject Invalid Configuration Version
    - TC_0007: Create Valid Configuration After Fix
    - TC_0008: Edit Draft Version and Save
    - TC_0009: Publish Version and Auto-Archive Previous
    - Cannot edit published version (read-only mode)
  - **Configuration Validator Tests** (`e2e/config-validator.spec.ts`) - 6 automated tests:
    - TC_0011: Validate Configuration Using Validator Tool
    - Validator shows warnings distinctly from errors
    - Validator handles malformed JSON gracefully
    - Validator handles boundary values correctly
    - Validator accepts different controller strategies
  - **Health Check Tests** (`e2e/health-check.spec.ts`) - 4 automated tests:
    - TC_0012: Health Check UI and API
    - Health check page shows service information
    - Health check handles backend failure gracefully
    - Health check API returns structured JSON
  - **Dashboard Tests** (`e2e/dashboard.spec.ts`) - 4 automated tests:
    - TC_0017: Dashboard Aggregate Counts Validation
    - Dashboard displays quick actions
    - Dashboard metrics update after system deletion
    - Dashboard is accessible from navigation
  - **Total**: 25 automated test cases covering critical user flows
  - **Test Coverage**: All tests follow best practices with:
    - Clear test descriptions mapping to manual test case IDs
    - Stable selectors using class names, IDs, and text content
    - Proper assertions with meaningful error messages
    - Backend availability checks with automatic test skipping
    - Cleanup logic to prevent test data pollution
    - Resilience to backend unavailability (smoke tests)
  - **Test Organization**:
    - Tests grouped by feature area (Lift Systems, Versions, Validator, Health, Dashboard)
    - Reusable helpers reduce code duplication
    - Configuration fixtures loaded from backend scenario files
    - Clear mapping between manual test cases and automated tests

### Fixed
- **Lift Systems Navigation Test**: Corrected smoke test to use `/systems` route instead of `/lift-systems` to match actual routing in App.jsx

## [0.42.0] - 2026-01-19

### Added
- **Version Display in Footer**: Current version number is now displayed in the footer on all screens
  - Version number pulled from package.json and displayed as "Version X.Y.Z"
  - Visible on all application screens through the shared Layout component
  - Provides users with quick reference to the current application version
- **Lift System Edit Functionality**: Added ability to edit lift system name and description after creation
  - New "Edit System" button in the Lift System Detail page header
  - EditSystemModal component for editing system metadata
  - System Key field is displayed as read-only (cannot be changed after creation)
  - Edit modal pre-populates with current system values
  - Form validation matching backend constraints (displayName 1-200 chars, description max 5000 chars)
  - Automatic refresh of system details after successful update
  - Responsive design with mobile support

### Changed
- **Create New Version UI Standardization**: Standardized action buttons between Edit Config and Create New Version for consistency
  - Added "Validate" button to Create New Version form to match Edit Config workflow
  - "Create Version" button now disabled after any configuration change until validation is completed
  - Split-pane layout with configuration editor on left and validation results panel on right
  - Real-time validation feedback with detailed error and warning messages
  - Validation state tracking (validating, validationResult, hasConfigChanges)
  - Configuration changes clear previous validation results
  - Users must explicitly validate before creating a version, preventing invalid configurations
  - Consistent user experience across all configuration editing workflows
  - Improved error prevention by requiring validation before version creation
- **Footer Copyright Notice**: Updated the footer copyright to reference Manoj Bhaskaran.

### Fixed
- **Lift System Service Tests**: Mocked version-count repository dependencies to prevent null pointer failures in LiftSystemService unit tests.
- **Lift System Service Tests**: Corrected mocked version-count return typing to fix test compilation.
- **Run Simulator UI Feedback**: Clicking Run Simulator now shows a graceful message noting the feature is unavailable until a future release.
- **Version Search Matching**: Searching by version number now returns only exact version matches instead of versions that merely contain the digits.
- **Create Version Validation Workflow**: Added a Validate button to the Create New Version form and require a successful validation before enabling version creation.
- **Create Version Button Action**: Wired the Create Version button to trigger version creation even when the wrapper is a non-form container.
- **Lift System Detail Lint Errors**: Removed unused handlers and wired the create-version form submit to fix ESLint no-unused-vars/no-undef violations.
- **Dashboard Versions Metric**: Lift system responses now include `versionCount`, allowing the dashboard and lift system list to accurately total configuration versions.
- **Config Validation Compilation**: Corrected the Jackson exception reference type to restore compilation in ConfigValidationService.
- **Configuration Validation Error Messages**: Non-numeric values in configuration fields now display clear, user-friendly error messages
  - When entering non-numeric values (e.g., "A", "abc", true) for numeric fields, the system now shows: "Field 'fieldName' must be a numeric value, got 'value'"
  - Previously displayed generic JSON parsing errors: "Invalid JSON format: Unrecognized token..."
  - Added specific handling for InvalidFormatException and MismatchedInputException in ConfigValidationService
  - Error messages now include the field name and the actual invalid value provided
  - Improves user experience when creating or editing lift system version configurations
- **Dashboard Label Consistency**: Updated label from "Total Versions" to "Configuration Versions" in the Overview section for consistency with terminology used throughout the application
- **Dashboard Versions Metric**: Lift system responses now include `versionCount`, allowing the dashboard and lift system list to accurately total configuration versions.
- **Configuration Validator**: Fixed validation failure on valid configurations in standalone Configuration Validator tool
  - ConfigValidator page now correctly sends validation requests as `{ config: "..." }` instead of parsed JSON object
  - Resolves "Unknown property 'floors' is not allowed" error when validating configurations
  - Improved validation result display to show errors and warnings distinctly
  - Validation errors now display field name and message in a clear, structured format
  - Warnings are shown separately from errors when configuration is valid but has potential issues
- **Lift Systems Version Counts**: Lift system list responses now compute version totals per system to prevent zeroed counts on the Lift Systems page.
- **Versions Status Sort Order**: Fixed incorrect sort order when sorting versions by status in Manage Versions screen
  - When selecting "Published" order (desc), versions now correctly display: Published, Draft, Archived
  - When selecting "Archived" order (asc), versions now correctly display: Archived, Draft, Published
  - Previously the sort order was reversed, showing Archived first when "Published" was selected and vice versa
- **Create New Version Form HTML Tag Mismatch**: Fixed mismatched HTML tags in LiftSystemDetail component
  - Corrected closing `</form>` tag to `</div>` tag on line 543 to match opening `<div className="create-version-form">` tag
  - Resolved build error: "Unexpected closing 'form' tag does not match opening 'div' tag"
  - Build now completes successfully without transformation errors

## [0.41.4] - 2026-01-18

### Fixed
- **Draft Version Validation**: Fixed validation failure when editing DRAFT versions
  - ConfigEditor now correctly sends validation requests as `{ config: "..." }` instead of parsed JSON object
  - Resolves "Unknown property 'floors' is not allowed" error when validating DRAFT configurations
  - Both handleValidate and handleSaveDraft validation calls updated to use correct request format
  - Validation now works consistently with create and update operations
- **Create Version Modal**: Version number is now displayed prominently at the top of the Create Version form
  - Shows next version number (calculated as max existing version + 1, or 1 for first version)
  - Added styled version number display with blue accent border
  - Version number automatically updates based on existing versions
- Show descriptive validation feedback in the Create Version modal when configuration JSON fails backend validation.
- **React Hooks and Linting**: Fixed React hooks exhaustive-deps warnings and ESLint errors
  - Fixed missing dependency warnings in `useEffect` hooks in ConfigEditor.jsx and LiftSystemDetail.jsx
  - Wrapped `loadData` and `loadSystemData` functions in `useCallback` to properly memoize them
  - Added `useCallback` to dependency arrays to satisfy exhaustive-deps rules
  - Removed unused `configObject` variables in ConfigEditor.jsx that violated no-unused-vars rule
  - Changed to direct `JSON.parse()` calls where parsed object wasn't needed
  - All ESLint warnings and errors now resolved

## [0.41.3] - 2026-01-17

### Fixed
- Return a helpful 404 response when the SPA index.html asset is missing, avoiding noisy stack traces when the frontend is not built.
- Ensure SPA forwarding uses explicit return types so Spring MVC applies the correct view/response handling.

## [0.41.2] - 2026-01-17

### Fixed
- Return a helpful 404 response when the SPA index.html asset is missing, avoiding noisy stack traces when the frontend is not built.

## [0.41.1] - 2026-01-17

### Fixed
- **SPA Forwarding Recursion**: Prevented recursive forwarding when requesting `/index.html`, avoiding StackOverflowError logs during HTML route handling

## [0.41.0] - 2026-01-17

### Added
- **Persistent File-Based Logging**: Backend logs are now persisted to files in the `logs/` directory
  - **Logback Configuration**: Created comprehensive `logback-spring.xml` for centralized logging management
  - **Console and File Output**: Logs are written to both console (for development) and files (for debugging)
  - **Rolling File Appenders**: Automatic log rotation to prevent unbounded disk usage
    - Main application log: `logs/application.log` (rotated daily and at 10MB, max 30 days, 1GB total)
    - Error-only log: `logs/application-error.log` (rotated daily and at 10MB, max 90 days, 500MB total)
    - Archive format: `logs/application-YYYY-MM-DD.N.log`
  - **Full Stack Traces**: All exceptions logged with complete stack traces using `%ex{full}` pattern
  - **Profile-Specific Configuration**: Different logging levels for dev and prod profiles
    - Dev profile: DEBUG level for application code, verbose SQL logging
    - Prod profile: INFO level for application code, reduced database logging noise
  - **Structured Log Patterns**: Timestamp, thread, level, logger, message, and full exception details
  - **Logs Directory**: Created `logs/` directory with `.gitkeep` to ensure it exists in version control
    - Log files are gitignored (already in .gitignore as `*.log`)
- **Improved Debugging Capability**: Stack traces from API failures (e.g., `/api/health`) can now be retrieved from log files
- **Audit Trail**: All backend runtime errors are now persistently recorded for post-mortem analysis
- **Local Configuration Overrides**: Implemented `application-local.properties` pattern for git-conflict-free customization
  - **Created** `application-local.properties.template` with examples for common overrides
  - **Added** `application-local.properties` to `.gitignore` (local-only, not tracked)
  - **Use Cases**:
    - Custom log file paths (avoid git pull conflicts with `application.properties`)
    - Different server ports to avoid local conflicts
    - Alternative database connection settings
    - Any local-only configuration overrides
  - **Activation**: `SPRING_PROFILES_ACTIVE=dev,local mvn spring-boot:run`
  - **Spring Boot Support**: Leverages built-in profile-specific property files
  - **Documentation**: Added comprehensive README sections on local overrides

### Security
- **Configuration Template Pattern**: Implemented template-based configuration for database credentials
  - **Created** `application-dev.yml.template` with placeholders for sensitive values
  - **Removed** `application-dev.yml` from version control (added to `.gitignore`)
  - **Environment Variable Support**: Database credentials can be overridden via environment variables:
    - `DB_URL`: Database connection URL
    - `DB_USERNAME`: Database username
    - `DB_PASSWORD`: Database password
  - **Developer Workflow**: Developers copy template to `application-dev.yml` and customize locally
  - **Security Best Practice**: Credentials never committed to version control
  - **Documentation**: Updated README with detailed setup instructions for configuration file

### Changed
- Version bumped from 0.40.0 to 0.41.0
- Frontend package version updated to 0.41.0
- **Logging Configuration**: Updated `application.properties` to specify log file location
  - Removed pattern definitions (now managed by logback-spring.xml)
  - Added `logging.file.name=logs/application.log`
  - Added `logging.file.path=logs`
  - Preserved log level settings (INFO for root, DEBUG for com.liftsimulator)
- **Logback Configuration**: Application now uses Spring Boot's Logback integration via logback-spring.xml
  - Replaces pattern-only configuration in application.properties
  - Provides better control over appenders, rotation, and formatting
  - Supports profile-specific logging behavior
- **Database Configuration**: `application-dev.yml` is now a local-only file (not tracked in git)
  - Template file `application-dev.yml.template` provides reference configuration
  - Follows Spring Boot best practices for credential management
  - Enables different credentials per developer without git conflicts
- **Configuration Override Strategy**: Introduced multiple layers for configuration customization
  - **Defaults**: `application.properties` (version controlled, safe defaults)
  - **Profile-specific**: `application-dev.yml` (local-only, for database credentials)
  - **Local overrides**: `application-local.properties` (local-only, for paths/ports/etc.)
  - **Environment variables**: Highest priority, for CI/CD and Docker deployments
  - **Priority order**: Environment vars > local profile > dev profile > application.properties

### Fixed
- **Console Buffer Overflow**: Backend logs no longer overflow console buffer during `mvn spring-boot:run`
- **Lost Stack Traces**: Exception stack traces are now fully preserved in log files
- **Debugging Blocker**: Root cause analysis is now possible for backend failures via persistent logs

### Technical Details
- **Logback Components**:
  - CONSOLE appender: Writes to stdout with thread and logger information
  - FILE appender: Writes all logs to rotating files with full exception details
  - ERROR_FILE appender: Dedicated error log for quick issue identification
- **Rotation Strategy**:
  - Size-based: Files rotate when reaching 10MB
  - Time-based: Daily rollover at midnight
  - Retention: 30 days for main log, 90 days for error log
  - Total cap: 1GB for main log, 500MB for error log
- **Log Patterns**:
  - Console: `%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n`
  - File: `%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n%ex{full}`
- **Logger Levels** (inherited from existing configuration):
  - Root: INFO
  - com.liftsimulator: DEBUG
  - org.hibernate.SQL: DEBUG (dev), WARN (prod)
  - org.hibernate.type.descriptor.sql.BasicBinder: TRACE (dev), WARN (prod)
  - org.flywaydb: INFO
  - com.zaxxer.hikari: INFO
  - Spring Framework: INFO

### Benefits
- **Effective Debugging**: Developers can retrieve stack traces after console overflow
- **UAT Unblocked**: Active defects can now be diagnosed using persistent logs
- **Production Readiness**: Audit trail of runtime errors exists for compliance and debugging
- **Developer Productivity**: No longer blocked by lost console output
- **Disk Space Management**: Automatic rotation prevents disk exhaustion
- **Quick Error Access**: Separate error log allows rapid identification of failures

## [0.40.0] - 2026-01-17

### Added
- **Comprehensive Logging to GlobalExceptionHandler**: Added SLF4J-based logging for all exception handlers
  - **SLF4J Logger Integration**: Added `org.slf4j.Logger` and `org.slf4j.LoggerFactory` imports
  - **ERROR-level Logging**: Generic exception handler (`handleGenericException`) logs unexpected errors with full stack traces
    - Logs exception message and complete stack trace for debugging production issues
    - Critical for troubleshooting unexpected 500 errors
  - **WARN-level Logging**: Malformed JSON request handler (`handleHttpMessageNotReadable`)
    - Logs malformed JSON requests with specific details for unknown properties
    - Distinguishes between unknown property errors and other malformation issues
  - **INFO-level Logging**: Business exception handlers log expected operational errors
    - `handleResourceNotFound`: Logs 404 resource not found errors
    - `handleIllegalArgument`: Logs 400 bad request errors
    - `handleIllegalState`: Logs 409 conflict errors
    - `handleConfigValidationError`: Logs configuration validation failures
  - **DEBUG-level Logging**: Validation error handler (`handleValidationErrors`)
    - Logs validation failures with field count and field names
    - Suitable for verbose validation debugging without overwhelming logs
  - **Benefits**:
    - Enables debugging of production issues with full stack traces
    - Provides audit trail of all exceptions
    - Supports monitoring and alerting based on log levels
    - Maintains security best practice (logs internally, hides from clients)
    - Improves system observability for operations teams

### Changed
- Version bumped from 0.39.1 to 0.40.0
- Frontend package version updated to 0.40.0
- Enhanced JavaDoc for `GlobalExceptionHandler` class to document logging capabilities

### Technical Details
- Uses SLF4J API with Logback implementation (included in `spring-boot-starter-web`)
- No additional dependencies required
- Logging levels follow industry best practices:
  - ERROR: Unexpected errors requiring immediate investigation
  - WARN: Malformed requests that may indicate client issues
  - INFO: Expected business errors and operational events
  - DEBUG: Verbose validation details for development/debugging
- Stack traces included only for ERROR-level logs to balance detail with log volume
- All log messages use parameterized logging (SLF4J `{}` placeholders) for performance

## [0.39.1] - 2026-01-19

### Added
- Frontend testing strategy documentation, including tooling recommendations, conventions, and example scenarios
- CI/CD documentation for frontend and backend workflows, plus local check commands
- Deployment guidance for Vercel/Netlify, AWS S3 + CloudFront, Docker, and Spring Boot integration
- CI status badge in the frontend README

### Changed
- GitHub Actions workflow now runs frontend install, lint, build, and test steps
- Frontend npm scripts now include `lint:fix` and `test` placeholders
- Version bumped from 0.39.0 to 0.39.1
- Frontend package version updated to 0.39.1

## [0.39.0] - 2026-01-18

### Added
- **Comprehensive JSDoc Documentation**: Added JSDoc comments to all React components and utility functions
  - **Component Documentation**: All components now have detailed JSDoc with:
    - Component descriptions and feature lists
    - Complete props documentation with types and descriptions
    - Parameter and return type annotations
    - Usage examples where appropriate
  - **Documented Components**:
    - Modal components: AlertModal, ConfirmModal, CreateSystemModal, Modal
    - Layout components: Layout, VersionActions
    - Page components: ConfigEditor, LiftSystemDetail, LiftSystems
  - **Utility Functions**: errorHandlers.js and statusUtils.js now have full JSDoc
  - **Benefits**:
    - Better IDE autocomplete and type hints
    - Improved developer onboarding experience
    - Inline documentation for component APIs
    - Enhanced code maintainability
- **Professional README Badges**: Added visual badges to frontend README
  - Node.js version requirement (>=18.0.0)
  - MIT License badge
  - React version (19.x) with logo
  - Vite version (7.x) with logo
- **Maintenance Documentation**: Added comprehensive dependency management section
  - Interactive dependency update workflow using npm-check-updates
  - Best practices for reviewing and applying updates
  - Version synchronization guidelines
  - Security audit recommendations

### Changed
- **Frontend README Improvements**: Significantly improved documentation accuracy and organization
  - **Version Documentation Fix**: Tech stack versions now show caret ranges (e.g., ^19.2.0) matching package.json
    - Added explanatory note about semantic versioning ranges
    - Prevents confusion about exact vs. compatible versions
    - Documents how to check installed versions with `npm list`
  - **Deployment Reorganization**: Restructured deployment documentation with clear hierarchy
    - **Primary Deployment**: Spring Boot integration (recommended) with benefits listed
    - **Alternative Deployment**: Standalone static hosting with use cases
    - Moved deployment section earlier in README for better discoverability
    - Added clear benefits and trade-offs for each approach
    - Included CORS configuration notes for standalone deployment
  - **Improved Structure**: Better information architecture throughout
    - Added deployment decision guidance
    - Enhanced troubleshooting section placement
    - Consolidated production build documentation
- Version bumped from 0.38.1 to 0.39.0
- Frontend package version updated to 0.39.0

### Fixed
- **Version Documentation Mismatch**: Resolved inconsistency between README (exact versions) and package.json (caret ranges)
- **Deployment Model Ambiguity**: Clarified which deployment approach is recommended for which use case
- **Missing Maintenance Guidance**: Addressed gap in dependency update process documentation

## [0.38.1] - 2026-01-18

### Added
- Shared frontend utilities for status badge styling and API error handling
- Reusable VersionActions component for version action buttons

### Changed
- Refactored ConfigEditor, LiftSystemDetail, and LiftSystems to use shared status/error utilities
- Version bumped from 0.38.0 to 0.38.1
- Frontend package version updated to 0.38.1

## [0.38.0] - 2026-01-17

### Added
- **Version List Pagination and Sorting**: Enhanced version list management with comprehensive filtering, sorting, and pagination
  - **Pagination Controls**:
    - Configurable items per page (10/20/50/100 selectable via dropdown)
    - Page navigation buttons (first, previous, next, last)
    - Page number buttons with active page highlighting
    - Pagination info showing current range and total count (e.g., "Showing 1-10 of 25 versions")
    - Smart page number display (shows up to 5 page numbers centered around current page)
    - Pagination controls only displayed when needed (more than one page)
  - **Sorting Options**:
    - Sort by version number (ascending/descending)
    - Sort by creation date (newest first/oldest first)
    - Sort by status (Published → Draft → Archived, or reverse)
    - Context-aware sort order labels (e.g., "Newest First" vs "Oldest First" for date sorting)
  - **Filtering and Search**:
    - Filter versions by status (All/Published/Draft/Archived)
    - Search by version number with real-time filtering
    - Combined filtering (status filter + search work together)
    - Empty state message when no versions match filters
  - **UI Enhancements**:
    - Clean controls panel with organized filter/sort/search inputs
    - Responsive grid layout for controls (stacks on mobile)
    - Focus states and visual feedback on all controls
    - Version count display shows filtered count and total (e.g., "Versions (5 of 25)")
    - Automatic reset to page 1 when filters or sorting changes
  - **Styling**:
    - Professional control panel with light background and subtle borders
    - Consistent input styling with focus indicators
    - Pagination buttons with hover effects and disabled states
    - Active page number highlighted with primary color
    - Responsive design with mobile breakpoints
    - Accessible form labels and ARIA-friendly markup

### Changed
- Version bumped from 0.37.0 to 0.38.0
- Frontend package version updated to 0.38.0
- LiftSystemDetail page now displays paginated version lists instead of showing all versions
- Version list header now shows filtered count vs total count

### Technical Details
- **State Management**: React hooks manage pagination, sorting, and filtering state
  - `currentPage`, `itemsPerPage`, `sortBy`, `sortOrder`, `statusFilter`, `versionSearch`
  - Automatic page reset when filters change via `useEffect` dependency tracking
- **Data Processing Pipeline**:
  1. Filter by status (if not "ALL")
  2. Filter by version number search
  3. Sort by selected field and order
  4. Paginate results based on current page and items per page
- **Pagination Logic**:
  - Total pages calculated from filtered results
  - Start/end indices computed for array slicing
  - Page navigation validates bounds before updating state
  - Smart page number rendering shows context around current page
- **CSS Architecture**:
  - `.versions-controls` - Container for all filter/sort controls
  - `.pagination-controls` - Container for pagination UI
  - `.pagination-buttons` - Flexbox layout for page navigation
  - `.page-number.active` - Active page indicator styling
  - Mobile-responsive with column stacking below 768px
- **Performance**: Client-side filtering/sorting/pagination for optimal UX with typical dataset sizes

### User Experience Improvements
- Better performance when lift systems have many versions
- Easier to find specific versions using search and filters
- More organized display with logical sorting options
- Professional pagination interface matching modern web standards
- Responsive design works seamlessly on mobile devices

## [0.37.0] - 2026-01-16

### Added
- Lift Systems search bar with client-side filtering by display name and system key
- Empty state message for search results with no matches

### Changed
- Lift Systems list now filters results case-insensitively based on the search query
- Version bumped from 0.36.3 to 0.37.0
- Frontend package version updated to 0.37.0

## [0.36.3] - 2026-01-16

### Added
- ConfigEditor now auto-validates configuration on save attempt to prevent saving invalid configurations
- Validation errors are displayed immediately when attempting to save invalid configuration
- Invalid configurations are now blocked from being saved until errors are fixed

### Changed
- ConfigEditor save workflow improved: auto-runs validation before save if not already validated
- LiftSystems error handling improved: removed unnecessary error throw that could cause unexpected behavior
- Version bumped from 0.36.2 to 0.36.3
- Frontend package version updated to 0.36.3

### Fixed
- ConfigEditor no longer allows saving invalid configurations without validation
- LiftSystems error handling no longer throws after displaying error in AlertModal, preventing duplicate error handling

## [0.36.2] - 2026-01-16

### Added
- Shared Modal component to centralize overlay, header, and focus/keyboard handling for modal variants
- Shared Modal CSS for consistent base modal styling across the UI

### Changed
- AlertModal and ConfirmModal now render via the shared Modal component to reduce duplication
- Version bumped from 0.36.1 to 0.36.2
- Frontend package version updated to 0.36.2

## [0.36.1] - 2026-01-16

### Added
- Frontend API client now supports environment-configured base URL and request timeout via Vite env variables

### Changed
- Default Axios request timeout set to 10 seconds to prevent hanging requests
- Version bumped from 0.36.0 to 0.36.1
- Frontend package version updated to 0.36.1

## [0.36.0] - 2026-01-16

### Added
- **In-App UI Feedback Modals**: Replaced blocking browser alert/confirm dialogs with accessible modal components
  - Created `ConfirmModal` component for confirmation dialogs (publish, delete operations)
  - Created `AlertModal` component for error/notification messages
  - Both modals feature comprehensive accessibility:
    - Automatic focus management (auto-focus on primary action button)
    - Keyboard navigation support (ESC to close, Enter for confirm/dismiss, Tab trapping)
    - Proper ARIA attributes (role, aria-modal, aria-labelledby, aria-label)
    - Focus trap in ConfirmModal to keep keyboard navigation within modal
  - Modal styling follows existing design system with consistent colors and spacing
  - Visual feedback with color-coded icons for different alert types (error, warning, success, info)

### Changed
- **ConfigEditor**: Replaced `window.confirm()` with ConfirmModal for publish confirmation
- **LiftSystems**: Replaced `window.alert()` with AlertModal for create system errors
- **LiftSystemDetail**: Replaced all alert/confirm dialogs with modal components
  - Publish version confirmation uses ConfirmModal
  - Delete system confirmation uses ConfirmModal with danger styling
  - All error messages (create version, publish version, delete system) use AlertModal
- Version bumped from 0.35.2 to 0.36.0
- Frontend package version updated to 0.36.0

### Technical Details
- **ConfirmModal**: Customizable title, message, button text, and styles; ESC/Tab/click-outside handling
- **AlertModal**: Type-based styling (error/warning/success/info); ESC/Enter dismissal; auto-focus on OK button
- Both components use React hooks (useEffect, useRef) for lifecycle management
- Accessible role attributes ("dialog" for ConfirmModal, "alertdialog" for AlertModal)
- Non-blocking UI integration with consistent visual design across browsers

## [0.35.2] - 2026-02-12

### Fixed
- Ensure the Versions anchor scroll runs after loading completes, including systems with zero versions
- Version bumped from 0.35.1 to 0.35.2

## [0.35.1] - 2026-02-12

### Changed
- Lift systems list now routes the Manage Versions action to the versions section instead of duplicating the view details navigation
- Admin UI version details page scrolls to the versions section when linked with a `#versions` anchor
- Documentation refreshed for updated version numbers and UI navigation guidance
- Frontend package version aligned with the application release
- Version bumped from 0.35.0 to 0.35.1

## [0.35.0] - 2026-01-16

### Added
- **Strict Configuration Schema Validation**: Enhanced config validation to reject unknown JSON fields
  - Created `JacksonConfiguration` to configure ObjectMapper with `FAIL_ON_UNKNOWN_PROPERTIES`
  - Unknown properties in configuration JSON now trigger validation errors instead of being silently ignored
  - Helps catch typos and unexpected fields in configuration payloads (e.g., "floor" instead of "floors")
  - Enhanced `ConfigValidationService` to provide specific error messages for unknown properties
  - Applied strict validation to both Spring-managed ObjectMapper and CLI LocalSimulationMain
- **Comprehensive Test Coverage**: Added 5 new tests for unknown field rejection scenarios
  - `testValidate_UnknownFieldRejected()` - Validates rejection of unknown fields
  - `testValidate_TypoInFieldNameRejected()` - Catches typos in field names
  - `testValidate_MultipleUnknownFieldsRejected()` - Handles multiple unknown fields
  - `testValidate_UnknownFieldWithValidData()` - Ensures unknown fields cause rejection even with valid data
  - Tests verify clear error messages with field names for debugging

### Changed
- Version bumped from 0.34.2 to 0.35.0
- `ConfigValidationService` now catches `UnrecognizedPropertyException` separately for clearer error messages
- Test suite updated to configure ObjectMapper with `FAIL_ON_UNKNOWN_PROPERTIES` matching production settings
- `LocalSimulationMain` CLI tool now enforces same strict validation as REST API

### Documentation
- Added ADR-0013 documenting decision to reject unknown fields vs. ignore/log alternatives

### Technical Details
- Uses Jackson `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES` for schema enforcement
- ObjectMapper configured via `Jackson2ObjectMapperBuilderCustomizer` (preserves Spring Boot Jackson modules)
- Unknown property errors include field name and clear message: "Unknown property 'fieldName' is not allowed in configuration schema"
- Validation occurs at JSON deserialization time before any business logic validation
- Added `HttpMessageNotReadableException` handler in `GlobalExceptionHandler` for REST request validation
- Consistent validation behavior across all entry points (REST API, validation endpoint, CLI)

### Benefits
- **Prevents Configuration Errors**: Catches typos like "floor" instead of "floors" at validation time
- **API Safety**: Ensures clients only use documented configuration fields
- **Clear Feedback**: Detailed error messages help developers identify and fix issues quickly
- **Security**: Prevents injection of unexpected data through unknown fields

## [0.34.2] - 2026-02-10

### Fixed
- Avoid null dereference warnings when detecting packaged JARs by using a path string check

### Changed
- Version bumped from 0.34.1 to 0.34.2

## [0.34.1] - 2026-02-10

### Fixed
- Guard runtime simulator process tracking against PID reuse by removing entries only when the same process exits
- Avoid null dereference when detecting packaged JARs and use executor execute to avoid ignored submit results

### Changed
- Version bumped from 0.34.0 to 0.34.1

## [0.34.0] - 2026-02-10

### Added
- Runtime simulation launcher now supports packaged Spring Boot JARs via `PropertiesLauncher` with `--loader.main`
- Process lifecycle management for runtime-launched simulators, including tracked PIDs, output logging, and graceful shutdown on service stop
- Runtime documentation for local vs packaged simulation launch assumptions

### Changed
- Version bumped from 0.33.5 to 0.34.0

## [0.33.5] - 2026-01-15

### Fixed
- **Validation Error Handling**: Hardened GlobalExceptionHandler to safely handle both field-level and object-level validation constraints
  - Fixed ClassCastException when processing object-level constraint violations (e.g., @AssertTrue on class methods)
  - Updated handleValidationErrors method to check error type before casting
  - Field-level errors (FieldError) now use field name as key
  - Object-level errors (ObjectError) now use object name as key
  - Both error types are properly surfaced in ValidationErrorResponse

### Added
- **Comprehensive Validation Tests**: Added GlobalExceptionHandlerValidationTest with 10 test cases
  - Test coverage for field-level validation errors (single and multiple fields)
  - Test coverage for object-level validation errors (e.g., password matching, date range validation)
  - Test coverage for mixed validation scenarios (both field and object errors)
  - Test DTOs with @AssertTrue constraints for object-level validation testing

### Changed
- Version bumped from 0.33.4 to 0.33.5
- GlobalExceptionHandler now imports ObjectError alongside FieldError
- Enhanced JavaDoc comments in GlobalExceptionHandler for validation error handling

## [0.33.4] - 2026-01-13

### Added
- **Database Backup and Restore Documentation**: Comprehensive documentation for protecting configuration data
  - Manual ad-hoc backup procedure using `pg_dump` with **cross-platform support**
    - Linux/macOS bash commands with date formatting
    - Windows Command Prompt commands with date/time variables
    - Windows PowerShell commands with Get-Date cmdlet
  - Automated scheduled backup integration via external PowerShell script (My-Scripts repository)
  - Standard and clean restore procedures using `psql` for **all platforms**
    - Linux/macOS restore commands with sudo
    - Windows restore commands (no sudo required)
  - Backup verification commands for **all platforms**
    - Linux/macOS: ls, head commands
    - Windows Command Prompt: dir, more, findstr commands
    - Windows PowerShell: Get-ChildItem, Get-Content cmdlets
  - Periodic restore testing procedures for **all platforms**
  - Reference to external automation infrastructure (Windows Task Scheduler + My-Scripts repository)
  - Database Backup and Restore section added to README with platform-specific commands
  - Architecture Decision Record (ADR-0012) documenting backup/restore strategy
    - Rationale for using native PostgreSQL tools (pg_dump/pg_restore)
    - Comparison with alternatives (WAL archiving, application-level export, cloud services, third-party tools)
    - Integration points with external automation repository
    - Backup file naming conventions and storage locations
    - Restore verification checklist
    - Platform-specific commands for Linux/macOS and Windows
    - Future considerations for enhanced backup features

### Changed
- Version bumped from 0.33.3 to 0.33.4
- README Database Setup section now includes comprehensive backup and restore procedures for all platforms
- ADR-0012 includes cross-platform commands (Linux/macOS, Windows Command Prompt, Windows PowerShell)
- ADR list updated to include ADR-0012

### Documentation
- Added Database Backup and Restore subsection to README (lines 634-803)
  - **Cross-platform manual backup commands** with timestamp-based filenames
    - Linux/macOS: bash with $(date) syntax
    - Windows CMD: %date% and %time% variables
    - Windows PowerShell: Get-Date cmdlet
  - When to use manual backups (before migrations, updates, risky operations)
  - Automated backup schedule and script location details
  - **Cross-platform restore procedures** for existing and new installations
    - Linux/macOS: sudo -u postgres psql commands
    - Windows: psql -U postgres commands (no sudo)
  - **Cross-platform backup verification** commands and periodic restore testing
    - Linux/macOS: ls, head, createdb, dropdb
    - Windows CMD: dir, more, findstr, createdb, dropdb
    - Windows PowerShell: Get-ChildItem, Get-Content, createdb, dropdb
  - Important notes on online backups and recovery mechanisms
- Added ADR-0012 to Architecture Decisions section in README
- Comprehensive ADR-0012 documenting backup/restore architectural decisions
  - Context: Need for data protection and disaster recovery
  - Decision: Use pg_dump/pg_restore with external automation
  - **Platform-specific implementation examples** for Linux/macOS and Windows
  - Consequences: Positive (data protection, portability), Negative (setup complexity, external dependency)
  - Alternatives considered: WAL archiving, app-level export, cloud services, third-party tools
  - Implementation notes: Script structure, Task Scheduler config, restore verification
  - References to My-Scripts repository for automation setup

### Notes
- This is a documentation-only release; no code changes or new features
- Backup automation is handled externally via My-Scripts repository (PowerShell script for Windows)
- All commands provided for **Linux/macOS and Windows platforms**
- Paths and schedules shown are local examples; implementations may vary

## [0.33.3] - 2026-02-01

### Fixed
- Excluded runtime controller constructor injection from SpotBugs EI_EXPOSE_REP2 reporting

## [0.33.2] - 2026-02-01

### Fixed
- Replaced SpotBugs suppression annotation with a compiler-safe suppression to restore build success

## [0.33.1] - 2026-02-01

### Fixed
- Suppressed SpotBugs false-positive for Spring controller dependency injection

## [0.33.0] - 2026-02-01

### Added
- Runtime simulator launch endpoint to start local simulations from published configurations
- Local simulator CLI entry point for running a configuration JSON file
- Admin UI action to launch simulator for published versions

### Changed
- Version bumped from 0.32.1 to 0.33.0
- Updated README and frontend docs for runtime launch workflow

## [0.32.1] - 2026-01-20

### Fixed
- SPA forwarding controller path pattern compatibility with Spring Boot 3 path matching

## [0.32.0] - 2026-01-20

### Added
- Frontend production bundling profile to build the React UI and include it in the Spring Boot JAR
- SPA route forwarding controller so client-side routes load correctly when served by Spring Boot

### Changed
- Version bumped from 0.31.0 to 0.32.0
- Updated README and frontend docs with clear dev vs production run instructions

## [0.31.0] - 2026-01-12

### Added
- **Configuration Editor UI**: Full-featured JSON editor for editing lift system version configurations
  - Dedicated configuration editor page at `/systems/:systemId/versions/:versionNumber/edit`
  - JSON textarea with monospace font for editing configuration
  - **Save Draft** functionality to persist changes without publishing
    - Updates version configuration via PUT endpoint
    - Shows last saved timestamp
    - Visual indicator for unsaved changes
  - **Validate** button to check configuration for errors
    - Real-time validation using backend API
    - Displays detailed error messages with field names
    - Shows warnings for suboptimal configurations
    - Color-coded validation results (green for valid, red for errors)
  - **Publish** action with validation enforcement
    - Only enabled when configuration is valid and saved
    - Requires validation to pass before publishing
    - Confirmation dialog before publishing
    - Automatic navigation back to system detail view after publish
  - Status badge showing current version status (DRAFT, PUBLISHED, ARCHIVED)
  - Read-only mode for published and archived versions (view only)
  - Split-pane layout with editor on left and validation results on right
  - Breadcrumb navigation back to system detail view
  - "Edit Config" button added to version cards in system detail view
    - Appears for DRAFT versions alongside Publish button
    - "View Config" button for published/archived versions
  - Responsive design with mobile support
- **ConfigEditor Component**: New React component for configuration editing
  - `ConfigEditor.jsx` - Main editor component with state management
  - `ConfigEditor.css` - Comprehensive styling for editor UI
- **Enhanced Version Actions**: Updated version card UI in system detail view
  - Version action buttons grouped together
  - Clear visual separation between draft and published version actions
  - Consistent button styling with proper spacing

### Changed
- Version bumped from 0.30.0 to 0.31.0
- Updated README with Configuration Editor feature documentation
  - Added Configuration Editor to Features list
  - Expanded React Admin UI features section with detailed editor capabilities
- Enhanced routing configuration in `App.jsx`
  - Added `/systems/:systemId/versions/:versionNumber/edit` route
  - Imported ConfigEditor component
- Updated `LiftSystemDetail.jsx` component
  - Added "Edit Config" and "View Config" buttons to version cards
  - Improved version action layout with grouped buttons

### Technical Details
- **Component Architecture**:
  - Uses React hooks (useState, useEffect) for state management
  - Parallel API calls with `Promise.all` for efficient data loading
  - Real-time validation state tracking
  - Unsaved changes detection via comparison with original config
- **User Experience**:
  - Disabled states for buttons when actions are not allowed
  - Tooltips on disabled Publish button explaining why it's disabled
  - Loading states for all async operations (saving, validating, publishing)
  - Error banners for operation failures with clear messages
  - Info banners for non-DRAFT versions indicating read-only mode
- **Styling**:
  - Color-coded validation messages (errors in red, warnings in yellow)
  - Consistent button colors (blue for primary, gray for secondary, green for publish)
  - Responsive grid layout that stacks on smaller screens
  - Monospace textarea with syntax-friendly styling

### Notes
- Only DRAFT versions can be edited; published and archived versions are view-only
- Configuration must be saved before validation results enable publishing
- Publishing requires valid configuration (no errors, warnings are allowed)
- Form-based configuration wizard remains a future enhancement opportunity

## [0.30.0] - 2026-01-12

### Added
- **Lift System List and Detail Views**: Complete CRUD interface for managing lift systems
  - Enhanced list view with working "View Details" and "Create New System" buttons
  - Create System modal with form validation
    - Validates system key pattern (alphanumeric, hyphens, underscores)
    - Validates display name and description length constraints
    - Real-time error feedback and help text
  - Detail view page for individual lift systems
    - Full system metadata display (display name, system key, description, timestamps)
    - Delete system functionality with confirmation dialog
    - Breadcrumb navigation back to list view
  - Version management interface in detail view
    - List all versions with status badges (DRAFT, PUBLISHED, ARCHIVED)
    - Create new versions with JSON configuration input
    - Publish draft versions with validation
    - Expandable configuration view for each version
    - Automatic archiving of previous published version when publishing
  - Seamless navigation between list and detail views
  - Responsive design with mobile support
- **Create System Modal Component**: Reusable modal for creating new lift systems
  - Client-side validation matching backend constraints
  - Form state management with error handling
  - Accessible modal overlay with click-outside-to-close
- **Routing**: Added `/systems/:id` route for detail view
- **UI Components**: Enhanced components with navigation handlers
  - `CreateSystemModal.jsx` - Form-based system creation modal
  - `LiftSystemDetail.jsx` - Comprehensive detail page with version management
  - Updated `LiftSystems.jsx` with working navigation and create functionality
  - Updated `App.jsx` with new routing configuration

### Changed
- Version bumped from 0.29.0 to 0.30.0
- Updated README with new features documentation
  - Enhanced Frontend Admin UI features section
  - Added Version Management capabilities
  - Updated feature descriptions to reflect full CRUD functionality
- Lift Systems page now fully functional with navigation to detail views
- Both "View Details" and "Manage Versions" buttons navigate to detail page

### Technical Details
- **Components**:
  - Modal component uses React Portal pattern for proper overlay rendering
  - Detail view uses `useParams` hook for route parameter extraction
  - Parallel API calls with `Promise.all` for efficient data fetching
- **Styling**:
  - Modal with backdrop overlay and responsive design
  - Status badges with color coding (green for PUBLISHED, yellow for DRAFT, gray for ARCHIVED)
  - Expandable `<details>` elements for configuration display
  - Consistent button styling and hover states
- **User Experience**:
  - Confirmation dialogs for destructive actions (delete, publish)
  - Loading states during async operations
  - Error messages with user-friendly feedback
  - Empty states for new systems with no versions
  - Automatic navigation to newly created system

## [0.29.0] - 2026-01-12

### Added
- **React Admin UI Scaffold**: Modern web-based admin interface for managing lift systems
  - Built with React 19.2.0, Vite 7.2.4, React Router 7.12.0, and Axios 1.13.2
  - Client-side routing with four main pages: Dashboard, Lift Systems, Config Validator, Health Check
  - Responsive layout with header navigation and footer
  - API client with Axios configured for backend integration
  - Vite dev server proxy configuration for seamless local development (eliminates CORS issues)
- **Dashboard Page**: Overview of lift systems with statistics and quick actions
  - Displays total lift systems and version counts
  - Quick action buttons for common tasks
  - Real-time data fetching from backend API
- **Lift Systems Page**: Management interface for lift system configurations
  - Grid view of all lift systems with metadata
  - Placeholder UI for create, view, and manage operations
  - Display of system key, description, version count, and creation date
- **Config Validator Page**: Interactive JSON configuration validation tool
  - Live configuration editor with sample configuration
  - Real-time validation feedback with error and success states
  - Utilizes backend validation API endpoint
  - Split-pane layout with editor and results
- **Health Check Page**: Backend service monitoring interface
  - Real-time health status display
  - Manual refresh capability
  - Detailed health information display
  - Error handling for service unavailability
- **Frontend Documentation**: Comprehensive README for frontend setup
  - Installation and setup instructions
  - Development workflow documentation
  - API integration details
  - Project structure overview
  - Troubleshooting guide
  - Production build instructions

### Changed
- Version bumped from 0.28.0 to 0.29.0
- Updated main README with frontend setup instructions and overview
- Added frontend section to Admin Interface documentation

### Technical Details
- **Tech Stack**:
  - React 19.2.0 with functional components and hooks
  - Vite 7.2.4 for fast development and optimized builds
  - React Router 7.12.0 for client-side routing
  - Axios 1.13.2 for HTTP requests
- **Development Setup**:
  - Frontend runs on port 3000
  - Backend runs on port 8080
  - Vite proxy configuration forwards `/api/*` and `/actuator/*` to backend
- **Project Structure**:
  - `frontend/src/api/` - API client and service methods
  - `frontend/src/components/` - Reusable UI components
  - `frontend/src/pages/` - Page components for routes
  - `frontend/src/App.jsx` - Root component with routing configuration
- **API Integration**: Frontend integrates with all backend endpoints
  - Lift Systems CRUD APIs
  - Version Management APIs
  - Configuration Validation API
  - Health Check API

### Documentation
- Added `frontend/README.md` with comprehensive setup guide
- Updated main `README.md` with frontend overview and quick start
- Documented local development workflow
- Added troubleshooting section for common issues

## [0.28.0] - 2026-01-11

### Added
- **Publish/Archive Workflow**: Automatic archiving of previously published versions when publishing a new version
  - Ensures exactly one published configuration per lift system at any given time
  - Enhanced `LiftSystemVersionService.publishVersion()` to automatically archive previously published versions
  - Transactional workflow guarantees atomic state transitions
  - No manual archiving required - reduces human error and improves workflow efficiency
- **Runtime Configuration API**: New dedicated API for retrieving published configurations
  - `RuntimeConfigService` providing read-only access to published configurations
  - `GET /api/runtime/systems/{systemKey}/config` - Get currently published configuration by system key
  - `GET /api/runtime/systems/{systemKey}/versions/{versionNumber}` - Get specific published version
  - Returns 404 if no published version exists
  - Clear separation between admin APIs (management) and runtime APIs (consumption)
  - `RuntimeConfigDTO` with streamlined response format
- **Comprehensive Test Coverage**: Tests for publish/archive workflow and runtime APIs
  - `testPublishVersion_ArchivesPreviouslyPublishedVersion()` validates automatic archiving behavior
  - `RuntimeConfigServiceTest` with 6 unit tests covering all runtime API scenarios
  - Tests for system not found, no published version, and version not published error paths
- **Documentation**: ADR-0010 for publish/archive workflow design decisions

### Changed
- Version bumped from 0.27.0 to 0.28.0
- `publishVersion()` method now archives previously published versions before publishing new version
- Publish workflow is transactional - if archiving or publishing fails, entire operation rolls back
- Updated service layer tests to verify archiving behavior

### Documentation
- Updated README with Runtime API documentation and usage examples
- Added ADR-0010 to Architecture Decisions section
- Updated version references from 0.27.0 to 0.28.0

### Technical Details
- Publish operation uses `@Transactional` to ensure atomicity
- Previously published versions are found via `findByLiftSystemIdAndIsPublishedTrue()`
- Each published version's `archive()` method sets status to ARCHIVED
- Runtime APIs filter for `isPublished = true` configurations only
- Runtime package structure: `com.liftsimulator.runtime.{controller,service,dto}`

## [0.27.0] - 2026-01-11

### Added
- **Configuration Validation Framework**: Comprehensive validation for lift system configuration JSON
  - `LiftConfigDTO` record with Jakarta Bean Validation annotations for structural validation
  - `ConfigValidationService` for domain-level validation logic
  - Structural validation ensures all required fields are present and correctly typed
  - Domain validation enforces business rules and cross-field constraints:
    - Validates `doorReopenWindowTicks` does not exceed `doorTransitionTicks`
    - Validates `homeFloor` is within valid floor range (0 to floors-1)
    - Validates minimum values for all numeric fields
    - Validates enum values for `controllerStrategy` and `idleParkingMode`
  - Warning system for suboptimal configurations:
    - Low `doorDwellTicks` values
    - More lifts than floors
    - Low `idleTimeoutTicks` with `PARK_TO_HOME_FLOOR` mode
    - Zero `doorReopenWindowTicks` (disables door reopening)
- **Validation REST API**: `POST /api/config/validate` endpoint
  - Validates configuration JSON without persisting
  - Returns structured errors and warnings
  - Request: `ConfigValidationRequest` with config JSON string
  - Response: `ConfigValidationResponse` with validation results
- **Version Publishing API**: `POST /api/lift-systems/{systemId}/versions/{versionNumber}/publish`
  - Publishes a version after validating its configuration
  - Only versions with valid configurations can be published
  - Returns 409 Conflict if version is already published
  - Blocks publishing if configuration has validation errors
- **Automatic Validation Integration**: Validation automatically enforced when:
  - Creating new versions (`POST /api/lift-systems/{systemId}/versions`)
  - Updating version configurations (`PUT /api/lift-systems/{systemId}/versions/{versionNumber}`)
  - Publishing versions (`POST /api/lift-systems/{systemId}/versions/{versionNumber}/publish`)
  - Returns 400 Bad Request with detailed validation errors if configuration is invalid
- **Validation DTOs**: Type-safe validation response objects
  - `ConfigValidationRequest` for validation endpoint requests
  - `ConfigValidationResponse` with valid flag, errors list, and warnings list
  - `ValidationIssue` record with field, message, and severity (ERROR or WARNING)
- **Exception Handling**: Enhanced global exception handling
  - `ConfigValidationException` for validation failures with detailed response
  - `IllegalStateException` handler returning 409 Conflict for state errors (e.g., already published)
  - `ConfigValidationException` handler returning validation response with 400 status
- **Comprehensive Test Coverage**: Unit tests for validation framework
  - `ConfigValidationServiceTest`: 15 unit tests covering all validation scenarios
    - Valid configuration test
    - Invalid JSON test
    - Missing required fields test
    - Negative/invalid value tests for each field
    - Domain validation rule tests (doorReopenWindowTicks, homeFloor)
    - Invalid enum value tests
    - Warning generation tests (low values, inefficient configurations)
    - Multiple errors test
  - `LiftSystemVersionServiceTest`: Updated with 6 additional tests
    - Publish version success test
    - Publish already published version test (IllegalStateException)
    - Publish with validation errors test (ConfigValidationException)
    - Create version with validation errors test
    - Update version with validation errors test
    - All existing tests updated to mock ConfigValidationService

### Changed
- Version bumped from 0.26.0 to 0.27.0
- `LiftSystemVersionService` now validates configurations before saving
  - Injected `ConfigValidationService` dependency
  - `createVersion()` validates before creating
  - `updateVersionConfig()` validates before updating
  - `publishVersion()` validates before publishing
- Version creation and updates now fail with 400 Bad Request if configuration is invalid
- Global exception handler expanded to handle validation and state exceptions

### Documentation
- Updated README with Configuration Validation section
  - Documented validation endpoint and request/response format
  - Added configuration structure table with all required fields
  - Documented validation rules for each field
  - Listed validation features (structural, type, domain, warnings)
  - Documented automatic validation behavior
  - Added error response format examples
- Added publish endpoint documentation to Version Management section
- Added ADR-0009 for Configuration Validation Framework design decisions

### Technical Details
- Uses Jakarta Bean Validation (JSR-380) for structural validation
- Custom domain validation logic in `ConfigValidationService`
- Validation errors block operations, warnings are informational only
- JSON parsing using Jackson ObjectMapper
- Defensive validation with detailed error messages for debugging

## [0.26.0] - 2026-01-11

### Added
- **Lift System Versioning REST APIs**: Full REST API implementation for managing lift system versions
  - `POST /api/lift-systems/{systemId}/versions` - Create new version with optional cloning from existing version
  - `PUT /api/lift-systems/{systemId}/versions/{versionNumber}` - Update version configuration JSON
  - `GET /api/lift-systems/{systemId}/versions` - List all versions for a lift system
  - `GET /api/lift-systems/{systemId}/versions/{versionNumber}` - Get specific version by number
- **Version Service Layer**: `LiftSystemVersionService` providing business logic for version operations
  - Auto-incrementing version numbers per lift system
  - Configuration cloning from existing versions
  - Validation for lift system and version existence
  - Transactional integrity for all write operations
- **Version DTOs**: Type-safe API contracts for versioning
  - `CreateVersionRequest` with validation (config required, optional cloneFromVersionNumber)
  - `UpdateVersionConfigRequest` for config updates
  - `VersionResponse` for consistent version API responses
- **Comprehensive Test Coverage**: Unit and integration tests for versioning
  - `LiftSystemVersionServiceTest`: 10 unit tests covering all service operations with mocks
  - `LiftSystemVersionControllerTest`: 13 integration tests with full Spring context
  - Tests for version creation, cloning, updating, listing, and retrieval
  - Tests for version number auto-increment functionality
  - Full coverage of success cases, error cases, and validation failures

### Changed
- Version bumped from 0.25.0 to 0.26.0
- REST API now provides full version lifecycle management for lift system configurations
- Version numbers automatically increment starting from 1 for each lift system

### Documentation
- Updated README with Version Management API documentation
- Added API endpoint examples with request/response payloads for all versioning operations
- Documented version cloning functionality and auto-increment behavior

## [0.25.0] - 2026-01-11

### Added
- **Lift System CRUD REST APIs**: Full REST API implementation for managing lift systems
  - `POST /api/lift-systems` - Create new lift system with name and description
  - `GET /api/lift-systems` - List all lift systems
  - `GET /api/lift-systems/{id}` - Get lift system details by ID
  - `PUT /api/lift-systems/{id}` - Update lift system metadata (name/description)
  - `DELETE /api/lift-systems/{id}` - Delete lift system and all versions (cascade)
- **Service Layer**: `LiftSystemService` providing business logic for CRUD operations
  - Validation for duplicate system keys on creation
  - Automatic audit timestamp management (createdAt, updatedAt)
  - Transactional integrity for all write operations
  - Proper error handling with custom exceptions
- **Request/Response DTOs**: Type-safe API contracts
  - `CreateLiftSystemRequest` with validation (systemKey, displayName, description)
  - `UpdateLiftSystemRequest` for metadata updates (displayName, description)
  - `LiftSystemResponse` for consistent API responses
  - Jakarta validation annotations for request validation
- **Global Exception Handling**: Consistent error responses across all endpoints
  - `GlobalExceptionHandler` with `@RestControllerAdvice`
  - 404 responses for `ResourceNotFoundException`
  - 400 responses for validation errors and illegal arguments
  - Structured error responses with status, message, and timestamp
  - Field-level validation error details in responses
- **Comprehensive Test Coverage**: Unit and integration tests
  - `LiftSystemServiceTest`: 9 unit tests covering all service operations with mocks
  - `LiftSystemControllerTest`: 10 integration tests with full Spring context
  - Tests for success cases, error cases, and validation failures
  - Full coverage of create, read, update, delete operations
  - Transaction rollback testing with `@Transactional`

### Changed
- Version bumped from 0.24.0 to 0.25.0
- REST API now provides full lifecycle management for lift systems
- Service layer enforces business rules and validation

### Fixed
- **SpotBugs warnings**: Resolved static analysis warnings
  - `ValidationErrorResponse` now uses defensive copying with `Map.copyOf()` to prevent external modification
  - Added `@SuppressFBWarnings` to `LiftSystemController` constructor for Spring DI false positive
  - All medium-severity EI_EXPOSE_REP warnings resolved

### Documentation
- Updated README with Lift System CRUD API documentation
- API endpoint examples with request/response payloads
- Error response format documentation

## [0.24.0] - 2026-01-11

### Added
- **JPA Entities**: Created JPA entity mappings for database tables
  - `LiftSystem` entity mapping `lift_system` table with automatic timestamp management
  - `LiftSystemVersion` entity mapping `lift_system_version` table
  - One-to-many relationship between LiftSystem and LiftSystemVersion with cascade operations
  - Entity helper methods: `publish()`, `archive()` for version lifecycle management
- **JSONB Field Mapping**: Implemented proper PostgreSQL JSONB support
  - Uses `@JdbcTypeCode(SqlTypes.JSON)` annotation for Hibernate 6.x compatibility
  - Stores lift configuration as JSON strings in the `config` column
  - Full support for complex nested JSON structures
- **Spring Data Repositories**: Created repository interfaces for database access
  - `LiftSystemRepository` with custom query methods (`findBySystemKey`, `existsBySystemKey`)
  - `LiftSystemVersionRepository` with comprehensive query methods:
    - Find versions by lift system with ordering
    - Find by version number
    - Find published versions
    - Find by status (DRAFT, PUBLISHED, ARCHIVED)
    - Get maximum version number for a system
- **Integration Tests**: Comprehensive test coverage for JPA operations
  - `LiftSystemRepositoryTest`: 7 tests covering CRUD, queries, and updates
  - `LiftSystemVersionRepositoryTest`: 12 tests covering versions, JSONB, relationships
  - H2 in-memory database for testing with PostgreSQL compatibility mode
  - Test configuration with dedicated test profile (`application-test.yml`)
  - No external database required for running tests
  - Tests verify basic save/find operations, JSONB mapping, and cascading deletes
- **JPA Verification Runner**: Command-line tool to verify database operations
  - Enabled with `--spring.jpa.verify=true` flag
  - Tests all entity CRUD operations
  - Validates JSONB field persistence and retrieval
  - Verifies entity relationships and cascading
  - Tests all custom repository query methods
  - Located at `com.liftsimulator.admin.runner.JpaVerificationRunner`

### Changed
- Bump project version to 0.24.0 to reflect new JPA persistence layer

### Documentation
- Updated README with comprehensive JPA entities and repositories documentation
- Added JPA verification instructions and examples
- Documented JSONB field mapping approach
- Added integration test running instructions
- Updated all version references from 0.23.6 to 0.24.0

## [0.23.6] - 2026-01-09

### Fixed
- Ensure Flyway migrations are retained in build output by cleaning stale resources before the copy phase.
- Bump project version to 0.23.6 to reflect the migration packaging fix.

## [0.23.5] - 2026-01-09

### Fixed
- Document cleanup steps for legacy `public.schema_metadata`.
- Bump project version to 0.23.5 to reflect the legacy schema guidance.

## [0.23.4] - 2026-01-09

### Fixed
- Document cleanup steps when Flyway history exists in the `public` schema.
- Bump project version to 0.23.4 to reflect the schema-history guidance.

## [0.23.3] - 2026-01-09

### Fixed
- Configure Flyway to target the `lift_simulator` schema so baseline migrations create the expected tables.
- Document the schema-specific Flyway behavior and update database setup guidance.
- Bump project version to 0.23.3 to reflect the schema migration fix.

## [0.23.2] - 2026-01-09

### Fixed
- Remove stale Flyway migration resources during build so older `V1__init.sql` artifacts cannot collide with `V1__init_schema.sql`.
- Bump project version to 0.23.2 to reflect the migration cleanup fix.

## [0.23.1] - 2026-01-09

### Fixed
- Restore the baseline Flyway migration filename to `V1__init_schema.sql` to avoid duplicate version 1 migrations after upgrading.
- Document the cleanup step for stale `V1__init.sql` artifacts when upgrading from 0.23.0.
- Bump project version to 0.23.1 to reflect the migration fix.

## [0.23.0] - 2026-01-09

### Added
- Create the initial lift configuration schema with Flyway migration `V1__init.sql`.
- Add the `lift_simulator` schema with `lift_system` and `lift_system_version` tables for versioned JSONB configurations.
- Include publish status fields, foreign keys, and indexes for lift system versions.

### Changed
- Bump project version to 0.23.0 to reflect the new database schema.

## [0.22.3] - 2026-01-09

### Fixed
- Correct the development database password for `lift_admin` in configuration and documentation.
- Bump project version to 0.22.3 to reflect the credential update.

## [0.22.2] - 2026-01-09

### Fixed
- Remove the unused Flyway PostgreSQL module dependency so Maven resolves Flyway from Spring Boot's managed version.
- Bump project version to 0.22.2 to reflect the build fix.

## [0.22.1] - 2026-01-09

### Fixed
- Add an explicit Flyway PostgreSQL artifact version in the Maven POM to restore dependency resolution.
- Bump project version to 0.22.1 to reflect the build fix.

## [0.22.0] - 2026-01-09

### Added
- **PostgreSQL database connectivity**: Integrated PostgreSQL as the primary database for the Lift Config Service backend
  - Spring Data JPA for database access layer
  - PostgreSQL JDBC driver for database connectivity
  - HikariCP connection pooling (via Spring Boot defaults)
  - Configured for local development with dedicated database and user
- **Flyway database migration management**: Version-controlled schema migrations with automatic execution on startup
  - Flyway Core for migration engine
  - Flyway PostgreSQL-specific support
  - Initial baseline migration (V1__init_schema.sql) creating schema metadata tracking table
  - Automatic `flyway_schema_history` table creation for migration tracking
  - Baseline-on-migrate enabled for smooth initial setup
- **Profile-based configuration**: Separation of environment-specific settings
  - New `application-dev.yml` for development profile with PostgreSQL configuration
  - Default profile set to `dev` in `application.properties`
  - Database connection settings: `localhost:5432/lift_simulator`
  - Development credentials: `lift_admin` user with secure password
  - Connection pool tuning: max 5 connections, min 2 idle, 30s timeout
- **JPA/Hibernate configuration**: Optimized for development and PostgreSQL
  - PostgreSQL dialect configured
  - DDL auto-validation (validate-only, no auto-generation)
  - SQL logging enabled in dev profile for debugging
  - Formatted SQL output with comments
  - Batch processing optimized (batch size 20, ordered inserts/updates)
  - Open-in-view disabled for better transaction management
- **Database schema initialization**:
  - `schema_metadata` table tracking application versions and schema changes
  - Indexed version column for efficient lookups
  - Comprehensive table and column comments for documentation
  - Initial version record (0.22.0) inserted automatically
- **Documentation enhancements**:
  - Comprehensive "Database Setup" section in README with step-by-step instructions
  - PostgreSQL prerequisites and installation verification
  - Database and user creation commands
  - Connection verification steps
  - Configuration profiles explanation
  - Troubleshooting guide for common database issues
  - Schema overview and future migration guidance
- **Architecture Decision Record**: ADR-0007 documenting PostgreSQL and Flyway integration rationale (see `docs/decisions/0007-postgresql-flyway-integration.md`)

### Changed
- Version bumped from 0.21.0 to 0.22.0
- `application.properties` now includes database-related settings
- Spring Boot application now requires PostgreSQL database to start
- README updated with database setup instructions and new version references
- Build artifact name updated to `lift-simulator-0.22.0.jar`

### Technical Details
- **Dependencies added**:
  - `spring-boot-starter-data-jpa` - JPA and Hibernate ORM
  - `postgresql` (runtime scope) - PostgreSQL JDBC driver
  - `flyway-core` - Database migration framework
  - `flyway-database-postgresql` - PostgreSQL-specific Flyway support
- **Database schema version**: V1 (baseline)
- **Flyway settings**:
  - Migration location: `classpath:db/migration`
  - Baseline version: 0
  - Validation enabled on migrate
  - Out-of-order migrations disabled
- **Hibernate settings**:
  - Dialect: `org.hibernate.dialect.PostgreSQLDialect`
  - DDL auto: `validate` (production-safe, no auto-schema changes)
  - SQL logging: DEBUG level in dev profile
  - Parameter binding trace: TRACE level for troubleshooting
- **Connection pool** (HikariCP):
  - Maximum pool size: 5 connections
  - Minimum idle: 2 connections
  - Connection timeout: 30 seconds
  - Idle timeout: 10 minutes
  - Max lifetime: 30 minutes
- Database configuration follows Spring Boot best practices for production readiness
- Clear separation between base configuration and profile-specific settings
- Migration-first approach ensures consistent schema across environments

### Migration Notes
For developers updating from v0.21.0:
1. Install PostgreSQL 12 or later
2. Create database and user as documented in README
3. Run `mvn clean install` to download new dependencies
4. Start the application - Flyway will initialize the schema automatically
5. Verify schema with: `psql -U lift_admin -d lift_simulator -c "\dt"`

## [0.21.0] - 2026-01-09

### Added
- **Spring Boot backend for Lift Config Service**: Bootstrapped a runnable Spring Boot backend skeleton for managing lift simulator configurations
  - Spring Boot 3.2.1 with Maven configuration
  - Standard package structure: `admin/controller`, `admin/service`, `admin/repository`, `admin/domain`, `admin/dto`
  - Spring Boot application class: `LiftConfigServiceApplication`
  - Custom health endpoint at `/api/health` returning service name, status, and timestamp
  - Spring Boot Actuator for production-ready health monitoring at `/actuator/health` and `/actuator/info`
  - Application configuration via `application.properties`:
    - Application name: `lift-config-service`
    - Server port: 8080
    - Logging: INFO level (root), DEBUG level (com.liftsimulator package)
    - Custom console and file logging patterns
  - Cross-platform support: Runs on Windows, Linux, and macOS with Java 17+
- Documentation in README:
  - "Admin UI Backend" section with setup and running instructions
  - Available endpoints documentation
  - Configuration details
  - Updated project structure showing new admin package hierarchy

### Changed
- Maven POM now inherits from `spring-boot-starter-parent` for Spring Boot dependency management
- Project description updated to reflect Spring Boot backend integration
- Version bumped from 0.20.0 to 0.21.0

### Technical Details
- Uses Spring Boot 3.2.1 (requires Java 17+)
- Spring Boot Web Starter for RESTful API capabilities
- Spring Boot Actuator for health monitoring and metrics
- Existing lift simulator functionality remains unchanged and independent
- Backend provides foundation for future admin UI development

## [0.20.0] - 2026-01-09

### Added
- **Comprehensive scenario-based test suite** for both controller strategies (ControllerScenarioTest.java)
  - Reusable `ScenarioHarness` test utility for deterministic scenario testing
    - Supports tick simulation with configurable timing parameters
    - Request injection at specific ticks (hall calls and car calls)
    - Service event logging with floor and direction tracking
    - Assertion utilities for service order, direction transitions, and queue state
  - **DirectionalScan controller scenario tests** validating scheduling outcomes:
    - Canonical scenario from README documentation (floor 2 UP, floor 5 car, floor 3 DOWN)
    - Mixed calls above and below while moving (validates direction commitment)
    - Idle to direction selection to completion with multiple reversals
    - Single direction multiple stops (validates batching efficiency)
    - Alternating directions with hall and car calls
    - Service order precisely asserted with expected floor sequences
    - Direction transitions validated (UP sweep, reversal, DOWN sweep)
  - **NaiveLift controller scenario tests** protecting current behavior:
    - Nearest-first routing validation
    - Back-and-forth movement patterns (demonstrates inefficiency vs directional)
    - Mixed call types (hall and car calls)
    - Dynamic request addition during movement
    - Single request baseline test
  - **Comparison test** running identical scenario with both strategies to highlight differences
- All scenario tests verify:
  - Deterministic and reliable execution
  - All requests reach COMPLETED state
  - Queue is fully cleared after scenario completion
  - No lost or duplicated requests

### Changed
- Test suite now provides comprehensive coverage of realistic multi-request routing scenarios
- Both controller strategies protected against behavioral regressions through scenario tests

### Technical Details
- ScenarioHarness tracks last moving direction to accurately log service events
- Tests use realistic timing: 1 tick/floor travel, 2 ticks door transition, 3 ticks door dwell
- Maximum tick limits prevent infinite loops while allowing sufficient time for complex scenarios
- Service event logging captures tick, floor, and direction for detailed assertions

## [0.19.0] - 2026-01-09

### Added
- **Directional/SCAN controller integration**: DirectionalScanLiftController is now fully integrated with the simulation lifecycle
  - Accepts new requests during movement and schedules them according to directional rules
  - Recomputes next target stop dynamically without breaking invariants
  - Updates targets appropriately at floor arrivals and door cycles
  - Maintains key invariants: no duplicate servicing, no lost requests
  - Full compatibility with existing door open/close timing semantics
- Command-line flag `--strategy=<strategy>` for Main demo application to select controller strategy
  - Valid values: `nearest-request` (default), `directional-scan`
  - Example: `java -jar lift-simulator.jar --strategy=directional-scan`
- Comprehensive end-to-end integration tests for DirectionalScanLiftController (DirectionalScanIntegrationTest.java)
  - Tests request acceptance during movement
  - Tests proper direction-aware scheduling of hall calls
  - Tests request cancellation handling
  - Tests out-of-service and return-to-service scenarios
  - Tests complex multi-request scenarios ensuring no lost requests
  - Validates no duplicate servicing through state transition tracking
- Detailed README documentation of controller strategies
  - Comparison between Nearest Request Routing and Directional Scan algorithms
  - Example scenarios demonstrating directional scan behavior
  - Documentation of advantages and use cases for each strategy
  - Clear explanation of hall call filtering and direction commitment

### Changed
- Main.java now supports `--strategy` command-line argument for controller selection
- Main.java help text updated to document the new `--strategy` flag
- README updated with comprehensive DirectionalScanLiftController documentation
- README demo configuration section updated to show controller selection examples

### Technical Details
- DirectionalScanLiftController was implemented in v0.16.0 but not integrated with the main application
- This release completes the integration, making the controller usable in the demo application
- Regression testing confirms NaiveLiftController (nearest-request) continues to work unchanged
- All existing tests pass, confirming no behavioral regressions

## [0.18.1] - 2026-01-12

### Fixed
- Scenario runner now prints request lifecycle summaries after execution

## [0.18.0] - 2026-01-12

### Added
- Demo output now includes a request lifecycle summary table showing created and completed/cancelled ticks for each request

## [0.17.2] - 2026-01-12

### Changed
- Directional scan controller keeps traveling to the furthest pending stop in the current direction before reversing, even when only opposite-direction hall calls remain

## [0.17.1] - 2026-01-12

### Changed
- Directional scan controller now rides to the furthest pending stop in the current direction before reversing, even when only opposite-direction hall calls remain

## [0.17.0] - 2026-01-11

### Added
- Directional scan controller tests covering hall-call direction filtering and car-call eligibility

### Changed
- Directional scan controller now defers opposite-direction hall calls until after reversal and filters stop eligibility by hall-call direction

## [0.16.0] - 2026-01-10

### Added
- Directional scan controller implementation that continues in the current direction until requests are serviced
- Scenario runner support for DIRECTIONAL_SCAN controller strategy
- Directional scan controller unit tests covering direction commitment and reversal

### Changed
- Controller factory now instantiates the directional scan controller
- Scenario and main demo now use a shared request-managing controller interface

## [0.15.0] - 2026-01-09

### Added
- Explicit `idle_parking_mode` configuration in `demo.scenario` with inline documentation
- Comments in `demo.scenario` documenting available controller configuration options
- Comprehensive README documentation for command-line usage of both demo and scenario runner

### Changed
- Demo and scenario runner now display selected controller strategy and idle parking mode at startup
- Demo.scenario now explicitly shows both controller_strategy and idle_parking_mode with comments
- Demo and scenario runner only expose `--help` on the command line; controller strategy and idle parking mode are configured via scenario files or defaults

## [0.14.0] - 2026-01-09

### Added
- Selectable controller strategy with `ControllerStrategy` enum
  - `NEAREST_REQUEST_ROUTING`: Services nearest request first (current NaiveLiftController behavior, default)
  - `DIRECTIONAL_SCAN`: Directional scan algorithm (placeholder for future implementation)
- `ControllerFactory` class for creating controller instances based on strategy
  - Factory method with default parameters: `createController(ControllerStrategy strategy)`
  - Factory method with custom parameters: `createController(ControllerStrategy strategy, int homeFloor, int idleTimeoutTicks, IdleParkingMode idleParkingMode)`
- Scenario file support for `controller_strategy:` configuration parameter
- Architecture Decision Record (ADR-0005) documenting controller strategy design
- Comprehensive unit tests for `ControllerFactory` (6 test cases)
- Integration tests verifying controller selection in scenarios (4 test cases)

### Changed
- `ScenarioDefinition`, `ScenarioParser`, and `ScenarioRunnerMain` updated to support controller strategy configuration
- `Main.java` updated to use `ControllerFactory` with explicit `NEAREST_REQUEST_ROUTING` strategy
- `demo.scenario` updated to include explicit controller strategy configuration

### Design Decisions
- Enum-based strategy selection provides type safety and compile-time validation
- Factory pattern centralizes controller instantiation logic
- Default strategy preserves backward compatibility (NEAREST_REQUEST_ROUTING)
- Unimplemented strategies (DIRECTIONAL_SCAN) throw `UnsupportedOperationException` with clear error messages
- Controller strategy is configured at initialization time (not runtime switchable)

## [0.13.0] - 2026-01-09

### Added
- Configurable idle parking mode with `IdleParkingMode` enum
  - `STAY_AT_CURRENT_FLOOR`: Lift remains at current floor when idle (no parking movement)
  - `PARK_TO_HOME_FLOOR`: Lift moves to home floor after idle timeout (existing behavior, now default)
- Three-parameter `NaiveLiftController` constructor accepting `homeFloor`, `idleTimeoutTicks`, and `idleParkingMode`
- Scenario file support for `idle_parking_mode:` configuration parameter
- Architecture Decision Record (ADR-0004) documenting idle parking mode design
- Comprehensive unit tests for both parking modes (6 new test cases)

### Changed
- Idle parking logic now respects configured mode (only initiates parking movement when mode is `PARK_TO_HOME_FLOOR`)
- `ScenarioDefinition`, `ScenarioParser`, and `ScenarioRunnerMain` updated to support idle parking mode configuration

## [0.12.17] - 2026-01-25

### Changed
- Centralize request completion transitions in `LiftRequest` to avoid duplicated lifecycle chains

## [0.12.16] - 2026-01-24

### Fixed
- Guard idle timeout calculations in the naive controller against null idle tracking state

## [0.12.15] - 2026-01-23

### Changed
- Standardize comment formatting across the codebase and tests

## [0.12.14] - 2026-01-22

### Fixed
- Enforce scenario tick and event limits to prevent resource exhaustion when parsing scenario files

## [0.12.13] - 2026-01-21

### Changed
- Keep dedicated active/completed request collections in the naive controller to avoid rebuilding active sets during scheduling

## [0.12.12] - 2026-01-20

### Changed
- Index active lift requests by ID in the naive controller to avoid linear lookups during cancellation

## [0.12.11] - 2026-01-19

### Added
- Add OWASP Dependency-Check Maven plugin to scan for vulnerable dependencies during builds

## [0.12.10] - 2026-01-18

### Fixed
- Replace SpotBugs annotation dependency with a local exclude filter to keep builds resolving offline

## [0.12.7] - 2026-01-15

### Added
- Expand CI to include build, coverage, style checks, static analysis, and packaging steps
- Add Checkstyle and SpotBugs tooling with a shared Checkstyle configuration

## [0.12.6] - 2026-01-14

### Added
- Add package-level documentation for the core lift simulator packages

## [0.12.5] - 2026-01-13

### Changed
- Add JavaDoc coverage for public APIs in the lift engine and domain enums

## [0.12.4] - 2026-01-12

### Added
- Add JaCoCo coverage reporting and enforce an 80% line coverage minimum in Maven builds
- Add integration tests covering scenario parsing, execution, and invalid scenario handling

### Changed
- Refactor simulation engine tests to use a sequence-based controller helper to reduce boilerplate
- Extend the move-down test sequence to cover all downward ticks

### Fixed
- Validate scenario event ticks to reject negative or malformed values with clear error messages
- Return explicit action results and log invalid action attempts in the simulation engine to avoid silent failures

## [0.12.3] - 2026-01-11

### Added
- Add a test reset hook for the lift request ID generator to improve test isolation

## [0.12.2] - 2026-01-10

### Changed
- Refactor tick processing into state-specific handlers for clearer maintenance

## [0.12.1] - 2026-01-10

### Changed
- Extract default timing values and sentinel configuration into named constants

## [0.12.0] - 2026-01-10

### Changed
- Replace telescoping `SimulationEngine` constructors with a builder for configuration

## [0.11.0] - 2026-01-10

### Added
- Allow scenario files to configure controller and simulation timing parameters (including door dwell time) alongside scripted events

### Fixed
- Expand scenario runner floor bounds to include requested negative floors while keeping the default range and starting floor

## [0.10.2] - 2026-01-10

### Fixed
- Prevent direction reversals while still in a moving state by stopping before changing direction

## [0.10.1] - 2026-01-10

### Fixed
- Defer return-to-service events until the out-of-service shutdown sequence completes

## [0.10.0] - 2026-01-09

### Added
- Scenario runner for scripted simulations with tick-based event triggers
- Plain-text scenario definitions with support for car calls, hall calls, cancellations, and service mode changes
- Scenario output logging that includes tick, floor, lift state, and pending requests

## [0.9.4] - 2026-01-08

### Fixed
- Ignore new request assignments while a lift is out of service

## [0.9.3] - 2026-01-08

### Fixed
- Align out-of-service integration test flow with the required stop tick before door opening

## [0.9.2] - 2026-01-08

### Fixed
- Preserve MOVING_* status on arrival until controller decisions so door dwell timing and parking interruptions occur after a stop tick

## [0.9.1] - 2026-01-08

### Fixed
- Normalize movement status before controller decisions so doors can open immediately after arriving at a requested floor
- Avoid reopening doors during out-of-service shutdown when doors are already open or opening

## [0.9.0] - 2026-01-08

### Added
- **Out-of-service functionality**: Full support for taking lifts out of service safely
- `takeOutOfService()` method in `NaiveLiftController` to cancel all active requests when taking lift out of service
- `returnToService()` method in `NaiveLiftController` to prepare lift for returning to normal operation
- `setOutOfService()` method in `SimulationEngine` to transition lift to OUT_OF_SERVICE state from any state
- `returnToService()` method in `SimulationEngine` to transition from OUT_OF_SERVICE back to IDLE state
- Automatic cancellation of all pending requests (QUEUED, ASSIGNED, SERVING) when taking lift out of service
- Comprehensive test suite (`OutOfServiceTest`) covering entering/exiting service from all states
- Demo updated to showcase out-of-service scenario at tick 25 and return to service at tick 30
- Documentation of out-of-service behavior in README with usage examples

### Changed
- OUT_OF_SERVICE state (already existed since v0.2.0) now has full operational support
- Lifts in OUT_OF_SERVICE state cannot move, open doors, or accept new requests
- Demo simulation extended to 50 ticks to show complete out-of-service workflow

### Design Decisions
- Taking lift out of service immediately cancels all unserviced requests for safety
- **Graceful shutdown sequence**: If moving, lift completes movement to next floor; doors then open to allow passenger exit, then close, before transitioning to OUT_OF_SERVICE
- Ensures passenger safety by allowing exit before going offline
- If lift is stationary with doors closed, doors still open/close to ensure nobody is trapped
- Door state is CLOSED when out of service (derived from status)
- Direction is IDLE when out of service (derived from status)
- Returning to service transitions to IDLE state, ready to accept new requests
- Two-step process: controller cancels requests, engine manages graceful shutdown (separation of concerns)

## [0.8.0] - 2026-01-06

### Added
- Configurable idle parking behavior for the naive controller with home floor and idle timeout
- Parking logic that resumes normal request handling when new requests arrive
- Tests covering idle parking and interruption scenarios

## [0.7.1] - 2026-01-06

### Fixed
- Align cancellation-while-moving integration test expectations with tick-per-floor movement behavior

## [0.7.0] - 2026-01-06

### Added
- **Request cancellation API**: New `cancelRequest(long requestId)` method in `NaiveLiftController` to safely cancel pending requests
- Ability to cancel requests in any non-terminal state (QUEUED, ASSIGNED, SERVING)
- Automatic removal of cancelled requests from the controller's queue
- Return value indicates cancellation success (false if request not found or already terminal)
- Comprehensive unit tests for cancelling requests in each lifecycle state
- Integration tests for cancellation scenarios (while moving, multiple requests same floor)
- Lifecycle tests verifying CANCELLED terminal state behavior

### Design Decisions
- Cancelled requests transition to CANCELLED state before removal, maintaining lifecycle integrity
- Cancellation is idempotent - calling multiple times on same request ID is safe
- Cancelled requests are immediately removed from queue to prevent processing
- Existing `isTerminal()` checks automatically filter cancelled requests from active processing
- No ADR created as this extends the existing request lifecycle architecture (ADR-0003)

## [0.6.0] - 2026-01-06

### Added
- **Door reopening window**: Configurable time window during door closing when doors can be reopened for new requests
- `doorReopenWindowTicks` parameter to `SimulationEngine` constructor (default: `min(2, doorTransitionTicks)`)
- Realistic door behavior: doors reopen if request arrives for current floor within the reopen window
- Automatic request queuing when door closing window has passed
- Validation ensuring `doorReopenWindowTicks` is non-negative and does not exceed `doorTransitionTicks`
- Comprehensive unit tests for door reopening scenarios (within window, outside window, boundary cases, zero window)
- Integration tests with `NaiveLiftController` demonstrating real-world door reopening behavior
- Backward compatibility: default window automatically adjusts to `doorTransitionTicks` when smaller than 2

### Changed
- `NaiveLiftController` now attempts to reopen doors when a request arrives for the current floor during door closing
- `SimulationEngine` tracks elapsed ticks during door closing to enforce reopen window
- Door reopening logic in `startAction()` checks elapsed time against configured window

### Design Decisions
- Reopen window measured in simulation ticks for consistency with other timing parameters
- Window applies only during DOORS_CLOSING state, not after doors fully close
- Zero-tick window disables reopening entirely (doors cannot be interrupted once closing starts)
- Request remains queued if door closing window has passed, lift will serve it after current cycle

## [0.5.0] - 2026-01-06

### Added
- **Lift request lifecycle management**: Requests are now first-class entities with explicit lifecycle states
- `LiftRequest` domain model with unique ID, type, origin, destination, and direction
- `RequestState` enum with six lifecycle states: CREATED, QUEUED, ASSIGNED, SERVING, COMPLETED, CANCELLED
- `RequestType` enum to distinguish between HALL_CALL and CAR_CALL requests
- State transition validation ensuring only valid request state changes occur
- Factory methods `LiftRequest.hallCall()` and `LiftRequest.carCall()` for creating requests
- Terminal state detection with `isTerminal()` method
- Comprehensive unit tests for request lifecycle (LiftRequestTest, LiftRequestLifecycleTest)
- ADR-0003 documenting request lifecycle architectural decision

### Changed
- `NaiveLiftController` now manages requests as `LiftRequest` objects internally
- Controller automatically advances requests through lifecycle states (CREATED → QUEUED → ASSIGNED → SERVING → COMPLETED)
- Completed and cancelled requests are automatically removed from the controller
- Backward compatibility maintained: `addCarCall()` and `addHallCall()` methods still work
- Demo output now displays request lifecycle status with compact "Requests" column showing counts by state (Q:n, A:n, S:n)

### Design Decisions
- Every request must end in either COMPLETED or CANCELLED state (terminal states)
- Invalid state transitions throw `IllegalStateException`
- Self-transitions are prevented
- Terminal states cannot transition to any other state
- Request state progression is automatically managed by the controller

## [0.4.0] - 2026-01-13

### Added
- Added configurable door dwell timing with automatic door close cycles
- Extended simulation engine timing tests to cover door dwell and closing transitions

### Changed
- Door cycles now automatically progress from open to closing after the dwell duration

### Fixed
- Align door closing timing from dwell with door transition tick consumption

## [0.3.0] - 2026-01-12

### Added
- Introduced a deterministic simulation clock to manage tick progression
- Added configurable travel and door transition durations to model time per floor
- Expanded unit tests to validate tick-based movement and door timing
- Added a deterministic simulation regression test for fixed inputs

### Changed
- Movement and door transitions now consume configured ticks before completing

## [0.2.9] - 2026-01-06

### Fixed
- Configure Maven to use Java release targeting to avoid module path warnings
- Explicitly set the resources plugin encoding for filtered properties files

## [0.2.8] - 2026-01-11

### Fixed
- Keep the naive controller idle while doors are closing to avoid invalid move attempts

## [0.2.7] - 2026-01-11

### Added
- Print the running software version in the demo output

## [0.2.6] - 2026-01-11

### Fixed
- Keep the NaiveLiftController idle during door opening to prevent invalid move attempts
- Clear same-floor requests while doors are opening to avoid redundant open commands

## [0.2.5] - 2026-01-11

### Fixed
- Clear same-floor requests while doors are opening or open to avoid extra open/close cycles

## [0.2.4] - 2026-01-11

### Fixed
- Ensure the NaiveLiftController stops before opening doors when arriving at a requested floor while moving

## [0.2.3] - 2026-01-10

### Fixed
- Include lift status in the NaiveLiftController demo output so transitional states are visible

## [0.2.2] - 2026-01-10

### Fixed
- Align door-opening test flow with state machine requirements before attempting to move down

## [0.2.1] - 2026-01-10

### Fixed
- Add missing `LiftStatus` import in `SimulationEngineTest` to restore test compilation

## [0.2.0] - 2026-01-06

### Added
- Introduced formal lift state machine with `LiftStatus` enum (7 states)
- New lift states: IDLE, MOVING_UP, MOVING_DOWN, DOORS_OPENING, DOORS_OPEN, DOORS_CLOSING, OUT_OF_SERVICE
- `DOORS_OPENING` state for symmetric door transitions
- `StateTransitionValidator` class to enforce valid state transitions
- Comprehensive unit tests for state machine transitions and validation
- State transition table in README documentation
- ADR-0002 documenting single source of truth architectural decision
- State machine prevents invalid operations (e.g., movement with doors open)
- Logging for invalid state transitions

### Changed
- **Breaking**: `LiftState` refactored to single source of truth pattern
- `LiftState` now stores only `floor` and `status` (direction and doorState are derived)
- Constructor signature changed from `LiftState(floor, direction, doorState, status)` to `LiftState(floor, status)`
- `Direction` and `DoorState` are now computed properties, not stored fields
- `SimulationEngine` refactored to be state-driven with enforced transitions
- State transitions are validated before being applied
- Door behavior is now symmetric (both opening and closing are transitional states)

### Fixed
- Eliminated possibility of invalid state combinations (e.g., moving with doors open)
- Lift can no longer move when doors are open (enforced by state machine)
- Invalid state transitions are prevented and logged
- Door state changes are now controlled by the state machine
- Removed redundant state storage reducing memory usage

## [0.1.3] - 2026-01-09

### Fixed
- Use enum comparison for door state checks in the demo output

## [0.1.2] - 2026-01-08

### Fixed
- Make nearest-floor selection deterministic with a tie-breaker for equidistant requests
- Added tests covering deterministic behavior for equidistant requests

## [0.1.1] - 2026-01-07

### Fixed
- Initialize the simulation state at the configured minimum floor
- Normalize direction to idle when movement is requested at top or bottom floors
- Added tests for non-zero minimum floor initialization and boundary direction behavior

## [0.1.0] - 2026-01-06

### Added
- Initial tick-based simulation engine
- `SimulationEngine` class for advancing time in discrete ticks
- `LiftState` immutable domain model (floor, direction, door state)
- `Action` enum for lift actions (MOVE_UP, MOVE_DOWN, OPEN_DOOR, CLOSE_DOOR, IDLE)
- `LiftController` interface for pluggable controller strategies
- `NaiveLiftController` implementation (services nearest request first)
- `SimpleLiftController` alternative basic implementation
- Support for `CarCall` (requests from inside the lift)
- Support for `HallCall` (requests from a floor with direction)
- `Direction` enum (UP, DOWN, IDLE)
- `DoorState` enum (OPEN, CLOSED)
- Console-based demo in `Main.java`
- Unit tests for `SimulationEngine` and `NaiveLiftController`
- Maven build configuration with Java 17 and JUnit 5
- Basic project documentation (README, CHANGELOG, ADR-0001)
- EditorConfig file for consistent code formatting across IDEs and platforms

### Design Decisions
- Chose tick-based simulation for predictable, testable time advancement
- Immutable state objects to avoid bugs from shared mutable state
- Separated controller logic from simulation engine for flexibility

[0.9.1]: https://github.com/manoj-bhaskaran/lift-simulator/compare/v0.9.0...v0.9.1
[0.6.0]: https://github.com/manoj-bhaskaran/lift-simulator/compare/v0.5.0...v0.6.0
[0.5.0]: https://github.com/manoj-bhaskaran/lift-simulator/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.4.0
[0.3.0]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.3.0
[0.2.9]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.2.9
[0.2.8]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.2.8
[0.2.7]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.2.7
[0.2.6]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.2.6
[0.2.5]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.2.5
[0.2.4]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.2.4
[0.2.3]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.2.3
[0.2.2]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.2.2
[0.2.1]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.2.1
[0.2.0]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.2.0
[0.1.3]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.1.3
[0.1.2]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.1.2
[0.1.1]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.1.1
[0.1.0]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.1.0
