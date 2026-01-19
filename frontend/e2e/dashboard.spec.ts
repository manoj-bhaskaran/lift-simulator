import { test, expect } from '@playwright/test';
import {
  generateSystemData,
  createLiftSystem,
  createConfigVersion,
  getDashboardMetrics,
  countLiftSystems,
  countVersionsForSystem,
  cleanupSystemIfExists,
  isBackendAvailable,
  VALID_CONFIGS
} from './helpers/test-helpers';

/**
 * Dashboard Tests
 *
 * Manual Test Case Mapping:
 * - TC_0017: Dashboard Aggregate Counts Validation
 */

test.describe('Dashboard Metrics', () => {
  const testSystems: string[] = [];

  test.beforeEach(async ({ page }) => {
    // Check if backend is available
    const backendAvailable = await isBackendAvailable(page);
    if (!backendAvailable) {
      test.skip();
    }
  });

  test.afterEach(async ({ page }) => {
    // Cleanup: delete all test systems created
    for (const systemKey of testSystems) {
      await cleanupSystemIfExists(page, systemKey);
    }
    testSystems.length = 0; // Clear array
  });

  test('TC_0017: Dashboard Aggregate Counts Validation', async ({ page }) => {
    // Step 1: Navigate to Dashboard
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');

    // Verify Dashboard loads
    await expect(page.locator('h2:has-text("Dashboard")')).toBeVisible();

    // Verify metrics tiles are visible
    await expect(page.locator('text=/Number of.*Lift System/i')).toBeVisible();
    await expect(page.locator('text=/Number of.*Version/i')).toBeVisible();

    // Step 2: Note initial counts
    const initialMetrics = await getDashboardMetrics(page);
    const initialSystems = initialMetrics.systems;
    const initialVersions = initialMetrics.versions;

    // Step 3: Create test systems with versions
    // Create first system with 2 versions
    const system1Data = generateSystemData({
      displayName: 'Test System 1 for Dashboard'
    });
    testSystems.push(system1Data.systemKey);
    await createLiftSystem(page, system1Data);
    await createConfigVersion(page, system1Data.systemKey, VALID_CONFIGS.basicOffice);
    await createConfigVersion(page, system1Data.systemKey, VALID_CONFIGS.highRiseResidential);

    // Create second system with 1 version
    const system2Data = generateSystemData({
      displayName: 'Test System 2 for Dashboard'
    });
    testSystems.push(system2Data.systemKey);
    await createLiftSystem(page, system2Data);
    await createConfigVersion(page, system2Data.systemKey, VALID_CONFIGS.minimal);

    // Step 4: Navigate to Lift Systems and count total systems
    const actualSystemsCount = await countLiftSystems(page);

    // Expected: initial + 2 new systems
    const expectedSystems = initialSystems + 2;

    // Step 5: Count versions for each system
    const system1Versions = await countVersionsForSystem(page, system1Data.systemKey);
    const system2Versions = await countVersionsForSystem(page, system2Data.systemKey);

    // Calculate expected total versions
    const expectedVersions = initialVersions + system1Versions + system2Versions;

    // Step 6: Return to Dashboard and refresh
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');

    // Wait a bit for metrics to update
    await page.waitForTimeout(1000);

    // Step 7: Compare Dashboard metrics
    const updatedMetrics = await getDashboardMetrics(page);

    // Verify Number of Lift Systems matches
    expect(updatedMetrics.systems).toBe(expectedSystems);
    expect(updatedMetrics.systems).toBe(actualSystemsCount);

    // Verify Number of Versions matches
    expect(updatedMetrics.versions).toBe(expectedVersions);

    // Log results for debugging
    console.log('Dashboard Metrics Validation:');
    console.log(`  Initial Systems: ${initialSystems}, Current: ${updatedMetrics.systems}, Expected: ${expectedSystems}`);
    console.log(`  Initial Versions: ${initialVersions}, Current: ${updatedMetrics.versions}, Expected: ${expectedVersions}`);
    console.log(`  System 1 Versions: ${system1Versions}, System 2 Versions: ${system2Versions}`);
  });

  test('Dashboard displays quick actions', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');

    // Verify Quick Actions section exists
    await expect(page.locator('text=/Quick Actions/i')).toBeVisible();

    // Verify action links are present
    const manageLiftSystemsLink = page.locator('a:has-text("Manage Lift Systems")');
    await expect(manageLiftSystemsLink).toBeVisible();

    const validateConfigLink = page.locator('a:has-text("Validate Configuration")');
    await expect(validateConfigLink).toBeVisible();

    // Verify links navigate correctly
    await manageLiftSystemsLink.click();
    await page.waitForURL('**/systems');
    await expect(page.locator('h1, h2').filter({ hasText: /Lift Systems/i })).toBeVisible();

    // Go back to dashboard
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');

    // Click Validate Configuration
    await validateConfigLink.click();
    await page.waitForURL('**/config-validator');
    await expect(page.locator('h2:has-text("Configuration Validator")')).toBeVisible();
  });

  test('Dashboard metrics update after system deletion', async ({ page }) => {
    // Create a test system
    const systemData = generateSystemData();
    testSystems.push(systemData.systemKey);
    await createLiftSystem(page, systemData);

    // Get metrics before deletion
    const metricsBefore = await getDashboardMetrics(page);

    // Delete the system
    await cleanupSystemIfExists(page, systemData.systemKey);

    // Remove from cleanup list since we already deleted it
    const index = testSystems.indexOf(systemData.systemKey);
    if (index > -1) {
      testSystems.splice(index, 1);
    }

    // Refresh dashboard
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(1000);

    // Get metrics after deletion
    const metricsAfter = await getDashboardMetrics(page);

    // Systems count should decrease by 1
    expect(metricsAfter.systems).toBe(metricsBefore.systems - 1);
  });

  test('Dashboard is accessible from navigation', async ({ page }) => {
    // Start from a different page
    await page.goto('/systems');
    await page.waitForLoadState('domcontentloaded');

    // Click Dashboard link in navigation
    const dashboardLink = page.locator('a[href="/"]').first();
    await dashboardLink.click();

    // Verify navigation to dashboard
    await page.waitForURL('/');
    await expect(page.locator('h2:has-text("Dashboard")')).toBeVisible();
  });
});
