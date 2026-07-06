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
    minFloor: 0,
    maxFloor: 9,
    lifts: 1,
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
    minFloor: 0,
    maxFloor: 29,
    lifts: 1,
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
    minFloor: 0,
    maxFloor: 1,
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
    minFloor: 0,
    maxFloor: 99,
    lifts: 1,
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
    minFloor: 0,
    maxFloor: 0,
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
    minFloor: 0,
    maxFloor: 0,
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
    minFloor: 0,
    maxFloor: 9,
    lifts: 1,
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

  // Submit the form and wait for the backend create response
  const [createResponse] = await Promise.all([
    page.waitForResponse((response) =>
      response.url().includes('/api/v1/lift-systems') &&
      response.request().method() === 'POST'
    ),
    page.locator('.modal-content button:has-text("Create")').click(),
  ]);
  expect(createResponse.ok()).toBeTruthy();

  const createdSystem = await createResponse.json();

  // The app navigates to the new system detail page after a successful create.
  await page.locator('.modal-content').waitFor({ state: 'hidden' });
  await page.waitForURL(new RegExp(`/systems/${createdSystem.id}$`), { timeout: 5000 });
  // The system key is shown in the detail header; scope to it so the assertion
  // does not collide with the copy rendered in the System Information grid.
  await expect(page.locator('.detail-header .system-key')).toHaveText(systemData.systemKey, { timeout: 5000 });
}

/**
 * Helper function to delete a lift system via UI.
 * Deletion is only available from the system detail page, so this opens the
 * detail view first, then confirms the deletion in the modal.
 */
export async function deleteLiftSystem(page: Page, systemKey: string): Promise<void> {
  await page.goto('/systems');
  await page.waitForLoadState('domcontentloaded');

  // Open the detail page for the system (the list cards have no delete action).
  const systemCard = page.locator('.system-card').filter({ hasText: systemKey });
  await systemCard.locator('button:has-text("View Details")').click();
  await page.waitForURL(/\/systems\/[^/]+$/);

  // Trigger deletion and confirm in the modal.
  await page.locator('button:has-text("Delete System")').click();
  const confirmModal = page.locator('.modal-content').filter({ hasText: /delete/i });
  await confirmModal.waitFor({ state: 'visible' });
  await confirmModal.locator('button:has-text("Delete")').click();

  // The app navigates back to the systems list after a successful delete.
  await page.waitForURL('**/systems');
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
 * Helper function to switch the create-version form to the Advanced (JSON)
 * editor. The guided form is the default experience, so JSON-based flows must
 * opt into the raw editor first.
 */
export async function switchToAdvancedJsonMode(page: Page): Promise<void> {
  await page.locator('.editor-mode-toggle button:has-text("Advanced")').click();
  await expect(page.locator('#config')).toBeVisible();
}

/**
 * Helper function to open the inline create-version form.
 *
 * @param mode Which editor to surface once the form is open. Defaults to the
 *   raw JSON editor ('json') so existing JSON-based flows keep working; pass
 *   'guided' to remain on the default guided form.
 */
export async function openCreateVersionForm(page: Page, mode: 'guided' | 'json' = 'json'): Promise<void> {
  await page.locator('button:has-text("Create New Version")').click();
  await expect(page.locator('.create-version-form')).toBeVisible();
  if (mode === 'json') {
    await switchToAdvancedJsonMode(page);
  }
}

/**
 * Helper function to fill and validate the inline create-version form.
 */
export async function fillAndValidateVersionConfig(
  page: Page,
  config: object
): Promise<void> {
  await page.locator('#config').fill(JSON.stringify(config, null, 2));
  await page.locator('.create-version-form button:has-text("Validate")').click();
  await expect(page.locator('.validation-success-banner')).toBeVisible({ timeout: 5000 });
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

  await openCreateVersionForm(page);
  await fillAndValidateVersionConfig(page, config);

  // Submit
  await page.locator('.create-version-form button:has-text("Create Version")').click();

  // Wait for the inline form to close and the versions list to refresh
  await expect(page.locator('.create-version-form')).toBeHidden({ timeout: 5000 });
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
      const response = await page.request.get('http://localhost:8080/api/v1/health', { timeout: 1000 });
      if (response.ok()) {
        return true;
      }
    } catch (error) {
      // Backend not ready yet
    }
    // Use exponential backoff with small increments for polling
    await new Promise(resolve => setTimeout(resolve, 100));
  }

  return false;
}

/**
 * Helper function to check if backend is available
 * Used by tests that should skip if backend is down
 */
export async function isBackendAvailable(page: Page): Promise<boolean> {
  try {
    const response = await page.request.get('http://localhost:8080/api/v1/health');
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

    // Wait for the list to finish loading before checking for the card; the
    // systems are fetched asynchronously, so a raw count() immediately after
    // navigation can race the "Loading..." state and miss an existing system.
    await page.locator('.systems-grid, .empty-state').first().waitFor({ state: 'visible', timeout: 5000 });

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
 * Deletes any scenarios attached to a system's versions via the API.
 *
 * A lift system cannot be deleted while scenarios reference its versions (the
 * backend returns HTTP 409), so tests that create scenarios must remove them
 * before the system can be cleaned up.
 */
export async function deleteScenariosForSystem(page: Page, systemKey: string): Promise<void> {
  try {
    const response = await page.request.get('http://localhost:8080/api/v1/scenarios');
    if (!response.ok()) {
      return;
    }
    const scenarios = await response.json();
    for (const scenario of scenarios) {
      if (scenario?.versionInfo?.systemKey === systemKey) {
        await page.request.delete(`http://localhost:8080/api/v1/scenarios/${scenario.id}`);
      }
    }
  } catch (error) {
    console.log(`Cleanup: Could not delete scenarios for system ${systemKey}:`, error);
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

  // Wait for the systems grid or empty state to be visible (data loaded)
  await page.locator('.systems-grid, .empty-state').first().waitFor({ state: 'visible', timeout: 5000 });

  const systemCards = page.locator('.system-card');
  return await systemCards.count();
}

/**
 * Count versions for a specific system
 */
export async function countVersionsForSystem(page: Page, systemKey: string): Promise<number> {
  await navigateToSystemDetail(page, systemKey);

  // Wait for versions to be visible (they appear in the detail page)
  await page.locator('#versions h3, .no-versions').first().waitFor({ state: 'visible', timeout: 5000 });

  const versionCards = page.locator('.version-card');
  return await versionCards.count();
}
