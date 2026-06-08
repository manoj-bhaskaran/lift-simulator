# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

> **Note on dates:** Entries are ordered newest-first by semantic version. The
> dates reflect when each entry was authored rather than a strict release
> sequence, so a few dates do not decrease monotonically alongside the version
> numbers.

## [Unreleased]

## [0.49.9] - 2026-06-08

### Changed
- **Changelog de-duplication and ordering cleanup**: Merged the near-identical consecutive patch entries `0.41.2`/`0.41.3` (duplicate SPA index.html 404 fix) and `0.17.1`/`0.17.2` (duplicate directional-scan furthest-stop behavior) into single range entries matching the existing range pattern, and documented that entry dates reflect authoring order rather than a strict release sequence so the version/date ordering inversions no longer read as errors.
- **Patch version bump**: Updated repository package metadata, frontend package metadata, README version references, and extracted documentation JAR examples from 0.49.8 to 0.49.9.

## [0.49.8] - 2026-06-08

### Changed
- **Changelog proportionality cleanup**: Condensed over-detailed mid-history entries for 0.42.0, 0.29.0, 0.27.0, 0.26.0, 0.25.0, 0.24.0, 0.22.0, 0.20.0, and 0.19.0 to one-line summaries with no more than five high-value bullets each, while preserving user-facing behavior notes and confirming no breaking-change or security notes were present in those entries.
- **Patch version bump**: Updated repository package metadata, frontend package metadata, README version references, and extracted documentation JAR examples from 0.49.7 to 0.49.8.

## [0.49.7] - 2026-06-08

### Fixed
- **Changelog structure cleanup**: Relocated the empty `Unreleased` placeholder to the top of the changelog, migrated already-shipped entries into `0.49.6`, removed the phantom maintenance-version reference and outdated current-version claims, and normalized section headings to Keep a Changelog categories.

### Changed
- **Patch version bump**: Updated repository package metadata, frontend package metadata, README examples, and extracted documentation references from 0.49.6 to 0.49.7.

## [0.49.6] - 2026-06-08

### Added
- **Configurable OpenAPI/Swagger access**: Added `security.openapi.public-access` / `SECURITY_OPENAPI_PUBLIC_ACCESS` so Swagger UI and OpenAPI JSON can either remain public (default, preserving existing behavior) or require ADMIN-role HTTP Basic authentication.
- **CI coverage artifacts**: The backend CI job now uploads the JaCoCo HTML report from `target/site/jacoco/` on every run.
- **Testcontainers-backed integration tests**: Integration and repository tests now provision a throwaway `postgres:15-alpine` instance on demand via Testcontainers, so `mvn test` runs without a pre-existing PostgreSQL database (a running Docker daemon is the only prerequisite). A single container is shared across the suite via a globally-registered Spring `ContextCustomizerFactory`, covering both `@SpringBootTest` and `@DataJpaTest` slices.
- **Flyway migrations exercised by tests**: The test profile now runs the real `db/migration` scripts at startup and Hibernate runs in `validate` mode (instead of `ddl-auto: update`), so migration bugs and entity/schema drift are caught by the test suite.
- **Migration verification test**: Added `FlywayMigrationIntegrationTest`, which asserts the Flyway schema history is populated, all migrations succeeded, and migrated tables exist.
- **Unit tests for RunMetrics**: Added `RunMetricsTest` covering KPI computation (completed/cancelled counts, average and max wait ticks, utilisation), per-floor passenger flows and lift visits, per-lift config output, and idempotency of `recordTerminalRequests`.

### Changed
- **Developer guide extraction**: Moved simulation engine internals, request modeling, lift state machine, and JPA entity/repository reference material from `README.md` into the new `docs/DEVELOPER-GUIDE.md`; the README now links to the dedicated developer reference.
- **YAML configuration cleanup**: Replaced the checked-in base `application.properties` and local override properties template with YAML equivalents, removed the hardcoded development profile activation, and documented explicit `SPRING_PROFILES_ACTIVE` requirements for development and production launches.
- **README configuration documentation**: Documented the single-lift-system-per-simulation-run architecture assumption, profile setup, YAML configuration files, current 0.49.6 package version, and configurable Swagger/OpenAPI access.
- **Simulation run performance optimization**: Replaced O(n²) log tail buffering with an `ArrayDeque` ring buffer, cached shared `RunMetrics` KPI values across result serializers, and avoided allocating passenger-flow tick maps for empty scenarios. Added large-file tail coverage for 12K and 100K line logs.
- **Backend dependency refresh**: Updated backend package metadata to 0.49.6, upgraded Spring Boot from 3.2.1 to 3.4.13, added the Spring Boot-managed Flyway PostgreSQL database module required by the newer Flyway baseline, and verified the PostgreSQL JDBC driver remains on the latest 42.7.11 release.
- **CI Playwright E2E coverage**: Moved frontend Playwright execution into a dedicated `e2e-playwright` GitHub Actions job that provisions PostgreSQL, packages and starts the Spring Boot backend, waits for `/api/v1/health`, and runs the browser suite against the live API. The job publishes the Playwright HTML report and failure artifacts, including backend logs, so feature-test failures are visible in CI.
- **Playwright-only E2E auth configuration**: Added optional Playwright environment variables for admin Basic auth and runtime API-key headers so local and CI E2E runs can exercise authenticated backend endpoints without exposing credentials in browser bundles.
- **Backend-backed E2E stabilization**: Updated Playwright tests and helpers to use the inline Create Version validation flow, unique retry-safe system data, scoped assertions, backend response waits, route-aware system creation waits, current alert modal selectors, the current health-check UI payload, and Vite dev-proxy auth header injection so the new CI E2E job exercises the live backend reliably.
- **Decoupled simulation run execution wiring**: Removed the lazy circular dependency between `SimulationRunService` and `SimulationRunExecutionService`; the execution service now updates run lifecycle state and progress through `SimulationRunRepository` directly.
- **Extract RunMetrics to `metrics` sub-package**: Decomposed `SimulationRunExecutionService` by moving `RunMetrics`, `FloorMetrics`, and `RequestLifecycle` inner classes into a new `com.liftsimulator.admin.service.metrics` package. The execution service is reduced from ~711 lines to ~450 lines and the metrics classes are now independently testable. No public API or behaviour changes.
- **Patch version bump**: Updated package metadata, frontend package metadata, README references, and extracted API documentation references from 0.49.5 to 0.49.6.

### Fixed
- **Hibernate 6.6 cascade delete tests**: Cleared managed child entities before repository cascade-delete assertions so tests rely on the PostgreSQL `ON DELETE CASCADE` constraints without Hibernate transient-reference flush errors.
- **CI deployable JAR packaging**: GitHub Actions backend and E2E packaging steps now activate the Maven `frontend` profile, log profile activation, install a Vite-compatible Node.js 20.19.0 runtime, verify the produced Spring Boot JAR contains React assets under `BOOT-INF/classes/static/`, and confirm the packaged app serves the React root page during E2E startup.
- **NPE prevention in scenario execution**: Added null check for `scenario.durationTicks()` in `SimulationRunExecutionService.runSimulation()`. The method now fails cleanly with a clear error message if the scenario has a null duration, preventing NullPointerException during unboxing to primitive int.
- **Removed dead code**: Eliminated duplicate `SimulationRunService.getAllRuns()` method which was superseded by the more efficient `getAllRunsWithDetails()` that eagerly loads related entities (lift system, version, scenario).
- **Unified artefact path configuration**: Removed the duplicate `simulation.runs.artefacts-root` config key from `SimulationRunExecutionService`. All artefact storage now uses the single `simulation.artefacts.base-path` property (default: `./simulation-runs`) that was already present in `application.properties`.
- **Directory orphaning eliminated**: `SimulationRunExecutionService.executeRun()` no longer creates a second run directory and overwrites the persisted path. The execution service now reads `artefactBasePath` from the run entity (set once by `SimulationRunService`) and writes all artefacts to that directory.
- **Artefact storage documentation and tests**: Added `simulation.artefacts.base-path` to `application-dev.yml.template`, updated README documentation for the single artefact storage configuration key, and added `SimulationRunDirectoryIntegrationTest` coverage verifying that exactly one artefact directory is created per run and that it matches the persisted `artefactBasePath`.

## [0.49.5] - 2026-06-08

### Changed
- **Changelog compaction**: Collapsed narrow historical patch series into range entries, folded related patch fixes into their base minor entries, and replaced verbose test-case/method listings with concise test-suite scope summaries.
- **Patch version bump**: Updated version references from 0.49.4 to 0.49.5 across package metadata, README, and extracted API documentation.

## [0.49.4] - 2026-06-08

### Changed
- **Troubleshooting guide extraction**: Moved the Quick Troubleshooting section and Database Troubleshooting content from `README.md` into a new `docs/TROUBLESHOOTING.md`; the README Quick Troubleshooting section is now a short list of the four most common issues with a link to the full guide.
- **Database backup extraction**: Moved the Database Backup and Restore section from `README.md` into a new `docs/DATABASE-BACKUP.md`; the README Database Setup section now links to the dedicated file.
- **Patch version bump**: Updated version references from 0.49.3 to 0.49.4.

## [0.49.3] - 2026-06-08

### Changed
- **Changelog cleanup**: Removed boilerplate `### Technical Details`, `### Design Decisions`, `### Documentation`, and `### Migration Notes` sub-sections from pre-0.30.0 entries (0.5.0–0.29.0); stripped redundant "Version bumped from X to Y" and "Frontend package version updated to X.Y.Z" bullets from entries 0.22.0–0.43.0.

## [0.49.2] - 2026-06-07

### Changed
- **Changelog maintenance**: Condensed selected historical entries from 0.30.0 through 0.46.0 by removing boilerplate implementation, benefit, documentation, and notes sub-sections while preserving user-facing changes and breaking-change summaries.
- **Patch version bump**: Updated repository package metadata and README version references from 0.49.1 to 0.49.2.

## [0.49.1] - 2026-06-07

### Changed
- **REST API reference extraction**: Moved the detailed REST API reference, batch input generator notes, simulation run workflow documentation, runtime configuration API, and health endpoint reference out of `README.md` into `docs/API.md`; the README now keeps a concise API summary with links to the dedicated reference and Swagger UI.
- **Patch version bump**: Updated repository package metadata and documentation from 0.48.0 to 0.49.1 so package versions sort after the latest documented 0.49.0 release.

## [0.49.0] - 2026-06-07

### Added
- **Frontend unit test suite**: Configured Vitest with jsdom and `@testing-library/react` for component and utility unit tests. Added 54 unit tests across `Modal`, `AlertModal`, `ConfirmModal`, `Layout` components and `errorHandlers`/`statusUtils` utilities covering happy paths and error cases. CI frontend job now runs `npm run test:unit` before the build step.

## [0.48.0] - 2026-06-07

### Added
- **Paginated simulation run list endpoint**: `GET /api/v1/simulation-runs` now accepts `?page=0&size=20&sort=createdAt,desc` query parameters and returns a Spring Data `Page` envelope with `content`, `totalElements`, and `totalPages`. Default page size is 20; the API enforces a maximum of 100 per request. Filter parameters (`systemId`, `status`) continue to work alongside pagination. The frontend API client (`simulationRunsApi.js`) is updated to pass page, size, and sort on every call.

## [0.47.0] - 2026-06-06

### Security
- **API Key Validation at Startup**: Application now throws `IllegalStateException` during startup if `api.auth.key` is not configured, preventing misconfiguration from being masked by silent request rejection
- **Secure API Key Comparison**: API key validation now uses SHA-256 hashing with constant-time comparison instead of plaintext equality checks, preventing timing attacks and credential leakage
- **Frontend dependencies**: Updated vite from 7.2.4 to 7.2.9, axios from 1.13.5 to 1.18.1, and react-router-dom from 7.12.0 to 7.14.2 to resolve multiple security vulnerabilities including arbitrary file read, prototype pollution, SSRF, credential leakage, and XSS issues
- **Backend dependencies**: Updated PostgreSQL driver from 42.7.3 to 42.7.11 to address unbounded PBKDF2 iterations vulnerability (CVE GHSA-98qh-xjc8-98pq)

### Changed
- Condensed the historical foundation notes for the pre-release baseline into a shorter summary for readability.

## [0.46.0] - 2026-02-01

### Added
- **OpenAPI/Swagger Documentation**: Added interactive Swagger UI at `/api/v1/swagger-ui.html` and OpenAPI JSON at `/api/v1/api-docs`, documenting authentication schemes and backend API metadata.
- **URL-Based API Versioning**: Introduced the `/api/v1` prefix for REST endpoints, frontend API calls, Vite proxying, security rules, tests, and API versioning documentation.
- **Spring Security Baseline**: Added HTTP Basic authentication for admin APIs, API-key authentication for runtime and simulation APIs, role-based access control, consistent 401/403 JSON errors, stateless sessions, CORS/CSRF policy configuration, and authentication test coverage.

### Changed
- **Breaking: All API endpoints now use `/api/v1` prefix**: API consumers must update base URLs from `/api` to `/api/v1`; runtime and simulation endpoints are protected by API-key authentication and admin endpoints require HTTP Basic credentials.
- Version bumped from 0.45.0 to 0.46.0.

### Security
- Added startup validation for required API keys, constant-time hashed API-key comparison, configurable CORS/CSRF defaults, and dependency updates for frontend and backend vulnerabilities.

## [0.45.0] - 2026-02-01

### Added
- **Scenario Builder UI**: Added scenario list, form/JSON editors, templates, passenger-flow builder, server-side validation, random seed support, navigation, and CRUD flows.
- **Simulator Run UI**: Added lift system/version run setup, reproducible seed input, polling status, results rendering, artefact downloads, CLI reproduction guidance, and published-version launch links.
- **Simulation Runs History**: Added a persistent runs list/detail experience with filtering, historical result review, navigation integration, and backend list support.
- **Scenario and simulation APIs**: Added scenario naming/list/delete support, scenario validation/storage, asynchronous simulation-run lifecycle APIs, artefact handling, structured results output, and batch-input generation for CLI compatibility.
- **Testing**: Added run lifecycle, batch-input golden-file, CLI compatibility, and Scenario Builder Playwright coverage, including Playwright E2E tests for scenario creation, JSON mode editing, and validation error display (4 test cases).

### Fixed
- Improved run artefact downloads, progress persistence, scenario filtering, Advanced JSON Mode persistence, template selection styling, random seed checkbox alignment, Create Scenario navigation, linked-data delete conflicts, frontend version display, SpotBugs warnings, scenario validation failures, H2 test compatibility, simulator preselection behavior, stale version responses, cascade deletion, README schema references, and Start Run double-click handling.

## [0.44.0] - 2026-01-20

### Changed
- **Breaking: Unified floor range configuration**: Replaced `floors` with `minFloor`/`maxFloor` across runtime config, validation, API examples, and UI placeholders to support basement levels.
- **Validation updates**: Enforced `maxFloor > minFloor`, ensured `homeFloor` stays within the configured floor range, and adjusted lift count warnings to use the derived floor count.
- **Database migration**: Added a data migration to convert stored JSON configs from `floors` to `minFloor`/`maxFloor`.
- **Test suite updates**: Refreshed backend and Playwright fixtures to align with the new floor range schema, including basement-capable configurations.

## [0.43.0] - 2026-01-19

### Added
- **Playwright UI Automation Framework**: Added Playwright configuration, smoke tests, E2E scripts, CI artifact handling, and documentation for frontend browser testing.
- **Automated UI Test Suite**: Converted critical flows into reusable Playwright tests with fixtures and cleanup helpers: Lift Systems CRUD (5 tests), Configuration Versions (6 tests), Config Validator (6 tests), Health Check (4 tests), and Dashboard (4 tests).

### Fixed
- Corrected the lift-systems navigation smoke test route to match the actual React routing.

## [0.42.0] - 2026-01-19

UI consistency release for configuration-version workflows, lift-system editing, footer version display, and related frontend/backend fixes.

- Added a package-driven footer version display and Manoj Bhaskaran copyright notice across the shared layout.
- Added lift-system metadata editing with a read-only system key, validation-aligned form constraints, detail refresh, and responsive modal UI.
- Standardized Create New Version and Edit Config flows by requiring explicit validation before creation and presenting split-pane validation results.
- Fixed user-facing UI/API behavior for simulator availability messaging, exact version search, version-count totals, dashboard labels, version-status sort order, and standalone configuration validation display.
- Fixed test, lint, compilation, and build blockers in lift-system services, config validation, create-version form handling, and HTML tag structure.

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

## [0.41.2–0.41.3] - 2026-01-17

### Fixed
- Return a helpful 404 response when the SPA index.html asset is missing, avoiding noisy stack traces when the frontend is not built.
- Ensure SPA forwarding uses explicit return types so Spring MVC applies the correct view/response handling.

## [0.41.1] - 2026-01-17

### Fixed
- **SPA Forwarding Recursion**: Prevented recursive forwarding when requesting `/index.html`, avoiding StackOverflowError logs during HTML route handling

## [0.41.0] - 2026-01-17

### Added
- **Frontend E2E Testing Infrastructure**: Added Playwright-based browser tests, E2E scripts, CI workflow integration, trace/video/screenshot artifacts, and documentation for running the suite locally and in CI.

### Security
- Restricted Playwright browser binaries and generated artifacts from source control.

### Changed
- README now documents the E2E testing workflow.

### Fixed
- Covered navigation and admin UI workflows with browser-level regression tests.

## [0.40.0] - 2026-01-17

### Added
- **Centralized Frontend Logging**: Added structured logging utilities with levels, timestamps, optional context, and environment-aware output for API calls, validation, user actions, and component lifecycle events.

### Changed
- README and frontend documentation now describe logging usage and configuration.

## [0.39.1] - 2026-01-19

### Added
- Frontend testing strategy documentation, including tooling recommendations, conventions, and example scenarios
- CI/CD documentation for frontend and backend workflows, plus local check commands
- Deployment guidance for Vercel/Netlify, AWS S3 + CloudFront, Docker, and Spring Boot integration
- CI status badge in the frontend README

### Changed
- GitHub Actions workflow now runs frontend install, lint, build, and test steps
- Frontend npm scripts now include `lint:fix` and `test` placeholders

## [0.39.0] - 2026-01-18

### Added
- **Comprehensive JSDoc Documentation**: Documented React components and shared utilities for better IDE assistance and developer onboarding.
- **Professional README Badges**: Added frontend README badges for Node.js, license, React, and Vite.
- **Maintenance Documentation**: Added dependency update, version synchronization, and security audit guidance.

### Changed
- Improved frontend README accuracy and organization by documenting semantic version ranges, deployment options, troubleshooting placement, and production build guidance.
- Fixed version documentation mismatches, clarified deployment recommendations, and added missing maintenance guidance.

## [0.38.1] - 2026-01-18

### Added
- Shared frontend utilities for status badge styling and API error handling
- Reusable VersionActions component for version action buttons

### Changed
- Refactored ConfigEditor, LiftSystemDetail, and LiftSystems to use shared status/error utilities

## [0.38.0] - 2026-01-17

### Added
- **In-App UI Feedback Modals**: Replaced blocking browser alert/confirm dialogs with accessible `ConfirmModal` and `AlertModal` components, including focus management, keyboard support, ARIA attributes, type-based styling, and non-blocking UI behavior.

### Changed
- Updated ConfigEditor, LiftSystems, and LiftSystemDetail to use the new modal components for confirmations and error messages.

## [0.37.0] - 2026-01-16

### Added
- Lift Systems search bar with client-side filtering by display name and system key
- Empty state message for search results with no matches

### Changed
- Lift Systems list now filters results case-insensitively based on the search query

## [0.36.3] - 2026-01-16

### Added
- ConfigEditor now auto-validates configuration on save attempt to prevent saving invalid configurations
- Validation errors are displayed immediately when attempting to save invalid configuration
- Invalid configurations are now blocked from being saved until errors are fixed

### Changed
- ConfigEditor save workflow improved: auto-runs validation before save if not already validated
- LiftSystems error handling improved: removed unnecessary error throw that could cause unexpected behavior

### Fixed
- ConfigEditor no longer allows saving invalid configurations without validation
- LiftSystems error handling no longer throws after displaying error in AlertModal, preventing duplicate error handling

## [0.36.2] - 2026-01-16

### Added
- Shared Modal component to centralize overlay, header, and focus/keyboard handling for modal variants
- Shared Modal CSS for consistent base modal styling across the UI

### Changed
- AlertModal and ConfirmModal now render via the shared Modal component to reduce duplication

## [0.36.1] - 2026-01-16

### Added
- Frontend API client now supports environment-configured base URL and request timeout via Vite env variables

### Changed
- Default Axios request timeout set to 10 seconds to prevent hanging requests

## [0.36.0] - 2026-01-16

### Added
- **Comprehensive Responsive Design**: Added mobile-first responsive styling across admin UI pages, forms, modals, tables, JSON editors, and navigation.
- **Mobile Navigation**: Added a hamburger menu and touch-friendly navigation for smaller screens.

### Changed
- README now documents responsive design capabilities.

## [0.35.2] - 2026-02-12

### Fixed
- Ensure the Versions anchor scroll runs after loading completes, including systems with zero versions
- Version bumped from 0.35.1 to 0.35.2

## [0.35.1] - 2026-02-12

### Changed
- Lift systems list now routes the Manage Versions action to the versions section instead of duplicating the view details navigation
- Admin UI version details page scrolls to the versions section when linked with a `#versions` anchor
- Documentation refreshed for updated version numbers and UI navigation guidance

## [0.35.0] - 2026-01-16

### Added
- **Strict Configuration Schema Validation**: Rejects unknown JSON fields across REST validation and CLI entry points, producing clear field-specific errors for typos and unsupported configuration data.
- **Comprehensive Test Coverage**: Added unknown-field rejection tests covering single, typo, multiple, and otherwise-valid payload scenarios.

### Changed
- `ConfigValidationService`, production/test ObjectMapper configuration, and `LocalSimulationMain` now enforce strict schema validation consistently.

## [0.34.2] - 2026-02-10

### Fixed
- Avoid null dereference warnings when detecting packaged JARs by using a path string check

## [0.34.1] - 2026-02-10

### Fixed
- Guard runtime simulator process tracking against PID reuse by removing entries only when the same process exits
- Avoid null dereference when detecting packaged JARs and use executor execute to avoid ignored submit results

## [0.34.0] - 2026-02-10

### Added
- Runtime simulation launcher now supports packaged Spring Boot JARs via `PropertiesLauncher` with `--loader.main`
- Process lifecycle management for runtime-launched simulators, including tracked PIDs, output logging, and graceful shutdown on service stop
- Runtime documentation for local vs packaged simulation launch assumptions

## [0.33.5] - 2026-01-15

### Fixed
- **Validation Error Handling**: Hardened GlobalExceptionHandler to safely handle both field-level and object-level validation constraints
  - Fixed ClassCastException when processing object-level constraint violations (e.g., @AssertTrue on class methods)
  - Updated handleValidationErrors method to check error type before casting
  - Field-level errors (FieldError) now use field name as key
  - Object-level errors (ObjectError) now use object name as key
  - Both error types are properly surfaced in ValidationErrorResponse

### Added
- **Comprehensive Validation Tests**: Added a 10-case `GlobalExceptionHandlerValidationTest` suite covering field-level, object-level, and mixed validation constraint handling.

### Changed
- GlobalExceptionHandler now imports ObjectError alongside FieldError
- Enhanced JavaDoc comments in GlobalExceptionHandler for validation error handling

## [0.33.4] - 2026-01-13

### Added
- **Database Backup and Restore Documentation**: Added cross-platform PostgreSQL backup, restore, verification, and periodic restore-testing guidance to README.
- **ADR-0012**: Documented backup/restore strategy, tooling choices, automation integration, alternatives, and operational considerations.

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
- Updated README and frontend docs for runtime launch workflow

## [0.32.1] - 2026-01-20

### Fixed
- SPA forwarding controller path pattern compatibility with Spring Boot 3 path matching

## [0.32.0] - 2026-01-20

### Added
- Frontend production bundling profile to build the React UI and include it in the Spring Boot JAR
- SPA route forwarding controller so client-side routes load correctly when served by Spring Boot

### Changed
- Updated README and frontend docs with clear dev vs production run instructions

## [0.31.0] - 2026-01-12

### Added
- **Configuration Editor UI**: Added a JSON editor page for lift system versions with save draft, validate, publish, status, read-only, breadcrumb, validation-result, and responsive layout support.
- **ConfigEditor Component**: Added the React component and styling for configuration editing.
- **Enhanced Version Actions**: Added edit/view configuration actions to version cards and grouped version controls more clearly.

### Changed
- README now documents the Configuration Editor feature.
- Routing and `LiftSystemDetail` now expose configuration edit and view flows.

## [0.30.0] - 2026-01-12

### Added
- **Lift System List and Detail Views**: Added CRUD-oriented lift-system management with create form validation, detail pages, delete confirmation, breadcrumbs, version management, publish/archive behavior, expandable configuration display, navigation, and responsive layout.
- **Create System Modal Component**: Added a reusable accessible modal for creating lift systems with client-side validation and error handling.
- **Routing and UI Components**: Added `/systems/:id` routing and updated lift-system React components to support detail navigation and creation flows.

### Changed
- README now documents lift-system CRUD and version-management capabilities.
- Lift Systems page actions now navigate to the detail view.

## [0.29.0] - 2026-01-12

React admin UI scaffold release introducing the first web interface for lift-system management and backend monitoring.

- Added a React 19/Vite frontend with client-side routing, shared layout, Axios API client, and local dev-server proxying.
- Added Dashboard, Lift Systems, Config Validator, and Health Check pages for common administrative workflows.
- Added interactive JSON validation and backend health-monitoring screens with live API feedback.
- Added frontend README coverage for setup, development workflow, API integration, troubleshooting, and production builds.
- Updated the main README and admin-interface docs to include frontend setup and overview material.

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
- **Comprehensive Test Coverage**: Added publish/archive workflow coverage and `RuntimeConfigServiceTest` with 6 unit tests for runtime API success paths plus system-not-found, no-published-version, and unpublished-version errors.
- **Documentation**: ADR-0010 for publish/archive workflow design decisions

### Changed
- `publishVersion()` method now archives previously published versions before publishing new version
- Publish workflow is transactional - if archiving or publishing fails, entire operation rolls back
- Updated service layer tests to verify archiving behavior

## [0.27.0] - 2026-01-11

Configuration-validation release enforcing structural, domain, and publish-time checks for lift-system version JSON.

- Added `LiftConfigDTO`, `ConfigValidationService`, and validation DTOs for typed structural errors, domain rules, and warnings.
- Added `POST /api/config/validate` for non-persistent configuration validation with structured errors and warnings.
- Added version publishing validation through `POST /api/lift-systems/{systemId}/versions/{versionNumber}/publish`, including 409 responses for already-published versions.
- Enforced validation during version create, update, and publish flows so invalid configurations return 400 responses with detailed validation results.
- Expanded exception handling and tests for malformed JSON, domain rules, warnings, publish conflicts, and create/update validation failures.

## [0.26.0] - 2026-01-11

Lift-system versioning API release adding lifecycle management for configuration versions.

- Added REST endpoints to create, update, list, and retrieve lift-system versions, including optional cloning from an existing version.
- Added `LiftSystemVersionService` with per-system auto-incrementing version numbers, existence checks, and transactional writes.
- Added typed request/response DTOs for version creation, configuration updates, and version responses.
- Added unit and integration coverage for version creation, cloning, updating, listing, retrieval, auto-incrementing, error cases, and validation failures.
- Updated the REST API behavior so lift-system configurations have full version lifecycle management starting at version number 1 per system.

## [0.25.0] - 2026-01-11

Lift-system CRUD API release adding backend lifecycle management for lift-system records.

- Added create, list, detail, update, and delete endpoints for lift systems, with cascade deletion of versions.
- Added `LiftSystemService` business logic for duplicate-key validation, audit timestamps, transactions, and error handling.
- Added typed request/response DTOs with Jakarta validation for lift-system create and update flows.
- Added global exception handling for 404, validation, and illegal-argument responses with structured error payloads.
- Added service/controller tests and fixed SpotBugs representation-exposure warnings with defensive copies and targeted suppression for Spring DI.

## [0.24.0] - 2026-01-11

JPA persistence release mapping lift systems, versions, JSONB configuration, and repository access.

- Added `LiftSystem` and `LiftSystemVersion` entities with timestamps, relationships, cascade operations, and version lifecycle helpers.
- Added Hibernate 6 JSONB mapping for lift configuration storage in PostgreSQL.
- Added Spring Data repositories with lookup, status, published-version, ordering, and max-version-number queries.
- Added repository integration tests using an H2 PostgreSQL-compatible test profile for CRUD, JSONB, relationship, and cascade behavior.
- Added an optional JPA verification runner for exercising entity CRUD, JSONB persistence, relationships, and custom repository queries.

## [0.23.1–0.23.6] - 2026-01-09

### Fixed
- Fixed Flyway migration filename conflicts, PostgreSQL `lift_simulator` schema targeting, and stale build artifact cleanup across six consecutive patch releases.

## [0.23.0] - 2026-01-09

### Added
- Create the initial lift configuration schema with Flyway migration `V1__init.sql`.
- Add the `lift_simulator` schema with `lift_system` and `lift_system_version` tables for versioned JSONB configurations.
- Include publish status fields, foreign keys, and indexes for lift system versions.

## [0.22.0] - 2026-01-09

PostgreSQL/Flyway integration release establishing persistent storage for the Lift Config Service backend.

- Added PostgreSQL connectivity with Spring Data JPA, the PostgreSQL JDBC driver, and default HikariCP pooling.
- Added Flyway migration management with a baseline schema migration and automatic migration tracking at startup.
- Added profile-based development configuration for local PostgreSQL credentials, connection pooling, SQL logging, and Hibernate validation.
- Added initial `schema_metadata` storage and ADR-0007 documenting the PostgreSQL/Flyway decision.
- Updated README database setup guidance and fixed Flyway module resolution plus development credential alignment across patch releases.

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

## [0.20.0] - 2026-01-09

Scenario-test release adding deterministic coverage for controller routing behavior across realistic multi-request flows.

- Added a reusable `ScenarioHarness` for tick simulation, timed request injection, service-event logging, and queue/service assertions.
- Added DirectionalScan scenario tests for documented examples, calls above/below the lift, reversals, batching, alternating directions, and service order.
- Added NaiveLift scenario tests for nearest-first routing, back-and-forth behavior, mixed call types, dynamic additions, and baseline requests.
- Added comparison coverage running identical scenarios against both strategies.
- Verified scenario tests complete all requests, clear queues, and prevent lost or duplicated servicing.

## [0.19.0] - 2026-01-09

Directional/SCAN controller release integrating direction-aware scheduling into the simulator lifecycle.

- Integrated `DirectionalScanLiftController` so new requests, floor arrivals, and door cycles update target stops without losing or duplicating requests.
- Added `--strategy=<strategy>` to the Main demo with `nearest-request` as the default and `directional-scan` as an alternative.
- Added end-to-end DirectionalScan integration tests for movement-time requests, hall-call scheduling, cancellation, service transitions, and out-of-service recovery.
- Documented controller strategy tradeoffs, examples, hall-call filtering, and direction commitment in the README.
- Updated Main help text and demo configuration docs for controller selection.

## [0.18.1] - 2026-01-12

### Fixed
- Scenario runner now prints request lifecycle summaries after execution

## [0.18.0] - 2026-01-12

### Added
- Demo output now includes a request lifecycle summary table showing created and completed/cancelled ticks for each request

## [0.17.1–0.17.2] - 2026-01-12

### Changed
- Directional scan controller keeps traveling to the furthest pending stop in the current direction before reversing, even when only opposite-direction hall calls remain

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

## [0.12.7–0.12.17] - 2026-01-25

### Added
- Added CI and quality tooling with build, coverage, style, static-analysis, packaging, Checkstyle, SpotBugs, and OWASP Dependency-Check coverage.

### Changed
- Improved CI and quality implementation by centralizing request lifecycle transitions, indexing naive-controller active requests, retaining dedicated request collections, and standardizing comment formatting.

### Fixed
- Fixed SpotBugs offline dependency resolution, guarded idle-timeout calculations, enforced scenario tick/event input limits, optimized naive-controller request handling, and improved scenario validation safety.

## [0.12.1–0.12.6] - 2026-01-14

### Added
- Added simulation engine package documentation, JaCoCo coverage enforcement, scenario parsing/execution integration tests, and a lift request ID generator reset hook for test isolation.

### Changed
- Refactored the simulation engine around builder defaults, named timing/sentinel constants, state-specific tick handlers, JavaDoc coverage, and lower-boilerplate sequence-based engine tests.

### Fixed
- Fixed scenario event tick validation and invalid action reporting so malformed scenario files and invalid engine actions fail with explicit messages.

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

### Fixed
- Corrected arrival and door timing by normalizing movement status and preserving `MOVING_*` state until controller decisions.
- Aligned out-of-service test flow with the required stop tick and ignored new request assignments while a lift is out of service.
- Avoided reopening doors during out-of-service shutdown when doors are already open or opening.

### Changed
- OUT_OF_SERVICE state (already existed since v0.2.0) now has full operational support
- Lifts in OUT_OF_SERVICE state cannot move, open doors, or accept new requests
- Demo simulation extended to 50 ticks to show complete out-of-service workflow

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

## [0.2.1–0.2.9] - 2026-01-11

### Added
- Added version display to demo output so runs show the active software version.

### Fixed
- Fixed door-state, direction, and state-machine guard bugs in the naive controller, including same-floor request cleanup, transitional door states, stop-before-open behavior, and door-opening/downward movement sequencing.
- Fixed build reliability by adding the missing `LiftStatus` test import, configuring the Maven Java release target, and setting filtered resource encoding.

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
- Initial simulator foundation release with the tick-based core engine and immutable `LiftState` model.
- Core simulation contracts and strategies: `LiftController`, `NaiveLiftController`, and `SimpleLiftController`.
- Initial request/action primitives: `Action`, `Direction`, `DoorState`, plus support for `CarCall` and `HallCall`.
- Developer baseline: console demo (`Main.java`), unit tests, Maven/Java 17 + JUnit 5 build setup, and starter docs (README, CHANGELOG, ADR-0001).

### Changed
- Chose tick-based simulation for predictable, testable time advancement.
- Used immutable state objects to reduce shared-mutable-state defects.
- Separated controller logic from the simulation engine for extensibility.

[0.6.0]: https://github.com/manoj-bhaskaran/lift-simulator/compare/v0.5.0...v0.6.0
[0.5.0]: https://github.com/manoj-bhaskaran/lift-simulator/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.4.0
[0.3.0]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.3.0
[0.2.0]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.2.0
[0.1.3]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.1.3
[0.1.2]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.1.2
[0.1.1]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.1.1
[0.1.0]: https://github.com/manoj-bhaskaran/lift-simulator/releases/tag/v0.1.0
