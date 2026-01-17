# UAT Test Scenarios - Lift Simulator

**Version:** 0.40.0
**Last Updated:** 2026-01-17
**Purpose:** User Acceptance Testing guide for single-user local deployment

## Overview

This document provides step-by-step test scenarios to validate the Lift Simulator application. Each scenario includes:
- **Objective:** What you're testing
- **Prerequisites:** What needs to be set up first
- **Steps:** Detailed actions to perform
- **Expected Results:** What should happen
- **Pass Criteria:** How to know the test passed

## Prerequisites

Before starting UAT, ensure:
- [ ] PostgreSQL database is running
- [ ] Backend application is running (`mvn spring-boot:run`)
- [ ] Frontend application is running (`cd frontend && npm run dev`)
- [ ] Application is accessible at http://localhost:3000

---

## Scenario 1: Create a Lift System

**Objective:** Verify that users can create a new lift system configuration

**Prerequisites:** Application running, no specific data required

**Steps:**
1. Navigate to http://localhost:3000
2. Click on "Lift Systems" in the navigation menu
3. Click the "Create New System" button
4. Fill in the form:
   - System Key: `test-building-a`
   - Display Name: `Test Building A`
   - Description: `UAT test for building A lift system`
5. Click "Create" button

**Expected Results:**
- Form validates successfully
- Modal closes
- New system appears in the lift systems list
- System shows correct name and key
- Creation timestamp is displayed

**Pass Criteria:**
- ✅ System created without errors
- ✅ System appears in list with correct details
- ✅ System key is unique and stored correctly

---

## Scenario 2: View Lift System Details

**Objective:** Verify that users can view detailed information about a lift system

**Prerequisites:** At least one lift system exists (use system from Scenario 1)

**Steps:**
1. From the Lift Systems page, locate "Test Building A"
2. Click on the system name or "View Details" button
3. Review the system detail page

**Expected Results:**
- System detail page loads
- System metadata displayed correctly (key, name, description)
- Versions section is visible (may be empty)
- "Create New Version" button is available
- Navigation breadcrumb shows current location

**Pass Criteria:**
- ✅ All system information displays correctly
- ✅ Page layout is functional and readable
- ✅ No console errors in browser developer tools

---

## Scenario 3: Create a Valid Configuration Version

**Objective:** Verify that users can create a valid lift configuration

**Prerequisites:** At least one lift system exists (Test Building A from Scenario 1)

**Steps:**
1. Navigate to Test Building A detail page
2. Click "Create New Version" button
3. Modal appears with:
   - Version number (should be 1)
   - Configuration JSON textarea (required field)
   - "Create" and "Cancel" buttons
4. In the configuration textarea, paste the following valid configuration:
   ```json
   {
     "floors": 10,
     "lifts": 2,
     "travelTicksPerFloor": 1,
     "doorTransitionTicks": 2,
     "doorDwellTicks": 3,
     "doorReopenWindowTicks": 2,
     "homeFloor": 0,
     "idleTimeoutTicks": 5,
     "controllerStrategy": "NEAREST_REQUEST_ROUTING",
     "idleParkingMode": "PARK_TO_HOME_FLOOR"
   }
   ```
5. Click "Create" button
6. Wait for version to be created

**Expected Results:**
- Modal shows required configuration field
- Configuration is validated before creation
- Version 1 is created successfully with DRAFT status
- Modal closes after successful creation
- New version appears in the versions list
- Success message appears
- Configuration is stored correctly

**Pass Criteria:**
- ✅ Cannot create version without providing configuration (field is required)
- ✅ Valid configuration is accepted
- ✅ Version created with version number 1
- ✅ Status badge shows "DRAFT"
- ✅ Configuration is saved correctly

---

## Scenario 4: Test Configuration Validation (Invalid Config)

**Objective:** Verify that configuration validation rejects invalid configurations

**Prerequisites:** Test Building A exists with Version 1

**Steps:**
1. Navigate to Test Building A detail page
2. Click "Create New Version" button
3. Modal appears with configuration textarea
4. Paste the following INVALID configuration:
   ```json
   {
     "floors": 1,
     "lifts": 0,
     "travelTicksPerFloor": -1,
     "doorTransitionTicks": 2,
     "doorDwellTicks": 3,
     "doorReopenWindowTicks": 5,
     "homeFloor": 20,
     "idleTimeoutTicks": -5,
     "controllerStrategy": "INVALID_STRATEGY",
     "idleParkingMode": "PARK_TO_HOME_FLOOR"
   }
   ```
5. Click "Create" button
6. Observe the validation error response

**Expected Results:**
- Backend validation runs when "Create" is clicked
- Creation fails with validation errors:
  - `floors`: Must be at least 2
  - `lifts`: Must be at least 1
  - `travelTicksPerFloor`: Must be at least 1
  - `doorReopenWindowTicks`: Must not exceed doorTransitionTicks (5 > 2)
  - `homeFloor`: Must be within floor range (20 >= 1)
  - `idleTimeoutTicks`: Must be non-negative
  - `controllerStrategy`: Invalid enum value
- Error messages are displayed to the user
- Version is NOT created (validation is enforced)
- Modal remains open or shows error feedback
- No invalid version appears in the versions list

**Pass Criteria:**
- ✅ All validation errors are correctly identified
- ✅ Error messages are descriptive and actionable
- ✅ Invalid configuration is rejected (version not created)
- ✅ User receives clear feedback about why creation failed
- ✅ System enforces validation - no invalid configs can be saved

---

## Scenario 5: Create Valid Configuration After Fixing Errors

**Objective:** Verify that users can correct validation errors and successfully create a valid configuration

**Prerequisites:** Test Building A exists (from previous scenarios)

**Steps:**
1. Navigate to Test Building A detail page
2. Click "Create New Version" button
3. Modal appears with configuration textarea
4. In the textarea, paste the following CORRECTED configuration (fixing the errors from Scenario 4):
   ```json
   {
     "floors": 15,
     "lifts": 3,
     "travelTicksPerFloor": 2,
     "doorTransitionTicks": 3,
     "doorDwellTicks": 5,
     "doorReopenWindowTicks": 2,
     "homeFloor": 0,
     "idleTimeoutTicks": 10,
     "controllerStrategy": "DIRECTIONAL_SCAN",
     "idleParkingMode": "STAY_AT_CURRENT_FLOOR"
   }
   ```
5. Click "Create" button
6. Wait for version to be created

**Expected Results:**
- Configuration passes validation (all errors from Scenario 4 are fixed)
- Version 2 is created successfully with DRAFT status
- Modal closes after successful creation
- Version 2 appears in the versions list
- Success message appears
- Configuration is stored correctly

**Pass Criteria:**
- ✅ No validation errors (all Scenario 4 issues resolved)
- ✅ Version created successfully
- ✅ Status badge shows "DRAFT"
- ✅ Demonstrates that users can learn from validation errors and create valid configs

---

## Scenario 6: Publish a Configuration Version

**Objective:** Verify the publish workflow and auto-archiving of previous published versions

**Prerequisites:**
- Test Building A exists
- Version 1 has valid configuration (from Scenario 3)
- Version 2 has valid configuration (from Scenario 5)

**Steps:**
1. Navigate to Test Building A detail page
2. Locate Version 1 in the versions list
3. Click "Publish" button on Version 1
4. Confirm publish action if prompted
5. Wait for publish to complete
6. Verify Version 1 status changes to PUBLISHED
7. Note the publishedAt timestamp
8. Now click "Publish" button on Version 2
9. Confirm publish action
10. Wait for publish to complete
11. Verify Version 2 status changes to PUBLISHED
12. Verify Version 1 status automatically changes to ARCHIVED

**Expected Results:**
- Version 1 publishes successfully
- Status badge shows "PUBLISHED" with green color
- publishedAt timestamp is displayed
- When Version 2 is published:
  - Version 2 status becomes PUBLISHED
  - Version 1 status automatically becomes ARCHIVED
  - Only one version is PUBLISHED at a time per system
- Success messages appear for each action

**Pass Criteria:**
- ✅ Only one version can be published at a time
- ✅ Previous published version auto-archives
- ✅ Status badges update correctly
- ✅ Timestamps are recorded

---

## Scenario 7: Create Multiple Versions and Test Pagination

**Objective:** Verify version list pagination, sorting, and filtering

**Prerequisites:** Test Building A exists with at least 2 versions

**Steps:**
1. Navigate to Test Building A detail page
2. Create 8 additional versions (for a total of 10 versions)
   - Use the valid configuration from Scenario 3 for each
   - Publish 1-2 of them at different times
3. Test pagination controls:
   - Change "Items per page" to 5
   - Navigate to page 2
   - Navigate back to page 1
4. Test sorting:
   - Sort by "Version Number (Ascending)"
   - Sort by "Version Number (Descending)"
   - Sort by "Created Date (Newest First)"
   - Sort by "Created Date (Oldest First)"
5. Test filtering:
   - Filter by "Published"
   - Filter by "Draft"
   - Filter by "Archived"
   - Filter by "All"
6. Test search:
   - Search for a specific version number (e.g., "3")

**Expected Results:**
- Pagination controls appear when >5 versions exist
- Items per page selector works (5/10/20/50/100)
- Page navigation works correctly
- Sorting changes the order as expected
- Filtering shows only matching statuses
- Search filters by version number
- Pagination info shows correct counts (e.g., "Showing 1-5 of 10 versions")

**Pass Criteria:**
- ✅ Pagination controls function correctly
- ✅ Sorting works for all options
- ✅ Filtering shows correct versions
- ✅ Search finds matching versions
- ✅ No errors when switching pages or filters

---

## Scenario 8: Test Configuration Validator Tool

**Objective:** Verify standalone configuration validation tool

**Prerequisites:** Application running

**Steps:**
1. Navigate to "Configuration Validator" from the main menu
2. Paste the following configuration in the editor:
   ```json
   {
     "floors": 20,
     "lifts": 4,
     "travelTicksPerFloor": 1,
     "doorTransitionTicks": 2,
     "doorDwellTicks": 3,
     "doorReopenWindowTicks": 1,
     "homeFloor": 5,
     "idleTimeoutTicks": 8,
     "controllerStrategy": "DIRECTIONAL_SCAN",
     "idleParkingMode": "PARK_TO_HOME_FLOOR"
   }
   ```
3. Click "Validate Configuration" button
4. Review results
5. Now paste an invalid configuration:
   ```json
   {
     "floors": 1,
     "lifts": 1,
     "travelTicksPerFloor": 1,
     "doorTransitionTicks": 1,
     "doorDwellTicks": 1,
     "doorReopenWindowTicks": 0,
     "homeFloor": 0,
     "idleTimeoutTicks": 0,
     "controllerStrategy": "NEAREST_REQUEST_ROUTING",
     "idleParkingMode": "STAY_AT_CURRENT_FLOOR"
   }
   ```
6. Click "Validate Configuration" button
7. Review validation errors

**Expected Results:**
- First validation passes (may show warnings)
- Second validation fails with error: "floors must be at least 2"
- Validation results are clearly displayed
- Errors and warnings are distinguishable
- Tool works independently of saved configurations

**Pass Criteria:**
- ✅ Valid configurations are accepted
- ✅ Invalid configurations are rejected with clear errors
- ✅ Validation results are displayed correctly
- ✅ Tool is accessible and functional

---

## Scenario 9: Test Health Check Endpoint

**Objective:** Verify system health monitoring

**Prerequisites:** Application running

**Steps:**
1. Navigate to "Health Check" from the main menu
2. Review health status display
3. Click "Refresh Status" button (if available)
4. Verify timestamp updates
5. Open browser developer tools (F12)
6. Go to Network tab
7. Navigate to http://localhost:8080/api/health directly
8. Review response JSON

**Expected Results:**
- Health Check page shows "Healthy" status
- Service name is displayed
- Timestamp is current
- JSON response includes:
  ```json
  {
    "status": "UP",
    "service": "Lift Config Service",
    "timestamp": "..."
  }
  ```

**Pass Criteria:**
- ✅ Health status shows as healthy
- ✅ Endpoint returns HTTP 200
- ✅ Timestamp is current

---

## Scenario 10: Test System Deletion and Cascade

**Objective:** Verify that deleting a lift system cascades to versions

**Prerequisites:** At least one lift system with versions exists

**Steps:**
1. Create a new test system: `test-delete-cascade`
2. Create 2-3 versions for this system
3. Navigate to Lift Systems list
4. Locate `test-delete-cascade`
5. Click "Delete" button
6. Confirm deletion in modal
7. Verify system is removed from list
8. Check that versions are also deleted (try navigating to system detail - should 404)

**Expected Results:**
- Deletion confirmation modal appears
- After confirmation, system is deleted
- System no longer appears in list
- All associated versions are deleted (cascade delete)
- No orphaned version records

**Pass Criteria:**
- ✅ System deletes successfully
- ✅ Confirmation required before deletion
- ✅ All versions are deleted automatically
- ✅ No errors or orphaned data

---

## Scenario 11: Test Both Controller Strategies

**Objective:** Verify both controller strategies are accepted

**Prerequisites:** At least one lift system exists

**Steps:**
1. Create a new version with `NEAREST_REQUEST_ROUTING` strategy:
   ```json
   {
     "floors": 10,
     "lifts": 2,
     "travelTicksPerFloor": 1,
     "doorTransitionTicks": 2,
     "doorDwellTicks": 3,
     "doorReopenWindowTicks": 2,
     "homeFloor": 0,
     "idleTimeoutTicks": 5,
     "controllerStrategy": "NEAREST_REQUEST_ROUTING",
     "idleParkingMode": "PARK_TO_HOME_FLOOR"
   }
   ```
2. Validate and save
3. Create another version with `DIRECTIONAL_SCAN` strategy:
   ```json
   {
     "floors": 10,
     "lifts": 2,
     "travelTicksPerFloor": 1,
     "doorTransitionTicks": 2,
     "doorDwellTicks": 3,
     "doorReopenWindowTicks": 2,
     "homeFloor": 0,
     "idleTimeoutTicks": 5,
     "controllerStrategy": "DIRECTIONAL_SCAN",
     "idleParkingMode": "STAY_AT_CURRENT_FLOOR"
   }
   ```
4. Validate and save

**Expected Results:**
- Both configurations validate successfully
- Both controller strategies are accepted
- Both idle parking modes work

**Pass Criteria:**
- ✅ NEAREST_REQUEST_ROUTING strategy validates
- ✅ DIRECTIONAL_SCAN strategy validates
- ✅ Both PARK_TO_HOME_FLOOR and STAY_AT_CURRENT_FLOOR modes work

---

## Scenario 12: Test Edge Cases and Boundary Values

**Objective:** Verify validation at boundary values

**Prerequisites:** At least one lift system exists

**Steps:**
1. Test minimum valid configuration:
   ```json
   {
     "floors": 2,
     "lifts": 1,
     "travelTicksPerFloor": 1,
     "doorTransitionTicks": 1,
     "doorDwellTicks": 1,
     "doorReopenWindowTicks": 0,
     "homeFloor": 0,
     "idleTimeoutTicks": 0,
     "controllerStrategy": "NEAREST_REQUEST_ROUTING",
     "idleParkingMode": "STAY_AT_CURRENT_FLOOR"
   }
   ```
2. Validate (should pass)
3. Test large valid configuration:
   ```json
   {
     "floors": 100,
     "lifts": 10,
     "travelTicksPerFloor": 5,
     "doorTransitionTicks": 5,
     "doorDwellTicks": 10,
     "doorReopenWindowTicks": 5,
     "homeFloor": 50,
     "idleTimeoutTicks": 20,
     "controllerStrategy": "DIRECTIONAL_SCAN",
     "idleParkingMode": "PARK_TO_HOME_FLOOR"
   }
   ```
4. Validate (should pass)
5. Test homeFloor at boundary:
   ```json
   {
     "floors": 10,
     "lifts": 2,
     "travelTicksPerFloor": 1,
     "doorTransitionTicks": 2,
     "doorDwellTicks": 3,
     "doorReopenWindowTicks": 2,
     "homeFloor": 9,
     "idleTimeoutTicks": 5,
     "controllerStrategy": "NEAREST_REQUEST_ROUTING",
     "idleParkingMode": "PARK_TO_HOME_FLOOR"
   }
   ```
6. Validate (should pass - homeFloor 9 is valid for 10 floors [0-9])

**Expected Results:**
- Minimum valid configuration passes
- Large valid configuration passes
- Boundary values are accepted when valid
- homeFloor validation correctly enforces 0 <= homeFloor < floors

**Pass Criteria:**
- ✅ Minimum values pass validation
- ✅ Large values pass validation
- ✅ Boundary values work correctly
- ✅ No off-by-one errors in validation

---

## Scenario 13: Test UI Responsiveness and Error Handling

**Objective:** Verify UI handles errors gracefully

**Steps:**
1. Stop the backend application
2. Try to create a new lift system from the frontend
3. Observe error handling
4. Try to load a lift system detail page
5. Restart the backend
6. Verify the UI recovers

**Expected Results:**
- When backend is down:
  - User-friendly error messages appear
  - UI doesn't crash or become unresponsive
  - Error messages indicate connection issue
- When backend restarts:
  - UI recovers and can reconnect
  - Data loads successfully

**Pass Criteria:**
- ✅ Errors are handled gracefully
- ✅ No browser console crashes
- ✅ Error messages are informative
- ✅ UI recovers when backend restarts

---

## UAT Sign-Off Checklist

After completing all scenarios, verify:

### Core Functionality
- [ ] Can create lift systems
- [ ] Can view lift system details
- [ ] Can create configuration versions
- [ ] Can edit configurations
- [ ] Configuration validation works correctly
- [ ] Can publish versions
- [ ] Auto-archiving works (only one published version per system)
- [ ] Can delete lift systems

### Data Integrity
- [ ] Version numbers auto-increment correctly
- [ ] Cascade deletion works (system deletes all versions)
- [ ] Timestamps are recorded correctly
- [ ] Status transitions work correctly (DRAFT → PUBLISHED → ARCHIVED)

### User Experience
- [ ] UI is intuitive and easy to navigate
- [ ] Validation messages are clear and helpful
- [ ] Pagination works correctly
- [ ] Sorting and filtering work correctly
- [ ] Error messages are informative

### Quality
- [ ] No critical bugs encountered
- [ ] No data loss during testing
- [ ] Application performs adequately
- [ ] No browser console errors during normal use

### Documentation
- [ ] README setup instructions work
- [ ] Configuration examples are accurate
- [ ] API endpoints function as documented

---

## Issue Reporting Template

If you encounter issues during UAT, document them using this template:

```markdown
### Issue: [Brief Description]

**Scenario:** [Which scenario were you testing?]
**Steps to Reproduce:**
1.
2.
3.

**Expected Behavior:**

**Actual Behavior:**

**Severity:** [Critical / High / Medium / Low]
**Browser:** [Chrome/Firefox/Safari/Edge + version]
**Screenshot:** [If applicable]
**Console Errors:** [Copy from browser developer tools]
```

---

## UAT Completion

**UAT Start Date:** _______________
**UAT End Date:** _______________
**Tester Name:** _______________

**Overall Result:** [ ] PASS  [ ] PASS WITH ISSUES  [ ] FAIL

**Comments:**

---

**Notes:**
- These scenarios cover the main user workflows and edge cases
- Total estimated testing time: 2-3 hours
- It's recommended to test in order, as later scenarios build on earlier ones
- Keep notes of any issues or suggestions for improvement
