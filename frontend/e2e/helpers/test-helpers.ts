import { Page, expect } from '@playwright/test';

/**
 * Test data fixtures and helper functions for Playwright tests
 */

/**
 * Valid configuration scenarios
 * Note: These are inline copies of the scenario files to avoid file system dependencies
 */
export const VALID_CONFIGS = {
  basicOffice: {
    floors: 10,
    lifts: 2,
    travelTicksPerFloor: 1,
    doorTransitionTicks: 2,
    doorDwellTicks: 3,
    doorReopenWindowTicks: 2,
    homeFloor: 0,
    idleTimeoutTicks: 5,
    controllerStrategy: 'NEAREST_REQUEST_ROUTING',
    idleParkingMode: 'PARK_TO_HOME_FLOOR'
  },
  highRiseResidential: {
    floors: 30,
    lifts: 4,
    travelTicksPerFloor: 2,
    doorTransitionTicks: 3,
    doorDwellTicks: 5,
    doorReopenWindowTicks: 2,
    homeFloor: 0,
    idleTimeoutTicks: 10,
    controllerStrategy: 'DIRECTIONAL_SCAN',
    idleParkingMode: 'PARK_TO_HOME_FLOOR'
  },
  minimal: {
    floors: 2,
    lifts: 1,
    travelTicksPerFloor: 1,
    doorTransitionTicks: 1,
    doorDwellTicks: 1,
    doorReopenWindowTicks: 1,
    homeFloor: 0,
    idleTimeoutTicks: 1,
    controllerStrategy: 'NEAREST_REQUEST_ROUTING',
    idleParkingMode: 'PARK_TO_HOME_FLOOR'
  },
  large: {
    floors: 100,
    lifts: 8,
    travelTicksPerFloor: 3,
    doorTransitionTicks: 4,
    doorDwellTicks: 6,
    doorReopenWindowTicks: 3,
    homeFloor: 0,
    idleTimeoutTicks: 15,
    controllerStrategy: 'DIRECTIONAL_SCAN',
    idleParkingMode: 'STAY_AT_CURRENT_FLOOR'
  }
};

/**
 * Invalid configuration scenarios
 */
export const INVALID_CONFIGS = {
  invalidExample: {
    floors: 1,
    lifts: 0,
    travelTicksPerFloor: -1,
    doorTransitionTicks: 2,
    doorDwellTicks: 3,
    doorReopenWindowTicks: 5,
    homeFloor: 20,
    idleTimeoutTicks: -5,
    controllerStrategy: 'INVALID_STRATEGY',
    idleParkingMode: 'PARK_TO_HOME_FLOOR'
  },
  tooFewFloors: {
    floors: 1,
    lifts: 1,
    travelTicksPerFloor: 1,
    doorTransitionTicks: 1,
    doorDwellTicks: 1,
    doorReopenWindowTicks: 1,
    homeFloor: 0,
    idleTimeoutTicks: 1,
    controllerStrategy: 'NEAREST_REQUEST_ROUTING',
    idleParkingMode: 'PARK_TO_HOME_FLOOR'
  },
  homeFloorOutOfBounds: {
    floors: 10,
    lifts: 2,
    travelTicksPerFloor: 1,
    doorTransitionTicks: 2,
    doorDwellTicks: 3,
    doorReopenWindowTicks: 2,
    homeFloor: 10,
    idleTimeoutTicks: 5,
    controllerStrategy: 'NEAREST_REQUEST_ROUTING',
    idleParkingMode: 'PARK_TO_HOME_FLOOR'
  }
};

/**
 * Test data generators
 */
export function generateSystemKey(prefix: string = 'test-system'): string {
  return `${prefix}-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}

export function generateSystemData(overrides: Partial<{ systemKey: string; displayName: string; description: string }> = {}) {
  const timestamp = Date.now();
  return {
    systemKey: overrides.systemKey || generateSystemKey(),
    displayName: overrides.displayName || `Test Building ${timestamp}`,
    description: overrides.description || `Automated test system created at ${new Date().toISOString()}`
  };
}

/**
 * Helper function to create a lift system via UI
 */
export async function createLiftSystem(
  page: Page,
  systemData: { systemKey: string; displayName: string; description: string }
): Promise<void> {
  // Navigate to Lift Systems page
  await page.goto('/systems');
  await page.waitForLoadState('domcontentloaded');

  // Click Create New System button
  await page.locator('button:has-text("Create New System")').click();

  // Wait for modal to appear
  await page.locator('.modal-content').waitFor({ state: 'visible' });

  // Fill in the form
  await page.locator('#systemKey').fill(systemData.systemKey);
  await page.locator('#displayName').fill(systemData.displayName);
  await page.locator('#description').fill(systemData.description);

  // Submit the form
  await page.locator('.modal-content button:has-text("Create")').click();

  // Wait for modal to close
  await page.locator('.modal-content').waitFor({ state: 'hidden' });

  // Wait for success message or system card to appear
  await page.waitForTimeout(500); // Small delay for UI update
}

/**
 * Helper function to delete a lift system via UI
 */
export async function deleteLiftSystem(page: Page, systemKey: string): Promise<void> {
  await page.goto('/systems');
  await page.waitForLoadState('domcontentloaded');

  // Find the system card by system key and click delete
  const systemCard = page.locator('.system-card').filter({ hasText: systemKey });
  await systemCard.locator('button:has-text("Delete")').click();

  // Confirm deletion in modal
  await page.locator('.modal-content button:has-text("Delete")').click();

  // Wait for modal to close
  await page.locator('.modal-content').waitFor({ state: 'hidden' });
}

/**
 * Helper function to navigate to system detail page
 */
export async function navigateToSystemDetail(page: Page, systemKey: string): Promise<void> {
  await page.goto('/systems');
  await page.waitForLoadState('domcontentloaded');

  // Click on the system card to view details
  const systemCard = page.locator('.system-card').filter({ hasText: systemKey });
  await systemCard.locator('button:has-text("View Details")').click();

  // Wait for navigation
  await page.waitForURL(/\/systems\/[^/]+$/);
}

/**
 * Helper function to create a configuration version
 */
export async function createConfigVersion(
  page: Page,
  systemKey: string,
  config: object
): Promise<void> {
  // Navigate to system detail page
  await navigateToSystemDetail(page, systemKey);

  // Click Create New Version button
  await page.locator('button:has-text("Create New Version")').click();

  // Wait for modal
  await page.locator('.modal-content').waitFor({ state: 'visible' });

  // Fill in configuration JSON
  await page.locator('#config').fill(JSON.stringify(config, null, 2));

  // Submit
  await page.locator('.modal-content button:has-text("Create")').click();

  // Wait for modal to close
  await page.locator('.modal-content').waitFor({ state: 'hidden' });
}

/**
 * Helper function to get the status of a specific version
 */
export async function getVersionStatus(page: Page, versionNumber: number): Promise<string> {
  const versionCard = page.locator('.version-card').filter({ hasText: `Version ${versionNumber}` });
  const statusBadge = versionCard.locator('.status-badge');
  return await statusBadge.textContent() || '';
}

/**
 * Helper function to publish a version
 */
export async function publishVersion(page: Page, versionNumber: number): Promise<void> {
  const versionCard = page.locator('.version-card').filter({ hasText: `Version ${versionNumber}` });
  await versionCard.locator('button:has-text("Publish")').click();

  // Confirm in modal
  await page.locator('.modal-content button:has-text("Publish")').click();

  // Wait for modal to close
  await page.locator('.modal-content').waitFor({ state: 'hidden' });
}

/**
 * Helper function to wait for backend to be ready
 * Returns true if backend is ready, false if timeout
 */
export async function waitForBackend(page: Page, timeoutMs: number = 10000): Promise<boolean> {
  const startTime = Date.now();

  while (Date.now() - startTime < timeoutMs) {
    try {
      const response = await page.request.get('http://localhost:8080/api/health');
      if (response.ok()) {
        return true;
      }
    } catch (error) {
      // Backend not ready yet
    }
    await page.waitForTimeout(500);
  }

  return false;
}

/**
 * Helper function to check if backend is available
 * Used by tests that should skip if backend is down
 */
export async function isBackendAvailable(page: Page): Promise<boolean> {
  try {
    const response = await page.request.get('http://localhost:8080/api/health');
    return response.ok();
  } catch (error) {
    return false;
  }
}

/**
 * Cleanup helper - delete a system if it exists
 */
export async function cleanupSystemIfExists(page: Page, systemKey: string): Promise<void> {
  try {
    await page.goto('/systems');
    await page.waitForLoadState('domcontentloaded');

    const systemCard = page.locator('.system-card').filter({ hasText: systemKey });
    const count = await systemCard.count();

    if (count > 0) {
      await deleteLiftSystem(page, systemKey);
    }
  } catch (error) {
    // System doesn't exist or couldn't be deleted, which is fine for cleanup
    console.log(`Cleanup: Could not delete system ${systemKey}:`, error);
  }
}

/**
 * Get dashboard metrics
 */
export async function getDashboardMetrics(page: Page): Promise<{ systems: number; versions: number }> {
  await page.goto('/');
  await page.waitForLoadState('domcontentloaded');

  // Find the stat values
  const statsSection = page.locator('.card').filter({ hasText: 'Overview' });
  const systemsStat = statsSection.locator('.stat-item').filter({ hasText: /lift system/i });
  const versionsStat = statsSection.locator('.stat-item').filter({ hasText: /version/i });

  const systemsText = await systemsStat.locator('.stat-value').textContent() || '0';
  const versionsText = await versionsStat.locator('.stat-value').textContent() || '0';

  return {
    systems: parseInt(systemsText, 10),
    versions: parseInt(versionsText, 10)
  };
}

/**
 * Count lift systems in the systems list
 */
export async function countLiftSystems(page: Page): Promise<number> {
  await page.goto('/systems');
  await page.waitForLoadState('domcontentloaded');

  // Wait a bit for data to load
  await page.waitForTimeout(1000);

  const systemCards = page.locator('.system-card');
  return await systemCards.count();
}

/**
 * Count versions for a specific system
 */
export async function countVersionsForSystem(page: Page, systemKey: string): Promise<number> {
  await navigateToSystemDetail(page, systemKey);

  // Wait for versions to load
  await page.waitForTimeout(1000);

  const versionCards = page.locator('.version-card');
  return await versionCards.count();
}
