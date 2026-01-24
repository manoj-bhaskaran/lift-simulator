package com.liftsimulator;

/**
 * Alias for BaseIntegrationTest.
 * 
 * Both local dev and CI/CD use the same base class configuration.
 * The difference is:
 * - Local dev: application-test.yml defaults to localhost:5432 (developer must have PostgreSQL running)
 * - CI/CD: Workflow sets SPRING_DATASOURCE_* env vars pointing to the GitHub Actions service
 * 
 * Tests extending this class work in both environments automatically.
 */
public abstract class LocalIntegrationTest extends BaseIntegrationTest {
}
