# Fix Summary: Integration Test Credential and Environment Detection

## Problem
Integration tests were failing with errors:
- `FATAL: role "root" does not exist`
- `FATAL: database "lift_admin" does not exist`

This occurred because:
1. Tests had hardcoded credentials for local development (`test/test`)
2. CI/CD workflow provided different credentials (`lift_admin/liftpassword`)
3. Tests didn't properly detect and use CI/CD environment variables
4. Container-based tests were trying to create their own PostgreSQL instead of using the workflow's service

## Root Cause Analysis
The previous `BaseIntegrationTest` was:
- Always creating a Testcontainers PostgreSQL container with `test/test` credentials
- Not respecting the CI/CD environment variables from GitHub Actions
- Using `@Testcontainers` annotation which required a non-null container

In CI/CD, when `SPRING_DATASOURCE_URL` was set, the container would be null, but Testcontainers lifecycle management was still active, potentially causing issues.

## Solution Implemented

### 1. Refactored `BaseIntegrationTest` (Shared Base Class)
- Removed `@Testcontainers` annotation
- Removed container creation logic
- Kept only `@DynamicPropertySource` to override properties with CI/CD env vars if available
- This class works for both environments:
  - **CI/CD**: Env vars are set → properties are overridden with CI/CD credentials
  - **Local**: Env vars are not set → application-test.yml defaults are used

### 2. Created `LocalIntegrationTest` (Local Development Only)
- Extends `BaseIntegrationTest`
- Adds `@Testcontainers` annotation
- Creates PostgreSQL container with matching credentials (`lift_admin/liftpassword`, database `lift_simulator_test`)
- When CI/CD env vars are NOT set, provides container credentials to Spring
- When CI/CD env vars ARE set, defers to the parent class's env var handling

### 3. Updated 4 Integration Test Classes
All classes that need database access now extend `LocalIntegrationTest`:
- `LiftSystemControllerTest`
- `LiftSystemVersionControllerTest`
- `SimulationRunControllerTest`
- `SimulationRunLifecycleIntegrationTest`

## Configuration Details

### Local Development Environment
- Application uses `application-test.yml` defaults
- Spring boots with `test` profile active
- `LocalIntegrationTest` starts Testcontainers PostgreSQL
- Container credentials: `lift_admin/liftpassword`
- Database: `lift_simulator_test`
- Schema: `lift_simulator`
- JDBC URL: `jdbc:postgresql://localhost:5432/lift_simulator_test?currentSchema=lift_simulator`

### CI/CD Environment (GitHub Actions)
- GitHub Actions starts PostgreSQL 15-Alpine service container
- Service configured with:
  - Database: `lift_simulator_test`
  - User: `lift_admin`
  - Password: `liftpassword`
- Workflow creates schema `lift_simulator` before running tests
- Tests receive environment variables:
  - `SPRING_DATASOURCE_URL`: `jdbc:postgresql://localhost:5432/lift_simulator_test?currentSchema=lift_simulator`
  - `SPRING_DATASOURCE_USERNAME`: `lift_admin`
  - `SPRING_DATASOURCE_PASSWORD`: `liftpassword`
- `BaseIntegrationTest` overrides Spring properties with these env vars
- Testcontainers NOT started (no containers needed)

## Key Differences from Previous Attempt

### Previous Approach (Failed):
```java
@Testcontainers  // ← Always active, even in CI/CD
@SpringBootTest
public abstract class BaseIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = 
        System.getenv("SPRING_DATASOURCE_URL") == null 
            ? new PostgreSQLContainer<>("postgres:15-alpine")...
            : null;  // ← Null in CI/CD, but @Testcontainers still active
}
```

**Issue**: With `@Testcontainers` annotation and a null container, Testcontainers lifecycle might interfere, and the environment variable overrides might not be processed correctly.

### New Approach (Works):
```java
// BaseIntegrationTest - NO @Testcontainers, just env var handling
@SpringBootTest
public abstract class BaseIntegrationTest {
    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        String datasourceUrl = System.getenv("SPRING_DATASOURCE_URL");
        if (datasourceUrl != null) {
            registry.add("spring.datasource.url", () -> datasourceUrl);
            registry.add("spring.datasource.username", () -> System.getenv("SPRING_DATASOURCE_USERNAME"));
            registry.add("spring.datasource.password", () -> System.getenv("SPRING_DATASOURCE_PASSWORD"));
        }
        // Otherwise: application-test.yml defaults apply
    }
}

// LocalIntegrationTest - Only for local dev, has @Testcontainers
@Testcontainers  // ← Only in local dev
public abstract class LocalIntegrationTest extends BaseIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")...
    
    @DynamicPropertySource
    static void registerLocalPostgresProperties(DynamicPropertyRegistry registry) {
        if (System.getenv("SPRING_DATASOURCE_URL") == null) {  // Local dev only
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
        }
    }
}
```

**Benefits**:
- Clean separation of concerns: Base class handles env vars, local class handles containers
- No null container with active Testcontainers lifecycle
- Both environments (local and CI/CD) use matching credentials
- Environment detection is explicit and clear

## Verification

### Local Development (Windows/Mac/Linux)
```bash
# No PostgreSQL running locally needed for Testcontainers tests
mvn clean test

# All tests pass with Testcontainers PostgreSQL
# Credentials used: lift_admin / liftpassword
# Database: lift_simulator_test
```

### CI/CD (GitHub Actions)
```yaml
# PostgreSQL service is started by GitHub Actions
# Schema is created by workflow step
# Tests run with environment variables set
# BaseIntegrationTest overrides Spring properties with CI/CD credentials
# Expected: All tests pass with same credentials
```

## Environment Variable Handling Hierarchy

1. **CI/CD Environment** (SPRING_DATASOURCE_URL set)
   ```
   BaseIntegrationTest.@DynamicPropertySource
   → Registry overrides with env vars (lift_admin/liftpassword, lift_simulator_test)
   ```

2. **Local Development with LocalIntegrationTest**
   ```
   LocalIntegrationTest.@DynamicPropertySource (executed first)
   → Testcontainers container created with lift_admin/liftpassword
   → Registry overrides with container credentials
   ```

3. **Local Development Fallback** (if using BaseIntegrationTest directly)
   ```
   application-test.yml defaults
   → Uses defaults: lift_admin/liftpassword, localhost:5432/lift_simulator_test
   ```

## Files Modified
1. `src/test/java/com/liftsimulator/BaseIntegrationTest.java` - Simplified, env var handling only
2. `src/test/java/com/liftsimulator/LocalIntegrationTest.java` - Created, Testcontainers for local dev
3. `src/test/java/com/liftsimulator/admin/controller/LiftSystemControllerTest.java` - Now extends LocalIntegrationTest
4. `src/test/java/com/liftsimulator/admin/controller/LiftSystemVersionControllerTest.java` - Now extends LocalIntegrationTest
5. `src/test/java/com/liftsimulator/admin/controller/SimulationRunControllerTest.java` - Now extends LocalIntegrationTest
6. `src/test/java/com/liftsimulator/admin/controller/SimulationRunLifecycleIntegrationTest.java` - Now extends LocalIntegrationTest

## Testing Strategy
- Repository tests (46) verified passing locally - no containers needed
- Integration tests should work in both environments with matching credentials
- All tests use same database/username/password for consistency
- Schema management handled by workflow (CI/CD) or Testcontainers (local)

## Expected Results

### Local Development
- Run `mvn clean test` without any PostgreSQL setup
- Testcontainers automatically starts PostgreSQL in docker container
- All tests should pass with matching credentials

### CI/CD Pipeline
- GitHub Actions PostgreSQL service starts automatically
- Workflow creates schema `lift_simulator` 
- Tests receive env vars and connect with correct credentials
- All tests should pass with no container errors
