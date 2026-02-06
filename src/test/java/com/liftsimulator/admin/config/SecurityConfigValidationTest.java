package com.liftsimulator.admin.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SecurityConfig startup validation.
 * Tests that the application fails to start with insecure configurations.
 */
public class SecurityConfigValidationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(WebMvcAutoConfiguration.class))
        .withUserConfiguration(
            SecurityConfig.class,
            SecurityUsersProperties.class,
            CorsProperties.class,
            CsrfProperties.class
        );

    @Test
    void startupFails_WhenAdminPasswordIsEmpty() {
        contextRunner
            .withPropertyValues(
                "security.admin.username=admin",
                "security.admin.password=",
                "api.auth.key=test-key"
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
                "api.auth.key=test-key"
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
                "api.auth.key=test-key"
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
                "api.auth.key=test-key"
            )
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasBean("userDetailsService");
            });
    }
}
