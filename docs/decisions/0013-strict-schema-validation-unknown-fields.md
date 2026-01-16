# ADR-0013: Strict Schema Validation for Unknown Fields

**Date**: 2026-01-16

**Status**: Accepted

## Context

The Lift Config Service accepts configuration JSON payloads at multiple entry points:
- REST API endpoints (`POST /api/lift-systems/{systemId}/versions`, `PUT /api/lift-systems/{systemId}/versions/{versionNumber}`)
- Configuration validation endpoint (`POST /api/config/validate`)
- CLI tool (`LocalSimulationMain` reading JSON files)

By default, Jackson's `ObjectMapper` silently **ignores unknown properties** during JSON deserialization. This behavior can lead to several problems:

1. **Typos go undetected**: A user submitting `"floor": 10` instead of `"floors": 10` would receive no error, leading to a missing required field error instead of a clear "unknown property" error
2. **API contract violations**: Clients can send arbitrary extra fields without feedback, making it harder to detect integration issues
3. **Forward compatibility confusion**: Users might add fields expecting future support, unaware they're being ignored
4. **Security concerns**: Unknown fields could be used for injection attacks or to probe the API for vulnerabilities
5. **Debugging difficulty**: Silent ignoring makes it harder to identify why configurations aren't behaving as expected

We need to decide: should the system **reject**, **ignore**, or **log** unknown properties in configuration JSON?

## Decision

We will **reject unknown properties** by configuring Jackson's `ObjectMapper` with `DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES = true`.

### Implementation Details

#### 1. Spring Configuration Bean

**JacksonConfiguration** (`com.liftsimulator.admin.config.JacksonConfiguration`):
- Defines a `@Primary` `ObjectMapper` bean for Spring-managed components
- Configures `FAIL_ON_UNKNOWN_PROPERTIES` to `true`
- Applied automatically to all REST controllers and Spring-managed services

```java
@Configuration
public class JacksonConfiguration {
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        return mapper;
    }
}
```

#### 2. Enhanced Error Handling

**ConfigValidationService** (`com.liftsimulator.admin.service.ConfigValidationService`):
- Catches `UnrecognizedPropertyException` separately from other `JsonProcessingException`
- Provides specific error messages: `"Unknown property 'fieldName' is not allowed in configuration schema"`
- Returns field name in validation error for precise debugging

```java
try {
    config = objectMapper.readValue(configJson, LiftConfigDTO.class);
} catch (UnrecognizedPropertyException e) {
    String fieldName = e.getPropertyName();
    errors.add(new ValidationIssue(
        fieldName,
        "Unknown property '" + fieldName + "' is not allowed in configuration schema",
        ValidationIssue.Severity.ERROR
    ));
    return new ConfigValidationResponse(false, errors, warnings);
}
```

#### 3. CLI Tool Consistency

**LocalSimulationMain** (`com.liftsimulator.runtime.LocalSimulationMain`):
- Standalone CLI ObjectMapper configured with same strict validation
- Ensures consistent behavior across REST API and CLI workflows

```java
private static LiftConfigDTO readConfig(Path configPath) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    // ...
}
```

#### 4. Test Coverage

**ConfigValidationServiceTest**:
- Five new test cases for unknown field rejection:
  - `testValidate_UnknownFieldRejected()` - Basic unknown field rejection
  - `testValidate_TypoInFieldNameRejected()` - Catches typos like "floor" vs "floors"
  - `testValidate_MultipleUnknownFieldsRejected()` - Handles multiple unknown fields
  - `testValidate_UnknownFieldWithValidData()` - Rejects unknown fields even with valid data
- Test ObjectMapper configured to match production settings

### Behavior Change

**Before (v0.34.2 and earlier)**:
```json
{
  "floors": 10,
  "lifts": 2,
  ...
  "unknownField": "value",
  "typo": 123
}
```
→ **Silently ignored**, configuration accepted if all required fields present

**After (v0.35.0 and later)**:
```json
{
  "floors": 10,
  "lifts": 2,
  ...
  "unknownField": "value"
}
```
→ **Rejected** with error:
```json
{
  "valid": false,
  "errors": [{
    "field": "unknownField",
    "message": "Unknown property 'unknownField' is not allowed in configuration schema",
    "severity": "ERROR"
  }]
}
```

## Alternatives Considered

### 1. Silent Ignore (Jackson Default)

**Option**: Keep default Jackson behavior (`FAIL_ON_UNKNOWN_PROPERTIES = false`)
- **Pros**:
  - More lenient API contract
  - Backward compatible with existing clients sending extra fields
  - Easier forward compatibility (new clients can send fields to old servers)
- **Cons**:
  - Typos go undetected, leading to confusing "missing field" errors
  - No feedback on API contract violations
  - Security risk from unrestricted field injection
  - Harder to debug configuration issues
- **Rejected**: Silent failures create poor developer experience and security risks

### 2. Log Unknown Fields

**Option**: Log unknown properties but don't reject them
- **Pros**:
  - Maintains lenient behavior while providing visibility
  - Helps debugging without breaking existing integrations
- **Cons**:
  - Logs aren't visible to API clients (only server operators)
  - Doesn't prevent the root problem (typos, contract violations)
  - Log noise from legitimate forward-compatibility scenarios
  - Still allows security probing
- **Rejected**: Doesn't solve the core problem; logging alone insufficient

### 3. Configurable Strictness

**Option**: Make strictness configurable via request header or query parameter
- **Pros**:
  - Allows clients to choose behavior per request
  - Supports migration path (strict mode for new clients, lenient for legacy)
- **Cons**:
  - Increases API complexity
  - Splits contract into two modes, harder to document
  - Most clients would never use the flag
  - Doesn't help users who are unaware of typos
- **Rejected**: Complexity doesn't justify marginal benefit

### 4. Accept with Warnings

**Option**: Accept unknown fields but return them in validation warnings
- **Pros**:
  - Non-blocking feedback
  - Aligns with existing warning system (low door dwell, etc.)
- **Cons**:
  - Warnings can be ignored; users might miss critical typos
  - Blurs distinction between errors (must fix) and warnings (should review)
  - Still allows injection of arbitrary data
  - Inconsistent with strict validation philosophy
- **Rejected**: Unknown fields should be errors, not warnings

## Consequences

### Positive

1. **Typo Detection**: Catches common errors like `"floor"` instead of `"floors"` immediately
2. **Clear Error Messages**: Field-specific error messages with exact unknown property name
3. **API Contract Enforcement**: Ensures clients only use documented fields
4. **Security Improvement**: Prevents injection of unexpected data through unknown fields
5. **Debugging Aid**: Faster identification of configuration issues
6. **Consistent Behavior**: Same validation across REST API, validation endpoint, and CLI
7. **Documentation Alignment**: Forces API usage to match documented schema

### Negative

1. **Breaking Change**: Clients previously sending unknown fields will now receive errors
2. **Forward Compatibility Impact**: Cannot send future fields to old servers (versioning required)
3. **Strictness Trade-off**: Less forgiving API may frustrate users with minor mistakes
4. **Migration Effort**: Existing configurations with unknown fields must be cleaned up

### Risks and Mitigations

**Risk**: Breaking change disrupts existing integrations
- **Mitigation**:
  - Version bump to 0.35.0 (minor version) signals change
  - CHANGELOG clearly documents breaking change
  - Error messages clearly indicate problem field
  - Unlikely to affect existing users (no external clients yet)

**Risk**: Legitimate use cases for extra fields
- **Mitigation**:
  - Users can add comments via separate mechanism (e.g., description field)
  - Schema evolution follows proper versioning
  - If needed, can add explicit extension field in future

**Risk**: Too strict for development/testing
- **Mitigation**:
  - Validation endpoint allows testing before persistence
  - Clear error messages make fixing easy
  - Strictness improves quality, not hinders it

## Migration Impact

### For New Systems (v0.35.0+)
- **No impact**: All configurations are validated from start

### For Existing Systems (Upgrading from v0.34.2 or earlier)
- **Impact**: Configurations with unknown fields will fail validation
- **Action Required**:
  1. Run validation endpoint on existing configurations
  2. Remove any unknown fields reported in errors
  3. Update client code to only send documented fields

### For API Clients
- **Change**: Must only send documented fields
- **Detection**: Validation errors clearly identify unknown fields
- **Fix**: Remove unknown fields from request payloads

## Compliance

This ADR complies with:
- **JSON Schema Best Practices**: Strict validation prevents schema drift
- **REST API Design**: Clear, actionable error messages for invalid requests
- **Security Best Practices**: Reject unexpected input to prevent injection attacks
- **Fail-Fast Principle**: Detect errors at validation time, not runtime

## Related Decisions

- **ADR-0009**: Configuration Validation Framework - Overall validation architecture
- **ADR-0006**: Spring Boot Admin Backend - REST API implementation
- **ADR-0008**: JPA Entities and JSONB Mapping - JSON storage approach

## References

- [Jackson Documentation: DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES](https://fasterxml.github.io/jackson-databind/javadoc/2.9/com/fasterxml/jackson/databind/DeserializationFeature.html#FAIL_ON_UNKNOWN_PROPERTIES)
- [OWASP: Input Validation](https://cheatsheetseries.owasp.org/cheatsheets/Input_Validation_Cheat_Sheet.html)
- [REST API Error Handling Best Practices](https://www.rfc-editor.org/rfc/rfc7807)
- [Semantic Versioning 2.0.0](https://semver.org/)
