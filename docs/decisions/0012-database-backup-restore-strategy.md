# ADR-0012: Database Backup and Restore Strategy

**Date**: 2026-01-13

**Status**: Accepted

## Context

The lift simulator uses a locally hosted PostgreSQL database (`lift_simulator`) to store configuration data (ADR-0007). This configuration data is critical for the operation of the system and represents:

1. Lift system definitions and metadata
2. Versioned configuration payloads with published states
3. Historical configuration data for audit and rollback purposes

Unlike application code, which is version-controlled in Git, database content is dynamic and not committed to version control. Loss of this data would require manual recreation of all lift system configurations.

We need a documented backup and restore process that:
- Protects against data loss from hardware failure, operator error, or corruption
- Enables recovery on the same machine or migration to a new machine
- Is simple enough for individual developers to execute manually when needed
- Integrates with existing automation infrastructure for scheduled backups
- Supports both manual ad-hoc backups and automated scheduled backups
- Does not interfere with normal application operation

## Decision

We will use **pg_dump** and **pg_restore** for database backup and restore, with documentation-only integration to an existing external automation repository.

### Implementation Details

#### 1. Backup Strategy

**Tool**: PostgreSQL's `pg_dump` utility

**Rationale**:
- Native PostgreSQL tool, no additional software required
- Supports online backups (no application downtime required)
- Creates consistent point-in-time snapshots
- Produces portable SQL dumps that can be restored to any PostgreSQL instance
- Handles schema and data together
- Widely understood and documented tool

**Backup Format**: Plain SQL format (human-readable)

**Backup Scope**: Full database backup (schema + data)

#### 2. Manual Ad-Hoc Backup

For immediate, on-demand backups, developers can execute:

**Linux/macOS:**
```bash
pg_dump -h localhost -U lift_admin -d lift_simulator -F p -f lift_simulator_backup_$(date +%Y%m%d_%H%M%S).sql
```

**Windows (Command Prompt):**
```cmd
pg_dump -h localhost -U lift_admin -d lift_simulator -F p -f lift_simulator_backup_%date:~-4,4%%date:~-10,2%%date:~-7,2%_%time:~0,2%%time:~3,2%%time:~6,2%.sql
```

**Windows (PowerShell):**
```powershell
pg_dump -h localhost -U lift_admin -d lift_simulator -F p -f "lift_simulator_backup_$(Get-Date -Format 'yyyyMMdd_HHmmss').sql"
```

**Parameters**:
- `-h localhost`: Database host
- `-U lift_admin`: Database user
- `-d lift_simulator`: Database name
- `-F p`: Plain SQL format (human-readable)
- `-f`: Output file with timestamp

**When to use**:
- Before major schema migrations
- Before bulk data updates or deletions
- Before system upgrades
- When preparing to test risky operations

#### 3. Automated Scheduled Backup

**Automation Method**: External automation via PowerShell script in My-Scripts repository

**Schedule**: Every Tuesday at 8:00 a.m. (Windows Task Scheduler)

**Script Location**: `My-Scripts/src/powershell/backup/Backup-LiftSimulatorDatabase.ps1`

**Command**:
```powershell
pwsh -File "C:\Users\manoj\Documents\Scripts\src\powershell\backup\Backup-LiftSimulatorDatabase.ps1"
```

**Note**: The path above is a local example; implementations may vary per environment.

**Backup Storage**:
- Backups: `D:\pgbackup\lift_simulator`
- Logs: `D:\pgbackup\lift_simulator\logs`
- Retention policy: Managed by backup script (not in this repository)

**Rationale for External Automation**:
- My-Scripts repository already handles backup automation for multiple projects
- Centralized credential management and error handling
- Task Scheduler configuration managed externally
- Separation of concerns: this repository documents the process, automation repository handles execution

#### 4. Restore Procedure

**Standard Restore** (to existing database):

1. Stop the application to prevent writes during restore

2. Drop and recreate the database:

   **Linux/macOS:**
   ```bash
   sudo -u postgres psql -c "DROP DATABASE lift_simulator;"
   sudo -u postgres psql -c "CREATE DATABASE lift_simulator;"
   sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE lift_simulator TO lift_admin;"
   ```

   **Windows:**
   ```cmd
   psql -U postgres -c "DROP DATABASE lift_simulator;"
   psql -U postgres -c "CREATE DATABASE lift_simulator;"
   psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE lift_simulator TO lift_admin;"
   ```

3. Restore from backup:

   **Linux/macOS:**
   ```bash
   psql -h localhost -U lift_admin -d lift_simulator -f lift_simulator_backup_YYYYMMDD_HHMMSS.sql
   ```

   **Windows:**
   ```cmd
   psql -h localhost -U lift_admin -d lift_simulator -f lift_simulator_backup_YYYYMMDD_HHMMSS.sql
   ```

4. Verify restore:

   **Linux/macOS:**
   ```bash
   psql -h localhost -U lift_admin -d lift_simulator -c "\dt"
   psql -h localhost -U lift_admin -d lift_simulator -c "SELECT COUNT(*) FROM lift_system;"
   ```

   **Windows:**
   ```cmd
   psql -h localhost -U lift_admin -d lift_simulator -c "\dt"
   psql -h localhost -U lift_admin -d lift_simulator -c "SELECT COUNT(*) FROM lift_system;"
   ```

5. Restart the application

**Clean Restore** (to new machine or fresh install):

1. Install PostgreSQL 12 or later
2. Create database and user as documented in README (Database Setup section)
3. Restore from backup (step 3 above)
4. Verify restore (step 4 above)
5. Start the application

**Selective Restore** (individual tables):

```bash
# Extract schema only
pg_restore --schema-only -d lift_simulator backup.sql

# Restore specific table
pg_restore -t lift_system -d lift_simulator backup.sql
```

#### 5. Backup Verification

To ensure backups are valid:

1. **Immediate verification**: Check backup file size and format

   **Linux/macOS:**
   ```bash
   ls -lh lift_simulator_backup_*.sql
   head -n 20 lift_simulator_backup_*.sql  # Should show valid SQL
   ```

   **Windows (PowerShell):**
   ```powershell
   Get-ChildItem lift_simulator_backup_*.sql | Format-Table Name, Length, LastWriteTime
   Get-Content lift_simulator_backup_*.sql -Head 20  # Should show valid SQL
   ```

2. **Periodic restore testing**: Restore to a test database quarterly

   **Linux/macOS:**
   ```bash
   createdb lift_simulator_test
   psql -U lift_admin -d lift_simulator_test -f backup.sql
   psql -U lift_admin -d lift_simulator_test -c "\dt"
   dropdb lift_simulator_test
   ```

   **Windows:**
   ```cmd
   createdb lift_simulator_test
   psql -U lift_admin -d lift_simulator_test -f backup.sql
   psql -U lift_admin -d lift_simulator_test -c "\dt"
   dropdb lift_simulator_test
   ```

3. **Automated verification**: Backup script logs success/failure

#### 6. Documentation Integration

**This Repository**:
- README: Backup and restore procedures for developers
- ADR-0012: Architectural rationale and design decisions
- Database Setup section: References backup documentation

**My-Scripts Repository**:
- `src/powershell/backup/Backup-LiftSimulatorDatabase.ps1`: Backup automation script
- `src/powershell/backup/README-LiftSimulator.md`: Setup and configuration instructions
- `src/powershell/backup/README.md`: General backup template documentation

**Separation of Concerns**:
- This repository: What to back up, how to restore, manual procedures
- My-Scripts repository: How to automate backups, scheduling, credentials, logs

## Consequences

### Positive

1. **Data Protection**:
   - Configuration data is protected against hardware failure and operator error
   - Point-in-time recovery enables rollback to known good states
   - Scheduled backups reduce risk of significant data loss

2. **Disaster Recovery**:
   - Clear restore procedure enables recovery on the same or different machine
   - Plain SQL format is portable across PostgreSQL versions and platforms
   - Restore process is well-documented and testable

3. **Developer Confidence**:
   - Developers can safely test risky operations knowing they can restore
   - Ad-hoc backup procedure is simple and quick to execute
   - No specialized tools or knowledge required beyond pg_dump/pg_restore

4. **Operational Simplicity**:
   - Online backups mean no application downtime for scheduled backups
   - Integration with existing automation infrastructure (My-Scripts)
   - Leverages native PostgreSQL tools (no third-party dependencies)

5. **Separation of Concerns**:
   - This repository documents the database and backup/restore procedures
   - Automation repository handles scheduling, credentials, and execution
   - Clear boundaries between application and operations concerns

6. **Compliance and Audit**:
   - Backup logs provide audit trail of backup executions
   - Scheduled backups demonstrate due diligence for data protection
   - Restore procedures are documented and verifiable

### Negative

1. **Manual Setup Required**:
   - Developers must configure Task Scheduler and backup script manually
   - Initial setup requires access to My-Scripts repository
   - Backup paths may vary across environments
   - **Mitigation**: Comprehensive documentation in both repositories

2. **External Dependency**:
   - Backup automation depends on My-Scripts repository availability
   - Task Scheduler configuration is external to this repository
   - Backup script updates happen in a separate repository
   - **Mitigation**: Manual backup procedure always available as fallback

3. **No Built-In Retention Management**:
   - Old backups are not automatically cleaned up (managed by external script)
   - Disk space management is manual or external
   - **Mitigation**: Backup script in My-Scripts handles retention policy

4. **Limited Backup Verification**:
   - No automated restore testing in CI/CD pipeline
   - Backup validity is checked by script, but restore testing is manual
   - **Mitigation**: Document periodic restore testing procedure

5. **Recovery Time**:
   - Full database restore takes time proportional to database size
   - Application must be stopped during restore (downtime)
   - **Mitigation**: For small configuration databases, restore is fast (seconds to minutes)

6. **Point-in-Time Recovery Limitations**:
   - Backup captures state at backup time only (no continuous archiving)
   - Changes made between last backup and failure are lost
   - **Mitigation**: Weekly backup schedule limits data loss window to 7 days

### Neutral

1. **Backup Storage**:
   - Backups are stored on local disk (D: drive)
   - No cloud or remote backup storage
   - **Trade-off**: Simpler setup, but requires manual off-site backup for disaster recovery

2. **Backup Format**:
   - Plain SQL format is human-readable but larger than compressed formats
   - **Trade-off**: Readability and portability vs. storage efficiency

3. **Coordination with Schema Migrations**:
   - Backups include schema version from Flyway
   - Restoring to different schema version requires manual migration
   - **Trade-off**: Simple backup, but cross-version restore requires care

## Alternatives Considered

### Alternative 1: Continuous WAL Archiving (Point-in-Time Recovery)

**Description**: Configure PostgreSQL WAL (Write-Ahead Log) archiving for continuous backup

**Pros**:
- Enables point-in-time recovery to any moment (not just backup time)
- Minimizes data loss window (seconds instead of days)
- Supports warm standby and replication
- Industry standard for production databases

**Cons**:
- Significantly more complex to set up and manage
- Requires continuous archiving process running
- More disk space for WAL segments
- Overkill for a local development database
- Requires specialized PostgreSQL administration knowledge

**Why not chosen**: The lift simulator is primarily a development project with locally stored configurations. The complexity of WAL archiving is not justified for this use case. Weekly full backups provide sufficient protection for configuration data that changes infrequently.

### Alternative 2: Application-Level Backup (Export to JSON/YAML)

**Description**: Build backup/restore feature into the application that exports configurations to JSON

**Pros**:
- No database tools required
- Portable across different database types
- Can be triggered from admin UI
- Version-controlled backup format
- Selective backup of specific configurations

**Cons**:
- Requires development effort to build and maintain
- Only backs up application data (not database schema or Flyway history)
- Incompatible with direct database restore
- Custom format requires custom restore logic
- Does not capture database-level metadata

**Why not chosen**: Building a custom backup/restore feature would be significant development effort for a problem that pg_dump already solves perfectly. Native PostgreSQL tools are mature, well-tested, and handle edge cases we might miss in custom code. Focus development effort on application features instead.

### Alternative 3: Database Snapshots (Filesystem-level)

**Description**: Use filesystem or VM snapshots to back up PostgreSQL data directory

**Pros**:
- Very fast (snapshot is instant)
- Exact replica of database state
- Includes all PostgreSQL internals
- Works for entire server, not just one database

**Cons**:
- Requires stopping PostgreSQL or using filesystem that supports consistent snapshots
- Not portable across different machines (binary format)
- Requires specific filesystem or VM infrastructure
- More complex to restore individual databases
- Snapshot location and format are environment-specific

**Why not chosen**: Filesystem snapshots are powerful but require specific infrastructure and are not portable. Plain SQL dumps from pg_dump are portable, human-readable, and work on any PostgreSQL installation. Portability is important for moving configurations between development and production environments.

### Alternative 4: Cloud Backup Services (AWS RDS, Azure Database)

**Description**: Use managed cloud database services with built-in backup

**Pros**:
- Fully automated backups (no scripts needed)
- Point-in-time recovery built-in
- Geographic redundancy
- Professional management and monitoring
- Scalable and highly available

**Cons**:
- Requires cloud infrastructure and accounts
- Ongoing cost (not free for local development)
- Internet dependency for database access
- Overkill for local development database
- Adds complexity to local setup

**Why not chosen**: Cloud databases are excellent for production, but this is a local development project. Requiring cloud infrastructure for local development would add unnecessary complexity and cost. Local PostgreSQL with pg_dump is free, fast, and sufficient for the use case.

### Alternative 5: Third-Party Backup Tools (pgBackRest, Barman)

**Description**: Use specialized PostgreSQL backup tools like pgBackRest or Barman

**Pros**:
- Advanced features (incremental backups, parallel restore, compression)
- Built-in retention management
- Backup verification and testing
- Enterprise-grade reliability
- Better than plain pg_dump for large databases

**Cons**:
- Additional software to install and configure
- Steeper learning curve
- More complex configuration
- Overkill for small configuration database
- Requires ongoing maintenance of backup tool

**Why not chosen**: For a local development database with a small configuration dataset, the added complexity of third-party tools is not justified. pg_dump is simple, already installed with PostgreSQL, and perfectly adequate for our needs. These tools are better suited for large production databases.

## Implementation Notes

### Integration with My-Scripts Repository

The My-Scripts repository contains:

1. **Backup Script**: `src/powershell/backup/Backup-LiftSimulatorDatabase.ps1`
   - Executes pg_dump with appropriate parameters
   - Manages backup file naming with timestamps
   - Handles errors and logging
   - Implements retention policy (if configured)

2. **Setup Documentation**: `src/powershell/backup/README-LiftSimulator.md`
   - Prerequisites (PowerShell, pg_dump availability)
   - Script configuration (database credentials, backup paths)
   - Task Scheduler setup instructions
   - Troubleshooting common issues

3. **Template Documentation**: `src/powershell/backup/README.md`
   - General backup script architecture
   - Customization options
   - Credential management best practices

### Example Backup Script Logic

```powershell
# Example structure (actual script in My-Scripts repo)
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupDir = "D:\pgbackup\lift_simulator"
$backupFile = "$backupDir\lift_simulator_backup_$timestamp.sql"
$logFile = "$backupDir\logs\backup_$timestamp.log"

try {
    # Execute pg_dump
    & pg_dump -h localhost -U lift_admin -d lift_simulator -F p -f $backupFile

    # Verify backup file created
    if (Test-Path $backupFile) {
        "Backup successful: $backupFile" | Out-File -Append $logFile
    }

    # Optional: Implement retention policy
    # Remove backups older than 30 days, keep at least 4 backups, etc.

} catch {
    "Backup failed: $_" | Out-File -Append $logFile
    exit 1
}
```

### Windows Task Scheduler Configuration

1. Open Task Scheduler
2. Create new task: "Backup Lift Simulator Database"
3. Trigger: Weekly, Tuesday at 8:00 AM
4. Action: Start a program
   - Program: `pwsh`
   - Arguments: `-File "C:\Users\manoj\Documents\Scripts\src\powershell\backup\Backup-LiftSimulatorDatabase.ps1"`
5. Conditions: Only run if computer is on AC power
6. Settings: Stop if runs longer than 1 hour

### Backup File Naming Convention

```
lift_simulator_backup_YYYYMMDD_HHMMSS.sql
```

Example: `lift_simulator_backup_20260113_080000.sql`

This naming scheme:
- Clearly identifies the database
- Sorts chronologically
- Includes date and time for point-in-time identification
- Uses underscores for compatibility with various filesystems

### Restore Verification Checklist

After restore, verify:

1. ✅ Flyway schema history is intact
   ```sql
   SELECT * FROM flyway_schema_history ORDER BY installed_rank;
   ```

2. ✅ Lift systems are present
   ```sql
   SELECT COUNT(*) FROM lift_system;
   ```

3. ✅ Versions are present and associated correctly
   ```sql
   SELECT ls.system_key, COUNT(lsv.id) as version_count
   FROM lift_system ls
   LEFT JOIN lift_system_version lsv ON lsv.lift_system_id = ls.id
   GROUP BY ls.system_key;
   ```

4. ✅ Published versions are correct
   ```sql
   SELECT ls.system_key, lsv.version_number, lsv.status
   FROM lift_system ls
   JOIN lift_system_version lsv ON lsv.lift_system_id = ls.id
   WHERE lsv.is_published = true;
   ```

5. ✅ Application starts successfully
   ```bash
   mvn spring-boot:run
   # Check logs for startup errors
   ```

## References

- PostgreSQL Backup Documentation: https://www.postgresql.org/docs/current/backup.html
- pg_dump Documentation: https://www.postgresql.org/docs/current/app-pgdump.html
- pg_restore Documentation: https://www.postgresql.org/docs/current/app-pgrestore.html
- ADR-0007: PostgreSQL and Flyway Integration (database architecture)
- My-Scripts Repository: `src/powershell/backup/README-LiftSimulator.md` (automation setup)

## Future Considerations

1. **Enhanced Backup Verification**:
   - Automated restore testing in CI/CD pipeline
   - Periodic test restores to verify backup integrity
   - Backup size and row count validation

2. **Improved Retention Management**:
   - Grandfather-father-son backup rotation
   - Configurable retention policy (e.g., keep daily for 7 days, weekly for 4 weeks, monthly for 12 months)
   - Automatic cleanup of old backups

3. **Off-Site Backup**:
   - Copy backups to cloud storage (S3, Azure Blob, Google Cloud Storage)
   - Network-attached storage (NAS) for redundancy
   - Encrypted backup storage for security

4. **Monitoring and Alerting**:
   - Email notifications on backup success/failure
   - Monitoring dashboard for backup health
   - Alerts for missed backup schedules

5. **Incremental Backups**:
   - Reduce backup time and storage for large databases
   - Use pg_basebackup for physical backups
   - Consider WAL archiving for production deployments

6. **Application-Level Backup UI**:
   - Admin interface to trigger manual backups
   - View backup history and status
   - One-click restore from UI (with appropriate safeguards)

7. **Backup Encryption**:
   - Encrypt backup files at rest
   - Secure backup file transfers
   - Key management for encrypted backups

8. **Multi-Environment Backup**:
   - Separate backup strategies for dev/test/prod
   - Different retention policies per environment
   - Production backups with stricter requirements
