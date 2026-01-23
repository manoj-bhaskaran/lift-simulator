# ADR-0017: Batch Input Generator for Backwards Compatibility

**Date**: 2026-01-23

**Status**: Accepted

## Context

The Lift Simulator has evolved from a CLI-based batch simulation tool to a web-based system with database-backed configuration management. However, the existing CLI simulator (`ScenarioRunnerMain`) and its `.scenario` file format remain the production-ready execution engine.

The application now has two distinct data models:
1. **Modern UI workflow**: Lift configurations stored as JSONB in `lift_system_version.config` and passenger flow scenarios in `simulation_scenario.scenario_json`
2. **Legacy CLI format**: Text-based `.scenario` files parsed by `ScenarioParser` with specific format requirements

To run UI-defined scenarios through the existing simulator without rewriting the execution engine, we need a **backwards-compatible wrapper** that converts the modern data model to the legacy batch input format.

### Key Design Questions

1. Should we rewrite the simulator to accept JSON input directly, or generate legacy `.scenario` files?
2. How do we ensure exact format compliance with `ScenarioParser`?
3. Where should generated files be stored?
4. How do we convert passenger flows (origin/destination) to simulator events (hall calls)?
5. How do we validate that generated files work with the existing simulator?

## Decision

We will implement a **BatchInputGenerator** service that generates `.scenario` files from database-stored configurations. The generator treats the batch input format as a **stable public contract** and ensures exact compliance with the existing `ScenarioParser`.

### Implementation Details

#### 1. Generator Service Architecture

**`BatchInputGenerator` service**:
- Takes `LiftConfigDTO` (from `LiftSystemVersion.config`) and `ScenarioDefinitionDTO` (from `SimulationScenario.scenario_json`)
- Generates `.scenario` file content as a string
- Writes files to run-specific artifact directories
- Validates floor ranges and tick constraints during generation

**Key method signatures**:
```java
public String generateScenarioContent(
    LiftConfigDTO liftConfig,
    ScenarioDefinitionDTO scenarioDefinition,
    String scenarioName
)

public void generateBatchInputFile(
    String liftConfigJson,
    ScenarioDefinitionDTO scenarioDefinition,
    String scenarioName,
    Path outputPath
)
```

#### 2. Format Compliance Strategy

The generator produces files in the **exact format** expected by `ScenarioParser`:

**Header section** (configuration parameters):
```
name: <scenario_name>
ticks: <total_simulation_ticks>
min_floor: <min_floor_number>
max_floor: <max_floor_number>
initial_floor: <starting_floor>
travel_ticks_per_floor: <ticks_per_floor>
door_transition_ticks: <ticks>
door_dwell_ticks: <ticks>
door_reopen_window_ticks: <ticks>
home_floor: <floor_number>
idle_timeout_ticks: <ticks>
controller_strategy: NEAREST_REQUEST_ROUTING | DIRECTIONAL_SCAN
idle_parking_mode: STAY_AT_CURRENT_FLOOR | PARK_TO_HOME_FLOOR
```

**Events section** (passenger flows converted to hall calls):
```
<tick>, hall_call, <alias>, <floor>, <UP|DOWN>
```

**Format guarantees**:
- All field names match `ScenarioParser` exactly
- Field order matches parser expectations
- Enum values use canonical names (`ControllerStrategy.name()`, `IdleParkingMode.name()`)
- No extra fields are added
- Events are sorted by tick, then by alias

#### 3. Passenger Flow Conversion

**Input** (from `ScenarioDefinitionDTO`):
```json
{
  "durationTicks": 30,
  "passengerFlows": [
    {
      "startTick": 0,
      "originFloor": 0,
      "destinationFloor": 5,
      "passengers": 2
    }
  ]
}
```

**Output** (generated events):
```
0, hall_call, p1, 0, UP
0, hall_call, p2, 0, UP
```

**Conversion logic**:
1. Each passenger flow is expanded into individual passenger events
2. Direction is calculated: `destinationFloor > originFloor ? "UP" : "DOWN"`
3. Each passenger gets a unique alias: `p1`, `p2`, `p3`, etc.
4. All passengers in a flow start at `startTick` on `originFloor`
5. Events are represented as `hall_call` (passengers waiting for lift at origin)

**Why `hall_call` instead of `car_call`**:
- Passenger flows represent people **waiting at a floor** to travel to a destination
- `hall_call` events specify origin floor + direction (matches real-world button press)
- `car_call` events represent buttons pressed **inside the lift** (destination only)
- The simulator will handle boarding and destination selection automatically

#### 4. Integration with Simulation Runs

**`SimulationRunService.generateBatchInputFile()`**:
```java
public Path generateBatchInputFile(Long runId) throws IOException {
    SimulationRun run = getRunById(runId);

    // Validate run has scenario and artifact path
    if (run.getScenario() == null) {
        throw new IllegalStateException("Run does not have a scenario");
    }
    if (run.getArtefactBasePath() == null || run.getArtefactBasePath().isBlank()) {
        throw new IllegalStateException("Run does not have an artefact base path");
    }

    // Generate file at {artefactBasePath}/input.scenario
    Path outputPath = Paths.get(run.getArtefactBasePath(), "input.scenario");
    batchInputGenerator.generateBatchInputFile(
        run.getVersion().getConfig(),
        parseScenarioJson(run.getScenario().getScenarioJson()),
        buildScenarioName(run),
        outputPath
    );

    return outputPath;
}
```

#### 5. Validation Strategy

**Compile-time validation** (in generator):
- Floor values must be within `[minFloor, maxFloor]`
- Start ticks must be less than `durationTicks`
- Origin and destination floors cannot be equal (validated earlier in `PassengerFlowDTO`)

**Runtime validation** (via tests):
- Golden-file tests compare generated output to known-good examples
- Format compliance tests ensure no unexpected fields are present
- Parser round-trip tests verify generated files can be parsed successfully

**Test approach**:
```java
@Test
public void testGenerateScenarioContent_GoldenFile() {
    String result = generator.generateScenarioContent(liftConfig, scenario, "Test");
    String expected = """
        name: Test
        ticks: 30
        min_floor: 0
        max_floor: 10
        ...
        0, hall_call, p1, 0, UP
        """;
    assertEquals(expected, result);
}
```

## Alternatives Considered

### Alternative 1: Rewrite Simulator to Accept JSON Input

**Description**: Modify `ScenarioRunner` to parse JSON directly instead of `.scenario` files

**Pros**:
- Eliminates conversion step
- Single source of truth for input format
- No file generation overhead
- Easier to add new features

**Cons**:
- Requires rewriting and retesting core simulator logic
- Breaks existing CLI workflows that depend on `.scenario` files
- Increases risk of introducing bugs in production simulator
- Loses compatibility with existing scenario library
- Doesn't address the need to preserve `.scenario` files for archival purposes

**Why rejected**: The `.scenario` format is a **stable public contract** used in production. Rewriting the simulator introduces significant risk without clear benefit. The generator approach maintains perfect backwards compatibility.

### Alternative 2: Dual Input Support (JSON + Legacy)

**Description**: Add JSON parsing to simulator alongside existing `.scenario` parser

**Pros**:
- Supports both formats
- Gradual migration path
- No conversion overhead for JSON inputs

**Cons**:
- Maintains two parsers with duplicated logic
- Increases complexity and testing burden
- `.scenario` format still needed for some workflows
- Doesn't eliminate need for format conversion in UI

**Why rejected**: Adds complexity without solving the fundamental problem. The generator approach is simpler and maintains a single execution path.

### Alternative 3: Store Generated Files in Database

**Description**: Store generated `.scenario` content in `simulation_run` table as TEXT/JSONB

**Pros**:
- No file system dependency
- Easier backup and recovery
- Queryable scenario content

**Cons**:
- Simulator expects file paths, not in-memory content
- Would require temporary file creation anyway
- Adds database storage overhead
- Complicates artifact management

**Why rejected**: The simulator CLI requires file paths. Storing in database doesn't eliminate file generation, just moves it. Storing files in artifact directories provides better separation and debugging capability.

### Alternative 4: Direct DTO Mapping (No Generator)

**Description**: Use a simple mapping library (MapStruct, ModelMapper) to convert DTOs to scenario objects

**Pros**:
- Less code to maintain
- Automatic field mapping
- Type-safe conversions

**Cons**:
- Doesn't handle complex conversion logic (passenger flows → hall calls)
- Can't guarantee exact text format (whitespace, ordering, formatting)
- Mapping libraries don't handle text file generation
- No control over enum serialization format

**Why rejected**: The conversion involves non-trivial logic (passenger expansion, direction calculation, event ordering). A custom generator provides full control over output format.

### Alternative 5: Template-Based Generation (FreeMarker, Thymeleaf)

**Description**: Use template engine to generate `.scenario` files from data models

**Pros**:
- Separation of template from code
- Easy to modify format without code changes
- Familiar template syntax

**Cons**:
- Adds dependency on template engine
- Templates are harder to test than code
- Debugging template issues is more difficult
- Format validation requires parsing template output
- Overkill for simple text generation

**Why rejected**: The `.scenario` format is stable and unlikely to change. A simple string builder approach is more maintainable and testable than templates.

## Consequences

### Positive

1. **Perfect Backwards Compatibility**: Generated files work with existing CLI simulator without modification
2. **Stable Contract**: `.scenario` format treated as immutable public API ensures long-term compatibility
3. **Zero Simulator Changes**: No risk of breaking production simulator logic
4. **Testable Conversion**: Golden-file tests verify exact format compliance
5. **Reusable Library**: Generator can be used from API, CLI, or batch jobs
6. **Clear Separation**: UI concerns (database, JSON) separate from execution concerns (files, text)
7. **Artifact Preservation**: Generated files stored alongside run results for debugging
8. **Direction Inference**: Automatic UP/DOWN calculation from origin/destination
9. **Passenger Expansion**: Handles multiple passengers in a flow correctly
10. **Validation**: Early error detection for invalid floor ranges and ticks

### Negative

1. **File Generation Overhead**: Extra I/O operation before simulation execution
2. **Conversion Layer**: Adds indirection between UI data and simulator input
3. **Format Duplication**: Two representations of same data (JSON + text)
4. **Maintenance Burden**: Generator must stay in sync with `ScenarioParser` format
5. **Testing Complexity**: Need golden-file tests to ensure format compliance
6. **No Direct Feedback**: UI users don't see generated `.scenario` files (unless debugging)

### Neutral

1. **Storage Location**: Files stored in `artefactBasePath` (configurable per run)
2. **File Naming**: Currently `input.scenario`, may need versioning for multiple runs
3. **Overwrite Behavior**: Files are truncated/overwritten (could preserve previous versions)
4. **Direction Simplification**: Assumes all passengers in a flow want the same direction
5. **Alias Strategy**: Sequential numbering (p1, p2, p3) is simple but could be more descriptive

## Implementation Notes

1. **Package Location**: `com.liftsimulator.admin.service.BatchInputGenerator`
2. **Dependencies**: `ObjectMapper` (for JSON parsing), Spring `@Service`
3. **File Generation**: Uses `Files.writeString()` with `CREATE` and `TRUNCATE_EXISTING`
4. **Parent Directory Creation**: Automatically creates artifact directories if missing
5. **Event Sorting**: `Comparator.comparingInt(tick).thenComparing(alias)`
6. **Validation**: Throws `IllegalArgumentException` for invalid floor ranges or ticks
7. **Null Handling**: `seed` field is optional (scenario may be null)

## Testing Strategy

### Unit Tests (`BatchInputGeneratorTest`)

**Golden-file tests**:
- Compare generated output to known-good `.scenario` files character-by-character
- Verify exact field formatting, whitespace, and ordering

**Format compliance tests**:
- Ensure no unexpected fields are present
- Verify field names match `ScenarioParser` expectations

**Conversion logic tests**:
- Direction calculation (UP/DOWN)
- Passenger alias generation (p1, p2, p3...)
- Event ordering (by tick, then alias)
- Multiple passengers per flow

**Validation tests**:
- Floor range violations
- Tick constraint violations
- Invalid configuration JSON

**File operations tests**:
- Parent directory creation
- File overwrite behavior
- Path handling

### Integration Tests (`SimulationRunServiceTest`)

**Service integration**:
- `generateBatchInputFile()` creates files in correct location
- Method validates run has scenario and artifact path
- `ResourceNotFoundException` for missing run
- `IllegalStateException` for missing scenario or path

### Test Coverage

- **20+ unit tests** covering all generation logic
- **5 integration tests** for service layer
- **3 golden-file tests** for format compliance

## Future Considerations

1. **File Versioning**: Add timestamp or version number to generated filenames
2. **Batch Generation**: Generate multiple files for different simulation variants
3. **Preview API**: Add endpoint to preview generated `.scenario` without saving
4. **Parser Round-Trip Test**: Generate file → parse with `ScenarioParser` → verify equality
5. **Custom Aliases**: Allow users to specify meaningful aliases instead of p1, p2, p3
6. **Direction Override**: Support explicit direction in passenger flows (rare edge case)
7. **Event Types**: Add support for other event types (cancel, out_of_service)
8. **Compression**: Compress old generated files to save disk space
9. **Cleanup Policy**: Automatically delete generated files after successful run
10. **Diff Tool**: Add tool to compare generated files across runs

## Related ADRs

- **ADR-0016**: Persistent Simulation Run Lifecycle (establishes `SimulationRun` and `SimulationScenario` entities)
- **ADR-0008**: JPA Entities and JSONB Mapping (establishes JSONB storage pattern for configurations)
- **ADR-0013**: Strict Schema Validation for Unknown Fields (ensures configuration format compliance)

## References

- `ScenarioParser.java`: Defines the expected `.scenario` file format
- `ScenarioRunnerMain.java`: CLI entry point that consumes `.scenario` files
- `LiftConfigDTO.java`: Lift configuration structure
- `ScenarioDefinitionDTO.java`: Passenger flow scenario structure
- Keep a Changelog: https://keepachangelog.com/
- Semantic Versioning: https://semver.org/
