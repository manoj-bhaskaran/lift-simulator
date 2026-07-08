package com.liftsimulator.admin.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitingConfigTest {

    @Test
    void requestSizeLimitFilterRegistration_IncludesApiAndActuatorUrlPatternsBeforeRateLimit() {
        RateLimitingConfig config = new RateLimitingConfig();
        RequestSizeLimitProperties properties = new RequestSizeLimitProperties();

        FilterRegistrationBean<RequestSizeLimitFilter> registration =
            config.requestSizeLimitFilterRegistration(properties);

        assertThat(registration.getUrlPatterns())
            .containsExactlyInAnyOrder("/api/v1/*", "/actuator/*");
        assertThat(registration.getOrder()).isEqualTo(SecurityFilterProperties.DEFAULT_FILTER_ORDER - 2);
        assertThat(registration.getFilter()).isInstanceOf(RequestSizeLimitFilter.class);
    }

    @Test
    void rateLimitingFilterRegistration_IncludesApiAndActuatorUrlPatterns() {
        RateLimitingConfig config = new RateLimitingConfig();
        RateLimitingProperties properties = new RateLimitingProperties();

        FilterRegistrationBean<RateLimitingFilter> registration =
            config.rateLimitingFilterRegistration(properties);

        assertThat(registration.getUrlPatterns())
            .containsExactlyInAnyOrder("/api/v1/*", "/actuator/*");
        assertThat(registration.getOrder()).isEqualTo(SecurityFilterProperties.DEFAULT_FILTER_ORDER - 1);
        assertThat(registration.getFilter()).isInstanceOf(RateLimitingFilter.class);
    }
}
