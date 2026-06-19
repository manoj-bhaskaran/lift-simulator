package com.liftsimulator.admin.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitingConfigTest {

    @Test
    void rateLimitingFilterRegistration_IncludesApiAndActuatorUrlPatterns() {
        RateLimitingConfig config = new RateLimitingConfig();
        RateLimitingProperties properties = new RateLimitingProperties();

        FilterRegistrationBean<RateLimitingFilter> registration =
            config.rateLimitingFilterRegistration(properties);

        assertThat(registration.getUrlPatterns())
            .containsExactlyInAnyOrder("/api/v1/*", "/actuator/*");
        assertThat(registration.getOrder()).isEqualTo(SecurityProperties.DEFAULT_FILTER_ORDER - 1);
        assertThat(registration.getFilter()).isInstanceOf(RateLimitingFilter.class);
    }
}
