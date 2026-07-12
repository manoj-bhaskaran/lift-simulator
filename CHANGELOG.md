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

## [0.57.14] - 2026-07-12

### Changed
- **Workflow documentation split**: Split the oversized Workflows and Troubleshooting guide into focused workflow, configuration/schema, and troubleshooting documents. Added `docs/CONFIG-SCHEMA.md` for lift-configuration, scenario payload, and batch-input-generator reference material; restored `docs/TROUBLESHOOTING.md` as the single home for general setup and database diagnostics; refreshed inbound links in README, API, and frontend docs; and updated package metadata for the 0.57.14 pre-MVP patch release.

## [0.57.13] - 2026-07-12

### Changed
- **Simulation-run lifecycle cleanup**: Removed the dead lifecycle-transition pass-through methods from `SimulationRunService` and wired startup recovery directly to `RunLifecycleManager`, keeping lifecycle ownership in the dedicated manager while preserving run creation, lookup, and deletion behavior. Retargeted duplicated lifecycle unit coverage to `RunLifecycleManager`, kept the runtime log base suffix-free as `logs/application` so Logback continues producing `logs/application.log` and `logs/application-error.log`, tightened a CI whitespace gap, and repaired stale archive-relative documentation links. Updated README and package metadata for the 0.57.13 pre-MVP patch release.

## [0.57.12] - 2026-07-12

### Changed
- **Simulation execution split completed**: `SimulationRunner`'s tick loop is now a pure, dependency-free static method (config + scenario + cancellation check + progress callback in, `RunMetrics` out), unit-tested directly via new `SimulationRunnerTest` and `SimulationArtefactWriterTest` covering deterministic KPIs, same-floor flow skipping, mid-run cancellation, and all three terminal artefact statuses. Removed `SimulationRunExecutionService`'s test-only convenience constructor (tests now build `SimulationRunner`/`SimulationArtefactWriter` collaborators directly), and promoted the internal `CancellationToken` to a top-level type so `SimulationRunner` no longer references `SimulationRunExecutionService`. No behaviour change. Updated package metadata for the 0.57.12 pre-MVP patch release.

## [0.57.11] - 2026-07-12

### Fixed
- **Cascade artefact cleanup best-effort**: Scenario and lift-system cascade deletes now match completed-run deletion semantics by treating checked and unchecked artefact cleanup failures as best-effort post-commit work. Cleanup failures are logged with the run id and artefact path instead of surfacing an unreachable API error after the database delete has committed, and the obsolete `ArtefactDeletionException` handler/path has been retired. Updated README and package metadata for the 0.57.11 pre-MVP patch release.

## [0.57.10] - 2026-07-12

### Changed
- **Frontend page component decomposition**: Split the large scenario form and lift-system detail pages into focused React subcomponents for scenario basics, templates, JSON editing, simulation settings, validation results, lift-system headers/details, version creation, version filters, version cards, and pagination. Updated frontend documentation and package metadata for the 0.57.10 pre-MVP patch release.

## [0.57.9] - 2026-07-11

### Changed
- **Frontend simulation page decomposition**: Split the simulator setup, run-status, and results rendering out of the large `Simulator.jsx` and `SimulationRunDetail.jsx` pages into reusable simulation-run components, with shared result-formatting helpers for KPIs, artefact sizes, and per-lift metrics. Updated frontend documentation and package metadata for the 0.57.9 pre-MVP patch release.

## [0.57.8] - 2026-07-11

### Changed
- **Simulation execution service split**: Refactored the admin asynchronous execution path so `SimulationRunExecutionService` owns queueing, cancellation, executor shutdown, and in-flight task tracking, while the new `SimulationRunner` owns validation, lifecycle transitions, progress updates, and simulation ticking, and the new `SimulationArtefactWriter` owns run logs, config/scenario snapshots, generated batch input, and `results.json` writing. This keeps the public simulation-run API unchanged while isolating runner and filesystem responsibilities for easier maintenance. The rejection path now also removes any pre-created cancellation token when a run cannot be queued, preventing token retention for over-capacity submissions. Updated README and package metadata for the 0.57.8 pre-MVP patch release.

## [0.57.7] - 2026-07-11

### Changed
- **API/README documentation slimming**: Rewrote `docs/API.md` (1030 → 220 lines) as an API *conventions* reference — authentication (HTTP Basic + API key), role-based access control, URL versioning, rate limiting, request/artefact size limits, response conventions (201 + `Location`, 202, pagination/sorting), and the shared `ErrorResponse`/`ValidationErrorResponse` shape — with the generated SpringDoc OpenAPI spec (Swagger UI / `api-docs`) promoted as the authoritative, always-current endpoint reference instead of hand-maintained per-endpoint schemas. Relocated the consumer-relevant reference material that the generated spec does not present as tables — the lift-configuration schema and validation rules (with the 0.46.0 floor-range migration note), the scenario payload schema, and the batch-input-generator format — into a new "Configuration and Scenario Schema Reference" section of `docs/Workflows-and-Troubleshooting.md`. Condensed the root `README.md` (434 → 336 lines): merged Rate Limiting and Request/Artefact Size Limits into a single summary linking to `docs/API.md`, shortened the Logging and Testing sections to summaries that link to the authoritative docs, and left Quick Start intact. Slimmed `frontend/README.md` (670 → 308 lines) to frontend-specific setup, scripts, testing methodology, and type-checking, replacing the long CI/CD and deployment sections with pointers to the root README and CI workflows. Fixed a stale Table-of-Contents anchor and verified zero broken internal markdown links in the edited files. Documentation-only; synchronised package metadata for the 0.57.7 pre-MVP patch release.

## [0.57.6] - 2026-07-11

### Changed
- **CI pipeline deduplication and CodeQL**: The `backend` job now uploads the packaged JAR as a build artifact and the `e2e-playwright` job downloads it instead of rebuilding it (`mvn -Pfrontend package -DskipTests` no longer runs twice per pipeline run), removing the Maven/JDK-build overhead from the E2E job's setup while keeping the Java runtime needed to start the packaged application. The duplicated PostgreSQL schema-creation and packaged-JAR asset-verification script blocks are now shared scripts, `scripts/ci-create-test-schema.sh` and `scripts/ci-verify-jar-assets.sh`, called from both jobs instead of copy-pasted inline. Removed the redundant standalone `mvn clean compile` step in `backend`, since `mvn verify` already compiles. Added `.github/workflows/codeql.yml` running CodeQL static analysis for `java-kotlin` and `javascript-typescript` on pull requests to `main` and weekly. Updated `README.md` and `frontend/README.md` CI sections to match the new job shape.

## [0.57.5] - 2026-07-11

### Changed
- **Frontend polling/utils consolidation and credential guard**: Added a shared `useRunPolling` hook (`frontend/src/hooks/useRunPolling.js`) encapsulating interval lifecycle, terminal-status (`SUCCEEDED`/`FAILED`/`CANCELLED`) stop conditions, and unmount cleanup, and migrated the five duplicated `setInterval` polling blocks in `Simulator.jsx`, `SimulationRuns.jsx`, and `SimulationRunDetail.jsx` onto it. Moved duplicated status-badge and date/duration formatting helpers into `utils/statusUtils.js` and deleted the page-local copies. `api/client.js` now exports the single-source-of-truth `apiBaseUrl`/`normalizedApiBaseUrl`, removing the re-derivation in `Simulator.jsx` and `SimulationRunDetail.jsx`. `vite.config.js` now fails a production-mode build if `VITE_ADMIN_PASSWORD` or `VITE_API_KEY` is set, since `VITE_*` variables are embedded in the compiled bundle; `frontend/README.md` documents the guard and the risk. Updated affected unit tests and added focused coverage for the new hook.

## [0.57.4] - 2026-07-11

### Changed
- **Simulation-run lifecycle centralization**: Added a dedicated `RunLifecycleManager` as the single owner for simulation-run configuration, status transitions, progress updates, and startup recovery. `SimulationRunService` and `SimulationRunExecutionService` now delegate lifecycle writes through it, reducing drift risk between admin and async execution paths. Completed-run deletion still removes database rows before artefacts, but post-commit artefact deletion is now documented and implemented as best-effort: checked and unchecked cleanup failures are logged with the run id and artefact path without failing an already-committed delete request. Updated API/README/developer documentation and Maven/npm metadata for the 0.57.4 pre-MVP patch release.

## [0.57.3] - 2026-07-11

### Changed
- **Documentation consolidation and archival**: Archived 8 historical incident fix write-ups (`CI-CD-FIX-SUMMARY`, `GITHUB-CI-CD-FIX`, `CI-CD-IMPLEMENTATION-CHECKLIST`, and 5 integration test fixes) with point-in-time banners to `docs/archive/` and fixed or neutralized their broken external links. Moved the superseded `testquality-evaluation.md` to archive with a pointer from ADR-0015. Merged `docs/TROUBLESHOOTING.md` into `docs/Workflows-and-Troubleshooting.md` as a single consolidated troubleshooting home, updated all inbound links in `README.md` and `docs/` files, and verified zero broken relative links in all markdown. No functional changes; documentation-only improvements prior to planned API.md/README slimming.

## [0.57.2] - 2026-07-11

### Fixed
- **Repository hygiene and logging cleanup**: Untracked generated `run-artefacts/` outputs, removed the unused simple lift controller, ad-hoc one-test PowerShell runner, unused H2 test dependency, and no-op Checkstyle gate/configuration. Runtime logging now keeps the Logback base at `logs/application` so the effective files remain `logs/application.log` and `logs/application-error.log`, and engine warnings flow through SLF4J/Logback. Updated README/developer docs and package metadata for the 0.57.2 pre-MVP patch release.

## [0.57.1] - 2026-07-10

### Fixed
- **Version synchronization tooling**: Added `scripts/sync-versions.sh` as the single hook/CI implementation for syncing the Maven version into README current-version text, README/docs/frontend JAR filename examples, and frontend package metadata. The pre-commit hook now delegates to the script using the staged `pom.xml` blob, CI verifies with the script's `--check` mode, stale README JAR references were refreshed, and Maven no longer mutates tracked README/docs files during `generate-resources`. Updated package metadata for the 0.57.1 pre-MVP patch release.

## [0.57.0] - 2026-07-10

### Changed
- **Spring Boot 4.1.0 upgrade (completes the 3.4.5 → 4.1.0 sequence)**: Bumped the Maven parent from 4.0.7 to 4.1.0, moving the backend baseline to Spring Framework 7.0, Spring Security 7.1, Hibernate ORM 7.4, Jackson 3.1, and Flyway 12.4. Removed the now-redundant explicit Testcontainers BOM import (Spring Boot 4.1 dependency management supplies Testcontainers 2.0.5) and confirmed no 4.0-deprecated bridges remain (no `spring-boot-starter-web` alias, no Jackson 2 defaults toggle, no properties migrator). Replaced the Framework 7-deprecated `HttpStatus.PAYLOAD_TOO_LARGE` constant with `CONTENT_TOO_LARGE` (still HTTP 413; the JSON error body is unchanged) and the Jackson 3.1-deprecated `JsonNode.asText()` test usages with `asString()` for a deprecation-clean compile. Moved CI from JDK 17 to JDK 21 LTS while keeping the compiled bytecode at Java 17 (`maven.compiler.release=17`), so Java 17 runtimes remain supported. Verified Flyway 12.4 migrations against PostgreSQL via Testcontainers, behavioural smoke tests on a running instance (HTTP Basic + ADMIN/VIEWER RBAC, API-key auth, actuator behind ADMIN, Swagger UI, SPA forwarding, rate limiting, request-size caps, CORS preflight, and Framework 7 strict trailing-slash path matching), and an end-to-end simulation run (system → version → scenario → run → results/log artefacts). Refreshed README/architecture/testing docs with the new dependency baseline table for the 0.57.0 pre-MVP minor release.

## [0.56.0] - 2026-07-08

### Changed
- **Jackson 3 migration (test sources)**: Migrated backend test Jackson core/databind imports to the `tools.jackson` packages, kept annotations on `com.fasterxml.jackson.annotation`, and added JSON contract regression assertions for strict unknown-property 400 responses, ISO-8601 timestamp serialization, JSONB semantic round trips, and simulation result/metrics payload shape. Updated README and package metadata for the 0.56.0 pre-MVP minor release.

## [0.55.0] - 2026-07-07

### Changed
- **Jackson 3 migration (main sources)**: Migrated all main-source Jackson usages from the `com.fasterxml.jackson.databind`/`core` packages to Jackson 3's `tools.jackson.databind`/`core` packages (annotations remain under `com.fasterxml.jackson.annotation`), as required by the Spring Boot 4 default of Jackson 3. Reworked `JacksonConfiguration` from `Jackson2ObjectMapperBuilderCustomizer` to a `JsonMapper.Builder` bean seeded with a `JsonFactory` that enforces the existing stream-read constraints (max nesting depth 100, max string length 1 MiB) and re-asserts `FAIL_ON_UNKNOWN_PROPERTIES`, while re-applying every auto-configured `JsonMapperBuilderCustomizer` so Spring Boot defaults are preserved. Audited Jackson exception handling for Jackson 3's unchecked `JacksonException` (formerly checked `JsonProcessingException`) so JSON parse and unknown-property failures still surface as clean 400s and unreadable simulation results still surface as 500s. Removed now-redundant `JavaTimeModule` registrations and the `WRITE_DATES_AS_TIMESTAMPS` toggle (java.time support and ISO-8601 date output are Jackson 3 core defaults), and replaced `ObjectMapper.copy()` with `rebuild().build()`. The JSON contract — strict unknown-property rejection, JSONB round-trips, and the authentication/authorization error-body shape — is unchanged. Updated ADR-0013 and package metadata for the 0.55.0 pre-MVP minor release.

## [0.54.0] - 2026-07-07

### Changed
- **Spring Boot 4 build migration**: Upgraded the Maven parent to Spring Boot 4.0.7, renamed the MVC starter/test starter dependencies, added the modular Data JPA test starter for repository slices, moved springdoc to 3.0.3, and let Spring Boot dependency management provide JUnit 6 and Testcontainers 2. Updated Boot 4 test-slice imports, Testcontainers PostgreSQL package imports, README dependency notes, and package metadata for the 0.54.0 pre-MVP minor release.


## [0.53.9] - 2026-07-06

### Changed
- **Spring Boot 3.5 baseline refresh**: Upgraded the Maven Spring Boot parent from 3.4.5 to 3.5.15 and added the temporary Spring Boot properties migrator runtime dependency so renamed or removed configuration keys are reported during the 3.5.x transition. Updated README and package metadata for the 0.53.9 patch release.

## [0.53.8] - 2026-07-06

### Changed
- **Spring Boot 4 compatibility pre-work**: Replaced deprecated Spring Boot test mocking, Spring Security MVC request matcher, and Hibernate generated-value APIs with replacements already available on the current 3.4.x baseline. Runtime and simulation-run API-key paths, configurable CSRF ignored paths, and generated timestamp semantics are unchanged. Updated README, ADR notes, and package metadata for the 0.53.8 patch release.

## [0.53.7] - 2026-06-27

### Changed
- **Runtime simulation path removal**: Removed the no-op external-process runtime simulation endpoint (`POST /api/v1/runtime/systems/{systemKey}/simulate`), its `LocalSimulationMain` child-process launcher, and the unused frontend API wrapper while keeping simulation-run artefact generation such as `input.scenario` unchanged. Updated README/API/workflow documentation and package metadata for the 0.53.7 patch release.

## [0.53.6] - 2026-06-27

### Fixed
- **Scenario deletion impact and artefact cleanup**: Scenario responses and a dedicated run-count endpoint now expose associated simulation-run counts, the Scenarios UI warns when deletion will remove run history and artefacts, and backend deletion write-locks the scenario before capturing cascade-removed runs so their artefact directories are reliably cleaned after commit. Updated README and package metadata for the 0.53.6 patch release.

## [0.53.5] - 2026-06-26

### Fixed
- **Consistent JSONB normalization**: Lift system version configuration writes now use the same Jackson re-serialization policy as scenarios before persisting to JSONB with strict trailing-token rejection, while the JPA verification runner compares JSONB round trips semantically instead of requiring byte-for-byte string equality. Updated README and package metadata for the 0.53.5 patch release.

## [0.53.4] - 2026-06-26

### Fixed
- **Simulation run detail DTO transaction safety**: The by-id simulation-run endpoint now builds its response inside the service read transaction with the run relationships eagerly loaded, and the detail payload includes related lift-system, version, and scenario display fields to keep future DTO expansion safe from lazy-loading failures. Updated README and package metadata for the 0.53.4 patch release.

## [0.53.3] - 2026-06-23

### Fixed
- **Simulation run execution IO correctness**: Avoided reopening `run.log` while handling in-process cancellation, skipped same-floor passenger flows when generating downloadable `input.scenario` artefacts so CLI reproduction matches execution, and removed the discarded progress reload after direct tick updates. Updated README and package metadata for the 0.53.3 patch release.

## [0.53.2] - 2026-06-23

### Changed
- **Dependabot dependency monitoring**: Removed the OWASP Dependency-Check Maven plugin and its non-blocking CI scan because NVD database downloads can hang in CI. Added weekly Dependabot updates for Maven, frontend npm packages, and GitHub Actions, and refreshed README/package metadata for the 0.53.2 patch release.


## [0.53.1] - 2026-06-23

### Fixed
- **API contract polish**: Preserved duplicate bean-validation messages with `fieldErrors` arrays, added browser-readable `Location` headers to create endpoints through CORS, changed runtime simulate launch responses to HTTP 202 Accepted, and removed the unused one-argument lift-system response factory. Updated README/API documentation and package metadata for the 0.53.1 patch release.


## [0.53.0] - 2026-06-23

### Changed
- **Simulation run version identifier alignment**: Changed `POST /api/v1/simulation-runs` to accept `versionNumber` scoped to `liftSystemId` instead of the surrogate `versionId`, and updated simulation run status/result payloads, frontend simulator navigation, README/API/workflow documentation, and package metadata for the 0.53.0 minor release.


### Fixed
- **Simulation log tail validation**: Added controller-level validation for `GET /api/v1/simulation-runs/{id}/logs?tail=N` so non-positive or greater-than-10,000 tail values return HTTP 400 before log access, and added global `ConstraintViolationException` handling so request-parameter constraint failures no longer fall through as HTTP 500 responses. Updated README/API documentation and package metadata for the 0.52.20 patch release.
- **Simulation run sort allowlist**: Restricted `GET /api/v1/simulation-runs` sorting to documented properties (`createdAt`, `startedAt`, `endedAt`, `status`, and `id`) and now returns HTTP 400 for unsupported sort fields or ignore-case sort modifiers instead of allowing Spring Data property lookup or non-string `lower(...)` failures to surface as HTTP 500. Updated README/API documentation and package metadata for the 0.52.19 patch release.
- **Directional-scan same-floor reversal**: Changed DIRECTIONAL_SCAN reversal checks to ignore opposite-direction turnaround calls ahead when an opposite-direction hall call is already waiting at the current floor, preventing unnecessary overshoot while preserving turnaround-floor service for opposite-direction calls above or below. Updated README and package metadata for the 0.52.18 patch release.
- **Passenger-count KPIs**: Added `passengersServed` and `passengersCancelled` KPI fields that correctly sum the passenger count carried on each `LiftRequest`, distinct from the existing `pickupRequestsServed`/`pickupRequestsCancelled` request-lifecycle counts. Added `passengerCount` field to `LiftRequest` (default 1, exposed via `getPassengerCount()`). Extended the scenario `hall_call` event format to accept an optional `passengers` field so CLI scenarios can represent multi-passenger hall calls with correct KPI accounting. Updated frontend KPI label maps, API/workflow documentation, and tests.
- **Pickup-latency KPI semantics**: Renamed simulation result KPI fields and UI labels to make clear that the current single-lift MVP reports pickup-request completions, wait-to-pickup latency, and pickup-leg utilisation only; passenger destination travel remains represented as per-floor demand rather than simulated delivery. Updated API/workflow documentation, and kept the UI compatible with stored `results.json` artefacts that still use the legacy `utilisation` field.
- **Post-1.0 polish hardening**: Restricted simulation artefact reads to the canonical `run.log` and `results.json` writer outputs, rejected negative log `tail` requests with HTTP 400, switched Dashboard quick actions to client-side React Router links, reused shared API error formatting in Config Validator, added client-side `durationTicks >= 1` validation before scenario validation/save, refreshed JAR examples in developer/workflow docs, and synchronized package metadata for the 0.52.17 patch release.
- **Archived stale v1 readiness assessment**: Moved the outdated `docs/V1_READINESS_ASSESSMENT.md` into `docs/archive/` so the active documentation set no longer presents stale pre-1.0.0 status, version, Spring Boot, security, or API-readiness findings.
- **CI backend schema validation parity**: Changed the backend CI verify job to use Hibernate `ddl-auto=validate` instead of `update`, keeping CI aligned with the test profile and Flyway-owned schema management documented elsewhere. Updated README CI testing guidance.
- **Frontend credential exposure hardening**: Replaced raw Axios error console logging with sanitized API-error details that omit request config and auth headers, documented that Vite-bundled credentials are local-development conveniences only and must not be used as a hosted SPA security boundary, refreshed JAR examples in the developer/workflow docs, and synchronized package metadata for the 0.52.16 patch release.
- **DoS hardening for request and artefact reads**: Capped retained per-IP rate-limit buckets, added a configurable API request-body limit that rejects oversized JSON payloads with HTTP 413, constrained Jackson JSON nesting/string sizes, and bounded full simulation log/results artefact reads to prevent unbounded memory use. Updated README operational guidance for the new limits.
- **Private OpenAPI default**: The base `application.yml` and `SecurityConfig` code fallback now default `security.openapi.public-access` to private access (`false`), keeping Swagger UI and `/api-docs` ADMIN-only unless an environment or profile explicitly opts into public documentation access. Updated README guidance and synchronized package metadata for the 0.52.15 patch release.
- **Placeholder startup secrets**: Backend startup validation now rejects the template `CHANGE_ME` placeholder for the runtime API key and legacy admin password, preventing copied development templates from starting with predictable credentials. Updated README guidance and synchronized package metadata for the 0.52.14 patch release.
- **Friendly backend-unreachable errors**: Frontend API error formatting now maps network failures, Vite proxy gateway errors (`502`/`503`/`504`), connection refusals, and timeouts to a plain-language server-reachability message while keeping the original technical error in the console for debugging. Updated README and package metadata for the 0.52.13 patch release.
- **Frontend cold-start API timeouts**: The admin UI now retries one safe Axios read timeout automatically after a short delay, allowing first-use backend cold starts to keep existing page/action loading indicators visible instead of immediately surfacing a blocking error. Updated README and package metadata for the 0.52.12 patch release.
- **Lift system deletion blocked while runs are active**: Deleting a lift system is now rejected with HTTP 409 when any of its simulation runs is still active (`CREATED` or `RUNNING`), mirroring the existing scenario guard, so the database `ON DELETE CASCADE` can no longer remove a run row out from under an executing simulation thread. When deletion is allowed, the on-disk artefacts of the terminal runs that cascade away are removed after the delete transaction commits, preventing orphaned artefact directories. Updated README and package metadata for the 0.52.11 patch release.
- **Simulation run artefact deletion ordering**: Completed-run deletes now remove the database row first and defer artefact directory cleanup until after the delete transaction commits, preventing rollback or commit failures from permanently deleting files for a run row that remains in the database. Updated README and package metadata for the 0.52.10 patch release.
- **Duplicate scenario names**: Scenario names are now unique within a lift system version, matching the uniqueness already enforced for `lift_system.system_key` and `lift_system_version(lift_system_id, version_number)`. Added a Flyway migration (`V11`) that deduplicates any existing same-named scenarios and creates a unique index on `scenario(lift_system_version_id, name)`, plus service-layer pre-checks on create and update that return HTTP 409 with an actionable message before hitting the database constraint. Scenario copy now auto-resolves name collisions (e.g. `Copy of X (2)`) so the new constraint stays satisfied. Updated API documentation and package metadata for the 0.52.9 patch release.
- **Frontend white-screen recovery**: Added a top-level React error boundary with reload/dashboard recovery actions, guarded lift-system version configuration previews against malformed or already-parsed JSON, and made Config Validator error rendering tolerate missing `errors` arrays. Updated README and package metadata for the 0.52.8 patch release.

### Added
- **High-level architecture documentation**: Added `docs/architecture.md` (linked from the README) with a Mermaid component diagram and a simulation-run sequence diagram — plus a standalone `docs/architecture-diagram.mermaid` source — describing the major components (React admin UI, Spring Boot backend layers, simulation engine, PostgreSQL, artefact storage) and the key configuration-management and simulation-run flows. Also refreshed the README "Project Structure" section to match the current source layout.
- **Simulation Runs bulk actions**: Added multi-select checkboxes, select-all, guarded bulk action controls, confirmation dialogs, and per-operation outcome summaries so active runs can be cancelled together and completed runs can be deleted together from the Simulation Runs list. Updated README, Maven/JAR documentation, backend artifact metadata, and frontend package metadata for the 0.52.0 minor release.
- **API rate limiting**: Added configurable token-bucket rate limiting (Bucket4j) for admin and runtime API endpoints. Limits are enforced per client IP with separate thresholds for admin (`/api/v1/**`) and runtime/simulation-run paths. Requests exceeding the limit receive HTTP 429 with `Retry-After` and `X-RateLimit-*` response headers. All thresholds are configurable via `rate-limiting.*` properties. Rate limiting can be disabled entirely via `rate-limiting.enabled=false`.
- **Guided Create New Version form**: The Create New Version screen now defaults to a structured, guided form with a labelled input for every configuration parameter, inline help, and client-side validation that mirrors the backend constraints (minimum values, floor-range, home-floor-in-range, and door reopen window rules). The raw JSON editor is retained behind an **Advanced (JSON)** toggle for power users, and switching between modes preserves entered data where the JSON can be parsed. Added shared schema helpers and unit tests covering the form helpers and component.

### Changed
- **Condensed API reference**: Trimmed `docs/API.md` to remove duplication and internal implementation detail without dropping any consumer-facing information. The `results.json` schema, artefact directory structure, and scenario field table now each appear exactly once; the Batch Input Generator section no longer documents internal Java service mechanics (processing steps, programmatic usage, key features, and the worked conversion example) and instead describes the backwards-compatibility bridge with an input/output format summary; simulation run status values are consolidated into a single table next to the Create Run endpoint; the standalone configuration validation error block was removed in favour of the existing validation examples; and the API key generation guidance now shows a single OpenSSL example. Synchronised package metadata for the 0.52.1 patch release.

### Fixed
- **Simulation run validation ordering**: Validates stored configuration and scenario payloads before writing `config.json`, `scenario.json`, or generated `input.scenario` artefacts, preventing partial artefact sets and avoiding batch-input generation against invalid scenario fields. Updated README and package metadata for the 0.52.7 patch release.
- **Optimistic locking for concurrent state changes**: Added JPA optimistic-lock columns to simulation runs and lift-system versions, serialized version-number allocation and publish/archive operations with a lift-system write lock, and enforced a database-level single-published-version constraint. Publishing now clears the published flag when archiving older versions, preventing concurrent publishes, duplicate next-version allocation, and run lifecycle races from corrupting state. Updated README and package metadata for the 0.52.6 patch release.
- **Simulation run startup recovery**: Added application-ready reconciliation for orphaned simulation runs so rows left `RUNNING` by a JVM crash are marked `FAILED`, never-submitted `CREATED` rows are marked `CANCELLED`, and both states become deletable through the existing terminal-run delete flow. Updated README and synchronized package metadata for the 0.52.5 patch release.
- **CI quality/security gate enforcement**: Updated the backend CI workflow to run Maven `verify` so the JaCoCo line coverage gate and OWASP dependency CVE scan bound to that phase are enforced on pull requests. Set the coverage gate to a 50% baseline scoped to production business logic by excluding Spring wiring, DTO/entity/repository boilerplate, package descriptors, and CLI/application entrypoints, configured OWASP Dependency-Check to fail the build for dependency vulnerabilities at CVSS 7.0 or higher, upgraded Dependency-Check to 12.2.2, and wired it to read `NVD_API_KEY` with conservative NVD retry/delay settings plus a cached local NVD data directory in CI to avoid NVD API rate-limit failures. Refreshed README testing guidance and synchronized package metadata for the 0.52.4 patch release.
- **SQL/bind-parameter log leakage**: Changed the default Hibernate SQL and JDBC bind-parameter log levels in `logback-spring.xml` from `DEBUG`/`TRACE` to `WARN` so a default deployment no longer writes every persisted configuration/scenario value to `logs/application.log` in cleartext. Verbose SQL and bind-parameter tracing is now opt-in and scoped to the `dev` springProfile block; the `prod` block keeps an explicit WARN override so that a combined `prod,dev` activation cannot leave the verbose `dev` levels in effect. Synchronised package metadata for the 0.52.3 patch release.
- **Actuator endpoint disclosure**: Restricted `/actuator/**` to ADMIN-role HTTP Basic authentication, changed health detail exposure to `when-authorized`, and registered and included actuator paths in the admin rate-limiting bucket so operational health and info endpoints are no longer anonymously exposed or unthrottled. Updated README, developer/workflow/API/security documentation, and package metadata for the 0.52.2 patch release.

## [0.51.0] - 2026-06-14

### Added
- **Scenario copy across versions**: Added a validated scenario-copy API and Scenario list UI flow for copying existing passenger-flow scenarios to another Lift System Version. The backend validates the existing scenario JSON against the target version's floor constraints before persisting a new scenario record, and the UI surfaces success or validation failures from the copy action. Updated README, backend, and frontend package metadata for the 0.51.0 minor release.

## [0.50.1] - 2026-06-14

### Changed
- **API response DTO standardisation**: Replaced ad-hoc health and simulation-log map responses with typed DTOs, moved common error payloads into reusable DTO classes, aligned version controller methods on `ResponseEntity`, and removed the unused simulation-run start request DTO. Updated API documentation and synchronized package metadata for the 0.50.1 patch release.


## [0.50.0] - 2026-06-14

### Added
- **Delete completed simulation runs**: Added `DELETE /api/v1/simulation-runs/{id}` to remove a run record together with its artefact directory (generated input, logs, results). Only terminal runs (`SUCCEEDED`, `FAILED`, `CANCELLED`) can be deleted; in-progress runs return `409 Conflict` and unknown runs `404 Not Found`. Artefacts are removed before the database record so a file-system failure aborts cleanly. The Simulation Runs list and detail screens expose a guarded **Delete** action with a confirmation dialog and success/failure feedback. Synchronized metadata for the 0.50.0 release.

## [0.49.25] - 2026-06-14

### Fixed
- **Simulation results log navigation**: Removed the redundant `View logs` links from simulator result banners and run details, keeping log review within the existing results UI and avoiding unauthenticated direct log requests. Stabilized artefact download URL callbacks to clear React hook lint warnings, and updated README and package metadata for the 0.49.25 patch release.

## [0.49.24] - 2026-06-13

### Fixed
- **Simulation run async submission race**: Deferred simulation execution submission until after the create-and-start transaction commits, ensuring the async worker can see the persisted run before configuring artefacts or advancing ticks. Updated service coverage and synchronized README/JAR metadata for the 0.49.24 patch release.

## [0.49.23] - 2026-06-13

### Fixed
- **Simulation run failure recovery**: Hardened async run failure handling so lifecycle transition races fall back to a direct guarded database update for RUNNING runs instead of leaving them stuck indefinitely at 0.0% progress. Runs now also persist an initial 0-tick progress update immediately after starting execution, and README/JAR metadata was synchronized for the 0.49.23 patch release.

## [0.49.22] - 2026-06-12

### Fixed
- **Frontend API authentication headers**: The Axios client now reads Vite-provided admin Basic auth and runtime API-key credentials at construction time and sends `Authorization` and `X-API-Key` defaults with API requests. Updated setup documentation for `frontend/.env.local` and synchronized package metadata for the 0.49.22 patch release.

## [0.49.21] - 2026-06-12

### Fixed
- **Version configuration JSON guidance**: Replaced truncated configuration editor placeholders with complete valid JSON examples, added required-field/schema help and a schema documentation link beside the create/edit version fields, and refreshed API examples to avoid invalid ellipsis placeholders. Updated README, developer/workflow guides, and package metadata for the 0.49.21 patch release.

## [0.49.17–0.49.20] - 2026-06-12

### Added
- **Service- and controller-layer test coverage**: Added MockMvc coverage for scenario CRUD, runtime configuration, simulation-launch, and public health endpoints (API-key success/failure paths, validation errors, 405 handling), and service-layer coverage for scenario validation/storage, version-specific floor-range rules, artefact listing/download safety, and asynchronous run execution and progress persistence.

### Fixed
- **Artefact symlink escape protection**: Hardened artefact downloads and listings to reject or skip symbolic links that resolve outside the run artefact directory.
- **Fast execution cleanup race**: Removed completed run futures and cancellation tokens immediately after submission if a short run finishes before its future is tracked.

## [0.49.16] - 2026-06-12

### Fixed
- **Swagger/OpenAPI compatibility**: Upgraded `springdoc-openapi-starter-webmvc-ui` to `2.7.0` so Spring Boot 3.4.13 can generate `/api/v1/api-docs` without the removed `ControllerAdviceBean(Object)` constructor error, restoring Swagger UI at `/api/v1/swagger-ui.html` with the configured `basicAuth` and `apiKey` security schemes.

## [0.49.7–0.49.15] - 2026-06-08

### Changed
- **Documentation and changelog restructuring**: Extracted the CLI/UI workflow and troubleshooting guidance into `docs/Workflows-and-Troubleshooting.md`, deduplicated and reorganised the README into a single logical section order, and archived the detailed 0.1.0–0.29.0 history into `docs/CHANGELOG-ARCHIVE.md` (a one-line-per-milestone summary remains under "Earlier history"). Normalised the changelog to Keep a Changelog conventions — consistent category vocabulary and bullet style, range-merged duplicate entries, proportionality cleanup of over-detailed mid-history entries, and removal of boilerplate version-bump bullets. Also removed duplicate and low-value `NaiveLiftController` tests.

## [0.49.6] - 2026-06-08

### Added
- **Configurable OpenAPI/Swagger access**: Added `security.openapi.public-access` / `SECURITY_OPENAPI_PUBLIC_ACCESS` so Swagger UI and OpenAPI JSON can either remain public (default, preserving existing behavior) or require ADMIN-role HTTP Basic authentication.
- **CI coverage artifacts**: The backend CI job now uploads the JaCoCo HTML report from `target/site/jacoco/` on every run.
- **Testcontainers-backed integration tests**: Integration and repository tests now provision a throwaway `postgres:15-alpine` instance on demand via Testcontainers, so `mvn test` runs without a pre-existing PostgreSQL database (a running Docker daemon is the only prerequisite). A single container is shared across the suite via a globally-registered Spring `ContextCustomizerFactory`, covering both `@SpringBootTest` and `@DataJpaTest` slices.
- **Flyway migrations exercised by tests**: The test profile now runs the real `db/migration` scripts at startup with Hibernate in `validate` mode (instead of `ddl-auto: update`), so migration bugs and entity/schema drift are caught; `FlywayMigrationIntegrationTest` asserts the schema history is populated and migrated tables exist.
- **Unit tests for RunMetrics**: Added `RunMetricsTest` covering KPI computation (completed/cancelled counts, wait ticks, utilisation), per-floor flows and lift visits, per-lift config output, and `recordTerminalRequests` idempotency.

### Changed
- **Developer guide extraction**: Moved simulation engine internals, request modeling, lift state machine, and JPA entity/repository reference material from `README.md` into the new `docs/DEVELOPER-GUIDE.md`; the README now links to the dedicated developer reference.
- **YAML configuration cleanup**: Replaced the checked-in base `application.properties` and local override properties template with YAML equivalents, removed the hardcoded development profile activation, and documented explicit `SPRING_PROFILES_ACTIVE` requirements for development and production launches.
- **README configuration documentation**: Documented the single-lift-system-per-simulation-run architecture assumption, profile setup, YAML configuration files, current 0.49.6 package version, and configurable Swagger/OpenAPI access.
- **Simulation run performance optimization**: Replaced O(n²) log tail buffering with an `ArrayDeque` ring buffer, cached shared `RunMetrics` KPI values across result serializers, and avoided allocating passenger-flow tick maps for empty scenarios. Added large-file tail coverage for 12K and 100K line logs.
- **Backend dependency refresh**: Updated backend package metadata to 0.49.6, upgraded Spring Boot from 3.2.1 to 3.4.13, added the Spring Boot-managed Flyway PostgreSQL database module required by the newer Flyway baseline, and verified the PostgreSQL JDBC driver remains on the latest 42.7.11 release.
- **Live-backend Playwright E2E in CI**: Added a dedicated `e2e-playwright` GitHub Actions job that provisions PostgreSQL, packages and starts the backend, waits for `/api/v1/health`, and runs the browser suite against the live API (publishing the HTML report and failure artifacts). Added optional Playwright auth env vars for admin Basic and runtime API-key headers, and stabilised the tests and helpers (retry-safe data, backend response waits, current selectors, dev-proxy auth injection) so the suite exercises the live backend reliably.
- **Execution-service refactor**: Removed the lazy circular dependency between `SimulationRunService` and `SimulationRunExecutionService` (the execution service now goes through `SimulationRunRepository` directly), and extracted `RunMetrics`, `FloorMetrics`, and `RequestLifecycle` into a new `com.liftsimulator.admin.service.metrics` package — reducing the execution service from ~711 to ~450 lines with no public API or behaviour changes.

### Fixed
- **CI deployable JAR packaging**: GitHub Actions backend and E2E packaging steps now activate the Maven `frontend` profile, install a Vite-compatible Node.js 20.19.0 runtime, and verify the produced Spring Boot JAR contains and serves the React assets under `BOOT-INF/classes/static/`.
- **Unified artefact storage**: Consolidated artefact storage onto the single `simulation.artefacts.base-path` property (default `./simulation-runs`), removing the duplicate `simulation.runs.artefacts-root` key and the second run directory that orphaned artefacts; the execution service now writes everything to the path persisted by `SimulationRunService`, backed by new directory-integration coverage.
- **Robustness fixes**: Added a null check for `scenario.durationTicks()` to fail cleanly instead of throwing `NullPointerException`, removed the duplicate `SimulationRunService.getAllRuns()` method superseded by `getAllRunsWithDetails()`, and cleared managed child entities before cascade-delete assertions to avoid Hibernate 6.6 transient-reference flush errors.

## [0.49.1–0.49.5] - 2026-06-07

### Changed
- **Documentation extraction and changelog compaction**: Moved the REST API reference, batch-input generator notes, simulation-run workflow, and runtime/health endpoint documentation into `docs/API.md`, and the troubleshooting and database-backup guides into `docs/TROUBLESHOOTING.md` and `docs/DATABASE-BACKUP.md`; the README now keeps concise summaries that link to the dedicated references. Compacted the changelog by range-merging narrow patch series, folding patch fixes into their base minor entries, and removing boilerplate sub-sections and version-bump bullets.

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
- **Draft version validation**: ConfigEditor now sends validation requests as `{ config: "..." }` (raw string) rather than a parsed object, resolving the spurious "Unknown property 'floors' is not allowed" error when validating DRAFT versions; create and update flows now validate consistently.
- **Create Version modal**: Display the next version number (max existing + 1) prominently at the top of the form, and surface descriptive backend validation feedback inline when the configuration JSON fails validation.

## [0.41.1–0.41.3] - 2026-01-17

### Fixed
- Hardened SPA route forwarding: prevented recursive forwarding on `/index.html` (avoiding `StackOverflowError`), returned a helpful 404 when the index.html asset is missing instead of a noisy stack trace, and used explicit return types so Spring MVC applies the correct view/response handling.

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

## [0.36.1–0.36.3] - 2026-01-16

### Added
- Shared `Modal` component (and base CSS) centralising overlay, header, and focus/keyboard handling, with `AlertModal`/`ConfirmModal` rendering through it to reduce duplication.
- Frontend API client support for an environment-configured base URL and request timeout via Vite env variables (default Axios timeout 10s to prevent hanging requests).

### Changed
- **Auto-validate on save**: ConfigEditor now validates automatically before saving, blocking invalid configurations until errors are fixed; LiftSystems no longer re-throws after showing an `AlertModal` error, preventing duplicate error handling.

## [0.36.0] - 2026-01-16

### Added
- **Comprehensive Responsive Design**: Added mobile-first responsive styling across admin UI pages, forms, modals, tables, JSON editors, and navigation.
- **Mobile Navigation**: Added a hamburger menu and touch-friendly navigation for smaller screens.

### Changed
- README now documents responsive design capabilities.

## [0.35.1–0.35.2] - 2026-02-12

### Changed
- Routed the Manage Versions action to the versions section (instead of duplicating view-details navigation) and made the version details page scroll to the versions section when linked with a `#versions` anchor, ensuring the scroll runs after loading completes even for systems with zero versions.

## [0.35.0] - 2026-01-16

### Added
- **Strict Configuration Schema Validation**: Rejects unknown JSON fields across REST validation and CLI entry points, producing clear field-specific errors for typos and unsupported configuration data.
- **Comprehensive Test Coverage**: Added unknown-field rejection tests covering single, typo, multiple, and otherwise-valid payload scenarios.

### Changed
- `ConfigValidationService`, production/test ObjectMapper configuration, and `LocalSimulationMain` now enforce strict schema validation consistently.

## [0.34.1–0.34.2] - 2026-02-10

### Fixed
- Guard runtime simulator process tracking against PID reuse by removing entries only when the same process exits, avoid null dereference when detecting packaged JARs (path string check), and use executor `execute` to avoid ignored submit results.

## [0.34.0] - 2026-02-10

### Added
- Runtime simulation launcher now supports packaged Spring Boot JARs via `PropertiesLauncher` with `--loader.main`
- Process lifecycle management for runtime-launched simulators, including tracked PIDs, output logging, and graceful shutdown on service stop
- Runtime documentation for local vs packaged simulation launch assumptions

## [0.33.5] - 2026-01-15

### Fixed
- **Validation error handling**: Hardened `GlobalExceptionHandler` to handle both field-level (`FieldError`) and object-level (`ObjectError`, e.g. `@AssertTrue` on class methods) constraints by checking error type before casting — fixing a `ClassCastException` on object-level violations — and surfaced both, keyed by field/object name, in `ValidationErrorResponse`.

### Added
- **Validation tests**: Added a 10-case `GlobalExceptionHandlerValidationTest` covering field-level, object-level, and mixed constraint handling.

## [0.33.4] - 2026-01-13

### Added
- **Database Backup and Restore Documentation**: Added cross-platform PostgreSQL backup, restore, verification, and periodic restore-testing guidance to README.
- **ADR-0012**: Documented backup/restore strategy, tooling choices, automation integration, alternatives, and operational considerations.

## [0.33.1–0.33.3] - 2026-02-01

### Fixed
- Suppressed the SpotBugs `EI_EXPOSE_REP2` false-positive for Spring controller constructor injection using a compiler-safe suppression that restores build success.

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
