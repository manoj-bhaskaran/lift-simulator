import { test, expect } from '@playwright/test';
import {
  generateSystemData,
  createLiftSystem,
  navigateToSystemDetail,
  createConfigVersion,
  openCreateVersionForm,
  fillAndValidateVersionConfig,
  cleanupSystemIfExists,
  isBackendAvailable,
  VALID_CONFIGS,
  INVALID_CONFIGS
} from './helpers/test-helpers';

/**
 * Configuration Version Lifecycle Tests
 *
 * Manual Test Case Mapping:
 * - TC_0005: Create Valid Configuration Version (Draft)
 * - TC_0006: Reject Invalid Configuration Version
 * - TC_0007: Create Valid Configuration After Fix
 * - TC_0008: Edit Draft Version and Save
 * - TC_0009: Publish Version and Auto-Archive Previous
 */

test.describe('Configuration Version Management', () => {
  let testSystemKey: string;

  test.beforeEach(async ({ page }) => {
    // Check if backend is available
    const backendAvailable = await isBackendAvailable(page);
    if (!backendAvailable) {
      test.skip();
    }

    // Create a test system for all version tests
    const systemData = generateSystemData({
      displayName: 'Test Building A'
    });
    testSystemKey = systemData.systemKey;
    await createLiftSystem(page, systemData);
  });

  test.afterEach(async ({ page }) => {
    // Cleanup: delete the test system
    if (testSystemKey) {
      await cleanupSystemIfExists(page, testSystemKey);
    }
  });

  test('TC_0005: Create Valid Configuration Version (Draft)', async ({ page }) => {
    // Navigate to system detail page
    await navigateToSystemDetail(page, testSystemKey);

    // Step 1: Click Create New Version button
    await openCreateVersionForm(page);

    // Verify inline form opens with version number shown
    await expect(page.locator('.create-version-form')).toBeVisible();
    await expect(page.locator('.create-version-form h4')).toHaveText(/Version 1/i);

    const configField = page.locator('#config');
    await expect(configField).toHaveAttribute('placeholder', /"doorTransitionTicks"/);
    await expect(configField).not.toHaveAttribute('placeholder', /\.\.\./);
    await expect(page.locator('#config-help')).toContainText('Required integer fields');
    await expect(page.locator('#config-required-fields')).toContainText('controllerStrategy');
    await expect(page.locator('.create-version-form a:has-text("View schema docs")')).toBeVisible();

    // Step 2: Paste valid configuration JSON
    const validConfig = VALID_CONFIGS.basicOffice;
    await page.locator('#config').fill(JSON.stringify(validConfig, null, 2));

    // Verify JSON is accepted
    const configValue = await page.locator('#config').inputValue();
    expect(configValue).toContain('"minFloor"');

    // Step 3: Validate and click Create Version button
    await page.locator('.create-version-form button:has-text("Validate")').click();
    await expect(page.locator('.validation-success-banner')).toBeVisible({ timeout: 5000 });
    await page.locator('.create-version-form button:has-text("Create Version")').click();

    // Wait for inline form to close
    await expect(page.locator('.create-version-form')).toBeHidden({ timeout: 5000 });

    // Step 4: Verify version appears with status DRAFT
    await page.waitForTimeout(1000); // Wait for UI to update

    const versionCard = page.locator('.version-card').filter({ hasText: 'Version 1' });
    await expect(versionCard).toBeVisible();

    const statusBadge = versionCard.locator('.status-badge');
    await expect(statusBadge).toBeVisible();
    await expect(statusBadge).toHaveText(/DRAFT/i);
  });

  test('Guided form is the default and creates a version', async ({ page }) => {
    // Navigate to system detail page
    await navigateToSystemDetail(page, testSystemKey);

    // Open the create form; the guided form is the default experience
    await openCreateVersionForm(page, 'guided');

    // The guided form is shown and the raw JSON editor is hidden by default
    await expect(page.locator('.version-config-grid')).toBeVisible();
    await expect(page.locator('#config')).toBeHidden();

    // Fields are pre-filled with sensible defaults
    const liftsInput = page.locator('#vcf-lifts');
    await expect(liftsInput).toBeVisible();
    // v1.0.0 supports only 1 lift
    await expect(liftsInput).toHaveValue('1');

    // Switching to Advanced (JSON) preserves the entered data
    await page.locator('.editor-mode-toggle button:has-text("Advanced")').click();
    const jsonValue = await page.locator('#config').inputValue();
    expect(jsonValue).toContain('"lifts": 1');

    // Switching back to the guided form retains the value
    await page.locator('.editor-mode-toggle button:has-text("Guided")').click();
    await expect(page.locator('#vcf-lifts')).toHaveValue('1');

    // Validate and create the version from the guided form
    await page.locator('.create-version-form button:has-text("Validate")').click();
    await expect(page.locator('.validation-success-banner')).toBeVisible({ timeout: 5000 });
    await page.locator('.create-version-form button:has-text("Create Version")').click();
    await expect(page.locator('.create-version-form')).toBeHidden({ timeout: 5000 });

    // Verify the version appears with status DRAFT
    await page.waitForTimeout(1000);
    const versionCard = page.locator('.version-card').filter({ hasText: 'Version 1' });
    await expect(versionCard).toBeVisible();
    await expect(versionCard.locator('.status-badge')).toHaveText(/DRAFT/i);
  });

  test('TC_0006: Reject Invalid Configuration Version', async ({ page }) => {
    // Navigate to system detail page
    await navigateToSystemDetail(page, testSystemKey);

    // Step 1: Click Create New Version button
    await openCreateVersionForm(page);

    // Step 2: Paste invalid configuration JSON
    const invalidConfig = INVALID_CONFIGS.invalidExample;
    await page.locator('#config').fill(JSON.stringify(invalidConfig, null, 2));

    // Step 3: Validate configuration
    await page.locator('.create-version-form button:has-text("Validate")').click();

    // Verify validation fails and errors are displayed
    await expect(page.locator('.validation-error-banner')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('.validation-error-banner')).toContainText(/error|invalid|has errors/i);
    await expect(page.locator('.create-version-form button:has-text("Create Version")')).toBeDisabled();

    // Close the form
    await page.locator('.create-version-form button:has-text("Cancel")').click();
    await expect(page.locator('.create-version-form')).toBeHidden();

    // Step 4: Verify no new version was created
    await page.waitForTimeout(500);
    const versionCards = page.locator('.version-card');
    const versionCount = await versionCards.count();
    expect(versionCount).toBe(0);
  });

  test('TC_0007: Create Valid Configuration After Fix', async ({ page }) => {
    // This test demonstrates fixing an invalid config and successfully creating a version

    // Navigate to system detail page
    await navigateToSystemDetail(page, testSystemKey);

    // First attempt with invalid config (to show the fix workflow)
    await openCreateVersionForm(page);

    // Try invalid config first
    await page.locator('#config').fill(JSON.stringify(INVALID_CONFIGS.tooFewFloors, null, 2));
    await page.locator('.create-version-form button:has-text("Validate")').click();

    // Should fail - form stays open and Create Version stays disabled
    await expect(page.locator('.create-version-form')).toBeVisible();
    await expect(page.locator('.validation-error-banner')).toBeVisible({ timeout: 5000 });
    await expect(page.locator('.create-version-form button:has-text("Create Version")')).toBeDisabled();

    // Step 1: Now paste corrected configuration JSON
    const validConfig = VALID_CONFIGS.basicOffice;
    await page.locator('#config').clear();
    await fillAndValidateVersionConfig(page, validConfig);

    // Step 2: Click Create Version button
    await page.locator('.create-version-form button:has-text("Create Version")').click();

    // Wait for inline form to close
    await expect(page.locator('.create-version-form')).toBeHidden({ timeout: 5000 });

    // Step 3: Verify version is created with status DRAFT
    await page.waitForTimeout(1000);

    const versionCard = page.locator('.version-card').filter({ hasText: 'Version 1' });
    await expect(versionCard).toBeVisible();

    const statusBadge = versionCard.locator('.status-badge');
    await expect(statusBadge).toHaveText(/DRAFT/i);
  });

  test('TC_0008: Edit Draft Version and Save', async ({ page }) => {
    // Precondition: Create a DRAFT version
    await createConfigVersion(page, testSystemKey, VALID_CONFIGS.basicOffice);

    // Navigate back to system detail page
    await navigateToSystemDetail(page, testSystemKey);

    // Step 1: Click Edit for the DRAFT version
    const versionCard = page.locator('.version-card').filter({ hasText: 'Version 1' });
    await expect(versionCard).toBeVisible();

    await versionCard.locator('a:has-text("Edit Config")').click();

    // Wait for navigation to editor
    await page.waitForURL(/\/systems\/[^/]+\/versions\/\d+\/edit/);

    // Verify editor opens with existing configuration
    const configTextarea = page.locator('.config-textarea');
    await expect(configTextarea).toBeVisible();

    const existingConfig = await configTextarea.inputValue();
    expect(existingConfig).toContain('"minFloor"');

    // Step 2: Replace content with high-rise-residential.json
    const newConfig = VALID_CONFIGS.highRiseResidential;
    await configTextarea.clear();
    await configTextarea.fill(JSON.stringify(newConfig, null, 2));

    // Step 3: Click Validate button
    await page.locator('button:has-text("Validate")').click();

    // Wait for validation response
    await page.waitForTimeout(1500);

    // Verify validation passes
    const validationSection = page.locator('.validation-section');
    await expect(validationSection).toBeVisible();

    const successIndicator = page.locator('.validation-success');
    await expect(successIndicator).toBeVisible();

    // Step 4: Click Save Draft button
    await page.locator('button:has-text("Save Draft")').click();

    // Wait for save confirmation
    await page.waitForTimeout(1500);

    // Verify success message or that unsaved indicator is gone
    const unsavedIndicator = page.locator('.unsaved-indicator');
    await expect(unsavedIndicator).toBeHidden();

    // Step 5: Re-open editor to verify configuration persists
    await navigateToSystemDetail(page, testSystemKey);
    await versionCard.locator('a:has-text("Edit Config")').click();
    await page.waitForURL(/\/systems\/[^/]+\/versions\/\d+\/edit/);

    // Verify the updated configuration is present
    const updatedConfig = await page.locator('.config-textarea').inputValue();
    expect(updatedConfig).toContain('"maxFloor": 29'); // From high-rise-residential.json
  });

  test('TC_0009: Publish Version and Auto-Archive Previous', async ({ page }) => {
    // Precondition: Create two DRAFT versions
    await createConfigVersion(page, testSystemKey, VALID_CONFIGS.basicOffice);
    await createConfigVersion(page, testSystemKey, VALID_CONFIGS.highRiseResidential);

    // Navigate to system detail page
    await navigateToSystemDetail(page, testSystemKey);

    // Verify both versions are DRAFT
    await page.waitForTimeout(1000);

    const version1Card = page.locator('.version-card').filter({ hasText: 'Version 1' });
    const version2Card = page.locator('.version-card').filter({ hasText: 'Version 2' });

    await expect(version1Card).toBeVisible();
    await expect(version2Card).toBeVisible();

    // Step 1: Publish Version 1
    await version1Card.locator('button:has-text("Publish")').click();

    // Confirm in modal
    await page.locator('.modal-content button:has-text("Publish")').click();
    await expect(page.locator('.modal-content')).toBeHidden({ timeout: 5000 });

    // Wait for status update
    await page.waitForTimeout(1500);

    // Verify Version 1 becomes PUBLISHED
    await expect(version1Card.locator('.status-badge')).toHaveText(/PUBLISHED/i);

    // Step 2: Publish Version 2
    await version2Card.locator('button:has-text("Publish")').click();

    // Confirm in modal
    await page.locator('.modal-content button:has-text("Publish")').click();
    await expect(page.locator('.modal-content')).toBeHidden({ timeout: 5000 });

    // Wait for status update
    await page.waitForTimeout(1500);

    // Step 3: Verify Version 2 becomes PUBLISHED
    await expect(version2Card.locator('.status-badge')).toHaveText(/PUBLISHED/i);

    // Verify Version 1 is automatically ARCHIVED
    await expect(version1Card.locator('.status-badge')).toHaveText(/ARCHIVED/i);

    // Verify only one version remains PUBLISHED
    const publishedBadges = page.locator('.status-badge').filter({ hasText: /PUBLISHED/i });
    const publishedCount = await publishedBadges.count();
    expect(publishedCount).toBe(1);
  });

  test('Cannot edit published version', async ({ page }) => {
    // Create and publish a version
    await createConfigVersion(page, testSystemKey, VALID_CONFIGS.basicOffice);
    await navigateToSystemDetail(page, testSystemKey);

    const versionCard = page.locator('.version-card').filter({ hasText: 'Version 1' });

    // Publish the version
    await versionCard.locator('button:has-text("Publish")').click();
    await page.locator('.modal-content button:has-text("Publish")').click();
    await expect(page.locator('.modal-content')).toBeHidden({ timeout: 5000 });
    await page.waitForTimeout(1500);

    // Verify Edit button is not available for published version
    // The link text should change to "View Config" or edit button should be hidden
    const editLink = versionCard.locator('a:has-text("Edit Config")');
    const editLinkVisible = await editLink.isVisible();

    if (editLinkVisible) {
      // If edit link exists, it should open in read-only mode
      await editLink.click();
      await page.waitForURL(/\/systems\/[^/]+\/versions\/\d+\/edit/);

      // Save Draft button should not be visible for published versions
      const saveDraftButton = page.locator('button:has-text("Save Draft")');
      await expect(saveDraftButton).toBeHidden();
    } else {
      // Edit link should not be present at all
      expect(editLinkVisible).toBe(false);
    }
  });
});
