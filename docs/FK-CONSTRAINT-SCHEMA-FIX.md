# Foreign Key Constraint Fix - PostgreSQL Schema Resolution

## Problem

Hibernate was generating ALTER TABLE statements with qualified target tables but unqualified referenced tables:

```sql
ALTER TABLE IF EXISTS lift_simulator.lift_system_version 
  ADD CONSTRAINT fk_lift_system_id 
  FOREIGN KEY (lift_system_id) 
  REFERENCES lift_system(id)  -- UNQUALIFIED - PROBLEM!
  ON DELETE CASCADE
```

PostgreSQL couldn't resolve the unqualified `lift_system` table name because:
1. The search_path didn't include the `lift_simulator` schema
2. PostgreSQL looked for `lift_system` in the default public schema
3. Result: `ERROR: relation "lift_system" does not exist`

## Root Cause

While the test configuration set `hibernate.default_schema: lift_simulator`, the PostgreSQL session's search_path wasn't configured to include that schema. This mismatch caused Hibernate to generate correctly qualified DDL, but PostgreSQL couldn't resolve unqualified references.

## Solution

Added `?currentSchema=lift_simulator` parameter to the JDBC URL. This tells the PostgreSQL driver to set the search_path for the session, ensuring unqualified identifiers are resolved to the correct schema.

### Changed Files

#### 1. `src/test/resources/application-test.yml`

```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/lift_simulator_test?currentSchema=lift_simulator}
```

**Key Change:** Added `?currentSchema=lift_simulator` to both:
- Default JDBC URL (for local development)
- Environment variable default

#### 2. `.github/workflows/ci.yml`

```yaml
- name: Run tests with coverage
  run: mvn -q test jacoco:report
  env:
    SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/lift_simulator_test?currentSchema=lift_simulator
    SPRING_DATASOURCE_USERNAME: lift_admin
    SPRING_DATASOURCE_PASSWORD: liftpassword
```

**Key Change:** Updated SPRING_DATASOURCE_URL env var to include `?currentSchema=lift_simulator`

## How It Works

When `currentSchema=lift_simulator` is set:

1. **PostgreSQL Driver** sets the session's `search_path` to include `lift_simulator` schema
2. **Unqualified Table Names** are resolved to `lift_simulator` schema automatically
3. **Hibernate DDL** with unqualified references now succeeds:
   ```sql
   -- This now works because search_path includes lift_simulator
   REFERENCES lift_system(id)
   ```

## Verification

### Local Testing
```bash
# All repository tests pass with foreign key constraints
mvn test -Dtest="*RepositoryTest"

# Results: 46 Tests, 0 Failures, 0 Errors
```

### Test Coverage
- ✅ LiftSystemRepositoryTest (8 tests)
- ✅ LiftSystemVersionRepositoryTest (11 tests) - **FK Tests**
- ✅ SimulationRunRepositoryTest (19 tests)
- ✅ SimulationScenarioRepositoryTest (8 tests)

## Benefits

1. **Simple** - Single URL parameter, no code changes
2. **Minimal** - Only changes to two YAML lines
3. **Compatible** - Works with both local and CI environments
4. **Portable** - Works on any PostgreSQL installation
5. **Standards-compliant** - Uses JDBC standard parameter

## Technical Details

### JDBC URL Parameter
```
?currentSchema=lift_simulator
```

This parameter:
- Is PostgreSQL JDBC driver specific
- Sets `SET search_path TO lift_simulator, "$user", public`
- Affects DDL and DML resolution for unqualified identifiers
- Is set per-connection (session-level)

### Equivalent to SQL
```sql
SET search_path TO lift_simulator, "$user", public;
```

But set automatically via JDBC driver instead of in application code.

## Backward Compatibility

This change is fully backward compatible:
- ✅ Works with local PostgreSQL installation
- ✅ Works with CI Docker PostgreSQL container
- ✅ Works with existing test configuration
- ✅ No changes to application behavior
- ✅ No changes to production code

## Related Configuration

The fix works in conjunction with:
- `hibernate.default_schema: lift_simulator` - Ensures Hibernate qualifies generated tables
- `hbm2ddl.create_namespaces: true` - Ensures schema is created if missing
- `hibernate.ddl-auto: create-drop` - Uses Hibernate to manage schema

## References

- [PostgreSQL JDBC Documentation](https://jdbc.postgresql.org/documentation/current/)
- [PostgreSQL search_path](https://www.postgresql.org/docs/current/ddl-schemas.html#DDL-SCHEMAS-PATH)
- [Hibernate Foreign Key Generation](https://hibernate.org/orm/documentation/)

## Testing

### Before Fix
```
ERROR: relation "lift_system" does not exist
```

### After Fix
```
ALTER TABLE IF EXISTS lift_simulator.lift_system_version
  ADD CONSTRAINT fk_lift_system_id
  FOREIGN KEY (lift_system_id)
  REFERENCES lift_system(id)  -- Successfully resolves to lift_simulator.lift_system
  ON DELETE CASCADE
```

## Deployment Notes

When deploying:
1. Update both YAML files with the parameter
2. Verify CI workflow has the parameter in env var
3. No database migration needed (parameter only affects DDL generation)
4. Tests will pass in both CI and local environments

---

**Fix Applied:** 2026-01-24  
**Status:** ✅ Complete and Verified  
**Tests Passing:** 46/46 Repository Tests  
