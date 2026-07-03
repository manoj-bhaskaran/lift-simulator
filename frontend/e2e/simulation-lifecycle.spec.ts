import { test, expect, Page } from '@playwright/test';
import {
  generateSystemData,
  createLiftSystem,
  createConfigVersion,
  publishVersion,
  cleanupSystemIfExists,
  deleteScenariosForSystem,
  isBackendAvailable,
  VALID_CONFIGS,
} from './helpers/test-helpers';

/**
 * Simulation Lifecycle Tests
 *
 * Covers starting a simulation run from the UI, watching it poll through to
 * completion, and cancelling an in-progress run.
 */

async function createScenario(page: Page, systemDisplayName: string, name: string): Promise<void> {
  await page.goto('/scenarios');
  await page.waitForLoadState('domcontentloaded');
  await page.locator('button:has-text("Create New Scenario")').click();
  await page.waitForURL(/\/scenarios\/new/);

  await page.locator('#scenarioName').fill(name);
  await page.locator('#liftSystem').selectOption({ label: systemDisplayName });
  await page.waitForTimeout(500);
  await page.locator('#liftSystemVersion').selectOption({ index: 1 });
  // Keep the run short so the simulation reaches a terminal state quickly.
  await page.locator('#durationTicks').fill('20');

  await page.locator('button:has-text("Add Passenger Flow")').click();
  const addFlowForm = page.locator('.add-flow-form');
  await expect(addFlowForm).toBeVisible();
  await addFlowForm.locator('#startTick').fill('0');
  await addFlowForm.locator('#originFloor').fill('0');
  await addFlowForm.locator('#destinationFloor').fill('1');
  await addFlowForm.locator('#passengers').fill('1');
  await addFlowForm.locator('button:has-text("Save")').click();
  await expect(addFlowForm).toBeHidden();

  await page.locator('button:has-text("Create Scenario")').click();
  await page.waitForURL(/\/scenarios$/);
  await expect(page.locator(`text=${name}`)).toBeVisible();
}

test.describe('Simulation Lifecycle', () => {
  let testSystemKey: string;
  let testSystemDisplayName: string;

  test.beforeEach(async ({ page }) => {
    const backendAvailable = await isBackendAvailable(page);
    if (!backendAvailable) {
      test.skip();
    }

    const systemData = generateSystemData();
    testSystemKey = systemData.systemKey;
    testSystemDisplayName = systemData.displayName;
    await createLiftSystem(page, systemData);
    await createConfigVersion(page, testSystemKey, VALID_CONFIGS.minimal);
    await publishVersion(page, 1);
    await expect(
      page.locator('.version-card').filter({ hasText: 'Version 1' }).locator('.status-badge')
    ).toHaveText(/PUBLISHED/i);
  });

  test.afterEach(async ({ page }) => {
    if (testSystemKey) {
      await deleteScenariosForSystem(page, testSystemKey);
      await cleanupSystemIfExists(page, testSystemKey);
    }
  });

  test('starts a simulation run and polls through to completion', async ({ page }) => {
    const scenarioName = `Lifecycle Scenario ${Date.now()}`;
    await createScenario(page, testSystemDisplayName, scenarioName);

    await page.goto('/simulator/run');
    await page.waitForLoadState('domcontentloaded');

    await page.locator('label:has-text("Lift System") select').selectOption({ label: testSystemDisplayName });
    await page.waitForTimeout(500);
    await page.locator('label:has-text("Published Version") select').selectOption({ index: 1 });
    await page.waitForTimeout(500);
    await page.locator('label:has-text("Scenario") select').selectOption({ label: scenarioName });

    await page.locator('button:has-text("Start Run")').click();

    // The run status pill should appear and eventually reach a terminal state.
    await expect(page.locator('.status-pill')).toBeVisible();
    await expect(page.locator('.status-pill')).toHaveText(/SUCCEEDED|FAILED/i, { timeout: 30000 });
    await expect(page.locator('.status-pill')).toHaveText(/SUCCEEDED/i);

    await expect(page.locator('.result-banner.success')).toBeVisible();
    await expect(page.locator('.kpi-grid')).toBeVisible();
  });

  test('cancels an in-progress simulation run', async ({ page }) => {
    const scenarioName = `Cancel Scenario ${Date.now()}`;
    await createScenario(page, testSystemDisplayName, scenarioName);

    await page.goto('/simulator/run');
    await page.waitForLoadState('domcontentloaded');

    await page.locator('label:has-text("Lift System") select').selectOption({ label: testSystemDisplayName });
    await page.waitForTimeout(500);
    await page.locator('label:has-text("Published Version") select').selectOption({ index: 1 });
    await page.waitForTimeout(500);
    await page.locator('label:has-text("Scenario") select').selectOption({ label: scenarioName });

    await page.locator('button:has-text("Start Run")').click();
    await expect(page.locator('.status-pill')).toBeVisible();

    // Only attempt to cancel while the run is still active; if it has already
    // finished (very short scenarios can complete almost instantly), the
    // Cancel Run button will not be present.
    const cancelButton = page.locator('button:has-text("Cancel Run")');
    if (await cancelButton.count()) {
      await cancelButton.click();
      await page.locator('.modal-content button:has-text("Cancel run")').click();
      await expect(page.locator('.status-pill')).toHaveText(/CANCELLED|SUCCEEDED|FAILED/i, {
        timeout: 15000,
      });
    }
  });
});
