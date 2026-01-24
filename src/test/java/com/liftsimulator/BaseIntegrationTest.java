package com.liftsimulator;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for integration tests that need a real PostgreSQL database.
 * 
 * Uses application-test.yml configuration which:
 * - Defaults to localhost:5432 for local development
 * - Can be overridden via SPRING_DATASOURCE_* environment variables in CI/CD
 * 
 * For local development without a running PostgreSQL instance,
 * start one using docker compose or set SPRING_DATASOURCE_URL env var.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {
}
