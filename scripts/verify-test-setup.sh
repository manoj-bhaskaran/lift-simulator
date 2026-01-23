#!/bin/bash

# Verification script for test database setup
# This script checks if the test database is properly configured

set -e

echo "=== Lift Simulator Test Setup Verification ==="
echo

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check PostgreSQL is running
echo "1. Checking PostgreSQL connection..."
if PGPASSWORD=liftpassword psql -h localhost -U lift_admin -d lift_simulator_test -c "SELECT 1" &> /dev/null; then
    echo -e "${GREEN}✓${NC} PostgreSQL is running and accessible"
else
    echo -e "${RED}✗${NC} Cannot connect to PostgreSQL"
    echo "  Fix: Ensure PostgreSQL is running and test database exists"
    exit 1
fi
echo

# Check test database exists
echo "2. Checking test database..."
if PGPASSWORD=liftpassword psql -h localhost -U lift_admin -lqt | cut -d \| -f 1 | grep -qw lift_simulator_test; then
    echo -e "${GREEN}✓${NC} Test database 'lift_simulator_test' exists"
else
    echo -e "${RED}✗${NC} Test database 'lift_simulator_test' not found"
    exit 1
fi
echo

# Check schema exists
echo "3. Checking database schema..."
if PGPASSWORD=liftpassword psql -h localhost -U lift_admin -d lift_simulator_test -c "\dn" | grep -qw lift_simulator; then
    echo -e "${GREEN}✓${NC} Schema 'lift_simulator' exists"
else
    echo -e "${RED}✗${NC} Schema 'lift_simulator' not found"
    exit 1
fi
echo

# Check permissions
echo "4. Checking schema permissions..."
PERMS=$(PGPASSWORD=liftpassword psql -h localhost -U lift_admin -d lift_simulator_test -c "SELECT privilege_type FROM information_schema.role_table_grants WHERE grantee='lift_admin' AND table_schema='lift_simulator' LIMIT 1" -t)
if [ -n "$PERMS" ]; then
    echo -e "${GREEN}✓${NC} lift_admin has schema permissions"
else
    echo -e "${YELLOW}⚠${NC} Could not verify schema permissions (may still work)"
fi
echo

# Check Maven
echo "5. Checking Maven..."
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -v | head -n 1)
    echo -e "${GREEN}✓${NC} Maven installed: $MVN_VERSION"
else
    echo -e "${RED}✗${NC} Maven not found in PATH"
    exit 1
fi
echo

# Check Java
echo "6. Checking Java..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | grep 'version' | head -n 1)
    echo -e "${GREEN}✓${NC} Java installed: $JAVA_VERSION"
else
    echo -e "${RED}✗${NC} Java not found in PATH"
    exit 1
fi
echo

echo -e "${GREEN}=== Setup verification complete ===${NC}"
echo "Your test environment is ready to use."
echo
echo "Next steps:"
echo "1. Run tests: mvn clean test"
echo "2. Run specific test: mvn test -Dtest=LiftSystemRepositoryTest"
echo "3. For more info: see docs/TESTING-SETUP.md"
