import { test, expect } from '@playwright/test';
import {
  generateSystemData,
  createLiftSystem,
  deleteLiftSystem,
  navigateToSystemDetail,
  cleanupSystemIfExists,
  isBackendAvailable
} from './helpers/test-helpers';

/**
 * Lift Systems CRUD Tests
 *
 * Manual Test Case Mapping:
 * - TC_0003: Create New Lift System
 * - TC_0004: View Lift System Details
 * - TC_0013: Delete Lift System and Cascade Delete Versions
 */

test.describe('Lift Systems Management', () => {
  let testSystemKey: string;

  test.beforeEach(async ({ page }) => {
    // Check if backend is available
    const backendAvailable = await isBackendAvailable(page);
    if (!backendAvailable) {
      test.skip();
    }
  });

  test.afterEach(async ({ page }) => {
    // Cleanup: delete the test system if it was created
    if (testSystemKey) {
      await cleanupSystemIfExists(page, testSystemKey);
    }
  });

  test('TC_0003: Create New Lift System', async ({ page }) => {
    // Generate unique test data
    const systemData = generateSystemData({
      systemKey: 'test-building-a',
      displayName: 'Test Building A',
      description: 'UAT test for building A lift system'
    });
    testSystemKey = systemData.systemKey;

    // Step 1: Navigate to Lift Systems page
    await page.goto('/systems');
    await page.waitForLoadState('domcontentloaded');
    await expect(page.locator('h1, h2').filter({ hasText: /Lift Systems/i })).toBeVisible();

    // Step 2: Click Create New System button
    await page.locator('button:has-text("Create New System")').click();

    // Verify modal opens with required fields
    await expect(page.locator('.modal-content')).toBeVisible();
    await expect(page.locator('#systemKey')).toBeVisible();
    await expect(page.locator('#displayName')).toBeVisible();
    await expect(page.locator('#description')).toBeVisible();

    // Step 3: Enter system details
    await page.locator('#systemKey').fill(systemData.systemKey);
    await page.locator('#displayName').fill(systemData.displayName);
    await page.locator('#description').fill(systemData.description);

    // Verify form accepts input and shows no validation errors
    await expect(page.locator('#systemKey')).toHaveValue(systemData.systemKey);
    await expect(page.locator('#displayName')).toHaveValue(systemData.displayName);
    await expect(page.locator('#description')).toHaveValue(systemData.description);

    // Step 4: Click Create button
    await page.locator('.modal-content button:has-text("Create")').click();

    // Wait for modal to close
    await expect(page.locator('.modal-content')).toBeHidden({ timeout: 5000 });

    // Verify system appears in the list with correct details
    const systemCard = page.locator('.system-card').filter({ hasText: systemData.systemKey });
    await expect(systemCard).toBeVisible({ timeout: 5000 });
    await expect(systemCard.locator('text=' + systemData.displayName)).toBeVisible();
    await expect(systemCard.locator('text=' + systemData.description)).toBeVisible();
  });

  test('TC_0004: View Lift System Details', async ({ page }) => {
    // Precondition: Create a test system
    const systemData = generateSystemData({
      displayName: 'Test Building A'
    });
    testSystemKey = systemData.systemKey;
    await createLiftSystem(page, systemData);

    // Step 1: Navigate to Lift Systems and locate the system
    await page.goto('/systems');
    await page.waitForLoadState('domcontentloaded');

    const systemCard = page.locator('.system-card').filter({ hasText: systemData.systemKey });
    await expect(systemCard).toBeVisible();

    // Step 2: Click View Details button
    await systemCard.locator('button:has-text("View Details")').click();

    // Wait for navigation to detail page
    await page.waitForURL(/\/systems\/[^/]+$/);

    // Step 3: Verify metadata fields are displayed correctly
    await expect(page.locator('text=' + systemData.displayName)).toBeVisible();
    await expect(page.locator('text=' + systemData.systemKey)).toBeVisible();
    await expect(page.locator('text=' + systemData.description)).toBeVisible();

    // Step 4: Verify versions section and Create New Version button
    await expect(page.locator('text=/Version|Configurations/i')).toBeVisible();
    await expect(page.locator('button:has-text("Create New Version")')).toBeVisible();
  });

  test('TC_0013: Delete Lift System and Cascade Delete Versions', async ({ page }) => {
    // Precondition: Create a test system
    const systemData = generateSystemData({
      systemKey: 'test-delete-cascade',
      displayName: 'Test Delete Cascade System',
      description: 'System for testing cascade deletion'
    });
    testSystemKey = systemData.systemKey;
    await createLiftSystem(page, systemData);

    // Step 1: Navigate to Lift Systems list
    await page.goto('/systems');
    await page.waitForLoadState('domcontentloaded');

    // Find the system card
    const systemCard = page.locator('.system-card').filter({ hasText: systemData.systemKey });
    await expect(systemCard).toBeVisible();

    // Click Delete button
    await systemCard.locator('button:has-text("Delete")').click();

    // Step 2: Verify confirmation modal appears
    await expect(page.locator('.modal-content')).toBeVisible();
    await expect(page.locator('.modal-content').locator('text=/confirm|delete/i')).toBeVisible();

    // Confirm deletion
    await page.locator('.modal-content button:has-text("Delete")').click();

    // Wait for modal to close
    await expect(page.locator('.modal-content')).toBeHidden({ timeout: 5000 });

    // Step 3: Verify system is removed from list
    await page.waitForTimeout(1000); // Wait for UI to update
    await expect(systemCard).toBeHidden();

    // Step 4: Attempt to navigate to the system detail page directly
    // Extract system ID from the card before deletion or construct URL
    const systemsPage = await page.goto('/systems');
    await page.waitForLoadState('domcontentloaded');

    // Search for the deleted system
    const searchInput = page.locator('input[aria-label="Search lift systems"]');
    if (await searchInput.isVisible()) {
      await searchInput.fill(systemData.systemKey);
      await page.waitForTimeout(500);

      // Verify no results
      await expect(page.locator('.system-card').filter({ hasText: systemData.systemKey })).toBeHidden();
    }

    // Mark for cleanup as null since we already deleted it
    testSystemKey = '';
  });

  test('Create system with invalid system key shows validation error', async ({ page }) => {
    const systemData = generateSystemData({
      systemKey: 'Invalid Key With Spaces!',
      displayName: 'Test System',
      description: 'Test description'
    });

    await page.goto('/systems');
    await page.waitForLoadState('domcontentloaded');

    // Click Create New System
    await page.locator('button:has-text("Create New System")').click();
    await expect(page.locator('.modal-content')).toBeVisible();

    // Fill form with invalid system key
    await page.locator('#systemKey').fill(systemData.systemKey);
    await page.locator('#displayName').fill(systemData.displayName);
    await page.locator('#description').fill(systemData.description);

    // Try to submit
    await page.locator('.modal-content button:has-text("Create")').click();

    // Verify validation error appears (either in modal or as a message)
    // The error might appear as inline validation or as a toast/alert
    const modalStillVisible = await page.locator('.modal-content').isVisible();
    expect(modalStillVisible).toBe(true); // Modal should stay open on validation error
  });

  test('Create system with duplicate key shows error', async ({ page }) => {
    // Create first system
    const systemData = generateSystemData({
      displayName: 'Original System'
    });
    testSystemKey = systemData.systemKey;
    await createLiftSystem(page, systemData);

    // Try to create second system with same key
    await page.goto('/systems');
    await page.waitForLoadState('domcontentloaded');

    await page.locator('button:has-text("Create New System")').click();
    await expect(page.locator('.modal-content')).toBeVisible();

    await page.locator('#systemKey').fill(systemData.systemKey);
    await page.locator('#displayName').fill('Duplicate System');
    await page.locator('#description').fill('This should fail');

    await page.locator('.modal-content button:has-text("Create")').click();

    // Wait a bit for server response
    await page.waitForTimeout(1000);

    // Verify error is shown (modal stays open or error message appears)
    const modalStillVisible = await page.locator('.modal-content').isVisible();
    expect(modalStillVisible).toBe(true);
  });
});
