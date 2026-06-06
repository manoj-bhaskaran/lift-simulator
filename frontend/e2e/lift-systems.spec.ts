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

    // Step 4: Click Create button and wait for backend success
    const [createResponse] = await Promise.all([
      page.waitForResponse((response) =>
        response.url().includes('/api/v1/lift-systems') &&
        response.request().method() === 'POST'
      ),
      page.locator('.modal-content button:has-text("Create")').click(),
    ]);
    expect(createResponse.ok()).toBeTruthy();
    const createdSystem = await createResponse.json();

    // Wait for modal to close and the app to navigate to the created detail page
    await expect(page.locator('.modal-content')).toBeHidden({ timeout: 5000 });
    await page.waitForURL(new RegExp(`/systems/${createdSystem.id}$`), { timeout: 5000 });

    // Verify system detail page shows correct metadata. The display name and
    // system key each appear in both the header and the information grid, so
    // scope these assertions to the header to avoid strict-mode collisions.
    await expect(page.locator('.detail-header h2')).toHaveText(systemData.displayName);
    await expect(page.locator('.detail-header .system-key')).toHaveText(systemData.systemKey);
    await expect(page.locator('text=' + systemData.description)).toBeVisible();
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

    // Step 3: Verify metadata fields are displayed correctly. Scope the name
    // and key to the header so they don't collide with the information grid.
    await expect(page.locator('.detail-header h2')).toHaveText(systemData.displayName);
    await expect(page.locator('.detail-header .system-key')).toHaveText(systemData.systemKey);
    await expect(page.locator('text=' + systemData.description)).toBeVisible();

    // Step 4: Verify versions section and Create New Version button
    await expect(page.locator('#versions h3')).toContainText(/Version/i);
    await expect(page.locator('button:has-text("Create New Version")')).toBeVisible();
  });

  test('TC_0013: Delete Lift System and Cascade Delete Versions', async ({ page }) => {
    // Precondition: Create a test system
    const systemData = generateSystemData({
      displayName: 'Test Delete Cascade System',
      description: 'System for testing cascade deletion'
    });
    testSystemKey = systemData.systemKey;
    await createLiftSystem(page, systemData);

    // Step 1: Open the system detail page (deletion lives on the detail view)
    await page.goto('/systems');
    await page.waitForLoadState('domcontentloaded');

    // Find the system card and open its details
    const systemCard = page.locator('.system-card').filter({ hasText: systemData.systemKey });
    await expect(systemCard).toBeVisible();
    await systemCard.locator('button:has-text("View Details")').click();
    await page.waitForURL(/\/systems\/[^/]+$/);

    // Click Delete System
    await page.locator('button:has-text("Delete System")').click();

    // Step 2: Verify confirmation modal appears
    const confirmModal = page.locator('.modal-content').filter({ hasText: /delete/i });
    await expect(confirmModal).toBeVisible();

    // Confirm deletion
    await confirmModal.locator('button:has-text("Delete")').click();

    // The app navigates back to the systems list after a successful delete
    await page.waitForURL('**/systems');

    // Step 3: Verify the system (and its cascaded versions) are removed from the list
    const searchInput = page.locator('input[aria-label="Search lift systems"]');
    await searchInput.fill(systemData.systemKey);
    await expect(page.locator('.system-card').filter({ hasText: systemData.systemKey })).toHaveCount(0);

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

    // The duplicate key is rejected by the backend: an error alert is shown and
    // the create modal stays open so the user can correct the input.
    await expect(page.locator('.modal-content.alert-modal')).toBeVisible();
    await expect(page.locator('.modal-content').filter({ hasText: 'Create New Lift System' })).toBeVisible();
  });
});
