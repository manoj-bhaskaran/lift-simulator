import { test, expect } from '@playwright/test';

/**
 * Smoke tests for Lift Simulator Admin UI
 * These tests verify that the application loads and critical pages are accessible
 */

test.describe('Application Smoke Tests', () => {
  test('application loads successfully and displays dashboard', async ({ page }) => {
    // Navigate to the home page
    await page.goto('/');

    // Wait for the DOM to be loaded (don't wait for network idle as API may not be available)
    await page.waitForLoadState('domcontentloaded');

    // Verify the page title contains "Lift Simulator"
    await expect(page).toHaveTitle(/Lift Simulator/);

    // Verify the dashboard heading is visible
    await expect(page.locator('h2:has-text("Dashboard")')).toBeVisible();

    // Verify key UI elements are present (should be visible even if API fails)
    await expect(page.locator('text=Overview')).toBeVisible();
    await expect(page.locator('text=Quick Actions')).toBeVisible();
  });

  test('navigation to Lift Systems page works', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');

    // Click on the Lift Systems navigation link
    const liftSystemsLink = page.locator('a[href="/systems"]');
    await liftSystemsLink.first().click();

    // Wait for navigation
    await page.waitForURL('**/systems');

    // Verify we're on the lift systems page
    await expect(page.locator('h1, h2').filter({ hasText: /Lift Systems/i })).toBeVisible();
  });

  test('health check page is accessible', async ({ page }) => {
    await page.goto('/health');

    // Wait for the page to load
    await page.waitForLoadState('domcontentloaded');

    // Verify health check page heading is visible
    await expect(page.locator('h2:has-text("Health Check")')).toBeVisible();
  });

  test('configuration validator page is accessible', async ({ page }) => {
    await page.goto('/config-validator');

    // Wait for the page to load
    await page.waitForLoadState('domcontentloaded');

    // Verify configuration validator page heading is visible
    await expect(page.locator('h2:has-text("Configuration Validator")')).toBeVisible();
  });

  test('footer displays version information', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');

    // Verify footer is present and displays version
    const footer = page.locator('footer');
    await expect(footer).toBeVisible();
    await expect(footer.locator('text=/Version|v\d+\.\d+\.\d+/i')).toBeVisible();
  });
});
