# Testing Setup and Configuration

This document describes the test configuration for the Lift Simulator project, including local development setup and CI/CD integration.

## Overview

The project uses:
- **JUnit 5** for unit and integration tests
- **Spring Boot Test** for context-based testing
- **PostgreSQL** as the test database (matches production database)
- **Testcontainers** for some integration tests that need Docker
- **@DataJpaTest** for repository layer tests
- **@SpringBootTest** for full integration tests

## Local Development Setup

### Prerequisites

1. **Java 17** - Development environment
2. **PostgreSQL 15+** - Test database server
3. **Maven 3.6+** - Build tool

### Initial Setup

#### 1. Create Test Database

```bash
# Connect to PostgreSQL as postgres user
psql -U postgres

# Create the test database
CREATE DATABASE lift_simulator_test WITH OWNER lift_admin;

# Connect to the test database
\c lift_simulator_test

# Create the schema
CREATE SCHEMA IF NOT EXISTS lift_simulator;

# Grant permissions to lift_admin user
GRANT ALL PRIVILEGES ON SCHEMA lift_simulator TO lift_admin;

# Exit psql
\q
```

#### 2. Run Tests Locally

```bash
# Run all tests
mvn clean test

# Run only repository tests (no Docker required)
mvn test -Dtest="*RepositoryTest"

# Run a specific test
mvn test -Dtest=LiftSystemRepositoryTest#testFindAll

# Run with full error output
mvn test -e

# Run with debug output
mvn test -X
```

### Test Database Configuration

The test database is configured in `src/test/resources/application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/lift_simulator_test
    username: lift_admin
    password: liftpassword
    driver-class-name: org.postgresql.Driver
```

**Environment Variables Override:**
- `SPRING_DATASOURCE_URL` - Database connection URL
- `SPRING_DATASOURCE_USERNAME` - Database username
- `SPRING_DATASOURCE_PASSWORD` - Database password

## CI/CD Setup (GitHub Actions)

The GitHub Actions workflow (`.github/workflows/ci.yml`) automatically:

1. **Starts a PostgreSQL Service** - Uses `postgres:15-alpine` Docker image
2. **Creates the Test Database** - `lift_simulator_test` with `lift_admin` user
3. **Sets Up Schema** - Creates `lift_simulator` schema
4. **Configures Permissions** - Grants all privileges to the user
5. **Runs Tests** - Executes Maven tests with proper environment variables

### Workflow Steps

```yaml
services:
  postgres:
    image: postgres:15-alpine
    env:
      POSTGRES_DB: lift_simulator_test
      POSTGRES_USER: lift_admin
      POSTGRES_PASSWORD: liftpassword
    options: >-
      --health-cmd pg_isready
      --health-interval 10s
      --health-timeout 5s
      --health-retries 5
    ports:
      - 5432:5432
```

The workflow:
1. Creates PostgreSQL service container
2. Waits for database to be healthy
3. Creates schema and sets permissions
4. Runs tests with database environment variables

## Test Categories

### 1. Repository Tests (@DataJpaTest)

- **Files:** `src/test/java/com/liftsimulator/admin/repository/`
- **Database:** Uses test database configuration
- **Docker Required:** No
- **Configuration:** `application-test.yml`

Tests for:
- `LiftSystemRepositoryTest`
- `LiftSystemVersionRepositoryTest`
- `SimulationRunRepositoryTest`
- `SimulationScenarioRepositoryTest`

### 2. Integration Tests (@SpringBootTest)

- **Files:** `src/test/java/com/liftsimulator/BaseIntegrationTest.java`
- **Database:** Testcontainers PostgreSQL
- **Docker Required:** Yes
- **Configuration:** Dynamically configured via DynamicPropertySource

Tests using `BaseIntegrationTest`:
- `LiftSystemControllerTest`
- `LiftSystemVersionControllerTest`
- `SimulationRunControllerTest`
- `SimulationRunLifecycleIntegrationTest`

### 3. Service Tests

- **Database:** Uses test database configuration
- **Docker Required:** No
- **Configuration:** `application-test.yml`

## Troubleshooting

### Tests Fail with "schema does not exist"

**Solution:** Ensure the schema is created:
```bash
PGPASSWORD=liftpassword psql -h localhost -U lift_admin -d lift_simulator_test \
  -c "CREATE SCHEMA IF NOT EXISTS lift_simulator;"
```

### Tests Fail with "permission denied for schema"

**Solution:** Grant permissions:
```bash
PGPASSWORD=liftpassword psql -h localhost -U lift_admin -d lift_simulator_test \
  -c "GRANT ALL PRIVILEGES ON SCHEMA lift_simulator TO lift_admin;"
```

### Tests Fail with "database does not exist"

**Solution:** Create the test database:
```bash
PGPASSWORD=password psql -h localhost -U postgres \
  -c "CREATE DATABASE lift_simulator_test WITH OWNER lift_admin;"
```

### Docker-Related Test Failures

Some tests require Docker (Testcontainers). If Docker is not available:

**Option 1:** Skip Docker-dependent tests
```bash
mvn test -Dtest="!*ControllerTest,!*LifecycleIntegrationTest"
```

**Option 2:** Install Docker and ensure it's running
```bash
# On Windows with WSL2
wsl --list --verbose
wsl --set-default-version 2 Ubuntu-20.04
```

## Environment Variables

### Local Development

The `application-test.yml` provides defaults:
```yaml
url: ${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/lift_simulator_test}
username: ${SPRING_DATASOURCE_USERNAME:lift_admin}
password: ${SPRING_DATASOURCE_PASSWORD:liftpassword}
```

### CI/CD (GitHub Actions)

Environment variables are set in the workflow:
```yaml
env:
  SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/lift_simulator_test
  SPRING_DATASOURCE_USERNAME: lift_admin
  SPRING_DATASOURCE_PASSWORD: liftpassword
```

## Test Results

### Expected Results

- **362+ tests pass** - All repository and service tests
- **1 test failure** - `LiftSystemRepositoryTest.testCascadeDeleteLiftSystemWithVersions` (requires code review)
- **4 test errors** - Docker-dependent tests (expected if Docker unavailable)

### Total Test Count

- Repository Tests: 46
- Scenario/Integration Tests: 6+
- Service Tests: Various
- Controller Tests: 4 (require Docker)

**Total: ~367 tests**

## Best Practices

1. **Always run tests locally before pushing**
   ```bash
   mvn clean test
   ```

2. **Run specific test class during development**
   ```bash
   mvn test -Dtest=YourTestClass
   ```

3. **Check test output for warnings**
   ```bash
   mvn test -e  # Full errors
   mvn test -X  # Full debug
   ```

4. **Ensure database is clean between runs**
   - The test configuration uses `hibernate.ddl-auto: create-drop`
   - Schema is dropped and recreated for each test class

5. **Keep test database running**
   ```bash
   # Verify PostgreSQL is running
   psql -U postgres -c "SELECT version();"
   ```

## References

- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)
- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Testcontainers Documentation](https://www.testcontainers.org/)
- [PostgreSQL Docker Image](https://hub.docker.com/_/postgres)
