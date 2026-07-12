# Configuration and Scenario Schema Reference

This reference documents the payload schemas that clients construct when creating lift-system versions, authoring scenarios, and reproducing runs. The field constraints below are enforced by the backend validation framework and are surfaced in the generated OpenAPI spec; they are collected here as human-readable reference. For the API conventions that govern these payloads (auth, error shape, size limits), see [API Conventions](API.md). For operational run workflows, see [Workflows and Troubleshooting](Workflows-and-Troubleshooting.md).


### Lift Configuration Schema

All lift system configurations must conform to the following structure:

```json
{
  "minFloor": 0,
  "maxFloor": 9,
  "lifts": 1,
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

**Field constraints:**

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `minFloor` | Integer | Required | Minimum floor in the building (can be negative for basements) |
| `maxFloor` | Integer | > minFloor | Maximum floor in the building (must be greater than minFloor) |
| `lifts` | Integer | Exactly `1` | Number of lift cars. Multi-lift simulation is not supported in v1.0.0, so any value other than `1` is rejected with a validation error |
| `travelTicksPerFloor` | Integer | ≥ 1 | Ticks required to travel one floor |
| `doorTransitionTicks` | Integer | ≥ 1 | Ticks required for doors to open or close |
| `doorDwellTicks` | Integer | ≥ 1 | Ticks doors stay open before closing |
| `doorReopenWindowTicks` | Integer | ≥ 0, ≤ doorTransitionTicks | Window during door closing when doors can reopen |
| `homeFloor` | Integer | minFloor ≤ homeFloor ≤ maxFloor | Idle parking floor (must be within floor range) |
| `idleTimeoutTicks` | Integer | ≥ 0 | Ticks before idle parking behavior activates |
| `controllerStrategy` | Enum | NEAREST_REQUEST_ROUTING, DIRECTIONAL_SCAN | Controller algorithm |
| `idleParkingMode` | Enum | STAY_AT_CURRENT_FLOOR, PARK_TO_HOME_FLOOR | Idle parking behavior |

**Validation features:**

- **Structural validation:** ensures JSON is well-formed and all required fields are present.
- **Type validation:** validates field types and enum values.
- **Domain validation:** enforces business rules and cross-field constraints — `doorReopenWindowTicks` must not exceed `doorTransitionTicks`, `maxFloor` must be greater than `minFloor`, and `homeFloor` must be within the floor range.
- **Warnings** (non-blocking): low `doorDwellTicks`, low `idleTimeoutTicks` with `PARK_TO_HOME_FLOOR` mode, and zero `doorReopenWindowTicks` (which disables door reopening).

Validation runs automatically when creating a new version, updating a version's configuration, or publishing a version. If it fails with errors, the operation is rejected with a `400 Bad Request` using the `valid` / `errors` / `warnings` shape documented in [API Conventions](API.md#error-response-structure). To validate without persisting, `POST` the config to the validation endpoint (see the generated OpenAPI spec).

**Migration note (0.46.0 floor-range update):**

- Replace `floors` with explicit `minFloor` and `maxFloor`. For existing configs, set `minFloor` to `0` and `maxFloor` to `floors - 1`.
- Ensure `homeFloor` is within the new range (`minFloor` to `maxFloor`).
- Apply the Flyway migration `V2__migrate_floor_range_config.sql` to update stored configuration payloads.

### Scenario Payload Schema

Scenario payloads define passenger flow for UI-driven simulation runs. The scenario schema is separate from the batch `.scenario` files used by the CLI. A scenario JSON payload has the shape:

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

**Field constraints:**

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `durationTicks` | Integer | Required, ≥ 1 | Total number of ticks to simulate |
| `passengerFlows` | Array | Required, ≥ 1 entry | Passenger flow entries |
| `passengerFlows[].startTick` | Integer | ≥ 0, < durationTicks | Tick when passengers arrive |
| `passengerFlows[].originFloor` | Integer | Required | Origin floor for the flow |
| `passengerFlows[].destinationFloor` | Integer | Required, ≠ origin | Destination floor for the flow |
| `passengerFlows[].passengers` | Integer | Required, ≥ 1 | Number of passengers in the flow |
| `seed` | Integer | Optional, ≥ 0 | Random seed for deterministic runs |

Scenario names must be unique within a lift system version; a create or rename that collides returns `409 Conflict`. Deleting a scenario intentionally cascades its associated simulation-run history, and clients should warn users with the run count (available from the run-count endpoint) before deleting.

### Batch Input Generator Format

The batch input generator converts stored scenario definitions and lift configurations into the legacy `.scenario` file format used by the CLI simulator. It exists as a backwards-compatibility bridge so scenarios authored through the UI/API can be replayed by the existing CLI batch infrastructure without manual conversion. It runs internally as part of starting a simulation run; there is no standalone public endpoint.

| Direction | Format |
|-----------|--------|
| Input | Lift system version configuration plus scenario JSON (passenger flows) — see [Scenario Payload Schema](#scenario-payload-schema) for field constraints |
| Output | A `.scenario` file consumed by the CLI runner: a `key: value` configuration header followed by comma-delimited event rows of the form `<tick>, hall_call, <alias>, <originFloor>, <direction>`. See [CLI Usage](Workflows-and-Troubleshooting.md#cli-usage-unchanged) for running the generated file. |

Generated `input.scenario` artefacts skip same-floor passenger flows so CLI reproduction matches the in-process executor.

---
