import { test, expect } from '@playwright/test';

/**
 * Smoke tests for Lift Simulator Admin UI
 * These tests verify that the application loads and critical pages are accessible
 */

test.describe('Application Smoke Tests', () => {
  test('application loads successfully and displays dashboard', async ({ page }) => {
    // Navigate to the home page
    await page.goto('/');

    // Wait for the page to be fully loaded
    await page.waitForLoadState('networkidle');

    // Verify the page title contains "Lift Simulator"
    await expect(page).toHaveTitle(/Lift Simulator/);

    // Verify the page has loaded by checking for common elements
    // The dashboard should be visible
    await expect(page.locator('text=Dashboard')).toBeVisible();
  });

  test('navigation to Lift Systems page works', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // Click on the Lift Systems navigation link
    const liftSystemsLink = page.locator('a[href="/lift-systems"], text=Lift Systems');
    await liftSystemsLink.first().click();

    // Wait for navigation
    await page.waitForURL('**/lift-systems');

    // Verify we're on the lift systems page
    await expect(page.locator('h1, h2').filter({ hasText: /Lift Systems/i })).toBeVisible();
  });

  test('health check page is accessible', async ({ page }) => {
    await page.goto('/health');

    // Wait for the page to load
    await page.waitForLoadState('networkidle');

    // Verify health check page elements are visible
    await expect(page.locator('text=/Health|Status/i')).toBeVisible();
  });

  test('configuration validator page is accessible', async ({ page }) => {
    await page.goto('/config-validator');

    // Wait for the page to load
    await page.waitForLoadState('networkidle');

    // Verify configuration validator page elements are visible
    await expect(page.locator('text=/Configuration|Validator/i')).toBeVisible();
  });

  test('footer displays version information', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('networkidle');

    // Verify footer is present and displays version
    const footer = page.locator('footer');
    await expect(footer).toBeVisible();
    await expect(footer.locator('text=/Version|v\d+\.\d+\.\d+/i')).toBeVisible();
  });
});
