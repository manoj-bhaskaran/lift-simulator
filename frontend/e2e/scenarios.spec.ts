import { test, expect } from '@playwright/test';
import {
  generateSystemData,
  createLiftSystem,
  cleanupSystemIfExists,
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
  let systemId: number;

  test.beforeEach(async ({ page }) => {
    // Check if backend is available
    const backendAvailable = await isBackendAvailable(page);
    if (!backendAvailable) {
      test.skip();
    }

    // Create a test system and version for scenario tests
    const systemData = generateSystemData({
      displayName: 'Scenario Test Building'
    });
    testSystemKey = systemData.systemKey;
    await createLiftSystem(page, systemData);

    // Get the system ID from the UI
    await page.goto('/systems');
    await page.waitForLoadState('domcontentloaded');
    const systemCard = page.locator('.system-card').filter({ hasText: testSystemKey });
    await systemCard.locator('button:has-text("View Details")').click();
    await page.waitForURL(/\/systems\/([^/]+)$/);

    // Extract system ID from URL
    const url = page.url();
    const match = url.match(/\/systems\/([^/]+)$/);
    if (match) {
      systemId = parseInt(match[1], 10);
    }

    // Create a version for the system
    await page.locator('button:has-text("Create New Version")').click();
    await page.locator('.modal-content').waitFor({ state: 'visible' });
    await page.locator('#config').fill(JSON.stringify(VALID_CONFIGS.basicOffice, null, 2));
    await page.locator('.modal-content button:has-text("Create")').click();
    await page.locator('.modal-content').waitFor({ state: 'hidden' });
    await page.waitForTimeout(1000);

    // Publish the version so it can be used in scenarios
    const versionCard = page.locator('.version-card').filter({ hasText: 'Version 1' });
    await versionCard.locator('button:has-text("Publish")').click();
    await page.locator('.modal-content button:has-text("Publish")').click();
    await page.locator('.modal-content').waitFor({ state: 'hidden' });
    await page.waitForTimeout(1000);
  });

  test.afterEach(async ({ page }) => {
    // Cleanup: delete the test system
    if (testSystemKey) {
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
    await page.locator('#liftSystem').selectOption({ index: 1 }); // Select first available system

    // Wait a bit for versions to load
    await page.waitForTimeout(500);

    // Select version
    await page.locator('#liftSystemVersion').selectOption({ index: 1 }); // Select first available version

    // Set duration
    await page.locator('#durationTicks').fill('200');

    // Add passenger flow
    const addFlowButton = page.locator('button:has-text("Add Passenger Flow")');
    if (await addFlowButton.isVisible()) {
      await addFlowButton.click();

      // Fill in flow details
      await page.locator('input[placeholder*="Start Tick"]').first().fill('0');
      await page.locator('input[placeholder*="Origin"]').first().fill('0');
      await page.locator('input[placeholder*="Destination"]').first().fill('5');
      await page.locator('input[placeholder*="Passengers"]').first().fill('3');
    }

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
    await page.locator('#liftSystem').selectOption({ index: 1 });
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
    const alertModal = page.locator('.modal');
    await expect(alertModal).toBeVisible({ timeout: 3000 });
    await expect(page.locator('text=Scenario is valid!')).toBeVisible();

    // Close the alert
    await page.locator('.modal button:has-text("OK")').click();
    await expect(alertModal).toBeHidden();

    // Click Create Scenario button to save
    await page.locator('button:has-text("Create Scenario")').click();

    // Wait for navigation back to scenarios page
    await page.waitForURL(/\/scenarios$/, { timeout: 5000 });

    // Verify scenario was created
    await page.waitForTimeout(1000);
    await expect(page.locator('text=Advanced JSON Test Scenario')).toBeVisible();

    // Navigate to edit the scenario to verify it was saved correctly
    const scenarioRow = page.locator('tr, .scenario-card').filter({ hasText: 'Advanced JSON Test Scenario' });
    const editButton = scenarioRow.locator('button:has-text("Edit"), a:has-text("Edit")');
    
    if (await editButton.isVisible()) {
      await editButton.click();
      await page.waitForURL(/\/scenarios\/\d+\/edit/);

      // Switch to Advanced JSON Mode to verify the saved data
      const advancedJsonButton = page.locator('button:has-text("Switch to Advanced JSON Mode")');
      if (await advancedJsonButton.isVisible()) {
        await advancedJsonButton.click();
      }

      // Verify the JSON matches what we saved
      const savedJson = await page.locator('.json-editor').inputValue();
      const parsedJson = JSON.parse(savedJson);

      expect(parsedJson.durationTicks).toBe(250);
      expect(parsedJson.passengerFlows).toHaveLength(3);
      expect(parsedJson.passengerFlows[0].originFloor).toBe(0);
      expect(parsedJson.passengerFlows[0].destinationFloor).toBe(5);
      expect(parsedJson.seed).toBe(42);
    }
  });

  test('TC_SCENARIO_003: Edit existing scenario using Advanced JSON Mode', async ({ page }) => {
    // First, create a scenario using the form
    await page.goto('/scenarios');
    await page.waitForLoadState('domcontentloaded');
    await page.locator('button:has-text("Create New Scenario")').click();
    await page.waitForURL(/\/scenarios\/new/);

    await page.locator('#scenarioName').fill('Scenario To Edit');
    await page.locator('#liftSystem').selectOption({ index: 1 });
    await page.waitForTimeout(500);
    await page.locator('#liftSystemVersion').selectOption({ index: 1 });
    await page.locator('#durationTicks').fill('100');

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
    const alertModal = page.locator('.modal');
    if (await alertModal.isVisible()) {
      await page.locator('.modal button:has-text("OK")').click();
    }

    // Update the scenario
    await page.locator('button:has-text("Update Scenario")').click();

    // Wait for navigation
    await page.waitForURL(/\/scenarios$/, { timeout: 5000 });

    // Go back to edit and verify changes were saved
    await page.waitForTimeout(1000);
    const updatedScenarioRow = page.locator('tr, .scenario-card').filter({ hasText: 'Scenario To Edit' });
    const editButton2 = updatedScenarioRow.locator('button:has-text("Edit"), a:has-text("Edit")');
    await editButton2.click();
    await page.waitForURL(/\/scenarios\/\d+\/edit/);

    // Switch to Advanced JSON Mode
    const advancedJsonButton = page.locator('button:has-text("Switch to Advanced JSON Mode")');
    if (await advancedJsonButton.isVisible()) {
      await advancedJsonButton.click();
    }

    // Verify the updated values
    const savedJson = await page.locator('.json-editor').inputValue();
    const parsedJson = JSON.parse(savedJson);

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
    await page.locator('#liftSystem').selectOption({ index: 1 });
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
    const alertModal = page.locator('.modal');
    await expect(alertModal).toBeVisible({ timeout: 3000 });
    await expect(page.locator('text=/Invalid JSON format/i')).toBeVisible();

    // Close the alert
    await page.locator('.modal button').click();

    // Try to save - should also show error
    await page.locator('button:has-text("Create Scenario")').click();
    await page.waitForTimeout(1000);

    // Should show error message and stay on the same page
    await expect(page.locator('text=/Invalid JSON format/i')).toBeVisible();
  });
});
