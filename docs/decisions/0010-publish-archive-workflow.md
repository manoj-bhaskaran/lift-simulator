# ADR-0010: Publish/Archive Workflow for Configuration Management

**Date**: 2026-01-11

**Status**: Accepted

## Context

The Lift Config Service manages versioned lift system configurations with status tracking (DRAFT, PUBLISHED, ARCHIVED). While the basic versioning infrastructure exists, there is no enforcement mechanism to ensure exactly one published configuration per lift system at any given time.

Without this enforcement:

1. **Multiple published versions** could exist simultaneously, creating ambiguity about which configuration to use
2. **Runtime systems** have no clear way to retrieve the "current" configuration
3. **Manual archiving** would be required, prone to human error
4. **State transitions** lack proper workflow management

We need a publish/archive workflow that:
- Automatically archives the previously published version when publishing a new one
- Guarantees exactly one published configuration per lift system
- Provides dedicated runtime APIs that return only published configurations
- Enforces validation before allowing publication

## Decision

We will implement an **automatic publish/archive workflow** that ensures exactly one published version per lift system through transactional state transitions and dedicated runtime APIs.

### Implementation Details

#### 1. Enhanced Publish Service

**Modified `LiftSystemVersionService.publishVersion()`**:
- Transactional operation ensuring atomic state changes
- Before publishing a new version:
  1. Validate the configuration (already implemented)
  2. Check if version is already published (already implemented)
  3. **NEW**: Find all currently published versions for the lift system
  4. **NEW**: Archive each published version by calling `version.archive()`
  5. Publish the new version
- Result: Only one version remains in PUBLISHED status

**Code Flow**:
```java
@Transactional
public VersionResponse publishVersion(Long systemId, Integer versionNumber) {
    // ... existing validation logic ...

    // Archive any previously published versions
    List<LiftSystemVersion> publishedVersions =
        versionRepository.findByLiftSystemIdAndIsPublishedTrue(systemId);
    for (LiftSystemVersion publishedVersion : publishedVersions) {
        publishedVersion.archive();
        versionRepository.save(publishedVersion);
    }

    // Publish the new version
    version.publish();
    versionRepository.save(version);

    return VersionResponse.fromEntity(version);
}
```

#### 2. Runtime API Layer

**New Package Structure**: `com.liftsimulator.runtime`
- Separates runtime (consumer) APIs from admin (management) APIs
- Runtime APIs are read-only and return only published configurations

**RuntimeConfigService** (`com.liftsimulator.runtime.service.RuntimeConfigService`):
- Provides methods to retrieve published configurations
- Methods:
  - `getPublishedConfig(String systemKey)`: Returns the currently published version
  - `getPublishedVersion(String systemKey, Integer versionNumber)`: Returns a specific version if published
- Throws `ResourceNotFoundException` if no published version exists

**RuntimeConfigDTO** (`com.liftsimulator.runtime.dto.RuntimeConfigDTO`):
- Lightweight DTO for runtime responses
- Contains: `systemKey`, `displayName`, `versionNumber`, `config`, `publishedAt`
- Excludes internal metadata (id, status, timestamps)

**RuntimeConfigController** (`com.liftsimulator.runtime.controller.RuntimeConfigController`):
- REST endpoints under `/api/runtime/systems`
- Endpoints:
  - `GET /api/runtime/systems/{systemKey}/config`: Get current published config
  - `GET /api/runtime/systems/{systemKey}/versions/{versionNumber}`: Get specific published version
- Returns 404 if system not found or no published version exists

#### 3. State Transition Guarantees

**Transactional Safety**:
- All publish operations use `@Transactional` to ensure atomicity
- If archiving or publishing fails, entire transaction rolls back
- Prevents inconsistent states (e.g., two published versions)

**Database Constraints**:
- Existing unique constraint on `(lift_system_id, version_number)` prevents duplicate versions
- Application-level enforcement ensures single published version per system

#### 4. Testing Strategy

**Service Layer Tests** (`LiftSystemVersionServiceTest`):
- `testPublishVersion_Success()`: Updated to verify archiving call
- `testPublishVersion_ArchivesPreviouslyPublishedVersion()`: New test verifying:
  - Previously published version is archived (status = ARCHIVED)
  - New version is published (status = PUBLISHED)
  - Both save operations occur

**Runtime Service Tests** (`RuntimeConfigServiceTest`):
- `testGetPublishedConfig_Success()`: Retrieves published config by system key
- `testGetPublishedConfig_NoPublishedVersion()`: Returns 404 when no published version
- `testGetPublishedVersion_VersionNotPublished()`: Returns 404 for non-published versions
- Full coverage of error paths (system not found, version not found, not published)

### Alternatives Considered

#### 1. Database Constraint for Single Published Version

**Option**: Add unique partial index `(lift_system_id) WHERE is_published = true`
- **Pros**: Database enforces single published version guarantee
- **Cons**:
  - PostgreSQL-specific partial index syntax
  - Less portable across databases
  - Cryptic error messages on constraint violation
  - Requires migration to add constraint
- **Rejected**: Application-level enforcement provides better control and error handling

#### 2. Manual Archive Endpoint

**Option**: Provide separate `POST /archive` endpoint for users to archive versions
- **Pros**: Explicit control over archiving
- **Cons**:
  - Error-prone: users might forget to archive before publishing
  - Multiple API calls required for publish workflow
  - Race conditions if multiple users publish simultaneously
  - Poor user experience
- **Rejected**: Automatic archiving is safer and simpler

#### 3. Soft Delete Instead of Archive Status

**Option**: Use `deleted_at` timestamp instead of ARCHIVED status
- **Pros**: Common pattern for soft deletes
- **Cons**:
  - ARCHIVED is semantically different from deleted
  - Archived versions may still be useful for history/audit
  - Less clear in status queries
- **Rejected**: ARCHIVED status better represents the intent

#### 4. Versioned Runtime API (e.g., /api/v1/runtime)

**Option**: Version the runtime API path
- **Pros**: Easier to evolve API contract over time
- **Cons**:
  - Premature optimization before API stabilizes
  - Increases complexity of path management
  - Can add versioning later if needed
- **Deferred**: Can be added in future if API changes require it

## Consequences

### Positive

1. **Single Source of Truth**: Exactly one published configuration per lift system guaranteed
2. **Automatic Workflow**: No manual archiving required; reduces human error
3. **Transactional Safety**: Atomic state transitions prevent inconsistent states
4. **Clear Runtime API**: Dedicated endpoints for retrieving published configurations
5. **Separation of Concerns**: Admin APIs vs. runtime APIs clearly separated
6. **Better UX**: Users don't need to manually archive before publishing
7. **Auditability**: Archived versions remain in database for history tracking
8. **Testability**: Comprehensive test coverage for state transitions

### Negative

1. **Increased Complexity**: More database queries during publish operation
2. **Performance Cost**: Additional query to find published versions (minimal impact)
3. **No Rollback Endpoint**: Reverting to a previous version requires republishing it
4. **Archive Accumulation**: Archived versions accumulate over time (future cleanup needed)

### Risks and Mitigations

**Risk**: Performance degradation with many published versions to archive
- **Mitigation**: In practice, only one version should be published (enforced by workflow)
- **Mitigation**: Query is indexed on `(lift_system_id, is_published)`

**Risk**: Confusion about archived versions vs. deleted versions
- **Mitigation**: Clear API documentation explaining status meanings
- **Mitigation**: Version list API shows all statuses for transparency

**Risk**: Runtime API returning 404 disrupts consumer systems
- **Mitigation**: Clear error messages indicating no published version
- **Mitigation**: Admin UI should warn if unpublishing last version

**Risk**: Archive accumulation consumes database space
- **Mitigation**: Future ADR can address archival retention policies
- **Mitigation**: Archived versions provide valuable audit trail

## Compliance

This ADR complies with:
- **ACID Transactions**: Spring `@Transactional` ensures consistency
- **RESTful API Design**: Clear separation of admin vs. runtime concerns
- **Single Responsibility Principle**: Runtime service focused solely on published configs
- **Fail-Safe Defaults**: Operations fail if published config not available

## Related Decisions

- **ADR-0009**: Configuration Validation Framework - Validation enforced before publish
- **ADR-0008**: JPA Entities and JSONB Mapping - Version status tracking
- **ADR-0006**: Spring Boot Admin Backend - REST API architecture
- **ADR-0007**: PostgreSQL and Flyway Integration - Transactional database operations

## References

- [Spring Transaction Management](https://docs.spring.io/spring-framework/reference/data-access/transaction.html)
- [RESTful API Design Best Practices](https://restfulapi.net/)
- [Database Indexing for Performance](https://use-the-index-luke.com/)
