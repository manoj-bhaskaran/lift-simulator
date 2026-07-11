#!/usr/bin/env bash
# Waits for PostgreSQL to accept connections, then (re)creates the test
# schema. Shared by the backend and e2e-playwright CI jobs, which each run
# their own postgres service container and need an identically-shaped schema.
set -euo pipefail

PGHOST="${PGHOST:-localhost}"
PGPORT="${PGPORT:-5432}"
PGUSER="${PGUSER:-lift_admin}"
PGDATABASE="${PGDATABASE:-lift_simulator_test}"
SCHEMA="${SCHEMA:-lift_simulator}"

for i in $(seq 1 30); do
  pg_isready -h "$PGHOST" -p "$PGPORT" -U "$PGUSER" && break
  echo "Waiting for postgres... ($i/30)"
  sleep 1
done

psql -h "$PGHOST" -U "$PGUSER" -d "$PGDATABASE" -c "DROP SCHEMA IF EXISTS ${SCHEMA} CASCADE;"
psql -h "$PGHOST" -U "$PGUSER" -d "$PGDATABASE" -c "CREATE SCHEMA IF NOT EXISTS ${SCHEMA};"
psql -h "$PGHOST" -U "$PGUSER" -d "$PGDATABASE" -c "GRANT ALL PRIVILEGES ON SCHEMA ${SCHEMA} TO ${PGUSER};"
