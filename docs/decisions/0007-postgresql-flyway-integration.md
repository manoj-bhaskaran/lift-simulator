# ADR-0007: PostgreSQL and Flyway Integration

**Date**: 2026-01-09

**Status**: Accepted

## Context

With the Spring Boot admin backend in place (ADR-0006), the Lift Config Service needs persistent storage to manage lift configurations, simulation runs, and related data. The backend requires:

1. Reliable persistent storage for configuration data
2. Version-controlled database schema management
3. Support for ACID transactions and data integrity
4. Ability to handle concurrent access from multiple users
5. Production-ready database suitable for deployment
6. Clear migration path from development to production environments
7. Database schema versioning synchronized with application releases

We need to select a database technology and migration strategy that balances simplicity for development with robustness for production use.

## Decision

We will use **PostgreSQL** as the primary database and **Flyway** for database migration management.

### Implementation Details

#### 1. Database: PostgreSQL

**Version**: PostgreSQL 12 or later

**Rationale**:
- Industry-standard relational database with proven reliability
- Excellent support for complex queries and data integrity constraints
- Strong Spring Boot and JPA/Hibernate integration
- Production-ready with robust transaction support
- Excellent tooling ecosystem (pgAdmin, psql, monitoring tools)
- Open-source with active community support
- Scales well from development to production

**Development Configuration**:
- Database: `lift_simulator`
- User: `lift_admin` with dedicated password
- Host: `localhost:5432`
- Connection pool: HikariCP (Spring Boot default)
  - Max pool size: 5 connections
  - Min idle: 2 connections
  - Connection timeout: 30 seconds

#### 2. Migration Framework: Flyway

**Version**: Flyway Core (Spring Boot managed version)

**Rationale**:
- SQL-first approach keeps migrations readable and database-agnostic
- Version-controlled schema evolution with automatic tracking
- Integrates seamlessly with Spring Boot auto-configuration
- Runs migrations automatically on application startup
- Provides safety checks (validation, checksums) to prevent inconsistencies
- Baseline-on-migrate feature simplifies initial setup
- Clear migration history in `flyway_schema_history` table

**Configuration**:
- Migration location: `src/main/resources/db/migration`
- Naming convention: `V{version}__{description}.sql` (e.g., `V1__init_schema.sql`)
- Baseline version: 0
- Baseline-on-migrate: enabled (for smooth initial setup)
- Validation: enabled on migrate
- Out-of-order migrations: disabled (enforces sequential application)

#### 3. ORM Layer: Spring Data JPA with Hibernate

**Configuration**:
- JPA provider: Hibernate (Spring Boot default)
- Dialect: `org.hibernate.dialect.PostgreSQLDialect`
- DDL auto: `validate` (production-safe, schema changes only via Flyway)
- Open-in-view: disabled (explicit transaction management)
- SQL logging: DEBUG in dev profile, INFO in production
- Batch processing: enabled (batch size 20)

**Rationale**:
- Spring Data JPA reduces boilerplate for repository layer
- Hibernate provides mature ORM with excellent PostgreSQL support
- `validate` DDL mode ensures schema changes are intentional (via Flyway)
- Disabling open-in-view promotes better transaction boundaries

#### 4. Profile-Based Configuration

**Base Configuration** (`application.properties`):
- Active profile: `dev` (default)
- JPA settings: `open-in-view=false`
- Flyway: `enabled=true`

**Development Profile** (`application-dev.yml`):
- PostgreSQL connection settings
- HikariCP connection pool tuning
- SQL logging enabled (DEBUG level)
- Hibernate SQL formatting and comments
- Development-friendly settings

**Future Profiles**:
- `test`: H2 in-memory database for integration tests
- `prod`: Production PostgreSQL with enhanced security and monitoring

#### 5. Initial Schema (V1)

**Tables**:
- `flyway_schema_history` (auto-created by Flyway)
  - Tracks all applied migrations with checksums and timestamps
- `schema_metadata`
  - Application version tracking
  - Schema change descriptions
  - Applied timestamps
  - Purpose: Correlate database schema version with application releases

**Design Principles**:
- Explicit tracking of schema versions alongside application versions
- Comprehensive table and column comments for documentation
- Indexed columns for common queries
- Baseline record inserted with version 0.22.0

## Consequences

### Positive

1. **Production-Ready Storage**:
   - ACID transactions ensure data integrity
   - Multi-user concurrent access supported out-of-the-box
   - Proven reliability in production environments

2. **Schema Version Control**:
   - Database schema is version-controlled alongside application code
   - Migration history is tracked automatically in `flyway_schema_history`
   - Easy to rollback or apply migrations in different environments
   - Clear audit trail of schema changes

3. **Developer Experience**:
   - Automatic migration on startup reduces manual steps
   - SQL-based migrations are readable and maintainable
   - pgAdmin and psql provide excellent tooling for database exploration
   - Profile-based configuration separates dev/test/prod settings

4. **Spring Boot Integration**:
   - Zero-configuration auto-setup with Spring Boot starters
   - Flyway runs automatically before application context initialization
   - HikariCP connection pooling is optimized by default
   - Actuator can expose database health metrics

5. **Future Extensibility**:
   - PostgreSQL supports advanced features (JSON columns, full-text search, triggers)
   - Easy to add database-backed features (audit trails, versioning, search)
   - Can leverage PostgreSQL extensions if needed (e.g., `pg_cron` for scheduling)

6. **Testing Support**:
   - Can use H2 in-memory database for fast integration tests
   - Test containers can spin up PostgreSQL for realistic testing
   - Flyway migrations run in test environment for schema parity

7. **Deployment Flexibility**:
   - PostgreSQL runs on all major platforms (Windows, Linux, macOS)
   - Cloud-ready (AWS RDS, Google Cloud SQL, Azure Database)
   - Can run locally for development, remotely for production
   - Docker support for containerized deployments

### Negative

1. **Setup Complexity**:
   - Developers must install and configure PostgreSQL locally
   - Requires database creation and user setup before first run
   - Network connectivity issues can block application startup
   - **Mitigation**: Comprehensive README with step-by-step setup instructions

2. **Dependency on External Service**:
   - Application cannot start without database connectivity
   - Database outages directly impact application availability
   - **Mitigation**: Connection pooling and retry logic can handle transient failures

3. **Migration Risks**:
   - Failed migrations can leave database in inconsistent state
   - Rollback is manual and requires careful planning
   - Schema changes require coordination across environments
   - **Mitigation**: Flyway validation and checksums detect inconsistencies; test migrations in dev first

4. **Resource Usage**:
   - PostgreSQL requires dedicated memory and disk space
   - Connection pooling holds database connections open
   - **Mitigation**: Tuned connection pool settings (5 max, 2 idle) for dev environments

5. **Learning Curve**:
   - Developers need basic PostgreSQL knowledge
   - Understanding Flyway migration conventions
   - JPA/Hibernate entity mapping and transaction management
   - **Mitigation**: Documentation in README and inline code comments

### Neutral

1. **Multiple Moving Parts**:
   - Three layers: PostgreSQL → Flyway → JPA/Hibernate
   - Each layer has its own configuration and logging
   - **Trade-off**: Complexity is offset by robustness and Spring Boot integration

2. **Schema Evolution Process**:
   - All schema changes must go through Flyway migrations
   - Cannot use Hibernate's `ddl-auto: update` (intentional for safety)
   - **Trade-off**: More discipline required, but prevents accidental schema changes

## Alternatives Considered

### Alternative 1: H2 Embedded Database

**Description**: H2 in-memory or file-based embedded database with Flyway

**Pros**:
- Zero external dependencies (embedded in JVM)
- Very fast for development and testing
- No installation required
- File-based or in-memory modes
- Spring Boot auto-configures easily

**Cons**:
- Less production-ready than PostgreSQL
- Limited concurrency support
- Fewer features compared to PostgreSQL
- Different SQL dialect may cause portability issues
- Not typically used in production for serious applications

**Why not chosen**: While H2 is excellent for testing, we want development and production environments to be as similar as possible. Using PostgreSQL from the start avoids dialect issues and ensures production parity. We may still use H2 for unit/integration tests.

### Alternative 2: SQLite

**Description**: SQLite file-based database with Flyway

**Pros**:
- Single-file database (portable, easy to back up)
- No server setup required
- Lightweight and fast
- Good for single-user scenarios
- Cross-platform support

**Cons**:
- Limited concurrency (single writer at a time)
- Fewer data types and features than PostgreSQL
- Not designed for high-concurrency web applications
- Limited transaction isolation levels
- Less robust tooling compared to PostgreSQL

**Why not chosen**: SQLite's concurrency limitations make it unsuitable for a multi-user admin backend. While simpler to set up, it would require a migration to PostgreSQL later as the application grows.

### Alternative 3: MySQL/MariaDB

**Description**: MySQL or MariaDB with Flyway

**Pros**:
- Widely used and well-supported
- Good Spring Boot integration
- Mature ecosystem and tooling
- Cloud provider support (AWS RDS, etc.)

**Cons**:
- PostgreSQL has better standards compliance
- PostgreSQL has superior JSON support and advanced features
- MariaDB fork introduces compatibility considerations
- No significant advantages over PostgreSQL for this use case

**Why not chosen**: PostgreSQL offers better advanced features (JSON, full-text search, window functions) and stricter SQL standards compliance. Both are production-ready, but PostgreSQL has the edge for modern applications.

### Alternative 4: NoSQL (MongoDB, Redis)

**Description**: Document store (MongoDB) or key-value store (Redis) instead of relational database

**Pros**:
- Schema flexibility
- Horizontal scaling capabilities
- Fast for specific use cases (Redis for caching)
- JSON-native storage (MongoDB)

**Cons**:
- Overkill for structured configuration data
- Lose ACID guarantees (in some cases)
- Less mature Spring Data integration for configuration management
- Harder to query relationally (lift configs, runs, users, permissions)
- No standard SQL for queries

**Why not chosen**: Lift configurations are inherently relational (users, permissions, config versions, simulation runs). A relational database is the natural fit. NoSQL databases solve problems we don't have (massive scale, unstructured data).

### Alternative 5: Liquibase (instead of Flyway)

**Description**: Use Liquibase for database migrations instead of Flyway

**Pros**:
- XML/YAML/JSON migration formats (database-agnostic)
- More advanced rollback capabilities
- Change set tagging and conditional execution
- Preconditions and context filtering

**Cons**:
- More complex configuration
- XML/YAML less readable than SQL for developers
- Steeper learning curve
- Spring Boot has first-class Flyway support

**Why not chosen**: Flyway's SQL-first approach is simpler and more readable. For this project, we don't need Liquibase's advanced features (complex rollbacks, multi-database support). Flyway integrates seamlessly with Spring Boot and keeps migrations in plain SQL.

## Implementation Notes

### Migration Workflow

1. **Creating a New Migration**:
   - Create new SQL file in `src/main/resources/db/migration/`
   - Name format: `V{version}__{description}.sql` (double underscore)
   - Example: `V2__add_lift_config_table.sql`
   - Write SQL DDL statements (CREATE TABLE, ALTER TABLE, etc.)

2. **Testing Migration**:
   - Run application locally - Flyway applies migration automatically
   - Verify schema: `psql -U lift_admin -d lift_simulator -c "\d"`
   - Check migration history: `SELECT * FROM flyway_schema_history;`

3. **Migration Best Practices**:
   - One logical change per migration
   - Include comments in SQL for context
   - Test migration on clean database
   - Never modify applied migrations (Flyway checksums will fail)
   - For data migrations, include both schema and data changes

### Database Setup Script

Developers can use this script to set up PostgreSQL:

```bash
# Start PostgreSQL
sudo service postgresql start

# Create database and user
sudo -u postgres psql <<EOF
CREATE DATABASE lift_simulator;
CREATE USER lift_admin WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE lift_simulator TO lift_admin;
\c lift_simulator
GRANT ALL ON SCHEMA public TO lift_admin;
EOF
```

### Profile Usage

**Development (default)**:
```bash
mvn spring-boot:run
# Uses application-dev.yml automatically
```

**Custom Profile**:
```bash
SPRING_PROFILES_ACTIVE=prod mvn spring-boot:run
```

### Troubleshooting

**Connection Refused**:
- Check PostgreSQL is running: `sudo service postgresql status`
- Verify port 5432 is open: `netstat -tuln | grep 5432`
- Check connection settings in `application-dev.yml`

**Migration Failures**:
- Check Flyway logs in console output
- Query migration history: `SELECT * FROM flyway_schema_history;`
- For development, reset database: `DROP DATABASE lift_simulator; CREATE DATABASE lift_simulator;`
- Never modify applied migrations; create new corrective migration instead

**Permission Errors**:
- Ensure user has schema permissions: `GRANT ALL ON SCHEMA public TO lift_admin;`
- Check table-level permissions if needed: `GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO lift_admin;`

## References

- PostgreSQL Documentation: https://www.postgresql.org/docs/
- Flyway Documentation: https://flywaydb.org/documentation/
- Spring Boot Database Initialization: https://docs.spring.io/spring-boot/docs/current/reference/html/howto.html#howto.data-initialization
- Spring Data JPA: https://docs.spring.io/spring-data/jpa/docs/current/reference/html/
- HikariCP (Connection Pooling): https://github.com/brettwooldridge/HikariCP
- ADR-0006: Spring Boot Admin Backend (backend architecture)

## Future Considerations

1. **Production Configuration**:
   - Create `application-prod.yml` with production-specific settings
   - Environment variables for sensitive credentials
   - SSL/TLS for database connections
   - Read replicas for scaling read operations
   - Connection pool tuning for production load

2. **Advanced Features**:
   - Database audit trails using PostgreSQL triggers
   - JSON columns for flexible configuration storage
   - Full-text search for configuration discovery
   - Partitioning for large historical data
   - Point-in-time recovery backups

3. **Testing Strategy**:
   - H2 in-memory database for unit tests
   - Testcontainers for PostgreSQL integration tests
   - Separate test profile (`application-test.yml`)
   - Test data fixtures using Flyway callbacks or test-specific migrations

4. **Monitoring and Operations**:
   - Spring Boot Actuator database health checks
   - Connection pool metrics (via HikariCP MBean)
   - Slow query logging and analysis
   - Database performance monitoring (pg_stat_statements)
   - Automated backups and restore procedures
