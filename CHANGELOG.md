# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

Detail for releases **0.1.0 – 0.29.0** has been moved to
[docs/CHANGELOG-ARCHIVE.md](docs/CHANGELOG-ARCHIVE.md); a one-line-per-milestone
summary is kept under [Earlier history](#earlier-history).

> **Note on dates:** Entries are ordered newest-first by semantic version. The
> dates reflect when each entry was authored rather than a strict release
> sequence, so a few dates do not decrease monotonically alongside the version
> numbers.

## [Unreleased]

## [0.49.11] - 2026-06-08

### Changed
- **Changelog history archive**: Moved the detailed entries for releases 0.1.0–0.29.0 into a new `docs/CHANGELOG-ARCHIVE.md`, leaving a one-line-per-milestone summary under "Earlier history". Full history is preserved in the archive; the main changelog stays focused on recent and major releases.

## [0.49.10] - 2026-06-08

### Changed
- **Changelog style normalization**: Standardized entries on the Keep a Changelog category vocabulary and a single bullet style (bold lead-in titles for top-level changes), normalizing sections that mixed bold and plain bullets. Removed the boilerplate "Patch version bump" / "Updated version references" bullets that carried no user-facing information, and deleted the partial, dead footer compare-link block (which defined links only for `0.1.0`–`0.6.0` and linked none of the version headings).

## [0.49.9] - 2026-06-08

### Changed
- **Changelog de-duplication and ordering cleanup**: Merged the near-identical consecutive patch entries `0.41.2`/`0.41.3` (duplicate SPA index.html 404 fix) and `0.17.1`/`0.17.2` (duplicate directional-scan furthest-stop behavior) into single range entries matching the existing range pattern, and documented that entry dates reflect authoring order rather than a strict release sequence so the version/date ordering inversions no longer read as errors.

## [0.49.8] - 2026-06-08

### Changed
- **Changelog proportionality cleanup**: Condensed over-detailed mid-history entries for 0.42.0, 0.29.0, 0.27.0, 0.26.0, 0.25.0, 0.24.0, 0.22.0, 0.20.0, and 0.19.0 to one-line summaries with no more than five high-value bullets each, while preserving user-facing behavior notes and confirming no breaking-change or security notes were present in those entries.

## [0.49.7] - 2026-06-08

### Fixed
- **Changelog structure cleanup**: Relocated the empty `Unreleased` placeholder to the top of the changelog, migrated already-shipped entries into `0.49.6`, removed the phantom maintenance-version reference and outdated current-version claims, and normalized section headings to Keep a Changelog categories.

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

## [0.49.4] - 2026-06-08

### Changed
- **Troubleshooting guide extraction**: Moved the Quick Troubleshooting section and Database Troubleshooting content from `README.md` into a new `docs/TROUBLESHOOTING.md`; the README Quick Troubleshooting section is now a short list of the four most common issues with a link to the full guide.
- **Database backup extraction**: Moved the Database Backup and Restore section from `README.md` into a new `docs/DATABASE-BACKUP.md`; the README Database Setup section now links to the dedicated file.

## [0.49.3] - 2026-06-08

### Changed
- **Changelog cleanup**: Removed boilerplate `### Technical Details`, `### Design Decisions`, `### Documentation`, and `### Migration Notes` sub-sections from pre-0.30.0 entries (0.5.0–0.29.0); stripped redundant "Version bumped from X to Y" and "Frontend package version updated to X.Y.Z" bullets from entries 0.22.0–0.43.0.

## [0.49.2] - 2026-06-07

### Changed
- **Changelog maintenance**: Condensed selected historical entries from 0.30.0 through 0.46.0 by removing boilerplate implementation, benefit, documentation, and notes sub-sections while preserving user-facing changes and breaking-change summaries.

## [0.49.1] - 2026-06-07

### Changed
- **REST API reference extraction**: Moved the detailed REST API reference, batch input generator notes, simulation run workflow documentation, runtime configuration API, and health endpoint reference out of `README.md` into `docs/API.md`; the README now keeps a concise API summary with links to the dedicated reference and Swagger UI.

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
- **Validation feedback**: Show descriptive validation feedback in the Create Version modal when configuration JSON fails backend validation.
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
- **Versions anchor scroll**: Ensure the Versions anchor scroll runs after loading completes, including systems with zero versions

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


## Earlier history

Full detail for these pre-MVP releases is preserved in [docs/CHANGELOG-ARCHIVE.md](docs/CHANGELOG-ARCHIVE.md).

- **0.29.0** — React admin UI scaffold (Dashboard, Lift Systems, Config Validator, Health Check pages).
- **0.24.0 – 0.28.0** — JPA entities and Spring Data repositories, Lift System CRUD and versioning REST APIs, the configuration validation framework, and the publish/archive workflow with a runtime config API.
- **0.21.0 – 0.23.x** — Spring Boot backend skeleton, PostgreSQL connectivity, and the Flyway-managed `lift_simulator` schema.
- **0.13.0 – 0.20.0** — Directional/SCAN controller (implementation, integration, scenario tests), selectable controller strategy, configurable idle parking, and the scripted scenario runner.
- **0.5.0 – 0.12.x** — Request lifecycle model, door reopening window, request cancellation API, idle parking, the builder-based simulation engine, and CI/quality tooling.
- **0.1.0 – 0.4.0** — Tick-based engine foundation, immutable `LiftState`, the formal lift state machine, and deterministic timing.
