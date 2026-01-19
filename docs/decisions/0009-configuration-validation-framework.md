# ADR-0009: Configuration Validation Framework

**Date**: 2026-01-11

**Status**: Accepted

## Context

The Lift Config Service allows users to create and manage lift system configurations stored as JSON in the `config` field of `LiftSystemVersion` entities. Without validation, invalid configurations could be saved to the database, leading to:

1. **Runtime failures** when attempting to use configurations with missing or invalid fields
2. **Data integrity issues** with configurations that violate domain constraints
3. **Poor user experience** with unclear error messages when validation fails late in the process
4. **Publishing invalid configurations** that could break simulation functionality

We need a comprehensive validation framework that:
- Validates JSON structure and required fields (structural validation)
- Enforces domain-specific business rules (domain validation)
- Provides detailed, actionable error messages
- Blocks operations (create, update, publish) when configurations are invalid
- Supports warnings for suboptimal but valid configurations
- Integrates seamlessly with the REST API

## Decision

We will implement a **multi-layer validation framework** using Jakarta Bean Validation for structural validation and custom service-layer logic for domain validation.

### Implementation Details

#### 1. Configuration DTO

**LiftConfigDTO** (`com.liftsimulator.admin.dto.LiftConfigDTO`):
- Java record representing the structure of configuration JSON
- Jakarta Bean Validation annotations for structural validation:
  - `@NotNull` for required fields
  - `@Min` for minimum value constraints
  - Enum types for `controllerStrategy` and `idleParkingMode` (type safety)

```java
public record LiftConfigDTO(
    @NotNull Integer minFloor,
    @NotNull Integer maxFloor,
    @NotNull @Min(1) Integer lifts,
    @NotNull @Min(1) Integer travelTicksPerFloor,
    @NotNull @Min(1) Integer doorTransitionTicks,
    @NotNull @Min(1) Integer doorDwellTicks,
    @NotNull @Min(0) Integer doorReopenWindowTicks,
    @NotNull Integer homeFloor,
    @NotNull @Min(0) Integer idleTimeoutTicks,
    @NotNull ControllerStrategy controllerStrategy,
    @NotNull IdleParkingMode idleParkingMode
) {}
```

#### 2. Validation Service

**ConfigValidationService** (`com.liftsimulator.admin.service.ConfigValidationService`):
- Performs two-phase validation:
  1. **Structural validation**: Parse JSON to DTO, validate using Jakarta Validator
  2. **Domain validation**: Enforce cross-field constraints and business rules

**Validation Rules**:
- `doorReopenWindowTicks` must not exceed `doorTransitionTicks`
- `maxFloor` must be greater than `minFloor`
- `homeFloor` must be within valid floor range (`minFloor` to `maxFloor`)
- All numeric fields must meet minimum value constraints

**Warning System**:
- Low `doorDwellTicks` values (< 2)
- More lifts than available floors in the configured range (inefficient)
- Low `idleTimeoutTicks` with `PARK_TO_HOME_FLOOR` mode (< 3)
- Zero `doorReopenWindowTicks` (disables door reopening)

#### 3. Validation Response DTOs

**ValidationIssue** (`com.liftsimulator.admin.dto.ValidationIssue`):
- Represents a single validation issue
- Fields: `field`, `message`, `severity` (ERROR or WARNING)

**ConfigValidationResponse** (`com.liftsimulator.admin.dto.ConfigValidationResponse`):
- Contains validation results
- Fields: `valid` (boolean), `errors` (list), `warnings` (list)
- Helper methods: `hasErrors()`, `hasWarnings()`

#### 4. Validation Endpoint

**POST /api/config/validate**:
- Validates configuration JSON without persisting
- Returns structured validation response
- Allows users to validate before creating/updating versions

#### 5. Automatic Validation Integration

Validation is automatically enforced in:
- `LiftSystemVersionService.createVersion()` - validates before creating version
- `LiftSystemVersionService.updateVersionConfig()` - validates before updating
- `LiftSystemVersionService.publishVersion()` - validates before publishing

**Failure Behavior**:
- Throws `ConfigValidationException` with detailed validation response
- Global exception handler returns 400 Bad Request with validation details
- Operations are blocked; no invalid data is persisted

#### 6. Version Publishing

**POST /api/lift-systems/{systemId}/versions/{versionNumber}/publish**:
- New endpoint to publish versions
- Validates configuration before publishing
- Prevents publishing if configuration has errors
- Returns 409 Conflict if version is already published

#### 7. Exception Handling

**ConfigValidationException**:
- Custom exception containing `ConfigValidationResponse`
- Thrown when validation fails with errors
- Captured by `GlobalExceptionHandler`

**Exception Handler**:
- Returns validation response with 400 status
- Preserves detailed error and warning information
- Added `IllegalStateException` handler for state conflicts (409 status)

### Alternatives Considered

#### 1. JSON Schema Validation

**Option**: Use JSON Schema (javax.json.validation) for validation
- **Pros**: Standard JSON validation approach, declarative schema
- **Cons**:
  - Requires separate schema files
  - Less type-safe than Java DTOs
  - Harder to integrate with Spring validation
  - More complex error message customization
- **Rejected**: Bean Validation provides better type safety and Spring integration

#### 2. Validation Only at Publish Time

**Option**: Only validate when publishing, allow invalid drafts
- **Pros**: More flexible for users working on incomplete configurations
- **Cons**:
  - Users discover errors late in the workflow
  - Invalid data in database complicates testing and debugging
  - Inconsistent validation behavior
- **Rejected**: Fail-fast validation provides better user experience

#### 3. Database Constraints Only

**Option**: Rely on database check constraints for validation
- **Pros**: Validation enforced at database level
- **Cons**:
  - JSONB check constraints are complex and limited
  - Poor error messages from database violations
  - Harder to provide structured error responses
  - Can't distinguish errors from warnings
- **Rejected**: Service-layer validation provides better control and UX

## Consequences

### Positive

1. **Data Integrity**: Invalid configurations cannot be saved or published
2. **Better UX**: Users receive immediate, detailed feedback on validation errors
3. **Type Safety**: DTO approach catches type mismatches at validation time
4. **Maintainability**: Validation logic is centralized in service layer
5. **Testability**: Validation logic can be unit tested independently
6. **Warnings**: Users are informed about suboptimal configurations without blocking operations
7. **Documentation**: DTO serves as clear documentation of required fields

### Negative

1. **Coupling**: Configuration structure is defined in Java code (DTO)
2. **Schema Evolution**: Adding new fields requires DTO changes
3. **Validation Cost**: Every create/update operation performs validation
4. **Error Handling Complexity**: Multiple exception types to handle

### Risks and Mitigations

**Risk**: Validation becomes a performance bottleneck
- **Mitigation**: Validation is fast (in-memory); optimize if needed with caching

**Risk**: DTO changes break backward compatibility
- **Mitigation**: Follow semantic versioning; add optional fields as nullable

**Risk**: Validation rules become too strict
- **Mitigation**: Use warnings for soft constraints; errors only for critical issues

## Compliance

This ADR complies with:
- **Jakarta Bean Validation (JSR-380)**: Standard validation annotations
- **Spring Boot Best Practices**: Service-layer validation before persistence
- **RESTful API Design**: Structured error responses with appropriate status codes

## Related Decisions

- **ADR-0008**: JPA Entities and JSONB Mapping - Config stored as JSON string
- **ADR-0006**: Spring Boot Admin Backend - REST API architecture
- **ADR-0007**: PostgreSQL and Flyway Integration - Database schema

## References

- [Jakarta Bean Validation Specification](https://beanvalidation.org/3.0/)
- [Spring Boot Validation Guide](https://spring.io/guides/gs/validating-form-input/)
- [Keep a Changelog: Semantic Versioning](https://semver.org/)
