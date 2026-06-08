# Troubleshooting Guide

This guide covers common problems encountered when setting up, running, or operating the Lift Simulator.

For API-level errors, see [docs/API.md](API.md).

---

## Quick Start Troubleshooting

### Backend won't start

- Verify PostgreSQL is running: `psql -h localhost -U lift_admin -d lift_simulator`
- Check database credentials in `src/main/resources/application-dev.yml`
- Check port 8080 isn't already in use: `lsof -i :8080` (macOS/Linux) or `netstat -ano | findstr :8080` (Windows)

### StackOverflowError in backend logs when loading HTML routes

- Ensure the SPA forwarder does not match `/index.html` (the app now excludes it to prevent recursive forwards)
- Restart the backend after pulling the latest changes

### Backend returns 404 for index.html

- Build the frontend assets with `mvn -Pfrontend clean package` or run the frontend dev server at http://localhost:3000
- Verify `target/classes/static/index.html` exists after building the frontend bundle

### Frontend won't start

- Verify Node.js version: `node --version` (should be 20.19+ or 22.12+)
- Delete `node_modules` and reinstall: `rm -rf node_modules && npm install`
- Check port 3000 isn't already in use

### Can't connect to backend from frontend

- Verify backend is running at http://localhost:8080/api/v1/health
- Check browser console for CORS errors
- Vite dev proxy should handle this automatically

### Database migrations fail

- Drop and recreate database (see [Database Setup](../README.md#database-setup))
- Verify PostgreSQL version is 12+
- Check Flyway migration files in `src/main/resources/db/migration/`

### Scenario validation fails with "Unable to read scenario payload"

- Confirm the scenario JSON is valid and not empty
- Retry the request after verifying the payload format

### Cannot delete a lift system due to scenario dependencies

- Delete scenarios (or the versions that reference them) before deleting the lift system
- Retry the delete once dependent scenarios are removed

---

## Database Troubleshooting

### Connection refused errors

- Ensure PostgreSQL is running: `sudo service postgresql status`
- Check the connection settings in `application-dev.yml`

### Permission denied errors

- Verify the database user has proper permissions: `GRANT ALL PRIVILEGES ON DATABASE lift_simulator TO lift_admin;`
- Ensure schema-level permissions: `GRANT ALL ON SCHEMA lift_simulator TO lift_admin;`

### Migration errors

- Check Flyway history: `SELECT * FROM flyway_schema_history;`
- For development, you can reset the database: `DROP DATABASE lift_simulator; CREATE DATABASE lift_simulator;`
- If `public.flyway_schema_history` exists from earlier runs, drop it and restart the app so Flyway recreates history in `lift_simulator`: `DROP TABLE public.flyway_schema_history;`
- If a legacy `public.schema_metadata` table exists from older releases, it can be dropped; current migrations do not use it: `DROP TABLE public.schema_metadata;`
- If you upgraded from 0.23.0 and see "Found more than one migration with version 1", run `mvn clean` once to clear stale build artifacts; the build now removes old migration resources automatically.
- If Flyway reports "No migrations found", rebuild with `mvn clean package` to refresh the packaged `db/migration` resources.
