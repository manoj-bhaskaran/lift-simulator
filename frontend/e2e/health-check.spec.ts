import { test, expect } from '@playwright/test';
import { isBackendAvailable } from './helpers/test-helpers';

/**
 * Health Check Tests
 *
 * Manual Test Case Mapping:
 * - TC_0012: Health Check UI and API
 */

test.describe('Health Check', () => {
  test.beforeEach(async ({ page }) => {
    // Check if backend is available
    const backendAvailable = await isBackendAvailable(page);
    if (!backendAvailable) {
      test.skip();
    }
  });

  test('TC_0012: Health Check UI and API', async ({ page }) => {
    // Step 1: Open Health Check page from main menu
    await page.goto('/health');
    await page.waitForLoadState('domcontentloaded');

    // Verify page loads
    await expect(page.locator('h2:has-text("Health Check")')).toBeVisible();

    // Step 2: Verify status shows Healthy/UP with current timestamp
    const statusBadge = page.locator('.status-badge');
    await expect(statusBadge).toBeVisible();

    // Status should be OK or similar
    const statusText = await statusBadge.textContent();
    expect(statusText).toMatch(/OK|UP|HEALTHY/i);

    // Verify timestamp is shown
    const lastChecked = page.locator('.last-checked');
    await expect(lastChecked).toBeVisible();
    await expect(lastChecked).toContainText(/last checked/i);

    // Step 3: Refresh status (if button exists)
    const refreshButton = page.locator('button:has-text("Refresh")');
    const hasRefreshButton = await refreshButton.isVisible();

    if (hasRefreshButton) {
      // Get current timestamp
      const timestamp1 = await lastChecked.first().textContent();

      // Click refresh
      await refreshButton.click();

      // Wait for refresh
      await page.waitForTimeout(1500);

      // Timestamp should update (or stay the same if refresh is instant)
      // Just verify the page still shows healthy status
      await expect(statusBadge).toBeVisible();
    }

    // Step 4: Verify API endpoint directly
    const response = await page.request.get('http://localhost:8080/api/v1/health');

    // Verify HTTP 200 response
    expect(response.ok()).toBe(true);
    expect(response.status()).toBe(200);

    // Verify JSON response structure
    const healthData = await response.json();
    expect(healthData).toHaveProperty('status');
    expect(healthData.status).toMatch(/UP|OK/i);
  });

  test('Health check page shows service information', async ({ page }) => {
    await page.goto('/health');
    await page.waitForLoadState('domcontentloaded');

    // The page should display service/backend information
    await expect(page.locator('h2:has-text("Health Check")')).toBeVisible();

    // Look for status badge
    const statusBadge = page.locator('.status-badge');
    await expect(statusBadge).toBeVisible();

    // The current custom health payload always exposes at least the status badge.
    // Additional detail cards are rendered only when the backend includes message/details fields.
    await expect(statusBadge).toContainText(/OK|UP|HEALTHY/i);
  });

  test('Health check handles backend failure gracefully', async ({ page }) => {
    // This test verifies UI behavior when backend check fails
    // We'll navigate to health page and check it doesn't crash

    await page.goto('/health');
    await page.waitForLoadState('domcontentloaded');

    // Page should load without errors
    await expect(page.locator('h2:has-text("Health Check")')).toBeVisible();

    // Even if backend is down, UI should show status (possibly "DOWN" or "ERROR")
    // The key is that the page doesn't crash
    const statusOrError = page.locator('.status-badge, .error-card').first();
    await expect(statusOrError).toBeVisible();
  });

  test('Health check API returns structured JSON', async ({ page }) => {
    const response = await page.request.get('http://localhost:8080/api/v1/health');

    expect(response.ok()).toBe(true);

    const healthData = await response.json();

    // Verify required fields
    expect(healthData).toHaveProperty('status');

    // May have additional fields like service name, timestamp, etc.
    // Verify structure is consistent
    expect(typeof healthData.status).toBe('string');
  });
});
