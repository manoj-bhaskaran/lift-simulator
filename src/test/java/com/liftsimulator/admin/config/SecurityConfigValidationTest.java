package com.liftsimulator.admin.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SecurityConfig startup validation.
 * Tests that the application fails to start with insecure configurations.
 */
public class SecurityConfigValidationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(SecurityConfig.class);

    @Test
    void startupFails_WhenAdminPasswordIsEmpty() {
        contextRunner
            .withPropertyValues(
                "security.admin.username=admin",
                "security.admin.password=",
                "security.api-key=test-key"
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .isInstanceOf(BeanCreationException.class)
                    .rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Admin password must be configured");
            });
    }

    @Test
    void startupFails_WhenAdminPasswordIsBlank() {
        contextRunner
            .withPropertyValues(
                "security.admin.username=admin",
                "security.admin.password=   ",
                "security.api-key=test-key"
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .isInstanceOf(BeanCreationException.class)
                    .rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Admin password must be configured");
            });
    }

    @Test
    void startupFails_WhenAdminPasswordNotSet() {
        contextRunner
            .withPropertyValues(
                "security.admin.username=admin",
                "security.api-key=test-key"
                // security.admin.password not set - defaults to empty
            )
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .isInstanceOf(BeanCreationException.class)
                    .rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Admin password must be configured");
            });
    }

    @Test
    void startupSucceeds_WhenAdminPasswordIsConfigured() {
        contextRunner
            .withPropertyValues(
                "security.admin.username=admin",
                "security.admin.password=securepassword123",
                "security.api-key=test-key"
            )
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasBean("userDetailsService");
            });
    }
}
