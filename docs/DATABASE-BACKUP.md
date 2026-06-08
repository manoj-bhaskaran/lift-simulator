# Database Backup and Restore

The lift simulator's configuration database can be backed up and restored using PostgreSQL's native `pg_dump` and `pg_restore` utilities. Backups protect against data loss from hardware failure, operator error, or corruption.

For database setup instructions, see the [Database Setup](../README.md#database-setup) section of the README.

---

## Manual Ad-Hoc Backup

For immediate, on-demand backups, execute:

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

This creates a plain SQL backup file with a timestamp in the filename (e.g., `lift_simulator_backup_20260113_140530.sql`).

**When to use manual backups:**
- Before major schema migrations or application upgrades
- Before bulk data updates or deletions
- Before testing risky operations
- Before deploying to a new environment

---

## Automated Scheduled Backup

Automated backups are managed via an external PowerShell script in the **My-Scripts** repository.

**Schedule**: Every Tuesday at 8:00 a.m. (Windows Task Scheduler)

**Script Location**: `My-Scripts/src/powershell/backup/Backup-LiftSimulatorDatabase.ps1`

**Command** (example local path, may vary):
```powershell
pwsh -File "C:\Users\manoj\Documents\Scripts\src\powershell\backup\Backup-LiftSimulatorDatabase.ps1"
```

**Backup Storage**:
- Backups: `D:\pgbackup\lift_simulator`
- Logs: `D:\pgbackup\lift_simulator\logs`

**Note**: Paths shown are local examples; your implementation may vary. Refer to the My-Scripts repository at `src/powershell/backup/README-LiftSimulator.md` for setup instructions, prerequisites, and configuration details.

---

## Restore Procedure

### Standard Restore (to existing database)

1. Stop the Spring Boot application to prevent writes during restore

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

3. Restore from backup file:

   **Linux/macOS:**
   ```bash
   psql -h localhost -U lift_admin -d lift_simulator -f lift_simulator_backup_YYYYMMDD_HHMMSS.sql
   ```

   **Windows:**
   ```cmd
   psql -h localhost -U lift_admin -d lift_simulator -f lift_simulator_backup_YYYYMMDD_HHMMSS.sql
   ```

4. Verify the restore:

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

### Clean Restore (to new machine or fresh install)

1. Install PostgreSQL 12 or later
2. Create the database and user as documented in the [Database Setup](../README.md#database-setup) section
3. Restore from backup (step 3 from Standard Restore above)
4. Verify the restore (step 4 from Standard Restore above)
5. Start the application

---

## Backup Verification

To verify a backup file is valid:

**Linux/macOS:**
```bash
# Check file size and format
ls -lh lift_simulator_backup_*.sql

# View first 20 lines (should show valid SQL)
head -n 20 lift_simulator_backup_*.sql
```

**Windows (Command Prompt):**
```cmd
REM Check file size
dir lift_simulator_backup_*.sql

REM View first 20 lines (should show valid SQL)
more /E +1 lift_simulator_backup_*.sql | findstr /N ".*" | findstr "^[1-9]: ^[12][0-9]:"
```

**Windows (PowerShell):**
```powershell
# Check file size
Get-ChildItem lift_simulator_backup_*.sql | Format-Table Name, Length, LastWriteTime

# View first 20 lines (should show valid SQL)
Get-Content lift_simulator_backup_*.sql -Head 20
```

**Periodic restore testing** (recommended quarterly):

**Linux/macOS:**
```bash
# Create test database
createdb lift_simulator_test

# Restore to test database
psql -U lift_admin -d lift_simulator_test -f lift_simulator_backup_YYYYMMDD_HHMMSS.sql

# Verify tables exist
psql -U lift_admin -d lift_simulator_test -c "\dt"

# Clean up
dropdb lift_simulator_test
```

**Windows:**
```cmd
REM Create test database
createdb lift_simulator_test

REM Restore to test database
psql -U lift_admin -d lift_simulator_test -f lift_simulator_backup_YYYYMMDD_HHMMSS.sql

REM Verify tables exist
psql -U lift_admin -d lift_simulator_test -c "\dt"

REM Clean up
dropdb lift_simulator_test
```

---

## Important Notes

- Backups can be taken while the database is online (no application downtime required)
- Configuration data is **not** committed to version control; backups are the only recovery mechanism
- For detailed backup/restore architecture and automation setup, see [ADR-0012](decisions/0012-database-backup-restore-strategy.md) and the My-Scripts repository documentation
- Backup retention policy and log management are handled by the external backup script
