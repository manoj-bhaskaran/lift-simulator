# ADR-0015: Test Management Platform Evaluation (TestQuality vs Playwright-only)

**Date**: 2026-01-19

**Status**: Accepted

## Context

Following the adoption of Playwright for E2E testing (ADR-0014) and successful integration with GitHub Actions CI, the question arose whether to adopt a dedicated test management platform (TestQuality) or continue with the simpler Playwright-only approach using GitHub Actions artifacts for reporting.

The decision impacts:
1. Test execution orchestration and triggering mechanisms
2. Test result reporting and historical analysis
3. CI/CD pipeline complexity and maintenance
4. Long-term scalability as the test suite grows
5. Cost (subscription vs free GitHub features)

### Project Context
- **Current Test Suite**: 6 Playwright E2E spec files (~10 test cases)
- **Team Size**: Small (solo/small team)
- **CI Platform**: GitHub Actions
- **Current Reporting**: Playwright HTML reports uploaded as GitHub artifacts (30-day retention)

### TestQuality Overview

TestQuality is a test management platform offering:
- ✅ REST API for test result uploads
- ✅ CLI tool (`testquality`) for CI integration
- ✅ Playwright-specific reporter (`@testquality/playwright-reporter`)
- ✅ Unified reporting across multiple test frameworks
- ✅ Historical trend analysis and metrics
- ✅ Live GitHub integration (PR checks, automatic test runs)
- ✅ Manual + automated test case management
- ✅ AI-powered test generation and analysis
- ✅ 30+ CI/CD and automation tool integrations

**Key Limitation**: TestQuality is a **test management and reporting platform**, not a test orchestration platform. It cannot trigger test execution—tests still run in CI/CD as usual, and results are uploaded to TestQuality for centralized reporting.

### Evaluation Criteria

We evaluated two approaches against four key criteria:

1. **Complexity**: Setup, configuration, dependencies, debugging
2. **Maintenance Overhead**: Token management, version updates, platform dependencies
3. **Reporting Clarity**: Test results view, failure analysis, historical trends
4. **Future Scalability**: Multiple frameworks, team growth, enterprise features

## Decision

We will **continue with Approach A: Playwright-only execution in GitHub Actions** with native HTML reporting and GitHub artifacts storage.

### Approach A: Playwright-only GitHub Actions (Chosen)

```
GitHub Event (push/PR)
    ↓
GitHub Actions Workflow
    ↓
Playwright Test Execution
    ↓
HTML Report Generation
    ↓
Upload to GitHub Artifacts
```

**Implementation**: Already complete (see `.github/workflows/ci.yml` lines 69-89)

### Approach B: TestQuality-Integrated (Deferred)

```
GitHub Event (push/PR)
    ↓
GitHub Actions Workflow
    ↓
Playwright Test Execution (JUnit XML)
    ↓
TestQuality CLI Upload
    ↓
TestQuality Platform (unified reporting)
```

**Implementation**: Not pursued at this time (see evaluation document for details)

## Rationale

### Quantitative Evaluation

| Criteria | Weight | A Score | B Score | A Weighted | B Weighted |
|----------|--------|---------|---------|-----------|-----------|
| Complexity | 30% | 10/10 | 5/10 | 3.0 | 1.5 |
| Maintenance | 25% | 10/10 | 5/10 | 2.5 | 1.25 |
| Reporting | 25% | 6/10 | 9/10 | 1.5 | 2.25 |
| Scalability | 20% | 5/10 | 9/10 | 1.0 | 1.8 |
| **TOTAL** | **100%** | - | - | **8.0** | **6.8** |

**Winner**: Approach A (Playwright-only)

### Key Decision Factors

1. **Current Needs Satisfied**: Playwright HTML reports provide excellent failure debugging with screenshots, videos, and traces. No critical gaps identified.

2. **Simplicity First**: Current setup is already working with minimal dependencies and zero additional costs. Adding TestQuality would introduce:
   - Personal Access Token management
   - Additional CI workflow steps
   - External platform dependency
   - Subscription costs
   - Additional failure points

3. **Project Scale**: With 6 test files and ~10 test cases, the benefits of TestQuality (historical trends, cross-framework aggregation, manual test management) don't justify the added complexity.

4. **Team Size**: Small team without dedicated QA specialists managing manual test cases. TestQuality's manual test management features would go unused.

5. **Framework Diversity**: Currently only using Playwright for UI tests. TestQuality's cross-framework aggregation provides no value yet.

6. **Cost Efficiency**: GitHub Actions and artifacts are included in the repository subscription. TestQuality requires additional subscription costs.

### When to Reconsider TestQuality

TestQuality becomes valuable when **2+ of these conditions** are met:

- [ ] Test suite exceeds 50+ automated test cases
- [ ] Multiple test frameworks in use (e.g., Playwright + REST API tests + performance tests)
- [ ] Dedicated QA team managing manual test cases
- [ ] Need for historical trend analysis and metrics (beyond basic CI history)
- [ ] Regulatory/compliance requirements for test traceability
- [ ] Cross-team test result visibility needed
- [ ] Manual + automated test coordination required

## Consequences

### Positive

1. **Simplicity**: Minimal CI pipeline complexity with fewer dependencies
2. **Cost Effective**: No additional subscription costs
3. **Low Maintenance**: No tokens to rotate, no external platform to monitor
4. **Self-Contained**: All testing infrastructure within GitHub ecosystem
5. **Excellent Debugging**: Playwright HTML reports provide comprehensive failure analysis
6. **Proven Reliability**: Current setup is working well with no identified issues

### Negative

1. **Limited History**: GitHub artifacts expire after 30 days (vs persistent TestQuality history)
2. **No Trend Analysis**: Cannot easily track test pass rates, flakiness trends over time
3. **Single Framework**: Cannot aggregate results from multiple test frameworks in one dashboard
4. **Manual Test Gap**: No centralized manual test case management
5. **Artifact Download Required**: Team members must download artifacts to view reports (vs web-based TestQuality UI)
6. **Test Case Documentation**: No dedicated test case repository with formal specifications (see mitigation in evaluation document Section 11)

### Neutral

1. **Future Migration Path**: If TestQuality is adopted later, Playwright supports JUnit XML export and TestQuality CLI integration
2. **Hybrid Approach Possible**: Could implement lightweight GitHub Pages dashboard for improved historical visibility without full TestQuality integration
3. **Reversible Decision**: Easy to integrate TestQuality later if needs change

## Implementation

No changes required—current implementation in `.github/workflows/ci.yml` continues as-is:

```yaml
- name: Run Playwright tests
  working-directory: frontend
  run: npm test

- name: Upload Playwright HTML report
  uses: actions/upload-artifact@v4
  if: always()
  with:
    name: playwright-report
    path: frontend/playwright-report/
    retention-days: 30

- name: Upload test artifacts
  uses: actions/upload-artifact@v4
  if: failure()
  with:
    name: playwright-test-artifacts
    path: |
      frontend/test-results/
      frontend/playwright-report/
    retention-days: 30
```

## Alternatives Considered

### Alternative 1: TestQuality Integration (Deferred)

**Pros**: Superior reporting, historical trends, cross-framework aggregation, manual test management
**Cons**: Added complexity, maintenance overhead, subscription costs, external dependency
**Verdict**: Benefits don't justify costs at current project scale

### Alternative 2: Hybrid Lightweight Reporting

**Approach**: Generate JSON summaries and publish to GitHub Pages or simple dashboard
**Pros**: Improved historical visibility, no external platform, minimal complexity
**Cons**: Custom solution to build and maintain
**Verdict**: Future option if reporting needs increase without full TestQuality complexity

### Alternative 3: Other Test Management Platforms

Platforms evaluated but not deeply researched:
- **TestRail**: Enterprise-focused, similar to TestQuality
- **Zephyr**: Jira-native, requires Atlassian ecosystem
- **Xray**: Jira-native, requires Atlassian ecosystem
- **qTest**: Enterprise-focused, Tricentis product

**Verdict**: All share similar characteristics to TestQuality (test management platforms, not orchestration). Same decision logic applies.

## Follow-up Actions

1. ✅ Document evaluation findings (see `docs/testquality-evaluation.md`)
2. ✅ Create ADR capturing decision rationale
3. 🔲 Create `frontend/e2e/TEST-CATALOG.md` for formal test case documentation (recommended)
4. 🔲 Monitor test suite growth and team changes
5. 🔲 Revisit decision when trigger conditions are met (see "When to Reconsider" section)
6. 🔲 Consider lightweight GitHub Pages reporting if historical trends become important

## References

- [Detailed Evaluation Document](../testquality-evaluation.md) - Comprehensive 10-section analysis
- [TestQuality API Documentation](https://doc.testquality.com/api)
- [TestQuality Playwright Integration](https://doc.testquality.com/automations-imports/test-runners/integrating_with_Playwright)
- [TestQuality GitHub Integration](https://doc.testquality.com/integrate_with_github)
- [TestQuality CLI Repository](https://github.com/BitModern/testQualityCli)
- [ADR-0014: Playwright E2E Testing](./0014-playwright-e2e-testing.md)
- Current CI Implementation: `.github/workflows/ci.yml`
- Current Playwright Config: `frontend/playwright.config.ts`

## External Research Sources

This evaluation was based on current documentation and capabilities as of January 2026:

- [AI Test Management Tool - TestQuality](https://testquality.com/)
- [TestQuality Integrations](https://www.testquality.com/integrations/)
- [TestQuality Features](https://testquality.com/features/)
- [TestQuality GitHub Test Management](https://testquality.com/github-test-management/)
- [Best Automation Testing Tools for CI/CD Pipelines](https://testquality.com/best-automation-testing-tools-for-ci-cd-pipelines-your-complete-2025-guide/)
- [Comparing Top 10 Test Management Tools for 2025](https://testquality.com/comparing-the-top-10-test-management-tools-for-2025/)
- [TestQuality GitHub Marketplace](https://github.com/marketplace/testquality)
- [Playwright CI Documentation](https://playwright.dev/docs/ci-intro)

---

**Next Review Date**: When 2+ trigger conditions from "When to Reconsider" section are met
**Approved By**: [Pending]
