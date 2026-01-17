# Sample Lift Configuration Scenarios

This directory contains example lift system configurations for testing and reference purposes.

## Available Scenarios

### 1. basic-office-building.json

**Use Case:** Small to medium office building

**Configuration:**
- 10 floors
- 2 lifts
- NEAREST_REQUEST_ROUTING strategy (simple nearest-floor algorithm)
- Parks to home floor (ground floor) when idle
- Moderate timing parameters

**Best For:**
- Testing basic functionality
- Understanding the minimum viable configuration
- Low-traffic scenarios
- Getting started with the system

---

### 2. high-rise-residential.json

**Use Case:** Large residential high-rise building

**Configuration:**
- 30 floors
- 4 lifts
- DIRECTIONAL_SCAN strategy (SCAN/LOOK algorithm with direction commitment)
- Parks to home floor when idle
- Optimized for high-traffic scenarios
- Longer door dwell times for passenger boarding

**Best For:**
- Testing complex scenarios
- Demonstrating advanced controller strategy
- High-traffic simulation
- Performance testing

---

### 3. invalid-example.json

**Use Case:** Testing validation framework

**Configuration:**
- **INTENTIONALLY INVALID** - contains multiple validation errors
- Demonstrates validation error messages
- Shows configuration constraints

**Known Errors:**
- `floors: 1` - Must be at least 2
- `lifts: 0` - Must be at least 1
- `travelTicksPerFloor: -1` - Must be at least 1
- `doorReopenWindowTicks: 5` exceeds `doorTransitionTicks: 2`
- `homeFloor: 20` - Out of range for 1 floor
- `idleTimeoutTicks: -5` - Must be non-negative
- `controllerStrategy: "INVALID_STRATEGY"` - Invalid enum value

**Best For:**
- Testing validation error handling
- Understanding configuration constraints
- UAT validation testing (see docs/UAT-TEST-SCENARIOS.md - Scenario 4)

---

## How to Use These Configurations

### In the Admin UI

1. Navigate to a lift system detail page
2. Create a new version
3. Click "Edit" on the version
4. Copy the contents of one of these files
5. Paste into the configuration editor
6. Click "Validate" to check the configuration
7. Click "Save Draft" or "Publish" as needed

### Via API

```bash
# Example: Validate basic-office-building.json
curl -X POST http://localhost:8080/api/config/validate \
  -H "Content-Type: application/json" \
  -d @src/main/resources/scenarios/basic-office-building.json
```

### In Tests

These configurations can be loaded in integration tests:

```java
ClassPathResource resource = new ClassPathResource("scenarios/basic-office-building.json");
String configJson = new String(Files.readAllBytes(resource.getFile().toPath()));
```

---

## Creating Your Own Configurations

Use these as templates and modify to suit your needs:

### Required Fields

All configurations must include these fields:
- `floors` (integer, ≥ 2)
- `lifts` (integer, ≥ 1)
- `travelTicksPerFloor` (integer, ≥ 1)
- `doorTransitionTicks` (integer, ≥ 1)
- `doorDwellTicks` (integer, ≥ 1)
- `doorReopenWindowTicks` (integer, ≥ 0, ≤ doorTransitionTicks)
- `homeFloor` (integer, ≥ 0, < floors)
- `idleTimeoutTicks` (integer, ≥ 0)
- `controllerStrategy` (enum: "NEAREST_REQUEST_ROUTING" or "DIRECTIONAL_SCAN")
- `idleParkingMode` (enum: "STAY_AT_CURRENT_FLOOR" or "PARK_TO_HOME_FLOOR")

### Validation Rules

- **Cross-field validation:** `doorReopenWindowTicks` must not exceed `doorTransitionTicks`
- **Range validation:** `homeFloor` must be within `[0, floors)`
- **Enum validation:** Strategy and mode values must match exactly (case-sensitive)

See the main [README.md](../../../../README.md) for complete validation rules and API documentation.

---

## Controller Strategy Comparison

### NEAREST_REQUEST_ROUTING
- **Algorithm:** Services the closest requested floor first
- **Pros:** Simple, predictable, good for low traffic
- **Cons:** Can be inefficient in high-traffic scenarios
- **Use When:** Small buildings, predictable patterns, testing

### DIRECTIONAL_SCAN
- **Algorithm:** SCAN/LOOK algorithm with direction commitment
- **Pros:** Efficient for high traffic, prevents starvation
- **Cons:** Slightly more complex logic
- **Use When:** Large buildings, high traffic, real-world scenarios

---

## Testing Tips

1. **Start Simple:** Begin with `basic-office-building.json` to verify basic functionality
2. **Test Validation:** Use `invalid-example.json` to ensure error handling works
3. **Scale Up:** Move to `high-rise-residential.json` for complex scenarios
4. **Create Variants:** Modify these templates to test edge cases
5. **Compare Strategies:** Create two versions with different controller strategies and compare behavior

---

## Related Documentation

- [UAT Test Scenarios](../../../../docs/UAT-TEST-SCENARIOS.md) - Comprehensive testing guide
- [Main README](../../../../README.md) - Full API documentation and setup
- [ADR-0009: Configuration Validation Framework](../../../../docs/decisions/0009-configuration-validation-framework.md)
- [ADR-0005: Selectable Controller Strategy](../../../../docs/decisions/0005-selectable-controller-strategy.md)
