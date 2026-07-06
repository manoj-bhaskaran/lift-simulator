import { test, expect } from '@playwright/test';
import { VALID_CONFIGS, INVALID_CONFIGS, isBackendAvailable } from './helpers/test-helpers';

/**
 * Configuration Validator Tests
 *
 * Manual Test Case Mapping:
 * - TC_0011: Validate Configuration Using Validator Tool
 */

test.describe('Configuration Validator', () => {
  test.beforeEach(async ({ page }) => {
    // Check if backend is available
    const backendAvailable = await isBackendAvailable(page);
    if (!backendAvailable) {
      test.skip();
    }
  });

  test('TC_0011: Validate Configuration Using Validator Tool', async ({ page }) => {
    // Navigate to Configuration Validator page
    await page.goto('/config-validator');
    await page.waitForLoadState('domcontentloaded');

    // Verify page loads
    await expect(page.locator('h2:has-text("Configuration Validator")')).toBeVisible();

    // Step 1: Paste valid config and validate
    const validConfig = VALID_CONFIGS.basicOffice;
    await page.locator('.config-editor').fill(JSON.stringify(validConfig, null, 2));

    // Click Validate Configuration button
    await page.locator('button:has-text("Validate Configuration")').click();

    // Wait for validation result to appear
    const successResult = page.locator('.result-success');
    await expect(successResult).toBeVisible({ timeout: 5000 });

    // Check for validation success text inside the visible result card.
    await expect(successResult).toContainText(/valid|success|passed/i);

    // Step 2: Paste invalid config and validate
    const invalidConfig = INVALID_CONFIGS.invalidExample;
    await page.locator('.config-editor').clear();
    await page.locator('.config-editor').fill(JSON.stringify(invalidConfig, null, 2));

    // Click Validate Configuration button
    await page.locator('button:has-text("Validate Configuration")').click();

    // Wait for validation result to appear
    const errorResult = page.locator('.result-error');
    await expect(errorResult).toBeVisible({ timeout: 5000 });

    // Check for error messages (e.g., "must be greater than", "cannot have", "is required")
    // Be more specific about error messages instead of generic error word
    await expect(errorResult).toContainText(/must be|cannot|is required|invalid/i);
  });

  test('Validator shows warnings distinctly from errors', async ({ page }) => {
    await page.goto('/config-validator');
    await page.waitForLoadState('domcontentloaded');

    // Use a valid config that might trigger warnings (if any)
    const validConfig = VALID_CONFIGS.large;
    await page.locator('.config-editor').fill(JSON.stringify(validConfig, null, 2));

    await page.locator('button:has-text("Validate Configuration")').click();

    // Wait for validation result
    const successResult = page.locator('.result-success');
    await expect(successResult).toBeVisible({ timeout: 5000 });

    // Verify the result is success - no errors should be present
    const errorResult = page.locator('.result-error');
    await expect(errorResult).not.toBeVisible();

    // Check if warnings section exists and verify they're visually distinct
    const warningsSection = page.locator('.warnings');
    const hasWarnings = await warningsSection.isVisible().catch(() => false);

    // If warnings exist, verify they don't make validation fail
    if (hasWarnings) {
      // Warnings section should have distinct styling (not same as error section)
      await expect(successResult).toBeVisible();
      await expect(warningsSection).toHaveClass(/warning/i);
      // Error result should not be visible when warnings exist
      await expect(errorResult).not.toBeVisible();
    }
  });

  test('Validator handles malformed JSON gracefully', async ({ page }) => {
    await page.goto('/config-validator');
    await page.waitForLoadState('domcontentloaded');

    // Enter malformed JSON
    await page.locator('.config-editor').fill('{ this is not valid JSON }');

    await page.locator('button:has-text("Validate Configuration")').click();

    // Should show error (either JSON parse error or validation error).
    const errorIndicator = page.locator('.result-error, .error-message').first();
    await expect(errorIndicator).toBeVisible({ timeout: 5000 });
    // Check for JSON parse or validation errors more specifically
    await expect(errorIndicator).toContainText(/JSON|parse|unexpected|cannot|invalid/i);
  });

  test('Validator handles boundary values correctly', async ({ page }) => {
    await page.goto('/config-validator');
    await page.waitForLoadState('domcontentloaded');

    // Test minimum valid configuration
    const minConfig = VALID_CONFIGS.minimal;
    await page.locator('.config-editor').fill(JSON.stringify(minConfig, null, 2));

    await page.locator('button:has-text("Validate Configuration")').click();

    // Should pass
    await expect(page.locator('.result-success')).toBeVisible({ timeout: 5000 });

    // Test large valid configuration
    await page.locator('.config-editor').clear();
    const largeConfig = VALID_CONFIGS.large;
    await page.locator('.config-editor').fill(JSON.stringify(largeConfig, null, 2));

    await page.locator('button:has-text("Validate Configuration")').click();

    // Should pass
    await expect(page.locator('.result-success')).toBeVisible({ timeout: 5000 });

    // Test homeFloor boundary (homeFloor = maxFloor)
    const boundaryConfig = {
      ...VALID_CONFIGS.basicOffice,
      homeFloor: 9
    };

    await page.locator('.config-editor').clear();
    await page.locator('.config-editor').fill(JSON.stringify(boundaryConfig, null, 2));

    await page.locator('button:has-text("Validate Configuration")').click();

    // Should pass - no off-by-one error
    await expect(page.locator('.result-success')).toBeVisible({ timeout: 5000 });
  });

  test('Validator accepts different controller strategies', async ({ page }) => {
    await page.goto('/config-validator');
    await page.waitForLoadState('domcontentloaded');

    // Test NEAREST_REQUEST_ROUTING strategy
    const config1 = {
      ...VALID_CONFIGS.basicOffice,
      controllerStrategy: 'NEAREST_REQUEST_ROUTING',
      idleParkingMode: 'PARK_TO_HOME_FLOOR'
    };

    await page.locator('.config-editor').fill(JSON.stringify(config1, null, 2));
    await page.locator('button:has-text("Validate Configuration")').click();

    await expect(page.locator('.result-success')).toBeVisible({ timeout: 5000 });

    // Test DIRECTIONAL_SCAN strategy
    const config2 = {
      ...VALID_CONFIGS.basicOffice,
      controllerStrategy: 'DIRECTIONAL_SCAN',
      idleParkingMode: 'STAY_AT_CURRENT_FLOOR'
    };

    await page.locator('.config-editor').clear();
    await page.locator('.config-editor').fill(JSON.stringify(config2, null, 2));
    await page.locator('button:has-text("Validate Configuration")').click();

    await expect(page.locator('.result-success')).toBeVisible({ timeout: 5000 });
  });
});
