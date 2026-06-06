import { test, expect } from '@playwright/test';
import {
  generateSystemData,
  createLiftSystem,
  createConfigVersion,
  publishVersion,
  cleanupSystemIfExists,
  deleteScenariosForSystem,
  isBackendAvailable,
  VALID_CONFIGS
} from './helpers/test-helpers';

/**
 * Scenario Management Tests
 *
 * Test Cases:
 * - TC_SCENARIO_001: Create scenario using form mode
 * - TC_SCENARIO_002: Create scenario using Advanced JSON Mode (validates + saves)
 * - TC_SCENARIO_003: Edit existing scenario using Advanced JSON Mode
 * - TC_SCENARIO_004: Validate invalid JSON shows error message
 */

test.describe('Scenario Management', () => {
  let testSystemKey: string;
  let testSystemDisplayName: string;

  test.beforeEach(async ({ page }) => {
    // Check if backend is available
    const backendAvailable = await isBackendAvailable(page);
    if (!backendAvailable) {
      test.skip();
    }

    // Create a test system and version for scenario tests
    const systemData = generateSystemData();
    testSystemKey = systemData.systemKey;
    testSystemDisplayName = systemData.displayName;
    await createLiftSystem(page, systemData);

    // Create and publish a version for the system so it can be used in scenarios.
    await createConfigVersion(page, testSystemKey, VALID_CONFIGS.basicOffice);
    await publishVersion(page, 1);
    await expect(page.locator('.version-card').filter({ hasText: 'Version 1' }).locator('.status-badge')).toHaveText(/PUBLISHED/i);
  });

  test.afterEach(async ({ page }) => {
    // Cleanup: scenarios must be removed before the system, otherwise the
    // backend rejects the system deletion (HTTP 409) because scenarios still
    // reference its versions.
    if (testSystemKey) {
      await deleteScenariosForSystem(page, testSystemKey);
      await cleanupSystemIfExists(page, testSystemKey);
    }
  });

  test('TC_SCENARIO_001: Create scenario using form mode', async ({ page }) => {
    // Navigate to scenarios page
    await page.goto('/scenarios');
    await page.waitForLoadState('domcontentloaded');

    // Click Create New Scenario button
    await page.locator('button:has-text("Create New Scenario")').click();

    // Wait for navigation to scenario form
    await page.waitForURL(/\/scenarios\/new/);

    // Fill in scenario name
    await page.locator('#scenarioName').fill('Test Morning Rush Scenario');

    // Select lift system
    await page.locator('#liftSystem').selectOption({ label: testSystemDisplayName });

    // Wait a bit for versions to load
    await page.waitForTimeout(500);

    // Select version
    await page.locator('#liftSystemVersion').selectOption({ index: 1 }); // Select first available version

    // Set duration
    await page.locator('#durationTicks').fill('200');

    // Add passenger flow using the inline flow editor
    await page.locator('button:has-text("Add Passenger Flow")').click();

    const addFlowForm = page.locator('.add-flow-form');
    await expect(addFlowForm).toBeVisible();
    await addFlowForm.locator('#startTick').fill('0');
    await addFlowForm.locator('#originFloor').fill('0');
    await addFlowForm.locator('#destinationFloor').fill('5');
    await addFlowForm.locator('#passengers').fill('3');
    await addFlowForm.locator('button:has-text("Save")').click();
    await expect(addFlowForm).toBeHidden();

    // Submit the form
    await page.locator('button:has-text("Create Scenario")').click();

    // Wait for navigation back to scenarios page
    await page.waitForURL(/\/scenarios$/);

    // Verify scenario was created
    await page.waitForTimeout(1000);
    await expect(page.locator('text=Test Morning Rush Scenario')).toBeVisible();
  });

  test('TC_SCENARIO_002: Create scenario using Advanced JSON Mode (validates + saves)', async ({ page }) => {
    // Navigate to scenarios page
    await page.goto('/scenarios');
    await page.waitForLoadState('domcontentloaded');

    // Click Create New Scenario button
    await page.locator('button:has-text("Create New Scenario")').click();

    // Wait for navigation to scenario form
    await page.waitForURL(/\/scenarios\/new/);

    // Fill in scenario name
    await page.locator('#scenarioName').fill('Advanced JSON Test Scenario');

    // Select lift system
    await page.locator('#liftSystem').selectOption({ label: testSystemDisplayName });
    await page.waitForTimeout(500);

    // Select version
    await page.locator('#liftSystemVersion').selectOption({ index: 1 });

    // Switch to Advanced JSON Mode
    await page.locator('button:has-text("Switch to Advanced JSON Mode")').click();

    // Verify JSON editor is visible
    const jsonEditor = page.locator('.json-editor');
    await expect(jsonEditor).toBeVisible();

    // Edit the JSON with valid scenario data
    const scenarioJson = {
      durationTicks: 250,
      passengerFlows: [
        { startTick: 0, originFloor: 0, destinationFloor: 5, passengers: 3 },
        { startTick: 10, originFloor: 0, destinationFloor: 8, passengers: 2 },
        { startTick: 20, originFloor: 3, destinationFloor: 7, passengers: 4 }
      ],
      seed: 42
    };

    await jsonEditor.clear();
    await jsonEditor.fill(JSON.stringify(scenarioJson, null, 2));

    // Click Validate button
    await page.locator('button:has-text("Validate")').click();

    // Wait for validation to complete
    await page.waitForTimeout(2000);

    // Verify validation succeeded (alert modal should show success)
    const alertModal = page.locator('.modal-content.alert-modal');
    await expect(alertModal).toBeVisible({ timeout: 3000 });
    await expect(alertModal).toContainText('Scenario is valid!');

    // Close the alert
    await alertModal.locator('button:has-text("OK")').click();
    await expect(alertModal).toBeHidden();

    // Click Create Scenario button to save
    await page.locator('button:has-text("Create Scenario")').click();

    // Wait for navigation back to scenarios page
    await page.waitForURL(/\/scenarios$/, { timeout: 5000 });

    // Verify scenario was created
    await page.waitForTimeout(1000);
    await expect(page.locator('text=Advanced JSON Test Scenario')).toBeVisible();

    // Navigate to edit the scenario to verify it was saved correctly
    const scenarioRow = page.locator('.scenario-card').filter({ hasText: 'Advanced JSON Test Scenario' });
    await scenarioRow.locator('button:has-text("Edit")').click();
    await page.waitForURL(/\/scenarios\/\d+\/edit/);

    // Wait for the edit form to finish loading the scenario before interacting.
    await expect(page.locator('#scenarioName')).toHaveValue('Advanced JSON Test Scenario');

    // Switch to Advanced JSON Mode to verify the saved data
    await page.locator('button:has-text("Switch to Advanced JSON Mode")').click();
    const savedEditor = page.locator('.json-editor');
    await expect(savedEditor).toBeVisible();

    // Verify the JSON matches what we saved
    const parsedJson = JSON.parse(await savedEditor.inputValue());
    expect(parsedJson.durationTicks).toBe(250);
    expect(parsedJson.passengerFlows).toHaveLength(3);
    expect(parsedJson.passengerFlows[0].originFloor).toBe(0);
    expect(parsedJson.passengerFlows[0].destinationFloor).toBe(5);
    expect(parsedJson.seed).toBe(42);
  });

  test('TC_SCENARIO_003: Edit existing scenario using Advanced JSON Mode', async ({ page }) => {
    // First, create a scenario using the form
    await page.goto('/scenarios');
    await page.waitForLoadState('domcontentloaded');
    await page.locator('button:has-text("Create New Scenario")').click();
    await page.waitForURL(/\/scenarios\/new/);

    await page.locator('#scenarioName').fill('Scenario To Edit');
    await page.locator('#liftSystem').selectOption({ label: testSystemDisplayName });
    await page.waitForTimeout(500);
    await page.locator('#liftSystemVersion').selectOption({ index: 1 });
    await page.locator('#durationTicks').fill('100');

    // A scenario requires at least one passenger flow, so add one before saving.
    await page.locator('button:has-text("Add Passenger Flow")').click();
    const addFlowForm = page.locator('.add-flow-form');
    await expect(addFlowForm).toBeVisible();
    await addFlowForm.locator('#startTick').fill('0');
    await addFlowForm.locator('#originFloor').fill('0');
    await addFlowForm.locator('#destinationFloor').fill('5');
    await addFlowForm.locator('#passengers').fill('2');
    await addFlowForm.locator('button:has-text("Save")').click();
    await expect(addFlowForm).toBeHidden();

    await page.locator('button:has-text("Create Scenario")').click();
    await page.waitForURL(/\/scenarios$/);
    await page.waitForTimeout(1000);

    // Now edit the scenario using Advanced JSON Mode
    const scenarioRow = page.locator('tr, .scenario-card').filter({ hasText: 'Scenario To Edit' });
    const editButton = scenarioRow.locator('button:has-text("Edit"), a:has-text("Edit")');
    await editButton.click();
    await page.waitForURL(/\/scenarios\/\d+\/edit/);

    // Switch to Advanced JSON Mode
    await page.locator('button:has-text("Switch to Advanced JSON Mode")').click();

    const jsonEditor = page.locator('.json-editor');
    await expect(jsonEditor).toBeVisible();

    // Update the JSON
    const updatedJson = {
      durationTicks: 300,
      passengerFlows: [
        { startTick: 0, originFloor: 0, destinationFloor: 9, passengers: 5 },
        { startTick: 15, originFloor: 5, destinationFloor: 0, passengers: 3 }
      ],
      seed: 99
    };

    await jsonEditor.clear();
    await jsonEditor.fill(JSON.stringify(updatedJson, null, 2));

    // Validate
    await page.locator('button:has-text("Validate")').click();
    await page.waitForTimeout(2000);

    // Close validation success alert
    const alertModal = page.locator('.modal-content.alert-modal');
    if (await alertModal.isVisible()) {
      await alertModal.locator('button:has-text("OK")').click();
    }

    // Update the scenario
    await page.locator('button:has-text("Update Scenario")').click();

    // Wait for navigation
    await page.waitForURL(/\/scenarios$/, { timeout: 5000 });

    // Go back to edit and verify changes were saved
    await page.waitForTimeout(1000);
    const updatedScenarioRow = page.locator('.scenario-card').filter({ hasText: 'Scenario To Edit' });
    await updatedScenarioRow.locator('button:has-text("Edit")').click();
    await page.waitForURL(/\/scenarios\/\d+\/edit/);

    // Wait for the edit form to finish loading the scenario before interacting.
    await expect(page.locator('#scenarioName')).toHaveValue('Scenario To Edit');

    // Switch to Advanced JSON Mode
    await page.locator('button:has-text("Switch to Advanced JSON Mode")').click();
    const verifyEditor = page.locator('.json-editor');
    await expect(verifyEditor).toBeVisible();

    // Verify the updated values
    const parsedJson = JSON.parse(await verifyEditor.inputValue());

    expect(parsedJson.durationTicks).toBe(300);
    expect(parsedJson.passengerFlows).toHaveLength(2);
    expect(parsedJson.seed).toBe(99);
  });

  test('TC_SCENARIO_004: Validate invalid JSON shows error message', async ({ page }) => {
    // Navigate to create scenario page
    await page.goto('/scenarios');
    await page.waitForLoadState('domcontentloaded');
    await page.locator('button:has-text("Create New Scenario")').click();
    await page.waitForURL(/\/scenarios\/new/);

    // Fill required fields
    await page.locator('#scenarioName').fill('Invalid JSON Test');
    await page.locator('#liftSystem').selectOption({ label: testSystemDisplayName });
    await page.waitForTimeout(500);
    await page.locator('#liftSystemVersion').selectOption({ index: 1 });

    // Switch to Advanced JSON Mode
    await page.locator('button:has-text("Switch to Advanced JSON Mode")').click();

    const jsonEditor = page.locator('.json-editor');

    // Enter invalid JSON (missing closing brace)
    await jsonEditor.clear();
    await jsonEditor.fill('{ "durationTicks": 100, "passengerFlows": [');

    // Try to validate
    await page.locator('button:has-text("Validate")').click();
    await page.waitForTimeout(1000);

    // Should show error message about invalid JSON
    const alertModal = page.locator('.modal-content.alert-modal');
    await expect(alertModal).toBeVisible({ timeout: 3000 });
    await expect(alertModal).toContainText(/Invalid JSON format/i);

    // Close the alert
    await alertModal.locator('button:has-text("OK")').click();

    // Try to save - should also show error
    await page.locator('button:has-text("Create Scenario")').click();
    await page.waitForTimeout(1000);

    // Should show error message and stay on the same page
    await expect(page.locator('text=/Invalid JSON format/i')).toBeVisible();
  });
});
