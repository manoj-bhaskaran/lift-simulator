# ADR-0008: JPA Entities and JSONB Mapping

**Date**: 2026-01-11

**Status**: Accepted

## Context

With the PostgreSQL database schema in place (ADR-0007), we need to implement the data access layer for the Lift Config Service. The database stores lift system configurations with the following requirements:

1. Map `lift_system` and `lift_system_version` tables to Java objects
2. Handle the `config` JSONB column in `lift_system_version` for storing complex JSON configurations
3. Provide repository interfaces for database operations
4. Support versioned configurations with lifecycle states (DRAFT, PUBLISHED, ARCHIVED)
5. Maintain referential integrity between lift systems and their versions

We evaluated several approaches for implementing the data access layer and handling the JSONB field.

## Decision

We will use **Spring Data JPA** with JPA entities for database mapping and **Hibernate's `@JdbcTypeCode(SqlTypes.JSON)`** annotation for JSONB field handling.

### Implementation Details

#### 1. JPA Entities

**LiftSystem Entity** (`com.liftsimulator.admin.entity.LiftSystem`):
- Maps to `lift_system` table
- One-to-many relationship with `LiftSystemVersion` (bidirectional)
- Automatic timestamp management using `@PrePersist` and `@PreUpdate`
- Helper methods for managing version relationships (`addVersion`, `removeVersion`)
- Equals/hashCode based on business key (`systemKey`)

**LiftSystemVersion Entity** (`com.liftsimulator.admin.entity.LiftSystemVersion`):
- Maps to `lift_system_version` table
- Many-to-one relationship with `LiftSystem` (lazy-loaded)
- Version lifecycle managed via `VersionStatus` enum (DRAFT, PUBLISHED, ARCHIVED)
- Helper methods: `publish()`, `archive()` for state transitions
- JSONB `config` field mapped as `String` with `@JdbcTypeCode(SqlTypes.JSON)`
- Equals/hashCode based on composite business key (`liftSystem`, `versionNumber`)

#### 2. JSONB Field Mapping

For the `config` JSONB column, we chose to map it as a `String` using Hibernate 6.x's `@JdbcTypeCode`:

```java
@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "config", nullable = false, columnDefinition = "jsonb")
private String config;
```

**Rationale for String mapping**:
1. **Flexibility**: Application can use any JSON library (Jackson, Gson, etc.) for serialization/deserialization
2. **Type Safety**: Domain models will deserialize JSON to strongly-typed configuration objects
3. **Simplicity**: No custom Hibernate type converters needed
4. **Validation**: JSON schema validation can happen at service layer before persistence
5. **Future-proof**: Easy to change JSON structure without entity changes

**Alternative considered**: `Map<String, Object>` with automatic JSON conversion
- **Rejected because**: Loses type safety, harder to validate, couples entity to JSON structure

#### 3. Spring Data Repositories

**LiftSystemRepository**:
- Extends `JpaRepository<LiftSystem, Long>`
- Custom query: `findBySystemKey(String systemKey)` - find by unique business key
- Custom query: `existsBySystemKey(String systemKey)` - existence check
- Standard CRUD inherited from `JpaRepository`

**LiftSystemVersionRepository**:
- Extends `JpaRepository<LiftSystemVersion, Long>`
- Query methods:
  - `findByLiftSystemIdOrderByVersionNumberDesc` - get versions ordered by number
  - `findByLiftSystemIdAndVersionNumber` - find specific version
  - `findByLiftSystemIdAndIsPublishedTrue` - get published versions
  - `findByStatus(VersionStatus status)` - find by lifecycle status
  - `findMaxVersionNumberByLiftSystemId` - get next version number
- Uses `@Query` annotations for complex queries with JPQL

#### 4. Cascade and Orphan Removal

The `LiftSystem` entity uses:
```java
@OneToMany(mappedBy = "liftSystem", cascade = CascadeType.ALL, orphanRemoval = true)
private List<LiftSystemVersion> versions = new ArrayList<>();
```

This ensures:
- Deleting a `LiftSystem` cascades to delete all its versions
- Removing a version from the collection deletes it from the database
- Consistent with database `ON DELETE CASCADE` foreign key constraint

#### 5. Timestamp Management

Both entities use JPA lifecycle callbacks:
```java
@PrePersist
protected void onCreate() {
    OffsetDateTime now = OffsetDateTime.now();
    createdAt = now;
    updatedAt = now;
}

@PreUpdate
protected void onUpdate() {
    updatedAt = OffsetDateTime.now();
}
```

This keeps timestamp management consistent with database defaults while allowing application-controlled values for testing.

## Alternatives Considered

### Alternative 1: MyBatis for Data Access

**Description**: Use MyBatis for SQL mapping instead of JPA/Hibernate

**Pros**:
- Full control over SQL queries
- No ORM magic or lazy-loading surprises
- Better for complex queries

**Cons**:
- More boilerplate (XML/annotation mappers)
- No automatic dirty checking
- Manual relationship management
- Doesn't leverage Spring Boot's JPA auto-configuration

**Why rejected**: Spring Data JPA provides sufficient query capabilities while reducing boilerplate. The domain is simple enough that ORM benefits outweigh concerns.

### Alternative 2: JSONB as `Map<String, Object>`

**Description**: Map JSONB column directly to `Map<String, Object>` using Hibernate JSON type

**Pros**:
- Direct access to JSON properties in Java
- No manual serialization/deserialization

**Cons**:
- Type-unsafe (everything is `Object`)
- Harder to validate structure
- Couples entity to JSON schema
- Difficult to version JSON structure

**Why rejected**: Storing JSON as `String` and deserializing at service layer provides better type safety and flexibility.

### Alternative 3: Separate Configuration Entity

**Description**: Create a separate `LiftConfiguration` entity and use JSON only for serialization

**Pros**:
- Fully normalized schema
- Type-safe at entity level
- Easy to query individual config properties

**Cons**:
- Schema changes require migrations
- Less flexible for evolving configurations
- Doesn't leverage JSONB query capabilities
- More complex entity model

**Why rejected**: JSONB provides schema flexibility needed for versioned configurations. Individual properties can be extracted via JSON queries if needed.

### Alternative 4: Document Database (MongoDB)

**Description**: Use MongoDB for lift configurations instead of PostgreSQL

**Pros**:
- Native JSON storage
- Schema-less flexibility
- Good for evolving documents

**Cons**:
- Adds another database to infrastructure
- PostgreSQL already provides JSONB with similar capabilities
- Loses ACID guarantees for metadata
- Increases operational complexity

**Why rejected**: PostgreSQL JSONB provides sufficient JSON capabilities while maintaining relational integrity for metadata.

## Consequences

### Positive

1. **Standard Patterns**: Uses Spring Data JPA conventions familiar to Java developers
2. **Minimal Boilerplate**: Repository interfaces auto-implement CRUD operations
3. **Type Safety**: Entities provide compile-time safety for metadata fields
4. **Flexible JSON**: String-based JSONB allows any JSON structure without entity changes
5. **Query Capabilities**: Can leverage PostgreSQL's JSONB operators in custom queries if needed
6. **Transaction Support**: JPA transactions ensure data consistency
7. **Testing Support**: Spring Boot Test provides `@DataJpaTest` for repository testing
8. **Relationship Management**: JPA handles foreign keys and cascading operations

### Negative

1. **ORM Learning Curve**: Developers need to understand JPA lazy-loading and entity lifecycle
2. **JSON Coupling**: Service layer must handle JSON serialization/deserialization
3. **Migration Dependency**: Schema changes require Flyway migrations
4. **Performance Overhead**: ORM adds slight overhead vs raw JDBC (acceptable for this use case)

### Neutral

1. **JSON Library Choice**: Application needs to choose JSON library (Jackson via Spring Boot)
2. **Query Complexity**: Complex JSONB queries may require `@Query` annotations or Criteria API
3. **Caching Strategy**: JPA second-level cache not configured yet (can add later if needed)

## Implementation Notes

1. **Hibernate Version**: Using Hibernate 6.x (included with Spring Boot 3.2.1)
2. **PostgreSQL Dialect**: Configured in `application-dev.yml`
3. **DDL Validation**: Hibernate set to `validate` mode (Flyway handles schema creation)
4. **Test Configuration**: Separate `application-test.yml` for test database
5. **Verification**: `JpaVerificationRunner` provides runtime verification of JPA operations

## Testing Strategy

1. **Unit Tests**: Not applicable for repositories (require database)
2. **Integration Tests**:
   - `@DataJpaTest` for repository testing
   - `@AutoConfigureTestDatabase(replace = Replace.NONE)` to use PostgreSQL
   - Tests verify CRUD, custom queries, JSONB persistence, cascading
3. **Verification Runner**:
   - `JpaVerificationRunner` for runtime verification
   - Enabled with `--spring.jpa.verify=true`
   - Tests all operations in production-like environment

## Future Considerations

1. **JSON Schema Validation**: Add JSON schema validation at service layer
2. **JSONB Queries**: May add custom queries using PostgreSQL JSONB operators (`@>`, `->`, `->>`)
3. **Caching**: Consider adding JPA second-level cache if performance becomes an issue
4. **Auditing**: Could add Spring Data JPA auditing for created/modified metadata
5. **Optimistic Locking**: Consider `@Version` for concurrent update handling
6. **DTO Layer**: May introduce DTOs between entities and REST layer for decoupling

## References

- Spring Data JPA: https://docs.spring.io/spring-data/jpa/docs/current/reference/html/
- Hibernate 6.x JSON Mapping: https://docs.jboss.org/hibernate/orm/6.0/userguide/html_single/Hibernate_User_Guide.html#basic-jpa-convert
- PostgreSQL JSONB: https://www.postgresql.org/docs/current/datatype-json.html
- ADR-0006: Spring Boot Admin Backend
- ADR-0007: PostgreSQL and Flyway Integration
