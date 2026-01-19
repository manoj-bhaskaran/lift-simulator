# ADR-0014: Playwright for E2E UI Automation Testing

**Date**: 2026-01-19

**Status**: Accepted

## Context

The Lift Simulator admin UI (ADR-0011) provides a React-based interface for managing lift system configurations. To ensure reliability and prevent regressions, we need automated end-to-end (E2E) testing that validates user workflows against the running application. The tests must:

1. Verify critical user journeys work correctly (create systems, publish versions, validate configurations)
2. Run reliably in CI/CD pipelines with minimal flakiness
3. Provide fast feedback to developers during local development
4. Support debugging capabilities for test failures
5. Work seamlessly with the existing Vite + React stack
6. Scale to support multiple browsers if needed in the future

We evaluated several E2E testing frameworks for the frontend:

### Option 1: Cypress

**Pros**:
- Mature testing framework with large community
- Excellent developer experience with time-travel debugging
- Good documentation and learning resources
- Built-in test runner UI

**Cons**:
- Limited browser support (no WebKit/Safari support on some platforms)
- Runs tests inside the browser, which can cause limitations
- Some architectural constraints around same-origin and iframe handling
- Component testing support is less mature than E2E testing

### Option 2: Selenium WebDriver

**Pros**:
- Industry standard with decades of maturity
- Supports all major browsers
- Large ecosystem of tools and integrations

**Cons**:
- Verbose API requiring more boilerplate code
- Slower test execution compared to modern alternatives
- More prone to flakiness without careful configuration
- Steeper learning curve
- Requires separate browser driver management

### Option 3: Playwright

**Pros**:
- Modern architecture built by Microsoft (former Chrome DevTools team)
- Native support for Chromium, Firefox, and WebKit browsers
- Fast and reliable execution with auto-waiting mechanisms
- Excellent TypeScript support out of the box
- Built-in test runner with parallel execution
- Powerful debugging capabilities (trace viewer, UI mode, debug mode)
- Automatic web server management for dev workflow
- Headless and headed modes for different scenarios
- Active development and strong community momentum
- Minimal configuration required
- Excellent CI/CD integration with automatic retries
- Built-in screenshot, video, and trace capture on failures

**Cons**:
- Newer framework (less mature than Cypress/Selenium)
- Smaller community than Cypress (though growing rapidly)
- Browser binaries need to be installed separately (~300MB per browser)

### Option 4: Testing Library + Happy DOM/JSDOM

**Pros**:
- Lightweight and fast
- Works well for component-level testing
- Good integration with Vitest

**Cons**:
- Not a true E2E solution (runs in Node.js, not real browsers)
- Cannot test actual browser rendering and behavior
- Limited to DOM manipulation testing
- Cannot validate visual aspects or cross-browser compatibility

## Decision

We will adopt **Playwright** as the E2E testing framework for the Lift Simulator admin UI.

### Implementation Details

1. **Framework**: @playwright/test (latest version, currently 1.57.0)
2. **Language**: TypeScript for type safety and better IDE support
3. **Test Location**: `frontend/e2e/` directory
4. **Configuration**: `playwright.config.ts` in the frontend root

### Configuration Highlights

```typescript
{
  testDir: './e2e',
  baseURL: 'http://localhost:3000',
  fullyParallel: true,
  retries: process.env.CI ? 2 : 0,  // Retry on CI for reliability
  workers: process.env.CI ? 1 : undefined,  // Sequential on CI, parallel locally

  use: {
    actionTimeout: 10000,      // 10s for individual actions
    navigationTimeout: 30000,  // 30s for page navigation
    trace: 'on-first-retry',   // Capture trace on retry
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },

  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3000',
    reuseExistingServer: !process.env.CI,
    timeout: 120000,
  },

  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    // Firefox and WebKit can be enabled when needed
  ],
}
```

### Test Organization

**Naming Convention**:
- E2E tests: `e2e/*.spec.ts`
- Test suites grouped by feature or user journey
- Descriptive test names following "should [action] when [condition]" pattern

**Initial Test Coverage**:
- Smoke tests (`e2e/smoke.spec.ts`):
  - Application loads and displays dashboard
  - Navigation between pages works
  - Critical pages are accessible (Health Check, Config Validator)
  - Version information displays in footer

**Future Test Expansion**:
- Lift system creation and management workflows
- Configuration version lifecycle (create, edit, publish, archive)
- Configuration validation with error handling
- Search and filtering functionality
- Error state handling and user feedback

### NPM Scripts

```json
{
  "test": "playwright test",
  "test:headed": "playwright test --headed",
  "test:ui": "playwright test --ui",
  "test:debug": "playwright test --debug",
  "test:report": "playwright show-report"
}
```

### CI Integration

Playwright tests will run in the CI pipeline after frontend build:

1. Install dependencies: `npm ci`
2. Install Playwright browsers: `npx playwright install --with-deps chromium`
3. Run tests: `npm test`
4. Upload test artifacts (reports, traces) on failure

The `webServer` configuration automatically starts the dev server before tests, eliminating the need for separate server management in CI.

### Developer Workflow

**Local Development**:
1. One-time setup: `npx playwright install` (installs browser binaries)
2. Run tests: `npm test` (automatically starts dev server)
3. Debug tests: `npm run test:ui` (interactive mode)
4. View reports: `npm run test:report` (after test run)

**Writing New Tests**:
```typescript
import { test, expect } from '@playwright/test';

test.describe('Feature Name', () => {
  test('should perform action', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('text=Expected Text')).toBeVisible();
  });
});
```

## Rationale

Playwright was chosen over alternatives because:

1. **Modern Architecture**: Built with modern web app testing in mind, not retrofitted
2. **TypeScript First**: Native TypeScript support matches our frontend's JSDoc + TS declarations approach
3. **Developer Experience**: Excellent debugging tools (trace viewer, UI mode, time-travel debugging)
4. **CI/CD Ready**: Built-in retries, parallel execution, and artifact capture make CI integration seamless
5. **Auto-Waiting**: Reduces flakiness by automatically waiting for elements to be ready
6. **Cross-Browser**: Native support for Chromium, Firefox, and WebKit (Safari engine)
7. **Performance**: Fast execution with efficient browser automation protocol
8. **Integration**: Excellent integration with Vite via `webServer` configuration
9. **Future-Proof**: Active development by Microsoft with strong community momentum

### Why Not Cypress?

While Cypress has excellent developer experience, Playwright provides:
- Better cross-browser support (native WebKit support)
- More flexible architecture (not limited to running inside the browser)
- Better TypeScript integration out of the box
- More powerful debugging with trace viewer
- Better suited for complex modern web apps

### Why Not Selenium?

Selenium is mature but Playwright offers:
- Significantly faster execution
- More reliable tests with auto-waiting
- Better developer experience with less boilerplate
- Modern API design
- Built-in test runner (no need for separate tools)

## Consequences

### Positive

1. **Quality Assurance**: Automated E2E tests catch regressions before production
2. **Confidence**: Developers can refactor with confidence knowing tests validate core workflows
3. **Documentation**: Tests serve as living documentation of user workflows
4. **CI Integration**: Reliable test execution in CI pipeline prevents broken deployments
5. **Multi-Browser**: Can expand to test on Firefox and Safari if needed
6. **Debugging**: Excellent debugging tools reduce time spent investigating failures

### Negative

1. **Browser Binaries**: Requires ~300MB disk space per browser (mitigated by starting with Chromium only)
2. **Learning Curve**: Team needs to learn Playwright API (mitigated by excellent docs)
3. **Test Maintenance**: E2E tests require updates when UI changes (inherent to any E2E framework)
4. **CI Time**: E2E tests add execution time to CI pipeline (mitigated by parallel execution and caching)

### Neutral

1. **Framework Lock-in**: Committing to Playwright API (acceptable given maturity and stability)
2. **Test Coverage**: Starting with smoke tests, will expand coverage iteratively
3. **Unit Tests**: Playwright is for E2E only; unit tests would use Vitest + Testing Library (future work)

## Alternatives Considered

See "Context" section above for detailed evaluation of:
- Cypress
- Selenium WebDriver
- Testing Library + Happy DOM

## Follow-up Actions

1. ✅ Install @playwright/test package
2. ✅ Create `playwright.config.ts` configuration
3. ✅ Create `e2e/` directory structure
4. ✅ Write initial smoke test suite
5. ✅ Add npm scripts for test execution
6. ✅ Update frontend README with testing documentation
7. ✅ Update `.gitignore` to exclude Playwright artifacts
8. 🔲 Update CI/CD pipeline to run Playwright tests (separate task)
9. 🔲 Expand test coverage for critical user journeys (ongoing)
10. 🔲 Add visual regression testing with Playwright screenshots (future consideration)

## References

- [Playwright Documentation](https://playwright.dev/)
- [Playwright Best Practices](https://playwright.dev/docs/best-practices)
- [Playwright CI Configuration](https://playwright.dev/docs/ci)
- [ADR-0011: React Admin UI Scaffold](./0011-react-admin-ui-scaffold.md)
