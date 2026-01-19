# E2E Test Case Catalog

**Version:** 0.43.0
**Last Updated:** 2026-01-19
**Test Framework:** Playwright 1.57.0

## Overview

This catalog documents all end-to-end (E2E) test cases for the Lift Simulator Admin UI. Each test case includes:
- Formal specification (preconditions, steps, expected results)
- Implementation file reference
- Requirements traceability
- Automation status

**Test Implementations:** `frontend/e2e/*.spec.ts`
**Test Reports:** GitHub Actions artifacts (`.github/workflows/ci.yml`)

---

## Test Case Index

| ID | Title | Priority | Status | File |
|----|-------|----------|--------|------|
| TC_SMOKE_001 | Application Load | Critical | ✅ Automated | `smoke.spec.ts` |
| TC_SMOKE_002 | Navigation to Lift Systems | High | ✅ Automated | `smoke.spec.ts` |
| TC_SMOKE_003 | Health Check Page Accessibility | High | ✅ Automated | `smoke.spec.ts` |
| TC_SMOKE_004 | Config Validator Page Accessibility | High | ✅ Automated | `smoke.spec.ts` |
| TC_SMOKE_005 | Footer Version Display | Medium | ✅ Automated | `smoke.spec.ts` |
| TC_DASH_017 | Dashboard Aggregate Counts Validation | High | ✅ Automated | `dashboard.spec.ts` |
| TC_DASH_002 | Dashboard Quick Actions | Medium | ✅ Automated | `dashboard.spec.ts` |
| TC_DASH_003 | Dashboard Metrics Update After Deletion | Medium | ✅ Automated | `dashboard.spec.ts` |
| TC_DASH_004 | Dashboard Navigation Accessibility | Low | ✅ Automated | `dashboard.spec.ts` |

---

## Smoke Tests

### TC_SMOKE_001: Application Load

**File:** `smoke.spec.ts:9-25`
**Priority:** Critical
**Category:** Smoke Test
**Description:** Verify the application loads successfully and displays the dashboard page with core UI elements

**Preconditions:**
- Backend service running on `http://localhost:8080`
- Frontend dev server running on `http://localhost:3000`
- Database accessible (optional for this test)

**Test Steps:**
1. Navigate to root URL `/`
2. Wait for DOM content to load

**Expected Results:**
- ✅ Page title contains "Lift Simulator"
- ✅ Dashboard heading (`<h2>Dashboard</h2>`) is visible
- ✅ "Overview" section is present
- ✅ "Quick Actions" section is present

**Linked Requirements:** N/A (Smoke test)
**Status:** ✅ Automated
**Last Updated:** 2026-01-19

---

### TC_SMOKE_002: Navigation to Lift Systems

**File:** `smoke.spec.ts:27-40`
**Priority:** High
**Category:** Smoke Test
**Description:** Verify navigation from dashboard to Lift Systems page works correctly

**Preconditions:**
- Application loaded on dashboard page

**Test Steps:**
1. Navigate to root URL `/`
2. Click on "Lift Systems" navigation link
3. Wait for URL to change to `/systems`

**Expected Results:**
- ✅ URL changes to `/systems`
- ✅ Lift Systems page heading is visible

**Linked Requirements:** ADR-0011 (React Admin UI Navigation)
**Status:** ✅ Automated
**Last Updated:** 2026-01-19

---

### TC_SMOKE_003: Health Check Page Accessibility

**File:** `smoke.spec.ts:42-50`
**Priority:** High
**Category:** Smoke Test
**Description:** Verify Health Check page is accessible and displays correctly

**Preconditions:**
- Application running

**Test Steps:**
1. Navigate directly to `/health`
2. Wait for DOM content to load

**Expected Results:**
- ✅ "Health Check" heading is visible
- ✅ Page loads without errors

**Linked Requirements:** ADR-0011 (React Admin UI)
**Status:** ✅ Automated
**Last Updated:** 2026-01-19

---

### TC_SMOKE_004: Config Validator Page Accessibility

**File:** `smoke.spec.ts:52-60`
**Priority:** High
**Category:** Smoke Test
**Description:** Verify Configuration Validator page is accessible and displays correctly

**Preconditions:**
- Application running

**Test Steps:**
1. Navigate directly to `/config-validator`
2. Wait for DOM content to load

**Expected Results:**
- ✅ "Configuration Validator" heading is visible
- ✅ Page loads without errors

**Linked Requirements:** ADR-0009 (Configuration Validation Framework)
**Status:** ✅ Automated
**Last Updated:** 2026-01-19

---

### TC_SMOKE_005: Footer Version Display

**File:** `smoke.spec.ts:62-71`
**Priority:** Medium
**Category:** Smoke Test
**Description:** Verify footer displays application version information

**Preconditions:**
- Application running

**Test Steps:**
1. Navigate to root URL `/`
2. Locate footer element
3. Check for version text

**Expected Results:**
- ✅ Footer is visible
- ✅ Version information is displayed (format: "Version X.Y.Z" or "vX.Y.Z")

**Linked Requirements:** N/A
**Status:** ✅ Automated
**Last Updated:** 2026-01-19

---

## Dashboard Tests

### TC_DASH_017: Dashboard Aggregate Counts Validation

**File:** `dashboard.spec.ts:40-110`
**Priority:** High
**Category:** Dashboard Functionality
**Description:** Verify dashboard metrics accurately reflect the total count of lift systems and configuration versions across the entire application

**Preconditions:**
- Backend service available at `http://localhost:8080`
- Database accessible and initialized
- User has permission to create/delete lift systems

**Test Steps:**
1. Navigate to Dashboard page
2. Record initial metrics:
   - Number of Lift Systems
   - Number of Versions
3. Create Test System 1 with 2 configuration versions:
   - Version 1: Basic office configuration
   - Version 2: High-rise residential configuration
4. Create Test System 2 with 1 configuration version:
   - Version 1: Minimal configuration
5. Navigate to Lift Systems page and count total systems
6. For each test system, navigate to details and count versions
7. Calculate expected totals:
   - Expected Systems = Initial + 2
   - Expected Versions = Initial + System1 versions + System2 versions
8. Return to Dashboard and refresh page
9. Compare Dashboard metrics with expected values

**Expected Results:**
- ✅ Dashboard "Number of Lift Systems" = Initial systems + 2
- ✅ Dashboard "Number of Versions" = Initial versions + 3
- ✅ Dashboard metrics match actual counts from Lift Systems page
- ✅ Metrics update correctly after system creation

**Test Data:**
- System 1: "Test System 1 for Dashboard"
- System 2: "Test System 2 for Dashboard"

**Cleanup:**
- Delete both test systems after test completion

**Linked Requirements:** User Story #42 (Dashboard Metrics), ADR-0011 (React Admin UI)
**Status:** ✅ Automated
**Last Updated:** 2026-01-19
**Notes:** Originally manual test case, automated in v0.43.0

---

### TC_DASH_002: Dashboard Quick Actions

**File:** `dashboard.spec.ts:112-139`
**Priority:** Medium
**Category:** Dashboard Functionality
**Description:** Verify Quick Actions section displays correct links and navigation works

**Preconditions:**
- Application running
- Dashboard page accessible

**Test Steps:**
1. Navigate to Dashboard page
2. Verify "Quick Actions" section is visible
3. Verify "Manage Lift Systems" link is present
4. Verify "Validate Configuration" link is present
5. Click "Manage Lift Systems" link
6. Verify navigation to `/systems` page
7. Return to Dashboard
8. Click "Validate Configuration" link
9. Verify navigation to `/config-validator` page

**Expected Results:**
- ✅ Quick Actions section is displayed
- ✅ Both action links are visible and clickable
- ✅ "Manage Lift Systems" navigates to Lift Systems page
- ✅ "Validate Configuration" navigates to Configuration Validator page
- ✅ Navigation is smooth without errors

**Linked Requirements:** ADR-0011 (React Admin UI)
**Status:** ✅ Automated
**Last Updated:** 2026-01-19

---

### TC_DASH_003: Dashboard Metrics Update After Deletion

**File:** `dashboard.spec.ts:141-169`
**Priority:** Medium
**Category:** Dashboard Functionality
**Description:** Verify dashboard metrics decrease when a lift system is deleted

**Preconditions:**
- Backend service available
- Database accessible
- User has permission to delete lift systems

**Test Steps:**
1. Create a test lift system
2. Navigate to Dashboard and record metrics
3. Delete the test system
4. Refresh Dashboard page
5. Record updated metrics

**Expected Results:**
- ✅ "Number of Lift Systems" decreases by 1
- ✅ Metrics update correctly after deletion

**Cleanup:**
- Test system is deleted as part of test steps

**Linked Requirements:** ADR-0011 (React Admin UI)
**Status:** ✅ Automated
**Last Updated:** 2026-01-19

---

### TC_DASH_004: Dashboard Navigation Accessibility

**File:** `dashboard.spec.ts:171-183`
**Priority:** Low
**Category:** Navigation
**Description:** Verify Dashboard is accessible via navigation link from other pages

**Preconditions:**
- Application running

**Test Steps:**
1. Navigate to Lift Systems page (`/systems`)
2. Click Dashboard link in navigation menu
3. Verify navigation to Dashboard (`/`)

**Expected Results:**
- ✅ Dashboard link is visible in navigation
- ✅ Clicking Dashboard link navigates to `/`
- ✅ Dashboard page loads correctly

**Linked Requirements:** ADR-0011 (React Admin UI)
**Status:** ✅ Automated
**Last Updated:** 2026-01-19

---

## Test Case Template

Use this template when adding new test cases:

```markdown
### TC_[AREA]_[NUMBER]: [Descriptive Title]

**File:** `[filename].spec.ts:[line-numbers]`
**Priority:** [Critical|High|Medium|Low]
**Category:** [Smoke Test|Dashboard|Lift Systems|Configuration|Health Check]
**Description:** Brief description of what this test validates

**Preconditions:**
- List all prerequisites
- Environment requirements
- User permissions needed

**Test Steps:**
1. Step-by-step actions
2. In imperative mood
3. Clear and reproducible

**Expected Results:**
- ✅ Use checkmarks for expected outcomes
- ✅ Be specific and measurable
- ✅ Cover all success criteria

**Test Data:** (if applicable)
- Input data used in test
- Sample configurations

**Cleanup:** (if applicable)
- Resources to clean up after test

**Linked Requirements:** [User Story #X, ADR-00XX, etc.]
**Status:** [✅ Automated | 📝 Manual | ⏳ Planned | 🚧 In Progress]
**Last Updated:** YYYY-MM-DD
**Notes:** (optional) Additional context, known issues, etc.
```

---

## Test Naming Convention

- **Area Codes:**
  - `SMOKE` - Smoke tests
  - `DASH` - Dashboard tests
  - `SYS` - Lift Systems tests
  - `VER` - Version management tests
  - `VAL` - Configuration validation tests
  - `HEALTH` - Health check tests
  - `NAV` - Navigation tests

- **Numbering:** Sequential within each area, starting from 001

- **Examples:**
  - `TC_SMOKE_001` - First smoke test
  - `TC_DASH_017` - Dashboard test #17 (preserves manual test case IDs)
  - `TC_SYS_003` - Third lift systems test

---

## Test Coverage Summary

**Total Test Cases:** 9
**Automated:** 9 (100%)
**Manual:** 0
**Planned:** TBD

**Coverage by Category:**
- Smoke Tests: 5 test cases
- Dashboard Functionality: 4 test cases
- Lift Systems: 0 test cases (future work)
- Configuration Validation: 0 test cases (future work)
- Version Management: 0 test cases (future work)

**Coverage by Priority:**
- Critical: 1
- High: 4
- Medium: 3
- Low: 1

---

## Maintenance Guidelines

1. **Update this catalog** when adding/modifying/removing test cases
2. **Reference test case IDs** in code comments for traceability
3. **Link to requirements** (user stories, ADRs, tickets) where applicable
4. **Keep file paths current** if tests are moved or renamed
5. **Update "Last Updated" dates** when test cases change
6. **Version control** this file with the test code

---

## Future Test Areas

Test cases to be added:

- **Lift Systems Management:**
  - Create new lift system
  - Edit lift system details
  - Delete lift system
  - Search and filter systems
  - View system details

- **Version Management:**
  - Create configuration version
  - Edit version configuration
  - Publish version
  - Archive version
  - View version history

- **Configuration Validation:**
  - Validate valid configuration JSON
  - Validate invalid configuration JSON
  - Display validation error messages
  - Sample configuration loading

- **Error Handling:**
  - Backend unavailable scenarios
  - Network timeout handling
  - Invalid data handling

---

## References

- **ADR-0014:** [Playwright E2E Testing](../../docs/decisions/0014-playwright-e2e-testing.md)
- **ADR-0015:** [Test Management Platform Evaluation](../../docs/decisions/0015-test-management-platform-evaluation.md)
- **Playwright Docs:** https://playwright.dev/
- **CI Workflow:** `.github/workflows/ci.yml`
- **Test Config:** `playwright.config.ts`
