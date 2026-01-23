# Testing Architecture: Local Development vs CI/CD

## Quick Reference

### For Local Development
Use `LocalIntegrationTest` as your base class:
```java
public class MyIntegrationTest extends LocalIntegrationTest {
    // Testcontainers PostgreSQL starts automatically
    // Credentials: lift_admin/liftpassword
    // Database: lift_simulator_test
}
```

### For CI/CD (GitHub Actions)
Tests automatically detect GitHub Actions environment:
- Env vars set: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `BaseIntegrationTest` overrides Spring properties with these values
- No Testcontainers containers are started
- Tests connect to GitHub Actions PostgreSQL service

## Architecture

### Class Hierarchy
```
BaseIntegrationTest (abstract)
├── @SpringBootTest
├── @ActiveProfiles("test")
└── @DynamicPropertySource (checks for CI/CD env vars)
    │
    └─→ LocalIntegrationTest (abstract)
        ├── @Testcontainers (local dev only)
        ├── @Container PostgreSQLContainer
        └── @DynamicPropertySource (registers container credentials)
            │
            └─→ LiftSystemControllerTest
            └─→ LiftSystemVersionControllerTest
            └─→ SimulationRunControllerTest
            └─→ SimulationRunLifecycleIntegrationTest
```

## Configuration Stacking

### Local Development Environment
1. **Default** (from `application-test.yml`)
   - URL: `jdbc:postgresql://localhost:5432/lift_simulator_test?currentSchema=lift_simulator`
   - Username: `lift_admin`
   - Password: `liftpassword`

2. **BaseIntegrationTest.@DynamicPropertySource**
   - Checks: Is `SPRING_DATASOURCE_URL` env var set?
   - Local: No → Properties remain as defaults

3. **LocalIntegrationTest.@DynamicPropertySource** (ONLY if extending LocalIntegrationTest)
   - Checks: Is `SPRING_DATASOURCE_URL` env var set?
   - Local: No → Overrides with Testcontainers credentials
   - Testcontainers credentials: exactly same as defaults!

**Result**: Seamless transition from defaults to Testcontainers with matching credentials

### CI/CD Environment (GitHub Actions)
1. **Default** (from `application-test.yml`)
   - (Same as local)

2. **BaseIntegrationTest.@DynamicPropertySource**
   - Checks: Is `SPRING_DATASOURCE_URL` env var set?
   - CI/CD: YES → Overrides with env var values
   - URL: `jdbc:postgresql://localhost:5432/lift_simulator_test?currentSchema=lift_simulator`
   - Username: `lift_admin`
   - Password: `liftpassword`

3. **LocalIntegrationTest.@DynamicPropertySource** (NOT CALLED in CI/CD)
   - CI/CD: `SPRING_DATASOURCE_URL` is set, so this is skipped
   - @Container is still evaluated but not started (Testcontainers sees env vars)

**Result**: Environment variables override all defaults, Testcontainers not started

## Credential Consistency

All environments use the SAME credentials:
- **Username**: `lift_admin`
- **Password**: `liftpassword`
- **Database**: `lift_simulator_test`
- **Schema**: `lift_simulator`
- **JDBC URL**: `jdbc:postgresql://localhost:5432/lift_simulator_test?currentSchema=lift_simulator`

This consistency ensures:
- Tests behave identically in all environments
- No environment-specific credential errors
- Easy debugging and troubleshooting

## Running Tests

### Local Development
```bash
# For all tests
mvn clean test

# For only repository tests
mvn clean test -Dtest="*RepositoryTest"

# For only integration tests
mvn clean test -Dtest="*ControllerTest"

# For a specific test
mvn clean test -Dtest="LiftSystemControllerTest"
```

**Requirements**:
- Docker or Podman (for Testcontainers)
- Java 17+
- Maven 3.6+
- No PostgreSQL needed locally!

### CI/CD (GitHub Actions)
```yaml
# PostgreSQL service automatically started
# Schema created by workflow step
# Tests run with environment variables set

- name: Run tests
  run: mvn clean test jacoco:report
  env:
    SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/lift_simulator_test?currentSchema=lift_simulator
    SPRING_DATASOURCE_USERNAME: lift_admin
    SPRING_DATASOURCE_PASSWORD: liftpassword
```

## Property Resolution Order (Spring Boot)

1. **Environment Variables** (Highest priority in CI/CD)
   ```
   SPRING_DATASOURCE_URL
   SPRING_DATASOURCE_USERNAME
   SPRING_DATASOURCE_PASSWORD
   ```

2. **@DynamicPropertySource** (Processed by BaseIntegrationTest, then LocalIntegrationTest)
   ```
   if (SPRING_DATASOURCE_URL != null) {
       // CI/CD: Use env vars (from BaseIntegrationTest)
   } else if (!LocalIntegrationTest && hasTestcontainers) {
       // Local: Use Testcontainers (from LocalIntegrationTest)
   } else {
       // Fallback: Use application-test.yml defaults
   }
   ```

3. **application-test.yml** (Lowest priority)
   ```yaml
   spring:
     datasource:
       url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/lift_simulator_test?currentSchema=lift_simulator}
       username: ${SPRING_DATASOURCE_USERNAME:lift_admin}
       password: ${SPRING_DATASOURCE_PASSWORD:liftpassword}
   ```

## Troubleshooting

### Error: "FATAL: role 'root' does not exist"
**Cause**: Tests using wrong credentials
**Fix**: Ensure test class extends `LocalIntegrationTest` (not `BaseIntegrationTest`)
**Verification**:
```bash
# In test class hierarchy
public class MyTest extends LocalIntegrationTest {  // ✓ Correct
// not: public class MyTest extends BaseIntegrationTest {  // ✗ Wrong
```

### Error: "FATAL: database 'lift_admin' does not exist"
**Cause**: Connection string has wrong database name
**Fix**: Verify JDBC URL has correct database:
```properties
# ✓ Correct
jdbc:postgresql://localhost:5432/lift_simulator_test?currentSchema=lift_simulator

# ✗ Wrong
jdbc:postgresql://localhost:5432/lift_admin?currentSchema=lift_simulator
```

### Error: "Testcontainers could not find a running Docker daemon"
**Cause**: Docker/Podman not running
**Fix**: Start Docker before running tests
```bash
# On Windows (Docker Desktop)
# Start Docker Desktop from Applications

# On Linux
sudo systemctl start docker

# On Mac
# Docker Desktop starts automatically, or
open /Applications/Docker.app
```

### Error: "Connection refused: localhost:5432"
**Cause**: Tests extending `BaseIntegrationTest` directly without Docker
**Fix**: Either:
1. Extend `LocalIntegrationTest` instead (uses Testcontainers)
2. OR start PostgreSQL locally on port 5432
3. OR use `SPRING_DATASOURCE_URL` env var to point to existing database

### Tests pass locally but fail in CI/CD
**Cause**: Env vars not being passed to Maven
**Fix**: Check GitHub Actions workflow file:
```yaml
- name: Run tests
  run: mvn clean test
  env:
    SPRING_DATASOURCE_URL: ...
    SPRING_DATASOURCE_USERNAME: ...
    SPRING_DATASOURCE_PASSWORD: ...
```

### Tests hang or timeout
**Cause**: Testcontainers waiting for Docker, or database connection timeout
**Fix**:
1. Check Docker is running: `docker ps`
2. Increase timeout in application-test.yml: `connection-timeout: 30000`
3. Check if GitHub Actions PostgreSQL service is healthy in workflow logs

## IDE Integration

### IntelliJ IDEA / JetBrains IDEs
```
Run → Edit Configurations → Add new JUnit Configuration
├── Class: com.liftsimulator.admin.controller.LiftSystemControllerTest
├── VM Options: (leave empty)
└── Environment variables:
    └── (leave empty for local, or add for CI/CD simulation)
```

### VS Code
```json
{
    "configurations": [{
        "name": "Run Integration Tests",
        "type": "java",
        "name": "Run Integration Tests",
        "request": "launch",
        "mainClass": "org.junit.runner.JUnitCore",
        "args": "com.liftsimulator.admin.controller.LiftSystemControllerTest"
    }]
}
```

### Eclipse
```
Run → Run Configurations → JUnit
├── Test class: com.liftsimulator.admin.controller.LiftSystemControllerTest
└── VM Arguments: (none needed)
```

## Best Practices

### When Creating New Integration Tests
1. **Always extend `LocalIntegrationTest`** (not `BaseIntegrationTest`)
   ```java
   public class MyNewIntegrationTest extends LocalIntegrationTest {
       // Testcontainers will work in local dev
       // CI/CD will use GitHub Actions service
   }
   ```

2. **Use `@Transactional` for test isolation**
   ```java
   @Transactional
   @Test
   void testSomething() {
       // Database changes are rolled back after test
   }
   ```

3. **Never hardcode database credentials**
   ```java
   // ✗ Wrong
   String url = "jdbc:postgresql://localhost:5432/lift_simulator_test";
   
   // ✓ Correct - Use configuration
   // Credentials come from application-test.yml + DynamicPropertySource
   ```

4. **Use TestContainers for complex database setups**
   ```java
   // For tests needing specific database state:
   // Create helper methods in LocalIntegrationTest subclasses
   protected void setupTestData() { ... }
   ```

### When Debugging Tests
1. **Check which credentials are being used**
   ```bash
   mvn clean test -Dtest="MyTest" -X 2>&1 | grep -i datasource
   ```

2. **Verify environment variables are set (CI/CD)**
   ```yaml
   - name: Debug environment
     run: env | grep SPRING_DATASOURCE
   ```

3. **Check Testcontainers logs (local)**
   ```bash
   mvn clean test -Dtest="MyTest" | grep -i testcontainers
   ```

## Migration Guide

### Migrating Existing Tests from BaseIntegrationTest
If you have tests extending `BaseIntegrationTest` that need a database:

**Before**:
```java
public class MyTest extends BaseIntegrationTest {
    // Expects PostgreSQL running on localhost:5432
}
```

**After**:
```java
public class MyTest extends LocalIntegrationTest {
    // Testcontainers starts PostgreSQL automatically
}
```

**No other changes needed!** The rest of your test code remains identical.

## Performance Considerations

### Testcontainers Startup Time (Local)
- First run: ~10-15 seconds (pulling Docker image)
- Subsequent runs: ~2-3 seconds (container already cached)
- Parallel test execution: Testcontainers reuses single container for all tests in class

### CI/CD Performance
- GitHub Actions PostgreSQL: Already running
- No container startup time
- Tests start immediately after schema creation (~5 seconds)

## References
- [Testcontainers Documentation](https://www.testcontainers.org/)
- [Spring Boot Testing Guide](https://spring.io/guides/gs/testing-web/)
- [PostgreSQL Docker Image](https://hub.docker.com/_/postgres)
- [GitHub Actions PostgreSQL Service](https://docs.github.com/en/actions/using-containerized-services/creating-postgresql-service-containers)
